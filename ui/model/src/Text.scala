package works.iterative
package ui.model

type Text = OneLine | Paragraph | List[Paragraph]

// TODO: rename to MultiLine
opaque type Paragraph = String

object Paragraph:
  def apply(text: String): Paragraph = text

  given Conversion[String, Paragraph] with
    def apply(text: String): Paragraph = text

  given Conversion[Paragraph, String] with
    def apply(text: Paragraph): String = text

  extension (p: Paragraph) def toString: String = p

opaque type OneLine = String

object OneLine:
  // TODO: check that the string actually is one line
  def apply(text: String): OneLine = text

  given Conversion[String, OneLine] with
    def apply(text: String): OneLine = text

  given Conversion[OneLine, String] with
    def apply(text: OneLine): String = text

  extension (p: OneLine) def toString: String = p
