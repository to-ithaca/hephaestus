
package hephaestus
package skybox

import hephaestus.platform._
import hephaestus.io.Buffer

import com.hackoeur.jglm._
import com.hackoeur.jglm.support._

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

/** reads camera modelview and projection matrix from a file */
object Step06 {

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
  val cameraFile = "skybox.cam"
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

  val cameraData: List[Float] = {
    val decoder = decode.once(listOfN(provide(32), floatL))
    val fileStream: InputStream =
      getClass.getResourceAsStream(s"/$cameraFile")
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


  def toMat(a: List[Float]): Mat4 =
    new Mat4(
      a(0), a(1), a(2), a(3),
      a(4), a(5), a(6), a(7), 
      a(8), a(9), a(10), a(11), 
      a(12), a(13), a(14), a(15))


  val cam = cameraData
  def cameraUniformData(width: Int, height: Int, frame: Int): ByteBuffer = {
    // val angle = (frame % 1000).toDouble / 1000.0
    // val radius = math.sqrt(1.34*1.34 + 4.75*4.75)
    // val eyeX = radius * math.cos(angle * math.Pi)
    // val eyeY = radius * math.sin(angle * math.Pi)

    // val aspect = if (width > height) height.toFloat / width.toFloat else 1f
    // val focalLength = 35.0 //mm
    // val sensor = 30.0 //mm
    // val fov = math.atan(sensor / (2.0 * focalLength)) / math.Pi * 360.0
    // //val fov = aspect * 49.0f / 4f
    // val projection = Matrices.perspective(fov.toFloat, width.toFloat / height.toFloat, 0.1f, 100.0f)
    // val view = Matrices.lookAt(new Vec3(eyeX.toFloat, eyeY.toFloat, 1.73f),
    //                            new Vec3(0f, 0f, 0f),
    //                            new Vec3(0f, 0f, 1f))
    // val model = Mat4.MAT4_IDENTITY
    val modelView = toMat(cam.take(16)).transpose
    val projection = toMat(cam.drop(16)).transpose
    println(modelView)
    println(projection)
    val clip = new Mat4(
      1.0f, 0.0f, 0.0f, 0.0f,
      0.0f, -1.0f, 0.0f, 0.0f, 
      0.0f, 0.0f, 0.5f, 0.5f, 
      0.0f, 0.0f, 0.0f, 1.0f).transpose
    val mvp = clip.multiply(projection).multiply(modelView)



    val fbuf = mvp.getBuffer()
    val fs = new Array[Float](fbuf.capacity())
    fbuf.get(fs)
    Buffer.direct(fs: _*).value
  }

  def main(args: Array[String]): Unit = {
    val vertexData =
      Buffer.direct(-1f, -1f, -1f, 1f, 1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f).value
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

    val hPhysicalDevice = hephaestus.vulkan.PhysicalDevice(instance, vk)
    val physicalDevice = hPhysicalDevice.device

    val qi = initGraphicsPresentQueueFamilyIndex(instance, physicalDevice)
    val hDevice = hephaestus.vulkan.PhysicalDevice.createDevice(
      new Vulkan.DeviceCreateInfo(
        queueCreateInfos = Array(
          new Vulkan.DeviceQueueCreateInfo(flags = 0,
                                           queueFamilyIndex = qi,
                                           queuePriorities = Array(0f, 0f))),
        enabledExtensionNames = Array(Vulkan.SWAPCHAIN_EXTENSION_NAME)
      )).run(hPhysicalDevice)
    val device = hDevice.device

    //we can now allocate memory
    val allocator = hephaestus.vulkan.Suballocator(hDevice)
    //we need to create the loading queue here
    val loadQueue = vk.getDeviceQueue(device, qi, 1)
    val loadSemaphore = hephaestus.vulkan.Device.createSemaphore(new Vulkan.SemaphoreCreateInfo(flags = 0)).run(hDevice)

    val commandPool = vk.createCommandPool(
      device,
      new Vulkan.CommandPoolCreateInfo(flags = Vulkan.COMMAND_POOL_BLANK_FLAG,
                                       queueFamilyIndex = qi))
    val loadCommandBuffer = hephaestus.vulkan.CommandBuffer(vk.allocateCommandBuffers(
      device,
      new Vulkan.CommandBufferAllocateInfo(
        commandPool = commandPool,
        level = Vulkan.COMMAND_BUFFER_LEVEL_PRIMARY,
        commandBufferCount = 1)), vk)
    //now we have enough to create a memory manager
    val loadFence = vk.createFence(device, new Vulkan.FenceCreateInfo(flags = 0))
    val manager = hephaestus.vulkan.ResourceManager.from(hDevice, allocator, loadQueue, loadSemaphore, loadCommandBuffer, loadFence)
    println(s"manager is $manager")
    //we want to allocate memory on the memory manager


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


    val uniformData = Buffer.direct(scale, 0f).value
    val cubeUniformData = cameraUniformData(width, height, 0)


    val textureData = loadTexture(skyboxFile)
    val hTextureImageInfo = hephaestus.vulkan.ImageInfo(
      format = Vulkan.FORMAT_R8G8B8A8_UNORM,
      usage = Vulkan.IMAGE_USAGE_SAMPLED_BIT | Vulkan.IMAGE_USAGE_TRANSFER_DST_BIT,
      layout = Vulkan.IMAGE_LAYOUT_PREINITIALIZED,
      width = textureWidth,
      height = textureHeight)

    val cubeTextureData = loadTexture(terrainTextureFile) // lunarg.tutorial.Cube.textureData(cubeTextureWidth, cubeTextureHeight, 0)
    val hCubeTextureImageInfo = hephaestus.vulkan.ImageInfo(
      format = Vulkan.FORMAT_R8G8B8A8_UNORM,
      usage = Vulkan.IMAGE_USAGE_SAMPLED_BIT,
      layout = Vulkan.IMAGE_LAYOUT_PREINITIALIZED,
      width = cubeTextureWidth,
      height = cubeTextureHeight)
    val hDepthImageInfo = hephaestus.vulkan.ImageInfo(
      format = Vulkan.FORMAT_D16_UNORM,
      usage = Vulkan.IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT,
      layout = Vulkan.IMAGE_LAYOUT_UNDEFINED,
      width = swapchainExtent.width,
      height = swapchainExtent.height
    )
    //fudge this for now, since we know that the alignment is 256 bytes for this device
    val (manager0, (skySlice, groundSlice, groundVertexSlice, skyVertexSlice, groundElementSlice, skyTextureSlice, groundTextureSlice, depthSlice)) = ((for {
      skySlice <- hephaestus.vulkan.ResourceManager.loadHost(uniformData, Some(256))
      groundSlice <- hephaestus.vulkan.ResourceManager.loadHost(cubeUniformData, Some(256))
      groundVertexSlice <- hephaestus.vulkan.ResourceManager.loadLocal(terrainVertexBytes, None)
      skyVertexSlice <- hephaestus.vulkan.ResourceManager.loadLocal(vertexData, None)
      groundElementSlice <- hephaestus.vulkan.ResourceManager.loadLocal(terrainPolygonBytes, None)
      skyTextureSlice <- hephaestus.vulkan.ResourceManager.loadImage(textureData, hTextureImageInfo)
      groundTextureSlice <- hephaestus.vulkan.ResourceManager.loadImage(cubeTextureData, hCubeTextureImageInfo)
      depthSlice <- hephaestus.vulkan.ResourceManager.loadEmptyImage(hDepthImageInfo)
    } yield (skySlice, groundSlice, groundVertexSlice, skyVertexSlice, groundElementSlice, skyTextureSlice, groundTextureSlice, depthSlice)).run(manager) match {
      case Left(err) => sys.error(err.toString)
      case Right(a) => a
    })

    val depthImageView = vk.createImageView(
      device,
      new Vulkan.ImageViewCreateInfo(
        flags = 0,
        image = depthSlice.image.image,
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
                                           baseArrayLayer = depthSlice.layer,
                                           layerCount = 1)
      )
    )


    val textureImageView = vk.createImageView(
      device,
      new Vulkan.ImageViewCreateInfo(
        flags = 0,
        image = skyTextureSlice.image.image,
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
                                           baseArrayLayer = skyTextureSlice.layer,
                                           layerCount = 1)
      )
    )


    val cubeTextureImageView = vk.createImageView(
      device,
      new Vulkan.ImageViewCreateInfo(
        flags = 0,
        image = groundTextureSlice.image.image,
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
                                           baseArrayLayer = groundTextureSlice.layer,
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
          new Vulkan.DescriptorBufferInfo(buffer = skySlice.buffer.buffer,
                                          offset = skySlice.slot.lower,
                                          range = skySlice.slot.upper - skySlice.slot.lower)),
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
          new Vulkan.DescriptorBufferInfo(buffer = groundSlice.buffer.buffer,
                                          offset = groundSlice.slot.lower,
                                          range = groundSlice.slot.upper - groundSlice.slot.lower)),
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
        cullMode = Vulkan.CULL_MODE_NONE,
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
        cullMode = Vulkan.CULL_MODE_NONE,
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

    //TODO: bind device local buffer here
    vk.cmdBindVertexBuffers(secondaryCommandBuffer,
                            0,
                            1,
                            Array(skyVertexSlice.buffer.buffer),
                            Array(skyVertexSlice.slot.lower))
    vk.cmdBindIndexBuffer(secondaryCommandBuffer,
                          groundElementSlice.buffer.buffer,
                          groundElementSlice.slot.lower,
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

    println(s"binding ground vertex buffer")
    vk.cmdBindVertexBuffers(secondaryCommandBuffer,
                            0,
                            1,
                            Array(groundVertexSlice.buffer.buffer),
                            Array(groundVertexSlice.slot.lower))
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

    val finalM = (0 until 5000).foldLeft(manager0) { (m, i) =>
      val theta = (i % 5000).toDouble / 5000.0
      val uniformDataPerFrame = Buffer.direct(scale, theta.toFloat).value
      val cubeUniformDataPerFrame = cameraUniformData(width, height, i)
      val (nextM, loadSemaphoreOpt) = (
        hephaestus.vulkan.ResourceManager.reloadHost(uniformDataPerFrame, skySlice) >>
        hephaestus.vulkan.ResourceManager.reloadHost(cubeUniformDataPerFrame, groundSlice) >>
        hephaestus.vulkan.ResourceManager.submit).run(m).value

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
            waitSemaphores = Array(acquireSemaphore) ++ loadSemaphoreOpt.map(a => Array(a)).getOrElse(Array.empty),
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
      nextM
    }

    Thread.sleep(1000)

    hephaestus.vulkan.ResourceManager.free.run(finalM).value
    hephaestus.vulkan.Suballocator.free.run(allocator).value
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
    vk.freeDescriptorSets(device, descriptorPool, 1, descriptorSets)
    vk.destroyDescriptorPool(device, descriptorPool)
    vk.destroyPipelineLayout(device, pipelineLayout)
    vk.destroyDescriptorSetLayout(device, descriptorSetLayout)
    vk.destroySampler(device, textureSampler)
    vk.destroyImageView(device, textureImageView)
    vk.destroyImageView(device, cubeTextureImageView)
    vk.destroyImageView(device, depthImageView)
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
