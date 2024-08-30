package works.iterative.ui

import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.ZoneId
import java.time.temporal.TemporalAccessor
import java.time.LocalDate
import java.time.LocalDateTime

object TimeUtils:
    val dateTimeFormat =
        DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.SHORT)
            // TODO: locale
            // .withLocale(Locale("cs", "CZ"))
            .withZone(ZoneId.systemDefault())

    val dateFormat =
        DateTimeFormatter
            .ofLocalizedDate(FormatStyle.SHORT)
            // TODO: locale
            // .withLocale(Locale("cs", "CZ"))
            .withZone(ZoneId.systemDefault())

    val htmlDateFormat =
        DateTimeFormatter
            .ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.systemDefault())

    val htmlDateTimeFormat =
        DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault())

    def formatDateTime(i: TemporalAccessor): String =
        dateTimeFormat.format(i)

    def formatDate(i: TemporalAccessor): String =
        dateFormat.format(i)

    def formatHtmlDate(i: TemporalAccessor): String =
        htmlDateFormat.format(i)

    def formatHtmlDateTime(i: TemporalAccessor): String =
        htmlDateTimeFormat.format(i)

    def parseHtmlLocalDate(i: String): LocalDate =
        LocalDate.parse(i, htmlDateFormat)

    def parseHtmlLocalDateTime(i: String): LocalDateTime =
        LocalDateTime.parse(i, htmlDateTimeFormat)
end TimeUtils
