package works.iterative
package tapir.codecs

import zio.json.*
import works.iterative.tapir.CustomTapir.*
import works.iterative.core.*
import sttp.tapir.Validator
import works.iterative.core.service.FileStore

case class LegacyFileRef(
    name: String,
    url: String,
    fileType: Option[String],
    size: Option[Long]
) derives JsonCodec

trait FileCodecs extends JsonFileCodecs with TapirFileCodecs

trait JsonFileCodecs:
    given fileRefEncoder: JsonEncoder[FileRef] = DeriveJsonEncoder.gen[FileRef]
    val fileRefDecoder: JsonDecoder[FileRef] = DeriveJsonDecoder.gen[FileRef]

    given completeFileRefDecoder: JsonDecoder[FileRef] =
        fileRefDecoder.orElse(
            JsonDecoder[LegacyFileRef].map(f =>
                FileRef(
                    f.name,
                    f.url,
                    List(
                        f.fileType.map(FileStore.Metadata.FileType -> _),
                        f.size.map(FileStore.Metadata.Size -> _.toString())
                    ).flatten.toMap
                )
            )
        )

    given JsonCodec[FileRef] = JsonCodec(fileRefEncoder, completeFileRefDecoder)
end JsonFileCodecs

trait TapirFileCodecs:
    given Schema[FileRef] = Schema.derived[FileRef]
end TapirFileCodecs

object FileCodecs extends FileCodecs
