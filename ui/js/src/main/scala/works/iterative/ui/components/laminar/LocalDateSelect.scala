package works.iterative.ui.components.laminar

import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*

import java.time.LocalDate
import org.scalajs.dom.Event
import com.raquo.laminar.modifiers.KeyUpdater

trait LocalDateSelectModule:
  val localDateSelect: LocalDateSelect = new LocalDateSelect

  class LocalDateSelect:
    import LocalDateSelect.*

    def valueUpdater(
        signal: Signal[Option[LocalDate]]
    ): KeyUpdater.PropUpdater[String, String] =
      L.value <-- signal.map(_.map(formatDate).getOrElse(""))

    // Does not work in `controlled`
    // Laminar refuses the custom prop, requries its own `value` or `checked`
    val value: HtmlProp[Option[LocalDate]] =
      htmlProp("value", OptLocalDateAsStringCodec)

    val min: HtmlProp[LocalDate] =
      htmlProp("min", LocalDateAsStringCodec)

    val max: HtmlProp[LocalDate] =
      htmlProp("max", LocalDateAsStringCodec)

    val onInput: EventProcessor[Event, LocalDate] =
      L.onInput.mapToValue.setAsValue.map(parseDate).collect { case Some(d) =>
        d
      }

    val onOptInput: EventProcessor[Event, Option[LocalDate]] =
      onInput.mapToValue.setAsValue.map(parseDate)

  object LocalDateSelect:
    import java.time.format.DateTimeFormatter
    import java.time.LocalDate
    import com.raquo.laminar.codecs.Codec

    private val formatter: DateTimeFormatter =
      DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private def parseDate(date: String): Option[LocalDate] =
      import scala.util.Try
      if date.isEmpty then None
      else Try(LocalDate.parse(date, formatter)).toOption

    private def formatDate(date: LocalDate): String =
      formatter.format(date)

    object LocalDateAsStringCodec extends Codec[LocalDate, String]:
      override def decode(domValue: String): LocalDate =
        parseDate(domValue).orNull

      override def encode(scalaValue: LocalDate): String =
        formatDate(scalaValue)

    object OptLocalDateAsStringCodec extends Codec[Option[LocalDate], String]:
      override def decode(domValue: String): Option[LocalDate] =
        parseDate(domValue)

      override def encode(scalaValue: Option[LocalDate]): String =
        scalaValue.map(formatDate).getOrElse("")
