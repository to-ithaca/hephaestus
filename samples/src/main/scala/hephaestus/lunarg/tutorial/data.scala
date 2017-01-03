package hephaestus
package lunarg
package tutorial

import java.nio.{ByteBuffer, ByteOrder}

object Cube {

  case class Vec2(x0: Float, x1: Float)
  case class Vec4(x0: Float, x1: Float, x2: Float, x3: Float)
  case class Vertex(position: Vec4, color: Vec4)
  case class VertexUV(position: Vec4, uv: Vec2)

  def XYZ1(x: Float, y: Float, z: Float): Vec4 = Vec4(x, y, z, 1f)
  def XYZ1(x: Int, y: Int, z: Int): Vec4 = Vec4(x.toFloat, y.toFloat, z.toFloat, 1f)
  def UV(u: Float, v: Float): Vec2 = Vec2(u, v)

  def insert(b: ByteBuffer, v: Vec4): Unit = {
    b.putFloat(v.x0).putFloat(v.x1).putFloat(v.x2).putFloat(v.x3)
  }
  def buffer(vs: List[Vertex]): ByteBuffer = {
    val b = ByteBuffer.allocateDirect(vs.size * (2 * 4 * 4)).order(ByteOrder.nativeOrder()) //6 * 6 vertices
    vs.foreach { v => 
      insert(b, v.position)
      insert(b, v.color)
    }
    // b.rewind
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

  val matrix = Array(
    0.5f, 0f, 0f, 0f,
    0f, 0.5f, 0f, 0f,
    0f, 0f, 0.5f, 0f,
    0f, 0f, 0f, 1f)
 
  val uniformData: ByteBuffer = {
    val buf = ByteBuffer.allocateDirect(matrix.size * 4).order(ByteOrder.nativeOrder())
    buf.asFloatBuffer().put(matrix, 0, matrix.size)
    buf
  }
}


// static const Vertex g_vbData[] = {
//     {XYZ1(-1, -1, -1), XYZ1(0.f, 0.f, 0.f)},
//     {XYZ1(1, -1, -1), XYZ1(1.f, 0.f, 0.f)},
//     {XYZ1(-1, 1, -1), XYZ1(0.f, 1.f, 0.f)},
//     {XYZ1(-1, 1, -1), XYZ1(0.f, 1.f, 0.f)},
//     {XYZ1(1, -1, -1), XYZ1(1.f, 0.f, 0.f)},
//     {XYZ1(1, 1, -1), XYZ1(1.f, 1.f, 0.f)},

//     {XYZ1(-1, -1, 1), XYZ1(0.f, 0.f, 1.f)},
//     {XYZ1(-1, 1, 1), XYZ1(0.f, 1.f, 1.f)},
//     {XYZ1(1, -1, 1), XYZ1(1.f, 0.f, 1.f)},
//     {XYZ1(1, -1, 1), XYZ1(1.f, 0.f, 1.f)},
//     {XYZ1(-1, 1, 1), XYZ1(0.f, 1.f, 1.f)},
//     {XYZ1(1, 1, 1), XYZ1(1.f, 1.f, 1.f)},

//     {XYZ1(1, 1, 1), XYZ1(1.f, 1.f, 1.f)},
//     {XYZ1(1, 1, -1), XYZ1(1.f, 1.f, 0.f)},
//     {XYZ1(1, -1, 1), XYZ1(1.f, 0.f, 1.f)},
//     {XYZ1(1, -1, 1), XYZ1(1.f, 0.f, 1.f)},
//     {XYZ1(1, 1, -1), XYZ1(1.f, 1.f, 0.f)},
//     {XYZ1(1, -1, -1), XYZ1(1.f, 0.f, 0.f)},

//     {XYZ1(-1, 1, 1), XYZ1(0.f, 1.f, 1.f)},
//     {XYZ1(-1, -1, 1), XYZ1(0.f, 0.f, 1.f)},
//     {XYZ1(-1, 1, -1), XYZ1(0.f, 1.f, 0.f)},
//     {XYZ1(-1, 1, -1), XYZ1(0.f, 1.f, 0.f)},
//     {XYZ1(-1, -1, 1), XYZ1(0.f, 0.f, 1.f)},
//     {XYZ1(-1, -1, -1), XYZ1(0.f, 0.f, 0.f)},

//     {XYZ1(1, 1, 1), XYZ1(1.f, 1.f, 1.f)},
//     {XYZ1(-1, 1, 1), XYZ1(0.f, 1.f, 1.f)},
//     {XYZ1(1, 1, -1), XYZ1(1.f, 1.f, 0.f)},
//     {XYZ1(1, 1, -1), XYZ1(1.f, 1.f, 0.f)},
//     {XYZ1(-1, 1, 1), XYZ1(0.f, 1.f, 1.f)},
//     {XYZ1(-1, 1, -1), XYZ1(0.f, 1.f, 0.f)},

//     {XYZ1(1, -1, 1), XYZ1(1.f, 0.f, 1.f)},
//     {XYZ1(1, -1, -1), XYZ1(1.f, 0.f, 0.f)},
//     {XYZ1(-1, -1, 1), XYZ1(0.f, 0.f, 1.f)},
//     {XYZ1(-1, -1, 1), XYZ1(0.f, 0.f, 1.f)},
//     {XYZ1(1, -1, -1), XYZ1(1.f, 0.f, 0.f)},
//     {XYZ1(-1, -1, -1), XYZ1(0.f, 0.f, 0.f)},
// };


// static const VertexUV g_vb_texture_Data[] = {
//     //left face
//     {XYZ1(-1,-1,-1), UV(1.f, 0.f)},  // lft-top-front
//     {XYZ1(-1, 1, 1), UV(0.f, 1.f)},  // lft-btm-back
//     {XYZ1(-1,-1, 1), UV(0.f, 0.f)},  // lft-top-back
//     {XYZ1(-1, 1, 1), UV(0.f, 1.f)},  // lft-btm-back
//     {XYZ1(-1,-1,-1), UV(1.f, 0.f)},  // lft-top-front
//     {XYZ1(-1, 1,-1), UV(1.f, 1.f)},  // lft-btm-front
//     //front face
//     {XYZ1(-1,-1,-1), UV(0.f, 0.f)},  // lft-top-front
//     {XYZ1( 1,-1,-1), UV(1.f, 0.f)},  // rgt-top-front
//     {XYZ1( 1, 1,-1), UV(1.f, 1.f)},  // rgt-btm-front
//     {XYZ1(-1,-1,-1), UV(0.f, 0.f)},  // lft-top-front
//     {XYZ1( 1, 1,-1), UV(1.f, 1.f)},  // rgt-btm-front
//     {XYZ1(-1, 1,-1), UV(0.f, 1.f)},  // lft-btm-front
//     //top face
//     {XYZ1(-1,-1,-1), UV(0.f, 1.f)},  // lft-top-front
//     {XYZ1( 1,-1, 1), UV(1.f, 0.f)},  // rgt-top-back
//     {XYZ1( 1,-1,-1), UV(1.f, 1.f)},  // rgt-top-front
//     {XYZ1(-1,-1,-1), UV(0.f, 1.f)},  // lft-top-front
//     {XYZ1(-1,-1, 1), UV(0.f, 0.f)},  // lft-top-back
//     {XYZ1( 1,-1, 1), UV(1.f, 0.f)},  // rgt-top-back
//     //bottom face
//     {XYZ1(-1, 1,-1), UV(0.f, 0.f)},  // lft-btm-front
//     {XYZ1( 1, 1, 1), UV(1.f, 1.f)},  // rgt-btm-back
//     {XYZ1(-1, 1, 1), UV(0.f, 1.f)},  // lft-btm-back
//     {XYZ1(-1, 1,-1), UV(0.f, 0.f)},  // lft-btm-front
//     {XYZ1( 1, 1,-1), UV(1.f, 0.f)},  // rgt-btm-front
//     {XYZ1( 1, 1, 1), UV(1.f, 1.f)},  // rgt-btm-back
//     //right face
//     {XYZ1( 1, 1,-1), UV(0.f, 1.f)},  // rgt-btm-front
//     {XYZ1( 1,-1, 1), UV(1.f, 0.f)},  // rgt-top-back
//     {XYZ1( 1, 1, 1), UV(1.f, 1.f)},  // rgt-btm-back
//     {XYZ1( 1,-1, 1), UV(1.f, 0.f)},  // rgt-top-back
//     {XYZ1( 1, 1,-1), UV(0.f, 1.f)},  // rgt-btm-front
//     {XYZ1( 1,-1,-1), UV(0.f, 0.f)},  // rgt-top-front
//     //back face
//     {XYZ1(-1, 1, 1), UV(1.f, 1.f)},  // lft-btm-back
//     {XYZ1( 1, 1, 1), UV(0.f, 1.f)},  // rgt-btm-back
//     {XYZ1(-1,-1, 1), UV(1.f, 0.f)},  // lft-top-back
//     {XYZ1(-1,-1, 1), UV(1.f, 0.f)},  // lft-top-back
//     {XYZ1( 1, 1, 1), UV(0.f, 1.f)},  // rgt-btm-back
//     {XYZ1( 1,-1, 1), UV(0.f, 0.f)},  // rgt-top-back
// };
