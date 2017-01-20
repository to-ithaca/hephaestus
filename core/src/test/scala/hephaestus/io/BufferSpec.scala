package hephaestus
package io

import org.scalatest._

class BufferSpec extends FlatSpec {
  "A Buffer" should "increment buffer pointer when getting elements" in {
    val b = Buffer(1, 2, 3)
    assert(b.head == 1)
    assert(b.head == 2)
    assert(b.head == 3)
  }

  it should "get an arbitrary element" in {
    val b = Buffer(1, 2, 3)
    assert(b(1) == 2)
  }

  it should "put an element" in {
    val b = Buffer.empty[Int](1)
    b.put(32)
    assert(b(0) == 32)
  }

  it should "get the length in elements" in {
    val b = Buffer(1, 2)
    assert(b.size == 2)
  }

  it should "be direct and native ordered" in {
    val b = Buffer.direct(1, 2)
    assert(b.direct == true)
    assert(b.order == java.nio.ByteOrder.nativeOrder())
  }
}
