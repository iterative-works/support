package works.iterative.core.service

import works.iterative.core.FileSupport.*
import works.iterative.core.FileRef
import works.iterative.core.UserMessage

import zio.*

trait FileStore:
  type Op[A] = IO[UserMessage, A]
  def store(file: FileRepr): Op[FileRef]

object FileStore:
  type Op[A] = ZIO[FileStore, UserMessage, A]
  def store(file: FileRepr): Op[FileRef] =
    ZIO.serviceWithZIO(_.store(file))
