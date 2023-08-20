package works.iterative.ui.components.laminar.modules.formpage

import zio.Tag
import works.iterative.ui.components.laminar.forms.Form
import works.iterative.ui.components.laminar.forms.FormBuilderModule

trait FormPage[T: Tag: Form, K]
    extends FormPageModel[T]
    with FormPageZIOHandler[T, K]
    with FormPageView[T]
    with FormPageComponent[T]
    with FormBuilderModule
