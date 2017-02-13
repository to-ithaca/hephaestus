package hephaestus
package lunarg
package tutorial

import hephaestus.io.Buffer
import java.nio.{ByteBuffer, ByteOrder}
import com.hackoeur.jglm._
import com.hackoeur.jglm.support._

object Cube {

  private case class Vec4(x0: Float, x1: Float, x2: Float, x3: Float) {
    def list: List[Float] = List(x0, x1, x2, x3)
  }
  private case class Vec2(x0: Float, x1: Float) {
    def list: List[Float] = List(x0, x1)
  }
  private case class Vertex(position: Vec4, color: Vec4) {
    def list: List[Float] = position.list ::: color.list
  }
  private case class UVVertex(position: Vec4, uv: Vec2) {
    def list: List[Float] = position.list ::: uv.list
  }
  private case class Texel(r: Float, g: Float, b: Float, a: Float) {
    def list: List[Byte] = List(r, g, b, a).map(v => (v * 255).toByte)
  }

  private def XYZ1(x: Float, y: Float, z: Float): Vec4 = Vec4(x, y, z, 1f)
  private def XYZ1(x: Int, y: Int, z: Int): Vec4 =
    Vec4(x.toFloat, y.toFloat, z.toFloat, 1f)
  private def UV(u: Float, v: Float): Vec2 = Vec2(u, v)

  private val solidFaceColors: List[Vertex] = List(
    //redface
    Vertex(XYZ1(-1, -1, 1), XYZ1(1f, 0f, 0f)),
    Vertex(XYZ1(-1, 1, 1), XYZ1(1f, 0f, 0f)),
    Vertex(XYZ1(1, -1, 1), XYZ1(1f, 0f, 0f)),
    Vertex(XYZ1(1, -1, 1), XYZ1(1f, 0f, 0f)),
    Vertex(XYZ1(-1, 1, 1), XYZ1(1f, 0f, 0f)),
    Vertex(XYZ1(1, 1, 1), XYZ1(1f, 0f, 0f)),
    //greenface
    Vertex(XYZ1(-1, -1, -1), XYZ1(0f, 1f, 0f)),
    Vertex(XYZ1(1, -1, -1), XYZ1(0f, 1f, 0f)),
    Vertex(XYZ1(-1, 1, -1), XYZ1(0f, 1f, 0f)),
    Vertex(XYZ1(-1, 1, -1), XYZ1(0f, 1f, 0f)),
    Vertex(XYZ1(1, -1, -1), XYZ1(0f, 1f, 0f)),
    Vertex(XYZ1(1, 1, -1), XYZ1(0f, 1f, 0f)),
    //blueface
    Vertex(XYZ1(-1, 1, 1), XYZ1(0f, 0f, 1f)),
    Vertex(XYZ1(-1, -1, 1), XYZ1(0f, 0f, 1f)),
    Vertex(XYZ1(-1, 1, -1), XYZ1(0f, 0f, 1f)),
    Vertex(XYZ1(-1, 1, -1), XYZ1(0f, 0f, 1f)),
    Vertex(XYZ1(-1, -1, 1), XYZ1(0f, 0f, 1f)),
    Vertex(XYZ1(-1, -1, -1), XYZ1(0f, 0f, 1f)),
    //yellowface
    Vertex(XYZ1(1, 1, 1), XYZ1(1f, 1f, 0f)),
    Vertex(XYZ1(1, 1, -1), XYZ1(1f, 1f, 0f)),
    Vertex(XYZ1(1, -1, 1), XYZ1(1f, 1f, 0f)),
    Vertex(XYZ1(1, -1, 1), XYZ1(1f, 1f, 0f)),
    Vertex(XYZ1(1, 1, -1), XYZ1(1f, 1f, 0f)),
    Vertex(XYZ1(1, -1, -1), XYZ1(1f, 1f, 0f)),
    //magentaface
    Vertex(XYZ1(1, 1, 1), XYZ1(1f, 0f, 1f)),
    Vertex(XYZ1(-1, 1, 1), XYZ1(1f, 0f, 1f)),
    Vertex(XYZ1(1, 1, -1), XYZ1(1f, 0f, 1f)),
    Vertex(XYZ1(1, 1, -1), XYZ1(1f, 0f, 1f)),
    Vertex(XYZ1(-1, 1, 1), XYZ1(1f, 0f, 1f)),
    Vertex(XYZ1(-1, 1, -1), XYZ1(1f, 0f, 1f)),
    //cyanface
    Vertex(XYZ1(1, -1, 1), XYZ1(0f, 1f, 1f)),
    Vertex(XYZ1(1, -1, -1), XYZ1(0f, 1f, 1f)),
    Vertex(XYZ1(-1, -1, 1), XYZ1(0f, 1f, 1f)),
    Vertex(XYZ1(-1, -1, 1), XYZ1(0f, 1f, 1f)),
    Vertex(XYZ1(1, -1, -1), XYZ1(0f, 1f, 1f)),
    Vertex(XYZ1(-1, -1, -1), XYZ1(0f, 1f, 1f))
  )

  val solidFaceColorsData: ByteBuffer =
    Buffer.direct(solidFaceColors.flatMap(_.list): _*).value

  def uniformData(width: Int, height: Int): ByteBuffer = {
    val aspect = if (width > height) height.toFloat / width.toFloat else 1f
    val fov = aspect * 45.0f
    val projection =
      Matrices.perspective(fov, width.toFloat / height.toFloat, 0.1f, 100.0f)
    val view = Matrices.lookAt(new Vec3(-5f, 3f, -10f),
                               new Vec3(0f, 0f, 0f),
                               new Vec3(0f, -1f, 0f))
    val model = Mat4.MAT4_IDENTITY
    val clip = new Mat4(1.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, 0.0f,
      0.0f, 0.5f, 0.5f, 0.0f, 0.0f, 0.0f, 1.0f).transpose
    val mvp = clip.multiply(projection).multiply(view).multiply(model)

    val fbuf = mvp.getBuffer()
    val fs = new Array[Float](fbuf.capacity())
    fbuf.get(fs)
    Buffer.direct(fs: _*).value
  }

  def uniformData(width: Int, height: Int, frame: Int): ByteBuffer = {
    val angle = (frame % 1000).toDouble / 1000.0
    val radius = math.sqrt(34.0)
    val eyeX = radius * math.cos(angle * math.Pi)
    val eyeY = radius //* math.sin(angle * 2.0 * math.Pi)

    val aspect = if (width > height) height.toFloat / width.toFloat else 1f
    val fov = aspect * 45.0f
    val projection =
      Matrices.perspective(fov, width.toFloat / height.toFloat, 0.1f, 100.0f)
    val view = Matrices.lookAt(new Vec3(eyeX.toFloat, eyeY.toFloat, -10f),
                               new Vec3(0f, 0f, 0f),
                               new Vec3(0f, -1f, 0f))
    val model = Mat4.MAT4_IDENTITY
    val clip = new Mat4(1.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, 0.0f,
      0.0f, 0.5f, 0.5f, 0.0f, 0.0f, 0.0f, 1.0f).transpose
    val mvp = clip.multiply(projection).multiply(view).multiply(model)

    val fbuf = mvp.getBuffer()
    val fs = new Array[Float](fbuf.capacity())
    fbuf.get(fs)
    Buffer.direct(fs: _*).value
  }

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
    Buffer.direct(texels.flatMap(_.list): _*).value
  }

  private val solidFaceUvs = List(
    //left face
    UVVertex(XYZ1(-1, -1, -1), UV(1f, 0f)), // lft-top-front
    UVVertex(XYZ1(-1, 1, 1), UV(0f, 1f)), // lft-btm-back
    UVVertex(XYZ1(-1, -1, 1), UV(0f, 0f)), // lft-top-back
    UVVertex(XYZ1(-1, 1, 1), UV(0f, 1f)), // lft-btm-back
    UVVertex(XYZ1(-1, -1, -1), UV(1f, 0f)), // lft-top-front
    UVVertex(XYZ1(-1, 1, -1), UV(1f, 1f)), // lft-btm-front
    //front face
    UVVertex(XYZ1(-1, -1, -1), UV(0f, 0f)), // lft-top-front
    UVVertex(XYZ1(1, -1, -1), UV(1f, 0f)), // rgt-top-front
    UVVertex(XYZ1(1, 1, -1), UV(1f, 1f)), // rgt-btm-front
    UVVertex(XYZ1(-1, -1, -1), UV(0f, 0f)), // lft-top-front
    UVVertex(XYZ1(1, 1, -1), UV(1f, 1f)), // rgt-btm-front
    UVVertex(XYZ1(-1, 1, -1), UV(0f, 1f)), // lft-btm-front
    //top face
    UVVertex(XYZ1(-1, -1, -1), UV(0f, 1f)), // lft-top-front
    UVVertex(XYZ1(1, -1, 1), UV(1f, 0f)), // rgt-top-back
    UVVertex(XYZ1(1, -1, -1), UV(1f, 1f)), // rgt-top-front
    UVVertex(XYZ1(-1, -1, -1), UV(0f, 1f)), // lft-top-front
    UVVertex(XYZ1(-1, -1, 1), UV(0f, 0f)), // lft-top-back
    UVVertex(XYZ1(1, -1, 1), UV(1f, 0f)), // rgt-top-back
    //bottom face
    UVVertex(XYZ1(-1, 1, -1), UV(0f, 0f)), // lft-btm-front
    UVVertex(XYZ1(1, 1, 1), UV(1f, 1f)), // rgt-btm-back
    UVVertex(XYZ1(-1, 1, 1), UV(0f, 1f)), // lft-btm-back
    UVVertex(XYZ1(-1, 1, -1), UV(0f, 0f)), // lft-btm-front
    UVVertex(XYZ1(1, 1, -1), UV(1f, 0f)), // rgt-btm-front
    UVVertex(XYZ1(1, 1, 1), UV(1f, 1f)), // rgt-btm-back
    //right face
    UVVertex(XYZ1(1, 1, -1), UV(0f, 1f)), // rgt-btm-front
    UVVertex(XYZ1(1, -1, 1), UV(1f, 0f)), // rgt-top-back
    UVVertex(XYZ1(1, 1, 1), UV(1f, 1f)), // rgt-btm-back
    UVVertex(XYZ1(1, -1, 1), UV(1f, 0f)), // rgt-top-back
    UVVertex(XYZ1(1, 1, -1), UV(0f, 1f)), // rgt-btm-front
    UVVertex(XYZ1(1, -1, -1), UV(0f, 0f)), // rgt-top-front
    //back face
    UVVertex(XYZ1(-1, 1, 1), UV(1f, 1f)), // lft-btm-back
    UVVertex(XYZ1(1, 1, 1), UV(0f, 1f)), // rgt-btm-back
    UVVertex(XYZ1(-1, -1, 1), UV(1f, 0f)), // lft-top-back
    UVVertex(XYZ1(-1, -1, 1), UV(1f, 0f)), // lft-top-back
    UVVertex(XYZ1(1, 1, 1), UV(0f, 1f)), // rgt-btm-back
    UVVertex(XYZ1(1, -1, 1), UV(0f, 0f)) // rgt-top-back
  )
  val solidFaceUvsElementData: ByteBuffer =
    Buffer.direct((0 to 36).toList: _*).value
  val solidFaceUvsData: ByteBuffer =
    Buffer.direct(solidFaceUvs.flatMap(_.list): _*).value
}
