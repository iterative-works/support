package works.iterative.ui.components.laminar.modules.listpage

import zio.Tag
import works.iterative.ui.components.laminar.HtmlTabular
import works.iterative.ui.components.laminar.tables.HtmlTableBuilderModule

trait ListPage[T: Tag: HtmlTabular]
    extends ListPageModel[T]
    with ListPageZIOHandler[T]
    with ListPageView[T]
    with ListPageComponent[T]
    with HtmlTableBuilderModule
