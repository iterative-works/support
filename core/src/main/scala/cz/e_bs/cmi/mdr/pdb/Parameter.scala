package cz.e_bs.cmi.mdr.pdb

case class ParameterCriteria(
    chapterId: String,
    itemId: String,
    criteriumText: String
)

case class Parameter(
    name: String,
    description: String,
    criteria: List[ParameterCriteria]
)
