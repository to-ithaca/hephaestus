package hephaestus
package lunarg
package tutorial

import hephaestus.platform._

object Step03 extends Utils {

  def main(args: Array[String]): Unit = {

    val instance = initInstance()
    val physicalDevices = vk.enumeratePhysicalDevices(instance)
    val physicalDevice = physicalDevices(0)
    val qfps = vk.getPhysicalDeviceQueueFamilyProperties(physicalDevice)

    val qi = qfps.zipWithIndex.find {
      case (q, i) => (q.queueFlags & Vulkan.QUEUE_GRAPHICS_BIT) > 0
    }.map(_._2).get

    val dqinfo = new Vulkan.DeviceQueueCreateInfo(
      flags = 0,
      queueFamilyIndex = qi,
      queuePriorities = Array(0f)
    )
    val dinfo = new Vulkan.DeviceCreateInfo(
      queueCreateInfos = Array(dqinfo),
      enabledExtensionNames = Array.empty[String])
    val device = vk.createDevice(physicalDevice, dinfo)

    vk.destroyDevice(device)
    vk.destroyInstance(instance)
  }
}
