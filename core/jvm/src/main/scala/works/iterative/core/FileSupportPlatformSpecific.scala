package works.iterative.core

import zio.*
import zio.stream.*

trait FileSupportPlatformSpecific:
  type FileRepr = java.io.File

  extension (f: FileRepr)
    def name: String = f.getName
    def toStream: Stream[Throwable, Byte] =
      ZStream.fromInputStreamScoped(
        ZIO.fromAutoCloseable(
          ZIO.attempt(new java.io.FileInputStream(f)).refineOrDie {
            case e: java.io.IOException => e
          }
        )
      )
