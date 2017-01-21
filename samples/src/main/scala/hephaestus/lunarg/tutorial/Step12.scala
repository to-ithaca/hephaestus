package hephaestus
package lunarg
package tutorial

import hephaestus.platform._
import java.nio._

object Step12 extends Utils {

  def main(args: Array[String]): Unit = {
    glfw.init()

    val instance = initInstanceExtensions()
    glfw.windowHint(GLFW.CLIENT_API, GLFW.NO_API)
    val width = 500
    val height = 500
    val window = glfw.createWindow(width, height, "foobar")
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

    val uniformData = Cube.uniformData(width, height)
    val buffer = initBuffer(device, uniformData.capacity)
    val bufferMemory = initBufferMemory(device, memoryProperties, buffer, uniformData)

    val descriptorSetLayout = initDescriptorSetLayout(device)
    val pipelineLayout = initPipelineLayout(device, descriptorSetLayout)

    val descriptorPool = initDescriptorPool(device)
    val descriptorSets = initDescriptorSets(device, descriptorPool, descriptorSetLayout, buffer, uniformData.capacity)

    val semaphore = initSemaphore(device)
    val currentBuffer = vk.acquireNextImageKHR(device, swapchain, java.lang.Long.MAX_VALUE, semaphore, new Vulkan.Fence(0)) 
    val renderPass = initRenderPass(device, swapchainFormat)

    val vertexModule = initShaderModule("vert.spv", device)
    val fragmentModule = initShaderModule("frag.spv", device)

    val commandBufferBeginInfo = new Vulkan.CommandBufferBeginInfo(flags = Vulkan.COMMAND_BUFFER_USAGE_BLANK_FLAG,
    inheritanceInfo = Vulkan.COMMAND_BUFFER_INHERITANCE_INFO_NULL_HANDLE)
    vk.beginCommandBuffer(commandBuffer, commandBufferBeginInfo)

    val graphicsQueue = vk.getDeviceQueue(device, qi, 0)

    val framebuffers = imageViews.map { v =>
      val framebufferCreateInfo = new Vulkan.FramebufferCreateInfo(
        flags = 0,
        renderPass = renderPass,
        attachments = Array(v, depthImageView),
        width = width,
        height = height,
        layers = 1
      )
      val framebuffer = vk.createFramebuffer(device, framebufferCreateInfo)
      framebuffer
    }

    vk.endCommandBuffer(commandBuffer)

    val fenceCreateInfo = new Vulkan.FenceCreateInfo(flags = 0)
    val fence = vk.createFence(device, fenceCreateInfo)
    val submitInfo = new Vulkan.SubmitInfo(
      waitSemaphores = Array.empty[Vulkan.Semaphore],
      waitDstStageMask = Array(Vulkan.PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT),
      commandBuffers = Array(commandBuffer),
      signalSemaphores = Array.empty[Vulkan.Semaphore])
    vk.queueSubmit(graphicsQueue, 1, Array(submitInfo), fence)
    vk.waitForFences(device, 1, Array(fence), true, 100000000)
    vk.destroyFence(device, fence)

    framebuffers.foreach { f =>
      vk.destroyFramebuffer(device, f)
    }

    vk.destroyShaderModule(device, vertexModule)
    vk.destroyShaderModule(device, fragmentModule)
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
