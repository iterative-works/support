// PURPOSE: Pins the conformance corpus to the vocabulary — a new enum case without a sample is red
// PURPOSE: Also proves the corpus' visibility expectations against Condition.eval itself

package works.iterative.forms.scenarios

import zio.test.*
import works.iterative.forms.*
import works.iterative.ui.model.forms.IdPath
import scala.deriving.Mirror
import scala.compiletime.constValue

object ConformanceCorpusSpec extends ZIOSpecDefault:

    inline def caseCount[T](using m: Mirror.SumOf[T]): Int =
        constValue[Tuple.Size[m.MirroredElemTypes]]

    def spec = suite("ConformanceCorpus")(
        test("covers every FieldKind case") {
            val m = summon[Mirror.SumOf[FieldKind]]
            assertTrue(
                ConformanceCorpus.fieldKinds.map(m.ordinal).distinct.size == caseCount[FieldKind]
            )
        },
        test("covers every Validation case") {
            val m = summon[Mirror.SumOf[Validation]]
            assertTrue(
                ConformanceCorpus.validationSamples.map(s => m.ordinal(s.validation))
                    .distinct.size == caseCount[Validation]
            )
        },
        test("covers every Condition case") {
            val m = summon[Mirror.SumOf[Condition]]
            assertTrue(
                ConformanceCorpus.conditionSamples.map(s => m.ordinal(s.condition))
                    .distinct.size == caseCount[Condition]
            )
        },
        test("field ids are unique and path-safe") {
            val ids = ConformanceCorpus.fieldKinds.map(ConformanceCorpus.fieldId)
            assertTrue(
                ids.distinct.size == ids.size,
                ids.forall(id => !id.contains(':') && !id.contains('.'))
            )
        },
        test("visibility expectations match Condition.eval") {
            val base = IdPath.Root / "conditioned"
            check(Gen.fromIterable(ConformanceCorpus.conditionSamples)) { sample =>
                val values = ConformanceCorpus.conditionData(sample)
                    .map((k, v) => IdPath.full(k) -> v.head)
                val invalid = sample.invalid.map(s => IdPath.full(s"conditioned.$s"))
                assertTrue(
                    Condition.eval(
                        sample.condition,
                        base,
                        values.get,
                        p => !invalid.contains(p)
                    ) == sample.visible,
                    Condition.eval(
                        sample.condition,
                        base,
                        values.get,
                        Condition.alwaysValid
                    ) == sample.visibleWhenAlwaysValid
                )
            }
        }
    )
end ConformanceCorpusSpec
