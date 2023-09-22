package works.iterative.core.service

import works.iterative.core.FileSupport.*
import works.iterative.core.FileRef

import zio.*

trait FileStore:
  type Op[A] = UIO[A]
  def store(file: FileRepr): Op[FileRef]

object FileStore:
  type Op[A] = URIO[FileStore, A]
  def store(file: FileRepr): Op[FileRef] =
    ZIO.serviceWithZIO(_.store(file))
