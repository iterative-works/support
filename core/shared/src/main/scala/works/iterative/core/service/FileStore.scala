package works.iterative.core.service

import works.iterative.core.FileSupport.*
import works.iterative.core.FileRef

import zio.*

trait FileStore:
  type Op[A] = UIO[A]

  def store(files: List[FileRepr]): Op[List[FileRef]]

  def store(
      name: String,
      file: Array[Byte],
      contentType: Option[String]
  ): Op[FileRef]

  def load(url: String): Op[Option[Array[Byte]]]

object FileStore:
  type Op[A] = URIO[FileStore, A]

  def store(files: List[FileRepr]): Op[List[FileRef]] =
    ZIO.serviceWithZIO(_.store(files))

  def store(
      name: String,
      file: Array[Byte],
      contentType: Option[String]
  ): Op[FileRef] =
    ZIO.serviceWithZIO(_.store(name, file, contentType))

  def load(url: String): Op[Option[Array[Byte]]] =
    ZIO.serviceWithZIO(_.load(url))
