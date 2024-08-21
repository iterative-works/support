package portaly.forms
package impl

import com.raquo.laminar.api.L.*
import works.iterative.core.MessageCatalogue
import works.iterative.ui.model.forms.IdPath

extension (idPath: IdPath)
    def toMessageNode(itemType: String)(using messages: MessageCatalogue): Node =
        idPath.toMessage(itemType)
    def toMessageNodeOpt(itemType: String)(using
        messages: MessageCatalogue
    ): Option[Node] =
        idPath.toMessageOpt(itemType).map(i =>
            span(dataAttr("msgId")(s"${idPath.toHtmlName}.$itemType"), i)
        )
end extension
