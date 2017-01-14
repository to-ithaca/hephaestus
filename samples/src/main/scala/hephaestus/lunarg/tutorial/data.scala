package hephaestus
package lunarg
package tutorial

import java.nio.{ByteBuffer, ByteOrder}
import com.hackoeur.jglm._
import com.hackoeur.jglm.support._

object Cube {

  case class Vec4(x0: Float, x1: Float, x2: Float, x3: Float)
  case class Vertex(position: Vec4, color: Vec4)

  def XYZ1(x: Float, y: Float, z: Float): Vec4 = Vec4(x, y, z, 1f)
  def XYZ1(x: Int, y: Int, z: Int): Vec4 = Vec4(x.toFloat, y.toFloat, z.toFloat, 1f)

  def toArray(vs: List[Vertex]): Array[Float] = {
    vs.flatMap { v =>
      List(
        v.position.x0, v.position.x1, v.position.x2, v.position.x3,
        v.color.x0, v.color.x1, v.color.x2, v.color.x3)
    }.toArray
  }
  def buffer(vs: List[Vertex]): ByteBuffer = {
    val arr = toArray(vs)
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

  val solidFaceColorsData: ByteBuffer = buffer(solidFaceColors)

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
}
