package hephaestus
package vulkan

import hephaestus.platform.Vulkan
import hephaestus.platform.Vulkan.{
  MemoryHeap => VkMemoryHeap,
  MemoryType => VkMemoryType,
  MemoryRequirements => VkMemoryRequirements,
  DeviceMemory => VkDeviceMemory
}

import cats._, cats.data._, cats.implicits._
import monocle._, monocle.macros._, monocle.syntax._, monocle.function.all._, monocle.std.map._
import spire._, spire.implicits._, spire.math._

/** Enriched form of [[hephaestus.platform.Vulkan.MemoryType]].
  *
  * @param tpe the underlying memory type
  * @param index the index of the memory type
  * @param heap the corresponding memory heap
  */
case class MemoryType(tpe: VkMemoryType,
                      index: Int,
                      heap: VkMemoryHeap)

/** Slice of a [[hephaestus.platform.Vulkan.DeviceMemory]].
  * @param memory the underlying memory
  * @param tpe the memory type corresponding to the memory
  * @param offset the byte offset into the memory
  */
case class DeviceMemory(memory: VkDeviceMemory, tpe: MemoryType, offset: Long)

/** Controls the allocation and freeing of memory.
  * 
  * This is a basic, inefficient implementation which allocates a new block every time one is asked for.
  * It can be improved by allocating large blocks, and returning subsections of those blocks.
  * 
  * @param d the device
  * @param heaps map of heaps to available space in each heap
  * @param tpes list of all memory types
  * @param memories list of all created memory blocks
  */
case class Suballocator(device: Device, heaps: Map[VkMemoryHeap, Long], tpes: List[MemoryType], memories: List[DeviceMemory])

object Suballocator {

  val _heaps: Lens[Suballocator, Map[VkMemoryHeap, Long]] = GenLens[Suballocator](_.heaps)
  val _tpes: Lens[Suballocator, List[MemoryType]] = GenLens[Suballocator](_.tpes)
  val _memories: Lens[Suballocator, List[DeviceMemory]] = GenLens[Suballocator](_.memories)


  def apply(d: Device): Suballocator =
    Suballocator(
      device = d,
      heaps = d.physicalDevice.memoryProperties.memoryHeaps.map(h => (h, h.size)).toMap,
      tpes = d.physicalDevice.memoryProperties.memoryTypes.zipWithIndex.map { 
        case (t, i) => MemoryType(t, i, d.physicalDevice.memoryProperties.memoryHeaps(t.heapIndex)) }.toList,
      Nil)

  /** Selects a memory type matching desired flags and requirements.  Takes [[size]] available space out of the type's heap. **/
  def alloc(flags: List[Int], reqs: VkMemoryRequirements, size: Long): StateT[Either[MemoryError, ?], Suballocator, DeviceMemory]  = {

    def findType(flags: Int)(tpes: List[MemoryType], heaps: Map[VkMemoryHeap, Long]): Either[MemoryError, MemoryType] = {
      for {
        masked <- {
          val masked = tpes.mask(reqs.memoryTypeBits)
          Either.cond(masked.nonEmpty,
            masked,
            NoMatchForMask(reqs.memoryTypeBits, tpes))
        }
        matched <- {
          val matched = masked.filter(_.tpe.propertyFlags.hasBitFlags(flags))
          Either.cond(matched.nonEmpty,
            matched,
            NoMatchForProperties(flags, masked))
        }
        target <- Either.fromOption(matched.find(t => heaps(t.heap) >= size),
          NoHeapLargeEnough(size, matched, heaps))
      } yield target
    }

    @annotation.tailrec
    def go(flags: List[Int], errs: List[MemoryError])(tpes: List[MemoryType], heaps: Map[VkMemoryHeap, Long])
        : Either[MemoryError, MemoryType] = flags match {
      case h :: t =>
        findType(h)(tpes, heaps) match {
          case Right(tpe) => Right(tpe)
          case Left(err: NoMatchForMask) => Left(err)
          case Left(otherErr) => go(t, otherErr :: errs)(tpes, heaps)
        }
      case Nil => Left(NoFitForProperties(flags, tpes, errs))
    }
    StateT(s => go(flags, Nil)(s.tpes, s.heaps).map { t =>
      val m = DeviceMemory(Device.allocateMemory(new Vulkan.MemoryAllocateInfo(size, t.index)).run(s.device), t, 0)
      val s0 = ((_heaps ^|-> at(t.heap)).modify(ho => Some(ho.get - size)) andThen _memories.modify(ms => m :: ms))(s)
      (s0, m)
    })
  }

  def free: State[Suballocator, Unit] = State { s =>
    s.memories.traverse(m => Device.freeMemory(m.memory)).run(s.device)
    (_memories.set(Nil)(s), ())
  }
}

abstract class MemoryError(val message: String) extends Exception(message)

case class NoMatchForMask(mask: Int, tpes: List[MemoryType])
    extends MemoryError(s"NoMatchForMask: bitmaks [$mask] hides all memory types in [$tpes]")
case class NoMatchForProperties(props: Int, tpes: List[MemoryType])
    extends MemoryError(s"NoMatchForProperties: properties [$props] are not present in memory types [$tpes]")
case class NoHeapLargeEnough(size: Long, tpes: List[MemoryType], heaps: Map[VkMemoryHeap, Long])
    extends MemoryError(s"NoHeapLargeEnough: no heap large enough to fit size [$size] in memory types [$tpes] in memory heaps [$heaps]")
case class NoFitForProperties(props: List[Int],
                              tpes: List[MemoryType],
                              errors: List[MemoryError])
    extends MemoryError(
    s"""NoFitForProperties: none of the properties [$props] are satisfied by memory types [$tpes].  Properties have errors: ${errors.map(_.getMessage).mkString(System.lineSeparator, System.lineSeparator, System.lineSeparator)}""")
