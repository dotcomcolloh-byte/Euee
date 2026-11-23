import base64
import hashlib
import io
import json
import os
import socket
import struct
from PIL import Image, ImageOps

Image.MAX_IMAGE_PIXELS = 16_777_216  # 4096x4096 max decompression-bomb guard
MAX_BYTES = 10 * 1024 * 1024
SOCKET_PATH = os.environ.get("SANDBOX_SOCKET_PATH", "/run/sandbox/processor.sock")


def detect_magic_mime(data: bytes) -> str | None:
    if len(data) < 12:
        return None
    if data[:3] == b"\xff\xd8\xff":
        return "image/jpeg"
    if data[:8] == b"\x89PNG\r\n\x1a\n":
        return "image/png"
    if data[:4] == b"RIFF" and data[8:12] == b"WEBP":
        return "image/webp"
    return None


def compute_dhash(img: Image.Image) -> str:
    gray = img.convert("L").resize((9, 8), Image.Resampling.LANCZOS)
    pixels = list(gray.getdata())
    bits = 0
    for row in range(8):
        for col in range(8):
            left = pixels[row * 9 + col]
            right = pixels[row * 9 + col + 1]
            bits = (bits << 1) | (1 if left > right else 0)
    return f"{bits:016x}"


def process_image(meta: dict, raw: bytes) -> dict:
    if len(raw) == 0 or len(raw) > MAX_BYTES:
        return {"ok": False, "error": "Invalid image size"}
    mime = detect_magic_mime(raw)
    if mime is None:
        return {"ok": False, "error": "Unsupported image format"}

    # Pass 1: strict structural header/format verification before pixel decode
    with Image.open(io.BytesIO(raw)) as verify_img:
        if verify_img.format not in ("JPEG", "PNG", "WEBP"):
            return {"ok": False, "error": "Unsupported image format"}
        if getattr(verify_img, "is_animated", False) or getattr(verify_img, "n_frames", 1) > 1:
            return {"ok": False, "error": "Animated images are not allowed"}
        verify_img.verify()

    # Pass 2: decode, strip all metadata/EXIF/ICC/XMP, crop, resize, fingerprint, re-encode
    with Image.open(io.BytesIO(raw)) as img:
        if img.width < 16 or img.height < 16 or img.width > 4096 or img.height > 4096:
            return {"ok": False, "error": "Invalid image dimensions"}

        img = ImageOps.exif_transpose(img).convert("RGB")
        w, h = img.size

        cx = max(0.0, min(0.9, float(meta.get("cropX", 0.0))))
        cy = max(0.0, min(0.9, float(meta.get("cropY", 0.0))))
        cw = max(0.1, min(1.0 - cx, float(meta.get("cropWidth", 1.0))))
        ch = max(0.1, min(1.0 - cy, float(meta.get("cropHeight", 1.0))))

        left = int(cx * w)
        top = int(cy * h)
        right = max(left + 1, min(w, int((cx + cw) * w)))
        bottom = max(top + 1, min(h, int((cy + ch) * h)))

        cropped = img.crop((left, top, right, bottom))
        mode = str(meta.get("mode", "avatar")).lower()
        max_w = int(meta.get("maxWidth", 1440 if mode == "background" else 512))
        max_h = int(meta.get("maxHeight", 900 if mode == "background" else 512))
        max_w = max(64, min(1920, max_w))
        max_h = max(64, min(1920, max_h))
        cropped.thumbnail((max_w, max_h), Image.Resampling.LANCZOS)

        # Compute fingerprints from sanitized RGB pixels
        sha256_hex = hashlib.sha256(cropped.tobytes()).hexdigest()
        dhash_hex = compute_dhash(cropped)

        # Re-encode cleanly stripping all EXIF/ICC/XMP metadata
        out_buf = io.BytesIO()
        cropped.save(out_buf, format="JPEG", quality=86, optimize=True)
        encoded = out_buf.getvalue()

        return {
            "ok": True,
            "mime": "image/jpeg",
            "width": cropped.width,
            "height": cropped.height,
            "sha256Hex": sha256_hex,
            "dhashHex": dhash_hex,
            "jpegBase64": base64.b64encode(encoded).decode("ascii"),
        }


def recv_exact(conn: socket.socket, n: int) -> bytes:
    chunks = []
    remaining = n
    while remaining > 0:
        chunk = conn.recv(remaining)
        if not chunk:
            raise EOFError("Unexpected EOF")
        chunks.append(chunk)
        remaining -= len(chunk)
    return b"".join(chunks)


def main() -> None:
    os.makedirs(os.path.dirname(SOCKET_PATH), exist_ok=True)
    if os.path.exists(SOCKET_PATH):
        os.unlink(SOCKET_PATH)

    srv = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
    srv.bind(SOCKET_PATH)
    os.chmod(SOCKET_PATH, 0o660)
    srv.listen(32)

    while True:
        conn, _ = srv.accept()
        with conn:
            try:
                (meta_len,) = struct.unpack(">I", recv_exact(conn, 4))
                if meta_len > 8192:
                    raise ValueError("Header too large")
                meta = json.loads(recv_exact(conn, meta_len).decode("utf-8"))
                (img_len,) = struct.unpack(">I", recv_exact(conn, 4))
                if img_len > MAX_BYTES:
                    raise ValueError("Payload too large")
                raw = recv_exact(conn, img_len)
                res = process_image(meta, raw)
            except Exception:
                res = {"ok": False, "error": "Image processing failed"}
            payload = json.dumps(res).encode("utf-8")
            conn.sendall(struct.pack(">I", len(payload)) + payload)


if __name__ == "__main__":
    main()
