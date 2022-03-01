package cz.e_bs.cmi.mdr.pdb.app.components.files

import com.raquo.laminar.api.L.{*, given}
import io.laminext.syntax.core.{*, given}

object FilePicker:
  import cz.e_bs.cmi.mdr.pdb.app.components.files

  val (fsaStream, fsaObserver) = EventStream.withObserver[FileSelector.Action]
  val selectorOpen = Var[Boolean](false)

  def apply(chosenFiles: Var[List[File]]): HtmlElement =
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
      /* TODO: transition
            enter="ease-out duration-300"
            enterFrom="opacity-0"
            enterTo="opacity-100"
            leave="ease-in duration-200"
            leaveFrom="opacity-100"
            leaveTo="opacity-0"
       */
      div(
        div(
          onClick.mapTo(false) --> selectorOpen.writer,
          cls("fixed inset-0 transition-opacity"),
          div(cls("absolute inset-0 bg-gray-500 opacity-75"))
        )
      )

    inline def modalSelector: HtmlElement =
      div(
        cls("fixed inset-0 z-10 overflow-y-auto"),
        div(
          cls("text-center sm:block sm:p-0"),
          overlay,
          browserCenteringModalTrick,
          child <-- chosenFiles.signal.map(
            FileSelector(
              _,
              Val(
                List(
                  File(
                    "https://tc163.cmi.cz/first_file",
                    "Smlouva o pracovním poměru"
                  ),
                  File(
                    "https://tc163.cmi.cz/first_file",
                    "Vysokoškolský diplom"
                  ),
                  File(
                    "https://tc163.cmi.cz/first_file",
                    "Prezenční listina školení"
                  ),
                  File("https://tc163.cmi.cz/first_file", "Životopis")
                )
              )
            )(fsaObserver)
          )
        )
      )

    div(
      fsaStream.collect { case FileSelector.SelectionUpdated(files) =>
        files.to(List)
      } --> chosenFiles.writer,
      fsaStream.mapTo(false) --> selectorOpen.writer,
      child.maybe <-- selectorOpen.signal.map(isOpen =>
        if isOpen then Some(modalSelector) else None
      ),
      div(
        cls("flex flex-col space-y-5"),
        child.maybe <-- chosenFiles.signal.map(files =>
          if files.isEmpty then None else Some(FileList(files))
        ),
        button(
          tpe := "button",
          cls := "bg-white py-2 px-3 border border-gray-300 rounded-md shadow-sm text-sm leading-4 font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500",
          "Zvolit soubory",
          onClick.mapTo(true) --> selectorOpen.writer
        )
      )
    )
