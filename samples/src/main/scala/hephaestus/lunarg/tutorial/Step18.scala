package hephaestus
package lunarg
package tutorial

import hephaestus.platform._
import java.nio._

/** Draws a textured cube over several frames, updates the texture per frame */
object Step18 extends Utils {

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

    val commandPool = vk.createCommandPool(device, new Vulkan.CommandPoolCreateInfo(
      flags = Vulkan.COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT,
      queueFamilyIndex = qi
    ))
    val commandBuffer = vk.allocateCommandBuffers(device, new Vulkan.CommandBufferAllocateInfo(
      commandPool = commandPool,
      level = Vulkan.COMMAND_BUFFER_LEVEL_SECONDARY,
      commandBufferCount = 1
    ))
    val primaryCommandBuffer = initCommandBuffer(device, commandPool)

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

    val graphicsQueue = vk.getDeviceQueue(device, qi, 0)
    val textureCommandBuffer = initCommandBuffer(device, commandPool)
    vk.beginCommandBuffer(textureCommandBuffer, new Vulkan.CommandBufferBeginInfo(flags = Vulkan.COMMAND_BUFFER_USAGE_BLANK_FLAG,
      inheritanceInfo = Vulkan.COMMAND_BUFFER_INHERITANCE_INFO_NULL_HANDLE))

    //bind a texture too
    val textureFormatProperties = vk.getPhysicalDeviceFormatProperties(physicalDevice, Vulkan.FORMAT_R8G8B8A8_UNORM)
    if(Vulkan.FORMAT_FEATURE_SAMPLED_IMAGE_BIT & textureFormatProperties.linearTilingFeatures) {
      println("ok, no staging required")
    } else throw new Error("image needs staging!")
    val textureWidth = 128
    val textureHeight = 128
    val textureImageCreateInfo = new Vulkan.ImageCreateInfo(
      flags = 0,
      imageType = Vulkan.IMAGE_TYPE_2D, 
      format = Vulkan.FORMAT_R8G8B8A8_UNORM,
      extent = new Vulkan.Extent3D(textureWidth, textureHeight, 1), 
      mipLevels = 1, arrayLayers = 1, samples = 1, tiling = Vulkan.IMAGE_TILING_LINEAR, 
      usage = Vulkan.IMAGE_USAGE_SAMPLED_BIT, 
      sharingMode = Vulkan.SHARING_MODE_EXCLUSIVE,
      queueFamilyIndices = Array.empty, 
      initialLayout = Vulkan.IMAGE_LAYOUT_PREINITIALIZED)

    val textureImage = vk.createImage(device, textureImageCreateInfo)
    val textureImageMemoryRequirements = vk.getImageMemoryRequirements(device, textureImage)
    val textureMemoryAllocateInfo = new Vulkan.MemoryAllocateInfo(
      allocationSize = textureImageMemoryRequirements.size,
      memoryTypeIndex =
        memoryTypeIndex(memoryProperties, textureImageMemoryRequirements,
          Vulkan.MEMORY_PROPERTY_HOST_VISIBLE_BIT | Vulkan.MEMORY_PROPERTY_HOST_COHERENT_BIT)
    )
    val textureMemory = vk.allocateMemory(device, textureMemoryAllocateInfo)
    vk.bindImageMemory(device, textureImage, textureMemory, new Vulkan.DeviceSize(0))

    val imageSubresource = new Vulkan.ImageSubresource(
      aspectMask = Vulkan.IMAGE_ASPECT_COLOR_BIT,
      mipLevel = 0,
      arrayLayer = 0
    )

    setImageLayout(Vulkan.IMAGE_LAYOUT_PREINITIALIZED, Vulkan.IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
      textureImage, Vulkan.IMAGE_ASPECT_COLOR_BIT, Vulkan.PIPELINE_STAGE_HOST_BIT, 
      Vulkan.PIPELINE_STAGE_FRAGMENT_SHADER_BIT, textureCommandBuffer)
    vk.endCommandBuffer(textureCommandBuffer)
    val textureFence = vk.createFence(device, new Vulkan.FenceCreateInfo(0))
    vk.queueSubmit(graphicsQueue, 1, Array(new Vulkan.SubmitInfo(
      waitSemaphores = Array.empty,
      commandBuffers = Array(textureCommandBuffer),
      signalSemaphores = Array.empty,
      waitDstStageMask = Array.empty
    )), textureFence)

    var hasLoadedTexture = false
    while(!hasLoadedTexture) {
      println("waiting for texture fence")
      val res = vk.waitForFences(device, 1, Array(textureFence), true, FENCE_TIMEOUT)
      if(res != Vulkan.TIMEOUT) hasLoadedTexture = true
    }


    val textureLayout = vk.getImageSubresourceLayout(device, textureImage, imageSubresource)
    val textureDataPtr = vk.mapMemory(device, textureMemory, new Vulkan.DeviceSize(0), textureImageMemoryRequirements.size, 0)
    vk.loadMemory(textureDataPtr, Cube.textureData(textureWidth, textureHeight, 0))
    vk.unmapMemory(device, textureMemory)



    val textureImageView = vk.createImageView(device, new Vulkan.ImageViewCreateInfo(
      flags = 0,
      image = textureImage,
      viewType = Vulkan.IMAGE_VIEW_TYPE_2D,
      format = Vulkan.FORMAT_R8G8B8A8_UNORM,
      components = new Vulkan.ComponentMapping(
        Vulkan.COMPONENT_SWIZZLE_R,
        Vulkan.COMPONENT_SWIZZLE_G,
        Vulkan.COMPONENT_SWIZZLE_B,
        Vulkan.COMPONENT_SWIZZLE_A
      ),
      subresourceRange = new Vulkan.ImageSubresourceRange(
      aspectMask = Vulkan.IMAGE_ASPECT_COLOR_BIT,
      baseMipLevel = 0,
      levelCount = 1,
      baseArrayLayer = 0,
      layerCount = 1)))

    val textureSampler = vk.createSampler(device, new Vulkan.SamplerCreateInfo(
      flags = 0,
      magFilter = Vulkan.FILTER_NEAREST,
      minFilter = Vulkan.FILTER_NEAREST,
      mipmapMode = Vulkan.SAMPLER_MIPMAP_MODE_NEAREST,
      addressModeU = Vulkan.SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE,
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
      unnormalizedCoordinates = false))


    //have the uniform data change per frame
    val uniformData = Cube.uniformData(width, height, 0)
    val buffer = initBuffer(device, uniformData.capacity)

    //vk.loadMemory(dataPtr, data)
    val bufferMemoryRequirements = vk.getBufferMemoryRequirements(device, buffer)
    val bufferMemoryTypeIndex = memoryTypeIndex(memoryProperties, bufferMemoryRequirements,
      Vulkan.MEMORY_PROPERTY_HOST_VISIBLE_BIT | Vulkan.MEMORY_PROPERTY_HOST_COHERENT_BIT)
    val bufferMemoryAllocationInfo = new Vulkan.MemoryAllocateInfo(
      allocationSize = bufferMemoryRequirements.size,
      memoryTypeIndex = bufferMemoryTypeIndex)
    val bufferMemory = vk.allocateMemory(device, bufferMemoryAllocationInfo)
    val uniformDataPtr = vk.mapMemory(device, bufferMemory, new Vulkan.DeviceSize(0), bufferMemoryRequirements.size, 0) 
    //vk.unmapMemory(device, bufferMemory)
    vk.bindBufferMemory(device, buffer, bufferMemory, new Vulkan.DeviceSize(0))

    val descriptorSetLayout = vk.createDescriptorSetLayout(device, new Vulkan.DescriptorSetLayoutCreateInfo(
      flags = 0,
      bindings = Array(new Vulkan.DescriptorSetLayoutBinding(
        binding = 0,
        descriptorType = Vulkan.DESCRIPTOR_TYPE_UNIFORM_BUFFER,
        descriptorCount = 1,
        stageFlags = Vulkan.SHADER_STAGE_VERTEX_BIT,
        immutableSamplers = Array.empty[Vulkan.Sampler]),
        new Vulkan.DescriptorSetLayoutBinding(
          binding = 1,
          descriptorType = Vulkan.DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
          descriptorCount = 1,
          stageFlags = Vulkan.SHADER_STAGE_FRAGMENT_BIT,
          immutableSamplers = Array.empty
        ))))

    val pipelineLayout = initPipelineLayout(device, descriptorSetLayout)

    val descriptorPool = vk.createDescriptorPool(device, new Vulkan.DescriptorPoolCreateInfo(
      flags = 0,
      maxSets = 1,
      poolSizes = Array(
        new Vulkan.DescriptorPoolSize(tpe = Vulkan.DESCRIPTOR_TYPE_UNIFORM_BUFFER, descriptorCount = 1),
        new Vulkan.DescriptorPoolSize(tpe = Vulkan.DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, descriptorCount = 1)
      )))

    val descriptorSets = vk.allocateDescriptorSets(device, new Vulkan.DescriptorSetAllocateInfo(
      descriptorPool = descriptorPool,
      setLayouts = Array(descriptorSetLayout)))

    val writeDescriptorSets = Array(
      new Vulkan.WriteDescriptorSet(
        dstSet = descriptorSets(0),
        dstBinding = 0,
        dstArrayElement = 0,
        descriptorCount = 1,
        descriptorType = Vulkan.DESCRIPTOR_TYPE_UNIFORM_BUFFER,
        imageInfo = Array.empty[Vulkan.DescriptorImageInfo],
        bufferInfo = Array(new Vulkan.DescriptorBufferInfo(
          buffer = buffer,
          offset = new Vulkan.DeviceSize(0),
          range = new Vulkan.DeviceSize(uniformData.capacity))
        ),
        texelBufferView = Array.empty[Vulkan.BufferView]),
      new Vulkan.WriteDescriptorSet(
        dstSet = descriptorSets(0),
        dstBinding = 1,
        dstArrayElement = 0,
        descriptorCount = 1,
        descriptorType = Vulkan.DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
        imageInfo = Array(
          new Vulkan.DescriptorImageInfo(
            sampler = textureSampler,
            imageView = textureImageView,
            imageLayout = Vulkan.IMAGE_LAYOUT_GENERAL)),
        bufferInfo = Array.empty,
        texelBufferView = Array.empty))

    vk.updateDescriptorSets(device, 2,writeDescriptorSets, 0, Array.empty[Vulkan.CopyDescriptorSet])

    val renderPass = initRenderPass(device, swapchainFormat)
    val vertexModule = initShaderModule("texture.vert.spv", device)
    val fragmentModule = initShaderModule("texture.frag.spv", device)

    val commandBufferBeginInfo = new Vulkan.CommandBufferBeginInfo(
      Vulkan.COMMAND_BUFFER_USAGE_RENDER_PASS_CONTINUE_BIT | Vulkan.COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT,
      new Vulkan.CommandBufferInheritanceInfo(
        renderPass = renderPass
      ))
    vk.beginCommandBuffer(commandBuffer, commandBufferBeginInfo)

    val framebuffers = initFramebuffers(device, imageViews, depthImageView, renderPass, width, height)

    val vertexData: ByteBuffer = Cube.solidFaceUvsData
    val vertexBuffer = initVertexBuffer(device, vertexData.capacity)
    val vertexBufferMemory = initBufferMemory(device, memoryProperties, vertexBuffer, vertexData)

    //TODO: add textured pipeline
    val vertexBinding = new Vulkan.VertexInputBindingDescription(
      binding = 0,
      inputRate = Vulkan.VERTEX_INPUT_RATE_VERTEX,
      stride = 24
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
      format = Vulkan.FORMAT_R32G32_SFLOAT,
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
    val vertexInputStateCreateInfo = new Vulkan.PipelineVertexInputStateCreateInfo(
      flags = 0,
      vertexBindingDescriptions = Array(vertexBinding),
      vertexAttributeDescriptions = Array(vertexAttrib0, vertexAttrib1))

    val dynamicState = new Vulkan.PipelineDynamicStateCreateInfo(
      flags = 0,
      dynamicStates = Array(
        Vulkan.DYNAMIC_STATE_VIEWPORT,
        Vulkan.DYNAMIC_STATE_SCISSOR))

    val inputAssemblyStateCreateInfo = new Vulkan.PipelineInputAssemblyStateCreateInfo(
      flags = 0,
      topology = Vulkan.PRIMITIVE_TOPOLOGY_TRIANGLE_LIST,
      primitiveRestartEnable = false)
    val rasterizationStateCreateInfo = new Vulkan.PipelineRasterizationStateCreateInfo(
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
    val colorBlendAttachmentState = new Vulkan.PipelineColorBlendAttachmentState(
      colorWriteMask = 0xf,
      blendEnable = false,
      alphaBlendOp = Vulkan.BLEND_OP_ADD,
      colorBlendOp = Vulkan.BLEND_OP_ADD,
      srcColorBlendFactor = Vulkan.BLEND_FACTOR_ZERO,
      dstColorBlendFactor = Vulkan.BLEND_FACTOR_ZERO,
      srcAlphaBlendFactor = Vulkan.BLEND_FACTOR_ZERO,
      dstAlphaBlendFactor = Vulkan.BLEND_FACTOR_ZERO
    )
    val colorBlendStateCreateInfo = new Vulkan.PipelineColorBlendStateCreateInfo(
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
      writeMask = 0)
    val depthStencilStateCreateInfo = new Vulkan.PipelineDepthStencilStateCreateInfo(
      flags = 0,
      depthTestEnable = true,
      depthWriteEnable = true,
      depthCompareOp = Vulkan.COMPARE_OP_LESS_OR_EQUAL,
      depthBoundsTestEnable = false,
      minDepthBounds = 0,
      maxDepthBounds = 0,
      stencilTestEnable = false,
      back = depthStencilOpState,
      front = depthStencilOpState)
    val multisampleStateCreateInfo = new Vulkan.PipelineMultisampleStateCreateInfo(
      flags = 0,
      sampleMask = 0,
      rasterizationSamples = Vulkan.SAMPLE_COUNT_1_BIT,
      sampleShadingEnable = false,
      alphaToCoverageEnable = false,
      alphaToOneEnable = false,
      minSampleShading = 0f)
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
    val pipeline = pipelines(0)//initPipeline(device, renderPass, vertexModule, fragmentModule, pipelineLayout)
    val clearValues0 = new Vulkan.ClearValueColor(color = new Vulkan.ClearColorValueFloat(float32 = Array(0.2f, 0.2f, 0.2f, 0.2f)))
    val clearValues1 = new Vulkan.ClearValueDepthStencil(depthStencil = new Vulkan.ClearDepthStencilValue(depth = 1.0f, stencil = 0))

    //code start
    vk.cmdBindPipeline(commandBuffer, Vulkan.PIPELINE_BIND_POINT_GRAPHICS, pipeline)
    vk.cmdBindDescriptorSets(commandBuffer, Vulkan.PIPELINE_BIND_POINT_GRAPHICS, pipelineLayout,
      0, descriptorSets.size, descriptorSets, 0, Array.empty)
    vk.cmdBindVertexBuffers(commandBuffer, 0, 1, Array(vertexBuffer), Array(new Vulkan.DeviceSize(0)))
    val viewport = new Vulkan.Viewport(
      height = height,
      width = width,
      minDepth = 0f,
      maxDepth = 1f,
      x = 0,
      y = 0)
    vk.cmdSetViewport(commandBuffer, 0, 1, Array(viewport))
    val scissor = new Vulkan.Rect2D(
      extent = new Vulkan.Extent2D(width = width, height = height),
      offset = new Vulkan.Offset2D(x = 0, y = 0)
    )
    vk.cmdSetScissor(commandBuffer, 0, 1, Array(scissor))
    vk.cmdDraw(commandBuffer, 12 * 3, 1, 0, 0)
    vk.endCommandBuffer(commandBuffer)

    val acquireSemaphore = initSemaphore(device)
    val renderSemaphore = initSemaphore(device)
    val fence = initFence(device)

    (0 until 1000).foreach { i =>

      //since memory is coherent, we just need to do a memcopy
      val uniformDataPerFrame = Cube.uniformData(width, height, i)
      vk.loadMemory(uniformDataPtr, uniformDataPerFrame)
      val textureDataPerFrame = Cube.textureData(textureWidth, textureHeight, i)
      vk.loadMemory(textureDataPtr, textureDataPerFrame)

      val currentBuffer = vk.acquireNextImageKHR(device, swapchain, java.lang.Long.MAX_VALUE, acquireSemaphore, new Vulkan.Fence(0))
      vk.beginCommandBuffer(primaryCommandBuffer, new Vulkan.CommandBufferBeginInfo(flags = Vulkan.COMMAND_BUFFER_USAGE_BLANK_FLAG,
        inheritanceInfo = Vulkan.COMMAND_BUFFER_INHERITANCE_INFO_NULL_HANDLE))

      val clearValues = Array(clearValues0, clearValues1)
      val renderPassBeginInfo = new Vulkan.RenderPassBeginInfo(
        renderPass = renderPass,
        framebuffer = framebuffers(currentBuffer),
        renderArea = new Vulkan.Rect2D(
          offset = new Vulkan.Offset2D(x = 0, y = 0),
          extent = new Vulkan.Extent2D(width = width, height = height)),
        clearValues = clearValues
      )
      vk.cmdBeginRenderPass(primaryCommandBuffer, renderPassBeginInfo, Vulkan.SUBPASS_CONTENTS_SECONDARY_COMMAND_BUFFERS)
      vk.cmdExecuteCommands(primaryCommandBuffer, 1, Array(commandBuffer))
      //execute buffer here
      vk.cmdEndRenderPass(primaryCommandBuffer)
      vk.endCommandBuffer(primaryCommandBuffer)

      //signal the render semaphore
      val submitInfo = new Vulkan.SubmitInfo(
        waitSemaphores = Array(acquireSemaphore),
        waitDstStageMask = Array(Vulkan.PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT),
        commandBuffers = Array(primaryCommandBuffer),
        signalSemaphores = Array(renderSemaphore))
      vk.queueSubmit(graphicsQueue, 1, Array(submitInfo), new Vulkan.Fence(0))
      vk.queueSubmit(graphicsQueue, 0, Array.empty, fence)

      val presentInfo = new Vulkan.PresentInfoKHR(
        swapchains = Array(swapchain),
        imageIndices = currentBuffer,
        waitSemaphores = Array(renderSemaphore))

      vk.queuePresentKHR(graphicsQueue, presentInfo)

      var shouldWait = true
      println("about to wait")
      while(shouldWait) {
        val res = vk.waitForFences(device, 1, Array(fence), false, FENCE_TIMEOUT)
        if(res.value != Vulkan.TIMEOUT.value) {
          println("finished waiting")
          shouldWait = false
        }
      }
      vk.resetFences(device, 1, Array(fence))
    }
    Thread.sleep(1000)

    vk.destroyFence(device, fence)
    framebuffers.foreach { f => vk.destroyFramebuffer(device, f)}
    vk.destroyPipeline(device, pipeline)
    vk.destroyBuffer(device, vertexBuffer)
    vk.freeMemory(device, vertexBufferMemory)
    vk.destroyShaderModule(device, vertexModule)
    vk.destroyShaderModule(device, fragmentModule)
    vk.destroyRenderPass(device, renderPass)
    vk.destroySemaphore(device, acquireSemaphore)
    vk.destroySemaphore(device, renderSemaphore)
    vk.freeDescriptorSets(device, descriptorPool, 1, descriptorSets)
    vk.destroyDescriptorPool(device, descriptorPool)
    vk.destroyDescriptorSetLayout(device, descriptorSetLayout)
    vk.destroyPipelineLayout(device, pipelineLayout)
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


  def setImageLayout(oldLayout: Vulkan.ImageLayout, newLayout: Vulkan.ImageLayout, image: Vulkan.Image, 
  aspectMask: Vulkan.ImageAspectFlag, srcStages: Vulkan.PipelineStageFlag, dstStages: Vulkan.PipelineStageFlag, commandBuffer: Vulkan.CommandBuffer) {

    val srcAccessMask = oldLayout match {
      case Vulkan.IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL => Vulkan.ACCESS_COLOR_ATTACHMENT_WRITE_BIT
      case Vulkan.IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL => Vulkan.ACCESS_TRANSFER_WRITE_BIT
      case Vulkan.IMAGE_LAYOUT_PREINITIALIZED => Vulkan.ACCESS_HOST_WRITE_BIT
    }

    val dstAccessMask = newLayout match {
      case Vulkan.IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL => Vulkan.ACCESS_TRANSFER_WRITE_BIT
        case Vulkan.IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL => Vulkan.ACCESS_TRANSFER_READ_BIT
        case Vulkan.IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL => Vulkan.ACCESS_SHADER_READ_BIT
        case Vulkan.IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL => Vulkan.ACCESS_COLOR_ATTACHMENT_WRITE_BIT
        case Vulkan.IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL => Vulkan.ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT
    }

    val barrier = new Vulkan.ImageMemoryBarrier(
      srcAccessMask = srcAccessMask,
      dstAccessMask = dstAccessMask,
      oldLayout = oldLayout,
      newLayout = newLayout,
      //THESE values were different
      srcQueueFamilyIndex = Vulkan.QUEUE_FAMILY_IGNORED,
      dstQueueFamilyIndex = Vulkan.QUEUE_FAMILY_IGNORED,
      image = image,
      subresourceRange = new Vulkan.ImageSubresourceRange(aspectMask = aspectMask, 
        baseMipLevel = 0, 
        levelCount = 1, 
        baseArrayLayer = 0, 
        layerCount = 1))
    vk.cmdPipelineBarrier(commandBuffer, srcStages, dstStages, 0, Array.empty, Array.empty, Array(barrier))
  }
}
