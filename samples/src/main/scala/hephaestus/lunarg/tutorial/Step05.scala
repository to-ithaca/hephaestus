package hephaestus
package lunarg
package tutorial

import hephaestus.platform._

object Step05 extends Utils {
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

    val formats = vk.getPhysicalDeviceSurfaceFormats(physicalDevice, surface)
    val swapchainFormat = 
      if (formats(0).format == Vulkan.FORMAT_UNDEFINED) Vulkan.FORMAT_B8G8R8A8_UNORM
      else formats(0).format

    val caps = vk.getPhysicalDeviceSurfaceCapabilities(physicalDevice, surface)
    val modes = vk.getPhysicalDeviceSurfacePresentModes(physicalDevice, surface)

    val swapchainExtent = if(caps.currentExtent.width == 0xFFFFFFFF) {
      val defaultWidth = 200
      val defaultHeight = 200
      val width = if(defaultWidth < caps.minImageExtent.width) caps.minImageExtent.width
      else if (defaultWidth > caps.maxImageExtent.width) caps.maxImageExtent.width
      else defaultWidth
      val height = if(defaultHeight < caps.minImageExtent.height) caps.minImageExtent.height
      else if (defaultHeight > caps.maxImageExtent.height) caps.maxImageExtent.height
      else defaultHeight
      new Vulkan.Extent2D(width, height)
    } else {
        caps.currentExtent
    }

    val swapchainPresentMode = Vulkan.PRESENT_MODE_FIFO;
    val desiredNumberOfSwapChainImages = caps.minImageCount;

    val preTransform = 
      if ((caps.supportedTransforms & Vulkan.SURFACE_TRANSFORM_IDENTITY_BIT) > 0) Vulkan.SURFACE_TRANSFORM_IDENTITY_BIT
      else caps.currentTransform
    val swapchainCreateInfo = new Vulkan.SwapchainCreateInfo(
      flags = 0,
      surface = surface,
      minImageCount = desiredNumberOfSwapChainImages,
      imageFormat = swapchainFormat,
      imageExtent = swapchainExtent,
      preTransform = preTransform,
      compositeAlpha = Vulkan.COMPOSITE_ALPHA_OPAQUE_BIT,
      imageArrayLayers = 1,
      presentMode = swapchainPresentMode,
      clipped = true,
      imageColorSpace = Vulkan.COLORSPACE_SRGB_NONLINEAR,
      imageUsage = Vulkan.IMAGE_USAGE_COLOR_ATTACHMENT_BIT,
      imageSharingMode = Vulkan.SHARING_MODE_EXCLUSIVE,
      queueFamilyIndices = Array.empty[Int]
    )
    val swapchain = vk.createSwapchain(device, swapchainCreateInfo)

    val swapchainImages = vk.getSwapchainImages(device, swapchain)
    val imageViews = swapchainImages.map { i =>
      val info = new Vulkan.ImageViewCreateInfo(
        flags = 0,
        image = i,
        viewType = Vulkan.IMAGE_VIEW_TYPE_2D,
        format = swapchainFormat,
        components = new Vulkan.ComponentMapping(
          r = Vulkan.COMPONENT_SWIZZLE_R,
          g = Vulkan.COMPONENT_SWIZZLE_G,
          b = Vulkan.COMPONENT_SWIZZLE_B,
          a = Vulkan.COMPONENT_SWIZZLE_A
        ),
       subresourceRange = new Vulkan.ImageSubresourceRange(
         aspectMask = Vulkan.IMAGE_ASPECT_COLOR_BIT,
         baseMipLevel = 0,
         levelCount = 1,
         baseArrayLayer = 0,
         layerCount = 1)
      )
      vk.createImageView(device, info)
    }

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
