package hephaestus
package platform

object Step3 extends Utils {

  def main(args: Array[String]): Unit = {

    val instance = initInstance()
    val physicalDevices = vk.enumeratePhysicalDevices(instance)
    val physicalDevice = physicalDevices(0)
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
    val device = vk.createDevice(physicalDevice, dinfo)

    vk.destroyDevice(device)
    vk.destroyInstance(instance)
  }
}
