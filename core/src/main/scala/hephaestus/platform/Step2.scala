package hephaestus
package platform

object Step2 extends Utils {

  def main(args: Array[String]): Unit = {
    val instance = initInstance()
    val physicalDevices = vk.enumeratePhysicalDevices(instance)
    vk.destroyInstance(instance)
  }
}
