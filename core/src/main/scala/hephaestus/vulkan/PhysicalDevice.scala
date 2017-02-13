package hephaestus
package vulkan

import hephaestus.platform.Vulkan
import hephaestus.platform.Vulkan.{
  Instance => VkInstance,
  PhysicalDevice => VkPhysicalDevice,
  CommandPool => VkCommandPool,
  Device => VkDevice,
  _
}

import cats.data._

case class PhysicalDevice(device: VkPhysicalDevice,
                          vk: Vulkan,
                          memoryProperties: PhysicalDeviceMemoryProperties)

object PhysicalDevice {

  def apply(i: VkInstance, vk: Vulkan): PhysicalDevice = {
    val d = vk.enumeratePhysicalDevices(i)(0)
    val mps = vk.getPhysicalDeviceMemoryProperties(d)
    PhysicalDevice(d, vk, mps)
  }

  def memoryProperties: Reader[PhysicalDevice, PhysicalDeviceMemoryProperties] =
    Reader(d => d.vk.getPhysicalDeviceMemoryProperties(d.device))
  def queueFamilyProperties: Reader[PhysicalDevice,
                                    Array[QueueFamilyProperties]] =
    Reader(d => d.vk.getPhysicalDeviceQueueFamilyProperties(d.device))
  def formatProperties(f: Format): Reader[PhysicalDevice, FormatProperties] =
    Reader(d => d.vk.getPhysicalDeviceFormatProperties(d.device, f))

  def surfaceSupport(queueFamilyIndex: Int,
                     s: Surface): Reader[PhysicalDevice, Boolean] =
    Reader(
      d => d.vk.getPhysicalDeviceSurfaceSupport(d.device, queueFamilyIndex, s))
  def surfaceFormats(
      s: Surface): Reader[PhysicalDevice, Array[SurfaceFormat]] =
    Reader(d => d.vk.getPhysicalDeviceSurfaceFormats(d.device, s))
  def surfaceCapabilities(
      s: Surface): Reader[PhysicalDevice, SurfaceCapabilities] =
    Reader(d => d.vk.getPhysicalDeviceSurfaceCapabilities(d.device, s))
  def surfacePresentModes(s: Surface): Reader[PhysicalDevice, Array[Int]] =
    Reader(d => d.vk.getPhysicalDeviceSurfacePresentModes(d.device, s))

  def createDevice(info: DeviceCreateInfo): Reader[PhysicalDevice, Device] =
    Reader(d => Device(d.vk.createDevice(d.device, info), d, d.vk))
  def destroyDevice(dv: Device): Reader[PhysicalDevice, Unit] =
    Reader(d => d.vk.destroyDevice(dv.device))
}
