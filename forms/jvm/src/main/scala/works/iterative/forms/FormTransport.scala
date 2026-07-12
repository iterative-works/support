// PURPOSE: Transport wiring for the SSR form — the attributes that connect the rendered form
// PURPOSE: element to whatever carries its change re-renders and submissions (HTMX, Datastar, ...)

package works.iterative.forms

import scalatags.Text.all.*

/** Supplies the form element's transport attributes. The renderer emits the plain POST contract
  * (`method`, `action`, `novalidate`) itself; a transport adds the attributes that route change
  * re-renders and submissions through its own machinery instead of native navigation.
  */
trait FormTransport:
    def formAttributes(postAction: String): Seq[Modifier]
end FormTransport

object FormTransport:

    /** Only committed-choice controls trigger a re-render: replacing the form on text-field change
      * would wipe values typed while the request was in flight. Every transport renders this one
      * list into its own trigger syntax.
      */
    val rerenderSelectors: List[String] =
        List("select", "input[type='checkbox']", "input[type='radio']", "input[type='date']")

    /** HTMX wiring: the form posts itself and swaps its own outerHTML on committed changes. */
    val htmx: FormTransport = new FormTransport:
        def formAttributes(postAction: String): Seq[Modifier] = Seq(
            attr("hx-post") := postAction,
            attr("hx-trigger") :=
                rerenderSelectors.map(sel => s"change from:$sel").mkString(", "),
            attr("hx-target") := "this",
            attr("hx-swap") := "outerHTML"
        )
end FormTransport
