// PURPOSE: Tests for the guard algebra — Predicate, Guard, GuardFailure, GuardContext
// PURPOSE: Verifies sum-aware failure algebra and describe/check structural correspondence
package works.iterative.workflow

import zio.test.*
import zio.Scope
import works.iterative.core.MessageId
import works.iterative.core.auth.UserId

object GuardSpec extends ZIOSpecDefault:

    private case class TestEntity(hasData: Boolean, stav: String)

    private val hasDataPredicate = Predicate[TestEntity]("hasData", _.hasData)
    private val isInStatePredicate = Predicate[TestEntity]("inState", _.stav == "active")

    private val msg1 = MessageId("error.test.msg1")
    private val msg2 = MessageId("error.test.msg2")

    private def ctx(
        entity: TestEntity,
        roles: Set[TestRole] = Set.empty,
        userId: String = "test-user"
    ): GuardContext[TestRole, TestEntity] =
        GuardContext(roles, UserId.unsafe(userId), entity)

    private val entityWithData = TestEntity(hasData = true, stav = "active")
    private val entityWithoutData = TestEntity(hasData = false, stav = "inactive")

    override def spec: Spec[TestEnvironment & Scope, Any] = suite("Guard algebra")(
        suite("Predicate — named boolean test")(
            test("predicate true → apply returns true") {
                assertTrue(hasDataPredicate(entityWithData))
            },
            test("predicate false → apply returns false") {
                assertTrue(!hasDataPredicate(entityWithoutData))
            }
        ),
        suite("Requirement — data guard leaf")(
            test("true predicate → Right(())") {
                val guard = Guard.Requirement[TestRole, TestEntity](hasDataPredicate, msg1)
                assertTrue(guard.check(ctx(entityWithData)) == Right(()))
            },
            test("false predicate → Left(GuardFailure.Leaf(label, message))") {
                val guard = Guard.Requirement[TestRole, TestEntity](hasDataPredicate, msg1)
                val result = guard.check(ctx(entityWithoutData))
                assertTrue(result == Left(GuardFailure.Leaf("hasData", msg1)))
            }
        ),
        suite("RequireRole — auth guard, Unauthorized is distinct from Leaf")(
            test("role present → Right(())") {
                val guard = Guard.RequireRole[TestRole, TestEntity](TestRole.AdministratorZakazek)
                val result = guard.check(ctx(entityWithData, Set(TestRole.AdministratorZakazek)))
                assertTrue(result == Right(()))
            },
            test("role absent → Left(Unauthorized), NOT Left(Leaf)") {
                val guard = Guard.RequireRole[TestRole, TestEntity](TestRole.AdministratorZakazek)
                val result = guard.check(ctx(entityWithData, Set.empty))
                result match
                    case Left(GuardFailure.Unauthorized(_)) => assertTrue(true)
                    case Left(GuardFailure.Leaf(_, _))      => assertTrue(false)
                    case _                                  => assertTrue(false)
            },
            test("role absent → Left(Unauthorized(role.toString))") {
                val guard = Guard.RequireRole[TestRole, TestEntity](TestRole.AdministratorZakazek)
                val result = guard.check(ctx(entityWithData, Set.empty))
                assertTrue(result == Left(GuardFailure.Unauthorized("AdministratorZakazek")))
            }
        ),
        suite("RequireIdentity — auth guard with closure")(
            test("holds=true → Right(())") {
                val guard = Guard.RequireIdentity[TestRole, TestEntity]("assigned-vp", _.entity.hasData)
                assertTrue(guard.check(ctx(entityWithData)) == Right(()))
            },
            test("holds=false → Left(Unauthorized(label)), NOT Left(Leaf)") {
                val guard = Guard.RequireIdentity[TestRole, TestEntity]("assigned-vp", _.entity.hasData)
                val result = guard.check(ctx(entityWithoutData))
                assertTrue(result == Left(GuardFailure.Unauthorized("assigned-vp")))
            }
        ),
        suite("and — accumulates failures flatly into Conjunction")(
            test("both pass → Right(())") {
                val guard = Guard.Requirement[TestRole, TestEntity](hasDataPredicate, msg1) and
                    Guard.Requirement[TestRole, TestEntity](isInStatePredicate, msg2)
                assertTrue(guard.check(ctx(entityWithData)) == Right(()))
            },
            test("two failing data-leaves → Left(Conjunction(List(Leaf, Leaf)))") {
                val guard = Guard.Requirement[TestRole, TestEntity](hasDataPredicate, msg1) and
                    Guard.Requirement[TestRole, TestEntity](isInStatePredicate, msg2)
                val result = guard.check(ctx(entityWithoutData))
                assertTrue(
                    result == Left(
                        GuardFailure.Conjunction(
                            List(
                                GuardFailure.Leaf("hasData", msg1),
                                GuardFailure.Leaf("inState", msg2)
                            )
                        )
                    )
                )
            },
            test("mixed data+auth failure → Conjunction contains both Leaf and Unauthorized") {
                val guard = Guard.Requirement[TestRole, TestEntity](hasDataPredicate, msg1) and
                    Guard.RequireRole[TestRole, TestEntity](TestRole.AdministratorZakazek)
                val result = guard.check(ctx(entityWithoutData, Set.empty))
                result match
                    case Left(GuardFailure.Conjunction(failures)) =>
                        val hasLeaf  = failures.exists { case _: GuardFailure.Leaf  => true; case _ => false }
                        val hasUnauth = failures.exists { case _: GuardFailure.Unauthorized => true; case _ => false }
                        assertTrue(hasLeaf, hasUnauth)
                    case _ => assertTrue(false)
            },
            test("Conjunction(Conjunction(...)) does NOT nest — flat accumulation") {
                val g1       = Guard.Requirement[TestRole, TestEntity](hasDataPredicate, msg1)
                val g2       = Guard.Requirement[TestRole, TestEntity](isInStatePredicate, msg2)
                val g3       = Guard.Requirement[TestRole, TestEntity](hasDataPredicate, msg1)
                val combined = (g1 and g2) and g3
                val result   = combined.check(ctx(entityWithoutData))
                result match
                    case Left(GuardFailure.Conjunction(failures)) =>
                        val hasNestedConjunction =
                            failures.exists { case _: GuardFailure.Conjunction => true; case _ => false }
                        assertTrue(!hasNestedConjunction, failures.length == 3)
                    case _ => assertTrue(false)
            }
        ),
        suite("or — node-level Disjunction, one branch suffices")(
            test("first branch passes → Right(())") {
                val guard = Guard.RequireRole[TestRole, TestEntity](TestRole.AdministratorZakazek) or
                    Guard.Requirement[TestRole, TestEntity](hasDataPredicate, msg1)
                val result = guard.check(ctx(entityWithoutData, Set(TestRole.AdministratorZakazek)))
                assertTrue(result == Right(()))
            },
            test("second branch passes → Right(())") {
                val guard = Guard.RequireRole[TestRole, TestEntity](TestRole.AdministratorZakazek) or
                    Guard.Requirement[TestRole, TestEntity](hasDataPredicate, msg1)
                val result = guard.check(ctx(entityWithData, Set.empty))
                assertTrue(result == Right(()))
            },
            test("both branches fail → Left(Disjunction(List(lFail, rFail)))") {
                val guard = Guard.RequireRole[TestRole, TestEntity](TestRole.AdministratorZakazek) or
                    Guard.Requirement[TestRole, TestEntity](hasDataPredicate, msg1)
                val result = guard.check(ctx(entityWithoutData, Set.empty))
                result match
                    case Left(GuardFailure.Disjunction(failures)) => assertTrue(failures.length == 2)
                    case _                                        => assertTrue(false)
            },
            test("AZ or (VP and stav) — passes for AZ regardless of entity state") {
                val azGuard    = Guard.RequireRole[TestRole, TestEntity](TestRole.AdministratorZakazek)
                val vpGuard    = Guard.RequireIdentity[TestRole, TestEntity]("přiřazený VP", _.entity.hasData)
                val stavPred   = Predicate[TestEntity]("inState", _.stav == "active")
                val combined   = azGuard or (vpGuard and Guard.Requirement[TestRole, TestEntity](stavPred, msg1))
                val result     = combined.check(ctx(entityWithoutData, Set(TestRole.AdministratorZakazek)))
                assertTrue(result == Right(()))
            },
            test("AZ or (VP and stav) — passes for VP with entity.hasData=true and stav=active") {
                val azGuard  = Guard.RequireRole[TestRole, TestEntity](TestRole.AdministratorZakazek)
                val vpGuard  = Guard.RequireIdentity[TestRole, TestEntity]("přiřazený VP", _.entity.hasData)
                val stavPred = Predicate[TestEntity]("inState", _.stav == "active")
                val combined = azGuard or (vpGuard and Guard.Requirement[TestRole, TestEntity](stavPred, msg1))
                val result   = combined.check(ctx(entityWithData, Set.empty))
                assertTrue(result == Right(()))
            },
            test("AZ or (VP and stav) — fails for unrelated role → Left(Disjunction)") {
                val azGuard  = Guard.RequireRole[TestRole, TestEntity](TestRole.AdministratorZakazek)
                val vpGuard  = Guard.RequireIdentity[TestRole, TestEntity]("přiřazený VP", _.entity.hasData)
                val stavPred = Predicate[TestEntity]("inState", _.stav == "active")
                val combined = azGuard or (vpGuard and Guard.Requirement[TestRole, TestEntity](stavPred, msg1))
                val result   = combined.check(ctx(entityWithoutData, Set(TestRole.Pracovnik)))
                result match
                    case Left(GuardFailure.Disjunction(_)) => assertTrue(true)
                    case _                                 => assertTrue(false)
            }
        ),
        suite("describe corresponds to evaluation structure")(
            test("Requirement → GuardDescription.Leaf(predicate.label)") {
                val guard = Guard.Requirement[TestRole, TestEntity](hasDataPredicate, msg1)
                assertTrue(guard.describe == GuardDescription.Leaf("hasData"))
            },
            test("RequireRole → GuardDescription.Leaf(role.toString)") {
                val guard = Guard.RequireRole[TestRole, TestEntity](TestRole.AdministratorZakazek)
                assertTrue(guard.describe == GuardDescription.Leaf("AdministratorZakazek"))
            },
            test("RequireIdentity → GuardDescription.Leaf(label)") {
                val guard = Guard.RequireIdentity[TestRole, TestEntity]("přiřazený VP", _ => true)
                assertTrue(guard.describe == GuardDescription.Leaf("přiřazený VP"))
            },
            test("And → GuardDescription.All(List(left.describe, right.describe))") {
                val guard = Guard.Requirement[TestRole, TestEntity](hasDataPredicate, msg1) and
                    Guard.Requirement[TestRole, TestEntity](isInStatePredicate, msg2)
                assertTrue(
                    guard.describe == GuardDescription.All(
                        List(GuardDescription.Leaf("hasData"), GuardDescription.Leaf("inState"))
                    )
                )
            },
            test("(And and g3).describe is flat All with 3 parts — mirrors flat Conjunction check") {
                val g1       = Guard.Requirement[TestRole, TestEntity](hasDataPredicate, msg1)
                val g2       = Guard.Requirement[TestRole, TestEntity](isInStatePredicate, msg2)
                val g3       = Guard.Requirement[TestRole, TestEntity](hasDataPredicate, msg1)
                val combined = (g1 and g2) and g3
                combined.describe match
                    case GuardDescription.All(parts) =>
                        val hasNestedAll =
                            parts.exists { case _: GuardDescription.All => true; case _ => false }
                        assertTrue(!hasNestedAll, parts.length == 3)
                    case _ => assertTrue(false)
            },
            test("Or → GuardDescription.Any(List(left.describe, right.describe))") {
                val guard = Guard.RequireRole[TestRole, TestEntity](TestRole.AdministratorZakazek) or
                    Guard.Requirement[TestRole, TestEntity](hasDataPredicate, msg1)
                assertTrue(
                    guard.describe == GuardDescription.Any(
                        List(
                            GuardDescription.Leaf("AdministratorZakazek"),
                            GuardDescription.Leaf("hasData")
                        )
                    )
                )
            }
        ),
        suite("always — trivially passes")(
            test("always.check → Right(()) regardless of context") {
                val guard = Guard.always[TestRole, TestEntity]
                assertTrue(guard.check(ctx(entityWithoutData)) == Right(()))
            }
        )
    )
end GuardSpec
