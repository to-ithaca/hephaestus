package hephaestus
package lunarg
package tutorial

import hephaestus.platform._
import java.nio._

/** Draws a coloured cube using a secondary command buffer */
object Step16 extends Utils {

  def main(args: Array[String]): Unit = {
    glfw.init()

    val instance = initInstanceExtensionsDump()

    glfw.windowHint(GLFW.CLIENT_API, GLFW.NO_API)
    val width = 500
    val height = 500
    val window = glfw.createWindow(width, height, "foobar")
    val surface = glfw.createWindowSurface(instance, window)

    val physicalDevice = vk.enumeratePhysicalDevices(instance)(0)
    val qi = initGraphicsPresentQueueFamilyIndex(instance, physicalDevice)
    val device = initDeviceExtensions(physicalDevice, qi)

    val commandPool = vk.createCommandPool(
      device,
      new Vulkan.CommandPoolCreateInfo(
        flags = Vulkan.COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT,
        queueFamilyIndex = qi
      ))
    val commandBuffer = vk.allocateCommandBuffers(
      device,
      new Vulkan.CommandBufferAllocateInfo(
        commandPool = commandPool,
        level = Vulkan.COMMAND_BUFFER_LEVEL_SECONDARY,
        commandBufferCount = 1
      ))
    val primaryCommandBuffer = initCommandBuffer(device, commandPool)

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
    val depthImage = initDepthImage(physicalDevice, device, swapchainExtent)

    val memoryProperties = vk.getPhysicalDeviceMemoryProperties(physicalDevice)
    val depthImageMemory = initDepthImageMemory(physicalDevice,
                                                device,
                                                depthImage,
                                                memoryProperties)
    val depthImageView = initDepthImageView(device, depthImage)

    val uniformData = Cube.uniformData(width, height)
    val buffer = initBuffer(device, uniformData.capacity)
    val bufferMemory =
      initBufferMemory(device, memoryProperties, buffer, uniformData)

    val descriptorSetLayout = initDescriptorSetLayout(device)
    val pipelineLayout = initPipelineLayout(device, descriptorSetLayout)

    val descriptorPool = initDescriptorPool(device)
    val descriptorSets = initDescriptorSets(device,
                                            descriptorPool,
                                            descriptorSetLayout,
                                            buffer,
                                            uniformData.capacity)

    val renderPass = initRenderPass(device, swapchainFormat)

    val vertexModule = initShaderModule("vert.spv", device)
    val fragmentModule = initShaderModule("frag.spv", device)

    val commandBufferBeginInfo = new Vulkan.CommandBufferBeginInfo(
      Vulkan.COMMAND_BUFFER_USAGE_RENDER_PASS_CONTINUE_BIT | Vulkan.COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT,
      new Vulkan.CommandBufferInheritanceInfo(
        renderPass = renderPass
      )
    )
    vk.beginCommandBuffer(commandBuffer, commandBufferBeginInfo)

    val graphicsQueue = vk.getDeviceQueue(device, qi, 0)

    val framebuffers = initFramebuffers(device,
                                        imageViews,
                                        depthImageView,
                                        renderPass,
                                        width,
                                        height)

    val vertexData: ByteBuffer = Cube.solidFaceColorsData
    val vertexBuffer = initVertexBuffer(device, vertexData.capacity)
    val vertexBufferMemory =
      initBufferMemory(device, memoryProperties, vertexBuffer, vertexData)

    val pipeline = initPipeline(device,
                                renderPass,
                                vertexModule,
                                fragmentModule,
                                pipelineLayout)
    val clearValues0 = new Vulkan.ClearValueColor(
      color = new Vulkan.ClearColorValueFloat(
        float32 = Array(0.2f, 0.2f, 0.2f, 0.2f)))
    val clearValues1 = new Vulkan.ClearValueDepthStencil(
      depthStencil =
        new Vulkan.ClearDepthStencilValue(depth = 1.0f, stencil = 0))

    val semaphore = initSemaphore(device)
    val currentBuffer = vk.acquireNextImageKHR(device,
                                               swapchain,
                                               java.lang.Long.MAX_VALUE,
                                               semaphore,
                                               new Vulkan.Fence(0))

    //code start
    vk.cmdBindPipeline(commandBuffer,
                       Vulkan.PIPELINE_BIND_POINT_GRAPHICS,
                       pipeline)
    vk.cmdBindDescriptorSets(commandBuffer,
                             Vulkan.PIPELINE_BIND_POINT_GRAPHICS,
                             pipelineLayout,
                             0,
                             descriptorSets.size,
                             descriptorSets,
                             0,
                             Array.empty)
    vk.cmdBindVertexBuffers(commandBuffer,
                            0,
                            1,
                            Array(vertexBuffer),
                            Array(0L))
    val viewport = new Vulkan.Viewport(height = height,
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

    val fence = initFence(device)
    //wait for the semaphore
    val renderSemaphore = initSemaphore(device)

    //repeat start
    //(0 until 3).foreach { i =>
    println(s"drawing frame")
    vk.beginCommandBuffer(
      primaryCommandBuffer,
      new Vulkan.CommandBufferBeginInfo(
        flags = Vulkan.COMMAND_BUFFER_USAGE_BLANK_FLAG,
        inheritanceInfo = Vulkan.COMMAND_BUFFER_INHERITANCE_INFO_NULL_HANDLE)
    )

    val clearValues = Array(clearValues0, clearValues1)
    val renderPassBeginInfo = new Vulkan.RenderPassBeginInfo(
      renderPass = renderPass,
      framebuffer = framebuffers(currentBuffer),
      renderArea = new Vulkan.Rect2D(
        offset = new Vulkan.Offset2D(x = 0, y = 0),
        extent = new Vulkan.Extent2D(width = width, height = height)),
      clearValues = clearValues
    )
    vk.cmdBeginRenderPass(primaryCommandBuffer,
                          renderPassBeginInfo,
                          Vulkan.SUBPASS_CONTENTS_SECONDARY_COMMAND_BUFFERS)
    vk.cmdExecuteCommands(primaryCommandBuffer, 1, Array(commandBuffer))
    //execute buffer here
    vk.cmdEndRenderPass(primaryCommandBuffer)
    vk.endCommandBuffer(primaryCommandBuffer)
    submitQueueWait(device,
                    fence,
                    primaryCommandBuffer,
                    graphicsQueue,
                    semaphore)
    println("reseting fence")
    //vk.resetFences(device, 1, Array(fence))
    println("reset fence")

    val presentInfo = new Vulkan.PresentInfoKHR(swapchains = Array(swapchain),
                                                imageIndices = currentBuffer,
                                                waitSemaphores = Array.empty)

    vk.queuePresentKHR(graphicsQueue, presentInfo)
    //}

    //repeat end
    Thread.sleep(1000)
    //code end

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
