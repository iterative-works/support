package cz.e_bs.cmi.mdr.pdb.app.connectors

import com.raquo.laminar.api.L.{*, given}
import cz.e_bs.cmi.mdr.pdb.UserInfo
import cz.e_bs.cmi.mdr.pdb.app.pages.detail.components.DetailOsoby
import cz.e_bs.cmi.mdr.pdb.Parameter
import cz.e_bs.cmi.mdr.pdb.app.pages.detail.components.SeznamParametru
import cz.e_bs.cmi.mdr.pdb.app.components.Color
import cz.e_bs.cmi.mdr.pdb.ParameterCriteria
import cz.e_bs.cmi.mdr.pdb.app.pages.detail.components.SeznamKriterii
import cz.e_bs.cmi.mdr.pdb.app.pages.directory.components.UserRow

extension (o: UserInfo)
  def toDetailOsoby: DetailOsoby.ViewModel =
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
  def toParametr(container: Parameter => Anchor): SeznamParametru.Parametr =
    SeznamParametru.Parametr(
      id = param.id,
      nazev = param.name,
      status = "Nesplněno",
      statusColor = Color.red,
      a = container(param)
    )

extension (crit: ParameterCriteria)
  def toKriterium(
      container: ParameterCriteria => Anchor
  ): SeznamKriterii.Kriterium =
    SeznamKriterii.Kriterium(
      nazev = crit.criteriumText,
      kapitola = crit.chapterId,
      bod = crit.itemId,
      status = "Nesplněno",
      statusColor = Color.red,
      splneno = false,
      container = container(crit)
    )

extension (user: UserInfo)
  def toUserRow(
      container: UserInfo => HtmlElement = (_: UserInfo) => div()
  ): UserRow.ViewModel =
    UserRow.ViewModel(
      osobniCislo = user.personalNumber.toString,
      celeJmeno = user.name,
      prijmeni = user.surname,
      hlavniFunkce = user.mainFunction,
      img = user.img,
      container = container(user)
    )
