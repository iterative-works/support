// PURPOSE: Pins the FieldKind wire codec over the client-repo field-type audit (FC-D6)
// PURPOSE: Closed ids round-trip through of/wireId; unknown ids stay Custom byte-exactly

package works.iterative.forms

import zio.test.*

object FieldKindSpec extends ZIOSpecDefault:

    val closedVocabulary: Map[String, FieldKind] = Map(
        "string" -> FieldKind.Text,
        "hidden" -> FieldKind.Hidden,
        "date" -> FieldKind.Date,
        "prose" -> FieldKind.Prose,
        "select" -> FieldKind.Select,
        "checkbox" -> FieldKind.Checkbox,
        "number" -> FieldKind.Number(NumberKind.Real),
        "number:natural" -> FieldKind.Number(NumberKind.Natural),
        "number:real_non_negative" -> FieldKind.Number(NumberKind.NonNegativeReal),
        "base:email" -> FieldKind.Email,
        "base:phone" -> FieldKind.Phone,
        "base:zip" -> FieldKind.Zip,
        "base:country" -> FieldKind.Country,
        "base:ruian" -> FieldKind.Ruian
    )

    // Every field-type id observed in cmi-portaly and medeca-modul-poptavky that is
    // not part of the closed vocabulary; czech:ico moves to czech-support (FC-D3)
    val customVocabulary: List[String] = List(
        "czech:ico",
        "cmi:meridlo_druh",
        "cmi:meridlo_evidcislo",
        "cmi:meridlo_nazev",
        "cmi:meridlo_typ",
        "cmi:meridlo_vyrcislo",
        "cmi:meridlo_vyrobce",
        "cmi:preferovane_datum",
        "cmi:rozsah_zadosti",
        "cmi:srn",
        "cmi:erp_cenik:MER",
        "cmi:erp_cenik_kategorie",
        "medeca:invazivnost",
        "medeca:jazyk_dokumentace",
        "medeca:jazyk_komunikace",
        "medeca:klasifikace_zp",
        "medeca:kombinace_prostredku",
        "medeca:mda_mdn",
        "medeca:mds",
        "medeca:mdt",
        "medeca:mds_mdt",
        "medeca:podani_matrix_docs",
        "medeca:podani_soubory",
        "medeca:postup",
        "medeca:pravidlo",
        "medeca:sterilita"
    )

    def spec = suite("FieldKind")(
        test("closed wire ids decode to their kinds and encode back byte-exactly") {
            assertTrue(closedVocabulary.forall((id, kind) =>
                FieldKind.of(id) == kind && kind.wireId == id
            ))
        },
        test("alias ids decode to the closed kind; wireId picks the canonical spelling") {
            assertTrue(
                FieldKind.of("text") == FieldKind.Text,
                FieldKind.of("email") == FieldKind.Email,
                FieldKind.Text.wireId == "string",
                FieldKind.Email.wireId == "base:email"
            )
        },
        test("client-audit custom ids stay Custom and round-trip byte-exactly") {
            assertTrue(customVocabulary.forall(id =>
                FieldKind.of(id) == FieldKind.Custom(id) && FieldKind.of(id).wireId == id
            ))
        },
        test("a typo becomes a visible Custom instead of silently matching nothing") {
            assertTrue(FieldKind.of("medeca:mds_mdt") == FieldKind.Custom("medeca:mds_mdt"))
        },
        test("FieldType exposes the kind and constructs from it") {
            assertTrue(
                FieldType("base:email").kind == FieldKind.Email,
                FieldType("text").kind == FieldKind.Text,
                FieldType(FieldKind.Email) == FieldType("base:email"),
                FieldType(FieldKind.Hidden).hidden,
                FieldType("hidden").hidden,
                !FieldType("string").hidden
            )
        }
    )
end FieldKindSpec
