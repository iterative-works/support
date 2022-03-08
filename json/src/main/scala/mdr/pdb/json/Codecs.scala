package mdr.pdb
package json

import zio.json.*

trait Codecs:

  given JsonCodec[WhoWhen] = DeriveJsonCodec.gen
  given JsonCodec[OsobniCislo] =
    JsonCodec.string.transform(OsobniCislo.apply, _.toString)
  given JsonFieldEncoder[OsobniCislo] =
    JsonFieldEncoder.string.contramap(_.toString)
  given JsonFieldDecoder[OsobniCislo] =
    JsonFieldDecoder.string.map(OsobniCislo(_))

object Codecs extends Codecs
