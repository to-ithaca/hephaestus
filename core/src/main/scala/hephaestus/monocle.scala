package hephaestus

import monocle._
import cats._, cats.data._, cats.implicits._

trait MonocleInstances {
  implicit def toLensStateOps[S, A](lens: Lens[S, A]): LensStateOps[S, A] =
    new LensStateOps(lens)
}

final class LensStateOps[S, A](val lens: Lens[S, A]) extends AnyVal {
  //this needs to use a monad state
  def modifying[F[_]](f: A => A)(implicit M: MonadState[F, S]): F[Unit] =
    M.modify(s => lens.modify(f)(s))

  def use[F[_]](implicit M: MonadState[F, S]): F[A] =
    M.inspect(s => lens.get(s))
}

//TODO: may like to have
//Lens[S, F[A]] setPure(a: A), modifyPure(f: F[A] => A), where F has a monad instance
//similarly, if, F has a comonad instance, we can extract (would be strange though)
