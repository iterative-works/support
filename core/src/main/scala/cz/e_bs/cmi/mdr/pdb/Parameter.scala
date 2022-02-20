package cz.e_bs.cmi.mdr.pdb

import java.math.BigInteger
import java.security.MessageDigest

case class ParameterCriteria(
    chapterId: String,
    itemId: String,
    criteriumText: String
)

case class Parameter(
    id: String,
    name: String,
    description: String,
    criteria: List[ParameterCriteria]
)
