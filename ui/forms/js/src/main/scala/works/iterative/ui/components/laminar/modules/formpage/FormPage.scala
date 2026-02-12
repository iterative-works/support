package works.iterative.ui
package components
package laminar
package modules
package formpage

import zio.Tag
import works.iterative.ui.components.laminar.forms.Form
import works.iterative.ui.components.laminar.forms.FormBuilderModule

@scala.annotation.nowarn("msg=unused implicit parameter")
trait FormPage[T: Tag: Form, K]
    extends FormPageModel[T]
    with FormPageZIOHandler[T, K]
    with FormPageView[T]
    with FormPageComponent[T]
    with FormBuilderModule
    with ComputableComponents
