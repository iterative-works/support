package mdr.pdb
package parameters

import java.math.BigInteger
import java.security.MessageDigest

object ParameterCriteria:
  type Id = String

case class ParameterCriteria(
    chapterId: String,
    itemId: String,
    criteriumText: String
):
  val id: ParameterCriteria.Id = s"${chapterId}${itemId}"

object Parameter:
  type Id = String

case class Parameter(
    id: Parameter.Id,
    name: String,
    description: String,
    criteria: List[ParameterCriteria]
)
