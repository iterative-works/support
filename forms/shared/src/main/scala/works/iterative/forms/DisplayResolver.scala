package portaly
package forms
import works.iterative.ui.model.forms.IdPath
import works.iterative.core.MessageCatalogue
import works.iterative.core.Language

trait DisplayResolver[State, Output]:
    def resolve(id: IdPath, state: State)(using MessageCatalogue, Language): Output
