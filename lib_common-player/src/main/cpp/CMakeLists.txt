cmake_minimum_required(VERSION 3.22.1)

project(ffmpeg_player)

set(FFMPEG_ROOT "${CMAKE_CURRENT_LIST_DIR}/ffmpeg/${ANDROID_ABI}")

add_library(ffmpeg_player SHARED
        ffmpeg_player_jni.cpp)

target_include_directories(ffmpeg_player PRIVATE
        "${FFMPEG_ROOT}/include")

foreach(name avdevice avfilter avformat avcodec swresample swscale avutil)
    add_library(${name} SHARED IMPORTED)
    set_target_properties(${name} PROPERTIES
            IMPORTED_LOCATION "${CMAKE_CURRENT_LIST_DIR}/../jniLibs/${ANDROID_ABI}/lib${name}.so")
endforeach()

find_library(log-lib log)
find_library(android-lib android)

target_link_libraries(ffmpeg_player
        avdevice
        avfilter
        avformat
        avcodec
        swresample
        swscale
        avutil
        ${android-lib}
        ${log-lib})
