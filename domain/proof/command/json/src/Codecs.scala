package mdr.pdb
package proof.command
package json

import zio.json.*

trait Codecs extends proof.json.Codecs:

  given JsonCodec[AuthorizeOption] = DeriveJsonCodec.gen
  given JsonCodec[CreateProof] = DeriveJsonCodec.gen
  given JsonCodec[AuthorizeProof] = DeriveJsonCodec.gen
  given JsonCodec[UpdateProof] = DeriveJsonCodec.gen
  given JsonCodec[RevokeProof] = DeriveJsonCodec.gen
