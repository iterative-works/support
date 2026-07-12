// PURPOSE: The proof form both scenario variants render — SSR and SPA must behave identically on it
// PURPOSE: One declaration, one message catalogue, one initial state; divergence between variants is a bug

package works.iterative.forms.scenarios

import works.iterative.core.{Language, MessageCatalogue, MessageId, UserMessage}
import works.iterative.forms.*
import scala.util.Try

object InquiryProofForm:

    val declaration: Form = Form("inquiry", "1")(
        Field("token", FieldType("hidden"), default = Some("proof")),
        Section("customer")(
            Field("name"),
            Field("email", FieldType("email"), validations = List(Validation.Email)),
            Field("note", FieldType("prose"), optional = true)
        ),
        Section("request")(
            Enum("kind", default = Some("quote"))("quote", "order"),
            Enum.bool("urgent", default = Some(false)),
            ShowIf(
                Condition.IsEqual(".inquiry.request.kind", "order"),
                Section("delivery")(Field("address"))
            ),
            Date("deadline"),
            Display("summary")
        ),
        Repeated("items", default = Some("i1" -> "row"), optional = true)(
            Section("row")(
                Field("qty", FieldType("number")),
                Field("desc"),
                Button("remove")
            )
        ),
        Section("controls")(Button("addItem"))
    )

    val initialItems: Map[String, Seq[String]] = Map("inquiry.items.__items" -> Seq("i1:row"))

    private val messageMap: Map[String, String] = Map(
        "inquiry.title" -> "Inquiry",
        "inquiry.submit" -> "Send inquiry",
        "inquiry.customer.section" -> "Customer",
        "inquiry.customer.name.label" -> "Name",
        "inquiry.customer.email.label" -> "E-mail",
        "inquiry.customer.note.label" -> "Note",
        "inquiry.request.section" -> "Request",
        "inquiry.request.kind.label" -> "Kind",
        "inquiry.request.kind.quote.label" -> "Quote",
        "inquiry.request.kind.order.label" -> "Order",
        "inquiry.request.urgent.label" -> "Urgent",
        "inquiry.request.urgent.true.label" -> "Yes",
        "inquiry.request.urgent.false.label" -> "No",
        "inquiry.request.delivery.section" -> "Delivery",
        "inquiry.request.delivery.address.label" -> "Address",
        "inquiry.request.deadline.label" -> "Deadline",
        "inquiry.request.summary.title" -> "Summary",
        "inquiry.row.section" -> "Item %d",
        "inquiry.row.qty.label" -> "Quantity",
        "inquiry.row.desc.label" -> "Description",
        "inquiry.row.remove.label" -> "Remove item",
        "inquiry.row.remove.button" -> "Remove item",
        "inquiry.items.row.add.button" -> "Add item",
        "inquiry.controls.addItem.label" -> "Add item",
        "inquiry.controls.addItem.button" -> "Add item",
        "error.field.required" -> "Please fill in %s",
        "error.field.email" -> "%s is not a valid e-mail address"
    )

    /** Map-backed catalogue formatting like the server-side InMemoryMessageCatalogue, portable to
      * ScalaJS so both variants resolve the same texts.
      */
    val messages: MessageCatalogue = new MessageCatalogue:
        override val language: Language = Language.EN
        override def get(id: MessageId): Option[String] = messageMap.get(id.toString)
        override def get(msg: UserMessage): Option[String] =
            get(msg.id).map(template => Try(template.format(msg.args*)).getOrElse(template))
        override val root: MessageCatalogue = this
end InquiryProofForm
