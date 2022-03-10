package mdr.pdb
package users.query
package codecs

import zio.json.*

trait Codecs extends JsonCodecs with TapirCodecs

trait JsonCodecs extends mdr.pdb.codecs.JsonCodecs:
  given JsonCodec[Criteria] = DeriveJsonCodec.gen
  given JsonCodec[UserContract] = DeriveJsonCodec.gen
  given JsonCodec[UserFunction] = DeriveJsonCodec.gen
  given JsonCodec[UserInfo] = DeriveJsonCodec.gen
  given JsonCodec[UserProfile] = DeriveJsonCodec.gen

trait TapirCodecs extends mdr.pdb.codecs.TapirCodecs:
  given Schema[Criteria] = Schema.derived
  given Schema[UserContract] = Schema.derived
  given Schema[UserFunction] = Schema.derived
  given Schema[UserInfo] = Schema.derived
  given Schema[UserProfile] = Schema.derived

object Codecs extends Codecs
