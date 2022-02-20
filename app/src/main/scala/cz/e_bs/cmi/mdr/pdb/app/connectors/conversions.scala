package cz.e_bs.cmi.mdr.pdb.app.connectors

import cz.e_bs.cmi.mdr.pdb.UserInfo
import cz.e_bs.cmi.mdr.pdb.app.pages.detail.components.DetailOsoby
import cz.e_bs.cmi.mdr.pdb.Parameter
import cz.e_bs.cmi.mdr.pdb.app.pages.detail.components.SeznamParametru
import cz.e_bs.cmi.mdr.pdb.app.components.Color
import cz.e_bs.cmi.mdr.pdb.ParameterCriteria
import cz.e_bs.cmi.mdr.pdb.app.pages.detail.components.SeznamKriterii

extension (o: UserInfo)
  def toViewModel: DetailOsoby.ViewModel =
    DetailOsoby.ViewModel(
      o.personalNumber,
      o.name,
      o.email,
      o.phone,
      o.img,
      None,
      None
    )

extension (param: Parameter)
  def toViewModel: SeznamParametru.Parametr =
    SeznamParametru.Parametr(
      id = param.id,
      nazev = param.name,
      status = "Nesplněno",
      statusColor = Color.red
    )

extension (crit: ParameterCriteria)
  def toViewModel: SeznamKriterii.Kriterium =
    SeznamKriterii.Kriterium(
      nazev = crit.criteriumText,
      kapitola = crit.chapterId,
      bod = crit.itemId,
      status = "Nesplněno",
      statusColor = Color.red,
      splneno = false
    )
