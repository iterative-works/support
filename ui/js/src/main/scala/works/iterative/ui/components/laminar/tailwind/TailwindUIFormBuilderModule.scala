package works.iterative.ui.components.laminar.tailwind.ui

import works.iterative.ui.components.laminar.forms.FormBuilderModule
import works.iterative.ui.components.laminar.forms.FormMessagesResolver
import works.iterative.ui.components.ComponentContext
import works.iterative.ui.components.laminar.forms.FormUIFactory
import works.iterative.ui.components.laminar.forms.FormBuilderContext

trait TailwindUIFormBuilderModule extends FormBuilderModule

object TailwindUIFormBuilderModule:
  given (using ctx: ComponentContext[_]): FormBuilderContext with
    override def formMessagesResolver: FormMessagesResolver =
      summon[FormMessagesResolver]
    override def formUIFactory: FormUIFactory = TailwindUIFormUIFactory
