package hephaestus
package vulkan

import hephaestus.platform.Vulkan
import hephaestus.platform.Vulkan.{
  Device => VkDevice,
  CommandPool => VkCommandPool,
  CommandBuffer => VkCommandBuffer,
  Buffer => VkBuffer,
  _
}
import cats._
import cats.data._

case class CommandBuffer(buffer: VkCommandBuffer, vk: Vulkan)

object CommandBuffer {
  def begin(info: CommandBufferBeginInfo): Reader[CommandBuffer, Unit] =
    Reader(b => b.vk.beginCommandBuffer(b.buffer, info))
  def end: Reader[CommandBuffer, Unit] =
    Reader(b => b.vk.endCommandBuffer(b.buffer))

  def beginRenderPass(info: RenderPassBeginInfo,
                      c: SubpassContents): Reader[CommandBuffer, Unit] =
    Reader(b => b.vk.cmdBeginRenderPass(b.buffer, info, c))
  def endRenderPass: Reader[CommandBuffer, Unit] =
    Reader(b => b.vk.cmdEndRenderPass(b.buffer))
  def bindVertexBuffers(firstBinding: Int,
                        bs: Array[VkBuffer],
                        os: Array[DeviceSize]): Reader[CommandBuffer, Unit] =
    Reader(
      b => b.vk.cmdBindVertexBuffers(b.buffer, firstBinding, bs.size, bs, os))

  def executeCommands(bs: Array[CommandBuffer]): Reader[CommandBuffer, Unit] =
    Reader(b => b.vk.cmdExecuteCommands(b.buffer, bs.size, bs.map(_.buffer)))

  def bindPipeline(bp: PipelineBindPoint,
                   p: Pipeline): Reader[CommandBuffer, Unit] =
    Reader(b => b.vk.cmdBindPipeline(b.buffer, bp, p))
  def bindDescriptorSets(bp: PipelineBindPoint,
                         l: PipelineLayout,
                         firstSet: Int,
                         sets: Array[DescriptorSet],
                         offsets: Array[Int]): Reader[CommandBuffer, Unit] =
    Reader(
      b =>
        b.vk.cmdBindDescriptorSets(b.buffer,
                                   bp,
                                   l,
                                   firstSet,
                                   sets.size,
                                   sets,
                                   offsets.size,
                                   offsets))
  def setViewport(firstViewport: Int,
                  vs: Array[Viewport]): Reader[CommandBuffer, Unit] =
    Reader(b => b.vk.cmdSetViewport(b.buffer, firstViewport, vs.size, vs))
  def setScissor(firstScissor: Int,
                 ss: Array[Rect2D]): Reader[CommandBuffer, Unit] =
    Reader(b => b.vk.cmdSetScissor(b.buffer, firstScissor, ss.size, ss))
  def draw(vertexCount: Int,
           instanceCount: Int,
           firstVertex: Int,
           firstInstance: Int): Reader[CommandBuffer, Unit] =
    Reader(
      b =>
        b.vk.cmdDraw(b.buffer,
                     vertexCount,
                     instanceCount,
                     firstVertex,
                     firstInstance))

  def copy(src: VkBuffer, dest: VkBuffer, copies: List[ResourceManager.CopyOp]): Reader[CommandBuffer, Unit] =
    Reader(b => b.vk.cmdCopyBuffer(b.buffer, src, dest, copies.map(c => 
          new Vulkan.BufferCopy(
            c.src.slot.lower,
            c.dest.slot.lower,
            c.src.slot.upper - c.src.slot.lower
          )).toArray))


  def copyBufferToImage(ic: ResourceManager.ImageCopyOp): Reader[CommandBuffer, Unit] = 
    Reader(b => b.vk.cmdCopyBufferToImage(b.buffer, ic.src.buffer.buffer, ic.dest.image.image, Vulkan.IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
      Array(new Vulkan.BufferImageCopy(
        bufferOffset = ic.src.slot.lower,
        bufferRowLength = ic.dest.image.info.width,
        bufferImageHeight = ic.dest.image.info.height,
        imageSubresource = new Vulkan.ImageSubresourceLayers(
          aspectMask = Vulkan.IMAGE_ASPECT_COLOR_BIT,
          mipLevel = 0,
          baseArrayLayer = ic.dest.layer,
          layerCount = 1),
        imageOffset = new Vulkan.Offset3D(0, 0, 0),
        imageExtent = new Vulkan.Extent3D(ic.dest.image.info.width, ic.dest.image.info.height, 1)
      ))))
}
