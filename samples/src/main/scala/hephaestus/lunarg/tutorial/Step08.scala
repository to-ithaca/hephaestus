package hephaestus
package lunarg
package tutorial

import hephaestus.platform._
import java.nio._

object Step08 extends Utils {
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

    val descriptorSetLayoutInfo = new Vulkan.DescriptorSetLayoutCreateInfo(
      flags = 0,
      bindings = Array(new Vulkan.DescriptorSetLayoutBinding(
        binding = 0,
        descriptorType = Vulkan.DESCRIPTOR_TYPE_UNIFORM_BUFFER,
        descriptorCount = 1,
        stageFlags = Vulkan.SHADER_STAGE_VERTEX_BIT,
        immutableSamplers = Array.empty[Vulkan.Sampler]
      )))
    println("create description")
    val descriptorSetLayout = vk.createDescriptorSetLayout(device, descriptorSetLayoutInfo)
    val pipelineLayoutInfo = new Vulkan.PipelineLayoutCreateInfo(
      flags = 0,
      setLayouts = Array(descriptorSetLayout),
      pushConstantRanges = Array.empty)
    println("createPipelineLayout")
    val pipelineLayout = vk.createPipelineLayout(device, pipelineLayoutInfo)
    println("created pipeline")

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
