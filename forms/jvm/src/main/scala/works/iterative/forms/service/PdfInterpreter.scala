package portaly.forms
package service

import zio.*

final case class FormInfo(
    id: String,
    kind: String, // "zavazna, nezavazna",
    data: FormContent
)

trait PdfInterpreter
    extends Interpreter[FormInfo, UIO[Chunk[Byte]]]

object PdfInterpreter:
    def interpret(
        id: FormIdent,
        form: Form,
        data: Option[FormInfo]
    ): URIO[PdfInterpreter, Chunk[Byte]] =
        ZIO.serviceWithZIO(_.interpret(id, form, data))
end PdfInterpreter
