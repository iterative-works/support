package works.iterative.ui.model

case class ItemList(
    items: List[ListSection]
)

case class ItemProp(
    text: String,
    icon: Option[Icon] = None
)

case class ListSection(
    title: String,
    items: List[ListItem]
)

case class ListItem(
    title: String,
    href: String,
    label: Option[Label] = None,
    leftProps: List[ItemProp] = Nil,
    rightProp: Option[ItemProp] = None,
    categories: List[String] = Nil
)
