/* C source files to proxy GLFW calls */

#include <stdbool.h>

#include "hephaestus_platform_GLFW.h"

JNIEXPORT jboolean JNICALL Java_hephaestus_platform_GLFW_init
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)))
{
  return true;
}

JNIEXPORT jintArray JNICALL Java_hephaestus_platform_GLFW_version
(JNIEnv* env, jobject instance __attribute__((unused))) {
  jintArray result;
  result = (*env)->NewIntArray(env, 3);
  return result;
}


JNIEXPORT void JNICALL Java_hephaestus_platform_GLFW_terminate
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused))) {
}


JNIEXPORT jlong JNICALL Java_hephaestus_platform_GLFW_createWindow
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jint x_size __attribute__((unused)))
{
  return 0;
}

JNIEXPORT jlong JNICALL Java_hephaestus_platform_GLFW_destroyWindow
(JNIEnv* env __attribute__((unused)),  jobject instance __attribute__((unused)), jlong window __attribute__((unused)))
{
  return 0;
}

