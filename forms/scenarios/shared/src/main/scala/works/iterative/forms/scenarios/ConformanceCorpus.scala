// PURPOSE: The conformance corpus — one canonical sample per FieldKind, Validation and Condition case
// PURPOSE: Suites drive every interpreter over these samples; a vocabulary case without a sample is red

package works.iterative.forms.scenarios

import works.iterative.core.{Language, MessageCatalogue, MessageId, UserMessage}
import works.iterative.forms.*
import scala.util.Try

object ConformanceCorpus:

    // --- FieldKind: every case, Number expanded over all NumberKinds, Custom sampled twice ---

    val fieldKinds: List[FieldKind] = List(
        FieldKind.Text,
        FieldKind.Hidden,
        FieldKind.Date,
        FieldKind.Prose,
        FieldKind.Select,
        FieldKind.Checkbox,
        FieldKind.Number(NumberKind.Real),
        FieldKind.Number(NumberKind.Natural),
        FieldKind.Number(NumberKind.NonNegativeReal),
        FieldKind.Email,
        FieldKind.Phone,
        FieldKind.Zip,
        FieldKind.Country,
        FieldKind.Ruian,
        FieldKind.Custom("tel"),
        FieldKind.Custom("acme:widget")
    )

    /** A path-safe field id for a kind — wire ids may carry `:` which IdPath separates on. */
    def fieldId(kind: FieldKind): String = kind.wireId.replace(':', '_').replace('.', '_')

    def fieldFor(kind: FieldKind): Field = Field(fieldId(kind), FieldType(kind))

    /** One form holding a field of every kind, the per-kind dispatch surface of each interpreter.
      */
    val kindsForm: Form = Form("kinds", "1")(fieldKinds.map(fieldFor)*)

    // --- Validation: every case with a passing value and (where the case can fail) a failing one ---

    case class ValidationSample(
        validation: Validation,
        passing: String,
        failing: Option[String],
        errorKey: String
    )

    val validationSamples: List[ValidationSample] = List(
        // Required's failing shape is blankness, exercised by the required axis, not a filled value
        ValidationSample(Validation.Required, "filled", None, "error.field.required"),
        ValidationSample(Validation.Email, "a@b.cz", Some("not-an-email"), "error.field.email"),
        ValidationSample(Validation.Pattern("[0-9]+"), "123", Some("abc"), "error.field.pattern"),
        ValidationSample(Validation.MinLength(3), "abc", Some("ab"), "error.field.minlength"),
        ValidationSample(Validation.MaxLength(3), "abc", Some("abcd"), "error.field.maxlength"),
        // Rule binds at the edges behind a registry; without one it passes everywhere
        ValidationSample(Validation.Rule("conformance:even"), "2", None, "error.rule.even")
    )

    /** A one-field form declaring the sample's validation, the agreement surface of the validators.
      */
    def validationForm(sample: ValidationSample, optional: Boolean): Form =
        Form("validated", "1")(
            Field("value", optional = optional, validations = List(sample.validation))
        )

    // --- Condition: every case with the state it evaluates against and the expected visibility ---

    case class ConditionSample(
        label: String,
        condition: Condition,
        values: Map[String, String],
        invalid: Set[String],
        /** Visibility when the walker has a real validity view (UIFormBuilder). */
        visible: Boolean,
        /** Visibility for walkers that treat every field as valid (DeclaredValidation, encoder). */
        visibleWhenAlwaysValid: Boolean
    )

    object ConditionSample:
        def apply(
            label: String,
            condition: Condition,
            values: Map[String, String] = Map.empty,
            invalid: Set[String] = Set.empty,
            visible: Boolean
        ): ConditionSample =
            ConditionSample(
                label,
                condition,
                values,
                invalid,
                visible,
                visibleWhenAlwaysValid = if invalid.isEmpty then visible else true
            )
    end ConditionSample

    private val gatePath = ".conditioned.flags.gate"

    val conditionSamples: List[ConditionSample] = List(
        ConditionSample("Never hides", Condition.Never, visible = false),
        ConditionSample("Always shows", Condition.Always, visible = true),
        ConditionSample(
            "IsEqual matching value shows",
            Condition.IsEqual(gatePath, "on"),
            values = Map("flags.gate" -> "on"),
            visible = true
        ),
        ConditionSample(
            "IsEqual different value hides",
            Condition.IsEqual(gatePath, "on"),
            values = Map("flags.gate" -> "off"),
            visible = false
        ),
        ConditionSample(
            "NonEmpty filled value shows",
            Condition.NonEmpty(gatePath),
            values = Map("flags.gate" -> "something"),
            visible = true
        ),
        ConditionSample(
            "NonEmpty blank value hides — blanks are not presence",
            Condition.NonEmpty(gatePath),
            values = Map("flags.gate" -> "  "),
            visible = false
        ),
        ConditionSample(
            "NonEmpty missing value hides",
            Condition.NonEmpty(gatePath),
            visible = false
        ),
        ConditionSample(
            "IsValid valid field shows",
            Condition.IsValid(gatePath),
            values = Map("flags.gate" -> "anything"),
            visible = true
        ),
        ConditionSample(
            "IsValid invalid field hides where a validity view exists",
            Condition.IsValid(gatePath),
            values = Map("flags.gate" -> "anything"),
            invalid = Set("flags.gate"),
            visible = false
        ),
        ConditionSample("empty AnyOf hides", Condition.AnyOf(), visible = false),
        ConditionSample(
            "AnyOf shows when one branch holds",
            Condition.AnyOf(Condition.Never, Condition.IsEqual(gatePath, "on")),
            values = Map("flags.gate" -> "on"),
            visible = true
        ),
        ConditionSample("empty AllOf shows", Condition.AllOf(), visible = true),
        ConditionSample(
            "AllOf hides when one branch fails",
            Condition.AllOf(Condition.Always, Condition.Never),
            visible = false
        )
    )

    /** The form each condition sample gates: state fields, the gated marker, nothing else. */
    def conditionForm(sample: ConditionSample): Form = Form("conditioned", "1")(
        Section("flags")(Field("gate", optional = true), Field("other", optional = true)),
        ShowIf(sample.condition, Section("gated")(Field("marker", optional = true)))
    )

    /** The posted data for a condition sample: its state values plus a marker value, so walkers
      * that only emit filled fields still surface the gated content when visible.
      */
    def conditionData(sample: ConditionSample): Map[String, Seq[String]] =
        sample.values.map((k, v) => s"conditioned.$k" -> Seq(v))
            ++ Map("conditioned.gated.marker" -> Seq("present"))

    // --- Messages: labels and error texts the validation and rendering suites resolve ---

    private val messageMap: Map[String, String] = Map(
        "validated.value.label" -> "Value",
        "error.field.required" -> "Please fill in %s",
        "error.field.email" -> "%s is not a valid e-mail address",
        "error.field.pattern" -> "%s does not match the expected format",
        "error.field.minlength" -> "%s must have at least %s characters",
        "error.field.maxlength" -> "%s must have at most %s characters",
        "error.rule.even" -> "%s must be even"
    )

    val messages: MessageCatalogue = new MessageCatalogue:
        override val language: Language = Language.EN
        override def get(id: MessageId): Option[String] = messageMap.get(id.toString)
        override def get(msg: UserMessage): Option[String] =
            get(msg.id).map(template => Try(template.format(msg.args*)).getOrElse(template))
        override val root: MessageCatalogue = this
end ConformanceCorpus
