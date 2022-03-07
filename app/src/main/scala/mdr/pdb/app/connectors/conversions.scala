package mdr.pdb.app.connectors

import com.raquo.laminar.api.L.{*, given}
import mdr.pdb.users.query.UserInfo
import mdr.pdb.app.pages.detail.components.DetailOsoby
import mdr.pdb.parameters.*
import mdr.pdb.app.pages.detail.components.SeznamParametru
import mdr.pdb.app.pages.detail.components.SeznamKriterii
import mdr.pdb.app.pages.directory.components.UserRow
import mdr.pdb.app.pages.detail.components.DetailParametru
import mdr.pdb.app.pages.detail.components.DetailKriteria
import fiftyforms.ui.components.tailwind.Color

extension (o: UserInfo)
  def toDetailOsoby: DetailOsoby.ViewModel =
    DetailOsoby.ViewModel(
      o.personalNumber,
      o.name,
      o.email,
      o.phone,
      o.img,
      o.mainFunction.map(f =>
        DetailOsoby.Funkce.ViewModel(f.name, f.dept, f.ou)
      ),
      o.userContracts.headOption.map(c =>
        DetailOsoby.PracovniPomer.ViewModel(c.rel, c.startDate, c.endDate)
      )
    )

extension (param: Parameter)
  def toParametr(container: Parameter => Anchor): DetailParametru.ViewModel =
    DetailParametru.ViewModel(
      id = param.id,
      nazev = param.name,
      popis = param.description,
      status = "Nesplněno",
      statusColor = Color.red,
      a = container(param)
    )

extension (crit: ParameterCriterion)
  def toKriterium(
      container: ParameterCriterion => Anchor
  ): DetailKriteria.ViewModel =
    DetailKriteria.ViewModel(
      nazev = crit.criteriumText,
      kapitola = crit.chapterId,
      bod = crit.itemId,
      status = "Nesplněno",
      statusColor = Color.red,
      splneno = false,
      dukazy = Nil,
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
      hlavniFunkce = user.mainFunction.map(_.name),
      img = user.img,
      container = () => container(user)
    )
