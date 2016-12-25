/* C source files to proxy Vulkan calls */
#define GLFW_INCLUDE_VULKAN
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <GLFW/glfw3.h>

#include "hephaestus_platform_Vulkan.h"

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

VkApplicationInfo applicationInfo(JNIEnv* env, jobject info) {
  jclass cls = (*env)->GetObjectClass(env, info);
  jmethodID pan_id = (*env)->GetMethodID(env, cls, "pApplicationName", "()Ljava/lang/String;");
  jstring pan_s = (*env)->CallObjectMethod(env, info, pan_id);
  const char* pan = (*env)->GetStringUTFChars(env, pan_s, 0);
  jmethodID av_id = (*env)->GetMethodID(env, cls, "applicationVersion", "()I");
  jint av = (*env)->CallIntMethod(env, info, av_id);
  jmethodID pen_id = (*env)->GetMethodID(env, cls, "pEngineName", "()Ljava/lang/String;");
  jstring pen_s = (*env)->CallObjectMethod(env, info, pen_id);
  const char* pen = (*env)->GetStringUTFChars(env, pen_s, 0);
  jmethodID ev_id = (*env)->GetMethodID(env, cls, "engineVersion", "()I");
  jint ev = (*env)->CallIntMethod(env, info, ev_id);
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
  jmethodID pai_id = (*env)->GetMethodID(env, cls, "pApplicationInfo", 
                                         "()Lhephaestus/platform/Vulkan$ApplicationInfo;");
  jobject pai_obj = (*env)->CallObjectMethod(env, info, pai_id);
  VkApplicationInfo pai = applicationInfo(env, pai_obj);
  jmethodID elc_id = (*env)->GetMethodID(env, cls, "enabledLayerCount", "()I");
  jint elc = (*env)->CallIntMethod(env, info, elc_id);
  jmethodID eec_id = (*env)->GetMethodID(env, cls, "enabledExtensionCount", "()I");
  jint eec = (*env)->CallIntMethod(env, info, eec_id);
  jmethodID een_id = (*env)->GetMethodID(env, cls, "ppEnabledExtensionNames", "()[Ljava/lang/String;");
  jobjectArray een_objs = (*env)->CallObjectMethod(env, info, een_id);
  const char** eens = malloc(eec * sizeof(char*));
  stringEls(env, een_objs, eens, eec);
  VkInstanceCreateInfo v_info = {
    .sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO,
    .pNext = NULL,
    .flags = 0,
    .pApplicationInfo = &pai,
    .enabledExtensionCount = eec,
    .ppEnabledExtensionNames = eens,
    .enabledLayerCount = elc,
    .ppEnabledLayerNames = NULL
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
      "(I[Lhephaestus/platform/Vulkan$MemoryType;I[Lhephaestus/platform/Vulkan$MemoryHeap;)V");
  jobject obj = (*env)->NewObject(env, cls, cons_mid, ps.memoryTypeCount, ts, ps.memoryHeapCount, hs);
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
  jmethodID w_id = (*env)->GetMethodID(env, cls, "width", "()I");
  jint w = (*env)->CallIntMethod(env, e, w_id);
  jmethodID h_id = (*env)->GetMethodID(env, cls, "height", "()I");
  jint h = (*env)->CallIntMethod(env, e, h_id);
  jmethodID d_id = (*env)->GetMethodID(env, cls, "depth", "()I");
  jint d = (*env)->CallIntMethod(env, e, d_id);
  VkExtent3D ext = {
    .width = w,
    .height = h,
    .depth = d
  };
  return ext;
}

VkExtent2D toExtent2D(JNIEnv* env, jobject e) {
  jclass cls = (*env)->GetObjectClass(env, e);
  jmethodID w_id = (*env)->GetMethodID(env, cls, "width", "()I");
  jint w = (*env)->CallIntMethod(env, e, w_id);
  jmethodID h_id = (*env)->GetMethodID(env, cls, "height", "()I");
  jint h = (*env)->CallIntMethod(env, e, h_id);
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
  jmethodID count_id = (*env)->GetMethodID(env, cls, "queueCount", "()I");
  jint count = (*env)->CallIntMethod(env, info, count_id);
  jmethodID index_id = (*env)->GetMethodID(env, cls, "queueFamilyIndex", "()I");
  jint index = (*env)->CallIntMethod(env, info, index_id);
  jmethodID p_id = (*env)->GetMethodID(env, cls, "pQueuePriorities", "()[F");
  jfloatArray ps = (*env)->CallObjectMethod(env, info, p_id);
  VkDeviceQueueCreateInfo v_info = {
    .sType = VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO,
    .pNext = NULL,
    .flags = 0,
    .queueFamilyIndex = index,
    .queueCount = count,
    .pQueuePriorities = (float *) ps
  };
  return v_info;
}

void deviceQueueCreateInfos(JNIEnv* env, jobjectArray infos, VkDeviceQueueCreateInfo* v_infos, uint32_t count) {
  uint32_t i;
  for(i = 0; i < count; i++) {
    jobject info = (*env)->GetObjectArrayElement(env, infos, i);
    VkDeviceQueueCreateInfo v_info = deviceQueueCreateInfo(env, info);
    v_infos[i] = v_info;
  }
}

VkDeviceCreateInfo deviceCreateInfo(JNIEnv * env, jobject info) {
  jclass cls = (*env)->GetObjectClass(env, info);
  jmethodID info_count_id = (*env)->GetMethodID(env, cls, "queueCreateInfoCount", "()I");
  jint info_count = (*env)->CallIntMethod(env, info, info_count_id);
  jmethodID layer_count_id = (*env)->GetMethodID(env, cls, "enabledLayerCount", "()I");
  jint layer_count = (*env)->CallIntMethod(env, info, layer_count_id);
  jmethodID extension_count_id = (*env)->GetMethodID(env, cls, "enabledExtensionCount", "()I");
  jint extension_count = (*env)->CallIntMethod(env, info, extension_count_id);
  jmethodID extension_names_id = (*env)->GetMethodID(env, cls, "ppEnabledExtensionNames", "()[Ljava/lang/String;");
  CHECK(extension_names_id, "enid")
  jobjectArray extension_names_obj = (*env)->CallObjectMethod(env, info, extension_names_id);
  CHECK(extension_names_obj, "enobj")
  const char** extension_names = malloc(extension_count * sizeof(char*));
  stringEls(env, extension_names_obj, extension_names, extension_count);
  jmethodID queue_info_id = (*env)->GetMethodID(env, cls, "pQueueCreateInfos",
                    "()[Lhephaestus/platform/Vulkan$DeviceQueueCreateInfo;");
  jobjectArray queue_infos = (*env)->CallObjectMethod(env, info, queue_info_id);
  VkDeviceQueueCreateInfo* v_infos = malloc(info_count * sizeof(VkDeviceQueueCreateInfo));
  deviceQueueCreateInfos(env, (jobjectArray) queue_infos, v_infos, info_count);
  VkDeviceCreateInfo v_info = {
    .sType = VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO,
    .pNext = NULL,
    .flags = 0,
    .queueCreateInfoCount = info_count,
    .pQueueCreateInfos = v_infos,
    .enabledLayerCount = layer_count,
    .ppEnabledLayerNames = NULL,
    .enabledExtensionCount = extension_count,
    .ppEnabledExtensionNames = extension_names,
    .pEnabledFeatures = NULL
  };
  return v_info;
}

JNIEXPORT jlong JNICALL Java_hephaestus_platform_Vulkan_createDevice
(JNIEnv* env, jobject instance __attribute__((unused)), jlong pdevice, jobject info) {
  printf("about to create device info\n");
  fflush(stdout);
  VkDeviceCreateInfo v_info = deviceCreateInfo(env, info);
  printf("created device info\n");
  fflush(stdout);
  VkDevice device;
  RES(vkCreateDevice((VkPhysicalDevice) pdevice, &v_info, NULL, &device));
  printf("created vk device\n");
  fflush(stdout);
  return (jlong) device;
}


JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_destroyDevice
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong d) {
  vkDestroyDevice((VkDevice) d, NULL);
}

VkCommandPoolCreateInfo commandPoolCreateInfo(JNIEnv* env, jobject info) {
  jclass cls = (*env)->GetObjectClass(env, info);
  jmethodID f_id = (*env)->GetMethodID(env, cls, "flags", "()I");
  jint f = (*env)->CallIntMethod(env, info, f_id);
  jmethodID qfi_id = (*env)->GetMethodID(env, cls, "queueFamilyIndex", "()I");
  jint qfi = (*env)->CallIntMethod(env, info, qfi_id);
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
  jmethodID cbc_id = (*env)->GetMethodID(env, cls, "commandBufferCount", "()I");
  jint cbc = (*env)->CallIntMethod(env, info, cbc_id);
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
  printf("mode is %d %d \n", modes[0], modes[1]);
  fflush(stdout);
  jintArray ms = (*env)->NewIntArray(env, count);
  (*env)->SetIntArrayRegion(env, ms, 0, count, (const jint *) modes);
  return ms;
}

VkSwapchainCreateInfoKHR swapchainCreateInfo(JNIEnv* env, jobject info) {
  jclass cls = (*env)->GetObjectClass(env, info);
  jmethodID fs_id = (*env)->GetMethodID(env, cls, "flags", "()I");
  jint fs = (*env)->CallIntMethod(env, info, fs_id);
  jmethodID sfc_id = (*env)->GetMethodID(env, cls, "surface", "()J");
  jlong sfc = (*env)->CallLongMethod(env, info, sfc_id);
  jmethodID mic_id = (*env)->GetMethodID(env, cls, "minImageCount", "()I");
  jint mic = (*env)->CallIntMethod(env, info, mic_id);
  jmethodID ifmt_id = (*env)->GetMethodID(env, cls, "imageFormat", "()I");
  jint ifmt = (*env)->CallIntMethod(env, info, ifmt_id);
  jmethodID cs_id = (*env)->GetMethodID(env, cls, "imageColorSpace", "()I");
  jint cs = (*env)->CallIntMethod(env, info, cs_id);
  jmethodID ext_id = (*env)->GetMethodID(env, cls, "imageExtent", "()Lhephaestus/platform/Vulkan$Extent2D;");
  jobject ext_obj = (*env)->CallObjectMethod(env, info, ext_id);
  VkExtent2D ext = toExtent2D(env, ext_obj);
  jmethodID ial_id = (*env)->GetMethodID(env, cls, "imageArrayLayers", "()I");
  jint ial = (*env)->CallIntMethod(env, info, ial_id);
  jmethodID iu_id = (*env)->GetMethodID(env, cls, "imageUsage", "()I");
  jint iu = (*env)->CallIntMethod(env, info, iu_id);
  jmethodID ism_id = (*env)->GetMethodID(env, cls, "imageSharingMode", "()I");
  jlong ism = (*env)->CallIntMethod(env, info, ism_id);
  jmethodID qfic_id = (*env)->GetMethodID(env, cls, "queueFamilyIndexCount", "()I");
  jint qfic = (*env)->CallIntMethod(env, info, qfic_id);
  jmethodID pt_id = (*env)->GetMethodID(env, cls, "preTransform", "()I");
  jint pt = (*env)->CallIntMethod(env, info, pt_id);
  jmethodID ca_id = (*env)->GetMethodID(env, cls, "compositeAlpha", "()J");
  jlong ca = (*env)->CallLongMethod(env, info, ca_id);
  jmethodID pm_id = (*env)->GetMethodID(env, cls, "presentMode", "()I");
  jint pm = (*env)->CallIntMethod(env, info, pm_id);
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
    .queueFamilyIndexCount = qfic,
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
  jmethodID r_id = (*env)->GetMethodID(env, cls, "r", "()I");
  jint r = (*env)->CallIntMethod(env, cm, r_id);
  jmethodID g_id = (*env)->GetMethodID(env, cls, "g", "()I");
  jint g = (*env)->CallIntMethod(env, cm, g_id);
  jmethodID b_id = (*env)->GetMethodID(env, cls, "b", "()I");
  jint b = (*env)->CallIntMethod(env, cm, b_id);
  jmethodID a_id = (*env)->GetMethodID(env, cls, "a", "()I");
  jint a = (*env)->CallIntMethod(env, cm, a_id);
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
  jmethodID am_id = (*env)->GetMethodID(env, cls, "aspectMask", "()I");
  jint am = (*env)->CallIntMethod(env, sr, am_id);
  jmethodID bml_id = (*env)->GetMethodID(env, cls, "baseMipLevel", "()I");
  jint bml = (*env)->CallIntMethod(env, sr, bml_id);
  jmethodID lc_id = (*env)->GetMethodID(env, cls, "levelCount", "()I");
  jint lc = (*env)->CallIntMethod(env, sr, lc_id);
  jmethodID bal_id = (*env)->GetMethodID(env, cls, "baseArrayLayer", "()I");
  jint bal = (*env)->CallIntMethod(env, sr, bal_id);
  jmethodID lyrc_id = (*env)->GetMethodID(env, cls, "layerCount", "()I");
  jint lyrc = (*env)->CallIntMethod(env, sr, lyrc_id);
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
  jmethodID fs_id = (*env)->GetMethodID(env, cls, "flags", "()I");
  jint fs = (*env)->CallIntMethod(env, info, fs_id);
  jmethodID im_id = (*env)->GetMethodID(env, cls, "image", "()J");
  jlong im = (*env)->CallLongMethod(env, info, im_id);
  jmethodID vt_id = (*env)->GetMethodID(env, cls, "viewType", "()I");
  jint vt = (*env)->CallIntMethod(env, info, vt_id);
  jmethodID fmt_id = (*env)->GetMethodID(env, cls, "format", "()I");
  jint fmt = (*env)->CallIntMethod(env, info, fmt_id);
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
  jmethodID fs_id = (*env)->GetMethodID(env, cls, "flags", "()I");
  jint fs = (*env)->CallIntMethod(env, info, fs_id);
  jmethodID it_id = (*env)->GetMethodID(env, cls, "imageType", "()I");
  jint it = (*env)->CallIntMethod(env, info, it_id);
  jmethodID fmt_id = (*env)->GetMethodID(env, cls, "format", "()I");
  jint fmt = (*env)->CallIntMethod(env, info, fmt_id);
  jmethodID ext_id = (*env)->GetMethodID(env, cls, "extent", "()Lhephaestus/platform/Vulkan$Extent3D;");
  jobject ext_obj = (*env)->CallObjectMethod(env, info, ext_id);
  VkExtent3D ext = toExtent3D(env, ext_obj);
  jmethodID ml_id = (*env)->GetMethodID(env, cls, "mipLevels", "()I");
  jint ml = (*env)->CallIntMethod(env, info, ml_id);
  jmethodID al_id = (*env)->GetMethodID(env, cls, "arrayLayers", "()I");
  jint al = (*env)->CallIntMethod(env, info, al_id);
  jmethodID smp_id = (*env)->GetMethodID(env, cls, "samples", "()I");
  jint smp = (*env)->CallIntMethod(env, info, smp_id);
  jmethodID tl_id = (*env)->GetMethodID(env, cls, "tiling", "()I");
  jint tl = (*env)->CallIntMethod(env, info, tl_id);
  jmethodID us_id = (*env)->GetMethodID(env, cls, "usage", "()I");
  jint us = (*env)->CallIntMethod(env, info, us_id);
  jmethodID sm_id = (*env)->GetMethodID(env, cls, "sharingMode", "()I");
  jint sm = (*env)->CallIntMethod(env, info, sm_id);
  jmethodID qfic_id = (*env)->GetMethodID(env, cls, "queueFamilyIndexCount", "()I");
  jint qfic = (*env)->CallIntMethod(env, info, qfic_id);
  jmethodID qfics_id = (*env)->GetMethodID(env, cls, "pQueueFamilyIndices", "()[I");
  jfloatArray qfics = (*env)->CallObjectMethod(env, info, qfics_id);
  jmethodID il_id = (*env)->GetMethodID(env, cls, "initialLayout", "()I");
  jint il = (*env)->CallIntMethod(env, info, il_id);
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
    .queueFamilyIndexCount = qfic,
    .pQueueFamilyIndices = NULL,
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
  jmethodID mti_id = (*env)->GetMethodID(env, cls, "memoryTypeIndex", "()I");
  jint mti = (*env)->CallIntMethod(env, info, mti_id);
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
  jmethodID fs_id = (*env)->GetMethodID(env, cls, "flags", "()I");
  jint fs = (*env)->CallIntMethod(env, info, fs_id);
  jmethodID s_id = (*env)->GetMethodID(env, cls, "size", "()J");
  jlong s = (*env)->CallLongMethod(env, info, s_id);
  jmethodID sm_id = (*env)->GetMethodID(env, cls, "sharingMode", "()I");
  jint sm = (*env)->CallIntMethod(env, info, sm_id);
  jmethodID qfic_id = (*env)->GetMethodID(env, cls, "queueFamilyIndexCount", "()I");
  jint qfic = (*env)->CallIntMethod(env, info, qfic_id);
  VkBufferCreateInfo v_info = {
    .sType = VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO,
    .pNext = NULL,
    .flags = fs,
    .size = s,
    .sharingMode = sm,
    .queueFamilyIndexCount = qfic,
    .pQueueFamilyIndices = NULL
  };
  return v_info;
}

JNIEXPORT jlong JNICALL Java_hephaestus_platform_Vulkan_createBuffer
(JNIEnv* env, jobject instance __attribute__((unused)), jlong device, jobject info) {
  VkBufferCreateInfo v_info = toBufferCreateInfo(env, info);
  VkBuffer buf;
  vkCreateBuffer((VkDevice) device, &v_info, NULL, &buf);
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
  vkMapMemory((VkDevice) device, (VkDeviceMemory) memory, offset, size, flags, (void **)&pData);
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
  vkBindBufferMemory( (VkDevice) device, (VkBuffer) buffer, (VkDeviceMemory) memory, (VkDeviceSize) offset);
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_destroyBuffer
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong device, jlong buffer) {
  vkDestroyBuffer( (VkDevice) device, (VkBuffer) buffer, NULL);
}

VkDescriptorSetLayoutBinding toDescriptorSetLayoutBinding(JNIEnv* env, jobject b) {
  jclass cls = (*env)->GetObjectClass(env, b);
  jmethodID bd_id = (*env)->GetMethodID(env, cls, "binding", "()I");
  jint bd = (*env)->CallIntMethod(env, b, bd_id);
  jmethodID dt_id = (*env)->GetMethodID(env, cls, "descriptorType", "()I");
  jint dt = (*env)->CallIntMethod(env, b, dt_id);
  jmethodID dc_id = (*env)->GetMethodID(env, cls, "descriptorCount", "()I");
  jint dc = (*env)->CallIntMethod(env, b, dc_id);
  jmethodID sf_id = (*env)->GetMethodID(env, cls, "stageFlags", "()I");
  jint sf = (*env)->CallIntMethod(env, b, sf_id);
  jmethodID ss_id = (*env)->GetMethodID(env, cls, "pImmutableSamplers", "()[Lhephaestus/platform/Vulkan$Sampler;");
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

void toDescriptorSetLayoutBindings(JNIEnv* env, jobjectArray b_objs, VkDescriptorSetLayoutBinding* bs, uint32_t count) {
  uint32_t i;
  for(i = 0; i < count; i++) {
    jobject b = (*env)->GetObjectArrayElement(env, b_objs, i);
    VkDescriptorSetLayoutBinding v_b = toDescriptorSetLayoutBinding(env, b);
    bs[i] = v_b;
  }
}

VkDescriptorSetLayoutCreateInfo toDescriptorSetLayoutCreateInfo(JNIEnv* env, jobject info) {
  jclass cls = (*env)->GetObjectClass(env, info);
  jmethodID fs_id = (*env)->GetMethodID(env, cls, "flags", "()I");
  jint fs = (*env)->CallIntMethod(env, info, fs_id);
  jmethodID bc_id = (*env)->GetMethodID(env, cls, "bindingCount", "()I");
  jint bc = (*env)->CallIntMethod(env, info, bc_id);
  jmethodID bs_id = (*env)->GetMethodID(env, cls, "pBindings", "()[Lhephaestus/platform/Vulkan$DescriptorSetLayoutBinding;");
  jobjectArray bs_objs = (*env)->CallObjectMethod(env, info, bs_id);
  VkDescriptorSetLayoutBinding* bs = malloc(bc * sizeof(VkDescriptorSetLayoutBinding));
  toDescriptorSetLayoutBindings(env, bs_objs, bs, bc);
  VkDescriptorSetLayoutCreateInfo v_info = {
    .sType = VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO,
    .flags = fs,
    .pNext = NULL,
    .bindingCount = bc,
    .pBindings = bs
  };
  return v_info;
}

JNIEXPORT jlong JNICALL Java_hephaestus_platform_Vulkan_createDescriptorSetLayout
(JNIEnv* env, jobject instance __attribute__((unused)), jlong device, jobject info) {
  VkDescriptorSetLayoutCreateInfo v_info = toDescriptorSetLayoutCreateInfo(env, info);
  VkDescriptorSetLayout layout;
  vkCreateDescriptorSetLayout((VkDevice) device, &v_info, NULL, &layout);
  return (jlong) layout;
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_destroyDescriptorSetLayout
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong device, jlong desc) {
  vkDestroyDescriptorSetLayout((VkDevice) device, (VkDescriptorSetLayout) desc, NULL);
}

VkPushConstantRange toPushConstantRange(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  jmethodID fs_id = (*env)->GetMethodID(env, cls, "stageFlags", "()I");
  jint fs = (*env)->CallIntMethod(env, o, fs_id);
  jmethodID os_id = (*env)->GetMethodID(env, cls, "offset", "()I");
  jint os = (*env)->CallIntMethod(env, o, os_id);
  jmethodID sz_id = (*env)->GetMethodID(env, cls, "size", "()I");
  jint sz = (*env)->CallIntMethod(env, o, sz_id);
  VkPushConstantRange v_range = {
    .stageFlags = fs,
    .offset = os,
    .size = sz
  };
  return v_range;
}

void toPushConstantRanges(JNIEnv* env, jobjectArray objs, VkPushConstantRange* rs, uint32_t count) {
  uint32_t i;
  for(i = 0; i < count; i++) {
    jobject o = (*env)->GetObjectArrayElement(env, objs, i);
    VkPushConstantRange r = toPushConstantRange(env, o);
    rs[i] = r;
  }
}

VkDescriptorSetLayout toDescriptorSetLayout(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  jmethodID ptr_id = (*env)->GetMethodID(env, cls, "ptr", "()J");
  jlong ptr = (*env)->CallLongMethod(env, o, ptr_id);
  return (VkDescriptorSetLayout) ptr;
}

void toDescriptorSetLayouts(JNIEnv* env, jobject objs, VkDescriptorSetLayout* ls, uint32_t count) {
  uint32_t i;
  for(i = 0; i < count; i++) {
    jobject o = (*env)->GetObjectArrayElement(env, objs, i);
    VkDescriptorSetLayout l = toDescriptorSetLayout(env, o);
    ls[i] = l;
  }
}

VkPipelineLayoutCreateInfo toPipelineLayoutCreateInfo(JNIEnv* env, jobject info) {
  jclass cls = (*env)->GetObjectClass(env, info);
  jmethodID fs_id = (*env)->GetMethodID(env, cls, "flags", "()I");
  jint fs = (*env)->CallIntMethod(env, info, fs_id);
  jmethodID slc_id = (*env)->GetMethodID(env, cls, "setLayoutCount", "()I");
  jint slc = (*env)->CallIntMethod(env, info, slc_id);
  jmethodID sls_id = (*env)->GetMethodID(env, cls, "pSetLayouts", "()[Lhephaestus/platform/Vulkan$DescriptorSetLayout;");
  jobjectArray sls_objs = (*env)->CallObjectMethod(env, info, sls_id);
  VkDescriptorSetLayout* sls = malloc(slc * sizeof(VkDescriptorSetLayout));
  toDescriptorSetLayouts(env, sls_objs, sls, slc);
  jmethodID pcrc_id = (*env)->GetMethodID(env, cls, "pushConstantRangeCount", "()I");
  jint pcrc = (*env)->CallIntMethod(env, info, pcrc_id);
  jmethodID pcrs_id = (*env)->GetMethodID(env, cls, "pPushConstantRanges", "()[I");
  jobjectArray pcrs_objs = (*env)->CallObjectMethod(env, info, pcrs_id);
  VkPushConstantRange* pcrs = malloc(pcrc * sizeof(VkPushConstantRange));
  toPushConstantRanges(env, pcrs_objs, pcrs, pcrc);
  VkPipelineLayoutCreateInfo v_info = {
    .sType = VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO,
    .pNext = NULL,
    .flags = fs,
    .pushConstantRangeCount = pcrc,
    .pPushConstantRanges = pcrs,
    .setLayoutCount = slc,
    .pSetLayouts = sls
  };
  return v_info;
}

JNIEXPORT jlong JNICALL Java_hephaestus_platform_Vulkan_createPipelineLayout
(JNIEnv* env, jobject instance __attribute__((unused)), jlong device, jobject info) {
  VkPipelineLayoutCreateInfo v_info = toPipelineLayoutCreateInfo(env, info);
  VkPipelineLayout layout;
  vkCreatePipelineLayout((VkDevice) device, &v_info, NULL, &layout);
  return (jlong) layout;
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_destroyPipelineLayout
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong device, jlong pipeline) {
  vkDestroyPipelineLayout((VkDevice) device, (VkPipelineLayout) pipeline, NULL);
}

VkDescriptorPoolSize toDescriptorPoolSize(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  jmethodID t_id = (*env)->GetMethodID(env, cls, "tpe", "()I");
  jint t = (*env)->CallIntMethod(env, o, t_id);
  jmethodID c_id = (*env)->GetMethodID(env, cls, "descriptorCount", "()I");
  jint c = (*env)->CallIntMethod(env, o, c_id);
  VkDescriptorPoolSize p = {
    .type = t,
    .descriptorCount = c
  };
  return p;
}

void toDescriptorPoolSizes(JNIEnv* env, jobjectArray objs, VkDescriptorPoolSize* vs, uint32_t count) {
  uint32_t i;
  for(i = 0; i < count; i++) {
    jobject o = (*env)->GetObjectArrayElement(env, objs, i);
    VkDescriptorPoolSize v = toDescriptorPoolSize(env, o);
    vs[i] = v;
  }
}

VkDescriptorPoolCreateInfo toDescriptorPoolCreateInfo(JNIEnv* env, jobject info) {
  jclass cls = (*env)->GetObjectClass(env, info);
  jmethodID fs_id = (*env)->GetMethodID(env, cls, "flags", "()I");
  jint fs = (*env)->CallIntMethod(env, info, fs_id);
  jmethodID ms_id = (*env)->GetMethodID(env, cls, "maxSets", "()I");
  jint ms = (*env)->CallIntMethod(env, info, ms_id);
  jmethodID psc_id = (*env)->GetMethodID(env, cls, "poolSizeCount", "()I");
  jint psc = (*env)->CallIntMethod(env, info, psc_id);
  jmethodID pss_id = (*env)->GetMethodID(env, cls, "pPoolSizes", "()[Lhephaestus/platform/Vulkan$DescriptorPoolSize;");
  jobjectArray pss_objs = (*env)->CallObjectMethod(env, info, pss_id);
  VkDescriptorPoolSize* pss = malloc(psc * sizeof(VkDescriptorPoolSize));
  toDescriptorPoolSizes(env, pss_objs, pss, psc);
  VkDescriptorPoolCreateInfo v_info = {
    .sType = VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO,
    .pNext = NULL,
    .flags = fs,
    .maxSets = ms,
    .poolSizeCount = psc,
    .pPoolSizes = pss
  };
  return v_info;
}

JNIEXPORT jlong JNICALL Java_hephaestus_platform_Vulkan_createDescriptorPool
(JNIEnv* env, jobject instance __attribute__((unused)), jlong device, jobject info) {
  VkDescriptorPoolCreateInfo v_info = toDescriptorPoolCreateInfo(env, info);
  VkDescriptorPool pool;
  vkCreateDescriptorPool((VkDevice) device, &v_info, NULL, &pool);
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
  jmethodID dsc_id = (*env)->GetMethodID(env, cls, "descriptorSetCount", "()I");
  jint dsc = (*env)->CallIntMethod(env, info, dsc_id);
  jmethodID sls_id = (*env)->GetMethodID(env, cls, "pSetLayouts", "()[Lhephaestus/platform/Vulkan$DescriptorSetLayout;");
  jobjectArray sls_objs = (*env)->CallObjectMethod(env, info, sls_id);
  VkDescriptorSetLayout* sls = malloc(dsc * sizeof(VkDescriptorSetLayout));
  toDescriptorSetLayouts(env, sls_objs, sls, dsc);
  VkDescriptorSetAllocateInfo v_info = {
    .sType = VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO,
    .pNext = NULL,
    .descriptorPool = (VkDescriptorPool) dp,
    .descriptorSetCount = dsc,
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
  vkAllocateDescriptorSets((VkDevice) device, &v_info, sets);
  jobjectArray set_objs = fromDescriptorSets(env, sets, v_info.descriptorSetCount);
  return set_objs;
}

VkDescriptorSet toDescriptorSet(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  jmethodID ptr_id = (*env)->GetMethodID(env, cls, "ptr", "()J");
  jlong ptr = (*env)->CallLongMethod(env, o, ptr_id);
  return (VkDescriptorSet) ptr;
}

void toDescriptorSets(JNIEnv* env, jobjectArray objs, VkDescriptorSet* vs, uint32_t count) {
  uint32_t i;
  for(i = 0; i < count; i++) {
    jobject o = (*env)->GetObjectArrayElement(env, objs, i);
    VkDescriptorSet v = toDescriptorSet(env, o);
    vs[i] = v;
  }
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_freeDescriptorSets
(JNIEnv* env, jobject instance __attribute__((unused)), jlong device, jlong pool, jint count, jobjectArray sets) {
  VkDescriptorSet* v_sets = malloc(count * sizeof(VkDescriptorSet));
  toDescriptorSets(env, sets, v_sets, count);
  vkFreeDescriptorSets((VkDevice) device, (VkDescriptorPool) pool, count, v_sets);
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

VkWriteDescriptorSet toWriteDescriptorSet(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  jmethodID dsts_id = (*env)->GetMethodID(env, cls, "dstSet", "()J");
  jlong dsts = (*env)->CallLongMethod(env, o, dsts_id);
  jmethodID dstb_id = (*env)->GetMethodID(env, cls, "dstBinding", "()I");
  jint dstb = (*env)->CallIntMethod(env, o, dstb_id);
  jmethodID dsta_id = (*env)->GetMethodID(env, cls, "dstArrayElement", "()I");
  jint dsta = (*env)->CallIntMethod(env, o, dsta_id);
  jmethodID dc_id = (*env)->GetMethodID(env, cls, "descriptorCount", "()I");
  jint dc = (*env)->CallIntMethod(env, o, dc_id);
  jmethodID dt_id = (*env)->GetMethodID(env, cls, "descriptorType", "()I");
  jint dt = (*env)->CallIntMethod(env, o, dt_id);
  jmethodID bis_id = (*env)->GetMethodID(env, cls, "pBufferInfo", "()[Lhephaestus/platform/Vulkan$DescriptorBufferInfo;");
  jobjectArray bis_objs = (*env)->CallObjectMethod(env, o, bis_id);
  //TODO: how does vulkan know how big this should be?
  VkDescriptorBufferInfo* bis = malloc(sizeof(VkDescriptorBufferInfo));
  toDescriptorBufferInfos(env, bis_objs, bis, 1);
  VkWriteDescriptorSet v_set = {
    .sType = VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET,
    .pNext = NULL,
    .dstSet = (VkDescriptorSet) dsts,
    .dstBinding = dstb,
    .dstArrayElement = dsta,
    .descriptorCount = dc,
    .descriptorType = (VkDescriptorType) dt,
    .pImageInfo = NULL,
    .pBufferInfo = bis,
    .pTexelBufferView = NULL
  };
  return v_set;
}

void toWriteDescriptorSets(JNIEnv* env, jobjectArray objs, VkWriteDescriptorSet* vs, uint32_t count) {
  uint32_t i;
  for(i = 0; i < count; i++) {
    jobject o = (*env)->GetObjectArrayElement(env, objs, i);
    VkWriteDescriptorSet v = toWriteDescriptorSet(env, o);
    vs[i] = v;
  }
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_updateDescriptorSets
(JNIEnv* env, jobject instance __attribute__((unused)), jlong device, jint w_count, jobjectArray writes, jint c_count, jobjectArray copies) {
  VkWriteDescriptorSet* v_writes = malloc(w_count * sizeof(VkWriteDescriptorSet));
  toWriteDescriptorSets(env, writes, v_writes, w_count);
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
  vkCreateSemaphore((VkDevice) device, &v_info, NULL, &semaphore);
  return (jlong) semaphore;
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_destroySemaphore
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong device, jlong semaphore) {
  vkDestroySemaphore((VkDevice) device, (VkSemaphore) semaphore, NULL);
}

JNIEXPORT jint JNICALL Java_hephaestus_platform_Vulkan_acquireNextImageKHR
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong device, jlong swapchain, jlong timeout, jlong semaphore, jlong fence) {
  uint32_t i;
  vkAcquireNextImageKHR((VkDevice) device, (VkSwapchainKHR) swapchain, timeout, (VkSemaphore) semaphore, (VkFence) fence, &i);
  return i;
}

VkAttachmentDescription toAttachmentDescription(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  jmethodID fs_id = (*env)->GetMethodID(env, cls, "flags", "()I");
  jint fs = (*env)->CallIntMethod(env, o, fs_id);
  jmethodID fmt_id = (*env)->GetMethodID(env, cls, "format", "()I");
  jint fmt = (*env)->CallIntMethod(env, o, fmt_id);
  jmethodID smp_id = (*env)->GetMethodID(env, cls, "samples", "()I");
  jint smp = (*env)->CallIntMethod(env, o, smp_id);
  jmethodID lo_id = (*env)->GetMethodID(env, cls, "loadOp", "()I");
  jint lo = (*env)->CallIntMethod(env, o, lo_id);
  jmethodID so_id = (*env)->GetMethodID(env, cls, "storeOp", "()I");
  jint so = (*env)->CallIntMethod(env, o, so_id);
  jmethodID slo_id = (*env)->GetMethodID(env, cls, "stencilLoadOp", "()I");
  jint slo = (*env)->CallIntMethod(env, o, slo_id);
  jmethodID sso_id = (*env)->GetMethodID(env, cls, "stencilStoreOp", "()I");
  jint sso = (*env)->CallIntMethod(env, o, sso_id);
  jmethodID il_id = (*env)->GetMethodID(env, cls, "initialLayout", "()I");
  jint il = (*env)->CallIntMethod(env, o, il_id);
  jmethodID fl_id = (*env)->GetMethodID(env, cls, "finalLayout", "()I");
  jint fl = (*env)->CallIntMethod(env, o, fl_id);
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

void toAttachmentDescriptions(JNIEnv* env, jobjectArray objs, VkAttachmentDescription* vs, uint32_t count) {
  uint32_t i;
  for(i = 0; i < count; i++) {
    jobject o = (*env)->GetObjectArrayElement(env, objs, i);
    VkAttachmentDescription v = toAttachmentDescription(env, o);
    vs[i] = v;
  }
}

VkAttachmentReference toAttachmentReference(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  jmethodID at_id = (*env)->GetMethodID(env, cls, "attachment", "()I");
  jint at = (*env)->CallIntMethod(env, o, at_id);
  jmethodID il_id = (*env)->GetMethodID(env, cls, "layout", "()I");
  jint il = (*env)->CallIntMethod(env, o, il_id);
  VkAttachmentReference d = {
    .attachment = at,
    .layout = il
  };
  return d;
}

void toAttachmentReferences(JNIEnv* env, jobjectArray objs, VkAttachmentReference* vs, uint32_t count) {
  uint32_t i;
  for(i = 0; i < count; i++) {
    jobject o = (*env)->GetObjectArrayElement(env, objs, i);
    VkAttachmentReference v = toAttachmentReference(env, o);
    vs[i] = v;
  }
}
                   
VkSubpassDescription toSubpassDescription(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  jmethodID fs_id = (*env)->GetMethodID(env, cls, "flags", "()I");
  jint fs = (*env)->CallIntMethod(env, o, fs_id);
  jmethodID pbp_id = (*env)->GetMethodID(env, cls, "pipelineBindPoint", "()I");
  jint pbp = (*env)->CallIntMethod(env, o, pbp_id);
  jmethodID iac_id = (*env)->GetMethodID(env, cls, "inputAttachmentCount", "()I");
  jint iac = (*env)->CallIntMethod(env, o, iac_id);
  jmethodID ias_id = (*env)->GetMethodID(env, cls, "pInputAttachments", "()[Lhephaestus/platform/Vulkan$AttachmentReference;");
  jobjectArray ias_objs = (*env)->CallObjectMethod(env, o, ias_id);
  VkAttachmentReference* ias = malloc(iac * sizeof(VkAttachmentReference));
  toAttachmentReferences(env, ias_objs, ias, iac);
  jmethodID cac_id = (*env)->GetMethodID(env, cls, "colorAttachmentCount", "()I");
  jint cac = (*env)->CallIntMethod(env, o, cac_id);
  jmethodID cas_id = (*env)->GetMethodID(env, cls, "pColorAttachments", "()[Lhephaestus/platform/Vulkan$AttachmentReference;");
  jobjectArray cas_objs = (*env)->CallObjectMethod(env, o, cas_id);
  VkAttachmentReference* cas = malloc(cac * sizeof(VkAttachmentReference));
  toAttachmentReferences(env, cas_objs, cas, cac);
  jmethodID das_id = (*env)->GetMethodID(env, cls, "pDepthStencilAttachment", "()[Lhephaestus/platform/Vulkan$AttachmentReference;");
  jobjectArray das_objs = (*env)->CallObjectMethod(env, o, das_id);
  VkAttachmentReference* das = malloc(sizeof(VkAttachmentReference));
  toAttachmentReferences(env, das_objs, das, 1);
  jmethodID pac_id = (*env)->GetMethodID(env, cls, "preserveAttachmentCount", "()I");
  jint pac = (*env)->CallIntMethod(env, o, pac_id);
  jmethodID pas_id = (*env)->GetMethodID(env, cls, "pPreserveAttachments", "()[I");
  jintArray pas = (jintArray)(*env)->CallObjectMethod(env, o, pas_id);

  VkSubpassDescription d = {
    .flags = fs,
    .pipelineBindPoint = pbp,
    .inputAttachmentCount = iac,
    .pInputAttachments = ias,
    .colorAttachmentCount = cac,
    .pColorAttachments = cas,
    .pDepthStencilAttachment = das,
    .preserveAttachmentCount = pac,
    .pPreserveAttachments = NULL
  };
  return d;
}

void toSubpassDescriptions(JNIEnv* env, jobjectArray objs, VkSubpassDescription* vs, uint32_t count) {
  uint32_t i;
  for(i = 0; i < count; i++) {
    jobject o = (*env)->GetObjectArrayElement(env, objs, i);
    VkSubpassDescription v = toSubpassDescription(env, o);
    vs[i] = v;
  }
}

VkRenderPassCreateInfo toRenderPassCreateInfo(JNIEnv* env, jobject o) {
  jclass cls = (*env)->GetObjectClass(env, o);
  jmethodID fs_id = (*env)->GetMethodID(env, cls, "flags", "()I");
  jint fs = (*env)->CallIntMethod(env, o, fs_id);
  jmethodID ac_id = (*env)->GetMethodID(env, cls, "attachmentCount", "()I");
  jint ac = (*env)->CallIntMethod(env, o, ac_id);
  jmethodID as_id = (*env)->GetMethodID(env, cls, "pAttachments", "()[Lhephaestus/platform/Vulkan$AttachmentDescription;");
  jobjectArray as_objs = (*env)->CallObjectMethod(env, o, as_id);
  VkAttachmentDescription* as = malloc(ac * sizeof(VkAttachmentDescription));
  toAttachmentDescriptions(env, as_objs, as, ac);
  jmethodID sc_id = (*env)->GetMethodID(env, cls, "subpassCount", "()I");
  jint sc = (*env)->CallIntMethod(env, o, sc_id);
  jmethodID ss_id = (*env)->GetMethodID(env, cls, "pSubpasses", "()[Lhephaestus/platform/Vulkan$SubpassDescription;");
  jobjectArray ss_objs = (*env)->CallObjectMethod(env, o, ss_id);
  VkSubpassDescription* ss = malloc(sc * sizeof(VkSubpassDescription));
  toSubpassDescriptions(env, ss_objs, ss, sc);
  jmethodID dc_id = (*env)->GetMethodID(env, cls, "dependencyCount", "()I");
  jint dc = (*env)->CallIntMethod(env, o, dc_id);
  VkRenderPassCreateInfo v_info = {
    .sType = VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO,
    .pNext = NULL,
    .flags = fs,
    .attachmentCount = ac,
    .pAttachments = as,
    .subpassCount = sc,
    .pSubpasses = ss,
    .dependencyCount = dc,
    .pDependencies = NULL
  };
  return v_info;
}

JNIEXPORT jlong JNICALL Java_hephaestus_platform_Vulkan_createRenderPass
(JNIEnv* env, jobject instance __attribute__((unused)), jlong device, jobject info) {
  VkRenderPassCreateInfo v_info = toRenderPassCreateInfo(env, info);
  VkRenderPass p;
  vkCreateRenderPass((VkDevice) device, &v_info, NULL, &p);
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
  jmethodID pc_id = (*env)->GetMethodID(env, cls, "pCode", "()[I");
  jintArray pc = (jintArray) (*env)->CallObjectMethod(env, o, pc_id);
  VkShaderModuleCreateInfo v_info = {
    .sType = VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO,
    .pNext = NULL,
    .flags = fs,
    .codeSize = cs,
    .pCode = (uint32_t*) pc
  };
  return v_info;
}

JNIEXPORT jlong JNICALL Java_hephaestus_platform_Vulkan_createShaderModule
(JNIEnv* env, jobject instance __attribute__((unused)), jlong device, jobject info) {
  VkShaderModuleCreateInfo v_info = toShaderModuleCreateInfo(env, info);
  VkShaderModule module;
  vkCreateShaderModule((VkDevice) device, &v_info, NULL, &module);
  return (jlong) module;
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_destroyShaderModule
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong device, jlong module) {
  vkDestroyShaderModule((VkDevice) device, (VkShaderModule) module, NULL);
}


JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_beginCommandBuffer
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong buffer, jint info) {
  VkCommandBufferBeginInfo v_info = {
    .sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO,
    .pNext = NULL,
    .flags = info,
    .pInheritanceInfo = NULL
  };
  vkBeginCommandBuffer((VkCommandBuffer) buffer, &v_info);
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_endCommandBuffer
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong buffer) {
  vkEndCommandBuffer((VkCommandBuffer) buffer);
}

JNIEXPORT jlong JNICALL Java_hephaestus_platform_Vulkan_createFence
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong device, jint info) {
  VkFenceCreateInfo v_info = {
    .sType = VK_STRUCTURE_TYPE_FENCE_CREATE_INFO,
    .pNext = NULL,
    .flags = info
  };
  VkFence fence;
  vkCreateFence((VkDevice) device, &v_info, NULL, &fence);
  return fence;
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_destroyFence
(JNIEnv* env __attribute__((unused)), jobject device __attribute__((unused)), jlong device, jlong fence) {
  vkDestroyFence((VkDevice) device, (VkFence) fence, NULL);
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_queueSubmit
  (JNIEnv *, jobject, jlong, jint, jobjectArray, jlong);

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_waitForFences
  (JNIEnv *, jobject, jlong, jint, jobjectArray, jboolean, jlong);

JNIEXPORT jlong JNICALL Java_hephaestus_platform_Vulkan_createFramebuffer
  (JNIEnv *, jobject, jlong, jobject);

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_destroyFramebuffer
  (JNIEnv *, jobject, jlong, jlong);

JNIEXPORT jlong JNICALL Java_hephaestus_platform_Vulkan_getDeviceQueue
  (JNIEnv *, jobject, jlong, jint, jint);
