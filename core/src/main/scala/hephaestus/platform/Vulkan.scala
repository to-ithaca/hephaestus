package hephaestus
package platform

import ch.jodersky.jni.nativeLoader

@nativeLoader("hephaestus0")
class Vulkan {
  @native def createInstance(info: Vulkan.InstanceCreateInfo): Vulkan.Instance
  @native def destroyInstance(inst: Vulkan.Instance): Unit
  @native def enumeratePhysicalDevices(inst: Vulkan.Instance): Array[Vulkan.PhysicalDevice]
  @native def getPhysicalDeviceMemoryProperties(device: Vulkan.PhysicalDevice): Vulkan.PhysicalDeviceMemoryProperties
  @native def getPhysicalDeviceQueueFamilyProperties(device: Vulkan.PhysicalDevice): Array[Vulkan.QueueFamilyProperties]
  @native def createDevice(d: Vulkan.PhysicalDevice, info: Vulkan.DeviceCreateInfo): Vulkan.Device
  @native def destroyDevice(d: Vulkan.Device): Unit
  @native def createCommandPool(device: Vulkan.Device, info: Vulkan.CommandPoolCreateInfo): Vulkan.CommandPool 
  @native def destroyCommandPool(device: Vulkan.Device, pool: Vulkan.CommandPool): Unit
  @native def allocateCommandBuffers(device: Vulkan.Device, info: Vulkan.CommandBufferAllocateInfo): Vulkan.CommandBuffer
  @native def freeCommandBuffers(device: Vulkan.Device, pool: Vulkan.CommandPool, count: Int, buffer: Vulkan.CommandBuffer): Unit 

  @native def destroySurfaceKHR(inst: Vulkan.Instance, surface: Vulkan.Surface): Unit
  @native def getPhysicalDeviceSurfaceSupport(device: Vulkan.PhysicalDevice, queueFamilyIndex: Int, surface: Vulkan.Surface): Boolean
  @native def getPhysicalDeviceSurfaceFormats(device: Vulkan.PhysicalDevice, surface: Vulkan.Surface): Array[Vulkan.SurfaceFormat]
  @native def getPhysicalDeviceSurfaceCapabilities(device: Vulkan.PhysicalDevice, surface: Vulkan.Surface): Vulkan.SurfaceCapabilities
  @native def getPhysicalDeviceSurfacePresentModes(device: Vulkan.PhysicalDevice, surface: Vulkan.Surface): Array[Int]

  @native def createSwapchain(device: Vulkan.Device, info: Vulkan.SwapchainCreateInfo): Vulkan.Swapchain
  @native def destroySwapchain(device: Vulkan.Device, swapchain: Vulkan.Swapchain): Unit
  @native def getSwapchainImages(device: Vulkan.Device, swapchain: Vulkan.Swapchain): Array[Vulkan.Image]
  @native def createImageView(device: Vulkan.Device, info: Vulkan.ImageViewCreateInfo): Vulkan.ImageView
  @native def destroyImageView(device: Vulkan.Device, imageView: Vulkan.ImageView): Unit
  
  @native def getPhysicalDeviceFormatProperties(device: Vulkan.PhysicalDevice, format: Vulkan.Format): Vulkan.FormatProperties
  @native def createImage(device: Vulkan.Device, info: Vulkan.ImageCreateInfo): Vulkan.Image
  @native def destroyImage(device: Vulkan.Device, image: Vulkan.Image): Unit
  @native def getImageMemoryRequirements(device: Vulkan.Device, image: Vulkan.Image): Vulkan.MemoryRequirements
  @native def allocateMemory(device: Vulkan.Device, info: Vulkan.MemoryAllocateInfo): Vulkan.DeviceMemory
  @native def bindImageMemory(device: Vulkan.Device, image: Vulkan.Image, memory: Vulkan.DeviceMemory, offset: Vulkan.DeviceSize): Unit
  @native def freeMemory(device: Vulkan.Device, memory: Vulkan.DeviceMemory): Unit

  @native def createBuffer(device: Vulkan.Device, createInfo: Vulkan.BufferCreateInfo): Vulkan.Buffer
  @native def getBufferMemoryRequirements(device: Vulkan.Device, buffer: Vulkan.Buffer): Vulkan.MemoryRequirements
  @native def mapMemory(device: Vulkan.Device, memory: Vulkan.DeviceMemory, offset: Vulkan.DeviceSize, size: Vulkan.DeviceSize, flags: Int): Long
  @native def loadMemory(ptr: Long, buffer: java.nio.ByteBuffer): Unit
  @native def unmapMemory(device: Vulkan.Device, memory: Vulkan.DeviceMemory): Unit

  @native def bindBufferMemory(device: Vulkan.Device, buffer: Vulkan.Buffer, memory: Vulkan.DeviceMemory, offset: Vulkan.DeviceSize): Unit
  @native def destroyBuffer(device: Vulkan.Device, buffer: Vulkan.Buffer): Unit

  @native def createDescriptorSetLayout(device: Vulkan.Device, info: Vulkan.DescriptorSetLayoutCreateInfo): Vulkan.DescriptorSetLayout
  @native def destroyDescriptorSetLayout(device: Vulkan.Device, descriptorSetLayout: Vulkan.DescriptorSetLayout): Unit
  @native def createPipelineLayout(device: Vulkan.Device, info: Vulkan.PipelineLayoutCreateInfo): Vulkan.PipelineLayout
  @native def destroyPipelineLayout(device: Vulkan.Device, pipelineLayout: Vulkan.PipelineLayout): Unit
    res = vkCreateDescriptorPool(info.device, &descriptor_pool, NULL,
                                 &info.desc_pool);
        vkAllocateDescriptorSets(info.device, alloc_info, info.desc_set.data());
    vkUpdateDescriptorSets(info.device, 1, writes, 0, NULL);
    vkDestroyDescriptorPool(info.device, info.desc_pool, NULL);
}

object Vulkan {
  val API_VERSION_1_0: Int = 1 << 22

  val QUEUE_GRAPHICS_BIT: Int = 0x00000001

  final class StructureType(val sType: Int) extends AnyVal
  val APPLICATION_INFO = new StructureType(0)
  val INSTANCE_CREATE_INFO = new StructureType(1)

  final class ApplicationInfo(
    val pNext: Long,
    val pApplicationName: String,
    val applicationVersion: Int,
    val pEngineName: String,
    val engineVersion: Int,
    val apiVersion: Int
  ) {
    val sType: StructureType = APPLICATION_INFO
  }

  final class InstanceCreateInfo(
    val pNext: Long,
    val pApplicationInfo: ApplicationInfo,
    val enabledLayerCount: Int,
    val ppEnabledLayerNames: Array[String],
    val enabledExtensionCount: Int,
    val ppEnabledExtensionNames: Array[String]) {
    val sType: StructureType = INSTANCE_CREATE_INFO
    val flags: Long = 0
  }

  final class Instance(val ptr: Long) extends AnyVal
  
  final class PhysicalDevice(val ptr: Long) extends AnyVal

  final class MemoryType(
    val propertyFlags: Int,
    val heapIndex: Int)

  final class MemoryHeap(
    val size: DeviceSize,
    val flags: Int)
  
  final class PhysicalDeviceMemoryProperties(
    val memoryTypeCount: Int,
    val memoryTypes: Array[MemoryType],
    val memoryHeapCount: Int,
    val memoryHeaps: Array[MemoryHeap]
  )

  final class QueueFamilyProperties(
    val queueFlags: Int,
    val queueCount: Int,
    val timestampValidBits: Int,
    val minImageTransferGranularity: Extent3D
  )

  final class Extent3D(
    val width: Int,
    val height: Int,
    val depth: Int)

  final class Extent2D(
    val width: Int,
    val height: Int)

  final class DeviceQueueCreateInfo(
    val pNext: Long,
    val flags: Int,
    val queueFamilyIndex: Int,
    val queueCount: Int,
    val pQueuePriorities: Array[Float]
  )

  final class DeviceCreateInfo(
    val pNext: Long,
    val flags: Int,
    val queueCreateInfoCount: Int,
    val pQueueCreateInfos: Array[DeviceQueueCreateInfo],
    val enabledLayerCount: Int,
    val ppEnabledLayerNames: Array[String],
    val enabledExtensionCount: Int,
    val ppEnabledExtensionNames: Array[String]
  )

  final class Device(val ptr: Long) extends AnyVal

  final class CommandPoolCreateInfo(
    val pNext: Long,
    val flags: Int,
    val queueFamilyIndex: Int
  )

  final class CommandPool(val ptr: Long) extends AnyVal

  final class CommandBufferLevel(val level: Long) extends AnyVal
  val COMMAND_BUFFER_LEVEL_PRIMARY = new CommandBufferLevel(0)
  val COMMAND_BUFFER_LEVEL_SECONDARY = new CommandBufferLevel(1)

  final class CommandBufferAllocateInfo(
    val pNext: Long,
    val commandPool: CommandPool,
    val level: CommandBufferLevel,
    val commandBufferCount: Int
  )

  final class CommandBuffer(val ptr: Long) extends AnyVal

  final class Surface(val ptr: Long) extends AnyVal
  val SWAPCHAIN_EXTENSION_NAME = "VK_KHR_swapchain"

  final class Format(val format: Int) extends AnyVal
  val FORMAT_UNDEFINED = new Format(0)
  val FORMAT_B8G8R8A8_UNORM = new Format(44)
  val FORMAT_D16_UNORM = new Format(124)

  val FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT = 0x00000200

  final class ColorSpace(val colorSpace: Int) extends AnyVal
  val COLORSPACE_SRGB_NONLINEAR = new ColorSpace(0)

  final class SurfaceFormat(
    val format: Format,
    val colorSpace: ColorSpace)

  final class SurfaceCapabilities(
    val minImageCount: Int,
    val maxImageCount: Int,
    val currentExtent: Extent2D,
    val minImageExtent: Extent2D,
    val maxImageExtent: Extent2D,
    val maxImageArrayLayers: Int,
    val supportedTransforms: Int,
    val currentTransform: Int)

  final class PresentMode(val mode: Int) extends AnyVal
  val PRESENT_MODE_FIFO = new PresentMode(2)

  val SURFACE_TRANSFORM_IDENTITY_BIT = 0x00000001

  final class SwapchainCreateInfo(
    val flags: Int,
    val surface: Surface,
    val minImageCount: Int,
    val imageFormat: Format,
    val imageColorSpace: ColorSpace,
    val imageExtent: Extent2D,
    val imageArrayLayers: Int,
    val imageUsage: Int,
    val imageSharingMode: SharingMode,
    val queueFamilyIndexCount: Int,
    val pQueueFamilyIndices: Array[Int],
    val preTransform: Int,
    val compositeAlpha: Long,
    val presentMode: PresentMode,
    val clipped: Boolean)

  val COMPOSITE_ALPHA_OPAQUE_BIT = 1
  val NULL_HANDLE = 0
  val IMAGE_USAGE_COLOR_ATTACHMENT_BIT = 0x00000010
  val IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT = 0x00000020

  final class Swapchain(val ptr: Long) extends AnyVal
  final class Image(val ptr: Long) extends AnyVal

  final class ImageViewType(val tpe: Int) extends AnyVal
  val IMAGE_VIEW_TYPE_2D = new ImageViewType(1)

  final class ComponentSwizzle(val swizzle: Int) extends AnyVal
  val COMPONENT_SWIZZLE_R = new ComponentSwizzle(3)
  val COMPONENT_SWIZZLE_G = new ComponentSwizzle(4)
  val COMPONENT_SWIZZLE_B = new ComponentSwizzle(5)
  val COMPONENT_SWIZZLE_A = new ComponentSwizzle(6)

  final class ComponentMapping(
    val r: ComponentSwizzle,
    val g: ComponentSwizzle,
    val b: ComponentSwizzle,
    val a: ComponentSwizzle)
  final class ImageSubresourceRange(
    val aspectMask: Int,
    val baseMipLevel: Int,
    val levelCount: Int,
    val baseArrayLayer: Int,
    val layerCount: Int)
  val IMAGE_ASPECT_COLOR_BIT = 0x00000001
  val IMAGE_ASPECT_DEPTH_BIT = 0x00000002

  final class ImageViewCreateInfo(
    val flags: Int,
    val image: Image,
    val viewType: ImageViewType,
    val format: Format,
    val components: ComponentMapping,
    val subresourceRange: ImageSubresourceRange)

  final class ImageView(val ptr: Long) extends AnyVal

  final class ImageTiling(val tiling: Int) extends AnyVal
  val IMAGE_TILING_OPTIONAL = new ImageTiling(0)
  val IMAGE_TILING_LINEAR = new ImageTiling(1)

  final class FormatProperties(
    val linearTilingFeatures: Int,
    val optimalTilingFeatures: Int,
    val bufferFeatures: Int)

  final class ImageType(val tpe: Int) extends AnyVal
  val IMAGE_TYPE_2D = new ImageType(1)

  final class SharingMode(val mode: Int) extends AnyVal
  val SHARING_MODE_EXCLUSIVE = new SharingMode(0)

  final class ImageLayout(val layout: Int) extends AnyVal
  val IMAGE_LAYOUT_UNDEFINED = new ImageLayout(0)

  final class ImageCreateInfo(
    val flags: Int,
    val imageType: ImageType,
    val format: Format,
    val extent: Extent3D,
    val mipLevels: Int,
    val arrayLayers: Int,
    val samples: Int,
    val tiling: ImageTiling,
    val usage: Int,
    val sharingMode: SharingMode,
    val queueFamilyIndexCount: Int,
    val pQueueFamilyIndices: Array[Int],
    val initialLayout: ImageLayout)

  final class DeviceSize(val size: Long) extends AnyVal

  final class MemoryAllocateInfo(
    val allocationSize: DeviceSize,
    val memoryTypeIndex: Int)

  final class MemoryRequirements(
    val size: DeviceSize,
    val alignment: DeviceSize,
    val memoryTypeBits: Int)

  final class DeviceMemory(val ptr: Long) extends AnyVal
  val SAMPLE_COUNT_1_BIT = 0x00000001

  def memoryTypeIndex(ps: PhysicalDeviceMemoryProperties, bits: Int): Int = 
    ps.memoryTypes.zipWithIndex.foldLeft((Option.empty[Int], bits)) { (t0, t1) =>
      (t0, t1) match {
        case ((None, bits), (tpe, i)) => if((bits & 1) == 1) (Some(i), bits) else (None, bits >> 1)
        case (prev, _) => prev
      }
    }._1.get

  final class BufferCreateInfo(
    val flags: Int,
    val size: DeviceSize,
    val usage: Int,
    val sharingMode: SharingMode,
    val queueFamilyIndexCount: Int,
    val pQueueFamilyIndices: Array[Int])

  final class Buffer(val ptr: Long) extends AnyVal

  val BUFFER_USAGE_UNIFORM_BUFFER_BIT = 0x00000010
  val MEMORY_PROPERTY_HOST_VISIBLE_BIT = 0x00000002
  val MEMORY_PROPERTY_HOST_COHERENT_BIT = 0x00000004

  final class DescriptorType(val tpe: Int) extends AnyVal
  val DESCRIPTOR_TYPE_UNIFORM_BUFFER: DescriptorType = new DescriptorType(6)

  val SHADER_STAGE_VERTEX_BIT: Int = 0x00000001
  final class Sampler(val ptr: Long) extends AnyVal

  final class DescriptorSetLayoutBinding(
    val binding: Int,
    val descriptorType: DescriptorType,
    val descriptorCount: Int,
    val stageFlags: Int,
    val pImmutableSamplers: Array[Sampler]
  )

  final class DescriptorSetLayoutCreateInfo(
    val flags: Int,
    val bindingCount: Int,
    val pBindings: Array[DescriptorSetLayoutBinding]
  )

  final class DescriptorSetLayout(val ptr: Long) extends AnyVal
  
  final class PipelineLayoutCreateInfo(
    val flags: Int,
    val setLayoutCount: Int,
    val pSetLayouts: Array[DescriptorSetLayout],
    val pushConstantRangeCount: Int,
    val pPushConstantRanges: Array[Int]
  )

  final class PipelineLayout(val ptr: Long) extends AnyVal

  final class PushConstantRange(
    val stageFlags: Int,
    val offset: Int,
    val size: Int)

  final class DescriptorPoolSize(
    val tpe: DescriptorType,
    val descriptorCount: Int)

  final class DescriptorPoolCreateInfo(
    val flags: Int,
    val maxSets: Int.
    val poolSizeCount: Int,
    val pPoolSizes: Array[DescriptorPoolSize])

  final class DescriptorSetAllocateInfo(
    val descriptorPool: DescriptorPool,
    val descriptorSetCount: Int,
    val pSetLayouts: Array[DescriptorSetLayout])

  final class DescriptorImageInfo(
    val sampler: Sampler,
    val imageView: ImageView,
    val imageLayout: ImageLayout
  )

  final class DescriptorBufferInfo(
    val buffer: Buffer,
    val offset: DeviceSize,
    val range: DeviceSize
  )

  final class WriteDescriptorSet(
    val dstSet: DescriptorSet,
    val dstBinding: Int,
    val dstArrayElement: Int,
    val descriptorCount: Int,
    val descriptorType: DescriptorType,
    val pImageInfo: Array[DescriptorImageInfo],
    val pBufferInfo: Array[DescriptorBufferInfo],
    val pTexelBufferView: Array[BufferView]
  )

}

/*
bool memory_type_from_properties(struct sample_info &info, uint32_t typeBits,
                                 VkFlags requirements_mask,
                                 uint32_t *typeIndex) {
    // Search memtypes to find first index with those properties
    for (uint32_t i = 0; i < info.memory_properties.memoryTypeCount; i++) {
        if ((typeBits & 1) == 1) {
            // Type is available, does it match user properties?
            if ((info.memory_properties.memoryTypes[i].propertyFlags &
                 requirements_mask) == requirements_mask) {
                *typeIndex = i;
                return true;
            }
        }
        typeBits >>= 1;
    }
    // No memory types matched, return failure
    return false;
}**/
