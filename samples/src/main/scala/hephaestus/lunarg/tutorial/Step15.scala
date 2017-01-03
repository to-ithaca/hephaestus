package hephaestus
package lunarg
package tutorial

import hephaestus.platform._
import java.nio._

object Step15 extends Utils {

  def main(args: Array[String]): Unit = {
    glfw.init()

    val instance = initInstanceExtensions()

    glfw.windowHint(GLFW.CLIENT_API, GLFW.NO_API)
    val width = 200
    val height = 200
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

    val uniformData = Cube.uniformData
    val buffer = initBuffer(device, uniformData.capacity)
    val bufferMemory = initBufferMemory(device, memoryProperties, buffer, uniformData)

    val descriptorSetLayout = initDescriptorSetLayout(device)
    val pipelineLayout = initPipelineLayout(device, descriptorSetLayout)

    val descriptorPool = initDescriptorPool(device)
    val descriptorSets = initDescriptorSets(device, descriptorPool, descriptorSetLayout, buffer, uniformData.capacity)

    val renderPass = initRenderPass(device, swapchainFormat)

    val vertexModule = initShaderModule("vert.spv", device)
    val fragmentModule = initShaderModule("frag.spv", device)

    val commandBufferBeginInfo = new Vulkan.CommandBufferBeginInfo(flags = 0)
    vk.beginCommandBuffer(commandBuffer, commandBufferBeginInfo)

    val graphicsQueue = vk.getDeviceQueue(device, qi, 0)

    val framebuffers = initFramebuffers(device, imageViews, depthImageView, renderPass, width, height)

    val vertexData: ByteBuffer = Cube.solidFaceColorsData
    val vertexBuffer = initVertexBuffer(device, vertexData.capacity)
    val vertexBufferMemory = initBufferMemory(device, memoryProperties, vertexBuffer, vertexData)

    val pipeline = initPipeline(device, renderPass, vertexModule, fragmentModule, pipelineLayout)
    val clearValues0 = new Vulkan.ClearValueColor(color = new Vulkan.ClearColorValueFloat(float32 = Array(0.2f, 0.2f, 0.2f, 0.2f)))
    val clearValues1 = new Vulkan.ClearValueDepthStencil(depthStencil = new Vulkan.ClearDepthStencilValue(depth = 1.0f, stencil = 0))

    val semaphore = initSemaphore(device)
    val currentBuffer = vk.acquireNextImageKHR(device, swapchain, java.lang.Long.MAX_VALUE, semaphore, new Vulkan.Fence(0)) 
    beginRenderPass(commandBuffer, renderPass, currentBuffer, framebuffers, width, height, Array(clearValues0, clearValues1)) 

    //code start
    println("start")
    vk.cmdBindPipeline(commandBuffer, Vulkan.PIPELINE_BIND_POINT_GRAPHICS, pipeline)
    println("bindDescriptorSets")
    vk.cmdBindDescriptorSets(commandBuffer, Vulkan.PIPELINE_BIND_POINT_GRAPHICS, pipelineLayout,
      0, descriptorSets.size, descriptorSets, 0, Array.empty)
    println("vertexBuffes")
    vk.cmdBindVertexBuffers(commandBuffer, 0, 1, Array(vertexBuffer), Array(new Vulkan.DeviceSize(0)))
    val viewport = new Vulkan.Viewport(
      height = height,
      width = width,
      minDepth = 0f,
      maxDepth = 1f,
      x = 0,
      y = 0)
    println("cmdSetViewport")
    vk.cmdSetViewport(commandBuffer, 0, 1, Array(viewport))
    val scissor = new Vulkan.Rect2D(
      extent = new Vulkan.Extent2D(width = width, height = height),
      offset = new Vulkan.Offset2D(x = 0, y = 0)
    )
    println("cmdSetScissor")
    vk.cmdSetScissor(commandBuffer, 0, 1, Array(scissor))
    println("cmddraw")
    vk.cmdDraw(commandBuffer, 12 * 3, 1, 0, 0)
    //code end

    vk.cmdEndRenderPass(commandBuffer)
    vk.endCommandBuffer(commandBuffer)

    val fence = initFence(device)
    //wait for the semaphore
    println("submitQueueWait")
    submitQueueWait(device, fence, commandBuffer, graphicsQueue, semaphore)

    //code start
    val presentInfo = new Vulkan.PresentInfoKHR(
      swapchainCount = 1,
      pSwapchains = Array(swapchain),
      pImageIndices = currentBuffer,
      pWaitSemaphores = Array.empty,
      waitSemaphoreCount = 0)

    println("queuePresentKHR")
    vk.queuePresentKHR(graphicsQueue, presentInfo)
    Thread.sleep(1000)
    //code end

    vk.destroyFence(device, fence)

    framebuffers.foreach { f =>
      vk.destroyFramebuffer(device, f)
    }

    vk.destroyPipeline(device, pipeline)

    vk.destroyBuffer(device, vertexBuffer)
    vk.freeMemory(device, vertexBufferMemory)

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
