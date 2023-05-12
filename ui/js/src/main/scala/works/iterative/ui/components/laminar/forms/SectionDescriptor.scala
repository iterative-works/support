package works.iterative.ui.components.laminar.forms

import works.iterative.ui.components.tailwind.ComponentContext

trait SectionDescriptor:
  def title: String
  def subtitle: Option[String]

object SectionDescriptor:
  def apply(id: String)(using ctx: ComponentContext[_]): SectionDescriptor =
    new SectionDescriptor:
      override def title: String = ctx.messages(id)
      override def subtitle: Option[String] =
        ctx.messages.get(id + ".subtitle")
