package works.iterative.services.files
package components.tailwind

import com.raquo.laminar.api.L.{*, given}
import com.raquo.domtypes.generic.codecs.StringAsIsCodec
import works.iterative.ui.components.tailwind.Icons
import works.iterative.ui.components.tailwind.Loading
import io.laminext.syntax.core.{*, given}
import com.raquo.domtypes.generic.codecs.BooleanAsTrueFalseStringCodec

object FileSelector:
  import FilePicker._

  def apply(
      initialFiles: List[File],
      availableFiles: Signal[List[File]]
  )(selectionUpdates: Observer[Event]): HtmlElement =
    val selectedFiles = Var[Set[File]](initialFiles.to(Set))
    div(
      cls(
        "inline-block transform overflow-hidden rounded-lg bg-white text-left align-bottom shadow-xl transition-all sm:my-8 sm:w-full sm:max-w-7xl sm:align-middle"
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
        FileTable(availableFiles, selectedFiles)
      ),
      div(
        cls("bg-gray-50 px-4 py-3 sm:flex sm:flex-row-reverse sm:px-6"),
        span(
          cls("flex w-full rounded-md shadow-sm sm:ml-3 sm:w-auto"),
          button(
            typ("button"),
            cls(
              "focus:shadow-outline-green inline-flex w-full justify-center rounded-md border border-transparent bg-indigo-600 px-4 py-2 text-base font-medium leading-6 text-white shadow-sm transition duration-150 ease-in-out hover:bg-indigo-500 focus:border-indigo-700 focus:outline-none sm:text-sm sm:leading-5"
            ),
            "Potvrdit",
            composeEvents(onClick)(
              _.sample(selectedFiles)
                .map(SelectionUpdated(_))
            ) --> selectionUpdates
          )
        ),
        span(
          cls("mt-3 flex w-full rounded-md shadow-sm sm:mt-0 sm:w-auto"),
          button(
            typ("button"),
            cls(
              "focus:shadow-outline-blue inline-flex w-full justify-center rounded-md border border-gray-300 bg-white px-4 py-2 text-base font-medium leading-6 text-gray-700 shadow-sm transition duration-150 ease-in-out hover:text-gray-500 focus:border-blue-300 focus:outline-none sm:text-sm sm:leading-5"
            ),
            "Zrušit",
            onClick.mapTo(SelectionCancelled) --> selectionUpdates
          )
        )
      )
    )
