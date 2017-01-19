package hephaestus
package lunarg
package tutorial

import java.nio.{ByteBuffer, ByteOrder}
import com.hackoeur.jglm._
import com.hackoeur.jglm.support._

object Cube {

  case class Vec4(x0: Float, x1: Float, x2: Float, x3: Float)
  case class Vec2(x0: Float, x1: Float)
  case class Vertex(position: Vec4, color: Vec4)
  case class UVVertex(position: Vec4, uv: Vec2)
  case class Texel(r: Float, g: Float, b: Float, a: Float)

  def XYZ1(x: Float, y: Float, z: Float): Vec4 = Vec4(x, y, z, 1f)
  def XYZ1(x: Int, y: Int, z: Int): Vec4 = Vec4(x.toFloat, y.toFloat, z.toFloat, 1f)
  def UV(u: Float, v: Float): Vec2 = Vec2(u, v)

  def toArray(vs: List[Vertex]): Array[Float] = {
    vs.flatMap { v =>
      List(
        v.position.x0, v.position.x1, v.position.x2, v.position.x3,
        v.color.x0, v.color.x1, v.color.x2, v.color.x3)
    }.toArray
  }
  def toArrayUVs(vs: List[UVVertex]): Array[Float] = {
    vs.flatMap { v =>
      List(
        v.position.x0, v.position.x1, v.position.x2, v.position.x3,
        v.uv.x0, v.uv.x1)
    }.toArray
  }
  def buffer(arr: Array[Float]): ByteBuffer = {
    val b = ByteBuffer.allocateDirect(arr.size * 4).order(ByteOrder.nativeOrder())
    b.asFloatBuffer().put(arr, 0, arr.size)
    b
  }

  val solidFaceColors: List[Vertex] = List(
    //redface
    Vertex(XYZ1(-1,-1, 1), XYZ1(1f, 0f, 0f)),
    Vertex(XYZ1(-1, 1, 1), XYZ1(1f, 0f, 0f)),
    Vertex(XYZ1( 1,-1, 1), XYZ1(1f, 0f, 0f)),
    Vertex(XYZ1( 1,-1, 1), XYZ1(1f, 0f, 0f)),
    Vertex(XYZ1(-1, 1, 1), XYZ1(1f, 0f, 0f)),
    Vertex(XYZ1( 1, 1, 1), XYZ1(1f, 0f, 0f)),

    //greenface
    Vertex(XYZ1(-1,-1,-1), XYZ1(0f, 1f, 0f)),
    Vertex(XYZ1( 1,-1,-1), XYZ1(0f, 1f, 0f)),
    Vertex(XYZ1(-1, 1,-1), XYZ1(0f, 1f, 0f)),
    Vertex(XYZ1(-1, 1,-1), XYZ1(0f, 1f, 0f)),
    Vertex(XYZ1( 1,-1,-1), XYZ1(0f, 1f, 0f)),
    Vertex(XYZ1( 1, 1,-1), XYZ1(0f, 1f, 0f)),
    //blueface
    Vertex(XYZ1(-1, 1, 1), XYZ1(0f, 0f, 1f)),
    Vertex(XYZ1(-1,-1, 1), XYZ1(0f, 0f, 1f)),
    Vertex(XYZ1(-1, 1,-1), XYZ1(0f, 0f, 1f)),
    Vertex(XYZ1(-1, 1,-1), XYZ1(0f, 0f, 1f)),
    Vertex(XYZ1(-1,-1, 1), XYZ1(0f, 0f, 1f)),
    Vertex(XYZ1(-1,-1,-1), XYZ1(0f, 0f, 1f)),
    //yellowface
    Vertex(XYZ1( 1, 1, 1), XYZ1(1f, 1f, 0f)),
    Vertex(XYZ1( 1, 1,-1), XYZ1(1f, 1f, 0f)),
    Vertex(XYZ1( 1,-1, 1), XYZ1(1f, 1f, 0f)),
    Vertex(XYZ1( 1,-1, 1), XYZ1(1f, 1f, 0f)),
    Vertex(XYZ1( 1, 1,-1), XYZ1(1f, 1f, 0f)),
    Vertex(XYZ1( 1,-1,-1), XYZ1(1f, 1f, 0f)),
    //magentaface
    Vertex(XYZ1( 1, 1, 1), XYZ1(1f, 0f, 1f)),
    Vertex(XYZ1(-1, 1, 1), XYZ1(1f, 0f, 1f)),
    Vertex(XYZ1( 1, 1,-1), XYZ1(1f, 0f, 1f)),
    Vertex(XYZ1( 1, 1,-1), XYZ1(1f, 0f, 1f)),
    Vertex(XYZ1(-1, 1, 1), XYZ1(1f, 0f, 1f)),
    Vertex(XYZ1(-1, 1,-1), XYZ1(1f, 0f, 1f)),
    //cyanface
    Vertex(XYZ1( 1,-1, 1), XYZ1(0f, 1f, 1f)),
    Vertex(XYZ1( 1,-1,-1), XYZ1(0f, 1f, 1f)),
    Vertex(XYZ1(-1,-1, 1), XYZ1(0f, 1f, 1f)),
    Vertex(XYZ1(-1,-1, 1), XYZ1(0f, 1f, 1f)),
    Vertex(XYZ1( 1,-1,-1), XYZ1(0f, 1f, 1f)),
    Vertex(XYZ1(-1,-1,-1), XYZ1(0f, 1f, 1f))
  )

  val solidFaceColorsData: ByteBuffer = buffer(toArray(solidFaceColors))

  def uniformData(width: Int, height: Int): ByteBuffer = {
    val aspect = if(width > height) height.toFloat / width.toFloat else 1f
    val fov = aspect * 45.0f
    val projection = Matrices.perspective(fov, width.toFloat / height.toFloat, 0.1f, 100.0f)
    val view = Matrices.lookAt(
      new Vec3(-5f, 3f, -10f),
      new Vec3(0f, 0f, 0f),
      new Vec3(0f, -1f, 0f))
    val model = Mat4.MAT4_IDENTITY
    val clip = new Mat4(
      1.0f, 0.0f, 0.0f, 0.0f,
      0.0f, -1.0f, 0.0f, 0.0f,
      0.0f, 0.0f, 0.5f, 0.5f,
      0.0f, 0.0f, 0.0f, 1.0f).transpose
    val mvp =  clip.multiply(projection).multiply(view).multiply(model)

    val fbuf = mvp.getBuffer()
    val fs = new Array[Float](fbuf.capacity())
    fbuf.get(fs)
    val buf = ByteBuffer.allocateDirect(fs.size * 4).order(ByteOrder.nativeOrder())
    buf.asFloatBuffer().put(fs, 0, fs.size)
    buf
  }

  def uniformData(width: Int, height: Int, frame: Int): ByteBuffer = {
    val angle = (frame % 1000).toDouble / 1000.0
    val radius = math.sqrt(34.0)
    val eyeX = radius * math.cos(angle * 2.0 * math.Pi)
    val eyeY = radius * math.sin(angle * 2.0 * math.Pi)

    val aspect = if(width > height) height.toFloat / width.toFloat else 1f
    val fov = aspect * 45.0f
    val projection = Matrices.perspective(fov, width.toFloat / height.toFloat, 0.1f, 100.0f)
    val view = Matrices.lookAt(
      new Vec3(eyeX.toFloat, eyeY.toFloat, -10f),
      new Vec3(0f, 0f, 0f),
      new Vec3(0f, -1f, 0f))
    val model = Mat4.MAT4_IDENTITY
    val clip = new Mat4(
      1.0f, 0.0f, 0.0f, 0.0f,
      0.0f, -1.0f, 0.0f, 0.0f,
      0.0f, 0.0f, 0.5f, 0.5f,
      0.0f, 0.0f, 0.0f, 1.0f).transpose
    val mvp =  clip.multiply(projection).multiply(view).multiply(model)

    val fbuf = mvp.getBuffer()
    val fs = new Array[Float](fbuf.capacity())
    fbuf.get(fs)
    val buf = ByteBuffer.allocateDirect(fs.size * 4).order(ByteOrder.nativeOrder())
    buf.asFloatBuffer().put(fs, 0, fs.size)
    buf
  }

  def toArray(ts: List[Texel]): Array[Byte] = ts.flatMap(t => List(
    (t.r * 255).toByte,
    (t.g * 255).toByte,
    (t.b * 255).toByte,
    (t.a * 255).toByte
  )).toArray


  def textureData(width: Int, height: Int, frame: Int): ByteBuffer = {
    val b = (frame % 1000).toDouble / 1000.0
    val texels = for {
      x <- 0 until width
      y <- 0 until height
    } yield {
      val r = (width - x).toFloat / width.toFloat
      val g = (height - y).toFloat / width.toFloat
      Texel(r, g, b.toFloat, 1f)
    }
    val bytes = toArray(texels.toList)
    val buf = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder())
    buf.put(bytes, 0, bytes.size)
    buf
  }
  def textureData(width: Int, height: Int): ByteBuffer = {
    val texels = for {
      x <- 0 until width
      y <- 0 until height
    } yield {
      val r = (width - x).toFloat / width.toFloat
      val g = (height - y).toFloat / width.toFloat
      Texel(r, g, 1f, 1f)
    }
    val bytes = toArray(texels.toList)
    val buf = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder())
    buf.put(bytes, 0, bytes.size)
    buf
  }
  def solidFaceUvs = List(
    //left face
    UVVertex(XYZ1(-1,-1,-1), UV(1f, 0f)),  // lft-top-front
    UVVertex(XYZ1(-1, 1, 1), UV(0f, 1f)),  // lft-btm-back
    UVVertex(XYZ1(-1,-1, 1), UV(0f, 0f)),  // lft-top-back
    UVVertex(XYZ1(-1, 1, 1), UV(0f, 1f)),  // lft-btm-back
    UVVertex(XYZ1(-1,-1,-1), UV(1f, 0f)),  // lft-top-front
    UVVertex(XYZ1(-1, 1,-1), UV(1f, 1f)),  // lft-btm-front
    //front face
    UVVertex(XYZ1(-1,-1,-1), UV(0f, 0f)),  // lft-top-front
    UVVertex(XYZ1( 1,-1,-1), UV(1f, 0f)),  // rgt-top-front
    UVVertex(XYZ1( 1, 1,-1), UV(1f, 1f)),  // rgt-btm-front
    UVVertex(XYZ1(-1,-1,-1), UV(0f, 0f)),  // lft-top-front
    UVVertex(XYZ1( 1, 1,-1), UV(1f, 1f)),  // rgt-btm-front
    UVVertex(XYZ1(-1, 1,-1), UV(0f, 1f)),  // lft-btm-front
    //top face
    UVVertex(XYZ1(-1,-1,-1), UV(0f, 1f)),  // lft-top-front
    UVVertex(XYZ1( 1,-1, 1), UV(1f, 0f)),  // rgt-top-back
    UVVertex(XYZ1( 1,-1,-1), UV(1f, 1f)),  // rgt-top-front
    UVVertex(XYZ1(-1,-1,-1), UV(0f, 1f)),  // lft-top-front
    UVVertex(XYZ1(-1,-1, 1), UV(0f, 0f)),  // lft-top-back
    UVVertex(XYZ1( 1,-1, 1), UV(1f, 0f)),  // rgt-top-back
    //bottom face
    UVVertex(XYZ1(-1, 1,-1), UV(0f, 0f)),  // lft-btm-front
    UVVertex(XYZ1( 1, 1, 1), UV(1f, 1f)),  // rgt-btm-back
    UVVertex(XYZ1(-1, 1, 1), UV(0f, 1f)),  // lft-btm-back
    UVVertex(XYZ1(-1, 1,-1), UV(0f, 0f)),  // lft-btm-front
    UVVertex(XYZ1( 1, 1,-1), UV(1f, 0f)),  // rgt-btm-front
    UVVertex(XYZ1( 1, 1, 1), UV(1f, 1f)),  // rgt-btm-back
    //right face
    UVVertex(XYZ1( 1, 1,-1), UV(0f, 1f)),  // rgt-btm-front
    UVVertex(XYZ1( 1,-1, 1), UV(1f, 0f)),  // rgt-top-back
    UVVertex(XYZ1( 1, 1, 1), UV(1f, 1f)),  // rgt-btm-back
    UVVertex(XYZ1( 1,-1, 1), UV(1f, 0f)),  // rgt-top-back
    UVVertex(XYZ1( 1, 1,-1), UV(0f, 1f)),  // rgt-btm-front
    UVVertex(XYZ1( 1,-1,-1), UV(0f, 0f)),  // rgt-top-front
    //back face
    UVVertex(XYZ1(-1, 1, 1), UV(1f, 1f)),  // lft-btm-back
    UVVertex(XYZ1( 1, 1, 1), UV(0f, 1f)),  // rgt-btm-back
    UVVertex(XYZ1(-1,-1, 1), UV(1f, 0f)),  // lft-top-back
    UVVertex(XYZ1(-1,-1, 1), UV(1f, 0f)),  // lft-top-back
    UVVertex(XYZ1( 1, 1, 1), UV(0f, 1f)),  // rgt-btm-back
    UVVertex(XYZ1( 1,-1, 1), UV(0f, 0f))  // rgt-top-back
  )
  val solidFaceUvsData: ByteBuffer = buffer(toArrayUVs(solidFaceUvs))
}
