package hephaestus
package lunarg
package tutorial

import hephaestus.platform._
import java.nio._

object Step07 extends Utils {
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
    val bufferCreateInfo = new Vulkan.BufferCreateInfo(
      usage = Vulkan.BUFFER_USAGE_UNIFORM_BUFFER_BIT,
      size = new Vulkan.DeviceSize(uniformData.capacity),
      queueFamilyIndexCount = 0,
      pQueueFamilyIndices = Array.empty[Int],
      sharingMode = Vulkan.SHARING_MODE_EXCLUSIVE,
      flags = 0)
    val buffer = vk.createBuffer(device, bufferCreateInfo)
    val bufferMemoryRequirements = vk.getBufferMemoryRequirements(device, buffer)
    val bufferMemoryTypeIndex = memoryTypeIndex(memoryProperties, bufferMemoryRequirements,
      Vulkan.MEMORY_PROPERTY_HOST_VISIBLE_BIT | Vulkan.MEMORY_PROPERTY_HOST_COHERENT_BIT)
    val bufferMemoryAllocationInfo = new Vulkan.MemoryAllocateInfo(
      allocationSize = bufferMemoryRequirements.size,
      memoryTypeIndex = bufferMemoryTypeIndex)
    val bufferMemory = vk.allocateMemory(device, bufferMemoryAllocationInfo)
    val dataPtr = vk.mapMemory(device, bufferMemory, new Vulkan.DeviceSize(0), bufferMemoryRequirements.size, 0) 
    vk.loadMemory(dataPtr, uniformData)
    vk.unmapMemory(device, bufferMemory)
    vk.bindBufferMemory(device, buffer, bufferMemory, new Vulkan.DeviceSize(0))

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
