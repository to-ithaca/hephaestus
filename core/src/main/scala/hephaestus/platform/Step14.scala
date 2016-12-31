package hephaestus
package platform

import java.nio._

object Step14 extends Utils {

  def main(args: Array[String]): Unit = {
    glfw.init()

    val instance = initInstanceExtensionsDebug()

    glfw.windowHint(GLFW.CLIENT_API, GLFW.NO_API)
    val width = 200
    val height = 200
    val window = glfw.createWindow(width, height, "foobar")
    val surface = glfw.createWindowSurface(instance, window)

    val physicalDevice = vk.enumeratePhysicalDevices(instance)(0)
    val qi = initGraphicsPresentQueueFamilyIndex(instance, physicalDevice)
    val device = initDeviceExtensions(physicalDevice, qi)

    val commandPool = initCommandPool(device, qi)
    val commandBuffer = initCommandBuffer(device, commandPool)

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

    val uniformData = ByteBuffer.allocateDirect(4 * 4)
    .putFloat(1f).putFloat(2f).putFloat(3f).putFloat(4f)
    val buffer = initBuffer(device, uniformData.capacity)
    val bufferMemory = initBufferMemory(device, memoryProperties, buffer, uniformData)

    val descriptorSetLayout = initDescriptorSetLayout(device)
    val pipelineLayout = initPipelineLayout(device, descriptorSetLayout)

    val descriptorPool = initDescriptorPool(device)
    val descriptorSets = initDescriptorSets(device, descriptorPool, descriptorSetLayout, buffer, uniformData.capacity)

    val semaphore = initSemaphore(device)
    val currentBuffer = vk.acquireNextImageKHR(device, swapchain, java.lang.Long.MAX_VALUE, semaphore, new Vulkan.Fence(0)) 
    val renderPass = initRenderPass(device, swapchainFormat)

    val vertexModule = initShaderModule("vert.spv", device)
    val fragmentModule = initShaderModule("frag.spv", device)

    val commandBufferBeginInfo = new Vulkan.CommandBufferBeginInfo(flags = 0)
    vk.beginCommandBuffer(commandBuffer, commandBufferBeginInfo)

    val graphicsQueue = vk.getDeviceQueue(device, qi, 0)

    val framebuffers = initFramebuffers(device, imageViews, depthImageView, renderPass, width, height)

    val vertexData: ByteBuffer = Cube.solidFaceColorsData
    val vertexBuffer = initVertexBuffer(device, vertexData.capacity)
    val vertexBufferMemory = initBufferMemory(device, memoryProperties, vertexBuffer, vertexData)

    //code start
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
      pName = "main"
    )
    val fragmentShaderStage = new Vulkan.PipelineShaderStageCreateInfo(
      flags = 0,
      stage = Vulkan.SHADER_STAGE_FRAGMENT_BIT,
      module = fragmentModule,
      pName = "main"
    )

    val vertexInputStateCreateInfo = new Vulkan.PipelineVertexInputStateCreateInfo(
      flags = 0,
      vertexBindingDescriptionCount = 1,
      pVertexBindingDescriptions = Array(vertexBinding),
      vertexAttributeDescriptionCount = 2,
      pVertexAttributeDescriptions = Array(vertexAttrib0, vertexAttrib1))

    val dynamicState = new Vulkan.PipelineDynamicStateCreateInfo(
      flags = 0,
      dynamicStateCount = 2,
      pDynamicStates = Array(
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
      frontFace = Vulkan.FRONT_FACE_COUNTER_CLOCKWISE,
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
      attachmentCount = 1,
      pAttachments = Array(colorBlendAttachmentState),
      logicOpEnable = false,
      logicOp = Vulkan.LOGIC_OP_NO_OP,
      blendConstants = Array(1f, 1f, 1f, 1f)
    )

    val viewportStateCreateInfo = new Vulkan.PipelineViewportStateCreateInfo(
      flags = 0,
      viewportCount = 1,
      pViewports = Array.empty,
      scissorCount = 1,
      pScissors = Array.empty)


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
      pSampleMask = 0,
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
      pVertexInputState = vertexInputStateCreateInfo,
      pInputAssemblyState = inputAssemblyStateCreateInfo,
      pRasterizationState = rasterizationStateCreateInfo,
      pColorBlendState = colorBlendStateCreateInfo,
      pMultisampleState = multisampleStateCreateInfo,
      pDynamicState = dynamicState,
      pViewportState = viewportStateCreateInfo,
      pDepthStencilState = depthStencilStateCreateInfo,
      pStages = Array(vertexShaderStage, fragmentShaderStage),
      stageCount = 2,
      renderPass = renderPass,
      subpass = 0
    )

    println("createing pipelines")
    val pipelines = vk.createGraphicsPipelines(device, 1, Array(pipelineInfo))

    println("created pipeline")
    val pipeline = pipelines(0)
    println("got pipeline")
    /* VULKAN_KEY_END */

    val clearValues0 = new Vulkan.ClearValueColor(color = new Vulkan.ClearColorValueFloat(float32 = Array(0.2f, 0.2f, 0.2f, 0.2f)))
    val clearValues1 = new Vulkan.ClearValueDepthStencil(depthStencil = new Vulkan.ClearDepthStencilValue(depth = 1.0f, stencil = 0))
    beginRenderPass(commandBuffer, renderPass, currentBuffer, framebuffers, width, height, Array(clearValues0, clearValues1)) 
    vk.cmdBindVertexBuffers(commandBuffer, 0, 1, Array(vertexBuffer), Array(new Vulkan.DeviceSize(0)))
    vk.cmdEndRenderPass(commandBuffer)
    vk.endCommandBuffer(commandBuffer)

    val fence = initFence(device)
    submitQueue(device, fence, commandBuffer, graphicsQueue)

    vk.destroyFence(device, fence)

    framebuffers.foreach { f =>
      vk.destroyFramebuffer(device, f)
    }

    vk.destroyPipeline(device, pipeline)

    vk.destroyBuffer(device, vertexBuffer)
    vk.freeMemory(device, vertexBufferMemory)

    vk.destroyShaderModule(device, vertexModule)
    vk.destroyShaderModule(device, fragmentModule)
    vk.destroyRenderPass(device, renderPass)
    vk.destroySemaphore(device, semaphore)
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
}
