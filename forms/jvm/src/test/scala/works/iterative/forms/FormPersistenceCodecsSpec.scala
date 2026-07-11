// PURPOSE: Characterization tests pinning the JSON wire format of form definitions and submission values
// PURPOSE: The Form ADT is the wire format (stored in client DBs); these tests guard it through the consolidation

package portaly.forms

import zio.json.*
import zio.test.*
import works.iterative.ui.model.forms.IdPath
import portaly.forms.impl.FormR

object FormPersistenceCodecsSpec extends ZIOSpecDefault:
    import service.impl.rest.FormPersistenceCodecs.given

    val richForm: Form = Form("demo", "1")(
        Section("contact")(
            Field("name"),
            Field("token", FieldType("hidden"), default = Some("s3cret")),
            Field("email", FieldType("email"), optional = true),
            Enum("subscribe", default = Some("false"))("true", "false"),
            Date("birth"),
            File("attachments", multiple = true, optional = true),
            Button("lookup"),
            Display("info")
        ),
        ShowIf(
            Condition.AllOf(
                Condition.IsEqual(".demo.contact.subscribe", "true"),
                Condition.NonEmpty(".demo.contact.name")
            ),
            Section("newsletter")(Field("frequency"))
        ),
        Repeated("items", default = Some("first" -> "row"), optional = true)(
            Section("row")(Field("qty"))
        )
    )

    def spec = suite("Form wire format characterization")(
        test("a form with every segment kind round-trips through JSON") {
            val json = richForm.toJson
            val back = json.fromJson[Form]
            assertTrue(back == Right(richForm))
        },
        test("field type serializes as an object and decodes from both object and bare string") {
            val ft = FieldType("email")
            assertTrue(
                ft.toJson == """{"id":"email","disabled":false}""",
                """{"id":"email","disabled":false}""".fromJson[FieldType] == Right(ft),
                "\"email\"".fromJson[FieldType] == Right(FieldType("email"))
            )
        },
        test("relative paths serialize as dot-joined strings") {
            val p = IdPath("contact.name")
            assertTrue(
                p.toJson == "\"contact.name\"",
                "\"contact.name\"".fromJson[works.iterative.ui.model.forms.RelativePath] == Right(p)
            )
        },
        test("form values decode from tagged objects and from legacy bare strings") {
            val tagged = """{"StringValue":{"value":"hello"}}"""
            assertTrue(
                tagged.fromJson[FormValue] == Right(FormValue.StringValue("hello")),
                "\"hello\"".fromJson[FormValue] == Right(FormValue.StringValue("hello")),
                (FormValue.StringValue("hello"): FormValue).toJson == tagged
            )
        },
        test("a field without a validations key decodes with no declared validations") {
            // Stored declarations predate the vocabulary; zio-json must apply the default
            val legacy =
                """{"Field":{"id":"name","fieldType":{"id":"string","disabled":false},"optional":false}}"""
            val decoded = legacy.fromJson[SectionSegment]
            assertTrue(decoded == Right(Field("name")), decoded.map {
                case f: Field => f.validations
                case _        => List(Validation.Required)
            } == Right(Nil))
        },
        test("declared validations round-trip and pin their wire shape") {
            val field: SectionSegment = Field(
                "email",
                FieldType("email"),
                validations = List(
                    Validation.Required,
                    Validation.Email,
                    Validation.Pattern(".+@example.com"),
                    Validation.MinLength(3),
                    Validation.MaxLength(64),
                    Validation.Rule("ares", Map("country" -> "CZ"))
                )
            )
            val json = field.toJson
            assertTrue(
                json.fromJson[SectionSegment] == Right(field),
                json.contains("\"Required\""),
                json.contains("""{"regex":".+@example.com"}""")
            )
        },
        test("conditions round-trip including nested combinators") {
            val c: Condition = Condition.AnyOf(
                Condition.Never,
                Condition.AllOf(Condition.IsValid("a"), Condition.NonEmpty("^.b"))
            )
            assertTrue(c.toJson.fromJson[Condition] == Right(c))
        },
        test("FormR round-trips string data but flattens all values to strings") {
            val data = FormR(Map(
                IdPath("contact.name") -> List("John"),
                IdPath("contact.tags") -> List("a", "b")
            ))
            val back = data.toJson.fromJson[FormR]
            assertTrue(
                back.map(_.getString(IdPath.full("contact.name"))) == Right(Some("John")),
                back.map(_.getStringList(IdPath.full("contact.tags"))) == Right(Some(List("a", "b")))
            )
        },
        test("SavedForm envelope round-trips") {
            val saved = service.SavedForm(
                "f1",
                "1",
                "formr.v1+json",
                """{"a":["1"]}""",
                java.time.Instant.EPOCH
            )
            assertTrue(saved.toJson.fromJson[service.SavedForm] == Right(saved))
        }
    )
end FormPersistenceCodecsSpec
