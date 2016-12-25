package hephaestus
package platform

import java.nio._

object Step10 extends Utils {
  def main(args: Array[String]): Unit = {
    glfw.init()

    val instance = initInstanceExtensions()
    glfw.windowHint(GLFW.CLIENT_API, GLFW.NO_API)
    val window = glfw.createWindow(200, 200, "foobar")
    val surface = glfw.createWindowSurface(instance, window)

    val physicalDevice = vk.enumeratePhysicalDevices(instance)(0)
    val qi = initGraphicsPresentQueueFamilyIndex(instance, physicalDevice)
    val device = initDeviceExtensions(physicalDevice, qi)
    val commandPool = initCommandPool(device, qi)
    val commandBuffer = initCommandBuffer(device, commandPool)

    val swapchainFormat = initSwapchainFormat(surface, physicalDevice)
    val surfaceCapabilities = vk.getPhysicalDeviceSurfaceCapabilities(physicalDevice, surface)
    val swapchainExtent = initSwapchainExtent(surfaceCapabilities)
    val swapchain = initSwapchain(surface, physicalDevice, device, swapchainFormat, swapchainExtent, surfaceCapabilities)

    val swapchainImages = vk.getSwapchainImages(device, swapchain)
    val imageViews = initImageViews(device, swapchain, swapchainFormat)
    val depthImage = initDepthImage(physicalDevice, device, swapchainExtent)

    val memoryProperties = vk.getPhysicalDeviceMemoryProperties(physicalDevice)
    val depthImageMemory = initDepthImageMemory(physicalDevice, device, depthImage, memoryProperties)
    val depthImageView = initDepthImageView(device, depthImage)

    val uniformData = ByteBuffer.allocateDirect(4 * 4)
    .putFloat(1f).putFloat(2f).putFloat(3f).putFloat(4f)
    val buffer = initBuffer(device, uniformData.capacity)
    val bufferMemory = initBufferMemory(device, memoryProperties, buffer, uniformData)

    val descriptorSetLayout = initDescriptorSetLayout(device)
    val pipelineLayout = initPipelineLayout(device, descriptorSetLayout)

    val descriptorPool = initDescriptorPool(device)
    val descriptorSets = initDescriptorSets(device, descriptorPool, descriptorSetLayout, buffer, uniformData.capacity)

    val semaphoreCreateInfo = new Vulkan.SemaphoreCreateInfo(flags = 0)
    val semaphore = vk.createSemaphore(device, semaphoreCreateInfo)
    val currentBuffer = vk.acquireNextImageKHR(device, swapchain, java.lang.Long.MAX_VALUE, semaphore, new Vulkan.Fence(0)) 

    val colorAttachment = new Vulkan.AttachmentDescription(
      format = swapchainFormat,
      samples = Vulkan.SAMPLE_COUNT_1_BIT,
      loadOp = Vulkan.ATTACHMENT_LOAD_OP_CLEAR,
      storeOp = Vulkan.ATTACHMENT_STORE_OP_STORE,
      stencilLoadOp = Vulkan.ATTACHMENT_LOAD_OP_DONT_CARE,
      stencilStoreOp = Vulkan.ATTACHMENT_STORE_OP_DONT_CARE,
      initialLayout = Vulkan.IMAGE_LAYOUT_UNDEFINED,
      finalLayout = Vulkan.IMAGE_LAYOUT_PRESENT_SRC_KHR,
      flags = 0
    )

    val depthAttachment = new Vulkan.AttachmentDescription(
      format = Vulkan.FORMAT_D16_UNORM,
      samples = Vulkan.SAMPLE_COUNT_1_BIT,
      loadOp = Vulkan.ATTACHMENT_LOAD_OP_CLEAR,
      storeOp = Vulkan.ATTACHMENT_STORE_OP_DONT_CARE,
      stencilLoadOp = Vulkan.ATTACHMENT_LOAD_OP_DONT_CARE,
      stencilStoreOp = Vulkan.ATTACHMENT_STORE_OP_DONT_CARE,
      initialLayout = Vulkan.IMAGE_LAYOUT_UNDEFINED,
      finalLayout = Vulkan.IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL,
      flags = 0
    )

    val colorReference = new Vulkan.AttachmentReference(attachment = 0, layout = Vulkan.IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
    val depthReference = new Vulkan.AttachmentReference(attachment = 1, layout = Vulkan.IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL)

    val subpass = new Vulkan.SubpassDescription(
      pipelineBindPoint = Vulkan.PIPELINE_BIND_POINT_GRAPHICS,
      flags = 0,
      inputAttachmentCount = 0,
      pInputAttachments = Array.empty[Vulkan.AttachmentReference],
      colorAttachmentCount = 1,
      pColorAttachments = Array(colorReference),
      pResolveAttachments = Array.empty[Vulkan.AttachmentReference],
      pDepthStencilAttachment = Array(depthReference),
      preserveAttachmentCount = 0,
      pPreserveAttachments = Array.empty[Int]
    )

    val renderPassCreateInfo = new Vulkan.RenderPassCreateInfo(
      flags = 0,
      attachmentCount = 2,
      pAttachments = Array(colorAttachment, depthAttachment),
      subpassCount = 1,
      pSubpasses = Array(subpass),
      dependencyCount = 0,
      pDependencies = Array.empty
    )

    val renderPass = vk.createRenderPass(device, renderPassCreateInfo)

    vk.destroyRenderPass(device, renderPass)
    vk.destroySemaphore(device, semaphore)

    vk.freeDescriptorSets(device, descriptorPool, 1, descriptorSets)
    vk.destroyDescriptorPool(device, descriptorPool)

    vk.destroyDescriptorSetLayout(device, descriptorSetLayout)
    vk.destroyPipelineLayout(device, pipelineLayout)

    vk.destroyBuffer(device, buffer)
    vk.freeMemory(device, bufferMemory)

    vk.destroyImageView(device, depthImageView)
    vk.freeMemory(device, depthImageMemory)
    vk.destroyImage(device, depthImage)
    imageViews.foreach { i => vk.destroyImageView(device, i)}
    vk.destroySwapchain(device, swapchain)
    vk.freeCommandBuffers(device, commandPool, 1, commandBuffer)
    vk.destroyCommandPool(device, commandPool)
    vk.destroyDevice(device)
    vk.destroySurfaceKHR(instance, surface)
    glfw.destroyWindow(window)
    vk.destroyInstance(instance)
    glfw.terminate()
  }
}
