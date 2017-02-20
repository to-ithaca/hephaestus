package hephaestus
package vulkan

import hephaestus.platform.Vulkan
import hephaestus.platform.Vulkan.{
  Device => VkDevice,
  DeviceMemory => VkDeviceMemory,
  CommandPool => VkCommandPool,
  Buffer => VkBuffer,
  Image => VkImage,
  _
}
import cats._, cats.data._, cats.implicits._
import spire.math.Bounded

case class Device(device: VkDevice, physicalDevice: PhysicalDevice, vk: Vulkan)

object Device {
  def createCommandPool(
      info: CommandPoolCreateInfo): Reader[Device, CommandPool] =
    Reader(d => CommandPool(d.vk.createCommandPool(d.device, info), d, d.vk))
  def destroyCommandPool(p: CommandPool): Reader[Device, Unit] =
    Reader(d => d.vk.destroyCommandPool(d.device, p.pool))

  def createSwapchain(info: SwapchainCreateInfo): Reader[Device, Swapchain] =
    Reader(d => d.vk.createSwapchain(d.device, info))
  def destroySwapchain(s: Swapchain): Reader[Device, Unit] =
    Reader(d => d.vk.destroySwapchain(d.device, s))

  def createImageView(info: ImageViewCreateInfo): Reader[Device, ImageView] =
    Reader(d => d.vk.createImageView(d.device, info))
  def destroyImageView(v: ImageView): Reader[Device, Unit] =
    Reader(d => d.vk.destroyImageView(d.device, v))

  def createImage(info: ImageCreateInfo): Reader[Device, (VkImage, MemoryRequirements)] =
    Reader{d => 
      val i = d.vk.createImage(d.device, info)
      val r = d.vk.getImageMemoryRequirements(d.device, i)
      (i, r)
    }
  def destroyImage(i: VkImage): Reader[Device, Unit] =
    Reader(d => d.vk.destroyImage(d.device, i))

  def createDescriptorPool(
      info: DescriptorPoolCreateInfo): Reader[Device, DescriptorPool] =
    Reader(d => d.vk.createDescriptorPool(d.device, info))
  def destroyDescriptorPool(p: DescriptorPool): Reader[Device, Unit] =
    Reader(d => d.vk.destroyDescriptorPool(d.device, p))

  def createPipelineLayout(
      info: PipelineLayoutCreateInfo): Reader[Device, PipelineLayout] =
    Reader(d => d.vk.createPipelineLayout(d.device, info))
  def destroyPipelineLayout(p: PipelineLayout): Reader[Device, Unit] =
    Reader(d => d.vk.destroyPipelineLayout(d.device, p))

  def createDescriptorSetLayout(info: DescriptorSetLayoutCreateInfo)
    : Reader[Device, DescriptorSetLayout] =
    Reader(d => d.vk.createDescriptorSetLayout(d.device, info))
  def destroyDescriptorSetLayout(
      l: DescriptorSetLayout): Reader[Device, Unit] =
    Reader(d => d.vk.destroyDescriptorSetLayout(d.device, l))

  def createSemaphore(info: SemaphoreCreateInfo): Reader[Device, Semaphore] =
    Reader(d => d.vk.createSemaphore(d.device, info))
  def destroySemaphore(s: Semaphore): Reader[Device, Unit] =
    Reader(d => d.vk.destroySemaphore(d.device, s))

  def createFence(info: FenceCreateInfo): Reader[Device, Fence] =
    Reader(d => d.vk.createFence(d.device, info))
  def destroyFence(f: Fence): Reader[Device, Unit] =
    Reader(d => d.vk.destroyFence(d.device, f))

  def getFenceStatus(f: Fence): Reader[Device, Unit] =
    Reader(d => d.vk.getFenceStatus(d.device, f))

  def resetFence(f: Fence): Reader[Device, Unit] =
    Reader(d => d.vk.resetFences(d.device, 1, Array(f)))

  def createRenderPass(
      info: RenderPassCreateInfo): Reader[Device, RenderPass] =
    Reader(d => d.vk.createRenderPass(d.device, info))
  def destroyRenderPass(r: RenderPass): Reader[Device, Unit] =
    Reader(d => d.vk.destroyRenderPass(d.device, r))

  def allocateMemory(info: MemoryAllocateInfo): Reader[Device, VkDeviceMemory] =
    Reader(d => d.vk.allocateMemory(d.device, info))
  def freeMemory(m: VkDeviceMemory): Reader[Device, Unit] =
    Reader(d => d.vk.freeMemory(d.device, m))

  def createBuffer(info: BufferCreateInfo): Reader[Device, (VkBuffer, MemoryRequirements)] =
    Reader { d =>
      val b = d.vk.createBuffer(d.device, info)
      val req = d.vk.getBufferMemoryRequirements(d.device, b)
      (b, req)
    }

  def bindBufferMemory(buffer: VkBuffer,
                       memory: VkDeviceMemory,
                       offset: DeviceSize): Reader[Device, Unit] =
    Reader(d => d.vk.bindBufferMemory(d.device, buffer, memory, offset))

  def destroyBuffer(b: VkBuffer): Reader[Device, Unit] =
    Reader(d => d.vk.destroyBuffer(d.device, b))

  def createShaderModule(
      info: ShaderModuleCreateInfo): Reader[Device, ShaderModule] =
    Reader(d => d.vk.createShaderModule(d.device, info))
  def destroyShaderModule(m: ShaderModule): Reader[Device, Unit] =
    Reader(d => d.vk.destroyShaderModule(d.device, m))

  def createSampler(info: SamplerCreateInfo): Reader[Device, Sampler] =
    Reader(d => d.vk.createSampler(d.device, info))
  def destroySampler(s: Sampler): Reader[Device, Unit] =
    Reader(d => d.vk.destroySampler(d.device, s))
  def createFramebuffer(
      info: FramebufferCreateInfo): Reader[Device, Framebuffer] =
    Reader(d => d.vk.createFramebuffer(d.device, info))
  def destroyFramebuffer(f: Framebuffer): Reader[Device, Unit] =
    Reader(d => d.vk.destroyFramebuffer(d.device, f))

  def createGraphicsPipelines(infos: Array[GraphicsPipelineCreateInfo])
    : Reader[Device, Array[Pipeline]] =
    Reader(d => d.vk.createGraphicsPipelines(d.device, infos.size, infos))
  def destroyPipeline(p: Pipeline): Reader[Device, Unit] =
    Reader(d => d.vk.destroyPipeline(d.device, p))

  def mapMemory(memory: VkDeviceMemory, slot: Bounded[Long]): Reader[Device, Long] =
    Reader(d => d.vk.mapMemory(d.device, memory, slot.lower, slot.upper - slot.lower, 0))

  def unmapMemory(memory: VkDeviceMemory): Reader[Device, Unit] =
    Reader(d => d.vk.unmapMemory(d.device, memory))

  def flushMappedMemoryRanges(ranges: List[Vulkan.MappedMemoryRange]): Reader[Device, Unit] =
    Reader(d => d.vk.flushMappedMemoryRanges(d.device, ranges.toArray))

  def bindImageMemory(i: VkImage, m: VkDeviceMemory, offset: Long): Reader[Device, Unit] =
    Reader(d => d.vk.bindImageMemory(d.device, i, m, offset))
}
