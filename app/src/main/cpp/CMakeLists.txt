# Sets the minimum version of CMake required to build your native library.
# This ensures that a certain set of CMake features is available to
# your build.

cmake_minimum_required(VERSION 3.4.1)

# build app's shared lib

# Specifies a library name, specifies whether the library is STATIC or
# SHARED, and provides relative paths to the source code. You can
# define multiple libraries by adding multiple add_library() commands,
# and CMake builds them for you. When you build your app, Gradle
# automatically packages shared libraries with your APK.

add_library(byb-lib SHARED byb-lib.cpp drawing.cpp processing.cpp)
add_library (log4a-lib SHARED log4a-lib.cpp AsyncFileFlush.cpp FlushBuffer.cpp LogBuffer.cpp LogBufferHeader.cpp)
include_directories(includes/)

find_library(log-lib log)

target_link_libraries(byb-lib android log)
target_link_libraries(log4a-lib ${log-lib})
