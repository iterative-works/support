package cz.e_bs.cmi.mdr.pdb.app.components.files

import com.raquo.laminar.api.L.{*, given}
import com.raquo.domtypes.generic.codecs.BooleanAsTrueFalseStringCodec

object FilePicker:
  import cz.e_bs.cmi.mdr.pdb.app.components.files
  val File = files.File
  val Selector = FileSelector _

  def apply(content: HtmlElement): HtmlElement =
    // This sequence tricks browser into displaying modal content centered
    // Inspired by modal in headless ui playground
    // https://github.com/tailwindlabs/headlessui/blob/fdd26297953080d5ec905dda0bf5ec9607897d86/packages/playground-react/pages/transitions/component-examples/modal.tsx#L78-L79
    inline def browserCenteringModalTrick: Modifier[HtmlElement] =
      Seq[Modifier[HtmlElement]](
        span(cls("hidden sm:inline-block sm:h-screen sm:align-middle")),
        "​" // Zero width space
      )

    inline def overlay: Modifier[HtmlElement] =
      // Page overlay
      div(
        div(
          cls("fixed inset-0 transition-opacity"),
          div(cls("absolute inset-0 bg-gray-500 opacity-75"))
        )
      )

    div(
      cls("fixed inset-0 z-10 overflow-y-auto"),
      div(
        cls(
          "flex min-h-screen items-end justify-center px-4 pt-4 pb-20 text-center sm:block sm:p-0"
        ),
        overlay,
        browserCenteringModalTrick,
        div(
          cls(
            "inline-block transform overflow-hidden rounded-lg bg-white text-left align-bottom shadow-xl transition-all sm:my-8 sm:w-full sm:max-w-lg sm:align-middle"
          ),
          role("dialog"),
          customHtmlAttr("aria.modal", BooleanAsTrueFalseStringCodec)(true),
          aria.labelledBy("modal-headline"),
          div(
            cls("bg-white px-4 pt-5 pb-4 sm:p-6 sm:pb-4"),
            div(
              cls("sm:flex sm:items-start"),
              div(
                cls("mt-3 text-center sm:mt-0 sm:ml-4 sm:text-left"),
                h3(
                  cls("text-lg font-medium leading-6 text-gray-900"),
                  idAttr("modal-headline"),
                  "Výběr souborů"
                )
              )
            ),
            Selector(
              Val(
                List(
                  File(
                    "https://tc163.cmi.cz/first_file",
                    "Smlouva o pracovním poměru"
                  ),
                  File(
                    "https://tc163.cmi.cz/first_file",
                    "Diplom k vystudování VŠ"
                  ),
                  File(
                    "https://tc163.cmi.cz/first_file",
                    "Prezenční listina školení"
                  ),
                  File("https://tc163.cmi.cz/first_file", "Životopis")
                )
              )
            )
          )
        )
      ),
      content
    )
