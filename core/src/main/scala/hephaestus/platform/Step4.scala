package hephaestus
package platform

object Step4 extends Utils {
  def main(args: Array[String]): Unit = {
    val instance = initInstance()
    val physicalDevice = vk.enumeratePhysicalDevices(instance)(0)
    val device = initDevice(physicalDevice)

    val graphicsQueueFamilyIndex = initGraphicsQueueFamilyIndex(physicalDevice)
    val commandPoolInfo = new Vulkan.CommandPoolCreateInfo(
      pNext = 0,
      flags = 0,
      queueFamilyIndex = graphicsQueueFamilyIndex)
    val commandPool = vk.createCommandPool(device, commandPoolInfo)

    val commandBufferAllocateInfo = new Vulkan.CommandBufferAllocateInfo(
      pNext = 0,
      commandPool = commandPool,
      level = Vulkan.COMMAND_BUFFER_LEVEL_PRIMARY,
      commandBufferCount = 1)
    val commandBuffer = vk.allocateCommandBuffers(device, commandBufferAllocateInfo)

    vk.freeCommandBuffers(device, commandPool, 1, commandBuffer)
    vk.destroyCommandPool(device, commandPool)

    vk.destroyDevice(device)
    vk.destroyInstance(instance)
  }
}
