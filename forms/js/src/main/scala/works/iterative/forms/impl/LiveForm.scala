package portaly.forms
package impl

import com.raquo.laminar.api.L.*
import works.iterative.ui.model.forms.{AbsolutePath, IdPath}

trait LiveForm:
    def id: FormIdent
    def data: Signal[ValidationState[FormR]]
    def rawData: Signal[FormR]
    def rawInput: Observer[FormR]
    def buttonClicks: EventStream[IdPath]
    def element: HtmlElement
    def reset: Observer[Unit]
    def resetButton: HtmlElement
    def saveButton: HtmlElement
    def importButton: HtmlElement
    def nextButton: HtmlElement
    def previousButton: HtmlElement
    def doneButton(onDone: Observer[FormR]): HtmlElement
    def switchTo: Observer[AbsolutePath]
    def showErrors: Observer[Boolean]
    def control: Observer[FormControl]
    // TODO: use lifecycle events instead of this
    def aroundDone(advice: FormR => FormR): LiveForm
    def aroundDone(advice: Option[FormR => FormR]): LiveForm
end LiveForm
