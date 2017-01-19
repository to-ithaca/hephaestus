package hephaestus
package lunarg
package tutorial

import hephaestus.platform._
import java.nio._
import java.io._

object Step11 extends Utils {

  def main(args: Array[String]): Unit = {
    glfw.init()

    val instance = initInstanceExtensionsDebug()
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

    val vertexSpv = spvFile("vert.spv")
    val fragSpv = spvFile("frag.spv")

    val vertexModuleCreateInfo = new Vulkan.ShaderModuleCreateInfo(
      flags = 0,
      codeSize = vertexSpv.capacity,
      code = vertexSpv
    )
    val vertexModule = vk.createShaderModule(device, vertexModuleCreateInfo)
    val vertexShaderStage = new Vulkan.PipelineShaderStageCreateInfo(
      flags = 0,
      stage = Vulkan.SHADER_STAGE_VERTEX_BIT,
      module = vertexModule,
      name = "main"
    )

    val fragmentCreateInfo = new Vulkan.ShaderModuleCreateInfo(
      flags = 0,
      codeSize = fragSpv.capacity,
      code = fragSpv
    )

    val fragmentModule = vk.createShaderModule(device, fragmentCreateInfo)
    val fragmentShaderStage = new Vulkan.PipelineShaderStageCreateInfo(
      flags = 0,
      stage = Vulkan.SHADER_STAGE_FRAGMENT_BIT,
      module = fragmentModule,
      name = "main"
    )

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
