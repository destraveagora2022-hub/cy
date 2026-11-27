cmake_minimum_required(VERSION 3.22.1)
project("cryx_mem")

add_library(cryx_mem SHARED memory.cpp)

target_link_libraries(cryx_mem
    android
    log)
