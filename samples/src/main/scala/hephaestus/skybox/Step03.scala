package hephaestus
package skybox

import hephaestus.platform._
import hephaestus.io.Buffer

import java.nio.{ByteBuffer, ByteOrder}
import java.io.File
import java.io.InputStream
import java.nio.file.Files

import javax.imageio._
import java.awt.image._

import scodec.codecs._
import scodec.stream._
import cats.implicits._
import scodec.interop.cats._

object Step03 {

  val FENCE_TIMEOUT = 100000000
  val width = 500
  val height = 500
  val textureWidth = 1024 //900
  val textureHeight = 512 //1201
  val cubeTextureWidth = 1024
  val cubeTextureHeight = 1024
  val scale = 0.25f

  val glfw = new GLFW()
  val vk = new Vulkan()

  val name = "skybox01"
  val skyboxFile = "skybox.png"
  val terrainFile = "terrain.model"
  val terrainTextureFile = "terrain-texture.png"

  case class Component(size: Int, num: Int)
  case class Header(components: List[Component])

  //sucessfully decoded vertex data
  //now need to integrate it with the rest of the system
  def decodeFile(): (List[Component], List[Buffer[Byte]]) = {
    val perComponent = (uint32L ~ uint32L)
      .map {
        case (n, s) =>
          Component(n.toInt, s.toInt)
      }
      .contramap((c: Component) => (c.num, c.size))
      .fuse

    val vertexData = for {
      nComponents <- uint32L
      components <- listOfN(provide(nComponents.toInt), perComponent) // repeat this
      bufs <- components.traverse(c =>
        bytes(c.size.toInt * c.num.toInt).asDecoder.map(b =>
          Buffer.direct[Byte](b.toArray: _*)))
    } yield (components, bufs)
    val decoder: StreamDecoder[(List[Component], List[Buffer[Byte]])] =
      decode.once(vertexData)
    val fileStream: InputStream =
      getClass.getResourceAsStream(s"/$terrainFile")
    decoder.decodeInputStream(fileStream).runLog.unsafeRun().head
  }

  //need to compare image types to supported types
  //need to stage images, and set sample swizzles from input type
  def loadTexture(name: String): ByteBuffer = {
    val file = new File(getClass.getResource(s"/$name").toURI())
    val img = ImageIO.read(file)
    val width = img.getWidth
    val height = img.getHeight
    println(s"width $width height $height type ${img.getType}")
    val data = img.getRaster.getDataBuffer.asInstanceOf[DataBufferByte].getData
    println(s"data size ${data.size}, should be ${width * height * 4}")
    val buffer = Buffer.direct(data: _*).value
    buffer
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

  def spvFile(name: String): ByteBuffer = {
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

  def main(args: Array[String]): Unit = {
    val (comps, datas) = decodeFile()
    //position, normal, uv
    val terrainVertexBytes = datas.head.value
    val terrainPolygonBytes = datas(1).value
    glfw.init()
    val instance = vk.createInstance(
      new Vulkan.InstanceCreateInfo(
        applicationInfo = new Vulkan.ApplicationInfo(applicationName = name,
                                                     applicationVersion = 1,
                                                     engineName = name,
                                                     engineVersion = 1,
                                                     apiVersion =
                                                       Vulkan.API_VERSION_1_0),
        enabledExtensionNames = (Vulkan.EXT_DEBUG_REPORT_EXTENSION_NAME :: glfw
          .getRequiredInstanceExtensions()
          .toList).toArray,
        enabledLayerNames = Array(Vulkan.LAYER_LUNARG_STANDARD_VALIDATION_NAME,
                                  Vulkan.LAYER_LUNARG_API_DUMP_NAME)
      ))
    vk.debugReport(instance)

    glfw.windowHint(GLFW.CLIENT_API, GLFW.NO_API)
    val window = glfw.createWindow(width, height, name)
    val surface = glfw.createWindowSurface(instance, window)

    val physicalDevice = vk.enumeratePhysicalDevices(instance)(0)
    val qi = initGraphicsPresentQueueFamilyIndex(instance, physicalDevice)
    val device = vk.createDevice(
      physicalDevice,
      new Vulkan.DeviceCreateInfo(
        queueCreateInfos = Array(
          new Vulkan.DeviceQueueCreateInfo(flags = 0,
                                           queueFamilyIndex = qi,
                                           queuePriorities = Array(0f))),
        enabledExtensionNames = Array(Vulkan.SWAPCHAIN_EXTENSION_NAME)
      )
    )

    val commandPool = vk.createCommandPool(
      device,
      new Vulkan.CommandPoolCreateInfo(flags = Vulkan.COMMAND_POOL_BLANK_FLAG,
                                       queueFamilyIndex = qi))
    val primaryCommandBuffer = vk.allocateCommandBuffers(
      device,
      new Vulkan.CommandBufferAllocateInfo(
        commandPool = commandPool,
        level = Vulkan.COMMAND_BUFFER_LEVEL_PRIMARY,
        commandBufferCount = 1))
    val secondaryCommandBuffer = vk.allocateCommandBuffers(
      device,
      new Vulkan.CommandBufferAllocateInfo(
        commandPool = commandPool,
        level = Vulkan.COMMAND_BUFFER_LEVEL_SECONDARY,
        commandBufferCount = 1))
    val formats = vk.getPhysicalDeviceSurfaceFormats(physicalDevice, surface)
    val swapchainFormat =
      if (formats(0).format == Vulkan.FORMAT_UNDEFINED)
        Vulkan.FORMAT_B8G8R8A8_UNORM
      else formats(0).format
    val surfaceCapabilities =
      vk.getPhysicalDeviceSurfaceCapabilities(physicalDevice, surface)
    val swapchainExtent =
      if (surfaceCapabilities.currentExtent.width == 0xFFFFFFFF) {
        val ewidth =
          if (width < surfaceCapabilities.minImageExtent.width)
            surfaceCapabilities.minImageExtent.width
          else if (width > surfaceCapabilities.maxImageExtent.width)
            surfaceCapabilities.maxImageExtent.width
          else width
        val eheight =
          if (height < surfaceCapabilities.minImageExtent.height)
            surfaceCapabilities.minImageExtent.height
          else if (height > surfaceCapabilities.maxImageExtent.height)
            surfaceCapabilities.maxImageExtent.height
          else height
        new Vulkan.Extent2D(ewidth, eheight)
      } else surfaceCapabilities.currentExtent

    val preTransform =
      if ((surfaceCapabilities.supportedTransforms & Vulkan.SURFACE_TRANSFORM_IDENTITY_BIT) > 0)
        Vulkan.SURFACE_TRANSFORM_IDENTITY_BIT
      else surfaceCapabilities.currentTransform
    val swapchain = vk.createSwapchain(
      device,
      new Vulkan.SwapchainCreateInfo(
        flags = 0,
        surface = surface,
        minImageCount = surfaceCapabilities.minImageCount,
        imageFormat = swapchainFormat,
        imageExtent = swapchainExtent,
        preTransform = preTransform,
        compositeAlpha = Vulkan.COMPOSITE_ALPHA_OPAQUE_BIT,
        imageArrayLayers = 1,
        presentMode = Vulkan.PRESENT_MODE_FIFO,
        clipped = true,
        imageColorSpace = Vulkan.COLORSPACE_SRGB_NONLINEAR,
        imageUsage = Vulkan.IMAGE_USAGE_COLOR_ATTACHMENT_BIT,
        imageSharingMode = Vulkan.SHARING_MODE_EXCLUSIVE,
        queueFamilyIndices = Array.empty[Int]
      )
    )

    val swapchainImages = vk.getSwapchainImages(device, swapchain)
    val imageViews = swapchainImages.map { i =>
      vk.createImageView(
        device,
        new Vulkan.ImageViewCreateInfo(
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
      )
    }
    val formatProperties = vk.getPhysicalDeviceFormatProperties(
      physicalDevice,
      Vulkan.FORMAT_D16_UNORM)
    val imageTiling =
      if (Vulkan.FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT & formatProperties.linearTilingFeatures)
        Vulkan.IMAGE_TILING_LINEAR
      else if (Vulkan.FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT & formatProperties.optimalTilingFeatures)
        Vulkan.IMAGE_TILING_OPTIMAL
      else throw new Error("depth not supported")
    val depthImage = vk.createImage(
      device,
      new Vulkan.ImageCreateInfo(
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
    )

    val memoryProperties = vk.getPhysicalDeviceMemoryProperties(physicalDevice)

    val depthImageMemoryRequirements =
      vk.getImageMemoryRequirements(device, depthImage)
    val depthImageMemoryTypeIndex = memoryProperties.memoryTypes.zipWithIndex
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

    val depthImageMemory = vk.allocateMemory(
      device,
      new Vulkan.MemoryAllocateInfo(
        allocationSize = depthImageMemoryRequirements.size,
        memoryTypeIndex = depthImageMemoryTypeIndex))
    vk.bindImageMemory(device, depthImage, depthImageMemory, 0)
    val depthImageView = vk.createImageView(
      device,
      new Vulkan.ImageViewCreateInfo(
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
    )

    val uniformData = Buffer.direct(scale, 0f).value
    val cubeUniformData =
      hephaestus.lunarg.tutorial.Cube.uniformData(width, height, 0)

    val uniformBuffer = vk.createBuffer(
      device,
      new Vulkan.BufferCreateInfo(
        usage = Vulkan.BUFFER_USAGE_UNIFORM_BUFFER_BIT,
        size = uniformData.capacity,
        queueFamilyIndices = Array.empty[Int],
        sharingMode = Vulkan.SHARING_MODE_EXCLUSIVE,
        flags = 0
      )
    )
    val uniformBufferMemoryRequirements =
      vk.getBufferMemoryRequirements(device, uniformBuffer)
    val uniformBufferMemoryTypeIndex = memoryTypeIndex(
      memoryProperties,
      uniformBufferMemoryRequirements,
      Vulkan.MEMORY_PROPERTY_HOST_VISIBLE_BIT | Vulkan.MEMORY_PROPERTY_HOST_COHERENT_BIT)
    val uniformBufferMemory = vk.allocateMemory(
      device,
      new Vulkan.MemoryAllocateInfo(allocationSize =
                                      uniformBufferMemoryRequirements.size,
                                    memoryTypeIndex =
                                      uniformBufferMemoryTypeIndex))
    val uniformDataPtr = vk.mapMemory(device,
                                      uniformBufferMemory,
                                      0,
                                      uniformBufferMemoryRequirements.size,
                                      0)
    vk.loadMemory(uniformDataPtr, uniformData)
    vk.unmapMemory(device, uniformBufferMemory)
    vk.bindBufferMemory(device, uniformBuffer, uniformBufferMemory, 0)

    val cubeUniformBuffer = vk.createBuffer(
      device,
      new Vulkan.BufferCreateInfo(
        usage = Vulkan.BUFFER_USAGE_UNIFORM_BUFFER_BIT,
        size = cubeUniformData.capacity,
        queueFamilyIndices = Array.empty[Int],
        sharingMode = Vulkan.SHARING_MODE_EXCLUSIVE,
        flags = 0
      )
    )
    val cubeUniformBufferMemoryRequirements =
      vk.getBufferMemoryRequirements(device, cubeUniformBuffer)
    val cubeUniformBufferMemoryTypeIndex = memoryTypeIndex(
      memoryProperties,
      cubeUniformBufferMemoryRequirements,
      Vulkan.MEMORY_PROPERTY_HOST_VISIBLE_BIT | Vulkan.MEMORY_PROPERTY_HOST_COHERENT_BIT)
    val cubeUniformBufferMemory = vk.allocateMemory(
      device,
      new Vulkan.MemoryAllocateInfo(allocationSize =
                                      cubeUniformBufferMemoryRequirements.size,
                                    memoryTypeIndex =
                                      cubeUniformBufferMemoryTypeIndex))
    val cubeUniformDataPtr = vk.mapMemory(
      device,
      cubeUniformBufferMemory,
      0,
      cubeUniformBufferMemoryRequirements.size,
      0)
    vk.loadMemory(cubeUniformDataPtr, cubeUniformData)
    vk.unmapMemory(device, cubeUniformBufferMemory)
    vk.bindBufferMemory(device, cubeUniformBuffer, cubeUniformBufferMemory, 0)

    val textureFormatProperties = vk.getPhysicalDeviceFormatProperties(
      physicalDevice,
      Vulkan.FORMAT_R8G8B8A8_UNORM)
    if (!(Vulkan.FORMAT_FEATURE_SAMPLED_IMAGE_BIT & textureFormatProperties.linearTilingFeatures))
      throw new Error("image needs staging!")
    val textureImage = vk.createImage(
      device,
      new Vulkan.ImageCreateInfo(
        flags = 0,
        imageType = Vulkan.IMAGE_TYPE_2D,
        format = Vulkan.FORMAT_R8G8B8A8_UNORM,
        extent = new Vulkan.Extent3D(textureWidth, textureHeight, 1),
        mipLevels = 1,
        arrayLayers = 1,
        samples = 1,
        tiling = Vulkan.IMAGE_TILING_LINEAR,
        usage = Vulkan.IMAGE_USAGE_SAMPLED_BIT,
        sharingMode = Vulkan.SHARING_MODE_EXCLUSIVE,
        queueFamilyIndices = Array.empty,
        initialLayout = Vulkan.IMAGE_LAYOUT_PREINITIALIZED
      )
    )
    val textureImageMemoryRequirements =
      vk.getImageMemoryRequirements(device, textureImage)
    val textureMemory = vk.allocateMemory(
      device,
      new Vulkan.MemoryAllocateInfo(
        allocationSize = textureImageMemoryRequirements.size,
        memoryTypeIndex = memoryTypeIndex(
          memoryProperties,
          textureImageMemoryRequirements,
          Vulkan.MEMORY_PROPERTY_HOST_VISIBLE_BIT | Vulkan.MEMORY_PROPERTY_HOST_COHERENT_BIT)
      )
    )
    vk.bindImageMemory(device, textureImage, textureMemory, 0)

    val textureData = loadTexture(skyboxFile)
    val textureDataPtr = vk.mapMemory(device,
                                      textureMemory,
                                      0,
                                      textureImageMemoryRequirements.size,
                                      0)
    vk.loadMemory(textureDataPtr, textureData)
    vk.unmapMemory(device, textureMemory)

    val textureImageView = vk.createImageView(
      device,
      new Vulkan.ImageViewCreateInfo(
        flags = 0,
        image = textureImage,
        viewType = Vulkan.IMAGE_VIEW_TYPE_2D,
        format = Vulkan.FORMAT_R8G8B8A8_UNORM,
        components = new Vulkan.ComponentMapping(
          Vulkan.COMPONENT_SWIZZLE_A,
          Vulkan.COMPONENT_SWIZZLE_B,
          Vulkan.COMPONENT_SWIZZLE_G,
          Vulkan.COMPONENT_SWIZZLE_R
        ),
        subresourceRange =
          new Vulkan.ImageSubresourceRange(aspectMask =
                                             Vulkan.IMAGE_ASPECT_COLOR_BIT,
                                           baseMipLevel = 0,
                                           levelCount = 1,
                                           baseArrayLayer = 0,
                                           layerCount = 1)))

    val cubeTextureFormatProperties = vk.getPhysicalDeviceFormatProperties(
      physicalDevice,
      Vulkan.FORMAT_R8G8B8A8_UNORM)
    if (!(Vulkan.FORMAT_FEATURE_SAMPLED_IMAGE_BIT & cubeTextureFormatProperties.linearTilingFeatures))
      throw new Error("image needs staging!")

    val cubeTextureImage = vk.createImage(
      device,
      new Vulkan.ImageCreateInfo(
        flags = 0,
        imageType = Vulkan.IMAGE_TYPE_2D,
        format = Vulkan.FORMAT_R8G8B8A8_UNORM,
        extent = new Vulkan.Extent3D(cubeTextureWidth, cubeTextureHeight, 1),
        mipLevels = 1,
        arrayLayers = 1,
        samples = 1,
        tiling = Vulkan.IMAGE_TILING_LINEAR,
        usage = Vulkan.IMAGE_USAGE_SAMPLED_BIT,
        sharingMode = Vulkan.SHARING_MODE_EXCLUSIVE,
        queueFamilyIndices = Array.empty,
        initialLayout = Vulkan.IMAGE_LAYOUT_PREINITIALIZED
      )
    )
    val cubeTextureImageMemoryRequirements =
      vk.getImageMemoryRequirements(device, cubeTextureImage)
    val cubeTextureMemory = vk.allocateMemory(
      device,
      new Vulkan.MemoryAllocateInfo(
        allocationSize = cubeTextureImageMemoryRequirements.size,
        memoryTypeIndex = memoryTypeIndex(
          memoryProperties,
          cubeTextureImageMemoryRequirements,
          Vulkan.MEMORY_PROPERTY_HOST_VISIBLE_BIT | Vulkan.MEMORY_PROPERTY_HOST_COHERENT_BIT)
      )
    )
    vk.bindImageMemory(device, cubeTextureImage, cubeTextureMemory, 0)

    val cubeTextureData = loadTexture(terrainTextureFile) // lunarg.tutorial.Cube.textureData(cubeTextureWidth, cubeTextureHeight, 0)
    val cubeTextureDataPtr = vk.mapMemory(
      device,
      cubeTextureMemory,
      0,
      cubeTextureImageMemoryRequirements.size,
      0)
    vk.loadMemory(cubeTextureDataPtr, cubeTextureData)
    vk.unmapMemory(device, cubeTextureMemory)

    val cubeTextureImageView = vk.createImageView(
      device,
      new Vulkan.ImageViewCreateInfo(
        flags = 0,
        image = cubeTextureImage,
        viewType = Vulkan.IMAGE_VIEW_TYPE_2D,
        format = Vulkan.FORMAT_R8G8B8A8_UNORM,
        components = new Vulkan.ComponentMapping(
          Vulkan.COMPONENT_SWIZZLE_A,
          Vulkan.COMPONENT_SWIZZLE_B,
          Vulkan.COMPONENT_SWIZZLE_G,
          Vulkan.COMPONENT_SWIZZLE_R
        ),
        subresourceRange =
          new Vulkan.ImageSubresourceRange(aspectMask =
                                             Vulkan.IMAGE_ASPECT_COLOR_BIT,
                                           baseMipLevel = 0,
                                           levelCount = 1,
                                           baseArrayLayer = 0,
                                           layerCount = 1)
      )
    )

    val textureSampler = vk.createSampler(
      device,
      new Vulkan.SamplerCreateInfo(
        flags = 0,
        magFilter = Vulkan.FILTER_NEAREST,
        minFilter = Vulkan.FILTER_NEAREST,
        mipmapMode = Vulkan.SAMPLER_MIPMAP_MODE_NEAREST,
        addressModeU = Vulkan.SAMPLER_ADDRESS_MODE_REPEAT,
        addressModeV = Vulkan.SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE,
        addressModeW = Vulkan.SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE,
        mipLodBias = 0f,
        anisotropyEnable = false,
        maxAnisotropy = 0f,
        compareOp = Vulkan.COMPARE_OP_NEVER,
        minLod = 0f,
        maxLod = 0f,
        compareEnable = false,
        borderColor = Vulkan.BORDER_COLOR_FLOAT_OPAQUE_WHITE,
        unnormalizedCoordinates = false
      )
    )

    val descriptorSetLayout = vk.createDescriptorSetLayout(
      device,
      new Vulkan.DescriptorSetLayoutCreateInfo(
        flags = 0,
        bindings = Array(
          new Vulkan.DescriptorSetLayoutBinding(
            binding = 0,
            descriptorType = Vulkan.DESCRIPTOR_TYPE_UNIFORM_BUFFER,
            descriptorCount = 1,
            stageFlags = Vulkan.SHADER_STAGE_VERTEX_BIT,
            immutableSamplers = Array.empty[Vulkan.Sampler]
          ),
          new Vulkan.DescriptorSetLayoutBinding(
            binding = 1,
            descriptorType = Vulkan.DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
            descriptorCount = 1,
            stageFlags = Vulkan.SHADER_STAGE_FRAGMENT_BIT,
            immutableSamplers = Array.empty
          )
        )
      )
    )

    val pipelineLayout = vk.createPipelineLayout(
      device,
      new Vulkan.PipelineLayoutCreateInfo(flags = 0,
                                          setLayouts =
                                            Array(descriptorSetLayout),
                                          pushConstantRanges = Array.empty))

    val descriptorPool = vk.createDescriptorPool(
      device,
      new Vulkan.DescriptorPoolCreateInfo(
        flags = 0,
        maxSets = 2,
        poolSizes = Array(
          new Vulkan.DescriptorPoolSize(
            tpe = Vulkan.DESCRIPTOR_TYPE_UNIFORM_BUFFER,
            descriptorCount = 2),
          new Vulkan.DescriptorPoolSize(
            tpe = Vulkan.DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
            descriptorCount = 2)
        )
      )
    )

    val descriptorSets = vk.allocateDescriptorSets(
      device,
      new Vulkan.DescriptorSetAllocateInfo(
        descriptorPool = descriptorPool,
        setLayouts = Array(descriptorSetLayout, descriptorSetLayout)))

    //this could be dynamic
    //have forgotten why we couldn't use the same uniform buffer in the first place
    val writeDescriptorSets = Array(
      new Vulkan.WriteDescriptorSet(
        dstSet = descriptorSets(0),
        dstBinding = 0,
        dstArrayElement = 0,
        descriptorCount = 1,
        descriptorType = Vulkan.DESCRIPTOR_TYPE_UNIFORM_BUFFER,
        imageInfo = Array.empty[Vulkan.DescriptorImageInfo],
        bufferInfo = Array(
          new Vulkan.DescriptorBufferInfo(buffer = uniformBuffer,
                                          offset = 0,
                                          range = uniformData.capacity)),
        texelBufferView = Array.empty[Vulkan.BufferView]
      ),
      new Vulkan.WriteDescriptorSet(
        dstSet = descriptorSets(0),
        dstBinding = 1,
        dstArrayElement = 0,
        descriptorCount = 1,
        descriptorType = Vulkan.DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
        imageInfo = Array(
          new Vulkan.DescriptorImageInfo(sampler = textureSampler,
                                         imageView = textureImageView,
                                         imageLayout =
                                           Vulkan.IMAGE_LAYOUT_GENERAL)),
        bufferInfo = Array.empty,
        texelBufferView = Array.empty
      ),
      new Vulkan.WriteDescriptorSet(
        dstSet = descriptorSets(1),
        dstBinding = 0,
        dstArrayElement = 0,
        descriptorCount = 1,
        descriptorType = Vulkan.DESCRIPTOR_TYPE_UNIFORM_BUFFER,
        imageInfo = Array.empty[Vulkan.DescriptorImageInfo],
        bufferInfo = Array(
          new Vulkan.DescriptorBufferInfo(buffer = cubeUniformBuffer,
                                          offset = 0,
                                          range = cubeUniformData.capacity)),
        texelBufferView = Array.empty[Vulkan.BufferView]
      ),
      new Vulkan.WriteDescriptorSet(
        dstSet = descriptorSets(1),
        dstBinding = 1,
        dstArrayElement = 0,
        descriptorCount = 1,
        descriptorType = Vulkan.DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
        imageInfo = Array(
          new Vulkan.DescriptorImageInfo(sampler = textureSampler,
                                         imageView = cubeTextureImageView,
                                         imageLayout =
                                           Vulkan.IMAGE_LAYOUT_GENERAL)),
        bufferInfo = Array.empty,
        texelBufferView = Array.empty
      )
    )

    vk.updateDescriptorSets(device,
                            4,
                            writeDescriptorSets,
                            0,
                            Array.empty[Vulkan.CopyDescriptorSet])

    val vertexData =
      Buffer.direct(-1f, -1f, -1f, 1f, 1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f).value

    val cubeVertexData = terrainVertexBytes

    val vertexBuffer = vk.createBuffer(
      device,
      new Vulkan.BufferCreateInfo(
        usage = Vulkan.BUFFER_USAGE_VERTEX_BUFFER_BIT,
        size = vertexData.capacity + cubeVertexData.capacity,
        queueFamilyIndices = Array.empty[Int],
        sharingMode = Vulkan.SHARING_MODE_EXCLUSIVE,
        flags = 0
      )
    )

    val vertexBufferMemoryRequirements =
      vk.getBufferMemoryRequirements(device, vertexBuffer)
    val vertexBufferMemoryTypeIndex = memoryTypeIndex(
      memoryProperties,
      vertexBufferMemoryRequirements,
      Vulkan.MEMORY_PROPERTY_HOST_VISIBLE_BIT | Vulkan.MEMORY_PROPERTY_HOST_COHERENT_BIT)
    val vertexBufferMemory = vk.allocateMemory(
      device,
      new Vulkan.MemoryAllocateInfo(allocationSize =
                                      vertexBufferMemoryRequirements.size,
                                    memoryTypeIndex =
                                      vertexBufferMemoryTypeIndex))
    val vertexDataPtr = vk.mapMemory(device,
                                     vertexBufferMemory,
                                     0,
                                     vertexBufferMemoryRequirements.size,
                                     0)
    vk.loadMemory(vertexDataPtr, vertexData)
    vk.loadMemory(vertexDataPtr + vertexData.capacity, cubeVertexData)
    vk.unmapMemory(device, vertexBufferMemory)
    vk.bindBufferMemory(device, vertexBuffer, vertexBufferMemory, 0)

    val elementData = terrainPolygonBytes
    val elementBuffer = vk.createBuffer(
      device,
      new Vulkan.BufferCreateInfo(
        usage = Vulkan.BUFFER_USAGE_INDEX_BUFFER_BIT,
        size = elementData.capacity,
        queueFamilyIndices = Array.empty[Int],
        sharingMode = Vulkan.SHARING_MODE_EXCLUSIVE,
        flags = 0
      )
    )

    val elementBufferMemoryRequirements =
      vk.getBufferMemoryRequirements(device, elementBuffer)
    val elementBufferMemoryTypeIndex = memoryTypeIndex(
      memoryProperties,
      elementBufferMemoryRequirements,
      Vulkan.MEMORY_PROPERTY_HOST_VISIBLE_BIT | Vulkan.MEMORY_PROPERTY_HOST_COHERENT_BIT)
    val elementBufferMemory = vk.allocateMemory(
      device,
      new Vulkan.MemoryAllocateInfo(allocationSize =
                                      elementBufferMemoryRequirements.size,
                                    memoryTypeIndex =
                                      elementBufferMemoryTypeIndex))
    val elementDataPtr = vk.mapMemory(device,
                                      elementBufferMemory,
                                      0,
                                      elementBufferMemoryRequirements.size,
                                      0)
    vk.loadMemory(elementDataPtr, elementData)
    vk.unmapMemory(device, elementBufferMemory)
    vk.bindBufferMemory(device, elementBuffer, elementBufferMemory, 0)

    val renderPass = vk.createRenderPass(
      device,
      new Vulkan.RenderPassCreateInfo(
        flags = 0,
        attachments = Array(
          new Vulkan.AttachmentDescription(
            format = swapchainFormat,
            samples = Vulkan.SAMPLE_COUNT_1_BIT,
            loadOp = Vulkan.ATTACHMENT_LOAD_OP_CLEAR,
            storeOp = Vulkan.ATTACHMENT_STORE_OP_STORE,
            stencilLoadOp = Vulkan.ATTACHMENT_LOAD_OP_DONT_CARE,
            stencilStoreOp = Vulkan.ATTACHMENT_STORE_OP_DONT_CARE,
            initialLayout = Vulkan.IMAGE_LAYOUT_UNDEFINED,
            finalLayout = Vulkan.IMAGE_LAYOUT_PRESENT_SRC_KHR,
            flags = 0
          ),
          new Vulkan.AttachmentDescription(
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
        ),
        subpasses = Array(
          new Vulkan.SubpassDescription(
            pipelineBindPoint = Vulkan.PIPELINE_BIND_POINT_GRAPHICS,
            flags = 0,
            inputAttachments = Array.empty[Vulkan.AttachmentReference],
            colorAttachments = Array(
              new Vulkan.AttachmentReference(
                attachment = 0,
                layout = Vulkan.IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)),
            resolveAttachments = Array.empty[Vulkan.AttachmentReference],
            depthStencilAttachment = Array(new Vulkan.AttachmentReference(
              attachment = 1,
              layout = Vulkan.IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL)),
            preserveAttachments = Array.empty[Int]
          )),
        dependencies = Array.empty
      )
    )

    val vertexModule = initShaderModule("skybox.vert.spv", device)
    val fragmentModule = initShaderModule("skybox.frag.spv", device)

    val cubeVertexModule = initShaderModule("terrain.vert.spv", device)
    val cubeFragmentModule = initShaderModule("texture.frag.spv", device)

    val framebuffers = imageViews.map { v =>
      vk.createFramebuffer(device,
                           new Vulkan.FramebufferCreateInfo(
                             flags = 0,
                             renderPass = renderPass,
                             attachments = Array(v, depthImageView),
                             width = width,
                             height = height,
                             layers = 1))
    }

    vk.beginCommandBuffer(
      secondaryCommandBuffer,
      new Vulkan.CommandBufferBeginInfo(
        Vulkan.COMMAND_BUFFER_USAGE_RENDER_PASS_CONTINUE_BIT | Vulkan.COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT,
        new Vulkan.CommandBufferInheritanceInfo(renderPass = renderPass)
      )
    )

    //can bind multiple vertex buffers to a pipeline (i.e. give them different strides and attributes)
    //but what is the point, since they need to go in the same shader pipeline?
    //should be for if the attributes are stored in different buffers
    val vertexInputStateCreateInfo =
      new Vulkan.PipelineVertexInputStateCreateInfo(
        flags = 0,
        vertexBindingDescriptions = Array(
          new Vulkan.VertexInputBindingDescription(
            binding = 0,
            inputRate = Vulkan.VERTEX_INPUT_RATE_VERTEX,
            stride = 8)),
        vertexAttributeDescriptions = Array(
          new Vulkan.VertexInputAttributeDescription(
            binding = 0,
            location = 0,
            format = Vulkan.FORMAT_R32G32_SFLOAT,
            offset = 0
          ))
      )

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
        frontFace = Vulkan.FRONT_FACE_COUNTER_CLOCKWISE,
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
      stages = Array(
        new Vulkan.PipelineShaderStageCreateInfo(
          flags = 0,
          stage = Vulkan.SHADER_STAGE_VERTEX_BIT,
          module = vertexModule,
          name = "main"
        ),
        new Vulkan.PipelineShaderStageCreateInfo(
          flags = 0,
          stage = Vulkan.SHADER_STAGE_FRAGMENT_BIT,
          module = fragmentModule,
          name = "main")
      ),
      renderPass = renderPass,
      subpass = 0
    )

    val cubeVertexInputStateCreateInfo =
      new Vulkan.PipelineVertexInputStateCreateInfo(
        flags = 0,
        vertexBindingDescriptions = Array(
          new Vulkan.VertexInputBindingDescription(
            binding = 0,
            inputRate = Vulkan.VERTEX_INPUT_RATE_VERTEX,
            stride = 32)),
        vertexAttributeDescriptions = Array(
          new Vulkan.VertexInputAttributeDescription(
            binding = 0,
            location = 0,
            format = Vulkan.FORMAT_R32G32B32_SFLOAT,
            offset = 0
          ),
          new Vulkan.VertexInputAttributeDescription(
            binding = 0,
            location = 1,
            format = Vulkan.FORMAT_R32G32_SFLOAT,
            offset = 24
          )
        )
      )
    val cubeRasterizationStateCreateInfo =
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
    val cubePipelineInfo = new Vulkan.GraphicsPipelineCreateInfo(
      layout = pipelineLayout,
      basePipelineHandle = new Vulkan.Pipeline(0),
      basePipelineIndex = 0,
      flags = 0,
      vertexInputState = cubeVertexInputStateCreateInfo,
      inputAssemblyState = inputAssemblyStateCreateInfo,
      rasterizationState = cubeRasterizationStateCreateInfo,
      colorBlendState = colorBlendStateCreateInfo,
      multisampleState = multisampleStateCreateInfo,
      dynamicState = dynamicState,
      viewportState = viewportStateCreateInfo,
      depthStencilState = depthStencilStateCreateInfo,
      stages = Array(
        new Vulkan.PipelineShaderStageCreateInfo(
          flags = 0,
          stage = Vulkan.SHADER_STAGE_VERTEX_BIT,
          module = cubeVertexModule,
          name = "main"
        ),
        new Vulkan.PipelineShaderStageCreateInfo(
          flags = 0,
          stage = Vulkan.SHADER_STAGE_FRAGMENT_BIT,
          module = cubeFragmentModule,
          name = "main")
      ),
      renderPass = renderPass,
      subpass = 0
    )
    val pipelines = vk.createGraphicsPipelines(
      device,
      2,
      Array(pipelineInfo, cubePipelineInfo))

    vk.cmdBindPipeline(secondaryCommandBuffer,
                       Vulkan.PIPELINE_BIND_POINT_GRAPHICS,
                       pipelines(0))
    //set number should be specified in the shader
    vk.cmdBindDescriptorSets(secondaryCommandBuffer,
                             Vulkan.PIPELINE_BIND_POINT_GRAPHICS,
                             pipelineLayout,
                             0,
                             1,
                             Array(descriptorSets(0)),
                             0,
                             Array.empty)
    //we need to bind this twice, because the format of the data is different.  If not, we would just bind it once and set the offset in the draw

    vk.cmdBindVertexBuffers(secondaryCommandBuffer,
                            0,
                            1,
                            Array(vertexBuffer),
                            Array(0))
    vk.cmdBindIndexBuffer(secondaryCommandBuffer,
                          elementBuffer,
                          0,
                          Vulkan.INDEX_TYPE_UINT32)

    vk.cmdSetViewport(secondaryCommandBuffer,
                      0,
                      1,
                      Array(
                        new Vulkan.Viewport(height = height,
                                            width = width,
                                            minDepth = 0f,
                                            maxDepth = 1f,
                                            x = 0,
                                            y = 0)))
    vk.cmdSetScissor(
      secondaryCommandBuffer,
      0,
      1,
      Array(
        new Vulkan.Rect2D(
          extent = new Vulkan.Extent2D(width = width, height = height),
          offset = new Vulkan.Offset2D(x = 0, y = 0)
        )))
    vk.cmdDraw(secondaryCommandBuffer, 6, 1, 0, 0)

    vk.cmdBindVertexBuffers(secondaryCommandBuffer,
                            0,
                            1,
                            Array(vertexBuffer),
                            Array(vertexData.capacity))
    vk.cmdBindPipeline(secondaryCommandBuffer,
                       Vulkan.PIPELINE_BIND_POINT_GRAPHICS,
                       pipelines(1))
    vk.cmdBindDescriptorSets(secondaryCommandBuffer,
                             Vulkan.PIPELINE_BIND_POINT_GRAPHICS,
                             pipelineLayout,
                             0,
                             1,
                             Array(descriptorSets(1)),
                             0,
                             Array.empty)
    vk.cmdDrawIndexed(secondaryCommandBuffer, comps(1).num * 3, 1, 0, 0, 0)

    vk.endCommandBuffer(secondaryCommandBuffer)

    val acquireSemaphore =
      vk.createSemaphore(device, new Vulkan.SemaphoreCreateInfo(flags = 0))
    val renderSemaphore =
      vk.createSemaphore(device, new Vulkan.SemaphoreCreateInfo(flags = 0))
    val fence = vk.createFence(device, new Vulkan.FenceCreateInfo(flags = 0))
    val graphicsQueue = vk.getDeviceQueue(device, qi, 0)

    (0 until 5000).foreach { i =>
      val theta = (i % 5000).toDouble / 5000.0
      val uniformDataPerFrame = Buffer.direct(scale, theta.toFloat).value
      //since memory is coherent, we just need to do a memcopy
      vk.loadMemory(uniformDataPtr, uniformDataPerFrame)
      val cubeUniformDataPerFrame =
        hephaestus.lunarg.tutorial.Cube.uniformData(width, height, i)
      vk.loadMemory(cubeUniformDataPtr, cubeUniformDataPerFrame)
      // val textureDataPerFrame = hephaestus.lunarg.tutorial.Cube.textureData(cubeTextureWidth, cubeTextureHeight, i)
      //  vk.loadMemory(cubeTextureDataPtr, textureDataPerFrame)

      val currentBuffer = vk.acquireNextImageKHR(device,
                                                 swapchain,
                                                 java.lang.Long.MAX_VALUE,
                                                 acquireSemaphore,
                                                 new Vulkan.Fence(0))
      vk.beginCommandBuffer(
        primaryCommandBuffer,
        new Vulkan.CommandBufferBeginInfo(
          flags = Vulkan.COMMAND_BUFFER_USAGE_BLANK_FLAG,
          inheritanceInfo = Vulkan.COMMAND_BUFFER_INHERITANCE_INFO_NULL_HANDLE)
      )

      vk.cmdBeginRenderPass(
        primaryCommandBuffer,
        new Vulkan.RenderPassBeginInfo(
          renderPass = renderPass,
          framebuffer = framebuffers(currentBuffer),
          renderArea = new Vulkan.Rect2D(
            offset = new Vulkan.Offset2D(x = 0, y = 0),
            extent = new Vulkan.Extent2D(width = width, height = height)),
          clearValues = Array(
            new Vulkan.ClearValueColor(
              color = new Vulkan.ClearColorValueFloat(
                float32 = Array(0.2f, 0.2f, 0.2f, 0.2f))),
            new Vulkan.ClearValueDepthStencil(
              depthStencil =
                new Vulkan.ClearDepthStencilValue(depth = 1.0f, stencil = 0))
          )
        ),
        Vulkan.SUBPASS_CONTENTS_SECONDARY_COMMAND_BUFFERS
      )

      vk.cmdExecuteCommands(primaryCommandBuffer,
                            1,
                            Array(secondaryCommandBuffer))
      vk.cmdEndRenderPass(primaryCommandBuffer)
      vk.endCommandBuffer(primaryCommandBuffer)

      vk.queueSubmit(
        graphicsQueue,
        1,
        Array(
          new Vulkan.SubmitInfo(
            waitSemaphores = Array(acquireSemaphore),
            waitDstStageMask =
              Array(Vulkan.PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT),
            commandBuffers = Array(primaryCommandBuffer),
            signalSemaphores = Array(renderSemaphore)
          )),
        new Vulkan.Fence(0)
      )
      vk.queueSubmit(graphicsQueue, 0, Array.empty, fence)

      vk.queuePresentKHR(
        graphicsQueue,
        new Vulkan.PresentInfoKHR(swapchains = Array(swapchain),
                                  imageIndices = currentBuffer,
                                  waitSemaphores = Array(renderSemaphore)))

      var shouldWait = true
      println("about to wait")
      while (shouldWait) {
        val res =
          vk.waitForFences(device, 1, Array(fence), false, FENCE_TIMEOUT)
        if (res.value != Vulkan.TIMEOUT.value) {
          println("finished waiting")
          shouldWait = false
        }
      }
      vk.resetFences(device, 1, Array(fence))
    }

    Thread.sleep(1000)

    vk.destroySemaphore(device, renderSemaphore)
    vk.destroySemaphore(device, acquireSemaphore)
    vk.destroyFence(device, fence)
    vk.destroyPipeline(device, pipelines(1))
    vk.destroyPipeline(device, pipelines(0))
    framebuffers.foreach { f =>
      vk.destroyFramebuffer(device, f)
    }
    vk.destroyShaderModule(device, vertexModule)
    vk.destroyShaderModule(device, fragmentModule)
    vk.destroyRenderPass(device, renderPass)
    vk.destroyBuffer(device, vertexBuffer)
    vk.freeMemory(device, vertexBufferMemory)
    vk.freeDescriptorSets(device, descriptorPool, 1, descriptorSets)
    vk.destroyDescriptorPool(device, descriptorPool)
    vk.destroyPipelineLayout(device, pipelineLayout)
    vk.destroyDescriptorSetLayout(device, descriptorSetLayout)
    vk.destroySampler(device, textureSampler)
    vk.destroyImageView(device, textureImageView)
    vk.freeMemory(device, textureMemory)
    vk.destroyImage(device, textureImage)
    vk.destroyImageView(device, cubeTextureImageView)
    vk.freeMemory(device, cubeTextureMemory)
    vk.destroyImage(device, cubeTextureImage)
    vk.destroyBuffer(device, cubeUniformBuffer)
    vk.freeMemory(device, cubeUniformBufferMemory)
    vk.destroyBuffer(device, uniformBuffer)
    vk.freeMemory(device, uniformBufferMemory)
    vk.destroyImageView(device, depthImageView)
    vk.freeMemory(device, depthImageMemory)
    vk.destroyImage(device, depthImage)
    imageViews.foreach { i =>
      vk.destroyImageView(device, i)
    }
    vk.destroySwapchain(device, swapchain)
    vk.freeCommandBuffers(device, commandPool, 1, secondaryCommandBuffer)
    vk.freeCommandBuffers(device, commandPool, 1, primaryCommandBuffer)
    vk.destroyCommandPool(device, commandPool)
    vk.destroyDevice(device)
    vk.destroySurfaceKHR(instance, surface)
    glfw.destroyWindow(window)
    vk.destroyInstance(instance)
    glfw.terminate()
  }
}
/**

I would like:
 - resource management, so I don't have to clean everything up all the time
 - a clear idea of what the API is like in terms of structures derived from other structures
 - a cleraer idea of the memory management (we now have two objects in the same vertex buffer)
 - cleaner file loading.  We've got a codec, which is good
 - a better vector and camera system
 - I should be able to calculate the part of the skybox I need from the camera system
 -

Can create a pipeline without knowing anything about the data, except for format
So don't need to know data size.

    val uniformBuffer = createBuffer
    val uniformBufferMemoryRequirements = vk.getBufferMemoryRequirements(device, uniformBuffer)
  //find the index of the memory type which can be used to store the buffer, given the properties and buffer requirements
    val uniformBufferMemoryTypeIndex = memoryTypeIndex(memoryProperties, uniformBufferMemoryRequirements, props)
  //allocate memory for a memory type
    val uniformBufferMemory = vk.allocateMemory ...
  //map and copy an amount of memory
    val uniformDataPtr = vk.mapMemory(device, uniformBufferMemory, new Vulkan.DeviceSize(0), uniformBufferMemoryRequirements.size, 0)
    vk.loadMemory(uniformDataPtr, uniformData)
    vk.unmapMemory(device, uniformBufferMemory)
  //bind the memory to a buffer
    vk.bindBufferMemory(device, uniformBuffer, uniformBufferMemory, new Vulkan.DeviceSize(0))


  Ideally, we'd allocate a chunk of memory for a memory type
  We'd then find that the memory type was valid, and take a range for a buffer
  We'd load stuff in and bind that range to a buffer

When we use the buffer, we have to specify an offset.  This offset must be valid.  So the data bound to the buffer memory must be ok.

A single heap can support multiple types
So how do we pick a heap?  It seems to be chosen for us.  The type has the heap index in it!
e.g.
propertyFlags 0 heap 1
propertyFlags 0 heap 1
propertyFlags 0 heap 1
propertyFlags 0 heap 1
propertyFlags 0 heap 1
propertyFlags 0 heap 1
propertyFlags 0 heap 1
propertyFlags 1 heap 0 //device local
propertyFlags 1 heap 0 //device local
propertyFlags 6 heap 1 //host visible and coherent
propertyFlags 14 heap 1 // host visible and coherent and cached
heap size  2147483648 flags 1 //device local
heap size 25197714432 flags 0 //device local

 => This has a dedicated graphics card.  We need to copy to put into the device local memory

message: vkUpdateDescriptorsSets() failed write update validation for Descriptor Set 0x1a with error: Write update to descriptor in set 0x1a binding #0 failed with error message: Attempted write update to buffer descriptor failed due to: VkDescriptorBufferInfo range is 64 which is greater than buffer size (64) minus requested offset of 8. For more information refer to Vulkan Spec Section '13.2.4. Descriptor Set Updates' which states 'If range is not equal to VK_WHOLE_SIZE, range must be less than or equal to the size of buffer minus offset' (https://www.khronos.org/registry/vulkan/specs/1.0-extensions/xhtml/vkspec.html#VkDescriptorBufferInfo)


message: vkUpdateDescriptorSets(): pDescriptorWrites[2].pBufferInfo[0].offset (0x8) must be a multiple of device limit minUniformBufferOffsetAlignment 0x100

Vulkan
®
1.0.39 - A Specification
575 / 683
Valid Usage
•  If any member of this structure is
VK_FALSE
, as returned by
vkGetPhysicalDeviceFeatures
, then it
must be
VK_FALSE
when passed as part of the
VkDeviceCreateInfo
struct when creating a device
30.1.1    Feature Requirements
All Vulkan graphics implementations must support the following features:
•
robustBufferAccess
.
All other features are not required by the Specification.
30.2    Limits
There are a variety of implementation-dependent limits.
The
VkPhysicalDeviceLimits
are properties of the physical device. These are available in the
limits
member of
the
VkPhysicalDeviceProperties
structure which is returned from
vkGetPhysicalDeviceProperties

Design plan:
 - assume that there can be separate queues for loading and drawing in the same queue family.
   => we sync the queues using a semaphore when we want to use updated data
 - memory can be categorized as:
    no-updates, long term (should be device local)
    updates, long term (should be host visible, not device local)
    per-frame updates (using vkCmdUpdatebuffer, host visible)
 - give a size hint for a given memory type on application start
 - for a given buffer, get memoryTypeBits, find appropriate allocation, bind allocation
 - for a given set of vertex attributes, find a vertex buffer
 - for a given set of uniforms, find a uniform buffer
 - buffer quantities:
   - there should be a single index buffer
   - there should be a vertex buffer per attribute set (assuming interleaving here)
   - there should be a vertex buffer per instanced attribute set
   - there should be a single uniform buffer (no point in having multiple uniform buffers)

What about buffer views?
What about coherent / cached vs non-coherent?

What about images?
 - should always read in optimal. If non-optimal, need to stage first.
 -
lin 5121 op: 7555 buf: 88 // RGBA, blit_src_blit, ds att,
lin 0 op: 0 buf: 64 //RGB

    VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT = 0x00000001,
    VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT = 0x00000002,
    VK_FORMAT_FEATURE_STORAGE_IMAGE_ATOMIC_BIT = 0x00000004,
    VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT = 0x00000008,
    VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT = 0x00000010,
    VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_ATOMIC_BIT = 0x00000020,
    VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT = 0x00000040,
    VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT = 0x00000080,
    VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT = 0x00000100,
    VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT = 0x00000200,
    VK_FORMAT_FEATURE_BLIT_SRC_BIT = 0x00000400,
    VK_FORMAT_FEATURE_BLIT_DST_BIT = 0x00000800,
    VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT = 0x00001000,
    VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_CUBIC_BIT_IMG = 0x00002000,
  */
