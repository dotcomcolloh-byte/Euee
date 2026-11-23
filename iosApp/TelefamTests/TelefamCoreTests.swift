import XCTest
@testable import Telefam

final class TelefamCoreTests: XCTestCase {

    func testEnginePrepareAppleAuthProducesValidRequest() throws {
        let engine = telefam_engine_create()
        XCTAssertNotNil(engine)
        defer { telefam_engine_destroy(engine) }

        let masterKey = [UInt8](repeating: 0x2A, count: 32)
        let initPtr = masterKey.withUnsafeBytes { ptr in
            telefam_init(
                engine,
                "https://api.telefam.company",
                true,
                "iPad14,5",
                "devhash123",
                "pubkey123",
                "",
                "",
                ptr.bindMemory(to: UInt8.self).baseAddress,
                masterKey.count,
                "",
                1_750_000_000_000
            )
        }
        if let initPtr { telefam_free_string(initPtr) }

        let rnd = [UInt8](repeating: 0x11, count: 16)
        let authPtr = rnd.withUnsafeBytes { ptr in
            telefam_prepare_apple_auth(
                engine,
                "header.payload.signature",
                "auth_code_123",
                "raw_nonce_xyz",
                "Taylor Telefam",
                "taylor@telefam.company",
                true,
                1_750_000_005_000,
                ptr.bindMemory(to: UInt8.self).baseAddress,
                rnd.count
            )
        }
        XCTAssertNotNil(authPtr)
        let json = String(cString: authPtr!)
        telefam_free_string(authPtr)

        let decoded = try JSONDecoder().decode(NativeActionEnvelope.self, from: Data(json.utf8))
        XCTAssertEqual(decoded.httpAction?.method, "POST")
        XCTAssertTrue(decoded.httpAction?.url.hasSuffix("/api/v1/auth/apple") == true)
        XCTAssertTrue(decoded.httpAction?.body?.contains("raw_nonce_xyz") == true)
    }

    func testEngineEnforcesMinimum13YearsDobRule() {
        let engine = telefam_engine_create()
        XCTAssertNotNil(engine)
        defer { telefam_engine_destroy(engine) }

        let masterKey = [UInt8](repeating: 0x33, count: 32)
        if let p = masterKey.withUnsafeBytes({ ptr in
            telefam_init(
                engine,
                "https://api.telefam.company",
                true,
                "iPhone16,2",
                "devhash",
                "pubkey",
                "",
                "",
                ptr.bindMemory(to: UInt8.self).baseAddress,
                masterKey.count,
                "",
                1_750_000_000_000
            )
        }) {
            telefam_free_string(p)
        }

        if let p = telefam_update_profile_field(engine, "dateOfBirth", "2020-05-01", 2026, 11, 22) {
            telefam_free_string(p)
        }
        let uiPtr = telefam_get_ui_state(engine)
        XCTAssertNotNil(uiPtr)
        let uiJson = String(cString: uiPtr!)
        telefam_free_string(uiPtr)
        let state = try? JSONDecoder().decode(TelefamUiState.self, from: Data(uiJson.utf8))
        XCTAssertFalse(state?.profile.dobError.isEmpty ?? true)
    }

    func testEngineFollowRateLimitCapsAt100PerMinute() {
        let engine = telefam_engine_create()
        XCTAssertNotNil(engine)
        defer { telefam_engine_destroy(engine) }

        let baseMs: Int64 = 1_750_000_000_000
        for i in 0..<100 {
            XCTAssertTrue(telefam_check_and_record_follow_rate(engine, baseMs + Int64(i * 10)))
        }
        XCTAssertFalse(telefam_check_and_record_follow_rate(engine, baseMs + 2_000))
    }
}
