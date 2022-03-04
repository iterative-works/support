package mdr.pdb.app

import zio.*
import com.raquo.airstream.ownership.Owner
import scala.annotation.unused
import com.raquo.airstream.ownership.Subscription

object ZIOOwner:
  def acquire: UIO[ZIOOwner] = ZIO.succeed(new ZIOOwner)

  val layer = ZLayer.fromAcquireRelease(ZIOOwner.acquire)(_.release)

class ZIOOwner extends Owner:
  def release: UIO[Unit] =
    Task.attempt(this.killSubscriptions()).ignore
