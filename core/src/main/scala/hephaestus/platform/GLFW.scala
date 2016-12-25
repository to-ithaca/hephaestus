package hephaestus
package platform

import ch.jodersky.jni.nativeLoader

trait Callback[A] {
  def call(a: A): Unit
}


@nativeLoader("hephaestus0")
class GLFW {

  @native def init(): Boolean
  @native def version: GLFW.Version
  @native def terminate(): Unit

  @native def createWindow(width: Int, height: Int, name: String): GLFW.Window
  @native def destroyWindow(window: GLFW.Window): Unit

  @native def setWindowCloseCallback(window: GLFW.Window, callback: Callback[GLFW.Window]): Unit
  @native def waitEvents(): Unit

  @native def windowHint(hint: GLFW.Hint, value: GLFW.HintValue): Unit
  @native def vulkanSupported(): Boolean

  @native def getRequiredInstanceExtensions(): Array[String]
  @native def createWindowSurface(inst: Vulkan.Instance, window: GLFW.Window): Vulkan.Surface

  @native def getPhysicalDevicePresentationSupport(inst: Vulkan.Instance, device: Vulkan.PhysicalDevice, index: Int): Boolean
}

object GLFW {

  final class Version(val ptr: Array[Int]) extends AnyVal {
    def major: Int = ptr(0)
    def minor: Int = ptr(1)
    def rev: Int = ptr(2)
  }

  final class Window(val ptr: Long) extends AnyVal

  final class Hint(val hint: Int) extends AnyVal
  val CLIENT_API: Hint = new Hint(0x00022001)

  final class HintValue(val value: Int) extends AnyVal
  val NO_API: HintValue = new HintValue(0)
}
