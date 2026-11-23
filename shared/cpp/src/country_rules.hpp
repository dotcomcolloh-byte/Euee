#pragma once

#include <string>
#include <vector>

namespace telefam::countries {

struct CountryRule {
    std::string iso2;
    std::string name;
    std::string dial_code;   // digits without +, e.g. "254"
    int min_national_digits;
    int max_national_digits;
    bool strip_leading_zero;
    std::string allowed_first_digits;
    std::string placeholder;
};

struct PhoneValidationResult {
    bool valid = false;
    std::string normalized_e164;
    std::string national_digits;
    std::string error_message;
};

const std::vector<CountryRule>& all_countries();
const CountryRule* find_by_iso2(const std::string& iso2);
PhoneValidationResult validate_phone(const std::string& iso2, const std::string& raw_input);

}  // namespace telefam::countries
