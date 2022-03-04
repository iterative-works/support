package mdr.pdb

import java.math.BigInteger
import java.security.MessageDigest
import zio.json.DeriveJsonCodec
import zio.json.JsonCodec

object ParameterCriteria:
  type Id = String
  given JsonCodec[ParameterCriteria] = DeriveJsonCodec.gen

case class ParameterCriteria(
    chapterId: String,
    itemId: String,
    criteriumText: String
):
  val id: ParameterCriteria.Id = s"${chapterId}${itemId}"

object Parameter:
  type Id = String
  given JsonCodec[Parameter] = DeriveJsonCodec.gen

case class Parameter(
    id: Parameter.Id,
    name: String,
    description: String,
    criteria: List[ParameterCriteria]
)
