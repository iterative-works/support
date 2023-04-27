package works.iterative.ui.components.laminar

import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.{*, given}
import works.iterative.ui.components.tailwind.ComponentContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.raquo.laminar.keys.ReactiveProp
import com.raquo.domtypes.jsdom.defs.events.TypedTargetEvent
import org.scalajs.dom.html
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
    val value: ReactiveProp[Option[LocalDate], String] =
      customProp("value", OptLocalDateAsStringCodec)

    val min: ReactiveProp[LocalDate, String] =
      customProp("min", LocalDateAsStringCodec)

    val max: ReactiveProp[LocalDate, String] =
      customProp("max", LocalDateAsStringCodec)

    val onInput: EventProcessor[TypedTargetEvent[html.Element], LocalDate] =
      L.onInput.mapToValue.setAsValue.map(parseDate).collect { case Some(d) =>
        d
      }

    val onOptInput
        : EventProcessor[TypedTargetEvent[html.Element], Option[LocalDate]] =
      onInput.mapToValue.setAsValue.map(parseDate)

  object LocalDateSelect:
    import java.time.format.DateTimeFormatter
    import java.time.LocalDate
    import com.raquo.domtypes.generic.codecs.Codec

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
