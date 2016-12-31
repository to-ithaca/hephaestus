################################################################
# A minimal CMake file that is compatible with sbt-jni         #
#                                                              #
# All settings required by sbt-jni have been marked so, please #
# add/modify/remove settings to build your specific library.   #
################################################################

cmake_minimum_required(VERSION 2.8.0)

# Define project and related variables
# (required by sbt-jni) please use semantic versioning
#
project (hephaestus)
set(PROJECT_VERSION_MAJOR 0)
set(PROJECT_VERSION_MINOR 1)
set(PROJECT_VERSION_PATCH 0)

set(CMAKE_INSTALL_RPATH_USE_LINK_PATH TRUE)
set(CMAKE_C_FLAGS "-std=c99")
add_definitions(-Wall)
add_definitions(-Wextra)
#add_definitions(-pedantic)

# Setup JNI
find_package(JNI REQUIRED)
if (JNI_FOUND)
    message (STATUS "JNI include directories: ${JNI_INCLUDE_DIRS}")
endif()

# Setup GLFW
find_package(glfw3 3.2 REQUIRED)

# Include directories
include_directories(./target/include)
include_directories(${JNI_INCLUDE_DIRS})
include_directories("$ENV{VULKAN_SDK}/include")
link_directories("$ENV{VULKAN_SDK}/lib") 
link_directories("$ENV{VULKAN_SDK}/etc/explicit_layer.d")

# Sources
file(GLOB LIB_SRC
  "./src/*.c"
)

# Setup installation targets
# (required by sbt-jni) major version should always be appended to library name
#
set (LIB_NAME ${PROJECT_NAME}${PROJECT_VERSION_MAJOR})
add_library(${LIB_NAME} SHARED ${LIB_SRC})
target_link_libraries(${LIB_NAME} glfw)
target_link_libraries(${LIB_NAME} vulkan)
install(TARGETS ${LIB_NAME} LIBRARY DESTINATION .)
