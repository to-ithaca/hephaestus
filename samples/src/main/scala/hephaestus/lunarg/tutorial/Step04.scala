package hephaestus
package lunarg
package tutorial

import hephaestus.platform._

object Step04 extends Utils {
  def main(args: Array[String]): Unit = {
    val instance = initInstance()
    val physicalDevice = vk.enumeratePhysicalDevices(instance)(0)
    val device = initDevice(physicalDevice)

    val graphicsQueueFamilyIndex = initGraphicsQueueFamilyIndex(physicalDevice)
    val commandPoolInfo = new Vulkan.CommandPoolCreateInfo(
      flags = Vulkan.COMMAND_POOL_BLANK_FLAG,
      queueFamilyIndex = graphicsQueueFamilyIndex)
    val commandPool = vk.createCommandPool(device, commandPoolInfo)

    val commandBufferAllocateInfo = new Vulkan.CommandBufferAllocateInfo(
      commandPool = commandPool,
      level = Vulkan.COMMAND_BUFFER_LEVEL_PRIMARY,
      commandBufferCount = 1)
    val commandBuffer =
      vk.allocateCommandBuffers(device, commandBufferAllocateInfo)

    vk.freeCommandBuffers(device, commandPool, 1, commandBuffer)
    vk.destroyCommandPool(device, commandPool)

    vk.destroyDevice(device)
    vk.destroyInstance(instance)
  }
}
