package works.iterative.ui.components.laminar.tables

import works.iterative.ui.components.ComponentContext
import works.iterative.core.MessageCatalogue

opaque type TableHeaderResolver = String => String

object TableHeaderResolver extends LowPriorityTableHeaderResolverImplicits:
    def apply(resolver: String => String): TableHeaderResolver = resolver

    given (using ctx: ComponentContext[?]): TableHeaderResolver =
        name => ctx.messages(name)

    given (using cat: MessageCatalogue): TableHeaderResolver =
        name => cat(name)

    extension (v: TableHeaderResolver) def apply(name: String): String = v(name)
end TableHeaderResolver

trait LowPriorityTableHeaderResolverImplicits:
    given default: TableHeaderResolver = TableHeaderResolver(identity[String])
