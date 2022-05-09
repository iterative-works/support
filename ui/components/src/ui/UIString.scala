package works.iterative.ui

import com.raquo.laminar.nodes.TextNode

/** UIString is meant to mark the strings that are part of the UI and subject to
  * localization Another mechanism can later be used to find all these strings
  * and customize.
  */
opaque type UIString = String

object UIString:
  def apply(s: String): UIString = s

  extension (ui: UIString) inline def toNode: TextNode = TextNode(ui)

  extension (s: String) inline def ui: UIString = UIString(s)

  given Conversion[UIString, TextNode] with
    def apply(ui: UIString): TextNode = ui.toNode
  
  given Conversion[UIString, String] with
    inline def apply(ui: UIString): String = ui
