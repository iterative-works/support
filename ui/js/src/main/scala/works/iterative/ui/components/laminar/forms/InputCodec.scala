package works.iterative.ui.components.laminar.forms

import zio.prelude.Validation
import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom.html
import com.raquo.laminar.nodes.ReactiveHtmlElement
import works.iterative.core.UserMessage
import works.iterative.core.PlainMultiLine
import com.raquo.airstream.core.Signal
import works.iterative.ui.components.laminar.HtmlRenderable.given
import works.iterative.ui.components.ComponentContext
import works.iterative.core.MessageCatalogue
import works.iterative.core.Validated
import scala.util.NotGiven
import works.iterative.core.Email

trait InputCodec[A]:
  def encode(a: A): String
  def decode(s: String): Validated[A]

object InputCodec:
  given InputCodec[String] with
    override def encode(a: String): String = a
    override def decode(s: String): Validated[String] = Validation.succeed(s)

  given InputCodec[PlainMultiLine] with
    override def encode(a: PlainMultiLine): String = a.asString
    override def decode(s: String): Validated[PlainMultiLine] =
      PlainMultiLine(s)

  given InputCodec[Email] with
    def encode(a: Email): String = a.value
    def decode(s: String): Validated[Email] = Email(s)
