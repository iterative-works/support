package works.iterative
package ui.components.tailwind

import com.raquo.laminar.api.L.{*, given}

trait StyleGuide:
  def card: Setter[HtmlElement]

object StyleGuide:
  object default extends StyleGuide:
    override def card: Setter[HtmlElement] = cls(
      "bg-white shadow sm:rounded-md overflow-hidden"
    )
