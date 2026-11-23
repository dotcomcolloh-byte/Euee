import SwiftUI
import UIKit

struct PhotoCropSheet: View {
    let imageData: Data
    let mimeType: String
    var aspectRatio: CGFloat = 1.0
    var isCircularOverlay: Bool = true
    let onCancel: () -> Void
    let onApplyCrop: (Double, Double, Double, Double) -> Void

    @State private var scale: CGFloat = 1.0
    @State private var lastScale: CGFloat = 1.0
    @State private var offset: CGSize = .zero
    @State private var lastOffset: CGSize = .zero

    var body: some View {
        let uiImage = UIImage(data: imageData)

        ZStack {
            Color.black.ignoresSafeArea()

            VStack(spacing: 0) {
                HStack {
                    Button(action: onCancel) {
                        Text(TelefamStrings.cancel)
                            .font(.system(size: 16, weight: .medium))
                            .foregroundColor(.white.opacity(0.9))
                            .frame(minWidth: 48, minHeight: 48)
                    }
                    Spacer()
                    Text(TelefamStrings.cropPhoto)
                        .font(.system(size: 17, weight: .bold))
                        .foregroundColor(.white)
                    Spacer()
                    Button {
                        let crop = computeNormalizedCropRect(image: uiImage)
                        onApplyCrop(crop.0, crop.1, crop.2, crop.3)
                    } label: {
                        Text(TelefamStrings.applyCrop)
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(TelefamColors.crimsonLight)
                            .frame(minWidth: 48, minHeight: 48)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 8)

                Spacer()

                GeometryReader { geo in
                    let cropWidth = min(geo.size.width * 0.84, 380)
                    let cropHeight = cropWidth / aspectRatio

                    ZStack {
                        if let uiImage {
                            Image(uiImage: uiImage)
                                .resizable()
                                .aspectRatio(contentMode: .fit)
                                .scaleEffect(scale)
                                .offset(offset)
                                .gesture(
                                    SimultaneousGesture(
                                        DragGesture()
                                            .onChanged { value in
                                                offset = CGSize(
                                                    width: lastOffset.width + value.translation.width,
                                                    height: lastOffset.height + value.translation.height
                                                )
                                            }
                                            .onEnded { _ in
                                                lastOffset = offset
                                            },
                                        MagnificationGesture()
                                            .onChanged { value in
                                                scale = min(max(lastScale * value, 1.0), 4.0)
                                            }
                                            .onEnded { _ in
                                                lastScale = scale
                                            }
                                    )
                                )
                        }

                        Color.black.opacity(0.55)
                            .reverseMask {
                                if isCircularOverlay {
                                    Circle()
                                        .frame(width: cropWidth, height: cropWidth)
                                } else {
                                    RoundedRectangle(cornerRadius: 16)
                                        .frame(width: cropWidth, height: cropHeight)
                                }
                            }
                            .allowsHitTesting(false)

                        if isCircularOverlay {
                            Circle()
                                .stroke(Color.white, lineWidth: 2)
                                .frame(width: cropWidth, height: cropWidth)
                                .allowsHitTesting(false)
                        } else {
                            RoundedRectangle(cornerRadius: 16)
                                .stroke(Color.white, lineWidth: 2)
                                .frame(width: cropWidth, height: cropHeight)
                                .allowsHitTesting(false)
                        }
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .clipped()
                }

                Spacer()

                VStack(spacing: 10) {
                    Slider(
                        value: Binding(
                            get: { Double(scale) },
                            set: { newVal in
                                scale = CGFloat(newVal)
                                lastScale = scale
                            }
                        ),
                        in: 1.0...4.0
                    )
                    .tint(TelefamColors.crimson)
                }
                .padding(.horizontal, 28)
                .padding(.bottom, 28)
            }
        }
    }

    private func computeNormalizedCropRect(image: UIImage?) -> (Double, Double, Double, Double) {
        guard let image, image.size.width > 0, image.size.height > 0 else {
            return (0.0, 0.0, 1.0, 1.0)
        }
        let s = max(Double(scale), 1.0)
        let cropW = min(max(1.0 / s, 0.1), 1.0)
        let cropH = min(max((1.0 / s) / Double(aspectRatio), 0.1), 1.0)
        let shiftX = -Double(offset.width) / (300.0 * s)
        let shiftY = -Double(offset.height) / (300.0 * s)
        let cx = min(max(0.5 - cropW / 2.0 + shiftX, 0.0), 1.0 - cropW)
        let cy = min(max(0.5 - cropH / 2.0 + shiftY, 0.0), 1.0 - cropH)
        return (cx, cy, cropW, cropH)
    }
}

private extension View {
    func reverseMask<Mask: View>(@ViewBuilder _ mask: () -> Mask) -> some View {
        self.mask {
            Rectangle()
                .overlay(alignment: .center) {
                    mask()
                        .blendMode(.destinationOut)
                }
                .compositingGroup()
        }
    }
}
