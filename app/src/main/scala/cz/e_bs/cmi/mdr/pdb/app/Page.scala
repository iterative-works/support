package cz.e_bs.cmi.mdr.pdb.app

sealed abstract class Page(val title: String)

case object Dashboard extends Page("Dashboard")
case object Detail extends Page("Detail")
