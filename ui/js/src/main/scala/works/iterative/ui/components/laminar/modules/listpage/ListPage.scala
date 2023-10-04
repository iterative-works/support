package works.iterative.ui.components.laminar.modules.listpage

import zio.Tag
import works.iterative.ui.components.laminar.HtmlTabular
import works.iterative.ui.components.laminar.tables.HtmlTableBuilderModule

trait ListPage[T: Tag: HtmlTabular, Q: Tag]
    extends ListPageModel[T, Q]
    with ListPageZIOHandler[T, Q]
    with ListPageView[T]
    with ListPageComponent[T]
    with HtmlTableBuilderModule
