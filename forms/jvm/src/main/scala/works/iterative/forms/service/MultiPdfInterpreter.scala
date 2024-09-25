package portaly.forms
package service

import zio.*
import works.iterative.core.MessageCatalogue
import works.iterative.core.Language

trait MultiPdfInterpreter:
    import MultiPdfInterpreter.*
    def interpret(
        forms: List[FormRender],
        messages: MessageCatalogue,
        lang: Language
    ): UIO[Chunk[Byte]]
end MultiPdfInterpreter

object MultiPdfInterpreter:
    final case class FormRender(
        id: FormIdent,
        form: Form,
        data: Option[Map[String, Seq[String]]],
        params: Map[String, String] = Map.empty
    )

    def interpret(
        forms: List[FormRender],
        messages: MessageCatalogue,
        lang: Language
    ): URIO[MultiPdfInterpreter, Chunk[Byte]] =
        ZIO.serviceWithZIO(_.interpret(forms, messages, lang))
end MultiPdfInterpreter
