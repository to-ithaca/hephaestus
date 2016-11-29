/* C source files to proxy Vulkan calls */
#define GLFW_INCLUDE_VULKAN
#include <stdlib.h>
#include <stdio.h>
#include <GLFW/glfw3.h>

#include "hephaestus_platform_Vulkan.h"

#define CHECK(v, m) if(v == NULL) { \
    fprintf(stderr, "%s was null!\n", m); \
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

void stringEls(JNIEnv* env, jobjectArray ss, char** c_ss, uint32_t count) {
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
                                         "()Lhephaestus/platform/Vulkan\$ApplicationInfo;");
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
  vkCreateInstance(&v_info, NULL, &inst);
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
    vkEnumeratePhysicalDevices((VkInstance) inst, &gpu_count, NULL);
    VkPhysicalDevice gpus[gpu_count];
    vkEnumeratePhysicalDevices((VkInstance) inst, &gpu_count, gpus);
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
                    "()[Lhephaestus/platform/Vulkan\$DeviceQueueCreateInfo;");
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
  vkCreateDevice((VkPhysicalDevice) pdevice, &v_info, NULL, &device);
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
  vkCreateCommandPool((VkDevice) device, &v_info, NULL, &p);
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
  vkAllocateCommandBuffers((VkDevice) device, &v_info, &cmd);
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
(JNIEnv* env, jobject instance __attribute__((unused)), jlong device, jint index, jlong surface) {
    uint32_t b;
    vkGetPhysicalDeviceSurfaceSupportKHR(device, index, surface, &b);
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
  vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, &count, NULL);
  VkSurfaceFormatKHR formats[count];
  vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, &count, formats);
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
(JNIEnv* env, jobject instance, jlong device, jlong surface) {
  VkSurfaceCapabilitiesKHR caps;
  vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device, surface, &caps);
  return surfaceCapabilities(env, caps);
}


JNIEXPORT jintArray JNICALL Java_hephaestus_platform_Vulkan_getPhysicalDeviceSurfacePresentModes
(JNIEnv* env, jobject instance __attribute__((unused)), jlong device, jlong surface) {
  uint32_t count;
  vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface, &count, NULL);
  VkPresentModeKHR* modes = (VkPresentModeKHR*) malloc(count * sizeof(VkPresentModeKHR));
  vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface, &count, modes);
  printf("mode is %d %d \n", modes[0], modes[1]);
  fflush(stdout);
  jintArray ms = (*env)->NewIntArray(env, count);
  (*env)->SetIntArrayRegion(env, ms, 0, count, modes);
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
  jmethodID ism_id = (*env)->GetMethodID(env, cls, "imageSharingMode", "()J");
  jlong ism = (*env)->CallLongMethod(env, info, ism_id);
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
    .surface = sfc,
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
  vkCreateSwapchainKHR((VkDevice) device, &v_info, NULL, &s); 
  return (long) s;
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_destroySwapchain
(JNIEnv* env __attribute__((unused)), jobject instance __attribute__((unused)), jlong device, jlong swapchain) {
  vkDestroySwapchainKHR(device, swapchain, NULL);
}

jobjectArray fromImages(JNIEnv* env, uint32_t count, VkImage* ims) {
  jclass cls = (*env)->FindClass(env, "hephaestus/platform/Vulkan$Image");
  jmethodID cons_id = (*env)->GetMethodID(env, cls, "<init>", "(J)V");
  jobject obj0 = (*env)->NewObject(env, cls, cons_id, ims[0]);
  jobjectArray objs = (*env)->NewObjectArray(env, count, cls, obj0);
  uint32_t i;
  for(i = 1; i < count; i++) {
    VkImage im = ims[i];
    jobject obj = (*env)->NewObject(env, cls, cons_id, ims[i]);
    (*env)->SetObjectArrayElement(env, objs, i, obj);
  }
  return objs;
}

JNIEXPORT jobjectArray JNICALL Java_hephaestus_platform_Vulkan_getSwapchainImages
(JNIEnv* env, jobject instance __attribute__((unused)), jlong device, jlong swapchain) {
  uint32_t count;
  vkGetSwapchainImagesKHR(device, swapchain, &count, NULL);
  VkImage* is = malloc(count * sizeof(VkImage));
  vkGetSwapchainImagesKHR(device, swapchain, &count, is);
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
    .image = im,
    .viewType = vt,
    .format = fmt,
    .components = cm,
    .subresourceRange = sr
  };
  return v_info;
}

JNIEXPORT jlong JNICALL Java_hephaestus_platform_Vulkan_createImageView
(JNIEnv* env, jobject instance, jlong device, jobject info) {
  VkImageViewCreateInfo v_info = toImageViewCreateInfo(env, info);
  VkImageView* view;
  vkCreateImageView(device, &v_info, NULL, &view); 
  return view;
}

JNIEXPORT void JNICALL Java_hephaestus_platform_Vulkan_destroyImageView
(JNIEnv* env, jobject instance, jlong device, jlong view) {
  vkDestroyImageView(device, view, NULL);
}

