package works.iterative.services.files
package components.tailwind

import com.raquo.laminar.api.L.{*, given}
import io.laminext.syntax.core.{*, given}

object FilePicker:
  sealed trait Event
  sealed trait DoneEvent extends Event
  case class SelectionUpdated(files: Set[File]) extends DoneEvent
  case object SelectionCancelled extends DoneEvent

  def apply(
      currentFiles: Signal[List[File]],
      availableFiles: Signal[List[File]]
  )(selectionUpdates: Observer[Event]): HtmlElement =
    val (updatesStream, updatesObserver) = EventStream.withObserver[Event]
    val selectorOpen = Var[Boolean](false)

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
          child <-- currentFiles.map(
            FileSelector(_, availableFiles)(updatesObserver)
          )
        )
      )

    div(
      updatesStream --> selectionUpdates,
      updatesStream.collect { case _: DoneEvent =>
        false
      } --> selectorOpen.writer,
      child.maybe <-- selectorOpen.signal.map(isOpen =>
        if isOpen then Some(modalSelector) else None
      ),
      div(
        cls("flex flex-col space-y-5"),
        child.maybe <-- currentFiles.map(files =>
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
