package works.iterative.ui.components.tailwind

import com.raquo.laminar.api.L.{*, given}

import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.ZoneId
import java.time.Instant
import java.time.temporal.TemporalAccessor

object TimeUtils:
  val dateTimeFormat =
    DateTimeFormatter
      .ofLocalizedDateTime(FormatStyle.SHORT)
      // TODO: locale
      // .withLocale(Locale("cs", "CZ"))
      .withZone(ZoneId.of("CET"))

  val dateFormat =
    DateTimeFormatter
      .ofLocalizedDate(FormatStyle.SHORT)
      // TODO: locale
      // .withLocale(Locale("cs", "CZ"))
      .withZone(ZoneId.of("CET"))

  def formatDateTime(i: TemporalAccessor): String =
    dateTimeFormat.format(i)

  def formatDate(i: TemporalAccessor): String =
    dateFormat.format(i)
