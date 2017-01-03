package hephaestus
package lunarg
package tutorial

import hephaestus.platform._

object Step01 extends Utils {

  def main(args: Array[String]): Unit = {

    val appInfo = new Vulkan.ApplicationInfo(
      pNext = 0,
      pApplicationName = "helloWorld",
      applicationVersion = 1,
      pEngineName = "helloWorld",
      engineVersion = 1,
      apiVersion = Vulkan.API_VERSION_1_0
    )
    val instanceCreateInfo = new Vulkan.InstanceCreateInfo(
      pNext = 0,
      pApplicationInfo = appInfo,
      enabledExtensionCount = 0,
      ppEnabledExtensionNames = Array.empty[String],
      enabledLayerCount = 0,
      ppEnabledLayerNames = Array.empty[String]
    )
    val instance = vk.createInstance(instanceCreateInfo)
    vk.destroyInstance(instance)
  }
}
