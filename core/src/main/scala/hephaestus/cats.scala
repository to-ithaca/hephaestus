package hephaestus

import cats._, cats.data._
import monocle._

trait CatsInstances {
  implicit def toStateTLensOps[F[_], S, A](s: StateT[F, S, A]): StateTLensOps[F, S, A] = new StateTLensOps(s)
}

final class StateTLensOps[F[_], S, A](val s: StateT[F, S, A]) extends AnyVal {
  def transformLens[R](lens: Lens[R, S])(implicit F: Functor[F]): StateT[F, R, A] = 
    s.transformS(r => lens.get(r), (r, s) => lens.set(s)(r))
}
