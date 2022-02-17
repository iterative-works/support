package cz.e_bs.cmi.mdr.pdb.app

import cz.e_bs.cmi.mdr.pdb.OsobniCislo

sealed trait Action

case object FetchDirectory extends Action
case class FetchUserDetails(osc: OsobniCislo) extends Action
case class NavigateTo(page: Page) extends Action
