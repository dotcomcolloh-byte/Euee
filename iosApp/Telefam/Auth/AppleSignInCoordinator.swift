import AuthenticationServices
import CryptoKit
import Foundation
import Security
import UIKit

final class AppleSignInCoordinator: NSObject, ASAuthorizationControllerDelegate, ASAuthorizationControllerPresentationContextProviding {
    private var continuation: CheckedContinuation<AppleAuthCredentials, Error>?
    private var currentRawNonce: String = ""

    enum AppleSignInError: Error {
        case cancelled
        case missingToken
    }

    @MainActor
    func signIn() async throws -> AppleAuthCredentials {
        let rawNonce = Self.randomNonceString(length: 32)
        currentRawNonce = rawNonce
        let hashedNonce = Self.sha256Hex(rawNonce)

        let provider = ASAuthorizationAppleIDProvider()
        let request = provider.createRequest()
        request.requestedScopes = [.fullName, .email]
        request.nonce = hashedNonce

        return try await withCheckedThrowingContinuation { cont in
            self.continuation = cont
            let controller = ASAuthorizationController(authorizationRequests: [request])
            controller.delegate = self
            controller.presentationContextProvider = self
            controller.performRequests()
        }
    }

    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        for scene in UIApplication.shared.connectedScenes {
            if let windowScene = scene as? UIWindowScene {
                if let keyWindow = windowScene.windows.first(where: { $0.isKeyWindow }) {
                    return keyWindow
                }
                if let first = windowScene.windows.first {
                    return first
                }
            }
        }
        return ASPresentationAnchor()
    }

    func authorizationController(controller: ASAuthorizationController, didCompleteWithAuthorization authorization: ASAuthorization) {
        guard let credential = authorization.credential as? ASAuthorizationAppleIDCredential,
              let tokenData = credential.identityToken,
              let identityToken = String(data: tokenData, encoding: .utf8),
              !identityToken.isEmpty else {
            continuation?.resume(throwing: AppleSignInError.missingToken)
            continuation = nil
            return
        }

        let authCode: String
        if let codeData = credential.authorizationCode,
           let code = String(data: codeData, encoding: .utf8) {
            authCode = code
        } else {
            authCode = ""
        }

        var displayName = ""
        if let fullName = credential.fullName {
            let formatted = PersonNameComponentsFormatter.localizedString(from: fullName, style: .default)
                .trimmingCharacters(in: .whitespacesAndNewlines)
            displayName = formatted
        }

        let creds = AppleAuthCredentials(
            identityToken: identityToken,
            authorizationCode: authCode,
            rawNonce: currentRawNonce,
            fullName: displayName,
            emailHint: credential.email ?? ""
        )
        continuation?.resume(returning: creds)
        continuation = nil
    }

    func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        if let authError = error as? ASAuthorizationError, authError.code == .canceled {
            continuation?.resume(throwing: AppleSignInError.cancelled)
        } else {
            continuation?.resume(throwing: error)
        }
        continuation = nil
    }

    private static func randomNonceString(length: Int) -> String {
        var randomBytes = [UInt8](repeating: 0, count: length)
        _ = SecRandomCopyBytes(kSecRandomDefault, randomBytes.count, &randomBytes)
        let charset = Array("0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._")
        return String(randomBytes.map { charset[Int($0) % charset.count] })
    }

    private static func sha256Hex(_ input: String) -> String {
        let digest = SHA256.hash(data: Data(input.utf8))
        return digest.map { String(format: "%02x", $0) }.joined()
    }
}
