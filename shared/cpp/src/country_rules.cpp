#include "country_rules.hpp"

#include <algorithm>
#include <cctype>

namespace telefam::countries {

const std::vector<CountryRule>& all_countries() {
    static const std::vector<CountryRule> kCountries = {
        {"KE", "Kenya", "254", 9, 9, true, "17", "712 345 678"},
        {"US", "United States", "1", 10, 10, false, "23456789", "202 555 0143"},
        {"CA", "Canada", "1", 10, 10, false, "23456789", "416 555 0198"},
        {"GB", "United Kingdom", "44", 10, 10, true, "12378", "7700 900123"},
        {"NG", "Nigeria", "234", 10, 10, true, "789", "803 123 4567"},
        {"UG", "Uganda", "256", 9, 9, true, "347", "772 123 456"},
        {"TZ", "Tanzania", "255", 9, 9, true, "67", "712 345 678"},
        {"RW", "Rwanda", "250", 9, 9, true, "7", "788 123 456"},
        {"ET", "Ethiopia", "251", 9, 9, true, "179", "911 234 567"},
        {"ZA", "South Africa", "27", 9, 9, true, "12345678", "82 123 4567"},
        {"GH", "Ghana", "233", 9, 9, true, "235", "24 123 4567"},
        {"EG", "Egypt", "20", 10, 10, true, "1", "100 123 4567"},
        {"MA", "Morocco", "212", 9, 9, true, "567", "612 345 678"},
        {"IN", "India", "91", 10, 10, true, "6789", "98765 43210"},
        {"PK", "Pakistan", "92", 10, 10, true, "3", "300 123 4567"},
        {"BD", "Bangladesh", "880", 10, 10, true, "1", "1712 345 678"},
        {"AE", "United Arab Emirates", "971", 9, 9, true, "2345679", "50 123 4567"},
        {"SA", "Saudi Arabia", "966", 9, 9, true, "5", "50 123 4567"},
        {"DE", "Germany", "49", 10, 11, true, "123456789", "151 23456789"},
        {"FR", "France", "33", 9, 9, true, "12345679", "6 12 34 56 78"},
        {"IT", "Italy", "39", 9, 10, false, "30", "312 345 6789"},
        {"ES", "Spain", "34", 9, 9, false, "6789", "612 34 56 78"},
        {"NL", "Netherlands", "31", 9, 9, true, "12345678", "6 12345678"},
        {"BE", "Belgium", "32", 8, 9, true, "123456789", "470 12 34 56"},
        {"CH", "Switzerland", "41", 9, 9, true, "23456789", "79 123 45 67"},
        {"AT", "Austria", "43", 10, 11, true, "6", "664 1234567"},
        {"SE", "Sweden", "46", 9, 9, true, "123456789", "70 123 45 67"},
        {"NO", "Norway", "47", 8, 8, false, "49", "412 34 567"},
        {"IE", "Ireland", "353", 9, 9, true, "8", "85 123 4567"},
        {"PT", "Portugal", "351", 9, 9, false, "29", "912 345 678"},
        {"PL", "Poland", "48", 9, 9, false, "45678", "501 234 567"},
        {"TR", "Turkey", "90", 10, 10, true, "5", "532 123 4567"},
        {"BR", "Brazil", "55", 10, 11, false, "123456789", "11 91234 5678"},
        {"MX", "Mexico", "52", 10, 10, false, "123456789", "55 1234 5678"},
        {"AR", "Argentina", "54", 10, 10, true, "1239", "11 1234 5678"},
        {"AU", "Australia", "61", 9, 9, true, "4", "412 345 678"},
        {"NZ", "New Zealand", "64", 8, 10, true, "2", "21 123 4567"},
        {"JP", "Japan", "81", 10, 10, true, "789", "90 1234 5678"},
        {"KR", "South Korea", "82", 9, 10, true, "1", "10 1234 5678"},
        {"SG", "Singapore", "65", 8, 8, false, "896", "8123 4567"},
        {"MY", "Malaysia", "60", 9, 10, true, "1", "12 345 6789"},
        {"ID", "Indonesia", "62", 9, 12, true, "8", "812 3456 7890"},
        {"PH", "Philippines", "63", 10, 10, true, "9", "917 123 4567"},
        {"TH", "Thailand", "66", 9, 9, true, "689", "81 234 5678"},
        {"VN", "Vietnam", "84", 9, 10, true, "35789", "91 234 56 78"},
    };
    return kCountries;
}

const CountryRule* find_by_iso2(const std::string& iso2) {
    std::string upper = iso2;
    for (char& c : upper) c = static_cast<char>(std::toupper(static_cast<unsigned char>(c)));
    for (const auto& r : all_countries()) {
        if (r.iso2 == upper) return &r;
    }
    return nullptr;
}

PhoneValidationResult validate_phone(const std::string& iso2, const std::string& raw_input) {
    PhoneValidationResult res;
    const CountryRule* rule = find_by_iso2(iso2);
    if (!rule) {
        res.error_message = "Unsupported country code";
        return res;
    }

    std::string digits;
    for (unsigned char c : raw_input) {
        if (std::isdigit(c)) {
            digits.push_back(static_cast<char>(c));
        } else if (c != ' ' && c != '-' && c != '(' && c != ')' && c != '+') {
            res.error_message = "Phone number contains invalid characters";
            return res;
        }
    }

    if (digits.empty()) {
        res.error_message = "Enter your phone number";
        return res;
    }

    if (digits.size() > static_cast<size_t>(rule->max_national_digits) &&
        digits.compare(0, rule->dial_code.size(), rule->dial_code) == 0) {
        digits = digits.substr(rule->dial_code.size());
    }

    if (rule->strip_leading_zero && !digits.empty() && digits[0] == '0') {
        digits = digits.substr(1);
    }

    if (static_cast<int>(digits.size()) < rule->min_national_digits ||
        static_cast<int>(digits.size()) > rule->max_national_digits) {
        if (rule->min_national_digits == rule->max_national_digits) {
            res.error_message = rule->name + " phone must be " +
                std::to_string(rule->min_national_digits) + " digits";
        } else {
            res.error_message = rule->name + " phone must be " +
                std::to_string(rule->min_national_digits) + "-" +
                std::to_string(rule->max_national_digits) + " digits";
        }
        return res;
    }

    if (!rule->allowed_first_digits.empty() &&
        rule->allowed_first_digits.find(digits[0]) == std::string::npos) {
        res.error_message = "Invalid phone prefix for " + rule->name;
        return res;
    }

    res.valid = true;
    res.national_digits = digits;
    res.normalized_e164 = "+" + rule->dial_code + digits;
    return res;
}

}  // namespace telefam::countries
