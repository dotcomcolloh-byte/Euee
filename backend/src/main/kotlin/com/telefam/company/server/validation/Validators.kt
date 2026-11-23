package com.telefam.company.server.validation

import java.time.LocalDate
import java.time.Period
import java.time.ZoneOffset

object Validators {
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val USERNAME_REGEX = Regex("^[a-z0-9_]{3,24}$")
    private val ALLOWED_GENDERS = setOf("Male", "Female", "Non-binary", "Prefer not to say")

    private data class CountryPhoneRule(
        val dialCode: String,
        val minDigits: Int,
        val maxDigits: Int,
        val stripLeadingZero: Boolean,
        val allowedFirst: String
    )

    private val COUNTRY_RULES = mapOf(
        "KE" to CountryPhoneRule("254", 9, 9, true, "17"),
        "US" to CountryPhoneRule("1", 10, 10, false, "23456789"),
        "CA" to CountryPhoneRule("1", 10, 10, false, "23456789"),
        "GB" to CountryPhoneRule("44", 10, 10, true, "12378"),
        "NG" to CountryPhoneRule("234", 10, 10, true, "789"),
        "UG" to CountryPhoneRule("256", 9, 9, true, "347"),
        "TZ" to CountryPhoneRule("255", 9, 9, true, "67"),
        "RW" to CountryPhoneRule("250", 9, 9, true, "7"),
        "ET" to CountryPhoneRule("251", 9, 9, true, "179"),
        "ZA" to CountryPhoneRule("27", 9, 9, true, "12345678"),
        "GH" to CountryPhoneRule("233", 9, 9, true, "235"),
        "EG" to CountryPhoneRule("20", 10, 10, true, "1"),
        "MA" to CountryPhoneRule("212", 9, 9, true, "567"),
        "IN" to CountryPhoneRule("91", 10, 10, true, "6789"),
        "PK" to CountryPhoneRule("92", 10, 10, true, "3"),
        "BD" to CountryPhoneRule("880", 10, 10, true, "1"),
        "AE" to CountryPhoneRule("971", 9, 9, true, "2345679"),
        "SA" to CountryPhoneRule("966", 9, 9, true, "5"),
        "DE" to CountryPhoneRule("49", 10, 11, true, "123456789"),
        "FR" to CountryPhoneRule("33", 9, 9, true, "12345679"),
        "IT" to CountryPhoneRule("39", 9, 10, false, "30"),
        "ES" to CountryPhoneRule("34", 9, 9, false, "6789"),
        "NL" to CountryPhoneRule("31", 9, 9, true, "12345678"),
        "BE" to CountryPhoneRule("32", 8, 9, true, "123456789"),
        "CH" to CountryPhoneRule("41", 9, 9, true, "23456789"),
        "AT" to CountryPhoneRule("43", 10, 11, true, "6"),
        "SE" to CountryPhoneRule("46", 9, 9, true, "123456789"),
        "NO" to CountryPhoneRule("47", 8, 8, false, "49"),
        "IE" to CountryPhoneRule("353", 9, 9, true, "8"),
        "PT" to CountryPhoneRule("351", 9, 9, false, "29"),
        "PL" to CountryPhoneRule("48", 9, 9, false, "45678"),
        "TR" to CountryPhoneRule("90", 10, 10, true, "5"),
        "BR" to CountryPhoneRule("55", 10, 11, false, "123456789"),
        "MX" to CountryPhoneRule("52", 10, 10, false, "123456789"),
        "AR" to CountryPhoneRule("54", 10, 10, true, "1239"),
        "AU" to CountryPhoneRule("61", 9, 9, true, "4"),
        "NZ" to CountryPhoneRule("64", 8, 10, true, "2"),
        "JP" to CountryPhoneRule("81", 10, 10, true, "789"),
        "KR" to CountryPhoneRule("82", 9, 10, true, "1"),
        "SG" to CountryPhoneRule("65", 8, 8, false, "896"),
        "MY" to CountryPhoneRule("60", 9, 10, true, "1"),
        "ID" to CountryPhoneRule("62", 9, 12, true, "8"),
        "PH" to CountryPhoneRule("63", 10, 10, true, "9"),
        "TH" to CountryPhoneRule("66", 9, 9, true, "689"),
        "VN" to CountryPhoneRule("84", 9, 10, true, "35789")
    )

    private val COUNTRY_NAMES = mapOf(
        "KE" to "Kenya", "US" to "United States", "CA" to "Canada", "GB" to "United Kingdom",
        "NG" to "Nigeria", "UG" to "Uganda", "TZ" to "Tanzania", "RW" to "Rwanda",
        "ET" to "Ethiopia", "ZA" to "South Africa", "GH" to "Ghana", "EG" to "Egypt",
        "MA" to "Morocco", "IN" to "India", "PK" to "Pakistan", "BD" to "Bangladesh",
        "AE" to "United Arab Emirates", "SA" to "Saudi Arabia", "DE" to "Germany",
        "FR" to "France", "IT" to "Italy", "ES" to "Spain", "NL" to "Netherlands",
        "BE" to "Belgium", "CH" to "Switzerland", "AT" to "Austria", "SE" to "Sweden",
        "NO" to "Norway", "IE" to "Ireland", "PT" to "Portugal", "PL" to "Poland",
        "TR" to "Turkey", "BR" to "Brazil", "MX" to "Mexico", "AR" to "Argentina",
        "AU" to "Australia", "NZ" to "New Zealand", "JP" to "Japan", "KR" to "South Korea",
        "SG" to "Singapore", "MY" to "Malaysia", "ID" to "Indonesia", "PH" to "Philippines",
        "TH" to "Thailand", "VN" to "Vietnam"
    )

    fun countryName(iso2: String): String =
        COUNTRY_NAMES[iso2.trim().uppercase()] ?: iso2.trim().uppercase().ifEmpty { "Kenya" }

    fun isValidEmail(email: String): Boolean =
        email.length in 5..254 && EMAIL_REGEX.matches(email.trim())

    fun isValidUsername(username: String): Boolean =
        USERNAME_REGEX.matches(username.trim().lowercase())

    fun isValidGender(gender: String): Boolean = gender in ALLOWED_GENDERS

    fun validateAge13Plus(dobIso: String, todayUtc: LocalDate = LocalDate.now(ZoneOffset.UTC)): String? {
        val parsed = runCatching { LocalDate.parse(dobIso.trim()) }.getOrNull()
            ?: return "Invalid date of birth"
        if (parsed.isAfter(todayUtc)) return "Invalid date of birth"
        val years = Period.between(parsed, todayUtc).years
        return if (years < 13) "Must be at least 13 years old" else null
    }

    fun normalizeAndValidatePhone(countryIso: String, rawInput: String): Result<String> {
        val rule = COUNTRY_RULES[countryIso.trim().uppercase()]
            ?: return Result.failure(IllegalArgumentException("Unsupported country code"))
        var digits = rawInput.filter { it.isDigit() }
        if (digits.length > rule.maxDigits && digits.startsWith(rule.dialCode)) {
            digits = digits.removePrefix(rule.dialCode)
        }
        if (rule.stripLeadingZero && digits.startsWith("0")) {
            digits = digits.drop(1)
        }
        if (digits.length !in rule.minDigits..rule.maxDigits) {
            return Result.failure(IllegalArgumentException("Invalid phone number length for $countryIso"))
        }
        if (rule.allowedFirst.isNotEmpty() && digits[0] !in rule.allowedFirst) {
            return Result.failure(IllegalArgumentException("Invalid phone number prefix for $countryIso"))
        }
        return Result.success("+${rule.dialCode}$digits")
    }
}
