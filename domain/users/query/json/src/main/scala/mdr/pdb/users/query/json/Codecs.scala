package mdr.pdb
package users.query
package json

import zio.json.*

trait Codecs extends mdr.pdb.json.Codecs:

  given JsonCodec[UserContract] = DeriveJsonCodec.gen
  given JsonCodec[UserFunction] = DeriveJsonCodec.gen
  given JsonCodec[UserInfo] = DeriveJsonCodec.gen
  given JsonCodec[UserProfile] = DeriveJsonCodec.gen

object Codecs extends Codecs
