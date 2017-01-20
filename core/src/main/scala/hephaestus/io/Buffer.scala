package hephaestus
package io

import java.nio.ByteBuffer
import java.{nio => jio}

import sun.nio.ch.DirectBuffer

final class Buffer[A](val value: jio.ByteBuffer) extends AnyVal { self =>

  def apply(n: Int)(implicit read: ops.buffer.Apply[A], size: ops.buffer.Bytes[A]): A = read(value, n * size.N)

  def size(implicit size: ops.buffer.Bytes[A]): Int = value.capacity() / size.N

  def position(n: Int)(implicit size: ops.buffer.Bytes[A]): Buffer[A] = {
    value.position(n / size.N)
    self
  }

  def direct: Boolean = value.isDirect

  def position(implicit bytes: ops.buffer.Bytes[A]): Int = value.position() / bytes.N

  def reset(): Buffer[A] = {
    value.reset()
    self
  }

  def rewind(): Buffer[A] = {
    value.rewind()
    self
  }

  def mark(): Buffer[A] = {
    value.mark()
    self
  }

  def head(implicit read: ops.buffer.Read[A]): A = read(value)

  def put(a: A)(implicit put: ops.buffer.Put[A]): Buffer[A] = {
    put(value, a)
    self
  }

   def free(): Unit = {
    if (direct) {
      val db = value.asInstanceOf[DirectBuffer]
      db.cleaner().clean()
    }
  }
}

object Buffer {

  def apply[A](a: A*)(implicit bytes: ops.buffer.Bytes[A], put: ops.buffer.Put[A]): Buffer[A] = {
    val b = empty[A](a.size)
    a.foreach(b.put)
    b.rewind()
    b
  }

  def direct[A](a: A*)(implicit bytes: ops.buffer.Bytes[A], put: ops.buffer.Put[A]): Buffer[A] = {
    val b = emptyDirect[A](a.size)
    a.foreach(b.put)
    b.rewind()
    b
  }

  def empty[A](n: Int)(implicit bytes: ops.buffer.Bytes[A]): Buffer[A] = new Buffer[A](ByteBuffer.allocate(n * bytes.N))

  def emptyDirect[A](n: Int)(implicit size: ops.buffer.Bytes[A]): Buffer[A] = new Buffer(ByteBuffer.allocateDirect(n * size.N))
}
