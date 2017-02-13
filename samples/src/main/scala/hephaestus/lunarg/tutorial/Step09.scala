package hephaestus
package lunarg
package tutorial

import hephaestus.platform._
import java.nio._

object Step09 extends Utils {
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
    val surfaceCapabilities =
      vk.getPhysicalDeviceSurfaceCapabilities(physicalDevice, surface)
    val swapchainExtent = initSwapchainExtent(surfaceCapabilities)
    val swapchain = initSwapchain(surface,
                                  physicalDevice,
                                  device,
                                  swapchainFormat,
                                  swapchainExtent,
                                  surfaceCapabilities)

    val swapchainImages = vk.getSwapchainImages(device, swapchain)
    val imageViews = initImageViews(device, swapchain, swapchainFormat)
    val depthImage = initDepthImage(physicalDevice, device, swapchainExtent)

    val memoryProperties = vk.getPhysicalDeviceMemoryProperties(physicalDevice)
    val depthImageMemory = initDepthImageMemory(physicalDevice,
                                                device,
                                                depthImage,
                                                memoryProperties)
    val depthImageView = initDepthImageView(device, depthImage)

    val uniformData = Cube.uniformData(width, height)
    val buffer = initBuffer(device, uniformData.capacity)
    val bufferMemory =
      initBufferMemory(device, memoryProperties, buffer, uniformData)

    val descriptorSetLayout = initDescriptorSetLayout(device)
    val pipelineLayout = initPipelineLayout(device, descriptorSetLayout)

    //new code here
    val descriptorPoolSize = new Vulkan.DescriptorPoolSize(
      tpe = Vulkan.DESCRIPTOR_TYPE_UNIFORM_BUFFER,
      descriptorCount = 1)
    val descriptorPoolCreateInfo = new Vulkan.DescriptorPoolCreateInfo(
      flags = 0,
      maxSets = 1,
      poolSizes = Array(descriptorPoolSize)
    )
    val descriptorPool =
      vk.createDescriptorPool(device, descriptorPoolCreateInfo)
    val descriptorSetAllocateInfo = new Vulkan.DescriptorSetAllocateInfo(
      descriptorPool = descriptorPool,
      setLayouts = Array(descriptorSetLayout)
    )

    val descriptorSets =
      vk.allocateDescriptorSets(device, descriptorSetAllocateInfo)
    val bufferInfo = new Vulkan.DescriptorBufferInfo(
      buffer = buffer,
      offset = 0L,
      range = uniformData.capacity
    )
    val writeDescriptorSet = new Vulkan.WriteDescriptorSet(
      dstSet = descriptorSets(0),
      dstBinding = 0,
      dstArrayElement = 0,
      descriptorCount = 1,
      descriptorType = Vulkan.DESCRIPTOR_TYPE_UNIFORM_BUFFER,
      imageInfo = Array.empty[Vulkan.DescriptorImageInfo],
      bufferInfo = Array(bufferInfo),
      texelBufferView = Array.empty[Vulkan.BufferView]
    )

    vk.updateDescriptorSets(device,
                            1,
                            Array(writeDescriptorSet),
                            0,
                            Array.empty[Vulkan.CopyDescriptorSet])

    vk.freeDescriptorSets(device, descriptorPool, 1, descriptorSets)
    vk.destroyDescriptorPool(device, descriptorPool)

    vk.destroyDescriptorSetLayout(device, descriptorSetLayout)
    vk.destroyPipelineLayout(device, pipelineLayout)

    vk.destroyBuffer(device, buffer)
    vk.freeMemory(device, bufferMemory)

    vk.destroyImageView(device, depthImageView)
    vk.freeMemory(device, depthImageMemory)
    vk.destroyImage(device, depthImage)
    imageViews.foreach { i =>
      vk.destroyImageView(device, i)
    }
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
