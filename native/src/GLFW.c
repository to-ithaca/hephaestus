/* C source files to proxy GLFW calls */
#define GLFW_INCLUDE_VULKAN
#include <stdlib.h>
#include <stdio.h>
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

  glfwGetVersion(version, version + 1, version + 2);

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
(JNIEnv* env, jobject instance __attribute__((unused)), jint x_size, jint y_size, jstring title_str)
{
  const char* title = (*env)->GetStringUTFChars(env, title_str, 0);
  GLFWwindow* window = glfwCreateWindow(x_size, y_size, title, NULL, NULL);
  (*env)->ReleaseStringUTFChars(env, title_str, title);
  return (long) window;
}

JNIEXPORT void JNICALL Java_hephaestus_platform_GLFW_destroyWindow
(JNIEnv* env __attribute__((unused)),  jobject instance __attribute__((unused)), jlong window)
{
  glfwDestroyWindow((GLFWwindow*) window);
}

JavaVM* g_vm;
jobject g_callback;
jmethodID g_callback_id;

void f_callback(GLFWwindow* window) {
  JNIEnv* env;
  (*g_vm)->GetEnv(g_vm, (void**) &env, JNI_VERSION_1_6);
  (*g_vm)->AttachCurrentThread(g_vm, (void**)&env, NULL);
  jclass w_cls = (*env)->FindClass(env, "hephaestus/platform/GLFW$Window");
  jmethodID w_cons_mid = (*env)->GetMethodID(env, w_cls, "<init>", "(J)V");
  jobject w_obj = (*env)->NewObject(env, w_cls, w_cons_mid, window);
  (*env)->CallVoidMethod(env, g_callback, g_callback_id, w_obj);
  (*g_vm)->DetachCurrentThread(g_vm);
}

JNIEXPORT void JNICALL Java_hephaestus_platform_GLFW_setWindowCloseCallback
(JNIEnv* env, jobject instance __attribute__((unused)), 
 jlong window __attribute__((unused)), jobject callback)
{
  (*env)->GetJavaVM(env, &g_vm);
  g_callback = (*env)->NewGlobalRef(env, callback);
  jclass clazz = (*env)->GetObjectClass(env, callback);
  g_callback_id = (*env)->GetMethodID(env, clazz, "call", "(Ljava/lang/Object;)V");
  glfwSetWindowCloseCallback((GLFWwindow*) window, f_callback);
}

JNIEXPORT void JNICALL Java_hephaestus_platform_GLFW_waitEvents
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused))) {
  glfwWaitEvents();
}

JNIEXPORT jboolean JNICALL Java_hephaestus_platform_GLFW_vulkanSupported
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused))) {
  return glfwVulkanSupported();
}

JNIEXPORT void JNICALL Java_hephaestus_platform_GLFW_windowHint
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)),
jint hint, jint value) {
  glfwWindowHint(hint, value);
}


JNIEXPORT jobjectArray JNICALL Java_hephaestus_platform_GLFW_getRequiredInstanceExtensions
(JNIEnv* env, jobject instance __attribute__((unused))) {
  uint32_t count;
  const char** es = glfwGetRequiredInstanceExtensions(&count);
  jclass cls = (*env)->FindClass(env, "Ljava/lang/String;");
  jstring s0 = (*env)->NewStringUTF(env, es[0]);
  jobjectArray objs = (*env)->NewObjectArray(env, count, cls, s0);
  uint32_t i;
  for(i = 1; i < count; i++){
    jobject obj = (*env)->NewStringUTF(env, es[i]);
    (*env)->SetObjectArrayElement(env, objs, i, obj);
  }
  return objs;
}

JNIEXPORT jlong JNICALL Java_hephaestus_platform_GLFW_createWindowSurface
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)),
jlong inst, jlong window) {
  VkSurfaceKHR s;
  VkResult r = glfwCreateWindowSurface((VkInstance) inst, (GLFWwindow *) window, NULL, &s);
  if(r != VK_SUCCESS) {
    printf("result was unsuccessful %d\n", r);
    fflush(stdout);
  } else {
    printf("surface created successfully");
    fflush(stdout);
  }
  return (jlong) s;
}
