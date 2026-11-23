import Foundation

enum AppScreen: String, Codable {
    case authEmail = "AUTH_EMAIL"
    case authOtp = "AUTH_OTP"
    case profileSetup = "PROFILE_SETUP"
    case home = "HOME"
}

enum ThemeMode: String, Codable {
    case system = "SYSTEM"
    case light = "LIGHT"
    case dark = "DARK"
}

enum HomeTab: String, Codable {
    case chats = "CHATS"
    case contacts = "CONTACTS"
    case profile = "PROFILE"
}

enum ProfileContentTab: String, Codable, CaseIterable {
    case posts = "POSTS"
    case saved = "SAVED"
    case reposts = "REPOSTS"
    case locked = "LOCKED"
}

enum ContactsTab: String, Codable, CaseIterable {
    case all = "ALL"
    case suggestions = "SUGGESTIONS"
    case followers = "FOLLOWERS"
    case following = "FOLLOWING"
    case friends = "FRIENDS"
}

struct CountryOption: Codable, Identifiable, Equatable {
    let iso2: String
    let name: String
    let dialCode: String
    let placeholder: String

    var id: String { iso2 }
}

struct ContactItem: Codable, Identifiable, Equatable {
    let userId: String
    let fullName: String
    let username: String
    let countryIso: String
    let countryName: String
    let avatarUrl: String
    let iFollow: Bool
    let followsMe: Bool
    let isFriend: Bool

    var id: String { userId }

    func withFollowState(iFollow: Bool, isFriend: Bool) -> ContactItem {
        ContactItem(
            userId: userId,
            fullName: fullName,
            username: username,
            countryIso: countryIso,
            countryName: countryName,
            avatarUrl: avatarUrl,
            iFollow: iFollow,
            followsMe: followsMe,
            isFriend: isFriend
        )
    }
}

struct BackgroundPhotoItem: Codable, Identifiable, Equatable {
    let id: String
    let imageUrl: String
    let fingerprintSha256: String
    let fingerprintDhash: String
    let width: Int
    let height: Int
}

struct ProfileUiState: Codable, Equatable {
    var fullName: String = ""
    var username: String = ""
    var usernameChecking: Bool = false
    var usernameChecked: Bool = false
    var usernameAvailable: Bool = false
    var usernameStatusMsg: String = ""
    var phoneCountryIso: String = "KE"
    var phoneDialCode: String = "+254"
    var phoneCountryName: String = "Kenya"
    var phoneNumber: String = ""
    var phoneError: String = ""
    var gender: String = ""
    var dateOfBirth: String = ""
    var dobError: String = ""
    var location: String = ""
    var bio: String = ""
    var avatarUrl: String = ""
    var photoUploading: Bool = false
    var usernameChangedAtEpochSec: Int64 = 0
    var nextUsernameChangeAtEpochSec: Int64 = 0
    var canChangeUsername: Bool = true
    var followersCount: Int = 0
    var followingCount: Int = 0
    var likesCount: Int = 0
    var postsCount: Int = 0
    var backgroundPhotos: [BackgroundPhotoItem] = []
    var backgroundUploading: Bool = false
}

struct TelefamUiState: Codable, Equatable {
    var userId: String = ""
    var screen: AppScreen = .authEmail
    var themeMode: ThemeMode = .system
    var homeTab: HomeTab = .chats
    var profileContentTab: ProfileContentTab = .posts
    var contactsTab: ContactsTab = .all
    var contactsSearchQuery: String = ""
    var email: String = ""
    var otp: String = ""
    var isLoading: Bool = false
    var errorMessage: String = ""
    var noticeMessage: String = ""
    var resendRemainingSec: Int = 0
    var resendTimerText: String = "00:00"
    var canResend: Bool = true
    var failedAttempts: Int = 0
    var lockCycles: Int = 0
    var lockRemainingSec: Int = 0
    var lockTimerText: String = "00:00"
    var integrityCompromised: Bool = false
    var sealedVaultB64: String = ""
    var profile: ProfileUiState = ProfileUiState()
}

struct NativeHttpAction: Codable {
    let requestId: String
    let method: String
    let url: String
    let headers: [String: String]?
    let body: String?
    let extra: String?
}

struct NativeActionEnvelope: Codable {
    let error: String?
    let notice: String?
    let state: TelefamUiState?
    let httpAction: NativeHttpAction?
}

struct ContactsFilterEnvelope: Codable {
    let contacts: [ContactItem]?
}

struct ContactsSyncEnvelope: Codable {
    let ok: Bool?
    let contacts: [ContactItem]?
}

struct AppleAuthCredentials {
    let identityToken: String
    let authorizationCode: String
    let rawNonce: String
    let fullName: String
    let emailHint: String
}

enum TelefamStrings {
    static let appName = "Telefam"
    static let tagline = "Stay connected. Stay together."
    static let enterEmailTitle = "Enter Your Email"
    static let enterEmailSubtitle = "Enter your email to get OTP"
    static let emailLabel = "Email"
    static let emailHint = "Enter your email"
    static let sendOtp = "Send OTP"
    static let orDivider = "or"
    static let continueApple = "Continue with Apple"
    static let enterOtpTitle = "Enter OTP"
    static let otpSentTo = "We sent a verification code to"
    static let verifyOtp = "Verify OTP"
    static let resendCodeIn = "Resend code in"
    static let afterCooldownPrefix = "After"
    static let afterCooldownMiddle = "you can"
    static let resendCode = "Resend code"
    static let changeEmail = "Change email"
    static let profileSetupTitle = "Profile Setup"
    static let profileSetupSubtitle = "Tell us about yourself"
    static let uploadPhotoTitle = "Upload Profile Photo"
    static let uploadPhotoSubtitle = "Add a clear photo to help your friends recognize you."
    static let fullNameLabel = "Full Name"
    static let fullNameHint = "Enter your full name"
    static let usernameLabel = "Username"
    static let usernameHint = "Choose a username"
    static let phoneLabel = "Phone Number"
    static let phoneHint = "Enter your phone number"
    static let genderLabel = "Gender"
    static let genderHint = "Select gender"
    static let dobLabel = "Date of Birth"
    static let dobHint = "Select date of birth"
    static let locationLabel = "Location"
    static let locationHint = "Enter your city or location"
    static let bioLabel = "Bio"
    static let bioHint = "Tell us something about yourself\u{2026}"
    static let continueText = "Continue"
    static let searchHint = "Search chats, people, \u{2026}"
    static let startConversation = "Start a conversation"
    static let startConversationSubtitle = "Tap the button below to chat with your friends, family or groups"
    static let startChat = "Start chat"
    static let navChats = "Chats"
    static let navContacts = "Contacts"
    static let navPost = "Post"
    static let navFeeds = "Feeds"
    static let navProfile = "Profile"
    static let comingSoon = "Coming soon"
    static let noInternet = "No internet connection"
    static let genericError = "Error has occurred"
    static let cropPhoto = "Crop Photo"
    static let applyCrop = "Apply"
    static let cancel = "Cancel"
    static let contactsSearchHint = "Search by name or username"
    static let contactsTabAll = "All"
    static let contactsTabSuggestions = "Suggestions"
    static let contactsTabFollowers = "Followers"
    static let contactsTabFollowing = "Following"
    static let contactsTabFriends = "Friends"
    static let contactsActionFollow = "Follow"
    static let contactsActionFollowBack = "Follow back"
    static let contactsActionFollowing = "Following"
    static let contactsActionFriends = "Friends"
    static let contactsEmpty = "No contacts found"
    static let followRateLimitError = "Error has occurred. Try again later."
    static let profileEditBackground = "Edit background"
    static let profileEditProfile = "Edit profile"
    static let profileShareProfile = "Share profile"
    static let profileFollowing = "Following"
    static let profileFollowers = "Followers"
    static let profileLikes = "Likes"
    static let profileTabPosts = "Posts"
    static let profileTabSaved = "Saved"
    static let profileTabReposts = "Reposts"
    static let profileTabLocked = "Locked"
    static let profileAllPosts = "All posts"
    static let profileNoPosts = "No posts yet"
    static let profileSave = "Save"
    static let profileAddBackground = "Add background"
    static let profileBackgroundsTitle = "Background photos"
    static let profileUsernameLockedUntil = "Locked until"
    static let profileSubscribers = "Subscribers"
    static let profileSettings = "Settings"
    static let profileMenu = "Menu"
}
