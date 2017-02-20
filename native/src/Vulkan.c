/* C source files to proxy Vulkan calls */
#define GLFW_INCLUDE_VULKAN
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <GLFW/glfw3.h>

#include "hephaestus_platform_Vulkan.h"

#define HEPH "hephaestus/platform/Vulkan$"

#define CHECK(v, m) if(v == NULL) { \
    fprintf(stderr, "%s was null!\n", m); \
    fflush(stderr); \
}

#define MARK(m) \
    fprintf(stdout, "reached mark %s\n", m); \
    fflush(stdout); \

#define RES(r) if(r != VK_SUCCESS) { \
    fprintf(stderr, "vulkan failed with %d\n", r); \
    fflush(stderr); \
}

#define ITER(env, objs, count, tpe, f) \
({ \
  tpe* vs = malloc(count * sizeof(tpe)); \
  uint32_t _i; \
  for(_i = 0; _i < count; _i++) { \
    jobject _o = (*env)->GetObjectArrayElement(env, objs, _i); \
    vs[_i] = f(env, _o); \
  } \
  vs; \
});

#define getInt(env, cls, o, name) \
({ \
  jmethodID id = (*env)->GetMethodID(env, cls, name, "()I"); \
  (*env)->CallIntMethod(env, o, id);                         \
})

#define getFloat(env, cls, o, name) \
({ \
  jmethodID id = (*env)->GetMethodID(env, cls, name, "()F"); \
  (*env)->CallFloatMethod(env, o, id);                       \
})

#define getLong(env, cls, o, name) \
({ \
  jmethodID id = (*env)->GetMethodID(env, cls, name, "()J"); \
  (*env)->CallLongMethod(env, o, id);                       \
})

#define getBoolean(env, cls, o, name) \
({ \
  jmethodID id = (*env)->GetMethodID(env, cls, name, "()Z"); \
  (*env)->CallBooleanMethod(env, o, id);                     \
})

#define getObject(env, cls, o, name, target_cls, f) \
({ \
  jmethodID id = (*env)->GetMethodID(env, cls, name, "()L" target_cls ";"); \
  jobject _o = (*env)->CallObjectMethod(env, o, id); \
  f(env, _o); \
})

#define getFloatArray(env, cls, o, name, target) \
  jmethodID target ## _id = (*env)->GetMethodID(env, cls, name, "()[F"); \
  jfloatArray target ## _array = (jfloatArray) (*env)->CallObjectMethod(env, o, target ## _id); \
  float* target ## s = (*env)->GetFloatArrayElements(env, target ## _array , 0); \
  int target ## _count = (*env)->GetArrayLength(env, target ## _array); \

#define getIntArray(env, cls, o, name, target) \
  jmethodID target ## _id = (*env)->GetMethodID(env, cls, name, "()[I"); \
  jintArray target ## _array = (jintArray) (*env)->CallObjectMethod(env, o, target ## _id); \
  int* target ## s = (*env)->GetIntArrayElements(env, target ## _array, 0); \
  int target ## _count = (*env)->GetArrayLength(env, target ## _array); \

#define getObjectArray(env, cls, o, name, target_cls, target, tpe, f) \
  jmethodID target ## _id = (*env)->GetMethodID(env, cls, name, "()[L" target_cls ";"); \
  jobjectArray target ## _array = (*env)->CallObjectMethod(env, o, target ## _id); \
  jsize target ## _count = (*env)->GetArrayLength(env, target ## _array); \
  tpe* target ## s = ITER(env, target ## _array, (uint32_t) target ## _count, tpe, f); \ 

VkApplicationInfo applicationInfo(JNIEnv* env, jobject info) {
  jclass cls = (*env)->GetObjectClass(env, info);
  jmethodID pan_id = (*env)->GetMethodID(env, cls, "applicationName", "()Ljava/lang/String;");
  jstring pan_s = (*env)->CallObjectMethod(env, info, pan_id);
  const char* pan = (*env)->GetStringUTFChars(env, pan_s, 0);
  int av = getInt(env, cls, info, "applicationVersion");
  jmethodID pen_id = (*env)->GetMethodID(env, cls, "engineName", "()Ljava/lang/String;");
  jstring pen_s = (*env)->CallObjectMethod(env, info, pen_id);
  const char* pen = (*env)->GetStringUTFChars(env, pen_s, 0);
  int ev = getInt(env, cls, info, "engineVersion");

  VkApplicationInfo v_info = {
    .sType = VK_STRUCTURE_TYPE_APPLICATION_INFO,
    .pNext = NULL,
    .pApplicationName = pan,
    .applicationVersion = av,
    .pEngineName = pen,
    .engineVersion = ev,
    .apiVersion = VK_API_VERSION_1_0
  };
  /* (*env)->ReleaseStringUTFChars(env, pan_s, pan); */
  /* (*env)->ReleaseStringUTFChars(env, pen_s, pen); */
  return v_info;
}

void stringEls(JNIEnv* env, jobjectArray ss, const char** c_ss, uint32_t count) {
  uint32_t i;
  for(i = 0; i < count; i++) {
    jstring s = (jstring)(*env)->GetObjectArrayElement(env, ss, i);
    const char* c_s = (*env)->GetStringUTFChars(env, s, 0);
    c_ss[i] = c_s;
  }
}

VkInstanceCreateInfo instanceCreateInfo(JNIEnv* env, jobject info) {
  jclass cls = (*env)->GetObjectClass(env, info);
  jmethodID pai_id = (*env)->GetMethodID(env, cls, "applicationInfo", 
                                         "()Lhephaestus/platform/Vulkan$ApplicationInfo;");
  jobject pai_obj = (*env)->CallObjectMethod(env, info, pai_id);
  VkApplicationInfo pai = applicationInfo(env, pai_obj);
  VkApplicationInfo* pai_ptr = malloc(sizeof(VkApplicationInfo));
  *pai_ptr = pai;
  jmethodID een_id = (*env)->GetMethodID(env, cls, "enabledExtensionNames", "()[Ljava/lang/String;");
  jobjectArray een_objs = (*env)->CallObjectMethod(env, info, een_id);
  int eec = (*env)->GetArrayLength(env, een_objs);
  const char** eens = malloc(eec * sizeof(char*));
  stringEls(env, een_objs, eens, eec);
  jmethodID eln_id = (*env)->GetMethodID(env, cls, "enabledLayerNames", "()[Ljava/lang/String;");
  jobjectArray eln_objs = (*env)->CallObjectMethod(env, info, eln_id);
  int elc = (*env)->GetArrayLength(env, eln_objs);
  const char** elns = malloc(elc * sizeof(char*));
  stringEls(env, eln_objs, elns, elc);
  VkInstanceCreateInfo v_info = {
    .sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO,
    .pNext = NULL,
    .flags = 0,
    .pApplicationInfo = pai_ptr,
    .enabledExtensionCount = eec,
    .ppEnabledExtensionNames = eens,
    .enabledLayerCount = elc,
    .ppEnabledLayerNames = elns
  };
  return v_info;
}

JNIEXPORT jlong JNICALL Java_hephaestus_platform_Vulkan_createInstance
(JNIEnv* env, jobject instance __attribute__((unused)),
jobject info) {
  VkInstanceCreateInfo v_info = instanceCreateInfo(env, info);
  VkInstance inst;
  RES(vkCreateInstance(&v_info, NULL, &inst));
  return (long) inst;
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_destroyInstance
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong inst) {
  vkDestroyInstance((VkInstance) inst, NULL);
}

jobject physicalDevice(JNIEnv* env, VkPhysicalDevice d) {
  jclass cls = (*env)->FindClass(env, "hephaestus/platform/Vulkan$PhysicalDevice");
  jmethodID cons_mid = (*env)->GetMethodID(env, cls, "<init>", "(J)V");
  jobject obj = (*env)->NewObject(env, cls, cons_mid, (long) d);
  return obj;
}

JNIEXPORT jlongArray JNICALL Java_hephaestus_platform_Vulkan_enumeratePhysicalDevices
(JNIEnv* env, jobject instance __attribute__((unused)), jlong inst) {
  
    uint32_t gpu_count = 0;
    RES(vkEnumeratePhysicalDevices((VkInstance) inst, &gpu_count, NULL));
    VkPhysicalDevice gpus[gpu_count];
    RES(vkEnumeratePhysicalDevices((VkInstance) inst, &gpu_count, gpus));
    jclass cls = (*env)->FindClass(env, "hephaestus/platform/Vulkan$PhysicalDevice");
    jobject first_obj = physicalDevice(env, gpus[0]);
    jobjectArray objs = (*env)->NewObjectArray(env, gpu_count, cls, first_obj);
    uint32_t i;
    for(i = 1; i < gpu_count; i++){
        VkPhysicalDevice gpu = gpus[i];
        jobject obj = physicalDevice(env, gpu);
        (*env)->SetObjectArrayElement(env, objs, i, obj);
    }
    return objs;
} 

jobject fromMemoryType(JNIEnv* env, VkMemoryType t) {
  jclass cls = (*env)->FindClass(env, "hephaestus/platform/Vulkan$MemoryType");
  jmethodID cons_mid = (*env)->GetMethodID(env, cls, "<init>", "(II)V");
  jobject obj = (*env)->NewObject(env, cls, cons_mid, t.propertyFlags, t.heapIndex);
  return obj;
}

jobjectArray fromMemoryTypes(JNIEnv* env, VkMemoryType* ts, uint32_t count) {
  jclass cls = (*env)->FindClass(env, "hephaestus/platform/Vulkan$MemoryType");
  jobject obj0 = fromMemoryType(env, ts[0]);

  jobjectArray objs = (*env)->NewObjectArray(env, count, cls, obj0);
  uint32_t i;
  for(i = 1; i < count; i++){
    VkMemoryType t = ts[i];
    jobject obj = fromMemoryType(env, t);
    (*env)->SetObjectArrayElement(env, objs, i, obj);
  }
  return objs;
}

jobject fromMemoryHeap(JNIEnv* env, VkMemoryHeap h) {
  jclass cls = (*env)->FindClass(env, "hephaestus/platform/Vulkan$MemoryHeap");
  jmethodID cons_mid = (*env)->GetMethodID(env, cls, "<init>", "(JI)V");
  jobject obj = (*env)->NewObject(env, cls, cons_mid, h.size, h.flags);
  return obj;
}

jobjectArray fromMemoryHeaps(JNIEnv* env, VkMemoryHeap* hs, uint32_t count) {
  jclass cls = (*env)->FindClass(env, "hephaestus/platform/Vulkan$MemoryHeap");
  jobject obj0 = fromMemoryHeap(env, hs[0]);
  jobjectArray objs = (*env)->NewObjectArray(env, count, cls, obj0);
  uint32_t i;
  for(i = 1; i < count; i++){
    VkMemoryHeap h = hs[i];
    jobject obj = fromMemoryHeap(env, h);
    (*env)->SetObjectArrayElement(env, objs, i, obj);
  }
  return objs;
}

JNIEXPORT jobject JNICALL Java_hephaestus_platform_Vulkan_getPhysicalDeviceMemoryProperties
(JNIEnv* env, jobject instance __attribute((unused)), jlong device) {
  VkPhysicalDeviceMemoryProperties ps;
  vkGetPhysicalDeviceMemoryProperties((VkPhysicalDevice) device, &ps);
  jobjectArray ts = fromMemoryTypes(env, (VkMemoryType *) &ps.memoryTypes, ps.memoryTypeCount);
  jobjectArray hs = fromMemoryHeaps(env, (VkMemoryHeap *) &ps.memoryHeaps, ps.memoryHeapCount);
  jclass cls = (*env)->FindClass(env, "hephaestus/platform/Vulkan$PhysicalDeviceMemoryProperties");
  jmethodID cons_mid = (*env)->GetMethodID(env, cls, "<init>",
      "([Lhephaestus/platform/Vulkan$MemoryType;[Lhephaestus/platform/Vulkan$MemoryHeap;)V");
  jobject obj = (*env)->NewObject(env, cls, cons_mid, ts, hs);
  return obj;
}

jobject extent3D(JNIEnv* env, VkExtent3D e) {
  jclass cls = (*env)->FindClass(env, "hephaestus/platform/Vulkan$Extent3D");
  jmethodID cons_mid = (*env)->GetMethodID(env, cls, "<init>", "(III)V");
  jobject obj = (*env)->NewObject(env, cls, cons_mid, e.width, e.height, e.depth);
  return obj;
}

jobject extent2D(JNIEnv* env, VkExtent2D e) {
  jclass cls = (*env)->FindClass(env, "hephaestus/platform/Vulkan$Extent2D");
  jmethodID cons_mid = (*env)->GetMethodID(env, cls, "<init>", "(II)V");
  jobject obj = (*env)->NewObject(env, cls, cons_mid, e.width, e.height);
  return obj;
}

VkExtent3D toExtent3D(JNIEnv* env, jobject e) {
  jclass cls = (*env)->GetObjectClass(env, e);
  int w = getInt(env, cls, e, "width");
  int h = getInt(env, cls, e, "height");
  int d = getInt(env, cls, e, "depth");
  VkExtent3D ext = {
    .width = w,
    .height = h,
    .depth = d
  };
  return ext;
}

VkExtent2D toExtent2D(JNIEnv* env, jobject e) {
  jclass cls = (*env)->GetObjectClass(env, e);
  int w = getInt(env, cls, e, "width");
  int h = getInt(env, cls, e, "height");
  VkExtent2D ext = {
    .width = w,
    .height = h
  };
  return ext;
}

jobject queueFamilyProperties(JNIEnv* env, VkQueueFamilyProperties q) {
  jclass cls = (*env)->FindClass(env, "hephaestus/platform/Vulkan$QueueFamilyProperties");
  jmethodID cons_mid = (*env)->GetMethodID(env, cls, "<init>", "(IIILhephaestus/platform/Vulkan$Extent3D;)V");
  jobject obj = (*env)->NewObject(env, cls, cons_mid, q.queueFlags, q.queueCount, q.timestampValidBits, extent3D(env, q.minImageTransferGranularity));
    return obj;
}

JNIEXPORT jobjectArray JNICALL Java_hephaestus_platform_Vulkan_getPhysicalDeviceQueueFamilyProperties
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong device) {
  uint32_t queue_family_count = 0;
  vkGetPhysicalDeviceQueueFamilyProperties((VkPhysicalDevice) device, &queue_family_count, NULL);
  VkQueueFamilyProperties qfps[queue_family_count];
  vkGetPhysicalDeviceQueueFamilyProperties((VkPhysicalDevice) device, &queue_family_count, qfps);

  jclass cls = (*env)->FindClass(env, "hephaestus/platform/Vulkan$QueueFamilyProperties");
  jobject first_obj = queueFamilyProperties(env, qfps[0]);

  jobjectArray objs = (*env)->NewObjectArray(env, queue_family_count, cls, first_obj);
  uint32_t i;
  for(i = 1; i < queue_family_count; i++){
    VkQueueFamilyProperties qfp = qfps[i];
    jobject obj = queueFamilyProperties(env, qfp);
    (*env)->SetObjectArrayElement(env, objs, i, obj);
  }
  return objs;
}

VkDeviceQueueCreateInfo deviceQueueCreateInfo(JNIEnv* env, jobject info) {
  jclass cls = (*env)->GetObjectClass(env, info);
  int index = getInt(env, cls, info, "queueFamilyIndex");
  getFloatArray(env, cls, info, "queuePriorities", p);
  float* ps_ptr = malloc(p_count * sizeof(float));
  *ps_ptr = *ps;
  VkDeviceQueueCreateInfo v_info = {
    .sType = VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO,
    .pNext = NULL,
    .flags = 0,
    .queueFamilyIndex = index,
    .queueCount = p_count,
    .pQueuePriorities = ps_ptr
  };
  return v_info;
}

VkDeviceCreateInfo deviceCreateInfo(JNIEnv * env, jobject info) {
  jclass cls = (*env)->GetObjectClass(env, info);
  jmethodID extension_names_id = (*env)->GetMethodID(env, cls, "enabledExtensionNames", "()[Ljava/lang/String;");
  jobjectArray extension_names_obj = (*env)->CallObjectMethod(env, info, extension_names_id);
  jsize extension_names_count = (*env)->GetArrayLength(env, extension_names_obj);
  const char** extension_names = malloc(extension_names_count * sizeof(char*));
  stringEls(env, extension_names_obj, extension_names, extension_names_count);
  getObjectArray(env, cls, info, "queueCreateInfos", HEPH "DeviceQueueCreateInfo", queue_info, VkDeviceQueueCreateInfo, deviceQueueCreateInfo);
  VkDeviceCreateInfo v_info = {
    .sType = VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO,
    .pNext = NULL,
    .flags = 0,
    .queueCreateInfoCount = queue_info_count,
    .pQueueCreateInfos = queue_infos,
    .enabledLayerCount = 0,
    .ppEnabledLayerNames = NULL,
    .enabledExtensionCount = extension_names_count,
    .ppEnabledExtensionNames = extension_names,
    .pEnabledFeatures = NULL
  };
  return v_info;
}

JNIEXPORT jlong JNICALL Java_hephaestus_platform_Vulkan_createDevice
(JNIEnv* env, jobject instance __attribute__((unused)), jlong pdevice, jobject info) {
  VkDeviceCreateInfo v_info = deviceCreateInfo(env, info);
  VkDevice device;
  RES(vkCreateDevice((VkPhysicalDevice) pdevice, &v_info, NULL, &device));
  return (jlong) device;
}


JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_destroyDevice
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong d) {
  vkDestroyDevice((VkDevice) d, NULL);
}

VkCommandPoolCreateInfo commandPoolCreateInfo(JNIEnv* env, jobject info) {
  jclass cls = (*env)->GetObjectClass(env, info);
  int f = getInt(env, cls, info, "flags");
  int qfi = getInt(env, cls, info, "queueFamilyIndex");
  VkCommandPoolCreateInfo v_info = {
    .sType = VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO,
    .pNext = NULL,
    .flags = f,
    .queueFamilyIndex = qfi
  };
  return v_info;
}

JNIEXPORT jlong JNICALL Java_hephaestus_platform_Vulkan_createCommandPool
(JNIEnv* env, jobject instance __attribute__((unused)), jlong device, jobject info) {
  VkCommandPoolCreateInfo v_info = commandPoolCreateInfo(env, info);
  VkCommandPool p;
  RES(vkCreateCommandPool((VkDevice) device, &v_info, NULL, &p));
  return (jlong) p;
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_destroyCommandPool
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong device, jlong pool) {
  vkDestroyCommandPool((VkDevice) device, (VkCommandPool) pool, NULL);
}

VkCommandBufferAllocateInfo commandBufferAllocateInfo(JNIEnv* env, jobject info) {
  jclass cls = (*env)->GetObjectClass(env, info);
  jmethodID cp_id = (*env)->GetMethodID(env, cls, "commandPool", "()J");
  jlong cp = (*env)->CallLongMethod(env, info, cp_id);
  jmethodID l_id = (*env)->GetMethodID(env, cls, "level", "()J");
  jlong l = (*env)->CallLongMethod(env, info, l_id);
  int cbc = getInt(env, cls, info, "commandBufferCount");
  VkCommandBufferAllocateInfo v_info = {
    .sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO,
    .pNext = NULL,
    .commandPool = (VkCommandPool) cp,
    .level = l,
    .commandBufferCount = cbc
  };
  return v_info;
}

JNIEXPORT jlong JNICALL Java_hephaestus_platform_Vulkan_allocateCommandBuffers
(JNIEnv* env, jobject instance __attribute__((unused)), jlong device, jobject info) {
  VkCommandBufferAllocateInfo v_info = commandBufferAllocateInfo(env, info);
  VkCommandBuffer cmd;
  RES(vkAllocateCommandBuffers((VkDevice) device, &v_info, &cmd));
  return (long) cmd;
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_freeCommandBuffers
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong device, jlong pool, jint count, jlong buf) {
  vkFreeCommandBuffers((VkDevice) device, (VkCommandPool) pool, count, (VkCommandBuffer *) &buf);
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_destroySurfaceKHR
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong inst, jlong surf) {
  vkDestroySurfaceKHR((VkInstance) inst, (VkSurfaceKHR) surf, NULL);
}

JNIEXPORT jboolean JNICALL Java_hephaestus_platform_Vulkan_getPhysicalDeviceSurfaceSupport
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong device, jint index, jlong surface) {
    uint32_t b;
    RES(vkGetPhysicalDeviceSurfaceSupportKHR((VkPhysicalDevice) device, index, (VkSurfaceKHR) surface, &b));
    return (jboolean) b;
}

jobjectArray surfaceFormats(JNIEnv* env, VkSurfaceFormatKHR* formats, uint32_t count) {
  jclass cls = (*env)->FindClass(env, "hephaestus/platform/Vulkan$SurfaceFormat");
  jmethodID cons_id = (*env)->GetMethodID(env, cls, "<init>", "(II)V");
  jobject obj0 = (*env)->NewObject(env, cls, cons_id, formats[0].format, formats[0].colorSpace);
  jobjectArray objs = (*env)->NewObjectArray(env, count, cls, obj0);
  uint32_t i;
  for(i = 1; i < count; i++){
    jobject obj = (*env)->NewObject(env, cls, cons_id, formats[i].format, formats[i].colorSpace);
    (*env)->SetObjectArrayElement(env, objs, i, obj);
  }
  return objs;
}

JNIEXPORT jobjectArray JNICALL Java_hephaestus_platform_Vulkan_getPhysicalDeviceSurfaceFormats
(JNIEnv* env, jobject instance __attribute__((unused)), jlong device, jlong surface) {
  uint32_t count;
  RES(vkGetPhysicalDeviceSurfaceFormatsKHR((VkPhysicalDevice) device, (VkSurfaceKHR) surface, &count, NULL));
  VkSurfaceFormatKHR formats[count];
  RES(vkGetPhysicalDeviceSurfaceFormatsKHR((VkPhysicalDevice) device, (VkSurfaceKHR) surface, &count, formats));
  return surfaceFormats(env, formats, count);
}

jobject surfaceCapabilities(JNIEnv* env, VkSurfaceCapabilitiesKHR caps) {
  jclass cls = (*env)->FindClass(env, "hephaestus/platform/Vulkan$SurfaceCapabilities");
  jmethodID cons_id = (*env)->GetMethodID(env, cls, "<init>", 
     "(IILhephaestus/platform/Vulkan$Extent2D;Lhephaestus/platform/Vulkan$Extent2D;Lhephaestus/platform/Vulkan$Extent2D;III)V");
  return (*env)->NewObject(env, cls, cons_id, caps.minImageCount, caps.maxImageCount, 
        extent2D(env, caps.currentExtent), extent2D(env, caps.minImageExtent), extent2D(env, caps.maxImageExtent),
        caps.maxImageArrayLayers, caps.supportedTransforms, caps.currentTransform);
}

JNIEXPORT jobject JNICALL Java_hephaestus_platform_Vulkan_getPhysicalDeviceSurfaceCapabilities
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong device, jlong surface) {
  VkSurfaceCapabilitiesKHR caps;
  RES(vkGetPhysicalDeviceSurfaceCapabilitiesKHR((VkPhysicalDevice) device, (VkSurfaceKHR) surface, &caps));
  return surfaceCapabilities(env, caps);
}


JNIEXPORT jintArray JNICALL Java_hephaestus_platform_Vulkan_getPhysicalDeviceSurfacePresentModes
(JNIEnv* env, jobject instance __attribute__((unused)), jlong device, jlong surface) {
  uint32_t count;
  RES(vkGetPhysicalDeviceSurfacePresentModesKHR((VkPhysicalDevice) device, (VkSurfaceKHR) surface, &count, NULL));
  VkPresentModeKHR* modes = (VkPresentModeKHR*) malloc(count * sizeof(VkPresentModeKHR));
  RES(vkGetPhysicalDeviceSurfacePresentModesKHR((VkPhysicalDevice) device, (VkSurfaceKHR) surface, &count, modes));
  jintArray ms = (*env)->NewIntArray(env, count);
  (*env)->SetIntArrayRegion(env, ms, 0, count, (const jint *) modes);
  return ms;
}

VkSwapchainCreateInfoKHR swapchainCreateInfo(JNIEnv* env, jobject info) {
  jclass cls = (*env)->GetObjectClass(env, info);
  int fs = getInt(env, cls, info, "flags");
  jmethodID sfc_id = (*env)->GetMethodID(env, cls, "surface", "()J");
  jlong sfc = (*env)->CallLongMethod(env, info, sfc_id);
  int mic = getInt(env, cls, info, "minImageCount");
  int ifmt = getInt(env, cls, info, "imageFormat");
  int cs = getInt(env, cls, info, "imageColorSpace");
  jmethodID ext_id = (*env)->GetMethodID(env, cls, "imageExtent", "()Lhephaestus/platform/Vulkan$Extent2D;");
  jobject ext_obj = (*env)->CallObjectMethod(env, info, ext_id);
  VkExtent2D ext = toExtent2D(env, ext_obj);
  int ial = getInt(env, cls, info, "imageArrayLayers");
  int iu = getInt(env, cls, info, "imageUsage");
  int ism = getInt(env, cls, info, "imageSharingMode");
  int pt = getInt(env, cls, info, "preTransform");
  int pm = getInt(env, cls, info, "presentMode");
  jmethodID ca_id = (*env)->GetMethodID(env, cls, "compositeAlpha", "()J");
  jlong ca = (*env)->CallLongMethod(env, info, ca_id);
  jmethodID cpd_id = (*env)->GetMethodID(env, cls, "clipped", "()Z");
  jboolean cpd = (*env)->CallBooleanMethod(env, info, cpd_id);
  VkSwapchainCreateInfoKHR v_info = {
    .sType = VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR,
    .pNext = NULL,
    .flags = fs,
    .surface = (VkSurfaceKHR) sfc,
    .minImageCount = mic,
    .imageFormat = ifmt,
    .imageColorSpace = cs,
    .imageExtent = ext,
    .imageArrayLayers = ial,
    .imageUsage = iu,
    .imageSharingMode = ism,
    .queueFamilyIndexCount = 0,
    .pQueueFamilyIndices = NULL,
    .preTransform = pt,
    .compositeAlpha = ca,
    .presentMode = pm,
    .clipped = cpd
  };
  return v_info;
}

JNIEXPORT jlong JNICALL Java_hephaestus_platform_Vulkan_createSwapchain
(JNIEnv* env, jobject instance __attribute__((unused)), jlong device, jobject info) {
  VkSwapchainCreateInfoKHR v_info = swapchainCreateInfo(env, info);
  VkSwapchainKHR s;
  RES(vkCreateSwapchainKHR((VkDevice) device, &v_info, NULL, &s));
  return (long) s;
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_destroySwapchain
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong device, jlong swapchain) {
  vkDestroySwapchainKHR((VkDevice) device, (VkSwapchainKHR) swapchain, NULL);
}

jobjectArray fromImages(JNIEnv* env, uint32_t count, VkImage* ims) {
  jclass cls = (*env)->FindClass(env, "hephaestus/platform/Vulkan$Image");
  jmethodID cons_id = (*env)->GetMethodID(env, cls, "<init>", "(J)V");
  jobject obj0 = (*env)->NewObject(env, cls, cons_id, ims[0]);
  jobjectArray objs = (*env)->NewObjectArray(env, count, cls, obj0);
  uint32_t i;
  for(i = 1; i < count; i++) {
    jobject obj = (*env)->NewObject(env, cls, cons_id, ims[i]);
    (*env)->SetObjectArrayElement(env, objs, i, obj);
  }
  return objs;
}

JNIEXPORT jobjectArray JNICALL Java_hephaestus_platform_Vulkan_getSwapchainImages
(JNIEnv* env, jobject instance __attribute__((unused)), jlong device, jlong swapchain) {
  uint32_t count;
  RES(vkGetSwapchainImagesKHR((VkDevice) device, (VkSwapchainKHR) swapchain, &count, NULL));
  VkImage* is = malloc(count * sizeof(VkImage));
  RES(vkGetSwapchainImagesKHR((VkDevice) device, (VkSwapchainKHR) swapchain, &count, is));
  return fromImages(env, count, is);
}

VkComponentMapping toComponentMapping(JNIEnv* env, jobject cm) {
  jclass cls = (*env)->GetObjectClass(env, cm);
  int r = getInt(env, cls, cm, "r");
  int g = getInt(env, cls, cm, "g");
  int b = getInt(env, cls, cm, "b");
  int a = getInt(env, cls, cm, "a");
  VkComponentMapping m = {
    .r = r,
    .g = g,
    .b = b,
    .a = a
  };
  return m;
}

VkImageSubresourceRange toImageSubresourceRange(JNIEnv* env, jobject sr) {
  jclass cls = (*env)->GetObjectClass(env, sr);
  int am = getInt(env, cls, sr, "aspectMask");
  int bml = getInt(env, cls, sr, "baseMipLevel");
  int lc = getInt(env, cls, sr, "levelCount");
  int bal = getInt(env, cls, sr, "baseArrayLayer");
  int lyrc = getInt(env, cls, sr, "layerCount");
  VkImageSubresourceRange v_sr = {
    .aspectMask = am,
    .baseMipLevel = bml,
    .levelCount = lc,
    .baseArrayLayer = bal,
    .layerCount = lyrc
  };
  return v_sr;
}

VkImageViewCreateInfo toImageViewCreateInfo(JNIEnv* env, jobject info) {
  jclass cls = (*env)->GetObjectClass(env, info);
  int fs = getInt(env, cls, info, "flags");
  jmethodID im_id = (*env)->GetMethodID(env, cls, "image", "()J");
  jlong im = (*env)->CallLongMethod(env, info, im_id);
  int vt = getInt(env, cls, info, "viewType");
  int fmt = getInt(env, cls, info, "format");
  jmethodID cm_id = (*env)->GetMethodID(env, cls, "components", "()Lhephaestus/platform/Vulkan$ComponentMapping;");
  jobject cm_obj = (*env)->CallObjectMethod(env, info, cm_id);
  VkComponentMapping cm = toComponentMapping(env, cm_obj);
  jmethodID sr_id = (*env)->GetMethodID(env, cls, "subresourceRange", "()Lhephaestus/platform/Vulkan$ImageSubresourceRange;");
  jobject sr_obj = (*env)->CallObjectMethod(env, info, sr_id);
  VkImageSubresourceRange sr = toImageSubresourceRange(env, sr_obj);
  VkImageViewCreateInfo v_info = {
    .sType = VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO,
    .pNext = NULL,
    .flags = fs,
    .image = (VkImage) im,
    .viewType = vt,
    .format = fmt,
    .components = cm,
    .subresourceRange = sr
  };
  return v_info;
}

JNIEXPORT jlong JNICALL Java_hephaestus_platform_Vulkan_createImageView
(JNIEnv* env, jobject instance __attribute__((unused)), jlong device, jobject info) {
  VkImageViewCreateInfo v_info = toImageViewCreateInfo(env, info);
  VkImageView view;
  RES(vkCreateImageView((VkDevice) device, &v_info, NULL, &view));
  return (jlong) view;
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_destroyImageView
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong device, jlong view) {
  vkDestroyImageView((VkDevice) device, (VkImageView) view, NULL);
}


JNIEXPORT jobject JNICALL Java_hephaestus_platform_Vulkan_getPhysicalDeviceFormatProperties
(JNIEnv* env, jobject instance __attribute__((unused)), jlong device, jint format) {
  VkFormatProperties props;
  vkGetPhysicalDeviceFormatProperties((VkPhysicalDevice) device, format, &props);
  jclass cls = (*env)->FindClass(env, "hephaestus/platform/Vulkan$FormatProperties");
  jmethodID cons_id = (*env)->GetMethodID(env, cls, "<init>", "(III)V");
  jobject obj = (*env)->NewObject(env, cls, cons_id, props.linearTilingFeatures, props.optimalTilingFeatures, props.bufferFeatures);
  return obj;
}

VkImageCreateInfo toImageCreateInfo(JNIEnv* env, jobject info) {
  jclass cls = (*env)->GetObjectClass(env, info);
  int fs = getInt(env, cls, info, "flags");
  int it = getInt(env, cls, info, "imageType");
  int fmt = getInt(env, cls, info, "format");
  int ml = getInt(env, cls, info, "mipLevels");
  int al = getInt(env, cls, info, "arrayLayers");
  int smp = getInt(env, cls, info, "samples");
  int tl = getInt(env, cls, info, "tiling");
  int us = getInt(env, cls, info, "usage");
  int sm = getInt(env, cls, info, "sharingMode");
  int il = getInt(env, cls, info, "initialLayout");
  jmethodID ext_id = (*env)->GetMethodID(env, cls, "extent", "()Lhephaestus/platform/Vulkan$Extent3D;");
  jobject ext_obj = (*env)->CallObjectMethod(env, info, ext_id);
  VkExtent3D ext = toExtent3D(env, ext_obj);
  getIntArray(env, cls, info, "queueFamilyIndices", qfi);
  VkImageCreateInfo v_info = {
    .sType = VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO,
    .pNext = NULL,
    .flags = fs,
    .imageType = it,
    .format = fmt,
    .extent = ext,
    .mipLevels = ml,
    .arrayLayers = al,
    .samples = smp,
    .tiling = tl,
    .usage = us,
    .sharingMode = sm,
    .queueFamilyIndexCount = qfi_count,
    .pQueueFamilyIndices = qfis,
    .initialLayout = il
  };
  return v_info;
}

JNIEXPORT jlong JNICALL Java_hephaestus_platform_Vulkan_createImage
(JNIEnv* env, jobject instance __attribute__((unused)), jlong device, jobject info) {
  VkImageCreateInfo v_info = toImageCreateInfo(env, info);
  VkImage i;
  RES(vkCreateImage((VkDevice) device, &v_info, NULL, &i));
  return (jlong) i;
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_destroyImage
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong device, jlong image) {
  vkDestroyImage((VkDevice) device, (VkImage) image, NULL);
}

jobject fromMemoryRequirements(JNIEnv* env, VkMemoryRequirements reqs) {
  jclass cls = (*env)->FindClass(env, "hephaestus/platform/Vulkan$MemoryRequirements");
  jmethodID cons_id = (*env)->GetMethodID(env, cls, "<init>", "(JJI)V");
  jobject obj = (*env)->NewObject(env, cls, cons_id, reqs.size, reqs.alignment, reqs.memoryTypeBits);
  return obj;
}

JNIEXPORT jobject JNICALL Java_hephaestus_platform_Vulkan_getImageMemoryRequirements
(JNIEnv* env, jobject instance __attribute__((unused)), jlong device, jlong image) {
  VkMemoryRequirements reqs;
  vkGetImageMemoryRequirements((VkDevice) device, (VkImage) image, &reqs);
  return fromMemoryRequirements(env, reqs);
}

VkMemoryAllocateInfo toMemoryAllocateInfo(JNIEnv* env, jobject info) {
  jclass cls = (*env)->GetObjectClass(env, info);
  jmethodID as_id = (*env)->GetMethodID(env, cls, "allocationSize", "()J");
  jlong as = (*env)->CallLongMethod(env, info, as_id);
  int mti = getInt(env, cls, info, "memoryTypeIndex");
  VkMemoryAllocateInfo v_info = {
    .sType = VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO,
    .pNext = NULL,
    .allocationSize = as,
    .memoryTypeIndex = mti
  };
  return v_info;
}

JNIEXPORT jlong JNICALL Java_hephaestus_platform_Vulkan_allocateMemory
(JNIEnv* env, jobject instance __attribute__((unused)), jlong device, jobject info) {
  VkMemoryAllocateInfo v_info = toMemoryAllocateInfo(env, info);
  VkDeviceMemory mem;
  RES(vkAllocateMemory((VkDevice) device, &v_info, NULL, &mem));
  return (jlong) mem;
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_bindImageMemory
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong device, jlong image, jlong memory, jlong offset) {
  RES(vkBindImageMemory((VkDevice) device, (VkImage) image, (VkDeviceMemory) memory, (VkDeviceSize) offset));
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_freeMemory
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong device, jlong memory) {
  vkFreeMemory((VkDevice) device, (VkDeviceMemory) memory, NULL);
}

VkBufferCreateInfo toBufferCreateInfo(JNIEnv* env, jobject info) {
  jclass cls = (*env)->GetObjectClass(env, info);
  int fs = getInt(env, cls, info, "flags");
  int sm = getInt(env, cls, info, "sharingMode");
  int us = getInt(env, cls, info, "usage");
  jmethodID s_id = (*env)->GetMethodID(env, cls, "size", "()J");
  jlong s = (*env)->CallLongMethod(env, info, s_id);
  VkBufferCreateInfo v_info = {
    .sType = VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO,
    .pNext = NULL,
    .flags = fs,
    .size = s,
    .usage = us,
    .sharingMode = sm,
    .queueFamilyIndexCount = 0,
    .pQueueFamilyIndices = NULL
  };
  return v_info;
}

JNIEXPORT jlong JNICALL Java_hephaestus_platform_Vulkan_createBuffer
(JNIEnv* env, jobject instance __attribute__((unused)), jlong device, jobject info) {
  VkBufferCreateInfo v_info = toBufferCreateInfo(env, info);
  VkBuffer buf;
  RES(vkCreateBuffer((VkDevice) device, &v_info, NULL, &buf));
  return (jlong) buf;
}

JNIEXPORT jobject JNICALL Java_hephaestus_platform_Vulkan_getBufferMemoryRequirements
(JNIEnv* env, jobject instance __attribute__((unused)), jlong device, jlong buffer) {
  VkMemoryRequirements reqs;
  vkGetBufferMemoryRequirements((VkDevice) device, (VkBuffer) buffer, &reqs);
  return fromMemoryRequirements(env, reqs);
}


JNIEXPORT jlong JNICALL Java_hephaestus_platform_Vulkan_mapMemory
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong device, jlong memory, jlong offset, jlong size, jint flags) {
  uint8_t *pData;
  RES(vkMapMemory((VkDevice) device, (VkDeviceMemory) memory, offset, size, flags, (void **)&pData));
  return (jlong) pData;
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_loadMemory
(JNIEnv* env, jobject instance __attribute__((unused)), jlong ptr, jobject buffer) {
  const void* bufferPtr = (*env)->GetDirectBufferAddress(env, buffer);
  jlong capacity = (*env)->GetDirectBufferCapacity(env, buffer);
  memcpy((void*) ptr, bufferPtr, capacity);
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_unmapMemory
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong device, jlong memory) {
  vkUnmapMemory( (VkDevice) device, (VkDeviceMemory) memory);
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_bindBufferMemory
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong device, jlong buffer, jlong memory, jlong offset) {
  RES(vkBindBufferMemory( (VkDevice) device, (VkBuffer) buffer, (VkDeviceMemory) memory, (VkDeviceSize) offset));
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_destroyBuffer
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong device, jlong buffer) {
  vkDestroyBuffer( (VkDevice) device, (VkBuffer) buffer, NULL);
}

VkDescriptorSetLayoutBinding toDescriptorSetLayoutBinding(JNIEnv* env, jobject b) {
  jclass cls = (*env)->GetObjectClass(env, b);
  int bd = getInt(env, cls, b, "binding");
  int dt = getInt(env, cls, b, "descriptorType");
  int dc = getInt(env, cls, b, "descriptorCount");
  int sf = getInt(env, cls, b, "stageFlags");
  jmethodID ss_id = (*env)->GetMethodID(env, cls, "immutableSamplers", "()[Lhephaestus/platform/Vulkan$Sampler;");
  jobjectArray ss_objs = (*env)->CallObjectMethod(env, b, ss_id);
  //TODO: extract samplers
  VkDescriptorSetLayoutBinding v_info = {
    .binding = bd,
    .descriptorType = dt,
    .descriptorCount = dc,
    .stageFlags = sf,
    .pImmutableSamplers = NULL,
  };
  return v_info;
}

VkDescriptorSetLayoutCreateInfo toDescriptorSetLayoutCreateInfo(JNIEnv* env, jobject info) {
  jclass cls = (*env)->GetObjectClass(env, info);
  int fs = getInt(env, cls, info, "flags");
  getObjectArray(env, cls, info, "bindings", HEPH "DescriptorSetLayoutBinding", b, VkDescriptorSetLayoutBinding, toDescriptorSetLayoutBinding);
  VkDescriptorSetLayoutCreateInfo v_info = {
    .sType = VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO,
    .flags = fs,
    .pNext = NULL,
    .bindingCount = b_count,
    .pBindings = bs
  };
  return v_info;
}

JNIEXPORT jlong JNICALL Java_hephaestus_platform_Vulkan_createDescriptorSetLayout
(JNIEnv* env, jobject instance __attribute__((unused)), jlong device, jobject info) {
  VkDescriptorSetLayoutCreateInfo v_info = toDescriptorSetLayoutCreateInfo(env, info);
  VkDescriptorSetLayout layout;
  RES(vkCreateDescriptorSetLayout((VkDevice) device, &v_info, NULL, &layout));
  return (jlong) layout;
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_destroyDescriptorSetLayout
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong device, jlong desc) {
  vkDestroyDescriptorSetLayout((VkDevice) device, (VkDescriptorSetLayout) desc, NULL);
}

VkPushConstantRange toPushConstantRange(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  int fs = getInt(env, cls, o, "stageFlags");
  int os = getInt(env, cls, o, "offset");
  int sz = getInt(env, cls, o, "size");
  VkPushConstantRange v_range = {
    .stageFlags = fs,
    .offset = os,
    .size = sz
  };
  return v_range;
}

VkDescriptorSetLayout toDescriptorSetLayout(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  jmethodID ptr_id = (*env)->GetMethodID(env, cls, "ptr", "()J");
  jlong ptr = (*env)->CallLongMethod(env, o, ptr_id);
  return (VkDescriptorSetLayout) ptr;
}

VkPipelineLayoutCreateInfo toPipelineLayoutCreateInfo(JNIEnv* env, jobject info) {
  jclass cls = (*env)->GetObjectClass(env, info);
  int fs = getInt(env, cls, info, "flags");
  getObjectArray(env, cls, info, "setLayouts", HEPH "DescriptorSetLayout", sl, VkDescriptorSetLayout, toDescriptorSetLayout);
  getObjectArray(env, cls, info, "pushConstantRanges", HEPH "PushConstantRange", pcr, VkPushConstantRange, toPushConstantRange);
  VkPipelineLayoutCreateInfo v_info = {
    .sType = VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO,
    .pNext = NULL,
    .flags = fs,
    .pushConstantRangeCount = pcr_count,
    .pPushConstantRanges = pcrs,
    .setLayoutCount = sl_count,
    .pSetLayouts = sls
  };
  return v_info;
}

JNIEXPORT jlong JNICALL Java_hephaestus_platform_Vulkan_createPipelineLayout
(JNIEnv* env, jobject instance __attribute__((unused)), jlong device, jobject info) {
  VkPipelineLayoutCreateInfo v_info = toPipelineLayoutCreateInfo(env, info);
  VkPipelineLayout layout;
  RES(vkCreatePipelineLayout((VkDevice) device, &v_info, NULL, &layout));
  return (jlong) layout;
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_destroyPipelineLayout
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong device, jlong pipeline) {
  vkDestroyPipelineLayout((VkDevice) device, (VkPipelineLayout) pipeline, NULL);
}

VkDescriptorPoolSize toDescriptorPoolSize(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  int t = getInt(env, cls, o, "tpe");
  int c = getInt(env, cls, o, "descriptorCount");
  VkDescriptorPoolSize p = {
    .type = t,
    .descriptorCount = c
  };
  return p;
}

VkDescriptorPoolCreateInfo toDescriptorPoolCreateInfo(JNIEnv* env, jobject info) {
  jclass cls = (*env)->GetObjectClass(env, info);
  int fs = getInt(env, cls, info, "flags");
  int ms = getInt(env, cls, info, "maxSets");
  getObjectArray(env, cls, info, "poolSizes", HEPH "DescriptorPoolSize", ps, VkDescriptorPoolSize, toDescriptorPoolSize);
  VkDescriptorPoolCreateInfo v_info = {
    .sType = VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO,
    .pNext = NULL,
    .flags = fs,
    .maxSets = ms,
    .poolSizeCount = ps_count,
    .pPoolSizes = pss
  };
  return v_info;
}

JNIEXPORT jlong JNICALL Java_hephaestus_platform_Vulkan_createDescriptorPool
(JNIEnv* env, jobject instance __attribute__((unused)), jlong device, jobject info) {
  VkDescriptorPoolCreateInfo v_info = toDescriptorPoolCreateInfo(env, info);
  VkDescriptorPool pool;
  RES(vkCreateDescriptorPool((VkDevice) device, &v_info, NULL, &pool));
  return (jlong) pool;
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_destroyDescriptorPool
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong device, jlong pool) {
  vkDestroyDescriptorPool((VkDevice) device, (VkDescriptorPool) pool, NULL);
}

VkDescriptorSetAllocateInfo toDescriptorSetAllocateInfo(JNIEnv* env, jobject info) {
  jclass cls = (*env)->GetObjectClass(env, info);
  jmethodID dp_id = (*env)->GetMethodID(env, cls, "descriptorPool", "()J");
  jlong dp = (*env)->CallLongMethod(env, info, dp_id);
  getObjectArray(env, cls, info, "setLayouts", HEPH "DescriptorSetLayout", sl, VkDescriptorSetLayout, toDescriptorSetLayout);

  VkDescriptorSetAllocateInfo v_info = {
    .sType = VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO,
    .pNext = NULL,
    .descriptorPool = (VkDescriptorPool) dp,
    .descriptorSetCount = sl_count,
    .pSetLayouts = sls
  };
  return v_info;
}

jobject fromDescriptorSet(JNIEnv* env, VkDescriptorSet s) {
  jclass cls = (*env)->FindClass(env, "hephaestus/platform/Vulkan$DescriptorSet");
  jmethodID cons_mid = (*env)->GetMethodID(env, cls, "<init>", "(J)V");
  jobject obj = (*env)->NewObject(env, cls, cons_mid, (jlong) s);
  return obj;
}

jobjectArray fromDescriptorSets(JNIEnv* env, VkDescriptorSet* ss, uint32_t count) {
  jclass cls = (*env)->FindClass(env, "hephaestus/platform/Vulkan$DescriptorSet");
  jobject obj0 = fromDescriptorSet(env, ss[0]);

  jobjectArray objs = (*env)->NewObjectArray(env, count, cls, obj0);
  uint32_t i;
  for(i = 1; i < count; i++){
    VkDescriptorSet s = ss[i];
    jobject obj = fromDescriptorSet(env, s);
    (*env)->SetObjectArrayElement(env, objs, i, obj);
  }
  return objs;
}

JNIEXPORT jobjectArray JNICALL Java_hephaestus_platform_Vulkan_allocateDescriptorSets
(JNIEnv* env, jobject instance __attribute__((unused)), jlong device, jobject info) {
  VkDescriptorSetAllocateInfo v_info = toDescriptorSetAllocateInfo(env, info);
  VkDescriptorSet sets[v_info.descriptorSetCount];
  RES(vkAllocateDescriptorSets((VkDevice) device, &v_info, sets));
  jobjectArray set_objs = fromDescriptorSets(env, sets, v_info.descriptorSetCount);
  return set_objs;
}

VkDescriptorSet toDescriptorSet(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  jmethodID ptr_id = (*env)->GetMethodID(env, cls, "ptr", "()J");
  jlong ptr = (*env)->CallLongMethod(env, o, ptr_id);
  return (VkDescriptorSet) ptr;
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_freeDescriptorSets
(JNIEnv* env, jobject instance __attribute__((unused)), jlong device, jlong pool, jint count, jobjectArray sets) {
  VkDescriptorSet* v_sets = ITER(env, sets, (uint32_t) count, VkDescriptorSet, toDescriptorSet)
  RES(vkFreeDescriptorSets((VkDevice) device, (VkDescriptorPool) pool, count, v_sets));
}

VkDescriptorBufferInfo toDescriptorBufferInfo(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  jmethodID buf_id = (*env)->GetMethodID(env, cls, "buffer", "()J");
  jlong buf = (*env)->CallLongMethod(env, o, buf_id);
  jmethodID off_id = (*env)->GetMethodID(env, cls, "offset", "()J");
  jlong off = (*env)->CallLongMethod(env, o, off_id);
  jmethodID ran_id = (*env)->GetMethodID(env, cls, "range", "()J");
  jlong ran = (*env)->CallLongMethod(env, o, ran_id);
  VkDescriptorBufferInfo v_info = {
    .buffer = (VkBuffer) buf,
    .offset = off,
    .range = ran
  };
  return v_info;
}

void toDescriptorBufferInfos(JNIEnv* env, jobjectArray objs, VkDescriptorBufferInfo* vs, uint32_t count) {
  uint32_t i;
  for(i = 0; i < count; i++) {
    jobject o = (*env)->GetObjectArrayElement(env, objs, i);
    VkDescriptorBufferInfo v = toDescriptorBufferInfo(env, o);
    vs[i] = v;
  }
}

VkDescriptorImageInfo toDescriptorImageInfo(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  long smp = getLong(env, cls, o, "sampler");
  long iv = getLong(env, cls, o, "imageView");
  long il = getInt(env, cls, o, "imageLayout");
  VkDescriptorImageInfo v_info = {
    .sampler = (VkSampler) smp,
    .imageView = (VkImageView) iv,
    .imageLayout = il
  };
  return v_info;
}
VkWriteDescriptorSet toWriteDescriptorSet(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  jmethodID dsts_id = (*env)->GetMethodID(env, cls, "dstSet", "()J");
  jlong dsts = (*env)->CallLongMethod(env, o, dsts_id);
  int dstb = getInt(env, cls, o, "dstBinding");
  int dsta = getInt(env, cls, o, "dstArrayElement");
  int dc = getInt(env, cls, o, "descriptorCount");
  int dt = getInt(env, cls, o, "descriptorType");
  
  VkDescriptorBufferInfo* bis = NULL;
  VkDescriptorImageInfo* iis = NULL;
  if(dt == VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER) {
    getObjectArray(env, cls, o, "bufferInfo", HEPH "DescriptorBufferInfo", buffer_info, VkDescriptorBufferInfo, toDescriptorBufferInfo);
    bis = buffer_infos;
  } else if(dt == VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER) {
    getObjectArray(env, cls, o, "imageInfo", HEPH "DescriptorImageInfo", image_info, VkDescriptorImageInfo, toDescriptorImageInfo);
    iis = image_infos;
  }

  VkWriteDescriptorSet v_set = {
    .sType = VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET,
    .pNext = NULL,
    .dstSet = (VkDescriptorSet) dsts,
    .dstBinding = dstb,
    .dstArrayElement = dsta,
    .descriptorCount = dc,
    .descriptorType = (VkDescriptorType) dt,
    .pImageInfo = iis,
    .pBufferInfo = bis,
    .pTexelBufferView = NULL
  };
  return v_set;
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_updateDescriptorSets
(JNIEnv* env, jobject instance __attribute__((unused)), jlong device, jint w_count, jobjectArray writes, jint c_count, jobjectArray copies __attribute__((unused))) {
  VkWriteDescriptorSet* v_writes = ITER(env, writes, (uint32_t) w_count, VkWriteDescriptorSet, toWriteDescriptorSet)
  vkUpdateDescriptorSets((VkDevice) device, w_count, v_writes, c_count, NULL); 
}

JNIEXPORT jlong JNICALL Java_hephaestus_platform_Vulkan_createSemaphore
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong device, jint info) {
  VkSemaphoreCreateInfo v_info = {
    .sType = VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO,
    .pNext = NULL,
    .flags = info
  };
  VkSemaphore semaphore;
  RES(vkCreateSemaphore((VkDevice) device, &v_info, NULL, &semaphore));
  return (jlong) semaphore;
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_destroySemaphore
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong device, jlong semaphore) {
  vkDestroySemaphore((VkDevice) device, (VkSemaphore) semaphore, NULL);
}

JNIEXPORT jint JNICALL Java_hephaestus_platform_Vulkan_acquireNextImageKHR
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong device, jlong swapchain, jlong timeout, jlong semaphore, jlong fence) {
  uint32_t i;
  RES(vkAcquireNextImageKHR((VkDevice) device, (VkSwapchainKHR) swapchain, timeout, (VkSemaphore) semaphore, (VkFence) fence, &i));
  return i;
}

VkAttachmentDescription toAttachmentDescription(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  int fs = getInt(env, cls, o, "flags");
  int fmt = getInt(env, cls, o, "format");
  int smp = getInt(env, cls, o, "samples");
  int lo = getInt(env, cls, o, "loadOp");
  int so = getInt(env, cls, o, "storeOp");
  int slo = getInt(env, cls, o, "stencilLoadOp");
  int sso = getInt(env, cls, o, "stencilStoreOp");
  int il = getInt(env, cls, o, "initialLayout");
  int fl = getInt(env, cls, o, "finalLayout");
  VkAttachmentDescription d = {
    .flags = fs,
    .format = fmt,
    .samples = smp,
    .loadOp = lo,
    .storeOp = so,
    .stencilLoadOp = slo,
    .stencilStoreOp = sso,
    .initialLayout = il,
    .finalLayout = fl
  };
  return d;
}

VkAttachmentReference toAttachmentReference(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  int at = getInt(env, cls, o, "attachment");
  int il = getInt(env, cls, o, "layout");
  VkAttachmentReference d = {
    .attachment = at,
    .layout = il
  };
  return d;
}

VkSubpassDescription toSubpassDescription(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  int fs = getInt(env, cls, o, "flags");
  int pbp = getInt(env, cls, o, "pipelineBindPoint");
  getObjectArray(env, cls, o, "inputAttachments", HEPH "AttachmentReference", ia, VkAttachmentReference, toAttachmentReference);
  getObjectArray(env, cls, o, "colorAttachments", HEPH "AttachmentReference", ca, VkAttachmentReference, toAttachmentReference);
  getObjectArray(env, cls, o, "depthStencilAttachment", HEPH "AttachmentReference", da, VkAttachmentReference, toAttachmentReference);
  getIntArray(env, cls, o, "preserveAttachments", pa);
  VkSubpassDescription d = {
    .flags = fs,
    .pipelineBindPoint = pbp,
    .inputAttachmentCount = ia_count,
    .pInputAttachments = ias,
    .colorAttachmentCount = ca_count,
    .pColorAttachments = cas,
    .pDepthStencilAttachment = das,
    .preserveAttachmentCount = pa_count,
    .pPreserveAttachments = pas
  };
  return d;
}

VkRenderPassCreateInfo toRenderPassCreateInfo(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  int fs = getInt(env, cls, o, "flags");
  getObjectArray(env, cls, o, "attachments", HEPH "AttachmentDescription", a, VkAttachmentDescription, toAttachmentDescription);
  getObjectArray(env, cls, o, "subpasses", HEPH "SubpassDescription", s, VkSubpassDescription, toSubpassDescription);
  VkRenderPassCreateInfo v_info = {
    .sType = VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO,
    .pNext = NULL,
    .flags = fs,
    .attachmentCount = a_count,
    .pAttachments = as,
    .subpassCount = s_count,
    .pSubpasses = ss,
    .dependencyCount = 0,
    .pDependencies = NULL
  };
  return v_info;
}

JNIEXPORT jlong JNICALL Java_hephaestus_platform_Vulkan_createRenderPass
(JNIEnv* env, jobject instance __attribute__((unused)), jlong device, jobject info) {
  VkRenderPassCreateInfo v_info = toRenderPassCreateInfo(env, info);
  VkRenderPass p;
  RES(vkCreateRenderPass((VkDevice) device, &v_info, NULL, &p));
  return (jlong) p;
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_destroyRenderPass
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong device, jlong renderPass) {
  vkDestroyRenderPass((VkDevice) device, (VkRenderPass) renderPass, NULL);
}

VkShaderModuleCreateInfo toShaderModuleCreateInfo(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  jmethodID fs_id = (*env)->GetMethodID(env, cls, "flags", "()I");
  jint fs = (*env)->CallIntMethod(env, o, fs_id);
  jmethodID cs_id = (*env)->GetMethodID(env, cls, "codeSize", "()I");
  jint cs = (*env)->CallIntMethod(env, o, cs_id);
  jmethodID pc_id = (*env)->GetMethodID(env, cls, "code", "()Ljava/nio/ByteBuffer;");
  jobject buffer = (*env)->CallObjectMethod(env, o, pc_id);
  uint32_t* pc_ptr = malloc(cs);
  const void* bufferPtr = (*env)->GetDirectBufferAddress(env, buffer);
  memcpy((void*) pc_ptr, bufferPtr, cs);

  VkShaderModuleCreateInfo v_info = {
    .sType = VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO,
    .pNext = NULL,
    .flags = fs,
    .codeSize = cs,
    .pCode =  pc_ptr
  };
  return v_info;
}

JNIEXPORT jlong JNICALL Java_hephaestus_platform_Vulkan_createShaderModule
(JNIEnv* env, jobject instance __attribute__((unused)), jlong device, jobject info) {
  VkShaderModuleCreateInfo v_info = toShaderModuleCreateInfo(env, info);
  VkShaderModule module;
  RES(vkCreateShaderModule((VkDevice) device, &v_info, NULL, &module));
  return (jlong) module;
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_destroyShaderModule
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong device, jlong module) {
  vkDestroyShaderModule((VkDevice) device, (VkShaderModule) module, NULL);
}


VkCommandBufferInheritanceInfo toCommandBufferInheritanceInfo(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  jlong rp = getLong(env, cls, o, "renderPass");
  VkCommandBufferInheritanceInfo v_info = {
    .sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_INHERITANCE_INFO,
    .framebuffer = VK_NULL_HANDLE,
    .renderPass = (VkRenderPass) rp
  };
  return v_info;
}

VkCommandBufferBeginInfo toCommandBufferBeginInfo(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  jint flags = getInt(env, cls, o, "flags");
  jmethodID ii_id = (*env)->GetMethodID(env, cls, "inheritanceInfo", 
                                         "()Lhephaestus/platform/Vulkan$CommandBufferInheritanceInfo;");
  jobject ii_o = (*env)->CallObjectMethod(env, o, ii_id);
  VkCommandBufferInheritanceInfo ii = toCommandBufferInheritanceInfo(env, ii_o);
  VkCommandBufferInheritanceInfo* ii_ptr = malloc(sizeof(VkCommandBufferInheritanceInfo));
  if(ii.renderPass == 0) {
    fprintf(stdout, "null handle");
    fflush(stdout);
    ii_ptr = VK_NULL_HANDLE;
  } else {
    fprintf(stdout, "non-null handle");
    fflush(stdout);
    *ii_ptr = ii;
  }
  VkCommandBufferBeginInfo v_info = {
    .sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO,
    .pNext = NULL,
    .flags = flags,
    .pInheritanceInfo = ii_ptr
  };
  return v_info;
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_beginCommandBuffer
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong buffer, jobject info) {
  VkCommandBufferBeginInfo v_info =  toCommandBufferBeginInfo(env, info);
  RES(vkBeginCommandBuffer((VkCommandBuffer) buffer, &v_info));
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_endCommandBuffer
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong buffer) {
  RES(vkEndCommandBuffer((VkCommandBuffer) buffer));
}

JNIEXPORT jlong JNICALL Java_hephaestus_platform_Vulkan_createFence
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong device, jint info) {
  VkFenceCreateInfo v_info = {
    .sType = VK_STRUCTURE_TYPE_FENCE_CREATE_INFO,
    .pNext = NULL,
    .flags = info
  };
  VkFence fence;
  RES(vkCreateFence((VkDevice) device, &v_info, NULL, &fence));
  return (jlong) fence;
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_destroyFence
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong device, jlong fence) {
  vkDestroyFence((VkDevice) device, (VkFence) fence, NULL);
}

VkSemaphore toSemaphore(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  jmethodID ptr_id = (*env)->GetMethodID(env, cls, "ptr", "()J");
  jlong ptr = (*env)->CallLongMethod(env, o, ptr_id);
  return (VkSemaphore) ptr;
}

VkCommandBuffer toCommandBuffer(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  jmethodID ptr_id = (*env)->GetMethodID(env, cls, "ptr", "()J");
  jlong ptr = (*env)->CallLongMethod(env, o, ptr_id);
  return (VkCommandBuffer) ptr;
}

VkSubmitInfo toSubmitInfo(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  getObjectArray(env, cls, o, "waitSemaphores", HEPH "Semaphore", ws, VkSemaphore, toSemaphore);
  getObjectArray(env, cls, o, "signalSemaphores", HEPH "Semaphore", ss, VkSemaphore, toSemaphore);
  getObjectArray(env, cls, o, "commandBuffers", HEPH "CommandBuffer", cb, VkCommandBuffer, toCommandBuffer);
  getIntArray(env, cls, o, "waitDstStageMask", wdsm);

  VkSubmitInfo info = {
    .sType = VK_STRUCTURE_TYPE_SUBMIT_INFO,
    .pNext = NULL,
    .waitSemaphoreCount = ws_count,
    .pWaitSemaphores = wss,
    .pWaitDstStageMask = (uint32_t*) wdsms,
    .commandBufferCount = cb_count,
    .pCommandBuffers = cbs,
    .signalSemaphoreCount = ss_count,
    .pSignalSemaphores = sss
  };
  return info;
}

void toSubmitInfos(JNIEnv* env, jobjectArray objs, VkSubmitInfo* vs, uint32_t count) {
  uint32_t i;
  for(i = 0; i < count; i++) {
    jobject o = (*env)->GetObjectArrayElement(env, objs, i);
    VkSubmitInfo v = toSubmitInfo(env, o);
    vs[i] = v;
  }
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_queueSubmit
(JNIEnv* env, jobject instance __attribute__((unused)), jlong queue, jint count, jobjectArray infos, jlong fence) {
  VkSubmitInfo* v_infos = malloc(count * sizeof(VkSubmitInfo));
  toSubmitInfos(env, infos, v_infos, count);
  RES(vkQueueSubmit((VkQueue) queue, count, v_infos, (VkFence) fence));
}


VkFence toFence(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  jmethodID ptr_id = (*env)->GetMethodID(env, cls, "ptr", "()J");
  jlong ptr = (*env)->CallLongMethod(env, o, ptr_id);
  return (VkFence) ptr;
}


JNIEXPORT jlong JNICALL Java_hephaestus_platform_Vulkan_waitForFences
(JNIEnv* env, jobject instance __attribute__((unused)), jlong device, jint count, jobjectArray fences, jboolean waitAll, jlong timeout) {
  VkFence* v_fences = ITER(env, fences, (uint32_t) count, VkFence, toFence)
  VkResult r = vkWaitForFences((VkDevice) device, count, v_fences, waitAll, timeout);
  return (jlong) r;
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_resetFences
(JNIEnv* env, jobject instance __attribute__((unused)), jlong device, jint count, jobjectArray fences) {
  VkFence* v_fences = ITER(env, fences, (uint32_t) count, VkFence, toFence)
  RES(vkResetFences((VkDevice) device, count, v_fences));
}

VkImageView toImageView(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  jmethodID ptr_id = (*env)->GetMethodID(env, cls, "ptr", "()J");
  jlong ptr = (*env)->CallLongMethod(env, o, ptr_id);
  return (VkImageView) ptr;
}


VkFramebufferCreateInfo toFramebufferCreateInfo(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  jmethodID fs_id = (*env)->GetMethodID(env, cls, "flags", "()I");
  jint fs = (*env)->CallIntMethod(env, o, fs_id);
  jmethodID rp_id = (*env)->GetMethodID(env, cls, "renderPass", "()J");
  jlong rp = (*env)->CallLongMethod(env, o, rp_id);
  getObjectArray(env, cls, o, "attachments", HEPH "ImageView", a, VkImageView, toImageView);
  jmethodID w_id = (*env)->GetMethodID(env, cls, "width", "()I");
  jint w = (*env)->CallIntMethod(env, o, w_id);
  jmethodID h_id = (*env)->GetMethodID(env, cls, "height", "()I");
  jint h = (*env)->CallIntMethod(env, o, h_id);
  jmethodID ls_id = (*env)->GetMethodID(env, cls, "layers", "()I");
  jint ls = (*env)->CallIntMethod(env, o, ls_id);

  VkFramebufferCreateInfo v_info = {
    .sType = VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO,
    .pNext = NULL,
    .flags = fs,
    .renderPass = (VkRenderPass) rp,
    .attachmentCount = a_count,
    .pAttachments = as,
    .width = w,
    .height = h,
    .layers = ls
  };

  return v_info;
}

JNIEXPORT jlong JNICALL Java_hephaestus_platform_Vulkan_createFramebuffer
(JNIEnv* env, jobject instance __attribute__((unused)), jlong device, jobject info) {
  VkFramebufferCreateInfo v_info = toFramebufferCreateInfo(env, info);
  VkFramebuffer fb;
  RES(vkCreateFramebuffer((VkDevice) device, &v_info, NULL, &fb));
  return (jlong) fb;
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_destroyFramebuffer
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong device, jlong fb) {
  vkDestroyFramebuffer((VkDevice) device, (VkFramebuffer) fb, NULL);
}

JNIEXPORT jlong JNICALL Java_hephaestus_platform_Vulkan_getDeviceQueue
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong device, jint queue_family_index, jint queue_index) {
  VkQueue q;
  vkGetDeviceQueue((VkDevice) device, queue_family_index, queue_index, &q);
  return (jlong) q;
}

VkOffset2D toOffset2D(JNIEnv* env, jobject e) {
  jclass cls = (*env)->GetObjectClass(env, e);
  jmethodID x_id = (*env)->GetMethodID(env, cls, "x", "()I");
  jint x = (*env)->CallIntMethod(env, e, x_id);
  jmethodID y_id = (*env)->GetMethodID(env, cls, "y", "()I");
  jint y = (*env)->CallIntMethod(env, e, y_id);
  VkOffset2D off = {
    .x = x,
    .y = y
  };
  return off;
}

VkOffset3D toOffset3D(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  jint x = getInt(env, cls, o, "x");
  jint y = getInt(env, cls, o, "y");
  jint z = getInt(env, cls, o, "z");
  VkOffset3D off = {
    .x = x,
    .y = y,
    .z = z
  };
  return off;
}

VkRect2D toRect2D(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  jmethodID of_id = (*env)->GetMethodID(env, cls, "offset", "()Lhephaestus/platform/Vulkan$Offset2D;");
  jobject of_obj = (*env)->CallObjectMethod(env, o, of_id);
  VkOffset2D of = toOffset2D(env, of_obj);
  jmethodID ex_id = (*env)->GetMethodID(env, cls, "extent", "()Lhephaestus/platform/Vulkan$Extent2D;");
  jobject ex_obj = (*env)->CallObjectMethod(env, o, ex_id);
  VkExtent2D ex = toExtent2D(env, ex_obj);
  VkRect2D v_rect = {
    .offset = of,
    .extent = ex
  };
  return v_rect;
}


VkClearColorValue toClearColorValue(JNIEnv* env, jobject o) {
  jclass f_cls = (*env)->FindClass(env, "hephaestus/platform/Vulkan$ClearColorValueFloat");
  jboolean is_f = (*env)->IsInstanceOf(env, o, f_cls);
  jclass i_cls = (*env)->FindClass(env, "hephaestus/platform/Vulkan$ClearColorValueInt");
  jboolean is_i = (*env)->IsInstanceOf(env, o, i_cls);
  jclass ui_cls = (*env)->FindClass(env, "hephaestus/platform/Vulkan$ClearColorValueUint");
  jboolean is_ui = (*env)->IsInstanceOf(env, o, ui_cls);
  VkClearColorValue cv;
  if(is_f) {
    getFloatArray(env, f_cls, o, "float32", f);
    cv.float32[0] = fs[0];
    cv.float32[1] = fs[1];
    cv.float32[2] = fs[2];
    cv.float32[3] = fs[3];
  } else if(is_i) {
    getIntArray(env, i_cls, o, "int32", i);
    cv.int32[0] = is[0];
    cv.int32[1] = is[1];
    cv.int32[2] = is[2];
    cv.int32[3] = is[3];
  } else if(is_ui) {
    getIntArray(env, ui_cls, o, "uint32", ui);
    cv.uint32[0] = uis[0];
    cv.uint32[1] = uis[1];
    cv.uint32[2] = uis[2];
    cv.uint32[3] = uis[3];
  } else {
    fprintf(stderr, "Unable to find subtype");
    fflush(stderr);
  }
  return cv;
}

VkClearDepthStencilValue toClearDepthStencilValue(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  jmethodID d_id = (*env)->GetMethodID(env, cls, "depth", "()F");
  jfloat d = (*env)->CallFloatMethod(env, o, d_id);
  jmethodID s_id = (*env)->GetMethodID(env, cls, "stencil", "()I");
  jint s = (*env)->CallIntMethod(env, o, s_id);
  VkClearDepthStencilValue ds = {
    .depth = d,
    .stencil = s
  };
  return ds;
}

VkClearValue toClearValue(JNIEnv* env, jobject o) {
  jclass c_cls = (*env)->FindClass(env, "hephaestus/platform/Vulkan$ClearValueColor");
  jboolean is_c = (*env)->IsInstanceOf(env, o, c_cls);
  jclass ds_cls = (*env)->FindClass(env, "hephaestus/platform/Vulkan$ClearValueDepthStencil");
  jboolean is_ds = (*env)->IsInstanceOf(env, o, ds_cls);
  VkClearValue cv;
  if(is_c) {
    jmethodID c_id = (*env)->GetMethodID(env, c_cls, "color", "()Lhephaestus/platform/Vulkan$ClearColorValue;");
    jobject c_obj = (*env)->CallObjectMethod(env, o, c_id);
    VkClearColorValue c = toClearColorValue(env, c_obj);
    cv.color = c;
  } else if(is_ds) {
    jmethodID ds_id = (*env)->GetMethodID(env, ds_cls, "depthStencil", "()Lhephaestus/platform/Vulkan$ClearDepthStencilValue;");
    jobject ds_obj = (*env)->CallObjectMethod(env, o, ds_id);
    VkClearDepthStencilValue ds = toClearDepthStencilValue(env, ds_obj);
    cv.depthStencil = ds;
  }
  return cv;
}


VkRenderPassBeginInfo toRenderPassBeginInfo(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  jmethodID rp_id = (*env)->GetMethodID(env, cls, "renderPass", "()J");
  jlong rp = (*env)->CallLongMethod(env, o, rp_id);
  jmethodID fb_id = (*env)->GetMethodID(env, cls, "framebuffer", "()J");
  jlong fb = (*env)->CallLongMethod(env, o, fb_id);
  jmethodID ra_id = (*env)->GetMethodID(env, cls, "renderArea", "()Lhephaestus/platform/Vulkan$Rect2D;");
  jobject ra_obj = (*env)->CallObjectMethod(env, o, ra_id);
  VkRect2D ra = toRect2D(env, ra_obj);
  getObjectArray(env, cls, o, "clearValues", HEPH "ClearValue", cv, VkClearValue, toClearValue);

  VkRenderPassBeginInfo v_info = {
    .sType = VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO,
    .pNext = NULL,
    .renderPass = (VkRenderPass) rp,
    .framebuffer = (VkFramebuffer) fb,
    .renderArea = ra,
    .clearValueCount = cv_count,
    .pClearValues = cvs
  };

  return v_info;
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_cmdBeginRenderPass
(JNIEnv* env, jobject instance __attribute((unused)), jlong cmd_buffer, jobject info, jint contents) {
  VkRenderPassBeginInfo v_info = toRenderPassBeginInfo(env, info);
  vkCmdBeginRenderPass((VkCommandBuffer) cmd_buffer, &v_info, (VkSubpassContents) contents);
}


VkBuffer toBuffer(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  jmethodID ptr_id = (*env)->GetMethodID(env, cls, "ptr", "()J");
  jlong ptr = (*env)->CallLongMethod(env, o, ptr_id);
  return (VkBuffer) ptr;
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_cmdBindVertexBuffers
(JNIEnv* env, jobject instance __attribute__((unused)), jlong cmd_buffer, jint first, jint count, jobjectArray buffers, 
 jlongArray offsets) {
  VkBuffer* v_buffers = ITER(env, buffers, (uint32_t) count, VkBuffer, toBuffer)
  VkDeviceSize* v_offsets = (*env)->GetLongArrayElements(env, offsets, 0);
  vkCmdBindVertexBuffers((VkCommandBuffer) cmd_buffer, first, count, v_buffers, v_offsets);
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_cmdBindIndexBuffer
(JNIEnv* env, jobject instance __attribute__((unused)), jlong cmd_buffer, jlong buffer, jlong offset, jint index_type) {
  vkCmdBindIndexBuffer((VkCommandBuffer) cmd_buffer, (VkBuffer) buffer, offset, index_type);
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_cmdEndRenderPass
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong buf) {
  vkCmdEndRenderPass((VkCommandBuffer) buf);
}

VkPipelineShaderStageCreateInfo toPipelineShaderStageCreateInfo(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  jmethodID f_id = (*env)->GetMethodID(env, cls, "flags", "()I");
  jint f = (*env)->CallIntMethod(env, o, f_id);
  jmethodID s_id = (*env)->GetMethodID(env, cls, "stage", "()I");
  jint s = (*env)->CallIntMethod(env, o, s_id);
  jmethodID m_id = (*env)->GetMethodID(env, cls, "module", "()J");
  jlong m = (*env)->CallLongMethod(env, o, m_id);
  jmethodID pn_id = (*env)->GetMethodID(env, cls, "name", "()Ljava/lang/String;");
  jstring pn_s = (*env)->CallObjectMethod(env, o, pn_id);
  const char* pn = (*env)->GetStringUTFChars(env, pn_s, 0);

  VkPipelineShaderStageCreateInfo v_info = {
    .sType = VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO,
    .pNext = NULL,
    .flags = f,
    .stage = s,
    .module = (VkShaderModule) m,
    .pName = pn,
    .pSpecializationInfo = NULL
  };
  return v_info;
}

VkVertexInputBindingDescription toVertexInputBindingDescription(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  jmethodID b_id = (*env)->GetMethodID(env, cls, "binding", "()I");
  jint b = (*env)->CallIntMethod(env, o, b_id);
  jmethodID s_id = (*env)->GetMethodID(env, cls, "stride", "()I");
  jint s = (*env)->CallIntMethod(env, o, s_id);
  jmethodID ir_id = (*env)->GetMethodID(env, cls, "inputRate", "()I");
  jint ir = (*env)->CallIntMethod(env, o, ir_id);
  VkVertexInputBindingDescription d = {
    .binding = b,
    .stride = s,
    .inputRate = ir
  };
  return d;
}

VkVertexInputAttributeDescription toVertexInputAttributeDescription(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  jmethodID b_id = (*env)->GetMethodID(env, cls, "binding", "()I");
  jint b = (*env)->CallIntMethod(env, o, b_id);
  jmethodID f_id = (*env)->GetMethodID(env, cls, "format", "()I");
  jint f = (*env)->CallIntMethod(env, o, f_id);
  jmethodID of_id = (*env)->GetMethodID(env, cls, "offset", "()I");
  jint of = (*env)->CallIntMethod(env, o, of_id);
  jmethodID l_id = (*env)->GetMethodID(env, cls, "location", "()I");
  jint l = (*env)->CallIntMethod(env, o, l_id);
  VkVertexInputAttributeDescription d = {
    .binding = b,
    .format = f,
    .location = l,
    .offset = of
  };
  return d;
}

VkPipelineVertexInputStateCreateInfo toPipelineVertexInputStateCreateInfo(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  jmethodID f_id = (*env)->GetMethodID(env, cls, "flags", "()I");
  jint f = (*env)->CallIntMethod(env, o, f_id);
  getObjectArray(env, cls, o, "vertexBindingDescriptions", HEPH "VertexInputBindingDescription", b, VkVertexInputBindingDescription, toVertexInputBindingDescription);
  getObjectArray(env, cls, o, "vertexAttributeDescriptions", HEPH "VertexInputAttributeDescription", a, VkVertexInputAttributeDescription, toVertexInputAttributeDescription);
  VkPipelineVertexInputStateCreateInfo v_info = {
    .sType = VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO,
    .pNext = NULL,
    .flags = f,
    .vertexBindingDescriptionCount = b_count,
    .pVertexBindingDescriptions = bs,
    .vertexAttributeDescriptionCount = a_count,
    .pVertexAttributeDescriptions = as
  };
  return v_info;
}

VkPipelineInputAssemblyStateCreateInfo toPipelineInputAssemblyStateCreateInfo(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  jmethodID f_id = (*env)->GetMethodID(env, cls, "flags", "()I");
  jint f = (*env)->CallIntMethod(env, o, f_id);
  jmethodID t_id = (*env)->GetMethodID(env, cls, "topology", "()I");
  jint t = (*env)->CallIntMethod(env, o, t_id);
  jmethodID re_id = (*env)->GetMethodID(env, cls, "primitiveRestartEnable", "()Z");
  jboolean re = (*env)->CallBooleanMethod(env, o, re_id);
  VkPipelineInputAssemblyStateCreateInfo v_info = {
    .sType = VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO,
    .pNext = NULL,
    .flags = f,
    .topology = (VkPrimitiveTopology) t,
    .primitiveRestartEnable = re
  };
  return v_info;
}


VkPipelineViewportStateCreateInfo toPipelineViewportStateCreateInfo(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  jmethodID f_id = (*env)->GetMethodID(env, cls, "flags", "()I");
  jint f = (*env)->CallIntMethod(env, o, f_id);
  jmethodID vc_id = (*env)->GetMethodID(env, cls, "viewportCount", "()I");
  jint vc = (*env)->CallIntMethod(env, o, vc_id);
  jmethodID sc_id = (*env)->GetMethodID(env, cls, "scissorCount", "()I");
  jint sc = (*env)->CallIntMethod(env, o, sc_id);

  VkPipelineViewportStateCreateInfo v_info = {
    .sType = VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO,
    .pNext = NULL,
    .flags = f,
    .viewportCount = vc,
    .pViewports = NULL,
    .scissorCount = sc,
    .pScissors = NULL
  };
  return v_info;
}

VkPipelineRasterizationStateCreateInfo toPipelineRasterizationStateCreateInfo(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  jmethodID f_id = (*env)->GetMethodID(env, cls, "flags", "()I");
  jint f = (*env)->CallIntMethod(env, o, f_id);
  jmethodID dce_id = (*env)->GetMethodID(env, cls, "depthClampEnable", "()Z");
  jboolean dce = (*env)->CallBooleanMethod(env, o, dce_id);
  jmethodID rde_id = (*env)->GetMethodID(env, cls, "rasterizerDiscardEnable", "()Z");
  jboolean rde = (*env)->CallBooleanMethod(env, o, rde_id);
  jmethodID pm_id = (*env)->GetMethodID(env, cls, "polygonMode", "()I");
  jint pm = (*env)->CallIntMethod(env, o, pm_id);
  jmethodID cm_id = (*env)->GetMethodID(env, cls, "cullMode", "()I");
  jint cm = (*env)->CallIntMethod(env, o, cm_id);
  jmethodID ff_id = (*env)->GetMethodID(env, cls, "frontFace", "()I");
  jint ff = (*env)->CallIntMethod(env, o, ff_id);
  jmethodID dbe_id = (*env)->GetMethodID(env, cls, "depthBiasEnable", "()Z");
  jboolean dbe = (*env)->CallBooleanMethod(env, o, dbe_id);
  jmethodID cf_id = (*env)->GetMethodID(env, cls, "depthBiasConstantFactor", "()F");
  jfloat cf = (*env)->CallFloatMethod(env, o, cf_id);
  jmethodID dbc_id = (*env)->GetMethodID(env, cls, "depthBiasClamp", "()F");
  jfloat dbc = (*env)->CallFloatMethod(env, o, dbc_id);
  jmethodID sf_id = (*env)->GetMethodID(env, cls, "depthBiasSlopeFactor", "()F");
  jfloat sf = (*env)->CallFloatMethod(env, o, sf_id);
  jmethodID lw_id = (*env)->GetMethodID(env, cls, "lineWidth", "()F");
  jfloat lw = (*env)->CallFloatMethod(env, o, lw_id);

  VkPipelineRasterizationStateCreateInfo v_info = {
    .sType = VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO,
    .pNext = NULL,
    .flags = f,
    .depthClampEnable = dce,
    .rasterizerDiscardEnable = rde,
    .polygonMode = (VkPolygonMode) pm,
    .cullMode = cm,
    .frontFace = (VkFrontFace) ff,
    .depthBiasEnable = dbe,
    .depthBiasConstantFactor = cf,
    .depthBiasClamp = dbc,
    .depthBiasSlopeFactor = sf,
    .lineWidth = lw
  };
  return v_info;
}

VkPipelineMultisampleStateCreateInfo toPipelineMultisampleStateCreateInfo(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  jmethodID f_id = (*env)->GetMethodID(env, cls, "flags", "()I");
  jint f = (*env)->CallIntMethod(env, o, f_id);
  jmethodID rs_id = (*env)->GetMethodID(env, cls, "rasterizationSamples", "()I");
  jint rs = (*env)->CallIntMethod(env, o, rs_id);
  jmethodID sse_id = (*env)->GetMethodID(env, cls, "sampleShadingEnable", "()Z");
  jboolean sse = (*env)->CallBooleanMethod(env, o, sse_id);
  jmethodID mss_id = (*env)->GetMethodID(env, cls, "minSampleShading", "()F");
  jfloat mss = (*env)->CallFloatMethod(env, o, mss_id);
  jmethodID sm_id = (*env)->GetMethodID(env, cls, "sampleMask", "()I");
  jint sm = (*env)->CallIntMethod(env, o, sm_id);
  jmethodID ace_id = (*env)->GetMethodID(env, cls, "alphaToCoverageEnable", "()Z");
  jboolean ace = (*env)->CallBooleanMethod(env, o, ace_id);
  jmethodID aoe_id = (*env)->GetMethodID(env, cls, "alphaToOneEnable", "()Z");
  jboolean aoe = (*env)->CallBooleanMethod(env, o, aoe_id);

  VkPipelineMultisampleStateCreateInfo v_info = {
    .sType = VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO,
    .pNext = NULL,
    .flags = f,
    .rasterizationSamples = rs,
    .sampleShadingEnable = sse,
    .minSampleShading = mss,
    .pSampleMask = NULL,
    .alphaToCoverageEnable = ace,
    .alphaToOneEnable = aoe
  };
  return v_info;
}

VkStencilOpState toStencilOpState(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  jmethodID f_id = (*env)->GetMethodID(env, cls, "failOp", "()I");
  jint f = (*env)->CallIntMethod(env, o, f_id);
  jmethodID po_id = (*env)->GetMethodID(env, cls, "passOp", "()I");
  jint po = (*env)->CallIntMethod(env, o, po_id);
  jmethodID dfo_id = (*env)->GetMethodID(env, cls, "depthFailOp", "()I");
  jint dfo = (*env)->CallIntMethod(env, o, dfo_id);
  jmethodID co_id = (*env)->GetMethodID(env, cls, "compareOp", "()I");
  jint co = (*env)->CallIntMethod(env, o, co_id);
  jmethodID cm_id = (*env)->GetMethodID(env, cls, "compareMask", "()I");
  jint cm = (*env)->CallIntMethod(env, o, cm_id);
  jmethodID wm_id = (*env)->GetMethodID(env, cls, "writeMask", "()I");
  jint wm = (*env)->CallIntMethod(env, o, wm_id);
  jmethodID rs_id = (*env)->GetMethodID(env, cls, "reference", "()I");
  jint rs = (*env)->CallIntMethod(env, o, rs_id);

  VkStencilOpState ss = {
    .failOp = f,
    .passOp = po,
    .depthFailOp = dfo,
    .compareOp = co,
    .compareMask = cm,
    .writeMask = wm,
    .reference = rs
  };
  return ss;
}
VkPipelineDepthStencilStateCreateInfo toPipelineDepthStencilStateCreateInfo(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  jmethodID f_id = (*env)->GetMethodID(env, cls, "flags", "()I");
  jint f = (*env)->CallIntMethod(env, o, f_id);
  jmethodID dte_id = (*env)->GetMethodID(env, cls, "depthTestEnable", "()Z");
  jboolean dte = (*env)->CallBooleanMethod(env, o, dte_id);
  jmethodID dwe_id = (*env)->GetMethodID(env, cls, "depthWriteEnable", "()Z");
  jboolean dwe = (*env)->CallBooleanMethod(env, o, dwe_id);
  jmethodID dco_id = (*env)->GetMethodID(env, cls, "depthCompareOp", "()I");
  jint dco = (*env)->CallIntMethod(env, o, dco_id);
  jmethodID dbe_id = (*env)->GetMethodID(env, cls, "depthBoundsTestEnable", "()Z");
  jboolean dbe = (*env)->CallBooleanMethod(env, o, dbe_id);
  jmethodID ste_id = (*env)->GetMethodID(env, cls, "stencilTestEnable", "()Z");
  jboolean ste = (*env)->CallBooleanMethod(env, o, ste_id);
  jmethodID ft_id = (*env)->GetMethodID(env, cls, "front", "()Lhephaestus/platform/Vulkan$StencilOpState;");
  jobject ft_obj = (*env)->CallObjectMethod(env, o, ft_id);
  VkStencilOpState ft = toStencilOpState(env, ft_obj);
  jmethodID bk_id = (*env)->GetMethodID(env, cls, "back", "()Lhephaestus/platform/Vulkan$StencilOpState;");
  jobject bk_obj = (*env)->CallObjectMethod(env, o, bk_id);
  VkStencilOpState bk = toStencilOpState(env, bk_obj);

  jmethodID min_id = (*env)->GetMethodID(env, cls, "minDepthBounds", "()F");
  jfloat min = (*env)->CallFloatMethod(env, o, min_id);
  jmethodID max_id = (*env)->GetMethodID(env, cls, "maxDepthBounds", "()F");
  jfloat max = (*env)->CallFloatMethod(env, o, max_id);

  VkPipelineDepthStencilStateCreateInfo v_info = {
    .sType = VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO,
    .pNext = NULL,
    .flags = f,
    .depthTestEnable = dte,
    .depthWriteEnable = dwe,
    .depthCompareOp = dco,
    .depthBoundsTestEnable = dbe,
    .stencilTestEnable = ste,
    .front = ft,
    .back = bk,
    .minDepthBounds = min,
    .maxDepthBounds = max
  };
  return v_info;
}


VkPipelineColorBlendAttachmentState toPipelineColorBlendAttachmentState(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  jmethodID be_id = (*env)->GetMethodID(env, cls, "blendEnable", "()Z");
  jboolean be = (*env)->CallBooleanMethod(env, o, be_id);
  jmethodID scbf_id = (*env)->GetMethodID(env, cls, "srcColorBlendFactor", "()I");
  jint scbf = (*env)->CallIntMethod(env, o, scbf_id);
  jmethodID dcbf_id = (*env)->GetMethodID(env, cls, "dstColorBlendFactor", "()I");
  jint dcbf = (*env)->CallIntMethod(env, o, dcbf_id);
  jmethodID sabf_id = (*env)->GetMethodID(env, cls, "srcAlphaBlendFactor", "()I");
  jint sabf = (*env)->CallIntMethod(env, o, sabf_id);
  jmethodID dabf_id = (*env)->GetMethodID(env, cls, "dstAlphaBlendFactor", "()I");
  jint dabf = (*env)->CallIntMethod(env, o, dabf_id);
  jmethodID cbo_id = (*env)->GetMethodID(env, cls, "colorBlendOp", "()I");
  jint cbo = (*env)->CallIntMethod(env, o, cbo_id);
  jmethodID abo_id = (*env)->GetMethodID(env, cls, "alphaBlendOp", "()I");
  jint abo = (*env)->CallIntMethod(env, o, abo_id);
  jmethodID cwm_id = (*env)->GetMethodID(env, cls, "colorWriteMask", "()I");
  jint cwm = (*env)->CallIntMethod(env, o, cwm_id);

  VkPipelineColorBlendAttachmentState s = {
    .blendEnable = be,
    .srcColorBlendFactor = scbf,
    .dstColorBlendFactor = dcbf,
    .srcAlphaBlendFactor = sabf,
    .dstAlphaBlendFactor = dabf,
    .colorBlendOp = cbo,
    .alphaBlendOp = abo,
    .colorWriteMask = cwm
  };
  return s;
}

VkPipelineColorBlendStateCreateInfo toPipelineColorBlendStateCreateInfo(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  jmethodID f_id = (*env)->GetMethodID(env, cls, "flags", "()I");
  jint f = (*env)->CallIntMethod(env, o, f_id);
  jmethodID loe_id = (*env)->GetMethodID(env, cls, "logicOpEnable", "()Z");
  jboolean loe = (*env)->CallBooleanMethod(env, o, loe_id);
  jmethodID lo_id = (*env)->GetMethodID(env, cls, "logicOp", "()I");
  jint lo = (*env)->CallIntMethod(env, o, lo_id);
  getObjectArray(env, cls, o, "attachments", HEPH "PipelineColorBlendAttachmentState", a, VkPipelineColorBlendAttachmentState, toPipelineColorBlendAttachmentState);
  getFloatArray(env, cls, o, "blendConstants", b);

  VkPipelineColorBlendStateCreateInfo v_info = {
    .sType = VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO,
    .pNext = NULL,
    .flags = f,
    .logicOpEnable = loe,
    .logicOp = lo,
    .attachmentCount = a_count,
    .pAttachments = as,
    .blendConstants = { bs[0], bs[1], bs[2], bs[3] }
  };
  return v_info;
}


VkDynamicState toDynamicState(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  jmethodID v_id = (*env)->GetMethodID(env, cls, "value", "()I");
  jint v = (*env)->CallIntMethod(env, o, v_id);
  return (VkDynamicState) v;
}

void toDynamicStates(JNIEnv* env, jobjectArray objs, VkDynamicState* vs, uint32_t count) {
  uint32_t i;
  for(i = 0; i < count; i++) {
    jobject o = (*env)->GetObjectArrayElement(env, objs, i);
    VkDynamicState v = toDynamicState(env, o);
    vs[i] = v;
  }
}

VkPipelineDynamicStateCreateInfo toPipelineDynamicStateCreateInfo(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  jmethodID f_id = (*env)->GetMethodID(env, cls, "flags", "()I");
  jint f = (*env)->CallIntMethod(env, o, f_id);
  jmethodID ds_id = (*env)->GetMethodID(env, cls, "dynamicStates", "()[Lhephaestus/platform/Vulkan$DynamicState;");
  jobjectArray ds_objs = (*env)->CallObjectMethod(env, o, ds_id);
  jsize ds_count = (*env)->GetArrayLength(env, ds_objs);
  VkDynamicState* ds = malloc(VK_DYNAMIC_STATE_RANGE_SIZE * sizeof(VkDynamicState));
  memset(ds, 0, VK_DYNAMIC_STATE_RANGE_SIZE * sizeof(VkDynamicState));
  toDynamicStates(env, ds_objs, ds, ds_count);

  VkPipelineDynamicStateCreateInfo v_info = {
    .sType = VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO,
    .pNext = NULL,
    .flags = f,
    .dynamicStateCount = ds_count,
    .pDynamicStates = ds,
  };
  return v_info;
}

VkGraphicsPipelineCreateInfo toGraphicsPipelineCreateInfo(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  jmethodID f_id = (*env)->GetMethodID(env, cls, "flags", "()I");
  jint f = (*env)->CallIntMethod(env, o, f_id);
  jmethodID rp_id = (*env)->GetMethodID(env, cls, "renderPass", "()J");
  jlong rp = (*env)->CallLongMethod(env, o, rp_id);
  jmethodID sp_id = (*env)->GetMethodID(env, cls, "subpass", "()I");
  jint sp = (*env)->CallIntMethod(env, o, sp_id);
  jmethodID bph_id = (*env)->GetMethodID(env, cls, "basePipelineHandle", "()J");
  jlong bph = (*env)->CallLongMethod(env, o, bph_id);
  jmethodID bpi_id = (*env)->GetMethodID(env, cls, "basePipelineIndex", "()I");
  jint bpi = (*env)->CallIntMethod(env, o, bpi_id);
  jmethodID lt_id = (*env)->GetMethodID(env, cls, "layout", "()J");
  jlong lt = (*env)->CallLongMethod(env, o, lt_id);
  getObjectArray(env, cls, o, "stages", HEPH "PipelineShaderStageCreateInfo", s, VkPipelineShaderStageCreateInfo, toPipelineShaderStageCreateInfo);
  jmethodID vis_id = (*env)->GetMethodID(env, cls, "vertexInputState", "()Lhephaestus/platform/Vulkan$PipelineVertexInputStateCreateInfo;");
  jobject vis_obj = (*env)->CallObjectMethod(env, o, vis_id);
  VkPipelineVertexInputStateCreateInfo vis = toPipelineVertexInputStateCreateInfo(env, vis_obj);
  VkPipelineVertexInputStateCreateInfo* vis_ptr = malloc(sizeof(VkPipelineVertexInputStateCreateInfo));
  *vis_ptr = vis;
  jmethodID ias_id = (*env)->GetMethodID(env, cls, "inputAssemblyState", "()Lhephaestus/platform/Vulkan$PipelineInputAssemblyStateCreateInfo;");
  jobject ias_obj = (*env)->CallObjectMethod(env, o, ias_id);
  VkPipelineInputAssemblyStateCreateInfo ias = toPipelineInputAssemblyStateCreateInfo(env, ias_obj);
  VkPipelineInputAssemblyStateCreateInfo* ias_ptr = malloc(sizeof(VkPipelineInputAssemblyStateCreateInfo));
  *ias_ptr = ias;
  jmethodID vs_id = (*env)->GetMethodID(env, cls, "viewportState", "()Lhephaestus/platform/Vulkan$PipelineViewportStateCreateInfo;");
  jobject vs_obj = (*env)->CallObjectMethod(env, o, vs_id);
  VkPipelineViewportStateCreateInfo vs = toPipelineViewportStateCreateInfo(env, vs_obj);
  VkPipelineViewportStateCreateInfo* vs_ptr = malloc(sizeof(VkPipelineViewportStateCreateInfo));
  *vs_ptr = vs;
  jmethodID rs_id = (*env)->GetMethodID(env, cls, "rasterizationState", "()Lhephaestus/platform/Vulkan$PipelineRasterizationStateCreateInfo;");
  jobject rs_obj = (*env)->CallObjectMethod(env, o, rs_id);
  VkPipelineRasterizationStateCreateInfo rs = toPipelineRasterizationStateCreateInfo(env, rs_obj);
  VkPipelineRasterizationStateCreateInfo* rs_ptr = malloc(sizeof(VkPipelineRasterizationStateCreateInfo));
  *rs_ptr = rs;
  jmethodID ms_id = (*env)->GetMethodID(env, cls, "multisampleState", "()Lhephaestus/platform/Vulkan$PipelineMultisampleStateCreateInfo;");
  jobject ms_obj = (*env)->CallObjectMethod(env, o, ms_id);
  VkPipelineMultisampleStateCreateInfo ms = toPipelineMultisampleStateCreateInfo(env, ms_obj);
  VkPipelineMultisampleStateCreateInfo* ms_ptr = malloc(sizeof(VkPipelineMultisampleStateCreateInfo));
  *ms_ptr = ms;
  jmethodID dss_id = (*env)->GetMethodID(env, cls, "depthStencilState", "()Lhephaestus/platform/Vulkan$PipelineDepthStencilStateCreateInfo;");
  jobject dss_obj = (*env)->CallObjectMethod(env, o, dss_id);
  VkPipelineDepthStencilStateCreateInfo dss = toPipelineDepthStencilStateCreateInfo(env, dss_obj);
  VkPipelineDepthStencilStateCreateInfo* dss_ptr = malloc(sizeof(VkPipelineDepthStencilStateCreateInfo));
  *dss_ptr = dss;
  jmethodID cbs_id = (*env)->GetMethodID(env, cls, "colorBlendState", "()Lhephaestus/platform/Vulkan$PipelineColorBlendStateCreateInfo;");
  jobject cbs_obj = (*env)->CallObjectMethod(env, o, cbs_id);
  VkPipelineColorBlendStateCreateInfo cbs = toPipelineColorBlendStateCreateInfo(env, cbs_obj);
  VkPipelineColorBlendStateCreateInfo* cbs_ptr = malloc(sizeof(VkPipelineColorBlendStateCreateInfo));
  *cbs_ptr = cbs;
  jmethodID ds_id = (*env)->GetMethodID(env, cls, "dynamicState", "()Lhephaestus/platform/Vulkan$PipelineDynamicStateCreateInfo;");
  jobject ds_obj = (*env)->CallObjectMethod(env, o, ds_id);
  VkPipelineDynamicStateCreateInfo ds = toPipelineDynamicStateCreateInfo(env, ds_obj);
  VkPipelineDynamicStateCreateInfo* ds_ptr = malloc(sizeof(VkPipelineDynamicStateCreateInfo));
  *ds_ptr = ds;
  VkGraphicsPipelineCreateInfo v_info = {
    .sType = VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO,
    .pNext = NULL,
    .layout = lt,
    .basePipelineHandle = (VkPipeline) bph,
    .basePipelineIndex = bpi,
    .flags = f,
    .pVertexInputState = vis_ptr,
    .pInputAssemblyState = ias_ptr,
    .pRasterizationState = rs_ptr,
    .pColorBlendState = cbs_ptr,
    .pTessellationState = NULL,
    .pMultisampleState = ms_ptr,
    .pDynamicState = ds_ptr,
    .pViewportState = vs_ptr,
    .pDepthStencilState = dss_ptr,
    .pStages = ss,
    .stageCount = s_count,
    .renderPass = rp,
    .subpass = sp
  };

  return v_info;
}

jobject fromPipeline(JNIEnv* env, VkPipeline p) {
  jclass cls = (*env)->FindClass(env, "hephaestus/platform/Vulkan$Pipeline");
  jmethodID cons_mid = (*env)->GetMethodID(env, cls, "<init>", "(J)V");
  jobject obj = (*env)->NewObject(env, cls, cons_mid, p);
  return obj;
}

jobjectArray fromPipelines(JNIEnv* env, VkPipeline* ps, uint32_t count) {
  jclass cls = (*env)->FindClass(env, "hephaestus/platform/Vulkan$Pipeline");
  jobject obj0 = fromPipeline(env, ps[0]);

  jobjectArray objs = (*env)->NewObjectArray(env, count, cls, obj0);
  uint32_t i;
  for(i = 1; i < count; i++){
    VkPipeline p = ps[i];
    jobject obj = fromPipeline(env, p);
    (*env)->SetObjectArrayElement(env, objs, i, obj);
  }
  return objs;
}


JNIEXPORT jobjectArray JNICALL Java_hephaestus_platform_Vulkan_createGraphicsPipelines
(JNIEnv* env, jobject instance __attribute__((unused)), jlong device, jint count, jobjectArray infos) {
  VkGraphicsPipelineCreateInfo* v_infos = ITER(env, infos, (uint32_t) count, VkGraphicsPipelineCreateInfo, toGraphicsPipelineCreateInfo)
  VkPipeline* pipelines = malloc(count * sizeof(VkPipeline));
  RES(vkCreateGraphicsPipelines((VkDevice) device, VK_NULL_HANDLE, count, v_infos, NULL, pipelines));
  jobjectArray objs = fromPipelines(env, pipelines, count);
  return objs;
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_destroyPipeline
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong device, jlong pipeline) {
  vkDestroyPipeline((VkDevice) device, (VkPipeline) pipeline, NULL);
}


jobject fromLayerProperties(JNIEnv* env, VkLayerProperties p) {
  jclass cls = (*env)->FindClass(env, "hephaestus/platform/Vulkan$LayerProperties");
  jmethodID cons_mid = (*env)->GetMethodID(env, cls, "<init>", "(Ljava/lang/String;IILjava/lang/String;)V");
  jstring ln = (*env)->NewStringUTF(env, p.layerName); 
  jstring d = (*env)->NewStringUTF(env, p.description); 
  jobject obj = (*env)->NewObject(env, cls, cons_mid, ln, p.specVersion, p.implementationVersion, d);
  return obj;
}

jobjectArray fromLayerPropertiess(JNIEnv* env, VkLayerProperties* ts, uint32_t count) {
  jclass cls = (*env)->FindClass(env, "hephaestus/platform/Vulkan$LayerProperties");
  jobject obj0 = fromLayerProperties(env, ts[0]);

  jobjectArray objs = (*env)->NewObjectArray(env, count, cls, obj0);
  uint32_t i;
  for(i = 1; i < count; i++){
    VkLayerProperties t = ts[i];
    jobject obj = fromLayerProperties(env, t);
    (*env)->SetObjectArrayElement(env, objs, i, obj);
  }
  return objs;
}

JNIEXPORT jobjectArray JNICALL Java_hephaestus_platform_Vulkan_enumerateInstanceLayerProperties
(JNIEnv* env, jobject instance __attribute__((unused))) {
  uint32_t count = 0;
  RES(vkEnumerateInstanceLayerProperties(&count, NULL));
  VkLayerProperties* props = malloc(count * sizeof(VkLayerProperties));
  RES(vkEnumerateInstanceLayerProperties(&count, props));
  jobjectArray objs = fromLayerPropertiess(env, props, count);
  return objs;
}

JNIEXPORT jobjectArray JNICALL Java_hephaestus_platform_Vulkan_enumerateDeviceLayerProperties
(JNIEnv* env, jobject instance __attribute__((unused)), jlong device) {
  uint32_t count = 0;
  RES(vkEnumerateDeviceLayerProperties((VkPhysicalDevice) device, &count, NULL));
  VkLayerProperties* props = malloc(count * sizeof(VkLayerProperties));
  RES(vkEnumerateDeviceLayerProperties((VkPhysicalDevice) device, &count, props));
  jobjectArray objs = fromLayerPropertiess(env, props, count);
  return objs;
}

jobject fromExtensionProperties(JNIEnv* env, VkExtensionProperties p) {
  jclass cls = (*env)->FindClass(env, "hephaestus/platform/Vulkan$ExtensionProperties");
  jmethodID cons_mid = (*env)->GetMethodID(env, cls, "<init>", "(Ljava/lang/String;I)V");
  jstring en = (*env)->NewStringUTF(env, p.extensionName); 
  jobject obj = (*env)->NewObject(env, cls, cons_mid, en, p.specVersion);
  {
    jobject obj;
  }
  return obj;
}

jobjectArray fromExtensionPropertiess(JNIEnv* env, VkExtensionProperties* ts, uint32_t count) {
  jclass cls = (*env)->FindClass(env, "hephaestus/platform/Vulkan$ExtensionProperties");
  jobject obj0 = fromExtensionProperties(env, ts[0]);

  jobjectArray objs = (*env)->NewObjectArray(env, count, cls, obj0);
  uint32_t i;
  for(i = 1; i < count; i++){
    VkExtensionProperties t = ts[i];
    jobject obj = fromExtensionProperties(env, t);
    (*env)->SetObjectArrayElement(env, objs, i, obj);
  }
  return objs;
}

JNIEXPORT jobjectArray JNICALL Java_hephaestus_platform_Vulkan_enumerateInstanceExtensionProperties
(JNIEnv* env, jobject instance __attribute__((unused)), jstring name) {
  const char* n = (*env)->GetStringUTFChars(env, name, 0);
  uint32_t count = 0;
  RES(vkEnumerateInstanceExtensionProperties(n, &count, NULL));
  VkExtensionProperties* props = malloc(count * sizeof(VkExtensionProperties));
  RES(vkEnumerateInstanceExtensionProperties(n, &count, props));
  jobjectArray objs = fromExtensionPropertiess(env, props, count);
  return objs;
}

VkBool32 debugCallback(
    VkDebugReportFlagsEXT                       flags,
    VkDebugReportObjectTypeEXT                  objectType,
    uint64_t                                    object,
    size_t                                      location,
    int32_t                                     messageCode,
    const char*                                 pLayerPrefix,
    const char*                                 pMessage,
    void*                                       pUserData) {
  fprintf(stdout, "===callback called===\n object type:%d\nmessage: %s\nlocation: %d\nprefix: %s\n===\n", 
          objectType, pMessage, location, pLayerPrefix);
  fflush(stdout);
  return VK_FALSE;
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_debugReport
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong inst) {
  VkDebugReportCallbackCreateInfoEXT info = {
    .sType = VK_STRUCTURE_TYPE_DEBUG_REPORT_CALLBACK_CREATE_INFO_EXT,
    .pNext = NULL,
    .flags = VK_DEBUG_REPORT_INFORMATION_BIT_EXT | 
    VK_DEBUG_REPORT_WARNING_BIT_EXT | VK_DEBUG_REPORT_ERROR_BIT_EXT | VK_DEBUG_REPORT_DEBUG_BIT_EXT | 0,
    .pfnCallback = debugCallback,
    .pUserData = NULL
  };
  VkDebugReportCallbackEXT callback;
  PFN_vkCreateDebugReportCallbackEXT f = (PFN_vkCreateDebugReportCallbackEXT) glfwGetInstanceProcAddress((VkInstance) inst, "vkCreateDebugReportCallbackEXT");
  f((VkInstance) inst, &info, NULL, &callback);
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_cmdBindPipeline
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong buffer, jint bindpoint, jlong pipeline) {
  vkCmdBindPipeline((VkCommandBuffer) buffer, (VkPipelineBindPoint) bindpoint, (VkPipeline) pipeline);
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_cmdBindDescriptorSets
(JNIEnv* env, jobject instance __attribute__((unused)), jlong buffer, jint bindpoint, jlong layout, 
 jint first_set, jint set_count, jobjectArray sets, jint offset_count, jintArray offsets) {
  VkDescriptorSet* v_sets = ITER(env, sets, (uint32_t) set_count, VkDescriptorSet, toDescriptorSet);
  vkCmdBindDescriptorSets((VkCommandBuffer) buffer, (VkPipelineBindPoint) bindpoint, (VkPipelineLayout) layout,
                          first_set, set_count, v_sets, offset_count, (uint32_t*) offsets);
}

VkViewport toViewport(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  float x = getFloat(env, cls, o, "x");
  float y = getFloat(env, cls, o, "y");
  float w = getFloat(env, cls, o, "width");
  float h = getFloat(env, cls, o, "height");
  float min = getFloat(env, cls, o, "minDepth");
  float max = getFloat(env, cls, o, "maxDepth");
  VkViewport vp = {
    .x = x,
    .y = y,
    .width = w,
    .height = h,
    .minDepth = min,
    .maxDepth = max
  };
  return vp;
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_cmdSetViewport
(JNIEnv* env, jobject instance __attribute__((unused)), jlong buffer, jint first_viewport, jint count, jobjectArray viewports) {
  VkViewport* v_viewports = ITER(env, viewports, (uint32_t) count, VkViewport, toViewport);
  vkCmdSetViewport((VkCommandBuffer) buffer, first_viewport, count, v_viewports);
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_cmdSetScissor
(JNIEnv* env, jobject instance __attribute__((unused)), jlong buffer, jint first_scissor, jint count, jobjectArray scissors) {
  VkRect2D* v_scissors = ITER(env, scissors, (uint32_t) count, VkRect2D, toRect2D);
  vkCmdSetScissor((VkCommandBuffer) buffer, first_scissor, count, v_scissors);
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_cmdDraw
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong buffer, jint vertex_count, jint instance_count, jint first_vertex, jint first_instance) {
  vkCmdDraw((VkCommandBuffer) buffer, vertex_count, instance_count, first_vertex, first_instance);
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_cmdDrawIndexed
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong buffer, jint index_count, jint instance_count, jint first_index, jint vertex_offset, jint first_instance) {
  vkCmdDrawIndexed((VkCommandBuffer) buffer, index_count, instance_count, first_index, vertex_offset, first_instance);
}

VkSwapchainKHR toSwapchainKHR(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  jmethodID ptr_id = (*env)->GetMethodID(env, cls, "ptr", "()J");
  jlong ptr = (*env)->CallLongMethod(env, o, ptr_id);
  return (VkSwapchainKHR) ptr;
}

VkPresentInfoKHR toPresentInfoKHR(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  
  getObjectArray(env, cls, o, "waitSemaphores", HEPH "Semaphore", ws, VkSemaphore, toSemaphore);
  getObjectArray(env, cls, o, "swapchains", HEPH "Swapchain", s, VkSwapchainKHR, toSwapchainKHR);
  int ii = getInt(env, cls, o, "imageIndices");
  uint32_t* ii_ptr = malloc(sizeof(uint32_t));
  *ii_ptr = ii;
  VkPresentInfoKHR v_info = {
    .sType = VK_STRUCTURE_TYPE_PRESENT_INFO_KHR,
    .pNext = NULL,
    .waitSemaphoreCount = ws_count,
    .pWaitSemaphores = wss,
    .swapchainCount = s_count,
    .pSwapchains = ss,
    .pImageIndices = ii_ptr,
    .pResults = NULL
  };
  return v_info;
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_queuePresentKHR
(JNIEnv* env, jobject instance __attribute__((unused)), jlong queue, jobject info) {
  VkPresentInfoKHR v_info = toPresentInfoKHR(env, info);
  vkQueuePresentKHR((VkQueue) queue, &v_info);
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_cmdExecuteCommands
(JNIEnv* env, jobject instance __attribute((unused)), jlong buffer, jint count, jobjectArray buffers) {
  VkCommandBuffer* bufs = ITER(env, buffers, (uint32_t) count, VkCommandBuffer, toCommandBuffer);
  vkCmdExecuteCommands((VkCommandBuffer) buffer, (uint32_t) count, bufs);
}

VkImageSubresource toImageSubresource(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  int am = getInt(env, cls, o, "aspectMask");
  int ml = getInt(env, cls, o, "mipLevel");
  int al = getInt(env, cls, o, "arrayLayer");
  VkImageSubresource v_o = {
    .aspectMask = am,
    .mipLevel = ml,
    .arrayLayer = al
  };
  return v_o;
}

jobject fromSubresourceLayout(JNIEnv* env, VkSubresourceLayout l) {
  jclass cls = (*env)->FindClass(env, "hephaestus/platform/Vulkan$SubresourceLayout");
  jmethodID cons_mid = (*env)->GetMethodID(env, cls, "<init>", "(JJJJJ)V");
  jobject obj = (*env)->NewObject(env, cls, cons_mid, l.offset, l.size, l.rowPitch, l.arrayPitch, l.depthPitch);
  return obj;
}

JNIEXPORT jobject JNICALL Java_hephaestus_platform_Vulkan_getImageSubresourceLayout
(JNIEnv* env, jobject instance __attribute__((unused)), jlong device, jlong image, jobject subresource) {
  VkImageSubresource sr = toImageSubresource(env, subresource);
  VkSubresourceLayout layout;
  vkGetImageSubresourceLayout((VkDevice) device, (VkImage) image, &sr, &layout);
  jobject o = fromSubresourceLayout(env, layout);
  return o;
}

VkMemoryBarrier toMemoryBarrier(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  int sam = getInt(env, cls, o, "srcAccessMask");
  int dam = getInt(env, cls, o, "dstAccessMask");
  VkMemoryBarrier v_o = {
    .sType = VK_STRUCTURE_TYPE_MEMORY_BARRIER,
    .pNext = NULL,
    .srcAccessMask = sam,
    .dstAccessMask = dam
  };
  return v_o;
}
VkBufferMemoryBarrier toBufferMemoryBarrier(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  int sam = getInt(env, cls, o, "srcAccessMask");
  int dam = getInt(env, cls, o, "dstAccessMask");
  int sqfi = getInt(env, cls, o, "srcQueueFamilyIndex");
  int dqfi = getInt(env, cls, o, "dstQueueFamilyIndex");
  long buf = getLong(env, cls, o, "buffer");
  long ofs = getLong(env, cls, o, "offset");
  long sz = getLong(env, cls, o, "size");
  VkBufferMemoryBarrier v_o = {
    .sType = VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER,
    .pNext = NULL,
    .srcAccessMask = sam,
    .dstAccessMask = dam,
    .srcQueueFamilyIndex = sqfi,
    .dstQueueFamilyIndex = dqfi,
    .buffer = (VkBuffer) buf,
    .offset = ofs,
    .size = sz
  };
  return v_o;
}

VkImageMemoryBarrier toImageMemoryBarrier(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  int sam = getInt(env, cls, o, "srcAccessMask");
  int dam = getInt(env, cls, o, "dstAccessMask");
  int sqfi = getLong(env, cls, o, "srcQueueFamilyIndex");
  int dqfi = getLong(env, cls, o, "dstQueueFamilyIndex");
  int ol = getInt(env, cls, o, "oldLayout");
  int nl = getInt(env, cls, o, "newLayout");
  long im = getLong(env, cls, o, "image");
  VkImageSubresourceRange sr = getObject(env, cls, o, "subresourceRange", HEPH "ImageSubresourceRange", toImageSubresourceRange);

  VkImageMemoryBarrier v_o = {
    .sType = VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER,
    .pNext = NULL,
    .srcAccessMask = sam,
    .dstAccessMask = dam,
    .oldLayout = ol,
    .newLayout = nl,
    .srcQueueFamilyIndex = sqfi,
    .dstQueueFamilyIndex = dqfi,
    .image = (VkImage) im,
    .subresourceRange = sr
  };
  return v_o;
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_cmdPipelineBarrier
(JNIEnv* env, jobject instance __attribute__((unused)), jlong buffer , jint src_mask, jint dst_mask,
 jint flags, jobjectArray memory_barriers, jobjectArray buffer_memory_barriers, jobjectArray image_memory_barriers) {
  jsize mem_count = (*env)->GetArrayLength(env, memory_barriers);
  const VkMemoryBarrier* mems = ITER(env, memory_barriers, (uint32_t) mem_count, VkMemoryBarrier, toMemoryBarrier);
  jsize buf_count = (*env)->GetArrayLength(env, buffer_memory_barriers);
  const VkBufferMemoryBarrier* bufs = ITER(env, buffer_memory_barriers, (uint32_t) buf_count, VkBufferMemoryBarrier, toBufferMemoryBarrier);
  jsize im_count = (*env)->GetArrayLength(env, image_memory_barriers);
  const VkImageMemoryBarrier* ims = ITER(env, image_memory_barriers, (uint32_t) im_count, VkImageMemoryBarrier, toImageMemoryBarrier);
  vkCmdPipelineBarrier((VkCommandBuffer) buffer, src_mask, dst_mask, flags, mem_count, mems, buf_count, bufs, im_count, ims);
}

VkSamplerCreateInfo toSamplerCreateInfo(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  int fs = getInt(env, cls, o, "flags");
  int magf = getInt(env, cls, o, "magFilter");
  int minf = getInt(env, cls, o, "minFilter");
  int mmm = getInt(env, cls, o, "mipmapMode");
  int amu = getInt(env, cls, o, "addressModeU");
  int amv = getInt(env, cls, o, "addressModeV");
  int amw = getInt(env, cls, o, "addressModeW");
  float mlb = getFloat(env, cls, o, "mipLodBias");
  jboolean ae = getBoolean(env, cls, o, "anisotropyEnable");
  float ma = getFloat(env, cls, o, "maxAnisotropy");
  jboolean ce = getBoolean(env, cls, o, "compareEnable");
  int co = getInt(env, cls, o, "compareOp");
  float minl = getFloat(env, cls, o, "minLod");
  float maxl = getFloat(env, cls, o, "maxLod");
  int bc = getInt(env, cls, o, "borderColor");
  jboolean uc = getBoolean(env, cls, o, "unnormalizedCoordinates");

  VkSamplerCreateInfo v_o = {
    .sType = VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO,
    .pNext = NULL,
    .flags = fs,
    .magFilter = magf,
    .minFilter = minf,
    .mipmapMode = mmm,
    .addressModeU = amu,
    .addressModeV = amv,
    .addressModeW = amw,
    .mipLodBias = mlb,
    .anisotropyEnable = ae,
    .maxAnisotropy = ma,
    .compareEnable = ce,
    .compareOp = co,
    .minLod = minl,
    .maxLod = maxl,
    .borderColor = bc,
    .unnormalizedCoordinates = uc
  };
  return v_o;
}
JNIEXPORT jlong JNICALL Java_hephaestus_platform_Vulkan_createSampler
(JNIEnv* env, jobject instance __attribute__((unused)), jlong device, jobject info) {
  VkSamplerCreateInfo v_info = toSamplerCreateInfo(env, info);
  VkSampler s;
  vkCreateSampler((VkDevice) device, &v_info, NULL, &s);
  return s;
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_destroySampler
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong device, jlong sampler) {
  vkDestroySampler((VkDevice) device, (VkSampler) sampler, NULL);
}

VkBufferCopy toBufferCopy(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  int src = getLong(env, cls, o, "srcOffset");
  int dst = getLong(env, cls, o, "dstOffset");
  int size = getLong(env, cls, o, "size");
  VkBufferCopy v_o = {
    .srcOffset = src,
    .dstOffset = dst,
    .size = size
  };
  return v_o;
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_cmdCopyBuffer
(JNIEnv* env, jobject instance __attribute__((unused)), jlong cmd_buffer, jlong src, jlong dst, jobjectArray regions) {
  jsize region_count = (*env)->GetArrayLength(env, regions);
  const VkBufferCopy* v_regions = ITER(env, regions, (uint32_t) region_count, VkBufferCopy, toBufferCopy);
  vkCmdCopyBuffer((VkCommandBuffer) cmd_buffer, (VkBuffer) src, (VkBuffer) dst, region_count, v_regions);
}

VkImageSubresourceLayers toImageSubresourceLayers(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  int iaf = getInt(env, cls, o, "aspectMask");
  int ml = getInt(env, cls, o, "mipLevel");
  int bal = getInt(env, cls, o, "baseArrayLayer");
  int lc = getInt(env, cls, o, "layerCount");
  VkImageSubresourceLayers v_o = {
    .aspectMask = iaf,
    .mipLevel = ml,
    .baseArrayLayer = bal,
    .layerCount = lc
  };
  return v_o;
}


VkBufferImageCopy toBufferImageCopy(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  MARK("d")
  long boff = getLong(env, cls, o, "bufferOffset");
  MARK("e")
  int brl = getInt(env, cls, o, "bufferRowLength");
  MARK("f")
  int bih = getInt(env, cls, o, "bufferImageHeight");
  MARK("g")
  VkImageSubresourceLayers ils = getObject(env, cls, o, "imageSubresource", HEPH "ImageSubresourceLayers", toImageSubresourceLayers);
  MARK("h")
  VkOffset3D ioff = getObject(env, cls, o, "imageOffset", HEPH "Offset3D", toOffset3D);
  MARK("i")
  VkExtent3D iext = getObject(env, cls, o, "imageExtent", HEPH "Extent3D", toExtent3D);
  MARK("j")
  fprintf(stdout, "extent is %d %d %d \n", iext.width, iext.height, iext.depth);
  fprintf(stdout, "offset is %d %d %d \n", ioff.x, ioff.y, ioff.z);
  fprintf(stdout, "buffer offset is %d %d %d \n", boff, brl, bih);
  VkBufferImageCopy v_o = {
    .bufferOffset = boff,
    .bufferRowLength = brl,
    .bufferImageHeight = bih,
    .imageSubresource = ils,
    .imageOffset = ioff,
    .imageExtent = iext
  };
  return v_o;
}
/*
 * Class:     hephaestus_platform_Vulkan
 * Method:    cmdCopyBufferToImage
 * Signature: (JJJI[Lhephaestus/platform/Vulkan/BufferImageCopy;)V
 */
JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_cmdCopyBufferToImage
(JNIEnv* env, jobject instance __attribute__((unused)), jlong cmd_buffer, jlong src, jlong dst, jint layout, jobjectArray regions) {
  MARK("a")
  jsize region_count = (*env)->GetArrayLength(env, regions);
  MARK("b")
  const VkBufferImageCopy* v_regions = ITER(env, regions, (uint32_t) region_count, VkBufferImageCopy, toBufferImageCopy);
  fprintf(stdout, "region count is %d \n", region_count);
  fflush(stdout);
  vkCmdCopyBufferToImage((VkCommandBuffer) cmd_buffer, (VkBuffer) src, (VkImage) dst, layout, region_count, v_regions);
  MARK("c")
}

JNIEXPORT jlong JNICALL Java_hephaestus_platform_Vulkan_getFenceStatus
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong device, jlong fence) {
  return vkGetFenceStatus((VkDevice) device, (VkFence) fence);
}
