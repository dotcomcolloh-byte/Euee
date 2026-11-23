#include "json_codec.hpp"

#include <cctype>
#include <cstdio>
#include <cstdlib>
#include <sstream>

namespace telefam::json {

const Value* Value::get(const std::string& key) const {
    if (type != Type::Object) return nullptr;
    auto it = obj_val.find(key);
    return it == obj_val.end() ? nullptr : &it->second;
}

std::string Value::get_string(const std::string& key, const std::string& def) const {
    const Value* v = get(key);
    return (v && v->type == Type::String) ? v->str_val : def;
}

bool Value::get_bool(const std::string& key, bool def) const {
    const Value* v = get(key);
    return (v && v->type == Type::Bool) ? v->bool_val : def;
}

int64_t Value::get_int(const std::string& key, int64_t def) const {
    const Value* v = get(key);
    if (!v || v->type != Type::Number) return def;
    return v->is_int ? v->int_val : static_cast<int64_t>(v->num_val);
}

double Value::get_double(const std::string& key, double def) const {
    const Value* v = get(key);
    if (!v || v->type != Type::Number) return def;
    return v->num_val;
}

namespace {

class Parser {
public:
    explicit Parser(const std::string& src, int max_depth)
        : src_(src), pos_(0), max_depth_(max_depth) {}

    bool parse_root(Value& out) {
        skip_ws();
        if (!parse_value(out, 0)) return false;
        skip_ws();
        return pos_ == src_.size();
    }

private:
    const std::string& src_;
    size_t pos_;
    int max_depth_;

    void skip_ws() {
        while (pos_ < src_.size()) {
            char c = src_[pos_];
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                ++pos_;
            } else {
                break;
            }
        }
    }

    bool parse_value(Value& out, int depth) {
        if (depth > max_depth_) return false;
        skip_ws();
        if (pos_ >= src_.size()) return false;
        char c = src_[pos_];
        if (c == '{') return parse_object(out, depth);
        if (c == '[') return parse_array(out, depth);
        if (c == '"') {
            std::string s;
            if (!parse_string(s)) return false;
            out = Value::make_string(std::move(s));
            return true;
        }
        if (c == 't' && src_.compare(pos_, 4, "true") == 0) {
            pos_ += 4;
            out = Value::make_bool(true);
            return true;
        }
        if (c == 'f' && src_.compare(pos_, 5, "false") == 0) {
            pos_ += 5;
            out = Value::make_bool(false);
            return true;
        }
        if (c == 'n' && src_.compare(pos_, 4, "null") == 0) {
            pos_ += 4;
            out = Value::make_null();
            return true;
        }
        if (c == '-' || std::isdigit(static_cast<unsigned char>(c))) {
            return parse_number(out);
        }
        return false;
    }

    bool parse_string(std::string& out) {
        if (pos_ >= src_.size() || src_[pos_] != '"') return false;
        ++pos_;
        out.clear();
        while (pos_ < src_.size()) {
            char c = src_[pos_++];
            if (c == '"') return true;
            if (static_cast<unsigned char>(c) < 0x20) return false;
            if (c == '\\') {
                if (pos_ >= src_.size()) return false;
                char esc = src_[pos_++];
                switch (esc) {
                    case '"': out.push_back('"'); break;
                    case '\\': out.push_back('\\'); break;
                    case '/': out.push_back('/'); break;
                    case 'b': out.push_back('\b'); break;
                    case 'f': out.push_back('\f'); break;
                    case 'n': out.push_back('\n'); break;
                    case 'r': out.push_back('\r'); break;
                    case 't': out.push_back('\t'); break;
                    case 'u': {
                        if (pos_ + 4 > src_.size()) return false;
                        uint32_t cp = 0;
                        for (int i = 0; i < 4; ++i) {
                            char h = src_[pos_++];
                            cp <<= 4;
                            if (h >= '0' && h <= '9') cp |= (h - '0');
                            else if (h >= 'a' && h <= 'f') cp |= (h - 'a' + 10);
                            else if (h >= 'A' && h <= 'F') cp |= (h - 'A' + 10);
                            else return false;
                        }
                        if (cp < 0x80) {
                            out.push_back(static_cast<char>(cp));
                        } else if (cp < 0x800) {
                            out.push_back(static_cast<char>(0xC0 | (cp >> 6)));
                            out.push_back(static_cast<char>(0x80 | (cp & 0x3F)));
                        } else {
                            out.push_back(static_cast<char>(0xE0 | (cp >> 12)));
                            out.push_back(static_cast<char>(0x80 | ((cp >> 6) & 0x3F)));
                            out.push_back(static_cast<char>(0x80 | (cp & 0x3F)));
                        }
                        break;
                    }
                    default: return false;
                }
            } else {
                out.push_back(c);
            }
        }
        return false;
    }

    bool parse_number(Value& out) {
        size_t start = pos_;
        bool is_float = false;
        if (src_[pos_] == '-') ++pos_;
        if (pos_ >= src_.size() || !std::isdigit(static_cast<unsigned char>(src_[pos_]))) return false;
        while (pos_ < src_.size() && std::isdigit(static_cast<unsigned char>(src_[pos_]))) ++pos_;
        if (pos_ < src_.size() && src_[pos_] == '.') {
            is_float = true;
            ++pos_;
            if (pos_ >= src_.size() || !std::isdigit(static_cast<unsigned char>(src_[pos_]))) return false;
            while (pos_ < src_.size() && std::isdigit(static_cast<unsigned char>(src_[pos_]))) ++pos_;
        }
        if (pos_ < src_.size() && (src_[pos_] == 'e' || src_[pos_] == 'E')) {
            is_float = true;
            ++pos_;
            if (pos_ < src_.size() && (src_[pos_] == '+' || src_[pos_] == '-')) ++pos_;
            if (pos_ >= src_.size() || !std::isdigit(static_cast<unsigned char>(src_[pos_]))) return false;
            while (pos_ < src_.size() && std::isdigit(static_cast<unsigned char>(src_[pos_]))) ++pos_;
        }
        std::string token = src_.substr(start, pos_ - start);
        if (!is_float) {
            char* endp = nullptr;
            long long val = std::strtoll(token.c_str(), &endp, 10);
            if (endp == token.c_str() + token.size()) {
                out = Value::make_int(static_cast<int64_t>(val));
                return true;
            }
        }
        char* endp = nullptr;
        double dval = std::strtod(token.c_str(), &endp);
        if (endp != token.c_str() + token.size()) return false;
        out = Value::make_double(dval);
        return true;
    }

    bool parse_array(Value& out, int depth) {
        ++pos_;
        out = Value::make_array();
        skip_ws();
        if (pos_ < src_.size() && src_[pos_] == ']') {
            ++pos_;
            return true;
        }
        while (pos_ < src_.size()) {
            Value elem;
            if (!parse_value(elem, depth + 1)) return false;
            out.arr_val.push_back(std::move(elem));
            skip_ws();
            if (pos_ >= src_.size()) return false;
            if (src_[pos_] == ']') {
                ++pos_;
                return true;
            }
            if (src_[pos_] != ',') return false;
            ++pos_;
        }
        return false;
    }

    bool parse_object(Value& out, int depth) {
        ++pos_;
        out = Value::make_object();
        skip_ws();
        if (pos_ < src_.size() && src_[pos_] == '}') {
            ++pos_;
            return true;
        }
        while (pos_ < src_.size()) {
            skip_ws();
            std::string key;
            if (!parse_string(key)) return false;
            skip_ws();
            if (pos_ >= src_.size() || src_[pos_] != ':') return false;
            ++pos_;
            Value val;
            if (!parse_value(val, depth + 1)) return false;
            out.obj_val[std::move(key)] = std::move(val);
            skip_ws();
            if (pos_ >= src_.size()) return false;
            if (src_[pos_] == '}') {
                ++pos_;
                return true;
            }
            if (src_[pos_] != ',') return false;
            ++pos_;
        }
        return false;
    }
};

void escape_string(std::string& dst, const std::string& src) {
    dst.push_back('"');
    for (unsigned char c : src) {
        switch (c) {
            case '"': dst += "\\\""; break;
            case '\\': dst += "\\\\"; break;
            case '\b': dst += "\\b"; break;
            case '\f': dst += "\\f"; break;
            case '\n': dst += "\\n"; break;
            case '\r': dst += "\\r"; break;
            case '\t': dst += "\\t"; break;
            default:
                if (c < 0x20) {
                    char buf[7];
                    std::snprintf(buf, sizeof(buf), "\\u%04x", c);
                    dst += buf;
                } else {
                    dst.push_back(static_cast<char>(c));
                }
                break;
        }
    }
    dst.push_back('"');
}

void write_value(std::string& dst, const Value& v) {
    switch (v.type) {
        case Type::Null:
            dst += "null";
            break;
        case Type::Bool:
            dst += (v.bool_val ? "true" : "false");
            break;
        case Type::Number:
            if (v.is_int) {
                dst += std::to_string(v.int_val);
            } else {
                std::ostringstream oss;
                oss << v.num_val;
                dst += oss.str();
            }
            break;
        case Type::String:
            escape_string(dst, v.str_val);
            break;
        case Type::Array: {
            dst.push_back('[');
            for (size_t i = 0; i < v.arr_val.size(); ++i) {
                if (i > 0) dst.push_back(',');
                write_value(dst, v.arr_val[i]);
            }
            dst.push_back(']');
            break;
        }
        case Type::Object: {
            dst.push_back('{');
            bool first = true;
            for (const auto& kv : v.obj_val) {
                if (!first) dst.push_back(',');
                first = false;
                escape_string(dst, kv.first);
                dst.push_back(':');
                write_value(dst, kv.second);
            }
            dst.push_back('}');
            break;
        }
    }
}

}  // namespace

bool parse(const std::string& input, Value& out, int max_depth) {
    if (input.size() > 2 * 1024 * 1024) return false;
    Parser p(input, max_depth);
    return p.parse_root(out);
}

std::string stringify(const Value& val) {
    std::string out;
    out.reserve(256);
    write_value(out, val);
    return out;
}

}  // namespace telefam::json
