package mdr.pdb
package proof.command
package codecs

import zio.json.*

trait Codecs extends JsonCodecs with TapirCodecs

trait JsonCodecs extends proof.codecs.JsonCodecs:

  given JsonCodec[AuthorizeOption] = DeriveJsonCodec.gen
  given JsonCodec[CreateProof] = DeriveJsonCodec.gen
  given JsonCodec[AuthorizeProof] = DeriveJsonCodec.gen
  given JsonCodec[UpdateProof] = DeriveJsonCodec.gen
  given JsonCodec[RevokeProof] = DeriveJsonCodec.gen
  given JsonCodec[Command] = DeriveJsonCodec.gen

trait TapirCodecs extends proof.codecs.TapirCodecs:

  given Schema[AuthorizeOption] = Schema.derived
  given Schema[CreateProof] = Schema.derived
  given Schema[AuthorizeProof] = Schema.derived
  given Schema[UpdateProof] = Schema.derived
  given Schema[RevokeProof] = Schema.derived
  given Schema[Command] = Schema.derived
