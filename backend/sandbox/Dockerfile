FROM python:3.12-alpine
RUN apk add --no-cache libjpeg-turbo zlib libwebp \
    && pip install --no-cache-dir pillow==11.1.0 \
    && addgroup -S sandbox && adduser -S sandbox -G sandbox
WORKDIR /sandbox
COPY sandbox_worker.py /sandbox/sandbox_worker.py
USER 65534:65534
ENTRYPOINT ["python3", "/sandbox/sandbox_worker.py"]
