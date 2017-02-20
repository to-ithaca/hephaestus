package hephaestus
package lunarg
package tutorial

import hephaestus.platform._

object Step06 extends Utils {
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

    val formatProperties = vk.getPhysicalDeviceFormatProperties(
      physicalDevice,
      Vulkan.FORMAT_D16_UNORM)
    val imageTiling =
      if (Vulkan.FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT & formatProperties.linearTilingFeatures)
        Vulkan.IMAGE_TILING_LINEAR
      else if (Vulkan.FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT & formatProperties.optimalTilingFeatures)
        Vulkan.IMAGE_TILING_OPTIMAL
      else throw new Error("depth not supported")

    val depthImageInfo = new Vulkan.ImageCreateInfo(
      flags = 0,
      imageType = Vulkan.IMAGE_TYPE_2D,
      format = Vulkan.FORMAT_D16_UNORM,
      extent = new Vulkan.Extent3D(width = swapchainExtent.width,
                                   height = swapchainExtent.height,
                                   depth = 1),
      mipLevels = 1,
      arrayLayers = 1,
      samples = Vulkan.SAMPLE_COUNT_1_BIT,
      tiling = imageTiling,
      initialLayout = Vulkan.IMAGE_LAYOUT_UNDEFINED,
      usage = Vulkan.IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT,
      queueFamilyIndices = Array.empty,
      sharingMode = Vulkan.SHARING_MODE_EXCLUSIVE
    )
    val depthImage = vk.createImage(device, depthImageInfo)
    val depthImageMemoryRequirements =
      vk.getImageMemoryRequirements(device, depthImage)

    val memoryProperties = vk.getPhysicalDeviceMemoryProperties(physicalDevice)
    val memoryTypeIndex = memoryProperties.memoryTypes.zipWithIndex
      .foldLeft(
        (Option.empty[Int], depthImageMemoryRequirements.memoryTypeBits)) {
        (t0, t1) =>
          (t0, t1) match {
            case ((None, bits), (tpe, i)) =>
              if ((bits & 1) == 1) (Some(i), bits) else (None, bits >> 1)
            case (prev, _) => prev
          }
      }
      ._1
      .get

    val depthImageMemoryAllocateInfo = new Vulkan.MemoryAllocateInfo(
      allocationSize = depthImageMemoryRequirements.size,
      memoryTypeIndex = memoryTypeIndex)
    val depthImageMemory =
      vk.allocateMemory(device, depthImageMemoryAllocateInfo)
    vk.bindImageMemory(device, depthImage, depthImageMemory, 0L)
    val depthImageViewInfo = new Vulkan.ImageViewCreateInfo(
      flags = 0,
      image = depthImage,
      viewType = Vulkan.IMAGE_VIEW_TYPE_2D,
      format = Vulkan.FORMAT_D16_UNORM,
      components = new Vulkan.ComponentMapping(
        r = Vulkan.COMPONENT_SWIZZLE_R,
        g = Vulkan.COMPONENT_SWIZZLE_G,
        b = Vulkan.COMPONENT_SWIZZLE_B,
        a = Vulkan.COMPONENT_SWIZZLE_A
      ),
      subresourceRange =
        new Vulkan.ImageSubresourceRange(aspectMask =
                                           Vulkan.IMAGE_ASPECT_DEPTH_BIT,
                                         baseMipLevel = 0,
                                         levelCount = 1,
                                         baseArrayLayer = 0,
                                         layerCount = 1)
    )
    val depthImageView = vk.createImageView(device, depthImageViewInfo)

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
