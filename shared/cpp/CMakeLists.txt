cmake_minimum_required(VERSION 3.22.1)
project(telefam_shared_core LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

option(TELEFAM_BUILD_DESKTOP_JNI "Build Desktop JNI shared library (Windows/macOS/Linux)" ON)
option(TELEFAM_BUILD_C_API_STATIC "Build static C ABI library (Apple iOS/iPadOS & C consumers)" OFF)

set(TELEFAM_CORE_SOURCES
    src/string_vault.hpp
    src/crypto_engine.hpp
    src/crypto_engine.cpp
    src/anti_tamper.hpp
    src/anti_tamper.cpp
    src/json_codec.hpp
    src/json_codec.cpp
    src/country_rules.hpp
    src/country_rules.cpp
    src/telefam_core_engine.hpp
    src/telefam_core_engine.cpp
)

if(TELEFAM_BUILD_DESKTOP_JNI)
    find_package(JNI REQUIRED)
    add_library(telefam_core SHARED
        ${TELEFAM_CORE_SOURCES}
        src/jni_desktop_bridge.cpp
    )
    target_include_directories(telefam_core PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/src
        ${CMAKE_CURRENT_SOURCE_DIR}/include
        ${JNI_INCLUDE_DIRS}
    )
    set_target_properties(telefam_core PROPERTIES
        CXX_VISIBILITY_PRESET hidden
        VISIBILITY_INLINES_HIDDEN ON
    )
    if(MSVC)
        target_compile_options(telefam_core PRIVATE /W4 /GS)
    elseif(APPLE)
        target_compile_options(telefam_core PRIVATE -Wall -Wextra -Wno-unused-parameter -fvisibility=hidden -fstack-protector-strong -fPIC)
    else()
        target_compile_options(telefam_core PRIVATE -Wall -Wextra -Wno-unused-parameter -fvisibility=hidden -ffunction-sections -fdata-sections -fstack-protector-strong -fPIC)
        target_link_options(telefam_core PRIVATE -Wl,--gc-sections -Wl,-z,relro,-z,now)
    endif()
endif()

if(TELEFAM_BUILD_C_API_STATIC)
    add_library(telefam_core_c STATIC
        ${TELEFAM_CORE_SOURCES}
        include/telefam_c_api.h
        src/telefam_c_api.cpp
    )
    target_include_directories(telefam_core_c PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/include
    )
    target_include_directories(telefam_core_c PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/src
    )
    if(MSVC)
        target_compile_options(telefam_core_c PRIVATE /W4 /GS)
    else()
        target_compile_options(telefam_core_c PRIVATE -Wall -Wextra -Wno-unused-parameter -fstack-protector-strong -fPIC)
    endif()
endif()
