package hephaestus

trait IntInstances {
  implicit def toIntFlagOps(i: Int): IntFlagOps = new IntFlagOps(i)
}

final class IntFlagOps(val i: Int) extends AnyVal {
  def hasBitFlags(f: Int): Boolean = (i & f) == f
}

trait ListInstances {
  implicit def toListMaskOps[A](as: List[A]): ListMaskOps[A] =
    new ListMaskOps(as)
}
final class ListMaskOps[A](val as: List[A]) extends AnyVal {
  def mask(mask: Int): List[A] =
    as.zipWithIndex
      .foldLeft(List.empty[A])((acc, el) =>
        el match {
          case (a, i) => if ((mask & (1 << i)) > 0) a :: acc else acc
      })
      .reverse
}
