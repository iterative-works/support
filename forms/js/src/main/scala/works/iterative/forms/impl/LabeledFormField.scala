package portaly.forms
package impl

import com.raquo.laminar.api.L.*
import works.iterative.core.UserMessage
import works.iterative.core.MessageCatalogue

final class LabeledFormField[-S, -RI, +RO, +A](
    field: FormPart[S, RI, Boolean, RO, Nothing, A],
    required: Signal[Boolean]
)(using cs: Components, messages: MessageCatalogue)
    extends FormPart[S, RI, List[UserMessage], RO, Nothing, A]:

    private def domOutput(
        fi: FormPartInputs[S, RI, List[UserMessage]],
        innerOutputs: FormPartOutputs[RO, Unit, A]
    ): HtmlElement =
        cs.labeledField(
            innerOutputs.id.toHtmlId,
            innerOutputs.id.toMessageNode("label"),
            innerOutputs.id.toMessageNodeOpt("help"),
            required,
            fi.errorInput.toSignal(Nil).combineWithFn(fi.showErrors)((errs, show) =>
                if show then errs.map(messages(_)) else Nil
            ),
            innerOutputs.domOutput
        )

    override def apply(inputs: FormPartInputs[S, RI, List[UserMessage]])
        : FormPartOutputs[RO, Nothing, A] =
        val innerOutput = field(inputs.mapError(_.nonEmpty))
        FormPartOutputs(
            innerOutput.id,
            innerOutput.rawOutput.setDisplayName(s"labeled_raw:${innerOutput.id.toHtmlId}"),
            innerOutput.errorOutput,
            innerOutput.output.setDisplayName(s"labeled_output:${innerOutput.id.toHtmlId}"),
            domOutput(inputs, innerOutput)
        )
    end apply
end LabeledFormField
