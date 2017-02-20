package hephaestus
package vulkan

import hephaestus.platform.Vulkan
import hephaestus.platform.Vulkan.{
  CommandPool => VkCommandPool,
  Device => VkDevice,
  _
}

import cats.data._

case class CommandPool(pool: VkCommandPool, device: Device, vk: Vulkan)

object CommandPool {

  def allocateCommandBuffers(
      l: CommandBufferLevel,
      count: Int): Reader[CommandPool, Array[CommandBuffer]] =
    Reader(
      p =>
        Array(
          CommandBuffer(p.vk.allocateCommandBuffers(
                          p.device.device,
                          new CommandBufferAllocateInfo(p.pool, l, count)),
                        p.vk)))

  def freeCommandBuffers(bs: Array[CommandBuffer]): Reader[CommandPool, Unit] =
    Reader(p =>
      p.vk
        .freeCommandBuffers(p.device.device, p.pool, bs.size, bs.head.buffer))
}
