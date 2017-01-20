package hephaestus
package io
package ops

import java.nio.ByteBuffer

import scala.{specialized => sp}

object buffer {

  trait Read[@sp(Int, Float, Byte, Char) A] {
    def apply(b: ByteBuffer): A
  }

  object Read {
    implicit val intRead: Read[Int] = b => b.getInt()
    implicit val floatRead: Read[Float] = b => b.getFloat()
    implicit val byteRead: Read[Byte] = b => b.get()
    implicit val charRead: Read[Char] = b => b.getChar()
  }

  trait Apply[@sp(Int, Float, Byte, Char) A] {
    def apply(buf: ByteBuffer, n: Int): A
  }

  object Apply {
    implicit val intApply: Apply[Int] = (b, i) => b.getInt(i)
    implicit val floatApply: Apply[Float] = (b, i) => b.getFloat(i)
    implicit val byteApply: Apply[Byte] = (b, i) => b.get(i)
    implicit val charApply: Apply[Char] = (b, i) => b.getChar(i)
  }

  trait Bytes[A] {
    val N: Int
  }

  object Bytes {
    implicit val intSize: Bytes[Int] = new Bytes[Int] {
      val N = 4
    }
    implicit val floatSize: Bytes[Float] = new Bytes[Float] {
      val N = 4
    }
    implicit val charSize: Bytes[Char] = new Bytes[Char] {
      val N = 4
    }
    implicit val byteSize: Bytes[Byte] = new Bytes[Byte] {
      val N = 1
    }
  }

  trait Put[@sp(Int, Float, Byte, Char) A] {
    def apply(b: ByteBuffer, a: A): Unit
  }

  object Put {
    implicit val intPut: Put[Int] = (b, i) => b.putInt(i)
    implicit val floatPut: Put[Float] = (b, f) => b.putFloat(f)
    implicit val charPut: Put[Char] = (b, c) => b.putChar(c)
    implicit val bytePut: Put[Byte] = (b, a) => b.put(a)
  }

}
