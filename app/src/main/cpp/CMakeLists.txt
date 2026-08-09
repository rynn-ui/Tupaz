cmake_minimum_required(VERSION 3.22.1)

project("tupaz" CXX)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -ffast-math -fopenmp")

# ---- NCNN Android Vulkan SDK ----
# Set ncnn_DIR based on current ABI (arm64-v8a / x86_64)
set(ncnn_DIR "${CMAKE_CURRENT_SOURCE_DIR}/ncnn-android-vulkan/${ANDROID_ABI}/lib/cmake/ncnn")
find_package(ncnn REQUIRED)
message(STATUS "Tupaz: Found NCNN ${NCNN_VERSION} for ABI=${ANDROID_ABI}")

find_library(
    log-lib
    log
)

add_library(
    tupaz
    SHARED
    ncnn_bridge.cpp
    gpu_benchmark.cpp
    tile_merge.cpp
    artifact_classifier.cpp
    frame_quality_scorer.cpp
    rife_flow.cpp
    occlusion_mask.cpp
    face_merge.cpp
)

target_include_directories(
    tupaz
    PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

target_link_libraries(
    tupaz
    ncnn
    ${log-lib}
)
