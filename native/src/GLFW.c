/* C source files to proxy GLFW calls */
#include <stdlib.h>

#include <GLFW/glfw3.h>

#include "hephaestus_platform_GLFW.h"

JNIEXPORT jboolean JNICALL Java_hephaestus_platform_GLFW_init
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)))
{
  return glfwInit();
}

JNIEXPORT jintArray JNICALL Java_hephaestus_platform_GLFW_version
(JNIEnv* env, jobject instance __attribute__((unused))) {

  int* version = malloc(sizeof(int) * 3);

  glfwGetVersion(version, version + 1, version + 3);

  jintArray result;
  jint fill[3];

  fill[0] = version[0];
  fill[1] = version[1];
  fill[2] = version[2];

  free(version);

  result = (*env)->NewIntArray(env, 3);
  (*env)->SetIntArrayRegion(env, result, 0, 3, fill);
  return result;
}


JNIEXPORT void JNICALL Java_hephaestus_platform_GLFW_terminate
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused))) {
  glfwTerminate();
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

