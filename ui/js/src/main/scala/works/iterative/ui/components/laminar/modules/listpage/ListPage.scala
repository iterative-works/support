package works.iterative.ui
package components
package laminar
package modules
package listpage

import zio.Tag
import works.iterative.ui.components.laminar.tables.HtmlTableBuilderModule

@scala.annotation.nowarn("msg=unused implicit parameter")
trait ListPage[T: Tag: HtmlTabular, Q: Tag]
    extends ListPageModel[T, Q]
    with ListPageZIOHandler[T, Q]
    with ListPageView[T]
    with ListPageComponent[T]
    with HtmlTableBuilderModule
    with ComputableComponents
