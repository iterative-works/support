package mdr.pdb.proof
package json

import zio.json.*

trait Codecs extends mdr.pdb.json.Codecs:

  given JsonCodec[Authorization] = DeriveJsonCodec.gen
  given JsonCodec[RevocationReason] = DeriveJsonCodec.gen
  given JsonCodec[Revocation] = DeriveJsonCodec.gen
  given JsonCodec[Proof] = DeriveJsonCodec.gen

object Codecs extends Codecs
