package cz.e_bs.cmi.mdr.pdb.app

// enum is not working with Waypoints' SplitRender collectStatic
sealed abstract class Page(val title: String)

object Page:
  case object Dashboard extends Page("Dashboard")
  case object Detail extends Page("Detail")
