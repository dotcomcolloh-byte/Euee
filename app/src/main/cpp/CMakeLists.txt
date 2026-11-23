cmake_minimum_required(VERSION 3.22.1)
project(telefam_core LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

set(TELEFAM_SHARED_CPP "${CMAKE_CURRENT_SOURCE_DIR}/../../../../shared/cpp")

add_library(
    telefam_core
    SHARED
    "${TELEFAM_SHARED_CPP}/src/string_vault.hpp"
    "${TELEFAM_SHARED_CPP}/src/crypto_engine.hpp"
    "${TELEFAM_SHARED_CPP}/src/crypto_engine.cpp"
    "${TELEFAM_SHARED_CPP}/src/anti_tamper.hpp"
    "${TELEFAM_SHARED_CPP}/src/anti_tamper.cpp"
    "${TELEFAM_SHARED_CPP}/src/json_codec.hpp"
    "${TELEFAM_SHARED_CPP}/src/json_codec.cpp"
    "${TELEFAM_SHARED_CPP}/src/country_rules.hpp"
    "${TELEFAM_SHARED_CPP}/src/country_rules.cpp"
    "${TELEFAM_SHARED_CPP}/src/telefam_core_engine.hpp"
    "${TELEFAM_SHARED_CPP}/src/telefam_core_engine.cpp"
    "${TELEFAM_SHARED_CPP}/src/jni_bridge.cpp"
)

target_include_directories(telefam_core PRIVATE "${TELEFAM_SHARED_CPP}/src")

target_compile_options(telefam_core PRIVATE
    -Wall
    -Wextra
    -Wno-unused-parameter
    -fvisibility=hidden
    -ffunction-sections
    -fdata-sections
    -fstack-protector-strong
    -fno-rtti
)

target_link_options(telefam_core PRIVATE
    -Wl,--gc-sections
    -Wl,-z,relro,-z,now
    -Wl,--exclude-libs,ALL
)

find_library(log-lib log)
target_link_libraries(telefam_core PRIVATE ${log-lib})
