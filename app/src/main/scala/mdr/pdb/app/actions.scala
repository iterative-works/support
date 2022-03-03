package mdr.pdb.app

import mdr.pdb.OsobniCislo
import mdr.pdb.UserInfo
import mdr.pdb.Parameter
import mdr.pdb.ParameterCriteria

sealed trait Action

case object FetchDirectory extends Action
case class FetchUserDetails(osc: OsobniCislo) extends Action
case class FetchParameters(osc: OsobniCislo) extends Action
case class FetchParameter(osc: OsobniCislo, paramId: String) extends Action
case class FetchParameterCriteria(
    osc: OsobniCislo,
    paramId: String,
    critId: String,
    page: (UserInfo, Parameter, ParameterCriteria) => Page
) extends Action
case class FetchAvailableFiles(osc: OsobniCislo) extends Action
case class NavigateTo(page: Page) extends Action
