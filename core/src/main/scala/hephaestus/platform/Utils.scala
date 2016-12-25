package hephaestus
package platform

import java.nio._
import java.nio.file._
import java.io._
import java.util.Scanner
import scala.collection.mutable.MutableList

trait Utils {

  val glfw = new GLFW()
  val vk = new Vulkan()

  def initInstance(): Vulkan.Instance = {
    val appInfo = new Vulkan.ApplicationInfo(
      pNext = 0,
      pApplicationName = "helloWorld",
      applicationVersion = 1,
      pEngineName = "helloWorld",
      engineVersion = 1,
      apiVersion = Vulkan.API_VERSION_1_0
    )
    val instanceCreateInfo = new Vulkan.InstanceCreateInfo(
      pNext = 0,
      pApplicationInfo = appInfo,
      enabledExtensionCount = 0,
      ppEnabledExtensionNames = Array.empty[String],
      enabledLayerCount = 0,
      ppEnabledLayerNames = Array.empty[String]
    )
    vk.createInstance(instanceCreateInfo)
  }

  def initDevice(physicalDevice: Vulkan.PhysicalDevice): Vulkan.Device = {
    val qfps = vk.getPhysicalDeviceQueueFamilyProperties(physicalDevice)

    val qi = qfps.zipWithIndex.find {
      case (q, i) => (q.queueFlags & Vulkan.QUEUE_GRAPHICS_BIT) > 0
    }.map(_._2).get

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
      enabledExtensionCount = 0,
      ppEnabledExtensionNames = Array.empty[String])
    vk.createDevice(physicalDevice, dinfo)
  }

  def initGraphicsQueueFamilyIndex(physicalDevice: Vulkan.PhysicalDevice): Int = {
    val qfps = vk.getPhysicalDeviceQueueFamilyProperties(physicalDevice)
    qfps.zipWithIndex.find {
      case (q, i) => (q.queueFlags & Vulkan.QUEUE_GRAPHICS_BIT) > 0
    }.map(_._2).get
  }

  def initInstanceExtensions(): Vulkan.Instance = {
    val appInfo = new Vulkan.ApplicationInfo(
      pNext = 0,
      pApplicationName = "helloWorld",
      applicationVersion = 1,
      pEngineName = "helloWorld",
      engineVersion = 1,
      apiVersion = Vulkan.API_VERSION_1_0
    )
    val extensions = glfw.getRequiredInstanceExtensions()
    val instanceCreateInfo = new Vulkan.InstanceCreateInfo(
      pNext = 0,
      pApplicationInfo = appInfo,
      enabledExtensionCount = extensions.size,
      ppEnabledExtensionNames = extensions,
      enabledLayerCount = 0,
      ppEnabledLayerNames = Array.empty[String]
    )
    vk.createInstance(instanceCreateInfo)
  }

  def initGraphicsPresentQueueFamilyIndex(instance: Vulkan.Instance, physicalDevice: Vulkan.PhysicalDevice): Int = {
    val qfps = vk.getPhysicalDeviceQueueFamilyProperties(physicalDevice)
    qfps.zipWithIndex.find {
      case (q, i) => 
        val ss = glfw.getPhysicalDevicePresentationSupport(instance, physicalDevice, i)
        val gb = (q.queueFlags & Vulkan.QUEUE_GRAPHICS_BIT) > 0
        ss && gb
    }.map(_._2).get
  }

  def initDeviceExtensions(physicalDevice: Vulkan.PhysicalDevice, qi: Int): Vulkan.Device = {
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
    vk.createDevice(physicalDevice, dinfo)
  }

  def initCommandPool(device: Vulkan.Device, qi: Int): Vulkan.CommandPool = {
    val commandPoolInfo = new Vulkan.CommandPoolCreateInfo(
      pNext = 0,
      flags = 0,
      queueFamilyIndex = qi)
    vk.createCommandPool(device, commandPoolInfo)
  }

  def initCommandBuffer(device: Vulkan.Device, commandPool: Vulkan.CommandPool): Vulkan.CommandBuffer = {
    val commandBufferAllocateInfo = new Vulkan.CommandBufferAllocateInfo(
      pNext = 0,
      commandPool = commandPool,
      level = Vulkan.COMMAND_BUFFER_LEVEL_PRIMARY,
      commandBufferCount = 1)
    vk.allocateCommandBuffers(device, commandBufferAllocateInfo)
  }

  def initSwapchainFormat(surface: Vulkan.Surface, physicalDevice: Vulkan.PhysicalDevice): Vulkan.Format = {
    val formats = vk.getPhysicalDeviceSurfaceFormats(physicalDevice, surface)
    if (formats(0).format == Vulkan.FORMAT_UNDEFINED) Vulkan.FORMAT_B8G8R8A8_UNORM
    else formats(0).format
  }

  def initSwapchainExtent(caps: Vulkan.SurfaceCapabilities): Vulkan.Extent2D = {
    if(caps.currentExtent.width == 0xFFFFFFFF) {
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
  }

  def initSwapchain(surface: Vulkan.Surface, physicalDevice: Vulkan.PhysicalDevice, device: Vulkan.Device, 
    swapchainFormat: Vulkan.Format, swapchainExtent: Vulkan.Extent2D, caps: Vulkan.SurfaceCapabilities): Vulkan.Swapchain = {

    val modes = vk.getPhysicalDeviceSurfacePresentModes(physicalDevice, surface)

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
    vk.createSwapchain(device, swapchainCreateInfo)
  }

  def initImageViews(device: Vulkan.Device, swapchain: Vulkan.Swapchain, swapchainFormat: Vulkan.Format): Array[Vulkan.ImageView] = {
    val swapchainImages = vk.getSwapchainImages(device, swapchain)
    swapchainImages.map { i =>
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
  }

  def initDepthImage(physicalDevice: Vulkan.PhysicalDevice, device: Vulkan.Device, swapchainExtent: Vulkan.Extent2D): Vulkan.Image = {
    val formatProperties = vk.getPhysicalDeviceFormatProperties(physicalDevice, Vulkan.FORMAT_D16_UNORM)
    val imageTiling = if((formatProperties.linearTilingFeatures & Vulkan.FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT) > 0) Vulkan.IMAGE_TILING_LINEAR
    else if((formatProperties.optimalTilingFeatures & Vulkan.FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT) > 0) Vulkan.IMAGE_TILING_OPTIONAL
    else throw new Error("depth not supported")

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
    vk.createImage(device, depthImageInfo)
  }

  def initDepthImageMemory(physicalDevice: Vulkan.PhysicalDevice, device: Vulkan.Device, depthImage: Vulkan.Image, memoryProperties: Vulkan.PhysicalDeviceMemoryProperties): Vulkan.DeviceMemory = {
    val depthImageMemoryRequirements = vk.getImageMemoryRequirements(device, depthImage)

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
    vk.bindImageMemory(device, depthImage, depthImageMemory, new Vulkan.DeviceSize(0))
    depthImageMemory
  }

  def initDepthImageView(device: Vulkan.Device, depthImage: Vulkan.Image): Vulkan.ImageView = {
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
    vk.createImageView(device, depthImageViewInfo)
  }

  def initBuffer(device: Vulkan.Device, size: Int): Vulkan.Buffer = {
    val bufferCreateInfo = new Vulkan.BufferCreateInfo(
      usage = Vulkan.BUFFER_USAGE_UNIFORM_BUFFER_BIT,
      size = new Vulkan.DeviceSize(size),
      queueFamilyIndexCount = 0,
      pQueueFamilyIndices = Array.empty[Int],
      sharingMode = Vulkan.SHARING_MODE_EXCLUSIVE,
      flags = 0)
    vk.createBuffer(device, bufferCreateInfo)
  }

  def initBufferMemory(device: Vulkan.Device, memoryProperties: Vulkan.PhysicalDeviceMemoryProperties, buffer: Vulkan.Buffer,
  data: ByteBuffer): Vulkan.DeviceMemory = {
    val bufferMemoryRequirements = vk.getBufferMemoryRequirements(device, buffer)
    val bufferMemoryTypeIndex = Vulkan.memoryTypeIndex(memoryProperties, 
      Vulkan.MEMORY_PROPERTY_HOST_VISIBLE_BIT | Vulkan.MEMORY_PROPERTY_HOST_COHERENT_BIT)
    val bufferMemoryAllocationInfo = new Vulkan.MemoryAllocateInfo(
      allocationSize = bufferMemoryRequirements.size,
      memoryTypeIndex = bufferMemoryTypeIndex)
    val bufferMemory = vk.allocateMemory(device, bufferMemoryAllocationInfo)
    val dataPtr = vk.mapMemory(device, bufferMemory, new Vulkan.DeviceSize(0), bufferMemoryRequirements.size, 0) 
    vk.loadMemory(dataPtr, data)
    vk.unmapMemory(device, bufferMemory)
    vk.bindBufferMemory(device, buffer, bufferMemory, new Vulkan.DeviceSize(0))
    bufferMemory
  }

  def initDescriptorSetLayout(device: Vulkan.Device): Vulkan.DescriptorSetLayout = {
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
    vk.createDescriptorSetLayout(device, descriptorSetLayoutInfo)
  }

  def initPipelineLayout(device: Vulkan.Device, descriptorSetLayout: Vulkan.DescriptorSetLayout): Vulkan.PipelineLayout = {
    val pipelineLayoutInfo = new Vulkan.PipelineLayoutCreateInfo(
      flags = 0,
      setLayoutCount = 1,
      pSetLayouts = Array(descriptorSetLayout),
      pushConstantRangeCount = 0,
      pPushConstantRanges = Array.empty[Int])
    vk.createPipelineLayout(device, pipelineLayoutInfo)
  }

  def initDescriptorPool(device: Vulkan.Device): Vulkan.DescriptorPool = {
    val descriptorPoolSize = new Vulkan.DescriptorPoolSize(
      tpe = Vulkan.DESCRIPTOR_TYPE_UNIFORM_BUFFER,
      descriptorCount = 1)
    val descriptorPoolCreateInfo = new Vulkan.DescriptorPoolCreateInfo(
      flags = 0,
      maxSets = 1,
      poolSizeCount = 1,
      pPoolSizes = Array(descriptorPoolSize)
    )
    vk.createDescriptorPool(device, descriptorPoolCreateInfo)
  }

  def initDescriptorSets(device: Vulkan.Device, descriptorPool: Vulkan.DescriptorPool, descriptorSetLayout: Vulkan.DescriptorSetLayout, 
    buffer: Vulkan.Buffer, size: Int): Array[Vulkan.DescriptorSet] = {
    val descriptorSetAllocateInfo = new Vulkan.DescriptorSetAllocateInfo(
      descriptorPool = descriptorPool,
      descriptorSetCount = 1,
      pSetLayouts = Array(descriptorSetLayout)
    )

    val descriptorSets = vk.allocateDescriptorSets(device, descriptorSetAllocateInfo)
    val bufferInfo = new Vulkan.DescriptorBufferInfo(
      buffer = buffer,
      offset = new Vulkan.DeviceSize(0),
      range = new Vulkan.DeviceSize(size)
    )
    val writeDescriptorSet = new Vulkan.WriteDescriptorSet(
      dstSet = descriptorSets(0),
      dstBinding = 0,
      dstArrayElement = 0,
      descriptorCount = 1,
      descriptorType = Vulkan.DESCRIPTOR_TYPE_UNIFORM_BUFFER,
      pImageInfo = Array.empty[Vulkan.DescriptorImageInfo],
      pBufferInfo = Array(bufferInfo),
      pTexelBufferView = Array.empty[Vulkan.BufferView]
    )

    vk.updateDescriptorSets(device, 1, Array(writeDescriptorSet), 0, Array.empty[Vulkan.CopyDescriptorSet])
    descriptorSets
  }

  def initSemaphore(device: Vulkan.Device): Vulkan.Semaphore = {
    val semaphoreCreateInfo = new Vulkan.SemaphoreCreateInfo(flags = 0)
    vk.createSemaphore(device, semaphoreCreateInfo)
  }

  def initRenderPass(device: Vulkan.Device, swapchainFormat: Vulkan.Format): Vulkan.RenderPass = {
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

    vk.createRenderPass(device, renderPassCreateInfo)
  }

  def spvFile(name: String): List[Int] = {
    val ints = MutableList[Int]()
    val file = new File(getClass.getResource(s"/$name").toURI())
    val bytes = Files.readAllBytes(file.toPath())
    val buf = ByteBuffer.wrap(bytes).asIntBuffer()
    buf.rewind()
    val arr = new Array[Int](buf.capacity)
    buf.get(arr)
    arr.toList
  }
}
