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

    qfps.foreach { qfp =>
      println(
        s"flags ${qfp.queueFlags} count ${qfp.queueCount} bits: ${qfp.timestampValidBits}")
    }

    val qi = qfps.zipWithIndex
      .find {
        case (q, i) => (q.queueFlags & Vulkan.QUEUE_GRAPHICS_BIT) > 0
      }
      .map(_._2)
      .get

    val dqinfo = new Vulkan.DeviceQueueCreateInfo(
      flags = 0,
      queueFamilyIndex = qi,
      queuePriorities = Array(0f)
    )
    val dinfo = new Vulkan.DeviceCreateInfo(queueCreateInfos = Array(dqinfo),
                                            enabledExtensionNames =
                                              Array.empty[String])
    val device = vk.createDevice(physicalDevice, dinfo)

    val textureFormatProperties = List(
      vk.getPhysicalDeviceFormatProperties(physicalDevice,
                                           Vulkan.FORMAT_R8G8B8A8_UNORM),
      vk.getPhysicalDeviceFormatProperties(physicalDevice,
                                           Vulkan.FORMAT_R8G8B8_UNORM)
    )
    textureFormatProperties.foreach { p =>
      println(
        s"lin ${p.linearTilingFeatures} op: ${p.optimalTilingFeatures} buf: ${p.bufferFeatures}")
      val fi = p.optimalTilingFeatures & Vulkan.FORMAT_FEATURE_SAMPLED_IMAGE_BIT.value
      println(s"fi $fi")
    }

    vk.destroyDevice(device)
    vk.destroyInstance(instance)
  }
}
