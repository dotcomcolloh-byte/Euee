#pragma once

#include <cstdint>
#include <map>
#include <string>
#include <vector>

namespace telefam::json {

enum class Type {
    Null,
    Bool,
    Number,
    String,
    Array,
    Object
};

struct Value {
    Type type = Type::Null;
    bool bool_val = false;
    double num_val = 0.0;
    int64_t int_val = 0;
    bool is_int = false;
    std::string str_val;
    std::vector<Value> arr_val;
    std::map<std::string, Value> obj_val;

    static Value make_null() { return Value{}; }
    static Value make_bool(bool b) { Value v; v.type = Type::Bool; v.bool_val = b; return v; }
    static Value make_int(int64_t i) { Value v; v.type = Type::Number; v.int_val = i; v.num_val = static_cast<double>(i); v.is_int = true; return v; }
    static Value make_double(double d) { Value v; v.type = Type::Number; v.num_val = d; v.is_int = false; return v; }
    static Value make_string(std::string s) { Value v; v.type = Type::String; v.str_val = std::move(s); return v; }
    static Value make_array() { Value v; v.type = Type::Array; return v; }
    static Value make_object() { Value v; v.type = Type::Object; return v; }

    bool is_object() const { return type == Type::Object; }
    bool is_array() const { return type == Type::Array; }
    bool is_string() const { return type == Type::String; }
    bool is_bool() const { return type == Type::Bool; }
    bool is_number() const { return type == Type::Number; }

    const Value* get(const std::string& key) const;
    std::string get_string(const std::string& key, const std::string& def = "") const;
    bool get_bool(const std::string& key, bool def = false) const;
    int64_t get_int(const std::string& key, int64_t def = 0) const;
    double get_double(const std::string& key, double def = 0.0) const;
};

bool parse(const std::string& input, Value& out, int max_depth = 32);
std::string stringify(const Value& val);

}  // namespace telefam::json
