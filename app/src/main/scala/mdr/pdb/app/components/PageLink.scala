package mdr.pdb.app.components

import com.raquo.laminar.api.L.{*, given}
import works.iterative.ui.components.tailwind.LinkSupport.*
import mdr.pdb.app.Page
import mdr.pdb.app.Action
import com.raquo.waypoint.Router
import mdr.pdb.app.NavigateTo

object PageLink:
  type ViewModel = Page
  def apply($m: Signal[ViewModel], actions: Observer[Action])(using
      router: Router[Page]
  ): Anchor =
    a(mods($m, actions), child.text <-- $m.map(_.title))

  def apply(m: ViewModel, actions: Observer[Action])(using
      router: Router[Page]
  ): Anchor = apply(Val(m), actions)

  def container($m: Signal[ViewModel], actions: Observer[Action])(using
      router: Router[Page]
  ): Anchor = a(mods($m, actions))

  def container(m: ViewModel, actions: Observer[Action])(using
      Router[Page]
  ): Anchor = container(Val(m), actions)

  private def mods($m: Signal[Page], actions: Observer[Action])(using
      router: Router[Page]
  ): Modifier[Anchor] =
    Seq(
      href <-- $m.map(router.absoluteUrlForPage).recover { case _ =>
        Some("invalid url")
      },
      composeEvents(onClick.noKeyMod.preventDefault)(
        _.sample($m)
          .map(NavigateTo.apply)
      ) --> actions
    )
