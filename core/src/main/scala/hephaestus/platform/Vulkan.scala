package hephaestus
package platform

import ch.jodersky.jni.nativeLoader
import java.nio.ByteBuffer

@nativeLoader("hephaestus0")
class Vulkan {
  import Vulkan._
  @native def createInstance(info: InstanceCreateInfo): Instance
  @native def destroyInstance(inst: Instance): Unit
  @native def enumeratePhysicalDevices(inst: Instance): Array[PhysicalDevice]
  @native
  def getPhysicalDeviceMemoryProperties(
      device: PhysicalDevice): PhysicalDeviceMemoryProperties
  @native
  def getPhysicalDeviceQueueFamilyProperties(
      device: PhysicalDevice): Array[QueueFamilyProperties]
  @native def createDevice(d: PhysicalDevice, info: DeviceCreateInfo): Device
  @native def destroyDevice(d: Device): Unit
  @native
  def createCommandPool(device: Device,
                        info: CommandPoolCreateInfo): CommandPool
  @native def destroyCommandPool(device: Device, pool: CommandPool): Unit
  //FIXME: should generate an array
  @native
  def allocateCommandBuffers(device: Device,
                             info: CommandBufferAllocateInfo): CommandBuffer
  //FIXME: should take in an array
  @native
  def freeCommandBuffers(device: Device,
                         pool: CommandPool,
                         count: Int,
                         buffer: CommandBuffer): Unit

  @native def destroySurfaceKHR(inst: Instance, surface: Surface): Unit
  @native
  def getPhysicalDeviceSurfaceSupport(device: PhysicalDevice,
                                      queueFamilyIndex: Int,
                                      surface: Surface): Boolean
  @native
  def getPhysicalDeviceSurfaceFormats(device: PhysicalDevice,
                                      surface: Surface): Array[SurfaceFormat]
  @native
  def getPhysicalDeviceSurfaceCapabilities(
      device: PhysicalDevice,
      surface: Surface): SurfaceCapabilities
  @native
  def getPhysicalDeviceSurfacePresentModes(device: PhysicalDevice,
                                           surface: Surface): Array[Int]

  @native
  def createSwapchain(device: Device, info: SwapchainCreateInfo): Swapchain
  @native def destroySwapchain(device: Device, swapchain: Swapchain): Unit
  @native
  def getSwapchainImages(device: Device, swapchain: Swapchain): Array[Image]
  @native
  def createImageView(device: Device, info: ImageViewCreateInfo): ImageView
  @native def destroyImageView(device: Device, imageView: ImageView): Unit

  @native
  def getPhysicalDeviceFormatProperties(device: PhysicalDevice,
                                        format: Format): FormatProperties
  @native def createImage(device: Device, info: ImageCreateInfo): Image
  @native def destroyImage(device: Device, image: Image): Unit
  @native
  def getImageMemoryRequirements(device: Device,
                                 image: Image): MemoryRequirements
  @native
  def allocateMemory(device: Device, info: MemoryAllocateInfo): DeviceMemory
  @native
  def bindImageMemory(device: Device,
                      image: Image,
                      memory: DeviceMemory,
                      offset: DeviceSize): Unit
  @native def freeMemory(device: Device, memory: DeviceMemory): Unit
  @native
  def getImageSubresourceLayout(
      device: Device,
      image: Image,
      subresource: ImageSubresource): SubresourceLayout

  @native
  def createBuffer(device: Device, createInfo: BufferCreateInfo): Buffer
  @native
  def getBufferMemoryRequirements(device: Device,
                                  buffer: Buffer): MemoryRequirements
  @native
  def mapMemory(device: Device,
                memory: DeviceMemory,
                offset: DeviceSize,
                size: DeviceSize,
                flags: Int): Long
  @native def loadMemory(ptr: Long, buffer: java.nio.ByteBuffer): Unit
  @native def unmapMemory(device: Device, memory: DeviceMemory): Unit

  @native
  def bindBufferMemory(device: Device,
                       buffer: Buffer,
                       memory: DeviceMemory,
                       offset: DeviceSize): Unit
  @native def destroyBuffer(device: Device, buffer: Buffer): Unit

  @native
  def createDescriptorSetLayout(
      device: Device,
      info: DescriptorSetLayoutCreateInfo): DescriptorSetLayout
  @native
  def destroyDescriptorSetLayout(
      device: Device,
      descriptorSetLayout: DescriptorSetLayout): Unit
  @native
  def createPipelineLayout(device: Device,
                           info: PipelineLayoutCreateInfo): PipelineLayout
  @native
  def destroyPipelineLayout(device: Device,
                            pipelineLayout: PipelineLayout): Unit

  @native
  def createDescriptorPool(device: Device,
                           info: DescriptorPoolCreateInfo): DescriptorPool
  @native def destroyDescriptorPool(device: Device, pool: DescriptorPool): Unit
  @native
  def allocateDescriptorSets(
      device: Device,
      info: DescriptorSetAllocateInfo): Array[DescriptorSet]
  @native
  def freeDescriptorSets(device: Device,
                         pool: DescriptorPool,
                         count: Int,
                         sets: Array[DescriptorSet]): Unit
  @native
  def updateDescriptorSets(device: Device,
                           writeCount: Int,
                           writes: Array[WriteDescriptorSet],
                           copyCount: Int,
                           copies: Array[CopyDescriptorSet]): Unit

  @native
  def createSemaphore(device: Device, info: SemaphoreCreateInfo): Semaphore
  @native def destroySemaphore(device: Device, semaphore: Semaphore): Unit

  @native
  def acquireNextImageKHR(device: Device,
                          swapchain: Swapchain,
                          timeout: Long,
                          semaphore: Semaphore,
                          fence: Fence): Int
  @native
  def createRenderPass(device: Device, info: RenderPassCreateInfo): RenderPass
  @native def destroyRenderPass(device: Device, renderPass: RenderPass): Unit

  @native
  def createShaderModule(device: Device,
                         info: ShaderModuleCreateInfo): ShaderModule
  @native def destroyShaderModule(device: Device, module: ShaderModule): Unit

  @native
  def beginCommandBuffer(buffer: CommandBuffer,
                         info: CommandBufferBeginInfo): Unit
  @native def endCommandBuffer(buffer: CommandBuffer): Unit
  @native def createFence(device: Device, info: FenceCreateInfo): Fence
  @native def destroyFence(device: Device, fence: Fence): Unit
  @native
  def resetFences(device: Device, count: Int, fences: Array[Fence]): Unit
  @native
  def queueSubmit(queue: Queue,
                  submitCount: Int,
                  pSubmits: Array[SubmitInfo],
                  fence: Fence): Unit
  @native
  def waitForFences(device: Device,
                    fenceCount: Int,
                    pFences: Array[Fence],
                    waitAll: Boolean,
                    timeout: Long): Result
  @native
  def getFenceStatus(device: Device, fence: Fence): Result
  @native
  def createFramebuffer(device: Device,
                        info: FramebufferCreateInfo): Framebuffer
  @native
  def destroyFramebuffer(device: Device, framebuffer: Framebuffer): Unit
  @native
  def getDeviceQueue(device: Device,
                     queueFamilyIndex: Int,
                     queueIndex: Int): Queue

  @native
  def cmdBeginRenderPass(buffer: CommandBuffer,
                         info: RenderPassBeginInfo,
                         contents: SubpassContents): Unit
  @native
  def cmdBindVertexBuffers(commandBuffer: CommandBuffer,
                           firstBinding: Int,
                           bindingCount: Int,
                           buffers: Array[Buffer],
                           offsets: Array[DeviceSize]): Unit

  @native
  def cmdBindIndexBuffer(commandBuffer: CommandBuffer,
                         buffer: Buffer,
                         offset: DeviceSize,
                         indexType: IndexType): Unit

  @native def cmdEndRenderPass(buffer: CommandBuffer): Unit
  @native
  def cmdExecuteCommands(buffer: CommandBuffer,
                         count: Int,
                         buffers: Array[CommandBuffer]): Unit

  @native
  def createGraphicsPipelines(
      device: Device,
      count: Int,
      infos: Array[GraphicsPipelineCreateInfo]): Array[Pipeline]
  @native def destroyPipeline(device: Device, pipeline: Pipeline)

  @native def enumerateInstanceLayerProperties: Array[LayerProperties]
  @native
  def enumerateInstanceExtensionProperties(
      layerName: String): Array[ExtensionProperties]
  @native
  def enumerateDeviceLayerProperties(
      device: PhysicalDevice): Array[LayerProperties]

  @native def debugReport(inst: Instance): Unit
//  @native def createDebugReportCallbackEXT(instance: Instance, info: DebugReportCallbackCreateInfo): DebugReportCallbackEXT
  @native
  def cmdBindPipeline(buffer: Vulkan.CommandBuffer,
                      bindPoint: PipelineBindPoint,
                      pipeline: Pipeline): Unit
  @native
  def cmdBindDescriptorSets(buffer: Vulkan.CommandBuffer,
                            bindPoint: PipelineBindPoint,
                            layout: PipelineLayout,
                            firstSet: Int,
                            descriptorSetCount: Int,
                            descriptorSets: Array[DescriptorSet],
                            dynamicOffsetCount: Int,
                            dynamicOffsets: Array[Int]): Unit
  @native
  def cmdSetViewport(buffer: CommandBuffer,
                     firstViewport: Int,
                     viewportCount: Int,
                     viewports: Array[Viewport]): Unit
  @native
  def cmdSetScissor(buffer: CommandBuffer,
                    firstScissor: Int,
                    scissorCount: Int,
                    scissor: Array[Rect2D]): Unit
  @native
  def cmdDraw(buffer: CommandBuffer,
              vertexCount: Int,
              instanceCount: Int,
              firstVertex: Int,
              firstInstance: Int): Unit
  @native
  def cmdDrawIndexed(buffer: CommandBuffer,
                     indexCount: Int,
                     instanceCount: Int,
                     firstIndex: Int,
                     vertexOffset: Int,
                     firstInstance: Int): Unit
  @native def queuePresentKHR(queue: Queue, info: PresentInfoKHR): Unit

  @native
  def cmdPipelineBarrier(buffer: CommandBuffer,
                         srcStageMask: PipelineStageFlag,
                         dstStageMask: PipelineStageFlag,
                         dependencyFlags: Int,
                         memoryBarriers: Array[MemoryBarrier],
                         bufferMemoryBarriers: Array[BufferMemoryBarrier],
                         imageMemoryBarriers: Array[ImageMemoryBarrier])
  @native def createSampler(device: Device, info: SamplerCreateInfo): Sampler
  @native def destroySampler(device: Device, sampler: Sampler): Unit

  @native
  def cmdCopyBuffer(commandBuffer: CommandBuffer,
                    src: Buffer,
                    dst: Buffer,
                    regions: Array[BufferCopy]): Unit

  @native
  def cmdCopyBufferToImage(commandBuffer: CommandBuffer,
                           src: Buffer,
                           dst: Image,
                           layout: ImageLayout,
                           regions: Array[BufferImageCopy]): Unit

  @native
  def flushMappedMemoryRanges(device: Device,
                              ranges: Array[MappedMemoryRange]): Unit
}

object Vulkan {
  val API_VERSION_1_0: Int = 1 << 22

  val QUEUE_GRAPHICS_BIT: Int = 0x00000001
  val QUEUE_FAMILY_IGNORED: Long = 4294967295L

  final class StructureType(val sType: Int) extends AnyVal
  val APPLICATION_INFO = new StructureType(0)
  val INSTANCE_CREATE_INFO = new StructureType(1)

  final class ApplicationInfo(val applicationName: String,
                              val applicationVersion: Int,
                              val engineName: String,
                              val engineVersion: Int,
                              val apiVersion: Int)

  final class InstanceCreateInfo(val applicationInfo: ApplicationInfo,
                                 val enabledLayerNames: Array[String],
                                 val enabledExtensionNames: Array[String])

  final class Instance(val ptr: Long) extends AnyVal
  final class PhysicalDevice(val ptr: Long) extends AnyVal
  final class Device(val ptr: Long) extends AnyVal
  final class CommandPool(val ptr: Long) extends AnyVal
  final class CommandBuffer(val ptr: Long) extends AnyVal
  final class Surface(val ptr: Long) extends AnyVal
  final class Swapchain(val ptr: Long) extends AnyVal
  final class Image(val ptr: Long) extends AnyVal
  final class DeviceMemory(val ptr: Long) extends AnyVal
  final class ImageView(val ptr: Long) extends AnyVal

  final class MemoryType(val propertyFlags: Int, val heapIndex: Int)

  final class MemoryHeap(val size: DeviceSize, val flags: Int)

  final class PhysicalDeviceMemoryProperties(
      val memoryTypes: Array[MemoryType],
      val memoryHeaps: Array[MemoryHeap])

  final class QueueFamilyProperties(val queueFlags: Int,
                                    val queueCount: Int,
                                    val timestampValidBits: Int,
                                    val minImageTransferGranularity: Extent3D)

  final class Extent3D(val width: Int, val height: Int, val depth: Int)

  final class Extent2D(val width: Int, val height: Int)

  final class Offset2D(val x: Int, val y: Int)

  final class Offset3D(val x: Int, val y: Int, val z: Int)

  final class Rect2D(val offset: Offset2D, val extent: Extent2D)

  final class DeviceQueueCreateInfo(val flags: Int,
                                    val queueFamilyIndex: Int,
                                    val queuePriorities: Array[Float])

  final class DeviceCreateInfo(
      val queueCreateInfos: Array[DeviceQueueCreateInfo],
      val enabledExtensionNames: Array[String])

  class CommandPoolCreateFlag(val value: Int) extends AnyVal
  val COMMAND_POOL_BLANK_FLAG = new CommandPoolCreateFlag(0)
  val COMMAND_POOL_CREATE_TRANSIENT_BIT = new CommandPoolCreateFlag(0X00000001)
  val COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT = new CommandPoolCreateFlag(
    0X00000002)
  val COMMAND_POOL_CREATE_FLAG_BITS_MAX_ENUM = new CommandPoolCreateFlag(
    0X7FFFFFFF)

  final class CommandPoolCreateInfo(val flags: CommandPoolCreateFlag,
                                    val queueFamilyIndex: Int)

  final class CommandBufferLevel(val level: Long) extends AnyVal
  val COMMAND_BUFFER_LEVEL_PRIMARY = new CommandBufferLevel(0)
  val COMMAND_BUFFER_LEVEL_SECONDARY = new CommandBufferLevel(1)

  final class CommandBufferAllocateInfo(
      val commandPool: CommandPool,
      val level: CommandBufferLevel,
      val commandBufferCount: Int
  )

  val SWAPCHAIN_EXTENSION_NAME = "VK_KHR_swapchain"

  final class Format(val format: Int) extends AnyVal
  val FORMAT_UNDEFINED = new Format(0)
  val FORMAT_R8G8B8_UNORM = new Format(23)
  val FORMAT_R8G8B8A8_UNORM = new Format(37)
  val FORMAT_B8G8R8A8_UNORM = new Format(44)
  val FORMAT_D16_UNORM = new Format(124)
  val FORMAT_R32G32B32_SFLOAT = new Format(106)
  val FORMAT_R32G32B32A32_SFLOAT = new Format(109)
  val FORMAT_R32G32_SFLOAT = new Format(103)

  final class FormatFeatureFlagBit(val value: Int) extends AnyVal {
    def &(o: Int): Boolean = (value & o) > 0
  }
  val FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT = new FormatFeatureFlagBit(
    0x00000200)
  val FORMAT_FEATURE_SAMPLED_IMAGE_BIT = new FormatFeatureFlagBit(0x00000001)

  final class ColorSpace(val colorSpace: Int) extends AnyVal
  val COLORSPACE_SRGB_NONLINEAR = new ColorSpace(0)

  final class SurfaceFormat(val format: Format, val colorSpace: ColorSpace)

  final class SurfaceCapabilities(val minImageCount: Int,
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

  final class SwapchainCreateInfo(val flags: Int,
                                  val surface: Surface,
                                  val minImageCount: Int,
                                  val imageFormat: Format,
                                  val imageColorSpace: ColorSpace,
                                  val imageExtent: Extent2D,
                                  val imageArrayLayers: Int,
                                  val imageUsage: ImageUsageFlagBit,
                                  val imageSharingMode: SharingMode,
                                  val queueFamilyIndices: Array[Int],
                                  val preTransform: Int,
                                  val compositeAlpha: Long,
                                  val presentMode: PresentMode,
                                  val clipped: Boolean)

  val COMPOSITE_ALPHA_OPAQUE_BIT = 1
  val NULL_HANDLE = 0

  final class ImageUsageFlagBit(val value: Int) extends AnyVal {
    def |(o: ImageUsageFlagBit): ImageUsageFlagBit = new ImageUsageFlagBit(value | o.value)
  }
  val IMAGE_USAGE_COLOR_ATTACHMENT_BIT = new ImageUsageFlagBit(0x00000010)
  val IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT = new ImageUsageFlagBit(
    0x00000020)
  val IMAGE_USAGE_SAMPLED_BIT = new ImageUsageFlagBit(0x00000004)
  val IMAGE_USAGE_TRANSFER_DST_BIT = new ImageUsageFlagBit(0x00000002)

  final class ImageViewType(val tpe: Int) extends AnyVal
  val IMAGE_VIEW_TYPE_2D = new ImageViewType(1)

  final class ComponentSwizzle(val swizzle: Int) extends AnyVal
  val COMPONENT_SWIZZLE_R = new ComponentSwizzle(3)
  val COMPONENT_SWIZZLE_G = new ComponentSwizzle(4)
  val COMPONENT_SWIZZLE_B = new ComponentSwizzle(5)
  val COMPONENT_SWIZZLE_A = new ComponentSwizzle(6)

  final class ComponentMapping(val r: ComponentSwizzle,
                               val g: ComponentSwizzle,
                               val b: ComponentSwizzle,
                               val a: ComponentSwizzle)

  final class ImageSubresourceRange(val aspectMask: ImageAspectFlag,
                                    val baseMipLevel: Int,
                                    val levelCount: Int,
                                    val baseArrayLayer: Int,
                                    val layerCount: Int)

  final class ImageAspectFlag(val value: Int) extends AnyVal
  val IMAGE_ASPECT_COLOR_BIT = new ImageAspectFlag(0x00000001)
  val IMAGE_ASPECT_DEPTH_BIT = new ImageAspectFlag(0x00000002)

  final class ImageViewCreateInfo(val flags: Int,
                                  val image: Image,
                                  val viewType: ImageViewType,
                                  val format: Format,
                                  val components: ComponentMapping,
                                  val subresourceRange: ImageSubresourceRange)

  final class ImageSubresource(
      val aspectMask: ImageAspectFlag,
      val mipLevel: Int,
      val arrayLayer: Int
  )

  final class ImageSubresourceLayers(val aspectMask: ImageAspectFlag,
                                     val mipLevel: Int,
                                     val baseArrayLayer: Int,
                                     val layerCount: Int)

  final class SubresourceLayout(val offset: DeviceSize,
                                val size: DeviceSize,
                                val rowPitch: DeviceSize,
                                val arrayPitch: DeviceSize,
                                val depthPitch: DeviceSize)

  final class ImageTiling(val tiling: Int) extends AnyVal
  val IMAGE_TILING_OPTIMAL = new ImageTiling(0)
  val IMAGE_TILING_LINEAR = new ImageTiling(1)

  final class FormatProperties(val linearTilingFeatures: Int,
                               val optimalTilingFeatures: Int,
                               val bufferFeatures: Int)

  final class ImageType(val tpe: Int) extends AnyVal
  val IMAGE_TYPE_2D = new ImageType(1)

  final class SharingMode(val mode: Int) extends AnyVal
  val SHARING_MODE_EXCLUSIVE = new SharingMode(0)

  final class ImageLayout(val layout: Int) extends AnyVal
  val IMAGE_LAYOUT_UNDEFINED = new ImageLayout(0)
  val IMAGE_LAYOUT_GENERAL = new ImageLayout(1)
  val IMAGE_LAYOUT_PRESENT_SRC_KHR = new ImageLayout(1000001002)
  val IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL = new ImageLayout(2)
  val IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL = new ImageLayout(3)
  val IMAGE_LAYOUT_PREINITIALIZED = new ImageLayout(8)
  val IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL = new ImageLayout(5)
  val IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL = new ImageLayout(6)
  val IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL = new ImageLayout(7)

  final class ImageCreateInfo(val flags: Int,
                              val imageType: ImageType,
                              val format: Format,
                              val extent: Extent3D,
                              val mipLevels: Int,
                              val arrayLayers: Int,
                              val samples: Int,
                              val tiling: ImageTiling,
                              val usage: ImageUsageFlagBit,
                              val sharingMode: SharingMode,
                              val queueFamilyIndices: Array[Int],
                              val initialLayout: ImageLayout)

  type DeviceSize = Long

  final class MemoryAllocateInfo(val allocationSize: DeviceSize,
                                 val memoryTypeIndex: Int)

  final class MemoryRequirements(val size: DeviceSize,
                                 val alignment: DeviceSize,
                                 val memoryTypeBits: Int)

  val SAMPLE_COUNT_1_BIT = 0x00000001

  // def memoryTypeIndex(ps: PhysicalDeviceMemoryProperties, bits: Int): Int =
  //   ps.memoryTypes.zipWithIndex.foldLeft((Option.empty[Int], bits)) { (t0, t1) =>
  //     (t0, t1) match {
  //       case ((None, bits), (tpe, i)) => if((bits & 1) == 1) (Some(i), bits) else (None, bits >> 1)
  //       case (prev, _) => prev
  //     }
  //   }._1.get

  final class BufferCreateInfo(val flags: Int,
                               val size: DeviceSize,
                               val usage: Int,
                               val sharingMode: SharingMode,
                               val queueFamilyIndices: Array[Int])

  val BUFFER_USAGE_TRANSFER_SRC_BIT = 0x00000001
  val BUFFER_USAGE_TRANSFER_DST_BIT = 0x00000002
  val BUFFER_USAGE_UNIFORM_TEXEL_BUFFER_BIT = 0x00000004
  val BUFFER_USAGE_UNIFORM_BUFFER_BIT = 0x00000010
  val BUFFER_USAGE_INDEX_BUFFER_BIT = 0x00000040
  val BUFFER_USAGE_VERTEX_BUFFER_BIT = 0x00000080

  val MEMORY_PROPERTY_DEVICE_LOCAL_BIT = 0x00000001
  val MEMORY_PROPERTY_HOST_VISIBLE_BIT = 0x00000002
  val MEMORY_PROPERTY_HOST_COHERENT_BIT = 0x00000004
  val MEMORY_PROPERTY_HOST_CACHED_BIT = 0x00000008

  final class DescriptorType(val tpe: Int) extends AnyVal
  val DESCRIPTOR_TYPE_UNIFORM_BUFFER: DescriptorType = new DescriptorType(6)
  val DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER = new DescriptorType(1)

  val SHADER_STAGE_VERTEX_BIT: Int = 0x00000001
  val SHADER_STAGE_FRAGMENT_BIT = 0x00000010

  final class DescriptorSetLayoutBinding(val binding: Int,
                                         val descriptorType: DescriptorType,
                                         val descriptorCount: Int,
                                         val stageFlags: Int,
                                         val immutableSamplers: Array[Sampler])

  final class DescriptorSetLayoutCreateInfo(
      val flags: Int,
      val bindings: Array[DescriptorSetLayoutBinding])

  final class PipelineLayoutCreateInfo(
      val flags: Int,
      val setLayouts: Array[DescriptorSetLayout],
      val pushConstantRanges: Array[PushConstantRange])

  final class Buffer(val ptr: Long) extends AnyVal
  final class Sampler(val ptr: Long) extends AnyVal
  final class DescriptorSetLayout(val ptr: Long) extends AnyVal
  final class PipelineLayout(val ptr: Long) extends AnyVal

  final class PushConstantRange(val stageFlags: Int,
                                val offset: Int,
                                val size: Int)

  final class DescriptorPoolSize(val tpe: DescriptorType,
                                 val descriptorCount: Int)

  final class DescriptorPoolCreateInfo(
      val flags: Int,
      val maxSets: Int,
      val poolSizes: Array[DescriptorPoolSize])

  final class DescriptorPool(val ptr: Long) extends AnyVal

  final class DescriptorSetAllocateInfo(
      val descriptorPool: DescriptorPool,
      val setLayouts: Array[DescriptorSetLayout])

  final class DescriptorSet(val ptr: Long) extends AnyVal

  final class DescriptorImageInfo(val sampler: Sampler,
                                  val imageView: ImageView,
                                  val imageLayout: ImageLayout)

  final class DescriptorBufferInfo(val buffer: Buffer,
                                   val offset: DeviceSize,
                                   val range: DeviceSize)

  //TODO how big are these?
  final class WriteDescriptorSet(val dstSet: DescriptorSet,
                                 val dstBinding: Int,
                                 val dstArrayElement: Int,
                                 val descriptorCount: Int,
                                 val descriptorType: DescriptorType,
                                 val imageInfo: Array[DescriptorImageInfo],
                                 val bufferInfo: Array[DescriptorBufferInfo],
                                 val texelBufferView: Array[BufferView])

  final class CopyDescriptorSet(
      val srcSet: DescriptorSet,
      val srcBinding: Int,
      val srcArrayElement: Int,
      val dstSet: DescriptorSet,
      val dstBinding: Int,
      val dstArrayElement: Int,
      val descriptorCount: Int
  )

  final class BufferView(val prt: Long) extends AnyVal
  final class Semaphore(val ptr: Long) extends AnyVal
  final class Fence(val ptr: Long) extends AnyVal

  final class SemaphoreCreateInfo(val flags: Int) extends AnyVal

  final class AttachmentLoadOp(val op: Int) extends AnyVal
  val ATTACHMENT_LOAD_OP_CLEAR = new AttachmentLoadOp(1)
  val ATTACHMENT_LOAD_OP_DONT_CARE = new AttachmentLoadOp(2)

  final class AttachmentStoreOp(val op: Int) extends AnyVal
  val ATTACHMENT_STORE_OP_STORE = new AttachmentStoreOp(0)
  val ATTACHMENT_STORE_OP_DONT_CARE = new AttachmentStoreOp(1)

  final class AttachmentDescription(val flags: Int,
                                    val format: Format,
                                    val samples: Int,
                                    val loadOp: AttachmentLoadOp,
                                    val storeOp: AttachmentStoreOp,
                                    val stencilLoadOp: AttachmentLoadOp,
                                    val stencilStoreOp: AttachmentStoreOp,
                                    val initialLayout: ImageLayout,
                                    val finalLayout: ImageLayout)

  final class AttachmentReference(val attachment: Int, val layout: ImageLayout)

  final class PipelineBindPoint(val point: Int) extends AnyVal
  val PIPELINE_BIND_POINT_GRAPHICS = new PipelineBindPoint(0)

  final class SubpassDescription(
      val flags: Int,
      val pipelineBindPoint: PipelineBindPoint,
      val inputAttachments: Array[AttachmentReference],
      val colorAttachments: Array[AttachmentReference],
      val resolveAttachments: Array[AttachmentReference],
      val depthStencilAttachment: Array[AttachmentReference],
      val preserveAttachments: Array[Int])

  final class SubpassDependency(
      val srcSubpass: Int,
      val dstSubpass: Int,
      val srcStageMask: Int,
      val dstStageMask: Int,
      val srcAccessMask: Int,
      val dstAccessMask: Int,
      val dependencyFlags: Int
  )

  final class RenderPassCreateInfo(
      val flags: Int,
      val attachments: Array[AttachmentDescription],
      val subpasses: Array[SubpassDescription],
      val dependencies: Array[SubpassDependency])

  final class RenderPass(val ptr: Long) extends AnyVal

  final class ShaderModuleCreateInfo(val flags: Int,
                                     val codeSize: Int,
                                     val code: ByteBuffer)

  final class ShaderModule(val ptr: Long) extends AnyVal

  final class PipelineShaderStageCreateInfo(val flags: Int,
                                            val stage: Int,
                                            val module: ShaderModule,
                                            val name: String)

  final class FramebufferCreateInfo(val flags: Int,
                                    val renderPass: RenderPass,
                                    val attachments: Array[ImageView],
                                    val width: Int,
                                    val height: Int,
                                    val layers: Int)

  final class PipelineStageFlag(val value: Int) extends AnyVal
  val PIPELINE_STAGE_FRAGMENT_SHADER_BIT = new PipelineStageFlag(0x00000080)
  val PIPELINE_STAGE_HOST_BIT = new PipelineStageFlag(0x00004000)

  final class AccessFlag(val value: Int) extends AnyVal
  val ACCESS_SHADER_READ_BIT = new AccessFlag(0x00000020)
  val ACCESS_COLOR_ATTACHMENT_READ_BIT = new AccessFlag(0x00000080)
  val ACCESS_COLOR_ATTACHMENT_WRITE_BIT = new AccessFlag(0x00000100)
  val ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT = new AccessFlag(0x00000200)
  val ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT = new AccessFlag(0x00000400)
  val ACCESS_TRANSFER_READ_BIT = new AccessFlag(0x00000800)
  val ACCESS_TRANSFER_WRITE_BIT = new AccessFlag(0x00001000)
  val ACCESS_HOST_READ_BIT = new AccessFlag(0x00002000)
  val ACCESS_HOST_WRITE_BIT = new AccessFlag(0x00004000)
  val ACCESS_MEMORY_READ_BIT = new AccessFlag(0x00008000)
  val ACCESS_MEMORY_WRITE_BIT = new AccessFlag(0x00010000)
  val ACCESS_COMMAND_PROCESS_READ_BIT_NVX = new AccessFlag(0x00020000)
  val ACCESS_COMMAND_PROCESS_WRITE_BIT_NVX = new AccessFlag(0x00040000)

  final class MemoryBarrier(val srcAccessMask: AccessFlag,
                            val dstAccessMask: AccessFlag)

  final class BufferMemoryBarrier(val srcAccessMask: AccessFlag,
                                  val dstAccessMask: AccessFlag,
                                  val srcQueueFamilyIndex: Int,
                                  val dstQueueFamilyIndex: Int,
                                  val buffer: Buffer,
                                  val offset: DeviceSize,
                                  val size: DeviceSize)

  final class ImageMemoryBarrier(val srcAccessMask: AccessFlag,
                                 val dstAccessMask: AccessFlag,
                                 val oldLayout: ImageLayout,
                                 val newLayout: ImageLayout,
                                 val srcQueueFamilyIndex: Long,
                                 val dstQueueFamilyIndex: Long,
                                 val image: Image,
                                 val subresourceRange: ImageSubresourceRange)

  final class Framebuffer(val ptr: Long) extends AnyVal

  final class CommandBufferUsageFlag(val value: Int) extends AnyVal {
    def |(o: CommandBufferUsageFlag): CommandBufferUsageFlag =
      new CommandBufferUsageFlag(value | o.value)
  }
  val COMMAND_BUFFER_USAGE_BLANK_FLAG = new CommandBufferUsageFlag(0)
  val COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT = new CommandBufferUsageFlag(
    0x00000001)
  val COMMAND_BUFFER_USAGE_RENDER_PASS_CONTINUE_BIT =
    new CommandBufferUsageFlag(0x00000002)
  val COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT = new CommandBufferUsageFlag(
    0x00000004)
  val COMMAND_BUFFER_USAGE_FLAG_BITS_MAX_ENUM = new CommandBufferUsageFlag(
    0x7FFFFFFF)

  final class CommandBufferInheritanceInfo(val renderPass: RenderPass)
  val COMMAND_BUFFER_INHERITANCE_INFO_NULL_HANDLE =
    new CommandBufferInheritanceInfo(new RenderPass(0))
  final class CommandBufferBeginInfo(
      val flags: CommandBufferUsageFlag,
      val inheritanceInfo: CommandBufferInheritanceInfo)

  final class Queue(val ptr: Long) extends AnyVal

  final class FenceCreateInfo(val flags: Int) extends AnyVal

  final class SubmitInfo(
      val waitSemaphores: Array[Semaphore],
      val waitDstStageMask: Array[Int],
      val commandBuffers: Array[CommandBuffer],
      val signalSemaphores: Array[Semaphore]
  )
  val PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT = 0x00000400

  final class VertexInputRate(val rate: Int) extends AnyVal
  val VERTEX_INPUT_RATE_VERTEX = new VertexInputRate(0)

  final class VertexInputBindingDescription(
      val binding: Int,
      val stride: Int,
      val inputRate: VertexInputRate
  )

  final class VertexInputAttributeDescription(val location: Int,
                                              val binding: Int,
                                              val format: Format,
                                              val offset: Int)

  sealed trait ClearColorValue
  final class ClearColorValueFloat(val float32: Array[Float])
      extends ClearColorValue
  final class ClearColorValueInt(val int32: Array[Int]) extends ClearColorValue
  final class ClearColorValueUint(val uint32: Array[Int])
      extends ClearColorValue

  final class ClearDepthStencilValue(val depth: Float, val stencil: Int)

  sealed trait ClearValue
  final class ClearValueColor(val color: ClearColorValue) extends ClearValue
  final class ClearValueDepthStencil(val depthStencil: ClearDepthStencilValue)
      extends ClearValue

  final class RenderPassBeginInfo(val renderPass: RenderPass,
                                  val framebuffer: Framebuffer,
                                  val renderArea: Rect2D,
                                  val clearValues: Array[ClearValue])

  final class SubpassContents(val contents: Int) extends AnyVal
  val SUBPASS_CONTENTS_INLINE = new SubpassContents(0)
  val SUBPASS_CONTENTS_SECONDARY_COMMAND_BUFFERS = new SubpassContents(1)

  final class DynamicState(val value: Int) extends AnyVal

  val DYNAMIC_STATE_VIEWPORT = new DynamicState(0)
  val DYNAMIC_STATE_SCISSOR = new DynamicState(1)
  val DYNAMIC_STATE_STENCIL_REFERENCE = new DynamicState(8)
  val DYNAMIC_STATE_RANGE_SIZE: Int = DYNAMIC_STATE_STENCIL_REFERENCE.value - DYNAMIC_STATE_VIEWPORT.value + 1

  final class PipelineDynamicStateCreateInfo(
      val flags: Int,
      val dynamicStates: Array[DynamicState])

  final class PipelineVertexInputStateCreateInfo(
      val flags: Int,
      val vertexBindingDescriptions: Array[VertexInputBindingDescription],
      val vertexAttributeDescriptions: Array[VertexInputAttributeDescription])

  final class PrimitiveTopology(val value: Int) extends AnyVal
  val PRIMITIVE_TOPOLOGY_TRIANGLE_LIST = new PrimitiveTopology(3)

  final class PipelineInputAssemblyStateCreateInfo(
      val flags: Int,
      val topology: PrimitiveTopology,
      val primitiveRestartEnable: Boolean)

  final class PolygonMode(val value: Int) extends AnyVal
  val POLYGON_MODE_FILL = new PolygonMode(0)

  val CULL_MODE_BACK_BIT: Int = 0x00000002

  final class FrontFace(val value: Int) extends AnyVal
  val FRONT_FACE_COUNTER_CLOCKWISE = new FrontFace(0)
  val FRONT_FACE_CLOCKWISE = new FrontFace(1)

  final class PipelineRasterizationStateCreateInfo(
      val flags: Int,
      val depthClampEnable: Boolean,
      val rasterizerDiscardEnable: Boolean,
      val polygonMode: PolygonMode,
      val cullMode: Int,
      val frontFace: FrontFace,
      val depthBiasEnable: Boolean,
      val depthBiasConstantFactor: Float,
      val depthBiasClamp: Float,
      val depthBiasSlopeFactor: Float,
      val lineWidth: Float)

  final class LogicOp(val value: Int) extends AnyVal
  val LOGIC_OP_NO_OP = new LogicOp(5)

  final class PipelineColorBlendStateCreateInfo(
      val flags: Int,
      val logicOpEnable: Boolean,
      val logicOp: LogicOp,
      val attachments: Array[PipelineColorBlendAttachmentState],
      val blendConstants: Array[Float])

  final class BlendFactor(val value: Int) extends AnyVal
  val BLEND_FACTOR_ZERO = new BlendFactor(0)

  final class BlendOp(val value: Int) extends AnyVal
  val BLEND_OP_ADD = new BlendOp(0)

  final class PipelineColorBlendAttachmentState(
      val blendEnable: Boolean,
      val srcColorBlendFactor: BlendFactor,
      val dstColorBlendFactor: BlendFactor,
      val colorBlendOp: BlendOp,
      val srcAlphaBlendFactor: BlendFactor,
      val dstAlphaBlendFactor: BlendFactor,
      val alphaBlendOp: BlendOp,
      val colorWriteMask: Int)

  final class Viewport(val x: Float,
                       val y: Float,
                       val width: Float,
                       val height: Float,
                       val minDepth: Float,
                       val maxDepth: Float)

  final class PipelineViewportStateCreateInfo(val flags: Int,
                                              val viewportCount: Int,
                                              val viewports: Array[Viewport],
                                              val scissorCount: Int,
                                              val scissors: Array[Rect2D])

  final class CompareOp(val value: Int) extends AnyVal
  val COMPARE_OP_NEVER = new CompareOp(0)
  val COMPARE_OP_LESS_OR_EQUAL = new CompareOp(3)
  val COMPARE_OP_ALWAYS = new CompareOp(7)

  final class StencilOp(val value: Int) extends AnyVal
  val STENCIL_OP_KEEP = new StencilOp(0)

  final class StencilOpState(val failOp: StencilOp,
                             val passOp: StencilOp,
                             val depthFailOp: StencilOp,
                             val compareOp: CompareOp,
                             val compareMask: Int,
                             val writeMask: Int,
                             val reference: Int)

  final class PipelineDepthStencilStateCreateInfo(
      val flags: Int,
      val depthTestEnable: Boolean,
      val depthWriteEnable: Boolean,
      val depthCompareOp: CompareOp,
      val depthBoundsTestEnable: Boolean,
      val stencilTestEnable: Boolean,
      val front: StencilOpState,
      val back: StencilOpState,
      val minDepthBounds: Float,
      val maxDepthBounds: Float)

  final class PipelineMultisampleStateCreateInfo(
      val flags: Int,
      val rasterizationSamples: Int,
      val sampleShadingEnable: Boolean,
      val minSampleShading: Float,
      val sampleMask: Int,
      val alphaToCoverageEnable: Boolean,
      val alphaToOneEnable: Boolean)

  final class Pipeline(val ptr: Long) extends AnyVal

  final class GraphicsPipelineCreateInfo(
      val flags: Int,
      val stages: Array[PipelineShaderStageCreateInfo],
      val vertexInputState: PipelineVertexInputStateCreateInfo,
      val inputAssemblyState: PipelineInputAssemblyStateCreateInfo,
      val viewportState: PipelineViewportStateCreateInfo,
      val rasterizationState: PipelineRasterizationStateCreateInfo,
      val multisampleState: PipelineMultisampleStateCreateInfo,
      val depthStencilState: PipelineDepthStencilStateCreateInfo,
      val colorBlendState: PipelineColorBlendStateCreateInfo,
      val dynamicState: PipelineDynamicStateCreateInfo,
      val layout: PipelineLayout,
      val renderPass: RenderPass,
      val subpass: Int,
      val basePipelineHandle: Pipeline,
      val basePipelineIndex: Int)

  final class LayerProperties(val layerName: String,
                              val specVersion: Int,
                              val implementationVersion: Int,
                              val description: String)

  final class ExtensionProperties(val extensionName: String,
                                  val specVersion: Int)
  val EXT_DEBUG_REPORT_EXTENSION_NAME: String = "VK_EXT_debug_report"
  val LAYER_LUNARG_API_DUMP_NAME = "VK_LAYER_LUNARG_api_dump"
  val LAYER_LUNARG_STANDARD_VALIDATION_NAME =
    "VK_LAYER_LUNARG_standard_validation"

  final class Result(val value: Long) extends AnyVal
  val SUCCESS = new Result(0)
  val TIMEOUT = new Result(2)
  final class PresentInfoKHR(val waitSemaphores: Array[Semaphore],
                             val swapchains: Array[Swapchain],
                             val imageIndices: Int)

  final class Filter(val value: Int) extends AnyVal
  val FILTER_NEAREST = new Filter(0)

  final class SamplerMipmapMode(val value: Int) extends AnyVal
  val SAMPLER_MIPMAP_MODE_NEAREST = new SamplerMipmapMode(0)

  final class SamplerAddressMode(val value: Int) extends AnyVal
  val SAMPLER_ADDRESS_MODE_REPEAT = new SamplerAddressMode(0)
  val SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE = new SamplerAddressMode(2)

  final class BorderColor(val value: Int) extends AnyVal
  val BORDER_COLOR_FLOAT_OPAQUE_WHITE = new BorderColor(4)

  final class SamplerCreateInfo(
      val flags: Int,
      val magFilter: Filter,
      val minFilter: Filter,
      val mipmapMode: SamplerMipmapMode,
      val addressModeU: SamplerAddressMode,
      val addressModeV: SamplerAddressMode,
      val addressModeW: SamplerAddressMode,
      val mipLodBias: Float,
      val anisotropyEnable: Boolean,
      val maxAnisotropy: Float,
      val compareEnable: Boolean,
      val compareOp: CompareOp,
      val minLod: Float,
      val maxLod: Float,
      val borderColor: BorderColor,
      val unnormalizedCoordinates: Boolean
  )

  final class IndexType(val value: Int) extends AnyVal
  val INDEX_TYPE_UINT32 = new IndexType(1)

  final class BufferCopy(val srcOffset: DeviceSize,
                         val dstOffset: DeviceSize,
                         val size: DeviceSize)

  final class BufferImageCopy(
      val bufferOffset: DeviceSize,
      val bufferRowLength: Int,
      val bufferImageHeight: Int,
      val imageSubresource: ImageSubresourceLayers,
      val imageOffset: Offset3D,
      val imageExtent: Extent3D
  )

  final class MappedMemoryRange(
      val memory: DeviceMemory,
      val offset: DeviceSize,
      val size: DeviceSize
  )
}
