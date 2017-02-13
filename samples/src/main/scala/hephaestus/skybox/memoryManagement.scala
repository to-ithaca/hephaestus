// package hephaestus
// package skybox

// import hephaestus.platform.Vulkan
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

// object MemoryManager {

//   /** Enriched form of [[hephaestus.platform.Vulkan.MemoryType]].
//     *
//     * @param tpe the underlying memory type
//     * @param index the index of the memory type
//     * @param heap the heap which the memory type belongs to
//     * @param available the available space left on the heap in bytes
//     */
//   case class MemoryType(tpe: Vulkan.MemoryType, index: Int, heap: Vulkan.MemoryHeap, available: Long)
//   val _available: Lens[MemoryType, Long] = GenLens[MemoryType](_.available)

//   /** Enriched form of [[hephaestus.platform.Vulkan.Buffer]].
//     *
//     * @param buffer the underlying buffer
//     * @param reqs the memory requirements of the buffer
//     * @param size the size of the buffer in bytes
//     */
//   case class Buffer(buffer: Vulkan.Buffer, reqs: Vulkan.MemoryRequirements, size: Long)

//   /** The purpose served by a given buffer. Each buffer has a unique purpose. */
//   sealed trait BufferPurpose
//   case object Staging extends BufferPurpose
//   case object DeviceFastAccess extends BufferPurpose
//   case object HostFastAccess extends BufferPurpose

//   /** Represents the memory of a buffer
//     *
//     * @param purpose the purpose of the buffer
//     * @param tpe the memory type for the buffer
//     * @param memory the device memory backing the buffer
//     *        Multiple buffers will share a single device memory if they have the same memory type.
//     * @param flags the properties of the memory
//     * @param slots intervals of available space within the buffer
//     */
//   case class Memory(purpose: BufferPurpose, tpe: MemoryType, memory: Vulkan.DeviceMemory, flags: Flags, buffer: Buffer, slots: List[Interval[Long]])
//   val _slots: Lens[Memory, List[Interval[Long]]] = GenLens[Memory](_.slots)
//   val _hostFastAccess: Lens[MemoryManager, Memory] = GenLens[MemoryManager](_.hostFastAccess)
//   val _deviceFastAccess: Lens[MemoryManager, Memory] = GenLens[MemoryManager](_.deviceFastAccess)
//   val _staging: Lens[MemoryManager, Memory] = GenLens[MemoryManager](_.staging)

//   /** Contains the information to create a memory object */
//   case class MemorySpec(tpe: MemoryType, buffer: Buffer, flags: Flags, purpose: BufferPurpose)

//   object Memory {
//     def apply(s: MemorySpec, m: Vulkan.DeviceMemory): Memory =
//       Memory(s.purpose, s.tpe, m, s.flags, s.buffer, List(Interval.closed(0, s.buffer.size)))
//   }

//   type Flags = Int
//   type Usage = Int

//   sealed trait MemoryManagerError extends Exception {
//     def message: String
//   }
//   case class NoMatchForMask(mask: Int, tpes: List[MemoryType]) extends MemoryManagerError {
//     def message: String = s"NoMatchForMask: bitmaks [$mask] hides all memory types in [$tpes]"
//   }
//   case class NoMatchForProperties(props: Int, tpes: List[MemoryType]) extends MemoryManagerError {
//     def message: String = s"NoMatchForProperties: properties [$props] are not present in memory types [$tpes]"
//   }
//   case class NoHeapLargeEnough(size: Long, tpes: List[MemoryType]) extends MemoryManagerError {
//     def message: String = s"NoHeapLargeEnough: no heap large enough to fit size [$size] in memory types [$tpes]"
//   }
//   case class NoFitForProperties(props: List[Flags], tpes: List[MemoryType], errors: List[MemoryManagerError]) extends MemoryManagerError {
//     def message: String = s"NoFitForProperties: none of the properties [$props] are satisfied by memory types [$tpes].  Properties hve errors: ${errors.map(_.message).mkString(System.lineSeparator, System.lineSeparator, System.lineSeparator)}"
//   }

//   case class NoFreeSlot(size: Long, alignment: Option[Long], memory: Memory) extends MemoryManagerError {
//     def message: String = s"NoFreeSlot: none of the slots in memory [$memory] can hold data with size [$size] and alignment [$alignment]"
//   }

//   case class Slice(buffer: Buffer, interval: Interval[Long], purpose: BufferPurpose)

//   val deviceLocalUsage = Vulkan.BUFFER_USAGE_TRANSFER_DST_BIT | Vulkan.BUFFER_USAGE_VERTEX_BUFFER_BIT | Vulkan.BUFFER_USAGE_INDEX_BUFFER_BIT
//   val deviceLocalFlags = List(Vulkan.MEMORY_PROPERTY_DEVICE_LOCAL_BIT)
//   val hostVisibleFlags = List(
//     Vulkan.MEMORY_PROPERTY_HOST_VISIBLE_BIT | Vulkan.MEMORY_PROPERTY_HOST_CACHED_BIT | Vulkan.MEMORY_PROPERTY_HOST_COHERENT_BIT,
//     Vulkan.MEMORY_PROPERTY_HOST_VISIBLE_BIT | Vulkan.MEMORY_PROPERTY_HOST_CACHED_BIT,
//     Vulkan.MEMORY_PROPERTY_HOST_VISIBLE_BIT | Vulkan.MEMORY_PROPERTY_HOST_COHERENT_BIT
//   )
//   val stagingUsage = Vulkan.BUFFER_USAGE_TRANSFER_SRC_BIT
//   val hostVisibleUsage = Vulkan.BUFFER_USAGE_UNIFORM_BUFFER_BIT
//   /** Creates a Buffer given a usage and size.
//     *
//     * The buffer can only be used on a single queue family.
//     * Buffers for use in multiple queue families must have a list of queueFamilyIndices and SHARING_MODE_CONCURRENT.
//     */
//   def createBuffer(vk: Vulkan, d: Vulkan.Device, usage: Int, size: Long): Buffer = {
//     val buffer = vk.createBuffer(d, new Vulkan.BufferCreateInfo(
//       usage = usage,
//       size = new Vulkan.DeviceSize(size),
//       queueFamilyIndices = Array.empty[Int],
//       sharingMode = Vulkan.SHARING_MODE_EXCLUSIVE,
//       flags = 0))
//     val reqs = vk.getBufferMemoryRequirements(d, buffer)
//     Buffer(buffer, reqs, size)
//   }

//   /** Selects a memory type matching desired flags and buffer requirements.  Takes [buf.size] available space out of the memory type's heap. **/
//   def findMemorySpec(buf: Buffer, flags: List[Flags], pur: BufferPurpose)(tpes: List[MemoryType]): Either[MemoryManagerError, (List[MemoryType], MemorySpec)] = {

//     def findType(tpes: List[MemoryType], buf: Buffer, flags: Int): Either[MemoryManagerError, (List[MemoryType], MemoryType)] = {
//       for {
//         masked <- {
//           val masked = tpes.mask(buf.reqs.memoryTypeBits)
//           Either.cond(masked.nonEmpty, masked, NoMatchForMask(buf.reqs.memoryTypeBits, tpes))
//         }
//         matched <- {
//           val matched = masked.filter(_.tpe.propertyFlags.hasBitFlags(flags))
//           Either.cond(matched.nonEmpty, matched, NoMatchForProperties(flags, masked))
//         }
//         target <- Either.fromOption(matched.find(_.available >= buf.size), NoHeapLargeEnough(buf.size, matched))
//       } yield {
//         val nextTpes: List[MemoryType] = tpes.map(tpe => if(tpe.heap == target.heap) _available.modify(_ - buf.size)(tpe) else tpe)
//         (nextTpes, target)
//       }
//     }

//     @annotation.tailrec
//     def go(flags: List[Flags], errs: List[MemoryManagerError]): Either[MemoryManagerError, (List[MemoryType], MemorySpec)] = flags match {
//       case h :: t => findType(tpes, buf, h) match {
//         case Right((nextTpes, tpe)) => Right((nextTpes, MemorySpec(tpe, buf, h, pur)))
//         case Left(err : NoMatchForMask) => Left(err)
//         case Left(otherErr) => go(t, otherErr :: errs)
//       }
//       case Nil => Left(NoFitForProperties(flags, tpes, errs))
//     }
//     go(flags, Nil)
//   }

//   /** Allocates memory for each memory type in [specs] and binds allocated memory to each memory */
//   def alloc(vk: Vulkan, d: Vulkan.Device, specs: List[MemorySpec]): List[Memory] = {
//     def bind(specs: List[MemorySpec], m: Vulkan.DeviceMemory): List[Memory] =
//       specs.foldLeft((List.empty[Memory], 0L))((b, a) => (b, a) match {
//         case ((acc, offset), spec) =>
//           vk.bindBufferMemory(d, spec.buffer.buffer, m, new Vulkan.DeviceSize(offset))
//           (Memory(spec, m) :: acc, offset + spec.buffer.size)
//       })._1

//     val groups: List[(Int, List[MemorySpec])] = specs.groupBy(_.tpe.index).toList
//     groups.flatMap {
//       case (tpeIndex, specs) =>
//         val size = specs.map(_.buffer.size).sum
//         val memory = vk.allocateMemory(d, new Vulkan.MemoryAllocateInfo(new Vulkan.DeviceSize(size), tpeIndex))
//         bind(specs, memory)
//     }
//   }

//   /** Allocates memory for a MemoryManager
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
//   def fromSizeHints(vk: Vulkan, d: Vulkan.Device, tpes: List[MemoryType], q: Vulkan.Queue, cmd: Vulkan.CommandBuffer, semaphore: Vulkan.Semaphore)(deviceFastAccessSize: Long, stagingSize: Long, hostFastAccessSize: Long): Either[MemoryManagerError, MemoryManager] = {

//     val ufs: List[(BufferPurpose, Usage, List[Flags], Long)] = List(
//       (Staging, stagingUsage, hostVisibleFlags, stagingSize),
//       (HostFastAccess, hostVisibleUsage, hostVisibleFlags, hostFastAccessSize),
//       (DeviceFastAccess, deviceLocalUsage, deviceLocalFlags, deviceFastAccessSize)
//     )
//     val memoryProgram = ufs.traverse {
//       case (purpose, usage, props, size) =>
//         val buf = createBuffer(vk, d, usage, size)
//         StateT[Either[MemoryManagerError, ?], List[MemoryType], MemorySpec](findMemorySpec(buf, props, purpose))
//     }.map(f => alloc(vk, d, f))
//     memoryProgram.run(tpes).map {
//       case (nextTpes, mems) =>
//         val staging = mems.find(_.purpose == Staging).get
//         val hostFastAccess = mems.find(_.purpose == HostFastAccess).get
//         val deviceFastAccess = mems.find(_.purpose == DeviceFastAccess).get
//         MemoryManager(vk, d, q, cmd, staging, hostFastAccess, deviceFastAccess, semaphore)
//     }
//   }
// }

// /** Manages memory for the device.

//   * The MemoryManager is responsible for optimal allocation, staging, transfer and freeing of memory.
//   * It attempts to use the fewest possible allocations and buffers.

//   * @param vk the Vulkan binding class
//   * @param queue the queue on which load operations execute
//   * @param cmd the command buffer in which load commands are put.
//   *            the MemoryManager should have full control over this command buffer
//   * @param allocations the blocks of allocated memory for different memory types
//   */
// case class MemoryManager(vk: Vulkan, d: Vulkan.Device, queue: Vulkan.Queue, cmd: Vulkan.CommandBuffer,
//   staging: MemoryManager.Memory, hostFastAccess: MemoryManager.Memory, deviceFastAccess: MemoryManager.Memory,
//   semaphore: Vulkan.Semaphore) {

//   private def findSlot(memory: MemoryManager.Memory, alignment: Option[Long], size: Long): Either[MemoryManager.NoFreeSlot, (Interval[Long], Long)] = {
//     Either.fromOption(memory.slots.map {
//       case slot @ Bounded(l, u, _) =>
//         val offset = alignment.map(a => scala.math.ceil(l.toDouble / a.toDouble).toLong * a).getOrElse(l)
//         (slot, offset)
//       case _ => sys.error("Impossible state. Memory manager can only hold bounded intervals")
//     }.find {
//       case (slot, offset) => (slot.upper - offset) >= size
//     }, MemoryManager.NoFreeSlot(size, alignment, memory))
//   }

//   private def loadHost(mem: MemoryManager.Memory, offset: Long, data: ByteBuffer): MemoryManager.Slice = {
//     val ptr = vk.mapMemory(d, hostFastAccess.memory, new Vulkan.DeviceSize(offset), new Vulkan.DeviceSize(data.capacity), 0)
//     vk.loadMemory(ptr, data)
//     if(!mem.flags.hasBitFlags(Vulkan.MEMORY_PROPERTY_HOST_COHERENT_BIT)) {
//       vk.flushMappedMemoryRanges(d, Array(
//         new Vulkan.MappedMemoryRange(mem.memory, new Vulkan.DeviceSize(offset), new Vulkan.DeviceSize(data.capacity))))
//     }
//     vk.unmapMemory(d, mem.memory)
//     MemoryManager.Slice(mem.buffer, Interval.closed(offset, data.capacity), mem.purpose)
//   }

//   def putDeviceFastAccess(data: ByteBuffer, alignment: Option[Long]): Either[MemoryManager.MemoryManagerError, (MemoryManager, MemoryManager.Slice)] = {
//     for {
//       so <- findSlot(staging, alignment, data.capacity)
//       (s, offset) = so
//       dO <- findSlot(deviceFastAccess, alignment, data.capacity)
//       (ds, dOffset) = dO
//     } yield {
//       val hostSlice = loadHost(staging, offset, data)
//       vk.cmdCopyBuffer(cmd, staging.buffer.buffer, deviceFastAccess.buffer.buffer, Array(new Vulkan.BufferCopy(
//         srcOffset = new Vulkan.DeviceSize(offset),
//         dstOffset = new Vulkan.DeviceSize(dOffset),
//         size = new Vulkan.DeviceSize(data.capacity))))
//       val deviceSlice = MemoryManager.Slice(deviceFastAccess.buffer, Interval.closed(dOffset, data.capacity.toLong), deviceFastAccess.purpose)
//       val next = ((MemoryManager._staging ^|-> MemoryManager._slots).modify(ss => ss.filterNot(_ == s) ::: (s -- hostSlice.interval)) compose
//         (MemoryManager._deviceFastAccess ^|-> MemoryManager._slots).modify(ss => ss.filterNot(_ == ds) ::: (ds -- deviceSlice.interval)))(this)
//       (next, deviceSlice)
//     }
//   }

//   def putHostFastAccess(data: ByteBuffer, alignment: Option[Long]): Either[MemoryManager.MemoryManagerError, (MemoryManager, MemoryManager.Slice)] = {
//     findSlot(hostFastAccess, alignment, data.capacity).map { case (s, offset) =>
//       val slice = loadHost(hostFastAccess, offset, data)
//       val next = (MemoryManager._hostFastAccess ^|-> MemoryManager._slots).modify(ss => ss.filterNot(_ == s) ::: (s -- slice.interval))(this)
//       (next, slice)
//     }
//   }

//   //we are binding over the device local buffer memory - perhaps it would be good to section off a piece just for images?
//   def putImageOptimal(data: ByteBuffer, target: Vulkan.Image, width: Int, height: Int): Either[MemoryManager.MemoryManagerError, (MemoryManager, MemoryManager.Slice)] = {
//     for {
//       sof <- findSlot(staging, None, data.capacity)
//       (s, offset) = sof
//       dof <- findSlot(deviceFastAccess, None, data.capacity)
//       (ds, dOffset) = dof
//     } yield {
//       val hostSlice = loadHost(hostFastAccess, offset, data)
//       vk.bindImageMemory(d, target, deviceFastAccess.memory, new Vulkan.DeviceSize(dOffset))
//       vk.cmdCopyBufferToImage(cmd, staging.buffer.buffer, target, Vulkan.IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, Array(new Vulkan.BufferImageCopy(
//         bufferOffset = new Vulkan.DeviceSize(offset), bufferRowLength = width, bufferImageHeight = height,
//         imageSubresource = new Vulkan.ImageSubresourceLayers(
//           aspectMask = Vulkan.IMAGE_ASPECT_COLOR_BIT,
//           mipLevel = 0,
//           baseArrayLayer = 0,
//           layerCount = 1),
//         imageOffset = new Vulkan.Offset3D(0, 0, 0), imageExtent = new Vulkan.Extent3D(width, height, 1)
//       )))

// ???
//     }
//   }

//   def submit(): Unit = {
//     vk.queueSubmit(queue, 1, Array(
//       new Vulkan.SubmitInfo(
//         waitSemaphores = Array.empty,
//         waitDstStageMask = Array.empty,
//         commandBuffers = Array(cmd),
//         signalSemaphores = Array(semaphore))
//     ), new Vulkan.Fence(0))
//   }

//   private def join[A](target: Interval[A], is: List[Interval[A]]): List[Interval[A]] = {
//     is.foldLeft((List.empty[Interval[A]], target)) { (tup, i) => tup match {
//       case (acc, t) => if((i intersect t).isPoint) (acc, i union t) else (i :: acc, t)
//     }} match { case (is, i) => i :: is }
//   }

//   def free(slice: MemoryManager.Slice): MemoryManager = slice.purpose match {
//     case MemoryManager.HostFastAccess =>
//       (MemoryManager._hostFastAccess ^|-> MemoryManager._slots).modify{ ss => join(slice.interval, ss) }(this)
//     case MemoryManager.DeviceFastAccess =>
//       (MemoryManager._deviceFastAccess ^|-> MemoryManager._slots).modify{ ss => join(slice.interval, ss) }(this)
//     case MemoryManager.Staging => sys.error("Impossible case! Staging memory is internal to the memory manager")
//   }
//   def freeAll(): Unit = {
//     List(hostFastAccess, deviceFastAccess, staging).foreach { mem =>
//       vk.freeMemory(d, mem.memory)
//       vk.destroyBuffer(d, mem.buffer.buffer)
//     }
//   }
// }
