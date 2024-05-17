package works.iterative.core

import zio.*
import zio.stream.*
import scala.scalajs.js.typedarray.Int8Array

trait FileSupportPlatformSpecific:
    type FileRepr = org.scalajs.dom.File

    extension (f: FileRepr)
        def name: String = f.name
        def toStream: Stream[Throwable, Byte] =
            ZStream.unfoldChunkZIO(f.stream().getReader())(reader =>
                ZIO
                    .fromPromiseJS(reader.read())
                    .map(chunk =>
                        if chunk.done then None
                        else
                            val v = chunk.value
                            Some(
                                (
                                    Chunk.fromArray(
                                        new Int8Array(v.buffer, v.byteOffset, v.length).toArray
                                    ),
                                    reader
                                )
                            )
                    )
            )
    end extension
end FileSupportPlatformSpecific
