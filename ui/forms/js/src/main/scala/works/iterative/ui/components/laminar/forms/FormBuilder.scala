package works.iterative.ui.components.laminar.forms

trait FormBuilder[A]:
  def build(initialValue: Option[A]): FormComponent[A]
