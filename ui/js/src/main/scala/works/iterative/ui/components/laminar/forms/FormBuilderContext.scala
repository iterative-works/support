package works.iterative.ui.components.laminar.forms

trait FormBuilderContext:
  def formUIFactory: FormUIFactory
  def formMessagesResolver: FormMessagesResolver
