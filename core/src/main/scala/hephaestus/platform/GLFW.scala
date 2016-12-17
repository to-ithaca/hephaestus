package hephaestus
package platform

import ch.jodersky.jni.nativeLoader

trait Callback[A] {
  def call(a: A): Unit
}


@nativeLoader("hephaestus0")
class GLFW {

  @native def init(): Boolean
  @native def version: GLFW.Version
  @native def terminate(): Unit

  @native def createWindow(width: Int, height: Int, name: String): GLFW.Window
  @native def destroyWindow(window: GLFW.Window): Unit

  @native def setWindowCloseCallback(window: GLFW.Window, callback: Callback[GLFW.Window]): Unit
  @native def waitEvents(): Unit

  @native def windowHint(hint: GLFW.Hint, value: GLFW.HintValue): Unit
  @native def vulkanSupported(): Boolean

  @native def getRequiredInstanceExtensions(): Array[String]
  @native def createWindowSurface(inst: Vulkan.Instance, window: GLFW.Window): Vulkan.Surface
}

object GLFW {

  final class Version(val ptr: Array[Int]) extends AnyVal {
    def major: Int = ptr(0)
    def minor: Int = ptr(1)
    def rev: Int = ptr(2)
  }

  final class Window(val ptr: Long) extends AnyVal

  final class Hint(val hint: Int) extends AnyVal
  val CLIENT_API: Hint = new Hint(0x00022001)

  final class HintValue(val value: Int) extends AnyVal
  val NO_API: HintValue = new HintValue(0)
}

import java.nio._

object Foobar {
  def main(args: Array[String]): Unit = {
    val glfw = new GLFW()
    glfw.init()

    val window = glfw.createWindow(200, 200, "foobar")
    glfw.setWindowCloseCallback(window, { (i: GLFW.Window)  => 
      println(i.ptr)
    })
    println(s"vulkan is supported ${glfw.vulkanSupported()}")
    glfw.windowHint(GLFW.CLIENT_API, GLFW.NO_API)
    val appInfo = new Vulkan.ApplicationInfo(
      pNext = 0,
      pApplicationName = "helloWorld",
      applicationVersion = 1,
      pEngineName = "helloWorld",
      engineVersion = 1,
      apiVersion = Vulkan.API_VERSION_1_0
    )
    val extensions = glfw.getRequiredInstanceExtensions()
    println(s"got extensions ${extensions.toList}")
    val vk = new Vulkan()
    val instanceCreateInfo = new Vulkan.InstanceCreateInfo(
      pNext = 0,
      pApplicationInfo = appInfo,
      enabledExtensionCount = extensions.size,
      ppEnabledExtensionNames = extensions,
      enabledLayerCount = 0,
      ppEnabledLayerNames = Array.empty[String]
    )
    val instance = vk.createInstance(instanceCreateInfo)
    val surface = glfw.createWindowSurface(instance, window)
    println("created surface")
    Console.flush()
    val physicalDevices = vk.enumeratePhysicalDevices(instance)
    println(s"got devices ${physicalDevices.size}")
    val physicalDevice = physicalDevices(0)
    val qfps = vk.getPhysicalDeviceQueueFamilyProperties(physicalDevice)
    println(s"got qfps ${qfps.size}")

    val qi = qfps.zipWithIndex.find {
      case (q, i) => 
        val ss = vk.getPhysicalDeviceSurfaceSupport(physicalDevice, i, surface)
        val gb = (q.queueFlags & Vulkan.QUEUE_GRAPHICS_BIT) > 0
        println(s"queue [$i] has surface support [$ss] and graphics bit [$gb]")
        ss && gb
    }.get._2

    val dqinfo = new Vulkan.DeviceQueueCreateInfo(
      pNext = 0,
      flags = 0,
      queueFamilyIndex = qi,
      queueCount = 1,
      pQueuePriorities = Array(0f)
    )
    val dinfo = new Vulkan.DeviceCreateInfo(
      pNext = 0,
      flags = 0,
      queueCreateInfoCount = 1,
      pQueueCreateInfos = Array(dqinfo),
      enabledLayerCount = 0,
      ppEnabledLayerNames = Array.empty[String],
      enabledExtensionCount = 1,
      ppEnabledExtensionNames = Array(Vulkan.SWAPCHAIN_EXTENSION_NAME))
    println("about to create device")
    Console.flush()
    val device = vk.createDevice(physicalDevice, dinfo)
    println("created device!")
    Console.flush()
    val commandPoolInfo = new Vulkan.CommandPoolCreateInfo(
      pNext = 0,
      flags = 0,
      queueFamilyIndex = qi)
    val commandPool = vk.createCommandPool(device, commandPoolInfo)
    val commandBufferAllocateInfo = new Vulkan.CommandBufferAllocateInfo(
      pNext = 0,
      commandPool = commandPool,
      level = Vulkan.COMMAND_BUFFER_LEVEL_PRIMARY,
      commandBufferCount = 1)
    println("created command pool")
    Console.flush()
    val commandBuffer = vk.allocateCommandBuffers(device, commandBufferAllocateInfo)

    val formats = vk.getPhysicalDeviceSurfaceFormats(physicalDevice, surface)
    println(s"formats are [${formats.toList}]")
    val swapchainFormat = 
      if (formats(0).format == Vulkan.FORMAT_UNDEFINED) Vulkan.FORMAT_B8G8R8A8_UNORM
      else formats(0).format

    val caps = vk.getPhysicalDeviceSurfaceCapabilities(physicalDevice, surface)
    println(s"caps are [${caps}]")
    val modes = vk.getPhysicalDeviceSurfacePresentModes(physicalDevice, surface)
    println(s"modes are [${modes.toList}]")

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
      queueFamilyIndexCount = 0,
      pQueueFamilyIndices = Array.empty[Int]
    )
    println("about to create swapchain")
    val swapchain = vk.createSwapchain(device, swapchainCreateInfo)
    println("created swapchain")
    Console.flush()
    val swapchainImages = vk.getSwapchainImages(device, swapchain)
    println(s"got images ${swapchainImages.size}")
    swapchainImages.foreach { i =>
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
      println("creating image view")
      Console.flush()
      val imageView = vk.createImageView(device, info)
      println("created image view")
      vk.destroyImageView(device, imageView)
      println("destroyed image view")
    }
    val formatProperties = vk.getPhysicalDeviceFormatProperties(physicalDevice, Vulkan.FORMAT_D16_UNORM)
    println("got format properties")
    val imageTiling = if((formatProperties.linearTilingFeatures & Vulkan.FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT) > 0) Vulkan.IMAGE_TILING_LINEAR
    else if((formatProperties.optimalTilingFeatures & Vulkan.FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT) > 0) Vulkan.IMAGE_TILING_OPTIONAL
    else throw new Error("depth not supported")

    println("got image tiling")
    val depthImageInfo = new Vulkan.ImageCreateInfo(
      flags = 0,
      imageType = Vulkan.IMAGE_TYPE_2D,
      format = Vulkan.FORMAT_D16_UNORM,
      extent = new Vulkan.Extent3D(
        width = swapchainExtent.width,
        height = swapchainExtent.height,
        depth = 1),
      mipLevels = 1,
      arrayLayers = 1,
      samples = Vulkan.SAMPLE_COUNT_1_BIT,
      tiling = imageTiling,
      initialLayout = Vulkan.IMAGE_LAYOUT_UNDEFINED,
      usage = Vulkan.IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT,
      queueFamilyIndexCount = 0,
      pQueueFamilyIndices = Array.empty,
      sharingMode = Vulkan.SHARING_MODE_EXCLUSIVE)
    val depthImage = vk.createImage(device, depthImageInfo)
    println("created image")
    val depthImageMemoryRequirements = vk.getImageMemoryRequirements(device, depthImage)
    println("got memory requirments")

    val memoryProperties = vk.getPhysicalDeviceMemoryProperties(physicalDevice)
    println("got memory properties")
    val memoryTypeIndex = memoryProperties.memoryTypes.zipWithIndex.foldLeft((Option.empty[Int], depthImageMemoryRequirements.memoryTypeBits)) { (t0, t1) => 
      (t0, t1) match {
        case ((None, bits), (tpe, i)) => if((bits & 1) == 1) (Some(i), bits) else (None, bits >> 1)
        case (prev, _) => prev
      }
    }._1.get

    val depthImageMemoryAllocateInfo = new Vulkan.MemoryAllocateInfo(
      allocationSize = depthImageMemoryRequirements.size,
      memoryTypeIndex = memoryTypeIndex)
    val depthImageMemory = vk.allocateMemory(device, depthImageMemoryAllocateInfo)
    println("allocated memory")
    vk.bindImageMemory(device, depthImage, depthImageMemory, new Vulkan.DeviceSize(0))
    println("bound image memory")
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
      subresourceRange = new Vulkan.ImageSubresourceRange(
        aspectMask = Vulkan.IMAGE_ASPECT_DEPTH_BIT,
        baseMipLevel = 0,
        levelCount = 1,
        baseArrayLayer = 0,
        layerCount = 1)
    )
    println("about to create depth image view")
    val depthImageView = vk.createImageView(device, depthImageViewInfo)
    println("created image view")


    val uniformData = ByteBuffer.allocateDirect(4 * 4)
    .putFloat(1f).putFloat(2f).putFloat(3f).putFloat(4f)
    val bufferCreateInfo = new Vulkan.BufferCreateInfo(
      usage = Vulkan.BUFFER_USAGE_UNIFORM_BUFFER_BIT,
      size = new Vulkan.DeviceSize(uniformData.capacity),
      queueFamilyIndexCount = 0,
      pQueueFamilyIndices = Array.empty[Int],
      sharingMode = Vulkan.SHARING_MODE_EXCLUSIVE,
      flags = 0)
    println("about to create buffer")
    val buffer = vk.createBuffer(device, bufferCreateInfo)
    println("created buffer")
    val bufferMemoryRequirements = vk.getBufferMemoryRequirements(device, buffer)
    println("got memory requirements")
    val bufferMemoryTypeIndex = Vulkan.memoryTypeIndex(memoryProperties, 
      Vulkan.MEMORY_PROPERTY_HOST_VISIBLE_BIT | Vulkan.MEMORY_PROPERTY_HOST_COHERENT_BIT)
    println("got memory type index")
    val bufferMemoryAllocationInfo = new Vulkan.MemoryAllocateInfo(
      allocationSize = bufferMemoryRequirements.size,
      memoryTypeIndex = bufferMemoryTypeIndex)
    println("about to allocate memory")
    val bufferMemory = vk.allocateMemory(device, bufferMemoryAllocationInfo)
    println("allocated memory")
    val dataPtr = vk.mapMemory(device, bufferMemory, new Vulkan.DeviceSize(0), bufferMemoryRequirements.size, 0) 
    println("mapped memory")
    //copy
    vk.loadMemory(dataPtr, uniformData)
    println("loaded memory")
    vk.unmapMemory(device, bufferMemory)
    println("unmap memory")
    vk.bindBufferMemory(device, buffer, bufferMemory, new Vulkan.DeviceSize(0))
    println("bind buffer")

    val descriptorSetLayoutInfo = new Vulkan.DescriptorSetLayoutCreateInfo(
      flags = 0,
      bindingCount = 1,
      pBindings = Array(new Vulkan.DescriptorSetLayoutBinding(
        binding = 0,
        descriptorType = Vulkan.DESCRIPTOR_TYPE_UNIFORM_BUFFER,
        descriptorCount = 1,
        stageFlags = Vulkan.SHADER_STAGE_VERTEX_BIT,
        pImmutableSamplers = Array.empty[Vulkan.Sampler]
      )))
    val descriptorSetLayout = vk.createDescriptorSetLayout(device, descriptorSetLayoutInfo)
    println("create descriptor set layout")
    val pipelineLayoutInfo = new Vulkan.PipelineLayoutCreateInfo(
      flags = 0,
      setLayoutCount = 1,
      pSetLayouts = Array(descriptorSetLayout),
      pushConstantRangeCount = 0,
      pPushConstantRanges = Array.empty[Int])

    println("create pipeline layout")
    val pipelineLayout = vk.createPipelineLayout(device, pipelineLayoutInfo)
    println("created pipeline layout")
    vk.destroyDescriptorSetLayout(device, descriptorSetLayout)
    println("destroy desc layout")
    vk.destroyPipelineLayout(device, pipelineLayout)
    println("destroy pipeline layout")
    vk.destroyBuffer(device, buffer)
    println("destroy buffer")
    vk.freeMemory(device, bufferMemory)
    println("free memory")
    vk.destroyImageView(device, depthImageView)
    println("destroy image view")
    vk.destroyImage(device, depthImage)
    println("destroy image")
    vk.freeMemory(device, depthImageMemory)
    println("free memory")
    vk.destroySwapchain(device, swapchain)
    println("destroyed swapchain")
    // vk.destroySurfaceKHR(instance, surf)
    // println("destroyed surface")
    // Console.flush()
   //  vk.destroyCommandPool(dev, commandPool)
   //  vk.freeCommandBuffers(dev, commandPool, 1, commandBuffer)
   //  vk.destroyDevice(dev)
   // // println(s"got qfps ${qfps.size}")
   //  // Console.flush()
   //   vk.destroyInstance(instance)
   //   glfw.destroyWindow(w)
   //   glfw.terminate()
    // while(true) {
    // glfw.waitEvents()
    // println("got event")
    // Console.flush()
    // }
    //Thread.currentThread().join()
    // glfw.destroyWindow(w)
    // glfw.terminate()
  }
}
