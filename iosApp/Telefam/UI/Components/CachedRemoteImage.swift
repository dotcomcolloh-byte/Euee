import SwiftUI
import UIKit

struct CachedRemoteImage<Placeholder: View>: View {
    let urlString: String
    let bridge: TelefamCoreBridge
    var contentMode: ContentMode = .fill
    @ViewBuilder let placeholder: () -> Placeholder

    @State private var uiImage: UIImage?

    var body: some View {
        Group {
            if let uiImage {
                Image(uiImage: uiImage)
                    .resizable()
                    .aspectRatio(contentMode: contentMode)
            } else {
                placeholder()
            }
        }
        .task(id: urlString) {
            guard !urlString.isEmpty else {
                uiImage = nil
                return
            }
            if let cached = RemoteImageMemoryCache.shared.image(forKey: urlString) {
                uiImage = cached
                return
            }
            if let data = await bridge.fetchImageData(from: urlString),
               let loaded = UIImage(data: data) {
                RemoteImageMemoryCache.shared.insert(loaded, forKey: urlString)
                await MainActor.run {
                    uiImage = loaded
                }
            }
        }
    }
}

final class RemoteImageMemoryCache {
    static let shared = RemoteImageMemoryCache()
    private let cache = NSCache<NSString, UIImage>()

    private init() {
        cache.countLimit = 120
    }

    func image(forKey key: String) -> UIImage? {
        cache.object(forKey: key as NSString)
    }

    func insert(_ image: UIImage, forKey key: String) {
        cache.setObject(image, forKey: key as NSString)
    }
}
