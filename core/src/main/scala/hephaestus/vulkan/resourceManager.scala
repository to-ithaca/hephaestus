package hephaestus
package vulkan

import cats._, cats.data._, cats.implicits._
import monocle._, monocle.macros._, monocle.syntax._, monocle.function.all._, monocle.std.list._
import spire._, spire.implicits._, spire.math._
import hephaestus.platform.Vulkan
import hephaestus.platform.Vulkan.{
  Buffer => VkBuffer,
  Image => VkImage,
  MemoryRequirements => VkMemoryRequirements,
  Format => VkFormat,
  ImageUsageFlagBit => VkImageUsage,
  ImageLayout => VkImageLayout,
  Queue => VkQueue,
  Semaphore => VkSemaphore,
  Fence => VkFence
}
import java.nio.ByteBuffer



case class Buffer(buffer: VkBuffer, memory: DeviceMemory, slot: Bounded[Long], slots: List[Bounded[Long]])
object Buffer {
  val _slots: Lens[Buffer, List[Bounded[Long]]] = GenLens[Buffer](_.slots)
}
//TODO: type this
case class BufferGroup(buffers: List[Buffer], flags: List[Int], size: Long, usage: Int)
object BufferGroup {
  val _buffers: Lens[BufferGroup, List[Buffer]] = GenLens[BufferGroup](_.buffers)
  val _size: Lens[BufferGroup, Long] = GenLens[BufferGroup](_.size)
}

case class ImageInfo(format: VkFormat, usage: VkImageUsage, layout: VkImageLayout, width: Int, height: Int)
case class Image(image: VkImage, reqs: VkMemoryRequirements, info: ImageInfo, layers: Int, available: List[Int], memory: DeviceMemory)
object Image {
  val _available: Lens[Image, List[Int]] = GenLens[Image](_.available)
}

/** Manages buffers and images.  There should be a single resource manager per device.
  * 
  * @param device the device used
  * @param allocator the allocator used
  * @param queue the dedicated queue for resource loading.
  * @param semaphore the dedicated semaphore used to indicate when resources have loaded.
  * @param cmd the dedicated command buffer for resource loading.
  * @param fence the dedicated fence used to indicate when resources have loaded.
  * @param host the buffer group for host accessible memory
  * @param local the buffer group for device accessible memory
  * @param stage the buffer group for host accessible staging area
  * @param imageFlags the flags used to create a device local image
  * @param images a list of currently loaded images
  * @param loads a list of load operations to be executed on submit
  * @param copies a list of buffer copy operations to be executed on submit
  * @param imageCopies a list of image copy operations to be executed on submit
  * @param pending a list of staged resources that the device is currently copying.  The fence indicates when these have been copied.
  */
case class ResourceManager(
  device: Device,
  allocator: Suballocator, 
  queue: VkQueue,
  semaphore: VkSemaphore,
  cmd: CommandBuffer,
  fence: VkFence,
  host: BufferGroup, local: BufferGroup, stage: BufferGroup,
  imageFlags: List[Int],
  images: List[Image],
  loads: List[ResourceManager.LoadOp], copies: List[ResourceManager.CopyOp], imageCopies: List[ResourceManager.ImageCopyOp],
  pending: List[ResourceManager.Slot]
)
object ResourceManager {

  case class Slot(buffer: Buffer, slot: Bounded[Long])
  case class LoadOp(data: ByteBuffer, slot: Slot)
  case class CopyOp(src: Slot, dest: Slot)

  case class ImageSlot(image: Image, layer: Int)
  case class ImageCopyOp(src: Slot, dest: ImageSlot)

  case class StageFence(slots: List[Slot])

  type Stack[A] = StateT[Either[MemoryError, ?], ResourceManager, A]
  type SStack[A] = State[ResourceManager, A]

  val _device: Lens[ResourceManager, Device] = GenLens[ResourceManager](_.device)
  val _cmd: Lens[ResourceManager, CommandBuffer] = GenLens[ResourceManager](_.cmd)
  val _queue: Lens[ResourceManager, VkQueue] = GenLens[ResourceManager](_.queue)
  val _semaphore: Lens[ResourceManager, VkSemaphore] = GenLens[ResourceManager](_.semaphore)
  val _fence: Lens[ResourceManager, VkFence] = GenLens[ResourceManager](_.fence)
  val _allocator: Lens[ResourceManager, Suballocator] = GenLens[ResourceManager](_.allocator)
  val _imageFlags: Lens[ResourceManager, List[Int]] = GenLens[ResourceManager](_.imageFlags)
  val _stage: Lens[ResourceManager, BufferGroup] = GenLens[ResourceManager](_.stage)
  val _local: Lens[ResourceManager, BufferGroup] = GenLens[ResourceManager](_.local)
  val _host: Lens[ResourceManager, BufferGroup] = GenLens[ResourceManager](_.host)
  val _images: Lens[ResourceManager, List[Image]] = GenLens[ResourceManager](_.images)
  val _loads: Lens[ResourceManager, List[LoadOp]] = GenLens[ResourceManager](_.loads)
  val _copies: Lens[ResourceManager, List[CopyOp]] = GenLens[ResourceManager](_.copies)
  val _imageCopies: Lens[ResourceManager, List[ImageCopyOp]] = GenLens[ResourceManager](_.imageCopies)
  val _pending: Lens[ResourceManager, List[Slot]] = GenLens[ResourceManager](_.pending)

  def from(d: Device, a: Suballocator, q: VkQueue, s: VkSemaphore, cmd: CommandBuffer, f: VkFence): ResourceManager = {
    val localUsage = Vulkan.BUFFER_USAGE_TRANSFER_DST_BIT | Vulkan.BUFFER_USAGE_VERTEX_BUFFER_BIT | Vulkan.BUFFER_USAGE_INDEX_BUFFER_BIT
    val stageUsage = Vulkan.BUFFER_USAGE_TRANSFER_SRC_BIT
    val hostUsage = Vulkan.BUFFER_USAGE_UNIFORM_BUFFER_BIT

    val localFlags = List(Vulkan.MEMORY_PROPERTY_DEVICE_LOCAL_BIT)
    val hostVisibleFlags = List(
      Vulkan.MEMORY_PROPERTY_HOST_VISIBLE_BIT | Vulkan.MEMORY_PROPERTY_HOST_CACHED_BIT | Vulkan.MEMORY_PROPERTY_HOST_COHERENT_BIT,
      Vulkan.MEMORY_PROPERTY_HOST_VISIBLE_BIT | Vulkan.MEMORY_PROPERTY_HOST_CACHED_BIT,
      Vulkan.MEMORY_PROPERTY_HOST_VISIBLE_BIT | Vulkan.MEMORY_PROPERTY_HOST_COHERENT_BIT)
    ResourceManager(
      device = d,
      allocator = a,
      queue = q,
      semaphore = s,
      cmd = cmd,
      fence = f,
      host = BufferGroup(Nil, hostVisibleFlags, 1024, hostUsage),
      local = BufferGroup(Nil, localFlags, 1024, localUsage),
      stage = BufferGroup(Nil, hostVisibleFlags, 1024, stageUsage),
      imageFlags = localFlags,
      images = Nil,
      loads = Nil,
      copies = Nil,
      imageCopies = Nil,
      pending = Nil)
  }

  private def remove(s: Bounded[Long], o: Bounded[Long]): List[Bounded[Long]] = (s -- o).asInstanceOf[List[Bounded[Long]]]

  //finds the first empty slot, and removes it from the available space
  //this can be made more efficient by using a buddy system for memory management
  private def find(size: Long, alignment: Option[Long]): State[BufferGroup, Option[Slot]] = State { g =>
    g.buffers.foldLeft(Option.empty[(Buffer, Bounded[Long], Long)]) { (o, b) => o match {
      case None =>
        b.slots.map { s =>
          val offset = alignment.map(a => scala.math.ceil(s.lower.toDouble / a.toDouble).toLong * a).getOrElse(s.lower)
          (s, offset)
        }.find {
          case (s, o) => (s.upper - o) >= size
        }.map {
          case (s, o) => (b, s, o)
        }
      case Some(a) => Some(a)
    }} match {
      case Some((b, s, o)) =>
        val slot = Slot(b, Bounded[Long](o, o + size, 0))
        val next = BufferGroup._buffers.modify(bs => (Buffer._slots.modify(ss => ss.filterNot(_ == s) ::: remove(s, slot.slot))(b)) ::  bs.filterNot(_ == b))(g)
        (next, Some(slot))
      case None => (g, None)
    }
  }

  //allocates a new block of memory, binds it to a new buffer and removes a slot
  private def alloc(size: Long, alignment: Option[Long], l: Lens[ResourceManager, BufferGroup]): StateT[Either[MemoryError, ?], ResourceManager, Slot] = for {
    mSize <- (l ^|-> BufferGroup._size).use[Stack].map { s => (scala.math.ceil(size.toDouble / s.toDouble) * s).toLong }
    g <- l.use[Stack]
    vbr <- _device.use[Stack].map { d =>
      Device.createBuffer(
        new Vulkan.BufferCreateInfo(usage = g.usage,
          size = mSize,
          queueFamilyIndices = Array.empty,
          sharingMode = Vulkan.SHARING_MODE_EXCLUSIVE,
          flags = 0)).run(d)
    }
    m <- Suballocator.alloc(g.flags, vbr._2, mSize).transformLens(_allocator)
    _ <- _device.use[Stack].map { d => Device.bindBufferMemory(vbr._1, m.memory, 0).run(d) }
    _ <- (l ^|-> BufferGroup._buffers).modifying[Stack]{ bs =>
      Buffer(vbr._1, m, Bounded[Long](0, mSize, 0), List(Bounded[Long](0, mSize, 0))) :: bs
    }
    s <- find(size, alignment).transformF[Either[MemoryError, ?], Option[Slot]](a => Either.right(a.value)).transformLens(l).map {
      case Some(s) => s
      case None => sys.error("Impossible case! Space has been allocated for this slot.")
    }
  } yield s

  //finds or allocates a slot
  private def slot(size: Long, alignment: Option[Long], l: Lens[ResourceManager, BufferGroup]): Stack[Slot] = for {
    so <- find(size, alignment).transformF[Either[MemoryError, ?], Option[Slot]](a => Either.right(a.value)).transformLens(l)
    s <- so match {
      case Some(s) => StateT.pure[Either[MemoryError, ?], ResourceManager, Slot](s)
      case None => alloc(size, alignment, l)
    }
  } yield s


  private def join[A](as: List[Bounded[A]])(bs: List[Bounded[A]]): List[Bounded[A]] = {
    def single[A](t: Bounded[A])(is: List[Bounded[A]]): List[Bounded[A]] =
      is.foldLeft((List.empty[Bounded[A]], t)) { (tup, i) => tup match {
        case (acc, t) => if((i intersect t).isPoint) (acc, (i union t).asInstanceOf[Bounded[A]]) else (i :: acc, t)
      }} match { case (is, i) => i :: is }
    as.foldLeft(bs)((b, a) => single(a)(b))
  }

  private def flush(ps: List[Slot])(s: BufferGroup): BufferGroup = 
    BufferGroup._buffers.modify(_.map(b => Buffer._slots.modify(join(ps.filter(_.buffer.buffer == b.buffer).map(_.slot)))(b)))(s)

  //finds a slot, or waits for a free slot.  If has waited, and no slots are free, allocates a new slot
  private def stageSlot(size: Long, alignment: Option[Long], l: Lens[ResourceManager, BufferGroup]): Stack[Slot] = for {
    so <- find(size, alignment).transformF[Either[MemoryError, ?], Option[Slot]](a => Either.right(a.value)).transformLens(l)
    s <- so match {
      case Some(s) => StateT.pure[Either[MemoryError, ?], ResourceManager, Slot](s)
      case None => for {
        d <- _device.use[Stack]
        r <- _fence.use[Stack].map(f => Device.getFenceStatus(f).run(d))
        ps <- _pending.use[Stack]
        s <- if(r == Vulkan.SUCCESS) {
          _pending.modifying[Stack](_ => Nil) >> _stage.modifying[Stack](flush(ps)) >>
          find(size, alignment).transformF[Either[MemoryError, ?], Option[Slot]](a => Either.right(a.value)).transformLens(l).flatMap {
            case Some(s) => StateT.pure[Either[MemoryError, ?], ResourceManager, Slot](s)
            case None => alloc(size, alignment, l)
          }
        } else alloc(size, alignment, l)
      } yield s
    }
  } yield s

  def loadLocal(data: ByteBuffer, alignment: Option[Long]): Stack[Slot] = for {
    ss <- slot(data.capacity(), None, _stage)
    ls <- slot(data.capacity(), alignment, _local)
    _ <- _loads.modifying[Stack](ls => LoadOp(data, ss) :: ls)
    _ <- _copies.modifying[Stack](cs => CopyOp(ss, ls) :: cs)
  } yield ls

  def loadHost(data: ByteBuffer, alignment: Option[Long]): Stack[Slot] = for {
    hs <- slot(data.capacity(), alignment, _host)
    _ <- _loads.modifying[Stack](ls => LoadOp(data, hs) :: ls)
  } yield hs

  def reloadHost(data: ByteBuffer, slot: Slot): SStack[Unit] = 
    _loads.modifying[SStack](ls => LoadOp(data, slot) :: ls)


  //finds and removes an avaliable image layer
  private def find(info: ImageInfo): Stack[Option[ImageSlot]] = for {
    s <- _images.use[Stack].map(_.filter(_.info == info).map(i => i.available match {
      case Nil => None
      case h :: t => Some(ImageSlot(i, h))
    }).filter(_.nonEmpty).map(_.get).headOption)
    _ <- _images.modifying[Stack](is => s match {
      case Some(ImageSlot(i, l)) => Image._available.modify(_.filterNot(_ == l))(i) :: is.filterNot(_ == i)
      case None => is
    })
  } yield s

  //allocs a new image and removes a layer
  private def alloc(info: ImageInfo, layers: Int): Stack[ImageSlot] = for {
    tup <- (_device).use[Stack].map(d => Device.createImage(new Vulkan.ImageCreateInfo(
      flags = 0,
      imageType = Vulkan.IMAGE_TYPE_2D,
      format = info.format,
      extent = new Vulkan.Extent3D(width = info.width,
        height = info.height,
        depth = 1),
      mipLevels = 1,
      arrayLayers = layers,
      samples = Vulkan.SAMPLE_COUNT_1_BIT,
      tiling = Vulkan.IMAGE_TILING_OPTIMAL,
      initialLayout = info.layout,
      usage = info.usage,
      queueFamilyIndices = Array.empty,
      sharingMode = Vulkan.SHARING_MODE_EXCLUSIVE
    )).run(d))
    (i, reqs) = tup
    fs <- _imageFlags.use[Stack]
    m <- Suballocator.alloc(fs, reqs, reqs.size).transformLens(_allocator)
    _ <- _device.use[Stack].map { d => Device.bindImageMemory(i, m.memory, 0).run(d) }
    _ <- _images.modifying[Stack](is => Image(i, reqs, info, layers, (0 until layers).toList, m) :: is)
    a <- find(info).map {
      case Some(a) => a
      case None => sys.error("Impossible case! Space has been allocated for this image")
    }
  } yield a

  //finds or allocs an image slot
  private def image(info: ImageInfo): Stack[ImageSlot] = for {
    i <- find(info)
    s <- i match {
      case Some(a) => StateT.pure[Either[MemoryError, ?], ResourceManager, ImageSlot](a)
      case None => alloc(info, 1)
    }
  } yield s

  def loadImage(data: ByteBuffer, info: ImageInfo): Stack[ImageSlot] = for {
    ss <- slot(data.capacity(), None, _stage)
    is <- image(info)
    _ <- _loads.modifying[Stack](ls => LoadOp(data, ss) :: ls)
    _ <- _imageCopies.modifying[Stack](cs => ImageCopyOp(ss, is) :: cs)
  } yield is

  def loadEmptyImage(info: ImageInfo): Stack[ImageSlot] = image(info)

  def submit: SStack[Option[VkSemaphore]] = for {
    d <- _device.use[SStack]
    cmd <- _cmd.use[SStack]
    rm <- State.get
    q <- _queue.use[SStack]
    sm <- _semaphore.use[SStack]
    f <- _fence.use[SStack]
    ls <- _loads.use[SStack]
    cs <- _copies.use[SStack]
    ics <- _imageCopies.use[SStack]
    _ <- _loads.modifying[SStack](_ => Nil)
    _ <- _copies.modifying[SStack](_ => Nil)
    _ <- _imageCopies.modifying[SStack](_ => Nil)
    _ <- _pending.modifying[SStack](ps => ics.map(_.src) ::: cs.map(_.src) ::: ps)
  } yield {

    ls.groupBy(_.slot.buffer.buffer).map {
      case (b, ops) => 
        val buf = ops.head.slot.buffer
        val ptr = Device.mapMemory(buf.memory.memory, buf.slot).run(d)
        ops.foreach{ o =>
          d.vk.loadMemory(ptr + o.slot.slot.lower, o.data)
        }
    }

    val ranges = ls.filterNot { _.slot.buffer.memory.tpe.tpe.propertyFlags.hasBitFlags(Vulkan.MEMORY_PROPERTY_HOST_COHERENT_BIT) }.toList.map { d =>
      new Vulkan.MappedMemoryRange(memory = d.slot.buffer.memory.memory, offset = d.slot.slot.lower, size = d.slot.slot.upper - d.slot.slot.lower)
    }
    if(ranges.nonEmpty) {
      Device.flushMappedMemoryRanges(ranges).run(d)
    }
    ls.groupBy(_.slot.buffer.buffer).map {
      case (b, ops) => 
        val buf = ops.head.slot.buffer
        Device.unmapMemory(buf.memory.memory).run(d)
    }

    if(cs.nonEmpty || ics.nonEmpty) {
      CommandBuffer.begin(new Vulkan.CommandBufferBeginInfo(
          flags = Vulkan.COMMAND_BUFFER_USAGE_BLANK_FLAG,
          inheritanceInfo = Vulkan.COMMAND_BUFFER_INHERITANCE_INFO_NULL_HANDLE)).run(cmd)
      if(cs.nonEmpty) {
        cs.groupBy(c => (c.src.buffer.buffer, c.dest.buffer.buffer)).foreach { case ((s, d), cs) =>
          CommandBuffer.copy(s, d, cs).run(cmd)
        }
      }
      ics.foreach { ic =>
        CommandBuffer.copyBufferToImage(ic).run(cmd)
      }
      CommandBuffer.end.run(cmd)
      Device.resetFence(f).run(d)
      d.vk.queueSubmit(q, 1, Array(
        new Vulkan.SubmitInfo(
          waitSemaphores = Array.empty,
          waitDstStageMask = Array.empty,
          commandBuffers = Array(cmd.buffer),
          signalSemaphores = Array(sm))
      ), f)
      Some(sm)
    } else None
  }

  def free: SStack[Unit] = {
    def free(g: BufferGroup): Reader[Device, Unit] = g.buffers.traverse(b => Device.destroyBuffer(b.buffer)).void
    State { m =>
      (free(m.host) >> free(m.local) >> free(m.stage) >> m.images.traverse(i => Device.destroyImage(i.image))).run(m.device)
      val nextM = (
        (_host ^|-> BufferGroup._buffers).set(Nil) andThen
          (_local ^|-> BufferGroup._buffers).set(Nil) andThen
          (_stage ^|-> BufferGroup._buffers).set(Nil) andThen
          (_images.set(Nil)))(m)
      (nextM, ())
    }
  }
}
