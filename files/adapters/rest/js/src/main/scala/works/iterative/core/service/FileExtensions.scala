package works.iterative.core.service

import zio.*
import zio.stream.*
import works.iterative.core.FileSupport.*
import sttp.model.Part

extension (f: FileRepr)
    def toPart: Task[Part[Array[Byte]]] =
        f.toStream.run(ZSink.collectAll).map { bytes =>
            Part(
                "file",
                bytes.toArray,
                fileName = Some(f.name)
            )
        }
