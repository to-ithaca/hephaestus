package hephaestus
package lunarg
package tutorial

import hephaestus.platform._
import java.nio._
import java.nio.file._
import java.io._
import java.util.Scanner
import scala.collection.mutable.MutableList

trait Utils {

  val glfw = new GLFW()
  val vk = new Vulkan()

  val FENCE_TIMEOUT = 100000000
  def initInstance(): Vulkan.Instance = {
    val appInfo = new Vulkan.ApplicationInfo(
      applicationName = "helloWorld",
      applicationVersion = 1,
      engineName = "helloWorld",
      engineVersion = 1,
      apiVersion = Vulkan.API_VERSION_1_0
    )
    val instanceCreateInfo = new Vulkan.InstanceCreateInfo(
      applicationInfo = appInfo,
      enabledExtensionNames = Array.empty[String],
      enabledLayerNames = Array.empty[String]
    )
    vk.createInstance(instanceCreateInfo)
  }

  def initDevice(physicalDevice: Vulkan.PhysicalDevice): Vulkan.Device = {
    val qfps = vk.getPhysicalDeviceQueueFamilyProperties(physicalDevice)

    val qi = qfps.zipWithIndex
      .find {
        case (q, i) => (q.queueFlags & Vulkan.QUEUE_GRAPHICS_BIT) > 0
      }
      .map(_._2)
      .get

    val dqinfo = new Vulkan.DeviceQueueCreateInfo(
      flags = 0,
      queueFamilyIndex = qi,
      queuePriorities = Array(0f)
    )
    val dinfo = new Vulkan.DeviceCreateInfo(queueCreateInfos = Array(dqinfo),
                                            enabledExtensionNames =
                                              Array.empty[String])
    vk.createDevice(physicalDevice, dinfo)
  }

  def initDeviceDebug(physicalDevice: Vulkan.PhysicalDevice): Vulkan.Device = {
    val qfps = vk.getPhysicalDeviceQueueFamilyProperties(physicalDevice)

    val qi = qfps.zipWithIndex
      .find {
        case (q, i) => (q.queueFlags & Vulkan.QUEUE_GRAPHICS_BIT) > 0
      }
      .map(_._2)
      .get

    val dqinfo = new Vulkan.DeviceQueueCreateInfo(
      flags = 0,
      queueFamilyIndex = qi,
      queuePriorities = Array(0f)
    )
    val dinfo = new Vulkan.DeviceCreateInfo(queueCreateInfos = Array(dqinfo),
                                            enabledExtensionNames =
                                              Array.empty[String])
    vk.createDevice(physicalDevice, dinfo)
  }

  def initGraphicsQueueFamilyIndex(
      physicalDevice: Vulkan.PhysicalDevice): Int = {
    val qfps = vk.getPhysicalDeviceQueueFamilyProperties(physicalDevice)
    qfps.zipWithIndex
      .find {
        case (q, i) => (q.queueFlags & Vulkan.QUEUE_GRAPHICS_BIT) > 0
      }
      .map(_._2)
      .get
  }

  def initInstanceExtensions(): Vulkan.Instance = {
    val appInfo = new Vulkan.ApplicationInfo(
      applicationName = "helloWorld",
      applicationVersion = 1,
      engineName = "helloWorld",
      engineVersion = 1,
      apiVersion = Vulkan.API_VERSION_1_0
    )
    val extensions = glfw.getRequiredInstanceExtensions()
    val instanceCreateInfo = new Vulkan.InstanceCreateInfo(
      applicationInfo = appInfo,
      enabledExtensionNames = extensions,
      enabledLayerNames = Array.empty
    )
    vk.createInstance(instanceCreateInfo)
  }

  def initInstanceExtensionsDebug(): Vulkan.Instance = {
    val appInfo = new Vulkan.ApplicationInfo(
      applicationName = "helloWorld",
      applicationVersion = 1,
      engineName = "helloWorld",
      engineVersion = 1,
      apiVersion = Vulkan.API_VERSION_1_0
    )
    val extensions = (Vulkan.EXT_DEBUG_REPORT_EXTENSION_NAME :: glfw
      .getRequiredInstanceExtensions()
      .toList).toArray[String]
    println(s"extensions should be ${extensions.toList}")
    val instanceCreateInfo = new Vulkan.InstanceCreateInfo(
      applicationInfo = appInfo,
      enabledExtensionNames = extensions,
      enabledLayerNames = Array(Vulkan.LAYER_LUNARG_STANDARD_VALIDATION_NAME,
                                Vulkan.LAYER_LUNARG_API_DUMP_NAME)
    )
    val inst = vk.createInstance(instanceCreateInfo)
    vk.debugReport(inst)
    val supportedExtensions =
      vk.enumerateInstanceLayerProperties.map(_.layerName)
    val apiProps = vk.enumerateInstanceExtensionProperties(
      Vulkan.LAYER_LUNARG_API_DUMP_NAME)
    val debugProps = vk.enumerateInstanceExtensionProperties(
      Vulkan.LAYER_LUNARG_STANDARD_VALIDATION_NAME)
    println(s"supported ${supportedExtensions.toList}")
    println(s"api props ${apiProps.map(_.extensionName).toList}")
    println(s"debug props ${debugProps.map(_.extensionName).toList}")
    inst
  }
  def initInstanceExtensionsDump(): Vulkan.Instance = {
    val appInfo = new Vulkan.ApplicationInfo(
      applicationName = "helloWorld",
      applicationVersion = 1,
      engineName = "helloWorld",
      engineVersion = 1,
      apiVersion = Vulkan.API_VERSION_1_0
    )
    val extensions = glfw.getRequiredInstanceExtensions()
    println(s"extensions should be ${extensions.toList}")
    val instanceCreateInfo = new Vulkan.InstanceCreateInfo(
      applicationInfo = appInfo,
      enabledExtensionNames = extensions,
      enabledLayerNames = Array(Vulkan.LAYER_LUNARG_API_DUMP_NAME)
    )
    val inst = vk.createInstance(instanceCreateInfo)
    val supportedExtensions =
      vk.enumerateInstanceLayerProperties.map(_.layerName)
    val apiProps = vk.enumerateInstanceExtensionProperties(
      Vulkan.LAYER_LUNARG_API_DUMP_NAME)
    val debugProps = vk.enumerateInstanceExtensionProperties(
      Vulkan.LAYER_LUNARG_STANDARD_VALIDATION_NAME)
    println(s"supported ${supportedExtensions.toList}")
    println(s"api props ${apiProps.map(_.extensionName).toList}")
    println(s"debug props ${debugProps.map(_.extensionName).toList}")
    inst
  }

  def initGraphicsPresentQueueFamilyIndex(
      instance: Vulkan.Instance,
      physicalDevice: Vulkan.PhysicalDevice): Int = {
    val qfps = vk.getPhysicalDeviceQueueFamilyProperties(physicalDevice)
    qfps.zipWithIndex
      .find {
        case (q, i) =>
          val ss = glfw.getPhysicalDevicePresentationSupport(instance,
                                                             physicalDevice,
                                                             i)
          val gb = (q.queueFlags & Vulkan.QUEUE_GRAPHICS_BIT) > 0
          ss && gb
      }
      .map(_._2)
      .get
  }

  def initDeviceExtensions(physicalDevice: Vulkan.PhysicalDevice,
                           qi: Int): Vulkan.Device = {
    val dqinfo = new Vulkan.DeviceQueueCreateInfo(
      flags = 0,
      queueFamilyIndex = qi,
      queuePriorities = Array(0f)
    )
    val dinfo = new Vulkan.DeviceCreateInfo(
      queueCreateInfos = Array(dqinfo),
      enabledExtensionNames = Array(Vulkan.SWAPCHAIN_EXTENSION_NAME))
    vk.createDevice(physicalDevice, dinfo)
  }
  def initDeviceExtensionsDebug(physicalDevice: Vulkan.PhysicalDevice,
                                qi: Int): Vulkan.Device = {
    val dqinfo = new Vulkan.DeviceQueueCreateInfo(
      flags = 0,
      queueFamilyIndex = qi,
      queuePriorities = Array(0f)
    )
    val dinfo = new Vulkan.DeviceCreateInfo(
      queueCreateInfos = Array(dqinfo),
      enabledExtensionNames = Array(Vulkan.SWAPCHAIN_EXTENSION_NAME))
    vk.createDevice(physicalDevice, dinfo)
  }

  def initCommandPool(device: Vulkan.Device, qi: Int): Vulkan.CommandPool = {
    val commandPoolInfo = new Vulkan.CommandPoolCreateInfo(
      flags = Vulkan.COMMAND_POOL_BLANK_FLAG,
      queueFamilyIndex = qi)
    vk.createCommandPool(device, commandPoolInfo)
  }

  def initCommandBuffer(
      device: Vulkan.Device,
      commandPool: Vulkan.CommandPool): Vulkan.CommandBuffer = {
    val commandBufferAllocateInfo = new Vulkan.CommandBufferAllocateInfo(
      commandPool = commandPool,
      level = Vulkan.COMMAND_BUFFER_LEVEL_PRIMARY,
      commandBufferCount = 1)
    vk.allocateCommandBuffers(device, commandBufferAllocateInfo)
  }

  def initSwapchainFormat(
      surface: Vulkan.Surface,
      physicalDevice: Vulkan.PhysicalDevice): Vulkan.Format = {
    val formats = vk.getPhysicalDeviceSurfaceFormats(physicalDevice, surface)
    if (formats(0).format == Vulkan.FORMAT_UNDEFINED)
      Vulkan.FORMAT_B8G8R8A8_UNORM
    else formats(0).format
  }

  def initSwapchainExtent(caps: Vulkan.SurfaceCapabilities): Vulkan.Extent2D = {
    if (caps.currentExtent.width == 0xFFFFFFFF) {
      val defaultWidth = 200
      val defaultHeight = 200
      val width =
        if (defaultWidth < caps.minImageExtent.width) caps.minImageExtent.width
        else if (defaultWidth > caps.maxImageExtent.width)
          caps.maxImageExtent.width
        else defaultWidth
      val height =
        if (defaultHeight < caps.minImageExtent.height)
          caps.minImageExtent.height
        else if (defaultHeight > caps.maxImageExtent.height)
          caps.maxImageExtent.height
        else defaultHeight
      new Vulkan.Extent2D(width, height)
    } else {
      caps.currentExtent
    }
  }

  def initSwapchain(surface: Vulkan.Surface,
                    physicalDevice: Vulkan.PhysicalDevice,
                    device: Vulkan.Device,
                    swapchainFormat: Vulkan.Format,
                    swapchainExtent: Vulkan.Extent2D,
                    caps: Vulkan.SurfaceCapabilities): Vulkan.Swapchain = {

    val modes =
      vk.getPhysicalDeviceSurfacePresentModes(physicalDevice, surface)

    val swapchainPresentMode = Vulkan.PRESENT_MODE_FIFO;
    val desiredNumberOfSwapChainImages = caps.minImageCount;

    val preTransform =
      if ((caps.supportedTransforms & Vulkan.SURFACE_TRANSFORM_IDENTITY_BIT) > 0)
        Vulkan.SURFACE_TRANSFORM_IDENTITY_BIT
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
    vk.createSwapchain(device, swapchainCreateInfo)
  }

  def initImageViews(
      device: Vulkan.Device,
      swapchain: Vulkan.Swapchain,
      swapchainFormat: Vulkan.Format): Array[Vulkan.ImageView] = {
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
        subresourceRange =
          new Vulkan.ImageSubresourceRange(aspectMask =
                                             Vulkan.IMAGE_ASPECT_COLOR_BIT,
                                           baseMipLevel = 0,
                                           levelCount = 1,
                                           baseArrayLayer = 0,
                                           layerCount = 1)
      )
      vk.createImageView(device, info)
    }
  }

  def initDepthImage(physicalDevice: Vulkan.PhysicalDevice,
                     device: Vulkan.Device,
                     swapchainExtent: Vulkan.Extent2D): Vulkan.Image = {
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
    vk.createImage(device, depthImageInfo)
  }

  def initDepthImageMemory(
      physicalDevice: Vulkan.PhysicalDevice,
      device: Vulkan.Device,
      depthImage: Vulkan.Image,
      memoryProperties: Vulkan.PhysicalDeviceMemoryProperties)
    : Vulkan.DeviceMemory = {
    val depthImageMemoryRequirements =
      vk.getImageMemoryRequirements(device, depthImage)

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
    depthImageMemory
  }

  def initDepthImageView(device: Vulkan.Device,
                         depthImage: Vulkan.Image): Vulkan.ImageView = {
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
    vk.createImageView(device, depthImageViewInfo)
  }

  def initBuffer(device: Vulkan.Device, size: Int): Vulkan.Buffer = {
    val bufferCreateInfo = new Vulkan.BufferCreateInfo(
      usage = Vulkan.BUFFER_USAGE_UNIFORM_BUFFER_BIT,
      size = size,
      queueFamilyIndices = Array.empty[Int],
      sharingMode = Vulkan.SHARING_MODE_EXCLUSIVE,
      flags = 0)
    vk.createBuffer(device, bufferCreateInfo)
  }

  def memoryTypeIndex(ps: Vulkan.PhysicalDeviceMemoryProperties,
                      reqs: Vulkan.MemoryRequirements,
                      mask: Int): Int = {
    ps.memoryTypes.zipWithIndex
      .foldLeft((Option.empty[Int], reqs.memoryTypeBits)) { (t0, t1) =>
        (t0, t1) match {
          case ((None, bits), (tpe, i)) =>
            if ((bits & 1) == 1 && (tpe.propertyFlags & mask) == mask)
              (Some(i), bits)
            else (None, bits >> 1)
          case (idx, _) => idx
        }
      }
      ._1
      .get
  }

  def initBufferMemory(device: Vulkan.Device,
                       memoryProperties: Vulkan.PhysicalDeviceMemoryProperties,
                       buffer: Vulkan.Buffer,
                       data: ByteBuffer): Vulkan.DeviceMemory = {
    val bufferMemoryRequirements =
      vk.getBufferMemoryRequirements(device, buffer)
    val bufferMemoryTypeIndex = memoryTypeIndex(
      memoryProperties,
      bufferMemoryRequirements,
      Vulkan.MEMORY_PROPERTY_HOST_VISIBLE_BIT | Vulkan.MEMORY_PROPERTY_HOST_COHERENT_BIT)
    val bufferMemoryAllocationInfo = new Vulkan.MemoryAllocateInfo(
      allocationSize = bufferMemoryRequirements.size,
      memoryTypeIndex = bufferMemoryTypeIndex)
    val bufferMemory = vk.allocateMemory(device, bufferMemoryAllocationInfo)
    val dataPtr =
      vk.mapMemory(device, bufferMemory, 0L, bufferMemoryRequirements.size, 0)
    vk.loadMemory(dataPtr, data)
    vk.unmapMemory(device, bufferMemory)
    vk.bindBufferMemory(device, buffer, bufferMemory, 0L)
    bufferMemory
  }

  def initDescriptorSetLayout(
      device: Vulkan.Device): Vulkan.DescriptorSetLayout = {
    val descriptorSetLayoutInfo = new Vulkan.DescriptorSetLayoutCreateInfo(
      flags = 0,
      bindings = Array(
        new Vulkan.DescriptorSetLayoutBinding(
          binding = 0,
          descriptorType = Vulkan.DESCRIPTOR_TYPE_UNIFORM_BUFFER,
          descriptorCount = 1,
          stageFlags = Vulkan.SHADER_STAGE_VERTEX_BIT,
          immutableSamplers = Array.empty[Vulkan.Sampler]
        ))
    )
    vk.createDescriptorSetLayout(device, descriptorSetLayoutInfo)
  }

  def initPipelineLayout(device: Vulkan.Device,
                         descriptorSetLayout: Vulkan.DescriptorSetLayout)
    : Vulkan.PipelineLayout = {
    val pipelineLayoutInfo = new Vulkan.PipelineLayoutCreateInfo(
      flags = 0,
      setLayouts = Array(descriptorSetLayout),
      pushConstantRanges = Array.empty)
    vk.createPipelineLayout(device, pipelineLayoutInfo)
  }

  def initDescriptorPool(device: Vulkan.Device): Vulkan.DescriptorPool = {
    val descriptorPoolSize = new Vulkan.DescriptorPoolSize(
      tpe = Vulkan.DESCRIPTOR_TYPE_UNIFORM_BUFFER,
      descriptorCount = 1)
    val descriptorPoolCreateInfo = new Vulkan.DescriptorPoolCreateInfo(
      flags = 0,
      maxSets = 1,
      poolSizes = Array(descriptorPoolSize)
    )
    vk.createDescriptorPool(device, descriptorPoolCreateInfo)
  }

  def initDescriptorSets(device: Vulkan.Device,
                         descriptorPool: Vulkan.DescriptorPool,
                         descriptorSetLayout: Vulkan.DescriptorSetLayout,
                         buffer: Vulkan.Buffer,
                         size: Int): Array[Vulkan.DescriptorSet] = {
    val descriptorSetAllocateInfo = new Vulkan.DescriptorSetAllocateInfo(
      descriptorPool = descriptorPool,
      setLayouts = Array(descriptorSetLayout)
    )

    val descriptorSets =
      vk.allocateDescriptorSets(device, descriptorSetAllocateInfo)
    val bufferInfo = new Vulkan.DescriptorBufferInfo(
      buffer = buffer,
      offset = 0L,
      range = size
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
    descriptorSets
  }

  def initSemaphore(device: Vulkan.Device): Vulkan.Semaphore = {
    val semaphoreCreateInfo = new Vulkan.SemaphoreCreateInfo(flags = 0)
    vk.createSemaphore(device, semaphoreCreateInfo)
  }

  def initRenderPass(device: Vulkan.Device,
                     swapchainFormat: Vulkan.Format): Vulkan.RenderPass = {
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

    val colorReference = new Vulkan.AttachmentReference(
      attachment = 0,
      layout = Vulkan.IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
    val depthReference = new Vulkan.AttachmentReference(
      attachment = 1,
      layout = Vulkan.IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL)

    val subpass = new Vulkan.SubpassDescription(
      pipelineBindPoint = Vulkan.PIPELINE_BIND_POINT_GRAPHICS,
      flags = 0,
      inputAttachments = Array.empty[Vulkan.AttachmentReference],
      colorAttachments = Array(colorReference),
      resolveAttachments = Array.empty[Vulkan.AttachmentReference],
      depthStencilAttachment = Array(depthReference),
      preserveAttachments = Array.empty[Int]
    )

    val renderPassCreateInfo = new Vulkan.RenderPassCreateInfo(
      flags = 0,
      attachments = Array(colorAttachment, depthAttachment),
      subpasses = Array(subpass),
      dependencies = Array.empty
    )

    vk.createRenderPass(device, renderPassCreateInfo)
  }

  def spvFile(name: String): ByteBuffer = {
    val ints = MutableList[Int]()
    val file = new File(getClass.getResource(s"/$name").toURI())
    val bytes = Files.readAllBytes(file.toPath())
    val buf =
      ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder())
    buf.put(bytes, 0, bytes.size)
    buf
  }

  def initShaderModule(name: String,
                       device: Vulkan.Device): Vulkan.ShaderModule = {
    val spv = spvFile(name)
    val info = new Vulkan.ShaderModuleCreateInfo(
      flags = 0,
      codeSize = spv.capacity,
      code = spv
    )
    vk.createShaderModule(device, info)
  }

  def initFramebuffers(device: Vulkan.Device,
                       imageViews: Array[Vulkan.ImageView],
                       depthImageView: Vulkan.ImageView,
                       renderPass: Vulkan.RenderPass,
                       width: Int,
                       height: Int): Array[Vulkan.Framebuffer] = {
    imageViews.map { v =>
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
  }

  def submitQueue(device: Vulkan.Device,
                  fence: Vulkan.Fence,
                  commandBuffer: Vulkan.CommandBuffer,
                  graphicsQueue: Vulkan.Queue): Unit = {
    val submitInfo = new Vulkan.SubmitInfo(
      waitSemaphores = Array.empty[Vulkan.Semaphore],
      waitDstStageMask =
        Array(Vulkan.PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT),
      commandBuffers = Array(commandBuffer),
      signalSemaphores = Array.empty[Vulkan.Semaphore]
    )
    vk.queueSubmit(graphicsQueue, 1, Array(submitInfo), fence)
    vk.waitForFences(device, 1, Array(fence), true, FENCE_TIMEOUT)
  }

  def initVertexBuffer(device: Vulkan.Device, size: Int): Vulkan.Buffer = {
    val bufferCreateInfo = new Vulkan.BufferCreateInfo(
      usage = Vulkan.BUFFER_USAGE_VERTEX_BUFFER_BIT,
      size = size,
      queueFamilyIndices = Array.empty[Int],
      sharingMode = Vulkan.SHARING_MODE_EXCLUSIVE,
      flags = 0)
    vk.createBuffer(device, bufferCreateInfo)
  }

  def initFence(device: Vulkan.Device): Vulkan.Fence = {
    vk.createFence(device, new Vulkan.FenceCreateInfo(flags = 0))
  }

  def beginRenderPass(commandBuffer: Vulkan.CommandBuffer,
                      renderPass: Vulkan.RenderPass,
                      currentBuffer: Int,
                      framebuffers: Array[Vulkan.Framebuffer],
                      width: Int,
                      height: Int,
                      clearValues: Array[Vulkan.ClearValue]): Unit = {

    val renderPassBeginInfo = new Vulkan.RenderPassBeginInfo(
      renderPass = renderPass,
      framebuffer = framebuffers(currentBuffer),
      renderArea = new Vulkan.Rect2D(
        offset = new Vulkan.Offset2D(x = 0, y = 0),
        extent = new Vulkan.Extent2D(width = width, height = height)),
      clearValues = clearValues
    )

    vk.cmdBeginRenderPass(commandBuffer,
                          renderPassBeginInfo,
                          Vulkan.SUBPASS_CONTENTS_INLINE)
  }

  def initPipeline(device: Vulkan.Device,
                   renderPass: Vulkan.RenderPass,
                   vertexModule: Vulkan.ShaderModule,
                   fragmentModule: Vulkan.ShaderModule,
                   pipelineLayout: Vulkan.PipelineLayout): Vulkan.Pipeline = {
    val vertexBinding = new Vulkan.VertexInputBindingDescription(
      binding = 0,
      inputRate = Vulkan.VERTEX_INPUT_RATE_VERTEX,
      stride = 32
    )
    val vertexAttrib0 = new Vulkan.VertexInputAttributeDescription(
      binding = 0,
      location = 0,
      format = Vulkan.FORMAT_R32G32B32A32_SFLOAT,
      offset = 0
    )
    val vertexAttrib1 = new Vulkan.VertexInputAttributeDescription(
      binding = 0,
      location = 1,
      format = Vulkan.FORMAT_R32G32B32A32_SFLOAT,
      offset = 16
    )
    val vertexShaderStage = new Vulkan.PipelineShaderStageCreateInfo(
      flags = 0,
      stage = Vulkan.SHADER_STAGE_VERTEX_BIT,
      module = vertexModule,
      name = "main"
    )
    val fragmentShaderStage = new Vulkan.PipelineShaderStageCreateInfo(
      flags = 0,
      stage = Vulkan.SHADER_STAGE_FRAGMENT_BIT,
      module = fragmentModule,
      name = "main"
    )
    val vertexInputStateCreateInfo =
      new Vulkan.PipelineVertexInputStateCreateInfo(
        flags = 0,
        vertexBindingDescriptions = Array(vertexBinding),
        vertexAttributeDescriptions = Array(vertexAttrib0, vertexAttrib1))

    val dynamicState = new Vulkan.PipelineDynamicStateCreateInfo(
      flags = 0,
      dynamicStates =
        Array(Vulkan.DYNAMIC_STATE_VIEWPORT, Vulkan.DYNAMIC_STATE_SCISSOR))

    val inputAssemblyStateCreateInfo =
      new Vulkan.PipelineInputAssemblyStateCreateInfo(
        flags = 0,
        topology = Vulkan.PRIMITIVE_TOPOLOGY_TRIANGLE_LIST,
        primitiveRestartEnable = false)
    val rasterizationStateCreateInfo =
      new Vulkan.PipelineRasterizationStateCreateInfo(
        flags = 0,
        polygonMode = Vulkan.POLYGON_MODE_FILL,
        cullMode = Vulkan.CULL_MODE_BACK_BIT,
        frontFace = Vulkan.FRONT_FACE_CLOCKWISE,
        depthClampEnable = true,
        rasterizerDiscardEnable = false,
        depthBiasEnable = false,
        depthBiasConstantFactor = 0,
        depthBiasClamp = 0,
        depthBiasSlopeFactor = 0,
        lineWidth = 1f
      )
    val colorBlendAttachmentState =
      new Vulkan.PipelineColorBlendAttachmentState(
        colorWriteMask = 0xf,
        blendEnable = false,
        alphaBlendOp = Vulkan.BLEND_OP_ADD,
        colorBlendOp = Vulkan.BLEND_OP_ADD,
        srcColorBlendFactor = Vulkan.BLEND_FACTOR_ZERO,
        dstColorBlendFactor = Vulkan.BLEND_FACTOR_ZERO,
        srcAlphaBlendFactor = Vulkan.BLEND_FACTOR_ZERO,
        dstAlphaBlendFactor = Vulkan.BLEND_FACTOR_ZERO
      )
    val colorBlendStateCreateInfo =
      new Vulkan.PipelineColorBlendStateCreateInfo(
        flags = 0,
        attachments = Array(colorBlendAttachmentState),
        logicOpEnable = false,
        logicOp = Vulkan.LOGIC_OP_NO_OP,
        blendConstants = Array(1f, 1f, 1f, 1f)
      )
    val viewportStateCreateInfo = new Vulkan.PipelineViewportStateCreateInfo(
      flags = 0,
      viewportCount = 1,
      viewports = Array.empty,
      scissorCount = 1,
      scissors = Array.empty)
    val depthStencilOpState = new Vulkan.StencilOpState(
      failOp = Vulkan.STENCIL_OP_KEEP,
      passOp = Vulkan.STENCIL_OP_KEEP,
      compareOp = Vulkan.COMPARE_OP_ALWAYS,
      compareMask = 0,
      reference = 0,
      depthFailOp = Vulkan.STENCIL_OP_KEEP,
      writeMask = 0
    )
    val depthStencilStateCreateInfo =
      new Vulkan.PipelineDepthStencilStateCreateInfo(
        flags = 0,
        depthTestEnable = true,
        depthWriteEnable = true,
        depthCompareOp = Vulkan.COMPARE_OP_LESS_OR_EQUAL,
        depthBoundsTestEnable = false,
        minDepthBounds = 0,
        maxDepthBounds = 0,
        stencilTestEnable = false,
        back = depthStencilOpState,
        front = depthStencilOpState
      )
    val multisampleStateCreateInfo =
      new Vulkan.PipelineMultisampleStateCreateInfo(
        flags = 0,
        sampleMask = 0,
        rasterizationSamples = Vulkan.SAMPLE_COUNT_1_BIT,
        sampleShadingEnable = false,
        alphaToCoverageEnable = false,
        alphaToOneEnable = false,
        minSampleShading = 0f
      )
    val pipelineInfo = new Vulkan.GraphicsPipelineCreateInfo(
      layout = pipelineLayout,
      basePipelineHandle = new Vulkan.Pipeline(0),
      basePipelineIndex = 0,
      flags = 0,
      vertexInputState = vertexInputStateCreateInfo,
      inputAssemblyState = inputAssemblyStateCreateInfo,
      rasterizationState = rasterizationStateCreateInfo,
      colorBlendState = colorBlendStateCreateInfo,
      multisampleState = multisampleStateCreateInfo,
      dynamicState = dynamicState,
      viewportState = viewportStateCreateInfo,
      depthStencilState = depthStencilStateCreateInfo,
      stages = Array(vertexShaderStage, fragmentShaderStage),
      renderPass = renderPass,
      subpass = 0
    )
    val pipelines = vk.createGraphicsPipelines(device, 1, Array(pipelineInfo))
    pipelines(0)
  }

  def submitQueueWait(device: Vulkan.Device,
                      fence: Vulkan.Fence,
                      commandBuffer: Vulkan.CommandBuffer,
                      graphicsQueue: Vulkan.Queue,
                      semaphore: Vulkan.Semaphore): Unit = {
    val submitInfo = new Vulkan.SubmitInfo(
      waitSemaphores = Array(semaphore),
      waitDstStageMask =
        Array(Vulkan.PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT),
      commandBuffers = Array(commandBuffer),
      signalSemaphores = Array.empty[Vulkan.Semaphore]
    )
    vk.queueSubmit(graphicsQueue, 1, Array(submitInfo), fence)
    var shouldWait = true
    while (shouldWait) {
      val res = vk.waitForFences(device, 1, Array(fence), true, FENCE_TIMEOUT)
      //println(s"waiting...${res.value}")
      if (res.value != Vulkan.TIMEOUT.value) {
        println("finished waiting")
        shouldWait = false
      }
    }
  }
}
