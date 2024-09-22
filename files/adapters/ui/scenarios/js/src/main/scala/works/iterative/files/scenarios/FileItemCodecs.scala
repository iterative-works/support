package works.iterative.files

import zio.json.*
import works.iterative.tapir.codecs.FileCodecs.given

object FileItemCodecs:
    given JsonCodec[FileItem] = DeriveJsonCodec.gen[FileItem]
