package hephaestus
package lunarg
package tutorial

import hephaestus.platform._

object Step02 extends Utils {

  def main(args: Array[String]): Unit = {
    val instance = initInstance()
    val physicalDevices = vk.enumeratePhysicalDevices(instance)
    vk.destroyInstance(instance)
  }
}
