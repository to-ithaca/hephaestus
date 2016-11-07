package hephaestus
package platform

import ch.jodersky.jni.nativeLoader

@nativeLoader("hephaestus0")
class GLFW {

  @native def init(): Boolean
  @native def version: GLFW.Version
  @native def terminate()

  @native def createWindow(size: Int): GLFW.Window
  @native def destroyWindow(window: GLFW.Window): GLFW.Window
}

object GLFW {
  final class Version(val ptr: Array[Int]) extends AnyVal {
    def major: Int = ptr(0)
    def minor: Int = ptr(1)
    def rev: Int = ptr(2)
  }

  final class Window(val ptr: Long) extends AnyVal
}
