cmake_minimum_required(VERSION 3.4.1)

# Add BYB native library
aux_source_directory(byb/ BYB_LIB_SOURCE)
add_library(byb-lib SHARED ${BYB_LIB_SOURCE})
include_directories(byb/includes/)

target_link_libraries(byb-lib android log)

# Add Log4a native library
aux_source_directory(log4a/ LOG4A_LIB_SOURCE)
add_library (log4a-lib SHARED ${LOG4A_LIB_SOURCE})
include_directories(log4a/includes/)

find_library(log-lib log)
target_link_libraries(log4a-lib ${log-lib})
