// package hephaestus
// package vulkan

// import hephaestus.platform.Vulkan
// import hephaestus.platform.Vulkan.{
//   MemoryHeap => VkMemoryHeap,
//   MemoryType => VkMemoryType,
//   MemoryRequirements => VkMemoryRequirements,
//   Buffer => VkBuffer,
//   DeviceMemory => VkDeviceMemory
// }
// import java.nio.ByteBuffer
// import cats._
// import cats.data._
// import cats.implicits._
// import spire._
// import spire.implicits._
// import spire.math._

// import monocle._
// import monocle.macros._
// import monocle.syntax._
// import monocle.function.all._
// import monocle.std.map._


// case class Image(image: Vulkan.Image, width: Int, height: Int, data: ByteBuffer)

// object MemoryType {
//   val _available: Lens[MemoryType, Long] = GenLens[MemoryType](_.available)
// }

// /** The purpose served by a given buffer. Each buffer has a unique purpose. */
// sealed trait BufferPurpose
// case object Staging extends BufferPurpose
// case object DeviceFastAccess extends BufferPurpose
// case object HostFastAccess extends BufferPurpose

// /** Enriched form of [[hephaestus.platform.Vulkan.Buffer]].
//   * @param buffer the underlying buffer
//   * @param reqs the memory requirements of the buffer
//   * @param size the size of the buffer in bytes
//   * @param tpe the memory type for the buffer
//   * @param memory the device memory backing the buffer
//   *        Multiple buffers will share a single device memory if they have the same memory type.
//   * @param flags the properties of the memory
//   */
// case class Buffer(
//                         buffer: VkBuffer, reqs: VkMemoryRequirements,
//                         tpe: MemoryType,
//                         memory: VkDeviceMemory,
//                         flags: Int,
//                         slot: Bounded[Long]
// )

// object Buffer {

//   /** Contains the information to create a buffer */
//   private case class Spec(tpe: MemoryType,
//                   buffer: VkBuffer,
//                   flags: Int,
//                   purpose: BufferPurpose, reqs: VkMemoryRequirements, size: Long)

//   /** Creates a VkBuffer for a single queue family. For multiple queue families, queueFamilyIndices and SHARING_MODE_CONCURRENT must be set.
//     */
//   private def createBuffer(usage: Int, size: Long): Reader[Device, (VkBuffer, VkMemoryRequirements)] =
//     Device.createBuffer(
//       new Vulkan.BufferCreateInfo(usage = usage,
//                                   size = size,
//                                   queueFamilyIndices = Array.empty,
//                                   sharingMode = Vulkan.SHARING_MODE_EXCLUSIVE,
//                                   flags = 0))

//   /** Selects a memory type matching desired flags and buffer requirements.  Takes [buf.size] available space out of the memory type's heap. **/
//   private def findSpec(buf: VkBuffer, flags: List[Int], pur: BufferPurpose, size: Long, reqs: VkMemoryRequirements)(
//       tpes: List[MemoryType])
//     : Either[MemoryError, (List[MemoryType], Spec)] = {

//     def findType(flags: Int): Either[MemoryError, (List[MemoryType], MemoryType)] = {
//       for {
//         masked <- {
//           val masked = tpes.mask(reqs.memoryTypeBits)
//           Either.cond(masked.nonEmpty,
//             masked,
//             NoMatchForMask(reqs.memoryTypeBits, tpes))
//         }
//         matched <- {
//           val matched = masked.filter(_.tpe.propertyFlags.hasBitFlags(flags))
//           Either.cond(matched.nonEmpty,
//             matched,
//             NoMatchForProperties(flags, masked))
//         }
//         target <- Either.fromOption(matched.find(_.available >= size),
//           NoHeapLargeEnough(size, matched))
//       } yield {
//         val nextTpes: List[MemoryType] = tpes.map(
//           tpe =>
//           if (tpe.heap == target.heap)
//             MemoryType._available.modify(_ - size)(tpe)
//           else tpe)
//         (nextTpes, target)
//       }
//     }

//     @annotation.tailrec
//     def go(flags: List[Int], errs: List[MemoryError])
//         : Either[MemoryError, (List[MemoryType], Spec)] = flags match {
//       case h :: t =>
//         findType(h) match {
//           case Right((nextTpes, tpe)) =>
//             Right((nextTpes, Spec(tpe, buf, h, pur, reqs, size)))
//           case Left(err: NoMatchForMask) => Left(err)
//           case Left(otherErr) => go(t, otherErr :: errs)
//         }
//       case Nil => Left(NoFitForProperties(flags, tpes, errs))
//     }
//     go(flags, Nil)
//   }

//   /** Allocates memory for each memory type in [specs] and binds allocated memory to each memory */
//   private def alloc(specs: List[Spec]): Reader[Device, Map[BufferPurpose, Buffer]] = {
//     def bind(specs: List[Spec],
//              m: Vulkan.DeviceMemory): Reader[Device, List[(BufferPurpose, Buffer)]] = {
//       val offsetSpecs = specs
//         .foldLeft((List.empty[(Spec, Long)], 0L))((b, a) =>
//           (b, a) match {
//             case ((acc, offset), spec) =>
//               ((spec, offset) :: acc, offset + spec.size)
//         })
//         ._1
//       offsetSpecs.traverse {
//         case (s, o) =>
//           Device.bindBufferMemory(s.buffer, m, o).map(_ => (s.purpose, Buffer(s.buffer, s.reqs, s.tpe, m, s.flags, Bounded(o, o + s.size, 0))))
//       }
//     }

//     val groups: List[(Int, List[Spec])] = specs.groupBy(_.tpe.index).toList
//     groups.flatTraverse {
//       case (tpeIndex, specs) =>
//         val size = specs.map(_.size).sum
//         Device
//           .allocateMemory(new Vulkan.MemoryAllocateInfo(size, tpeIndex))
//           .flatMap(m => bind(specs, m))
//     }.map(_.toMap)
//   }

//   def memoryTypes(ps: Vulkan.PhysicalDeviceMemoryProperties): List[MemoryType] = ps.memoryTypes.zipWithIndex.map {
//     case (t, i) => MemoryType(t, i, ps.memoryHeaps(t.heapIndex), ps.memoryHeaps(t.heapIndex).size)
//   }.toList

//   case class Info(purpose: BufferPurpose, usage: Int, props: List[Int], size: Long)

//   /** Allocates Memory
//     *
//     * Memory is allocated using the following steps:
//     *  1. Create a buffer for each purpose with appropriate usage flags
//     *  2. Find the best memory type for the purpose based on a list of desired properties.
//     *  3. Allocates memory for each type
//     *  4. Binds the allocated memory to the created buffers
//     *
//     * Limitations:
//     *  This always allocates a staging area.  Integrated graphics may not need one.
//     *  This only uses a single queue family.
//     *  This does not distribute purposes accross memory types when size hints are not met.
//     */
//   def apply(infos: List[Info])
//     : Reader[Device, Either[MemoryError, Map[BufferPurpose, Buffer]]] =
//     Reader { d =>
//       val memoryProgram = infos
//         .traverse { i =>
//           val (buf, req) = createBuffer(i.usage, i.size).run(d)
//           StateT[Either[MemoryError, ?], List[MemoryType], Spec](
//             findSpec(buf, i.props, i.purpose, i.size, req))
//         }
//         .map(s => alloc(s).run(d))
//       memoryProgram.run(memoryTypes(d.physicalDevice.memoryProperties)).map {
//         case (_, mems) => mems
//       }
//     }

//     def from(localBufferSize: Long,
//       stagingSize: Long,
//       hostBufferSize: Long)
//         : Reader[Device, Either[MemoryError, Map[BufferPurpose, Buffer]]] = {

//       val localUsage = Vulkan.BUFFER_USAGE_TRANSFER_DST_BIT | Vulkan.BUFFER_USAGE_VERTEX_BUFFER_BIT | Vulkan.BUFFER_USAGE_INDEX_BUFFER_BIT
//       val stagingUsage = Vulkan.BUFFER_USAGE_TRANSFER_SRC_BIT
//       val hostUsage = Vulkan.BUFFER_USAGE_UNIFORM_BUFFER_BIT

//       val localFlags = List(Vulkan.MEMORY_PROPERTY_DEVICE_LOCAL_BIT)
//       val hostVisibleFlags = List(
//         Vulkan.MEMORY_PROPERTY_HOST_VISIBLE_BIT | Vulkan.MEMORY_PROPERTY_HOST_CACHED_BIT | Vulkan.MEMORY_PROPERTY_HOST_COHERENT_BIT,
//         Vulkan.MEMORY_PROPERTY_HOST_VISIBLE_BIT | Vulkan.MEMORY_PROPERTY_HOST_CACHED_BIT,
//         Vulkan.MEMORY_PROPERTY_HOST_VISIBLE_BIT | Vulkan.MEMORY_PROPERTY_HOST_COHERENT_BIT
//       )

//       apply(List(
//         Info(Staging, stagingUsage, hostVisibleFlags, stagingSize),
//         Info(HostFastAccess, hostUsage, hostVisibleFlags, hostBufferSize),
//         Info(DeviceFastAccess, localUsage, localFlags, localBufferSize)))
//     }
// }

// abstract class MemoryError(val message: String) extends Exception(message)

// case class NoMatchForMask(mask: Int, tpes: List[MemoryType])
//     extends MemoryError(s"NoMatchForMask: bitmaks [$mask] hides all memory types in [$tpes]")
// case class NoMatchForProperties(props: Int, tpes: List[MemoryType])
//     extends MemoryError(s"NoMatchForProperties: properties [$props] are not present in memory types [$tpes]")
// case class NoHeapLargeEnough(size: Long, tpes: List[MemoryType])
//     extends MemoryError(s"NoHeapLargeEnough: no heap large enough to fit size [$size] in memory types [$tpes]")
// case class NoFitForProperties(props: List[Int],
//                               tpes: List[MemoryType],
//                               errors: List[MemoryError])
//     extends MemoryError(
//     s"""NoFitForProperties: none of the properties [$props] are satisfied by memory types [$tpes].  Properties hve errors: ${errors
//       .map(_.getMessage)
//       .mkString(System.lineSeparator, System.lineSeparator, System.lineSeparator)}""")

// abstract class MemoryManagerError(val message: String) extends Exception(message)

// case class NoFreeSlot(size: Long,
//                       alignment: Option[Long],
//                       slots: MemoryManager.Slots)
//     extends MemoryManagerError(
//     s"NoFreeSlot: none of the slots in slots [$slots] can hold data with size [$size] and alignment [$alignment]")


// //slot must be relative to the buffer slot.
// case class Slice(buffer: Buffer,
//                  slot: Bounded[Long],
//                  purpose: BufferPurpose)


// case class MemoryManager(host: MemoryManager.Slots,
//   staging: MemoryManager.Slots,
//   local: MemoryManager.Slots,
//                          cmd: CommandBuffer,
//                          device: Device,
//                          queue: Vulkan.Queue,
//                          semaphore: Vulkan.Semaphore,
//   loads: Map[Buffer, List[MemoryManager.LoadOp]],
//   copies: List[MemoryManager.CopyOp],
//   imageCopies: List[MemoryManager.ImageCopyOp])

// object MemoryManager {

//   case class LoadOp(data: ByteBuffer, slot: Bounded[Long])

//   /** Represents a slot in the staging buffer that must be copied to the device local buffer */
//   case class CopyOp(src: Bounded[Long], dest: Bounded[Long])
//   case class ImageCopyOp(src: Bounded[Long], dest: Bounded[Long], target: Vulkan.Image, width: Int, height: Int)

//   case class Slots(memory: Buffer, slots: List[Bounded[Long]])
//   val _memory: Lens[Slots, Buffer] = GenLens[Slots](_.memory)
//   val _slots: Lens[Slots, List[Bounded[Long]]] = GenLens[Slots](_.slots)

//   val _host: Lens[MemoryManager, Slots] = GenLens[MemoryManager](_.host)
//   val _staging: Lens[MemoryManager, Slots] = GenLens[MemoryManager](_.staging)
//   val _local: Lens[MemoryManager, Slots] = GenLens[MemoryManager](_.local)
//   val _loads: Lens[MemoryManager, Map[Buffer, List[LoadOp]]] = GenLens[MemoryManager](_.loads)
//   val _copies: Lens[MemoryManager, List[CopyOp]] = GenLens[MemoryManager](_.copies)
//   val _imageCopies: Lens[MemoryManager, List[ImageCopyOp]] = GenLens[MemoryManager](_.imageCopies)
//   val _device: Lens[MemoryManager, Device] = GenLens[MemoryManager](_.device)
//   val _queue: Lens[MemoryManager, Vulkan.Queue] = GenLens[MemoryManager](_.queue)
//   val _semaphore: Lens[MemoryManager, Vulkan.Semaphore] = GenLens[MemoryManager](_.semaphore)
//   val _cmd: Lens[MemoryManager, CommandBuffer] = GenLens[MemoryManager](_.cmd)

//   private def slot(size: Long, alignment: Option[Long]): StateT[Either[MemoryManagerError, ?], Slots, Bounded[Long]] =
//     StateT(
//       slots => Either
//           .fromOption(
//             slots.slots
//               .map { s =>
//                 val offset = alignment
//                   .map(a =>
//                     scala.math.ceil(s.lower.toDouble / a.toDouble).toLong * a)
//                   .getOrElse(s.lower)
//                 (s, offset)
//               }
//               .find {
//                 case (slot, offset) => (slot.upper - offset) >= size
//               },
//             NoFreeSlot(size, alignment, slots)
//           ).map {
//             case (s, offset) => 
//             val slot = Bounded(offset, offset + size, 0)
//             val next = _slots.modify(ss =>
//                 ss.filterNot(_ == s) ::: (s -- slot)
//                   .asInstanceOf[List[Bounded[Long]]])(slots)
//               (next, slot)
//         })

//   private def add[A](a: A)(o: Option[List[A]]): Option[List[A]] = Some(a :: o.getOrElse(Nil))

//   type Stack[A] = StateT[Either[MemoryManagerError, ?], MemoryManager, A]

//   def putDeviceFastAccess(d: ByteBuffer, alignment: Option[Long]): StateT[Either[MemoryManagerError, ?], MemoryManager, Slice] =
//     for {
//       ss <- slot(d.capacity, alignment).transformLens[MemoryManager](_staging)
//       ls <- slot(d.capacity, alignment).transformLens[MemoryManager](_local)
//       s <- (_staging ^|-> _memory).use[Stack]
//       _ <- (_loads ^|-> at(s)).modifying[Stack](add(LoadOp(d, ss)))
//       _ <- _copies.modifying[Stack](CopyOp(ss, ls) :: _)
//       l <- (_local ^|-> _memory).use[Stack]
//     } yield Slice(l, ls, DeviceFastAccess)

//   def putHostFastAccess(d: ByteBuffer, alignment: Option[Long]): StateT[Either[MemoryManagerError, ?], MemoryManager, Slice] = for {
//     s <- slot(d.capacity, alignment).transformLens[MemoryManager](_host)
//     h <- (_host ^|-> _memory).use[Stack]
//       _ <- (_loads ^|-> at(h)).modifying[Stack](add(LoadOp(d, s)))
//   } yield Slice(h, s, HostFastAccess)

//   //TODO: load into an existing slice

//   //we can extend this later to incorporate views for each array layer of an image
//   def put(i: Image): StateT[Either[MemoryManagerError, ?], MemoryManager, Slice] =
//     for {
//       ss <- slot(i.data.capacity, None).transformLens[MemoryManager](_staging)
//       ls <- slot(i.data.capacity, None).transformLens[MemoryManager](_local)
//       s <- (_staging ^|-> _memory).use[StateT[Either[MemoryManagerError, ?], MemoryManager, ?]]
//       _ <- (_loads ^|-> at(s)).modifying[Stack](add(LoadOp(i.data, ss)))
//       _ <- _imageCopies.modifying[Stack](ImageCopyOp(ss, ls, i.image, i.width, i.height) :: _)
//       l <- (_local ^|-> _memory).use[Stack]
//       d <- (_device).use[Stack]
//     } yield {
//       Device.bindImageMemory(i, l.memory, l.slot.lower + ls.lower).run(d)
//       Slice(l, ls, DeviceFastAccess)
//     }

//   def loadMemory(ptr: Long, b: Buffer, ops: List[LoadOp]): Reader[MemoryManager, Unit] =
//     Reader(m => ops.foreach{o => 
//       println(s"loading data with slot ${o.slot} capacity ${o.data.capacity}")
//       m.device.vk.loadMemory(ptr + o.slot.lower, o.data)})

//   def submit: State[MemoryManager, Option[Vulkan.Semaphore]] = for {
//     loads <- _loads.use[State[MemoryManager, ?]]
//     copies <- _copies.use[State[MemoryManager, ?]]
//     imageCopy <- _imageCopies.use[State[MemoryManager, ?]]
//     device <- _device.use[State[MemoryManager, ?]]
//     cmd <- _cmd.use[State[MemoryManager, ?]]
//     mm <- State.get
//     host <- _host.use[State[MemoryManager, ?]]
//     local <- _local.use[State[MemoryManager, ?]]
//     staging <- _staging.use[State[MemoryManager, ?]]
//     queue <- _queue.use[State[MemoryManager, ?]]
//     semaphore <- _semaphore.use[State[MemoryManager, ?]]
//   } yield {
//     loads.foreach { case (buf, datas) => if(datas.nonEmpty) {
//       val ptr = Device.mapMemory(buf.memory, buf.slot).run(device)
//       loadMemory(ptr, buf, datas).run(mm)
//     }}
//     val ranges = loads.filterNot { case (buf, _) => buf.flags.hasBitFlags(Vulkan.MEMORY_PROPERTY_HOST_COHERENT_BIT) }.toList.flatMap {
//       case (buf, datas) =>
//         datas.map(d => new Vulkan.MappedMemoryRange(memory = buf.memory, offset = d.slot.lower, size = d.slot.upper - d.slot.lower))
//     }
//     if(ranges.nonEmpty) {
//       Device.flushMappedMemoryRanges(ranges).run(device)
//     }
//     loads.foreach { case (buf, datas) => if(datas.nonEmpty) {
//       Device.unmapMemory(buf.memory).run(device)
//     }}

//     if(copies.nonEmpty || imageCopy.nonEmpty) {
//       CommandBuffer.begin(new Vulkan.CommandBufferBeginInfo(
//           flags = Vulkan.COMMAND_BUFFER_USAGE_BLANK_FLAG,
//           inheritanceInfo = Vulkan.COMMAND_BUFFER_INHERITANCE_INFO_NULL_HANDLE)).run(cmd)
//       if(copies.nonEmpty) {
//         CommandBuffer.copy(staging.memory, local.memory, copies).run(cmd)
//       }
//       imageCopy.foreach { ic =>
//         CommandBuffer.copyBufferToImage(staging.memory, ic).run(cmd)
//       }
//       CommandBuffer.end.run(cmd)
//       device.vk.queueSubmit(queue, 1, Array(
//         new Vulkan.SubmitInfo(
//           waitSemaphores = Array.empty,
//           waitDstStageMask = Array.empty,
//           commandBuffers = Array(cmd.buffer),
//           signalSemaphores = Array(semaphore))
//       ), new Vulkan.Fence(0))
//       Some(semaphore)
//     } else None
//   }


//   def free(s: Slice): State[MemoryManager, Unit] = {
//     def join[A](t: Bounded[A])(is: List[Bounded[A]]): List[Bounded[A]] =
//       is.foldLeft((List.empty[Bounded[A]], t)) { (tup, i) => tup match {
//         case (acc, t) => if((i intersect t).isPoint) (acc, (i union t).asInstanceOf[Bounded[A]]) else (i :: acc, t)
//       }} match { case (is, i) => i :: is }
//     s.purpose match {
//       case HostFastAccess =>
//         (_host ^|-> _slots).modifying[State[MemoryManager, ?]](join(s.slot))
//       case DeviceFastAccess =>
//         (_local ^|-> _slots).modifying[State[MemoryManager, ?]](join(s.slot))
//       case Staging => sys.error("Impossible case! Staging memory is internal to the memory manager")
//     }
//   }

//   def apply(bs: Map[BufferPurpose, Buffer], d: Device, cb: CommandBuffer, q: Vulkan.Queue, sm: Vulkan.Semaphore): MemoryManager = {
//     val h = Slots(bs(HostFastAccess), List(Bounded(0, bs(HostFastAccess).slot.upper - bs(HostFastAccess).slot.lower, 0)))
//     val l = Slots(bs(DeviceFastAccess), List(Bounded(0, bs(DeviceFastAccess).slot.upper - bs(DeviceFastAccess).slot.lower, 0)))
//     val s = Slots(bs(Staging), List(Bounded(0, bs(Staging).slot.upper - bs(Staging).slot.lower, 0)))
//     MemoryManager(h, s, l, cb, d, q, sm, Map.empty, Nil, Nil)
//   }
// }
