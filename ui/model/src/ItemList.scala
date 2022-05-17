package works.iterative
package ui.model

import core.*

case class ItemList(
    items: List[ListSection]
)

case class ItemProp(
    text: PlainOneLine,
    icon: Option[Icon] = None
)

case class ListSection(
    title: PlainOneLine,
    items: List[ListItem]
)

case class ListItem(
    title: PlainOneLine,
    href: String,
    label: Option[Label] = None,
    leftProps: List[ItemProp] = Nil,
    rightProp: Option[ItemProp] = None,
    categories: List[String] = Nil
)
