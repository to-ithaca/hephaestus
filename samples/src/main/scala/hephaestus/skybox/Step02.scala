// package hephaestus
// package skybox

// import hephaestus.platform._
// import hephaestus.vulkan._
// import hephaestus.io.Buffer

// import java.nio.{ByteBuffer, ByteOrder}
// import java.io.File
// import java.nio.file.Files

// import javax.imageio._
// import java.awt.image._
// /**

// TODO:
// 1. Draw a plane at the back of the screen, behind everything else DONE
// 2. Using an angle theta, in a cylindrical coordinate system, work out the uv coords for each vertex
//  - the v coord is fixed, the x coord is dependent on theta DONE
// 3. Using an angle theta, and an angle phi, in a spherical coordinate system, work out the uv coords for each vertex
// 4. Draw a cube in front of this, rotating theta and phi

// 1. Read in a texture for the texture data
//   Bitmap bitmap = BitmapFactory.decodeFile(filePath);

// Ideas:
//  - could have a Reader for device / physical device / command buffer
//  - could have a ResourceTracker State for cleanups
//  - image loading (staging etc?)
//  -
//   * */
// object Step02 {

//   val FENCE_TIMEOUT  = 100000000
//   val width = 500
//   val height = 500
//   val textureWidth = 1024//900
//   val textureHeight = 512//1201
//   val cubeTextureWidth = 8
//   val cubeTextureHeight = 8
//   val scale = 0.25f

//   val glfw = new GLFW()
//   val vk = new Vulkan()

//   val name = "skybox01"
//   val skyboxFile = "skybox.png"

//   //need to compare image types to supported types
//   //need to stage images, and set sample swizzles from input type
//   //ultimately want to read ETC compressed images too
//   def skyboxTexture(): ByteBuffer = {
//     val file = new File(getClass.getResource(s"/$skyboxFile").toURI())
//     val img = ImageIO.read(file)
//     val width = img.getWidth
//     val height = img.getHeight
//     println(s"width $width height $height type ${img.getType}")
//     val data = img.getRaster.getDataBuffer.asInstanceOf[DataBufferByte].getData
//     println(s"data size ${data.size}, should be ${width * height * 4}")
//     val buffer = Buffer.direct(data :_*).value
//     buffer
//   }

//   def initGraphicsPresentQueueFamilyIndex(instance: Vulkan.Instance, physicalDevice: Vulkan.PhysicalDevice): Int = {
//     val qfps = vk.getPhysicalDeviceQueueFamilyProperties(physicalDevice)
//     qfps.zipWithIndex.find {
//       case (q, i) =>
//         val ss = glfw.getPhysicalDevicePresentationSupport(instance, physicalDevice, i)
//         val gb = (q.queueFlags & Vulkan.QUEUE_GRAPHICS_BIT) > 0
//         ss && gb
//     }.map(_._2).get
//   }

//   def memoryTypeIndex(ps: Vulkan.PhysicalDeviceMemoryProperties, reqs: Vulkan.MemoryRequirements, mask: Int): Int = {
//     ps.memoryTypes.zipWithIndex.foldLeft((Option.empty[Int], reqs.memoryTypeBits)) { (t0, t1) =>
//       (t0, t1) match {
//         case ((None, bits), (tpe, i)) =>
//           if((bits & 1) == 1 && (tpe.propertyFlags & mask) == mask)
//             (Some(i), bits)
//           else (None, bits >> 1)
//         case (idx, _) => idx
//       }
//     }._1.get
//   }

//   def spvFile(name: String): ByteBuffer = {
//     val file = new File(getClass.getResource(s"/$name").toURI())
//     val bytes = Files.readAllBytes(file.toPath())
//     val buf = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder())
//     buf.put(bytes, 0, bytes.size)
//     buf
//   }

//   def initShaderModule(name: String, device: Vulkan.Device): Vulkan.ShaderModule = {
//     val spv = spvFile(name)
//     val info = new Vulkan.ShaderModuleCreateInfo(
//       flags = 0,
//       codeSize = spv.capacity,
//       code = spv
//     )
//     vk.createShaderModule(device, info)
//   }
//   def main(args: Array[String]): Unit = {
//     //skyboxTexture().array
//     glfw.init()
//     val instance = vk.createInstance(new Vulkan.InstanceCreateInfo(
//       applicationInfo = new Vulkan.ApplicationInfo(
//       applicationName = name,
//       applicationVersion = 1,
//       engineName = name,
//       engineVersion = 1,
//       apiVersion = Vulkan.API_VERSION_1_0),
//       enabledExtensionNames = (Vulkan.EXT_DEBUG_REPORT_EXTENSION_NAME :: glfw.getRequiredInstanceExtensions().toList).toArray,
//       enabledLayerNames = Array(Vulkan.LAYER_LUNARG_STANDARD_VALIDATION_NAME, Vulkan.LAYER_LUNARG_API_DUMP_NAME)))
//     vk.debugReport(instance)

//     glfw.windowHint(GLFW.CLIENT_API, GLFW.NO_API)
//     val window = glfw.createWindow(width, height, name)
//     val surface = glfw.createWindowSurface(instance, window)

//     val physicalDevice = PhysicalDevice(vk.enumeratePhysicalDevices(instance)(0), vk)

//     val (formats, surfaceCapabilities, depthFormatProperties, textureFormatProperties, memoryProperties) = (for {
//       fs <- PhysicalDevice.surfaceFormats(surface)
//       sc <- PhysicalDevice.surfaceCapabilities(surface)
//       df <- PhysicalDevice.formatProperties(Vulkan.FORMAT_D16_UNORM)
//       tf <- PhysicalDevice.formatProperties(Vulkan.FORMAT_R8G8B8A8_UNORM)
//       mp <- PhysicalDevice.memoryProperties
//     } yield (fs, sc, df, tf, mp)).run(physicalDevice)

//     val qi = initGraphicsPresentQueueFamilyIndex(instance, physicalDevice.device)
//     val device = PhysicalDevice.createDevice(new Vulkan.DeviceCreateInfo(
//       queueCreateInfos = Array(new Vulkan.DeviceQueueCreateInfo(
//         flags = 0,
//         queueFamilyIndex = qi,
//         queuePriorities = Array(0f))),
//       enabledExtensionNames = Array(Vulkan.SWAPCHAIN_EXTENSION_NAME))).run(physicalDevice)

//     val commandPool = Device.createCommandPool(new Vulkan.CommandPoolCreateInfo(
//       flags = Vulkan.COMMAND_POOL_BLANK_FLAG,
//       queueFamilyIndex = qi)).run(device)
//     val (primaryCommandBuffer, secondaryCommandBuffer) = (for {
//       ps <- CommandPool.allocateCommandBuffers(Vulkan.COMMAND_BUFFER_LEVEL_PRIMARY, 1)
//       ss <- CommandPool.allocateCommandBuffers(Vulkan.COMMAND_BUFFER_LEVEL_SECONDARY, 1)
//     } yield (ps.head, ss.head)).run(commandPool)
//     val swapchainFormat =
//       if (formats(0).format == Vulkan.FORMAT_UNDEFINED) Vulkan.FORMAT_B8G8R8A8_UNORM
//       else formats(0).format
//     val swapchainExtent = if(surfaceCapabilities.currentExtent.width == 0xFFFFFFFF) {
//       val ewidth = if(width < surfaceCapabilities.minImageExtent.width) surfaceCapabilities.minImageExtent.width
//       else if (width > surfaceCapabilities.maxImageExtent.width) surfaceCapabilities.maxImageExtent.width
//       else width
//       val eheight = if(height < surfaceCapabilities.minImageExtent.height) surfaceCapabilities.minImageExtent.height
//       else if (height > surfaceCapabilities.maxImageExtent.height) surfaceCapabilities.maxImageExtent.height
//       else height
//       new Vulkan.Extent2D(ewidth, eheight)
//     } else surfaceCapabilities.currentExtent

//     val preTransform =
//       if ((surfaceCapabilities.supportedTransforms & Vulkan.SURFACE_TRANSFORM_IDENTITY_BIT) > 0) Vulkan.SURFACE_TRANSFORM_IDENTITY_BIT
//       else surfaceCapabilities.currentTransform
//     val swapchain = Device.createSwapchain(new Vulkan.SwapchainCreateInfo(
//       flags = 0,
//       surface = surface,
//       minImageCount = surfaceCapabilities.minImageCount,
//       imageFormat = swapchainFormat,
//       imageExtent = swapchainExtent,
//       preTransform = preTransform,
//       compositeAlpha = Vulkan.COMPOSITE_ALPHA_OPAQUE_BIT,
//       imageArrayLayers = 1,
//       presentMode = Vulkan.PRESENT_MODE_FIFO,
//       clipped = true,
//       imageColorSpace = Vulkan.COLORSPACE_SRGB_NONLINEAR,
//       imageUsage = Vulkan.IMAGE_USAGE_COLOR_ATTACHMENT_BIT,
//       imageSharingMode = Vulkan.SHARING_MODE_EXCLUSIVE,
//       queueFamilyIndices = Array.empty[Int])).run(device)

//     val swapchainImages = vk.getSwapchainImages(device.device, swapchain)
//     val imageViews = swapchainImages.map { i =>
//       Device.createImageView(new Vulkan.ImageViewCreateInfo(
//         flags = 0,
//         image = i,
//         viewType = Vulkan.IMAGE_VIEW_TYPE_2D,
//         format = swapchainFormat,
//         components = new Vulkan.ComponentMapping(
//           r = Vulkan.COMPONENT_SWIZZLE_R,
//           g = Vulkan.COMPONENT_SWIZZLE_G,
//           b = Vulkan.COMPONENT_SWIZZLE_B,
//           a = Vulkan.COMPONENT_SWIZZLE_A
//         ),
//         subresourceRange = new Vulkan.ImageSubresourceRange(
//           aspectMask = Vulkan.IMAGE_ASPECT_COLOR_BIT,
//           baseMipLevel = 0,
//           levelCount = 1,
//           baseArrayLayer = 0,
//           layerCount = 1))).run(device)
//     }
//     val imageTiling =
//       if(Vulkan.FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT & depthFormatProperties.linearTilingFeatures) Vulkan.IMAGE_TILING_LINEAR
//       else if(Vulkan.FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT & depthFormatProperties.optimalTilingFeatures) Vulkan.IMAGE_TILING_OPTIONAL
//       else throw new Error("depth not supported")
//     val depthImage = Device.createImage(new Vulkan.ImageCreateInfo(
//       flags = 0,
//       imageType = Vulkan.IMAGE_TYPE_2D,
//       format = Vulkan.FORMAT_D16_UNORM,
//       extent = new Vulkan.Extent3D(
//         width = swapchainExtent.width,
//         height = swapchainExtent.height,
//         depth = 1),
//       mipLevels = 1,
//       arrayLayers = 1,
//       samples = Vulkan.SAMPLE_COUNT_1_BIT,
//       tiling = imageTiling,
//       initialLayout = Vulkan.IMAGE_LAYOUT_UNDEFINED,
//       usage = Vulkan.IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT,
//       queueFamilyIndices = Array.empty,
//       sharingMode = Vulkan.SHARING_MODE_EXCLUSIVE)).run(device)

//     val depthImageMemoryRequirements = vk.getImageMemoryRequirements(device, depthImage)
//     val depthImageMemoryTypeIndex = memoryProperties.memoryTypes.zipWithIndex.foldLeft((Option.empty[Int], depthImageMemoryRequirements.memoryTypeBits)) { (t0, t1) =>
//       (t0, t1) match {
//         case ((None, bits), (tpe, i)) => if((bits & 1) == 1) (Some(i), bits) else (None, bits >> 1)
//         case (prev, _) => prev
//       }
//     }._1.get

//     val depthImageMemory = Device.allocateMemory(new Vulkan.MemoryAllocateInfo(
//       allocationSize = depthImageMemoryRequirements.size,
//       memoryTypeIndex = depthImageMemoryTypeIndex)).run(device)
//     vk.bindImageMemory(device, depthImage, depthImageMemory, new Vulkan.DeviceSize(0))
//     val depthImageView = Device.createImageView(new Vulkan.ImageViewCreateInfo(
//       flags = 0,
//       image = depthImage,
//       viewType = Vulkan.IMAGE_VIEW_TYPE_2D,
//       format = Vulkan.FORMAT_D16_UNORM,
//       components = new Vulkan.ComponentMapping(
//         r = Vulkan.COMPONENT_SWIZZLE_R,
//         g = Vulkan.COMPONENT_SWIZZLE_G,
//         b = Vulkan.COMPONENT_SWIZZLE_B,
//         a = Vulkan.COMPONENT_SWIZZLE_A
//       ),
//       subresourceRange = new Vulkan.ImageSubresourceRange(
//         aspectMask = Vulkan.IMAGE_ASPECT_DEPTH_BIT,
//         baseMipLevel = 0,
//         levelCount = 1,
//         baseArrayLayer = 0,
//         layerCount = 1))).run(device)

//     val uniformData = Buffer.direct(scale, 0f).value
//     val cubeUniformData = hephaestus.lunarg.tutorial.Cube.uniformData(width, height, 0)

//     //this whole section is largely repeated
//     //usage, size, memory, returns buffer, memory, mapped ptr
//     val uniformBuffer = Device.createBuffer(new Vulkan.BufferCreateInfo(
//       usage = Vulkan.BUFFER_USAGE_UNIFORM_BUFFER_BIT,
//       size = new Vulkan.DeviceSize(uniformData.capacity),
//       queueFamilyIndices = Array.empty[Int],
//       sharingMode = Vulkan.SHARING_MODE_EXCLUSIVE,
//       flags = 0)).run(device)
//     val uniformBufferMemoryRequirements = vk.getBufferMemoryRequirements(device, uniformBuffer)
//     val uniformBufferMemoryTypeIndex = memoryTypeIndex(memoryProperties, uniformBufferMemoryRequirements,
//       Vulkan.MEMORY_PROPERTY_HOST_VISIBLE_BIT | Vulkan.MEMORY_PROPERTY_HOST_COHERENT_BIT)
//     val uniformBufferMemory = vk.allocateMemory(device, new Vulkan.MemoryAllocateInfo(
//       allocationSize = uniformBufferMemoryRequirements.size,
//       memoryTypeIndex = uniformBufferMemoryTypeIndex))
//     val uniformDataPtr = vk.mapMemory(device, uniformBufferMemory, new Vulkan.DeviceSize(0), uniformBufferMemoryRequirements.size, 0)
//     vk.loadMemory(uniformDataPtr, uniformData)
//     vk.unmapMemory(device, uniformBufferMemory)
//     vk.bindBufferMemory(device, uniformBuffer, uniformBufferMemory, new Vulkan.DeviceSize(0))
//     val cubeUniformBuffer = vk.createBuffer(device, new Vulkan.BufferCreateInfo(
//       usage = Vulkan.BUFFER_USAGE_UNIFORM_BUFFER_BIT,
//       size = new Vulkan.DeviceSize(cubeUniformData.capacity),
//       queueFamilyIndices = Array.empty[Int],
//       sharingMode = Vulkan.SHARING_MODE_EXCLUSIVE,
//       flags = 0))
//     val cubeUniformBufferMemoryRequirements = vk.getBufferMemoryRequirements(device, cubeUniformBuffer)
//     val cubeUniformBufferMemoryTypeIndex = memoryTypeIndex(memoryProperties, cubeUniformBufferMemoryRequirements,
//       Vulkan.MEMORY_PROPERTY_HOST_VISIBLE_BIT | Vulkan.MEMORY_PROPERTY_HOST_COHERENT_BIT)
//     val cubeUniformBufferMemory = vk.allocateMemory(device, new Vulkan.MemoryAllocateInfo(
//       allocationSize = cubeUniformBufferMemoryRequirements.size,
//       memoryTypeIndex = cubeUniformBufferMemoryTypeIndex))
//     val cubeUniformDataPtr = vk.mapMemory(device, cubeUniformBufferMemory, new Vulkan.DeviceSize(0), cubeUniformBufferMemoryRequirements.size, 0)
//     vk.loadMemory(cubeUniformDataPtr, cubeUniformData)
//     vk.unmapMemory(device, cubeUniformBufferMemory)
//     vk.bindBufferMemory(device, cubeUniformBuffer, cubeUniformBufferMemory, new Vulkan.DeviceSize(0))
//     if(!(Vulkan.FORMAT_FEATURE_SAMPLED_IMAGE_BIT & textureFormatProperties.linearTilingFeatures)) throw new Error("image needs staging!")
//     //practically the same as buffer creation here
//     val textureImage = vk.createImage(device, new Vulkan.ImageCreateInfo(
//       flags = 0,
//       imageType = Vulkan.IMAGE_TYPE_2D,
//       format = Vulkan.FORMAT_R8G8B8A8_UNORM,
//       extent = new Vulkan.Extent3D(textureWidth, textureHeight, 1),
//       mipLevels = 1, arrayLayers = 1, samples = 1, tiling = Vulkan.IMAGE_TILING_LINEAR,
//       usage = Vulkan.IMAGE_USAGE_SAMPLED_BIT,
//       sharingMode = Vulkan.SHARING_MODE_EXCLUSIVE,
//       queueFamilyIndices = Array.empty,
//       initialLayout = Vulkan.IMAGE_LAYOUT_PREINITIALIZED))
//     val textureImageMemoryRequirements = vk.getImageMemoryRequirements(device, textureImage)
//     val textureMemory = vk.allocateMemory(device, new Vulkan.MemoryAllocateInfo(
//       allocationSize = textureImageMemoryRequirements.size,
//       memoryTypeIndex = memoryTypeIndex(memoryProperties, textureImageMemoryRequirements,
//           Vulkan.MEMORY_PROPERTY_HOST_VISIBLE_BIT | Vulkan.MEMORY_PROPERTY_HOST_COHERENT_BIT)))
//     vk.bindImageMemory(device, textureImage, textureMemory, new Vulkan.DeviceSize(0))

//     val textureData = skyboxTexture()  //lunarg.tutorial.Cube.textureData(textureWidth, textureHeight, 0)
//     //val textureData = lunarg.tutorial.Cube.textureData(textureWidth, textureHeight, 0)
//     val textureDataPtr = vk.mapMemory(device, textureMemory, new Vulkan.DeviceSize(0), textureImageMemoryRequirements.size, 0)
//     vk.loadMemory(textureDataPtr, textureData)
//     vk.unmapMemory(device, textureMemory)

//     val textureImageView = vk.createImageView(device, new Vulkan.ImageViewCreateInfo(
//       flags = 0,
//       image = textureImage,
//       viewType = Vulkan.IMAGE_VIEW_TYPE_2D,
//       format = Vulkan.FORMAT_R8G8B8A8_UNORM,
//       components = new Vulkan.ComponentMapping(
//         Vulkan.COMPONENT_SWIZZLE_A,
//         Vulkan.COMPONENT_SWIZZLE_B,
//         Vulkan.COMPONENT_SWIZZLE_G,
//         Vulkan.COMPONENT_SWIZZLE_R
//       ),
//       subresourceRange = new Vulkan.ImageSubresourceRange(
//       aspectMask = Vulkan.IMAGE_ASPECT_COLOR_BIT,
//       baseMipLevel = 0,
//       levelCount = 1,
//       baseArrayLayer = 0,
//       layerCount = 1)))
//     val cubeTextureImage = vk.createImage(device, new Vulkan.ImageCreateInfo(
//       flags = 0,
//       imageType = Vulkan.IMAGE_TYPE_2D,
//       format = Vulkan.FORMAT_R8G8B8A8_UNORM,
//       extent = new Vulkan.Extent3D(cubeTextureWidth, cubeTextureHeight, 1),
//       mipLevels = 1, arrayLayers = 1, samples = 1, tiling = Vulkan.IMAGE_TILING_LINEAR,
//       usage = Vulkan.IMAGE_USAGE_SAMPLED_BIT,
//       sharingMode = Vulkan.SHARING_MODE_EXCLUSIVE,
//       queueFamilyIndices = Array.empty,
//       initialLayout = Vulkan.IMAGE_LAYOUT_PREINITIALIZED))
//     val cubeTextureImageMemoryRequirements = vk.getImageMemoryRequirements(device, cubeTextureImage)
//     val cubeTextureMemory = vk.allocateMemory(device, new Vulkan.MemoryAllocateInfo(
//       allocationSize = cubeTextureImageMemoryRequirements.size,
//       memoryTypeIndex = memoryTypeIndex(memoryProperties, cubeTextureImageMemoryRequirements,
//           Vulkan.MEMORY_PROPERTY_HOST_VISIBLE_BIT | Vulkan.MEMORY_PROPERTY_HOST_COHERENT_BIT)))
//     vk.bindImageMemory(device, cubeTextureImage, cubeTextureMemory, new Vulkan.DeviceSize(0))

//     val cubeTextureData = lunarg.tutorial.Cube.textureData(cubeTextureWidth, cubeTextureHeight, 0)
//     val cubeTextureDataPtr = vk.mapMemory(device, cubeTextureMemory, new Vulkan.DeviceSize(0), cubeTextureImageMemoryRequirements.size, 0)
//     vk.loadMemory(cubeTextureDataPtr, cubeTextureData)
//     vk.unmapMemory(device, cubeTextureMemory)

//     val cubeTextureImageView = vk.createImageView(device, new Vulkan.ImageViewCreateInfo(
//       flags = 0,
//       image = cubeTextureImage,
//       viewType = Vulkan.IMAGE_VIEW_TYPE_2D,
//       format = Vulkan.FORMAT_R8G8B8A8_UNORM,
//       components = new Vulkan.ComponentMapping(
//         Vulkan.COMPONENT_SWIZZLE_R,
//         Vulkan.COMPONENT_SWIZZLE_G,
//         Vulkan.COMPONENT_SWIZZLE_B,
//         Vulkan.COMPONENT_SWIZZLE_A
//       ),
//       subresourceRange = new Vulkan.ImageSubresourceRange(
//       aspectMask = Vulkan.IMAGE_ASPECT_COLOR_BIT,
//       baseMipLevel = 0,
//       levelCount = 1,
//       baseArrayLayer = 0,
//       layerCount = 1)))
//     val textureSampler = vk.createSampler(device, new Vulkan.SamplerCreateInfo(
//       flags = 0,
//       magFilter = Vulkan.FILTER_NEAREST,
//       minFilter = Vulkan.FILTER_NEAREST,
//       mipmapMode = Vulkan.SAMPLER_MIPMAP_MODE_NEAREST,
//       addressModeU = Vulkan.SAMPLER_ADDRESS_MODE_REPEAT,
//       addressModeV = Vulkan.SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE,
//       addressModeW = Vulkan.SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE,
//       mipLodBias = 0f,
//       anisotropyEnable = false,
//       maxAnisotropy = 0f,
//       compareOp = Vulkan.COMPARE_OP_NEVER,
//       minLod = 0f,
//       maxLod = 0f,
//       compareEnable = false,
//       borderColor = Vulkan.BORDER_COLOR_FLOAT_OPAQUE_WHITE,
//       unnormalizedCoordinates = false))

//     val descriptorSetLayout = vk.createDescriptorSetLayout(device, new Vulkan.DescriptorSetLayoutCreateInfo(
//       flags = 0,
//       bindings = Array(
//         new Vulkan.DescriptorSetLayoutBinding(
//           binding = 0,
//           descriptorType = Vulkan.DESCRIPTOR_TYPE_UNIFORM_BUFFER,
//           descriptorCount = 1,
//           stageFlags = Vulkan.SHADER_STAGE_VERTEX_BIT,
//           immutableSamplers = Array.empty[Vulkan.Sampler]),
//         new Vulkan.DescriptorSetLayoutBinding(
//           binding = 1,
//           descriptorType = Vulkan.DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
//           descriptorCount = 1,
//           stageFlags = Vulkan.SHADER_STAGE_FRAGMENT_BIT,
//           immutableSamplers = Array.empty))))

//     val pipelineLayout = vk.createPipelineLayout(device, new Vulkan.PipelineLayoutCreateInfo(
//       flags = 0,
//       setLayouts = Array(descriptorSetLayout),
//       pushConstantRanges = Array.empty))

//     val descriptorPool = vk.createDescriptorPool(device, new Vulkan.DescriptorPoolCreateInfo(
//       flags = 0,
//       maxSets = 2,
//       poolSizes = Array(
//         new Vulkan.DescriptorPoolSize(tpe = Vulkan.DESCRIPTOR_TYPE_UNIFORM_BUFFER, descriptorCount = 2),
//         new Vulkan.DescriptorPoolSize(tpe = Vulkan.DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, descriptorCount = 2)
//       )))

//     val descriptorSets = vk.allocateDescriptorSets(device, new Vulkan.DescriptorSetAllocateInfo(
//       descriptorPool = descriptorPool,
//       setLayouts = Array(descriptorSetLayout, descriptorSetLayout)))

//     //this could be dynamic
//     val writeDescriptorSets = Array(
//       new Vulkan.WriteDescriptorSet(
//         dstSet = descriptorSets(0),
//         dstBinding = 0,
//         dstArrayElement = 0,
//         descriptorCount = 1,
//         descriptorType = Vulkan.DESCRIPTOR_TYPE_UNIFORM_BUFFER,
//         imageInfo = Array.empty[Vulkan.DescriptorImageInfo],
//         bufferInfo = Array(new Vulkan.DescriptorBufferInfo(
//           buffer = uniformBuffer,
//           offset = new Vulkan.DeviceSize(0),
//           range = new Vulkan.DeviceSize(uniformData.capacity))
//         ),
//         texelBufferView = Array.empty[Vulkan.BufferView]),
//       new Vulkan.WriteDescriptorSet(
//         dstSet = descriptorSets(0),
//         dstBinding = 1,
//         dstArrayElement = 0,
//         descriptorCount = 1,
//         descriptorType = Vulkan.DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
//         imageInfo = Array(
//           new Vulkan.DescriptorImageInfo(
//             sampler = textureSampler,
//             imageView = textureImageView,
//             imageLayout = Vulkan.IMAGE_LAYOUT_GENERAL)),
//         bufferInfo = Array.empty,
//         texelBufferView = Array.empty),
//       new Vulkan.WriteDescriptorSet(
//         dstSet = descriptorSets(1),
//         dstBinding = 0,
//         dstArrayElement = 0,
//         descriptorCount = 1,
//         descriptorType = Vulkan.DESCRIPTOR_TYPE_UNIFORM_BUFFER,
//         imageInfo = Array.empty[Vulkan.DescriptorImageInfo],
//         bufferInfo = Array(new Vulkan.DescriptorBufferInfo(
//           buffer = cubeUniformBuffer,
//           offset = new Vulkan.DeviceSize(0),
//           range = new Vulkan.DeviceSize(cubeUniformData.capacity))
//         ),
//         texelBufferView = Array.empty[Vulkan.BufferView]),
//       new Vulkan.WriteDescriptorSet(
//         dstSet = descriptorSets(1),
//         dstBinding = 1,
//         dstArrayElement = 0,
//         descriptorCount = 1,
//         descriptorType = Vulkan.DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
//         imageInfo = Array(
//           new Vulkan.DescriptorImageInfo(
//             sampler = textureSampler,
//             imageView = cubeTextureImageView,
//             imageLayout = Vulkan.IMAGE_LAYOUT_GENERAL)),
//         bufferInfo = Array.empty,
//         texelBufferView = Array.empty))

//     vk.updateDescriptorSets(device, 4,writeDescriptorSets, 0, Array.empty[Vulkan.CopyDescriptorSet])

//     val vertexData = Buffer.direct(
//       -1f, -1f, -1f, 1f, 1f, -1f,
//        1f, -1f, -1f, 1f, 1f,  1f).value

//     val cubeVertexData = hephaestus.lunarg.tutorial.Cube.solidFaceUvsData

//     //exactly the same as previous buffers
//     val vertexBuffer = vk.createBuffer(device, new Vulkan.BufferCreateInfo(
//       usage = Vulkan.BUFFER_USAGE_VERTEX_BUFFER_BIT,
//       size = new Vulkan.DeviceSize(vertexData.capacity + cubeVertexData.capacity),
//       queueFamilyIndices = Array.empty[Int],
//       sharingMode = Vulkan.SHARING_MODE_EXCLUSIVE,
//       flags = 0))

//     val vertexBufferMemoryRequirements = vk.getBufferMemoryRequirements(device, vertexBuffer)
//     val vertexBufferMemoryTypeIndex = memoryTypeIndex(memoryProperties, vertexBufferMemoryRequirements,
//       Vulkan.MEMORY_PROPERTY_HOST_VISIBLE_BIT | Vulkan.MEMORY_PROPERTY_HOST_COHERENT_BIT)
//     val vertexBufferMemory = vk.allocateMemory(device, new Vulkan.MemoryAllocateInfo(
//       allocationSize = vertexBufferMemoryRequirements.size,
//       memoryTypeIndex = vertexBufferMemoryTypeIndex))
//     val vertexDataPtr = vk.mapMemory(device, vertexBufferMemory, new Vulkan.DeviceSize(0), vertexBufferMemoryRequirements.size, 0)
//     vk.loadMemory(vertexDataPtr, vertexData)
//     vk.loadMemory(vertexDataPtr + vertexData.capacity, cubeVertexData)
//     vk.unmapMemory(device, vertexBufferMemory)
//     vk.bindBufferMemory(device, vertexBuffer, vertexBufferMemory, new Vulkan.DeviceSize(0))
//     val renderPass = vk.createRenderPass(device, new Vulkan.RenderPassCreateInfo(
//       flags = 0,
//       attachments = Array(new Vulkan.AttachmentDescription(
//         format = swapchainFormat,
//         samples = Vulkan.SAMPLE_COUNT_1_BIT,
//         loadOp = Vulkan.ATTACHMENT_LOAD_OP_CLEAR,
//         storeOp = Vulkan.ATTACHMENT_STORE_OP_STORE,
//         stencilLoadOp = Vulkan.ATTACHMENT_LOAD_OP_DONT_CARE,
//         stencilStoreOp = Vulkan.ATTACHMENT_STORE_OP_DONT_CARE,
//         initialLayout = Vulkan.IMAGE_LAYOUT_UNDEFINED,
//         finalLayout = Vulkan.IMAGE_LAYOUT_PRESENT_SRC_KHR,
//         flags = 0),
//         new Vulkan.AttachmentDescription(
//           format = Vulkan.FORMAT_D16_UNORM,
//           samples = Vulkan.SAMPLE_COUNT_1_BIT,
//           loadOp = Vulkan.ATTACHMENT_LOAD_OP_CLEAR,
//           storeOp = Vulkan.ATTACHMENT_STORE_OP_DONT_CARE,
//           stencilLoadOp = Vulkan.ATTACHMENT_LOAD_OP_DONT_CARE,
//           stencilStoreOp = Vulkan.ATTACHMENT_STORE_OP_DONT_CARE,
//           initialLayout = Vulkan.IMAGE_LAYOUT_UNDEFINED,
//           finalLayout = Vulkan.IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL,
//           flags = 0)),
//       subpasses = Array(new Vulkan.SubpassDescription(
//         pipelineBindPoint = Vulkan.PIPELINE_BIND_POINT_GRAPHICS,
//         flags = 0,
//         inputAttachments = Array.empty[Vulkan.AttachmentReference],
//         colorAttachments = Array(new Vulkan.AttachmentReference(attachment = 0, layout = Vulkan.IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)),
//         resolveAttachments = Array.empty[Vulkan.AttachmentReference],
//         depthStencilAttachment = Array(new Vulkan.AttachmentReference(attachment = 1, layout = Vulkan.IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL)),
//         preserveAttachments = Array.empty[Int])),
//       dependencies = Array.empty))

//     val vertexModule = initShaderModule("skybox.vert.spv", device)
//     val fragmentModule = initShaderModule("skybox.frag.spv", device)
//     val cubeVertexModule = initShaderModule("texture.vert.spv", device)
//     val cubeFragmentModule = initShaderModule("texture.frag.spv", device)

//     val framebuffers = imageViews.map { v =>
//       vk.createFramebuffer(device, new Vulkan.FramebufferCreateInfo(
//         flags = 0,
//         renderPass = renderPass,
//         attachments = Array(v, depthImageView),
//         width = width,
//         height = height,
//         layers = 1))
//     }

//     vk.beginCommandBuffer(secondaryCommandBuffer, new Vulkan.CommandBufferBeginInfo(
//       Vulkan.COMMAND_BUFFER_USAGE_RENDER_PASS_CONTINUE_BIT | Vulkan.COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT,
//       new Vulkan.CommandBufferInheritanceInfo(renderPass = renderPass)))

//     //can bind multiple vertex buffers to a pipeline (i.e. give them different strides and attributes)
//     //but what is the point, since they need to go in the same shader pipeline?
//     //should be for if the attributes are stored in different buffers
//     val vertexInputStateCreateInfo = new Vulkan.PipelineVertexInputStateCreateInfo(
//       flags = 0,
//       vertexBindingDescriptions = Array(new Vulkan.VertexInputBindingDescription(
//         binding = 0,
//         inputRate = Vulkan.VERTEX_INPUT_RATE_VERTEX,
//         stride = 8)),
//       vertexAttributeDescriptions = Array(
//         new Vulkan.VertexInputAttributeDescription(
//           binding = 0,
//           location = 0,
//           format = Vulkan.FORMAT_R32G32_SFLOAT,
//           offset = 0
//         )))

//     val dynamicState = new Vulkan.PipelineDynamicStateCreateInfo(
//       flags = 0,
//       dynamicStates = Array(
//         Vulkan.DYNAMIC_STATE_VIEWPORT,
//         Vulkan.DYNAMIC_STATE_SCISSOR))

//     val inputAssemblyStateCreateInfo = new Vulkan.PipelineInputAssemblyStateCreateInfo(
//       flags = 0,
//       topology = Vulkan.PRIMITIVE_TOPOLOGY_TRIANGLE_LIST,
//       primitiveRestartEnable = false)
//     val rasterizationStateCreateInfo = new Vulkan.PipelineRasterizationStateCreateInfo(
//       flags = 0,
//       polygonMode = Vulkan.POLYGON_MODE_FILL,
//       cullMode = Vulkan.CULL_MODE_BACK_BIT,
//       frontFace = Vulkan.FRONT_FACE_COUNTER_CLOCKWISE,
//       depthClampEnable = true,
//       rasterizerDiscardEnable = false,
//       depthBiasEnable = false,
//       depthBiasConstantFactor = 0,
//       depthBiasClamp = 0,
//       depthBiasSlopeFactor = 0,
//       lineWidth = 1f
//     )
//     val colorBlendAttachmentState = new Vulkan.PipelineColorBlendAttachmentState(
//       colorWriteMask = 0xf,
//       blendEnable = false,
//       alphaBlendOp = Vulkan.BLEND_OP_ADD,
//       colorBlendOp = Vulkan.BLEND_OP_ADD,
//       srcColorBlendFactor = Vulkan.BLEND_FACTOR_ZERO,
//       dstColorBlendFactor = Vulkan.BLEND_FACTOR_ZERO,
//       srcAlphaBlendFactor = Vulkan.BLEND_FACTOR_ZERO,
//       dstAlphaBlendFactor = Vulkan.BLEND_FACTOR_ZERO
//     )
//     val colorBlendStateCreateInfo = new Vulkan.PipelineColorBlendStateCreateInfo(
//       flags = 0,
//       attachments = Array(colorBlendAttachmentState),
//       logicOpEnable = false,
//       logicOp = Vulkan.LOGIC_OP_NO_OP,
//       blendConstants = Array(1f, 1f, 1f, 1f)
//     )
//     val viewportStateCreateInfo = new Vulkan.PipelineViewportStateCreateInfo(
//       flags = 0,
//       viewportCount = 1,
//       viewports = Array.empty,
//       scissorCount = 1,
//       scissors = Array.empty)
//     val depthStencilOpState = new Vulkan.StencilOpState(
//       failOp = Vulkan.STENCIL_OP_KEEP,
//       passOp = Vulkan.STENCIL_OP_KEEP,
//       compareOp = Vulkan.COMPARE_OP_ALWAYS,
//       compareMask = 0,
//       reference = 0,
//       depthFailOp = Vulkan.STENCIL_OP_KEEP,
//       writeMask = 0)
//     val depthStencilStateCreateInfo = new Vulkan.PipelineDepthStencilStateCreateInfo(
//       flags = 0,
//       depthTestEnable = true,
//       depthWriteEnable = true,
//       depthCompareOp = Vulkan.COMPARE_OP_LESS_OR_EQUAL,
//       depthBoundsTestEnable = false,
//       minDepthBounds = 0,
//       maxDepthBounds = 0,
//       stencilTestEnable = false,
//       back = depthStencilOpState,
//       front = depthStencilOpState)
//     val multisampleStateCreateInfo = new Vulkan.PipelineMultisampleStateCreateInfo(
//       flags = 0,
//       sampleMask = 0,
//       rasterizationSamples = Vulkan.SAMPLE_COUNT_1_BIT,
//       sampleShadingEnable = false,
//       alphaToCoverageEnable = false,
//       alphaToOneEnable = false,
//       minSampleShading = 0f)
//     val pipelineInfo = new Vulkan.GraphicsPipelineCreateInfo(
//       layout = pipelineLayout,
//       basePipelineHandle = new Vulkan.Pipeline(0),
//       basePipelineIndex = 0,
//       flags = 0,
//       vertexInputState = vertexInputStateCreateInfo,
//       inputAssemblyState = inputAssemblyStateCreateInfo,
//       rasterizationState = rasterizationStateCreateInfo,
//       colorBlendState = colorBlendStateCreateInfo,
//       multisampleState = multisampleStateCreateInfo,
//       dynamicState = dynamicState,
//       viewportState = viewportStateCreateInfo,
//       depthStencilState = depthStencilStateCreateInfo,
//       stages = Array(
//         new Vulkan.PipelineShaderStageCreateInfo(
//           flags = 0,
//           stage = Vulkan.SHADER_STAGE_VERTEX_BIT,
//           module = vertexModule,
//           name = "main"
//         ), new Vulkan.PipelineShaderStageCreateInfo(
//           flags = 0,
//           stage = Vulkan.SHADER_STAGE_FRAGMENT_BIT,
//           module = fragmentModule,
//           name = "main")),
//       renderPass = renderPass,
//       subpass = 0
//     )
//     val cubeVertexInputStateCreateInfo = new Vulkan.PipelineVertexInputStateCreateInfo(
//       flags = 0,
//       vertexBindingDescriptions = Array(new Vulkan.VertexInputBindingDescription(
//         binding = 0,
//         inputRate = Vulkan.VERTEX_INPUT_RATE_VERTEX,
//         stride = 24)),
//       vertexAttributeDescriptions = Array(
//         new Vulkan.VertexInputAttributeDescription(
//           binding = 0,
//           location = 0,
//           format = Vulkan.FORMAT_R32G32B32A32_SFLOAT,
//           offset = 0
//         ), new Vulkan.VertexInputAttributeDescription(
//           binding = 0,
//           location = 1,
//           format = Vulkan.FORMAT_R32G32_SFLOAT,
//           offset = 16
//         )))
//     val cubeRasterizationStateCreateInfo = new Vulkan.PipelineRasterizationStateCreateInfo(
//       flags = 0,
//       polygonMode = Vulkan.POLYGON_MODE_FILL,
//       cullMode = Vulkan.CULL_MODE_BACK_BIT,
//       frontFace = Vulkan.FRONT_FACE_CLOCKWISE,
//       depthClampEnable = true,
//       rasterizerDiscardEnable = false,
//       depthBiasEnable = false,
//       depthBiasConstantFactor = 0,
//       depthBiasClamp = 0,
//       depthBiasSlopeFactor = 0,
//       lineWidth = 1f
//     )
//     val cubePipelineInfo = new Vulkan.GraphicsPipelineCreateInfo(
//       layout = pipelineLayout,
//       basePipelineHandle = new Vulkan.Pipeline(0),
//       basePipelineIndex = 0,
//       flags = 0,
//       vertexInputState = cubeVertexInputStateCreateInfo,
//       inputAssemblyState = inputAssemblyStateCreateInfo,
//       rasterizationState = cubeRasterizationStateCreateInfo,
//       colorBlendState = colorBlendStateCreateInfo,
//       multisampleState = multisampleStateCreateInfo,
//       dynamicState = dynamicState,
//       viewportState = viewportStateCreateInfo,
//       depthStencilState = depthStencilStateCreateInfo,
//       stages = Array(
//         new Vulkan.PipelineShaderStageCreateInfo(
//           flags = 0,
//           stage = Vulkan.SHADER_STAGE_VERTEX_BIT,
//           module = cubeVertexModule,
//           name = "main"
//         ), new Vulkan.PipelineShaderStageCreateInfo(
//           flags = 0,
//           stage = Vulkan.SHADER_STAGE_FRAGMENT_BIT,
//           module = cubeFragmentModule,
//           name = "main")),
//       renderPass = renderPass,
//       subpass = 0
//     )
//     val pipelines = vk.createGraphicsPipelines(device, 2, Array(pipelineInfo, cubePipelineInfo))

//     vk.cmdBindPipeline(secondaryCommandBuffer, Vulkan.PIPELINE_BIND_POINT_GRAPHICS, pipelines(0))
//     //set number should be specified in the shader
//     vk.cmdBindDescriptorSets(secondaryCommandBuffer, Vulkan.PIPELINE_BIND_POINT_GRAPHICS, pipelineLayout,
//       0, 1, Array(descriptorSets(0)), 0, Array.empty)
//     vk.cmdBindVertexBuffers(secondaryCommandBuffer, 0, 1, Array(vertexBuffer), Array(new Vulkan.DeviceSize(0)))
//     vk.cmdSetViewport(secondaryCommandBuffer, 0, 1, Array(new Vulkan.Viewport(
//       height = height,
//       width = width,
//       minDepth = 0f,
//       maxDepth = 1f,
//       x = 0,
//       y = 0)))
//     vk.cmdSetScissor(secondaryCommandBuffer, 0, 1, Array(new Vulkan.Rect2D(
//       extent = new Vulkan.Extent2D(width = width, height = height),
//       offset = new Vulkan.Offset2D(x = 0, y = 0)
//     )))
//     vk.cmdDraw(secondaryCommandBuffer, 6, 1, 0, 0)

//     vk.cmdBindVertexBuffers(secondaryCommandBuffer, 0, 1, Array(vertexBuffer), Array(new Vulkan.DeviceSize(vertexData.capacity)))
//     vk.cmdBindPipeline(secondaryCommandBuffer, Vulkan.PIPELINE_BIND_POINT_GRAPHICS, pipelines(1))
//     vk.cmdBindDescriptorSets(secondaryCommandBuffer, Vulkan.PIPELINE_BIND_POINT_GRAPHICS, pipelineLayout,
//       0, 1, Array(descriptorSets(1)), 0, Array.empty)
//     vk.cmdDraw(secondaryCommandBuffer, 12 * 3, 1, 0, 0)

//     vk.endCommandBuffer(secondaryCommandBuffer)
//     val acquireSemaphore = vk.createSemaphore(device, new Vulkan.SemaphoreCreateInfo(flags = 0))
//     val renderSemaphore = vk.createSemaphore(device, new Vulkan.SemaphoreCreateInfo(flags = 0))
//     val fence = vk.createFence(device, new Vulkan.FenceCreateInfo(flags = 0))
//     val graphicsQueue = vk.getDeviceQueue(device, qi, 0)

//     (0 until 5000).foreach { i =>

//       val theta = (i % 5000).toDouble / 5000.0
//       val uniformDataPerFrame = Buffer.direct(scale, theta.toFloat).value
//       //since memory is coherent, we just need to do a memcopy
//       vk.loadMemory(uniformDataPtr, uniformDataPerFrame)
//       val cubeUniformDataPerFrame = hephaestus.lunarg.tutorial.Cube.uniformData(width, height, i)
//       vk.loadMemory(cubeUniformDataPtr, cubeUniformDataPerFrame)
//       val textureDataPerFrame = hephaestus.lunarg.tutorial.Cube.textureData(cubeTextureWidth, cubeTextureHeight, i)
//        vk.loadMemory(cubeTextureDataPtr, textureDataPerFrame)

//       val currentBuffer = vk.acquireNextImageKHR(device, swapchain, java.lang.Long.MAX_VALUE, acquireSemaphore, new Vulkan.Fence(0))
//       vk.beginCommandBuffer(primaryCommandBuffer, new Vulkan.CommandBufferBeginInfo(flags = Vulkan.COMMAND_BUFFER_USAGE_BLANK_FLAG,
//         inheritanceInfo = Vulkan.COMMAND_BUFFER_INHERITANCE_INFO_NULL_HANDLE))

//       vk.cmdBeginRenderPass(primaryCommandBuffer, new Vulkan.RenderPassBeginInfo(
//         renderPass = renderPass,
//         framebuffer = framebuffers(currentBuffer),
//         renderArea = new Vulkan.Rect2D(
//           offset = new Vulkan.Offset2D(x = 0, y = 0),
//           extent = new Vulkan.Extent2D(width = width, height = height)),
//         clearValues = Array(
//           new Vulkan.ClearValueColor(color = new Vulkan.ClearColorValueFloat(float32 = Array(0.2f, 0.2f, 0.2f, 0.2f))),
//           new Vulkan.ClearValueDepthStencil(depthStencil = new Vulkan.ClearDepthStencilValue(depth = 1.0f, stencil = 0)))),
//         Vulkan.SUBPASS_CONTENTS_SECONDARY_COMMAND_BUFFERS)

//       vk.cmdExecuteCommands(primaryCommandBuffer, 1, Array(secondaryCommandBuffer))
//       vk.cmdEndRenderPass(primaryCommandBuffer)
//       vk.endCommandBuffer(primaryCommandBuffer)

//       vk.queueSubmit(graphicsQueue, 1, Array(new Vulkan.SubmitInfo(
//         waitSemaphores = Array(acquireSemaphore),
//         waitDstStageMask = Array(Vulkan.PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT),
//         commandBuffers = Array(primaryCommandBuffer),
//         signalSemaphores = Array(renderSemaphore))),
//         new Vulkan.Fence(0))
//       vk.queueSubmit(graphicsQueue, 0, Array.empty, fence)

//       vk.queuePresentKHR(graphicsQueue, new Vulkan.PresentInfoKHR(
//         swapchains = Array(swapchain),
//         imageIndices = currentBuffer,
//         waitSemaphores = Array(renderSemaphore)))

//       var shouldWait = true
//       println("about to wait")
//       while(shouldWait) {
//         val res = vk.waitForFences(device, 1, Array(fence), false, FENCE_TIMEOUT)
//         if(res.value != Vulkan.TIMEOUT.value) {
//           println("finished waiting")
//           shouldWait = false
//         }
//       }
//       vk.resetFences(device, 1, Array(fence))
//     }

//     Thread.sleep(1000)

//     vk.destroySemaphore(device, renderSemaphore)
//     vk.destroySemaphore(device, acquireSemaphore)
//     vk.destroyFence(device, fence)
//     vk.destroyPipeline(device, pipelines(1))
//     vk.destroyPipeline(device, pipelines(0))
//     framebuffers.foreach { f => vk.destroyFramebuffer(device, f)}
//     vk.destroyShaderModule(device, vertexModule)
//     vk.destroyShaderModule(device, fragmentModule)
//     vk.destroyRenderPass(device, renderPass)
//     vk.destroyBuffer(device, vertexBuffer)
//     vk.freeMemory(device, vertexBufferMemory)
//     vk.freeDescriptorSets(device, descriptorPool, 1, descriptorSets)
//     vk.destroyDescriptorPool(device, descriptorPool)
//     vk.destroyPipelineLayout(device, pipelineLayout)
//     vk.destroyDescriptorSetLayout(device, descriptorSetLayout)
//     vk.destroySampler(device, textureSampler)
//     vk.destroyImageView(device, textureImageView)
//     vk.freeMemory(device, textureMemory)
//     vk.destroyImage(device, textureImage)
//     vk.destroyImageView(device, cubeTextureImageView)
//     vk.freeMemory(device, cubeTextureMemory)
//     vk.destroyImage(device, cubeTextureImage)
//     vk.destroyBuffer(device, cubeUniformBuffer)
//     vk.freeMemory(device, cubeUniformBufferMemory)
//     vk.destroyBuffer(device, uniformBuffer)
//     vk.freeMemory(device, uniformBufferMemory)
//     vk.destroyImageView(device, depthImageView)
//     vk.freeMemory(device, depthImageMemory)
//     vk.destroyImage(device, depthImage)
//     imageViews.foreach { i => vk.destroyImageView(device, i)}
//     vk.destroySwapchain(device, swapchain)
//     vk.freeCommandBuffers(device, commandPool, 1, secondaryCommandBuffer)
//     vk.freeCommandBuffers(device, commandPool, 1, primaryCommandBuffer)
//     vk.destroyCommandPool(device, commandPool)
//     vk.destroyDevice(device)
//     vk.destroySurfaceKHR(instance, surface)
//     glfw.destroyWindow(window)
//     vk.destroyInstance(instance)
//     glfw.terminate()
//   }
// }
