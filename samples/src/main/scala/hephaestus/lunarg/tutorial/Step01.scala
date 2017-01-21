package hephaestus
package lunarg
package tutorial

import hephaestus.platform._

object Step01 extends Utils {

  def main(args: Array[String]): Unit = {

    val appInfo = new Vulkan.ApplicationInfo(
      applicationName = "helloWorld",
      applicationVersion = 1,
      engineName = "helloWorld",
      engineVersion = 1,
      apiVersion = Vulkan.API_VERSION_1_0
    )
    val instanceCreateInfo = new Vulkan.InstanceCreateInfo(
      applicationInfo = appInfo,
      enabledExtensionNames = Array.empty[String],
      enabledLayerNames = Array.empty[String]
    )
    val instance = vk.createInstance(instanceCreateInfo)
    vk.destroyInstance(instance)
  }
}
