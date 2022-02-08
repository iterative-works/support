package cz.e_bs.cmi.mdr.pdb.app

enum Page(val title: String):
  case Dashboard extends Page("Dashboard")
  case Detail extends Page("Detail")
