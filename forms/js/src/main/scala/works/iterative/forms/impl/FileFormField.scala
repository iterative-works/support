package portaly.forms
package impl

import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*
import io.laminext.syntax.core.*
import works.iterative.core.MessageCatalogue
import org.scalajs.dom.File

class FileFormField(multiple: Boolean = false)(using cs: Components, messages: MessageCatalogue)
    extends FormPart[Any, List[String], Boolean, List[File], Nothing, List[File]]:
    private val selectedFiles: Var[List[File]] = Var(Nil)
    private val selectedFileNames: Var[List[String]] = Var(Nil)

    // TODO: use components
    private def domOutput(fi: FormPartInputs[Any, List[String], Boolean]) =
        val inError = fi.errorInput.toSignal(false)
        cs.inputFieldContainer(
            inError,
            // File list
            ul(
                cls("pb-2"),
                children <-- selectedFiles.signal.map(_.map { file =>
                    val active: Var[Boolean] = Var(false)
                    li(
                        cls("flex items-center text-sm text-blue-400 py-2"),
                        onMouseEnter.mapTo(true) --> active,
                        onMouseLeave.mapTo(false) --> active,
                        cls("bg-gray-50") <-- active.signal,
                        child <-- active.signal.switch(
                            span(
                                cls("hover:text-indigo-500 cursor-pointer"),
                                onClick.mapTo(file) --> Observer.combine[File](
                                    selectedFiles.updater((fs, f) => fs.filterNot(_ == f)),
                                    selectedFileNames.updater[File]((fs, f) =>
                                        fs.filterNot(_ == f.name)
                                    )
                                ),
                                cs.attachmentRemoveIcon(svg.cls("flex-shrink-0 h-5 w-5"))
                            ),
                            span(
                                cs.attachmentIcon(svg.cls("flex-shrink-0 h-5 w-5"))
                            )
                        ),
                        span(
                            cls("ml-2 truncate"),
                            file.name
                        )
                    )
                })
            ),
            // File upload button
            div(
                cls := "mt-4 sm:mt-0 sm:flex-none",
                L.label(
                    cls("block w-full"),
                    div(
                        cs.buttonLike,
                        cls("inline-flex"),
                        cs.uploadIcon(svg.cls("w-5 h-5 mr-1")),
                        fi.id.toMessageNode("button.add")
                    ),
                    L.input(
                        cls("hidden"),
                        tpe("file"),
                        L.multiple(multiple),
                        inContext: thisNode =>
                            onInput.mapTo(
                                thisNode.ref.files.toList
                            ) --> selectedFiles.updater[List[File]](_ ++ _)
                    )
                )
            )
        )
    end domOutput

    override def apply(fi: FormPartInputs[Any, List[String], Boolean])
        : FormPartOutputs[List[File], Nothing, List[File]] =
        FormPartOutputs.succeed(fi.id, selectedFiles.signal, domOutput(fi))
end FileFormField
