package mdr.pdb.proof
package codecs

import zio.json.*

trait Codecs extends JsonCodecs with TapirCodecs

trait JsonCodecs extends mdr.pdb.codecs.JsonCodecs:
  given JsonCodec[Authorization] = DeriveJsonCodec.gen
  given JsonCodec[RevocationReason] = DeriveJsonCodec.gen
  given JsonCodec[Revocation] = DeriveJsonCodec.gen
  given JsonCodec[Proof] = DeriveJsonCodec.gen

trait TapirCodecs extends mdr.pdb.codecs.TapirCodecs:
  given Schema[Authorization] = Schema.derived
  given Schema[RevocationReason] = Schema.derived
  given Schema[Revocation] = Schema.derived
  given Schema[Proof] = Schema.derived

object Codecs extends Codecs
