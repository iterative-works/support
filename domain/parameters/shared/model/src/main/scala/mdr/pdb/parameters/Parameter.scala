package mdr.pdb
package parameters

import java.math.BigInteger
import java.security.MessageDigest
import java.time.LocalDate

object ParameterCriterion:
  type Id = String

case class ParameterCriterion(
    chapterId: String,
    itemId: String,
    criteriumText: String
):
  val id: ParameterCriterion.Id = s"${chapterId}${itemId}"

object Parameter:
  type Id = String

case class Parameter(
    id: Parameter.Id,
    name: String,
    description: String,
    criteria: List[ParameterCriterion]
)
