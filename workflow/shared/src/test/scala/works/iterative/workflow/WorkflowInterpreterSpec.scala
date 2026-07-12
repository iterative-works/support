// PURPOSE: Tests for the engine outcome model (CommandMatch/EventShape/Outcome/Transition/
// PURPOSE: EngineDefinition), Guard.stripAuth, and WorkflowInterpreter.decide's three stages
package works.iterative.workflow

import zio.test.*
import zio.Scope
import works.iterative.core.MessageId
import works.iterative.core.auth.UserId

object WorkflowInterpreterSpec extends ZIOSpecDefault:

    // ─── Synthetic domain shared across the matcher/model suites ──────────────────

    private enum TState:
        case Draft, Published

    private enum TCommand:
        case Toggle
        case Publish(note: String)
        case Ignore

    private enum TEvent:
        case Toggled
        case PublishedEvt(note: String)

    private case class TEntity(state: TState, ready: Boolean)

    private val toggleMatch = CommandMatch[TCommand]("toggle", { case TCommand.Toggle => () })
    private val toggledShape = EventShape[TEvent]("toggled", { case TEvent.Toggled => () })

    private val entity = TEntity(TState.Draft, ready = true)

    private def userCtx(e: TEntity, roles: Set[TestRole] = Set.empty): GuardContext[TestRole, TEntity] =
        GuardContext(roles, UserId.unsafe("test-user"), e)

    // ─── Synthetic domain for the interpreter (decide) suites ──────────────────────

    private enum WState:
        case Draft, Published, Archived

    private enum WCommand:
        case Publish
        case PublishWithNote(note: String)
        case Archive

    private enum WEvent:
        case PublishedEvt
        case PublishedWithNoteEvt(note: String)
        case ArchivedEvt

    private case class WEntity(state: WState, ready: Boolean, approved: Boolean)

    private val readyPredicate    = Predicate[WEntity]("ready", _.ready)
    private val approvedPredicate = Predicate[WEntity]("approved", _.approved)

    private val notReadyMsg    = MessageId("test.notReady")
    private val notApprovedMsg = MessageId("test.notApproved")

    private val publishMatch = CommandMatch[WCommand]("publish", { case WCommand.Publish => () })

    private val publishedShape = EventShape[WEvent]("published", { case WEvent.PublishedEvt => () })

    private def wCtx(e: WEntity, roles: Set[TestRole] = Set.empty): GuardContext[TestRole, WEntity] =
        GuardContext(roles, UserId.unsafe("test-user"), e)

    override def spec: Spec[TestEnvironment & Scope, Any] = suite("WorkflowInterpreter kernel")(
        suite("CommandMatch — named command matcher")(
            test("accepts is true for a matching command") {
                assertTrue(toggleMatch.accepts(TCommand.Toggle))
            },
            test("accepts is false, never throws, for a non-matching command") {
                assertTrue(
                    !toggleMatch.accepts(TCommand.Publish("x")),
                    !toggleMatch.accepts(TCommand.Ignore)
                )
            }
        ),
        suite("EventShape — named event-type matcher")(
            test("matches is true for a matching event") {
                assertTrue(toggledShape.matches(TEvent.Toggled))
            },
            test("matches is false, never throws, for a non-matching event") {
                assertTrue(!toggledShape.matches(TEvent.PublishedEvt("x")))
            }
        ),
        suite("Outcome — one deterministic outcome")(
            test("field access round-trips label/to/emits") {
                val outcome = Outcome[TState, TEntity, TCommand, TEvent](
                    label = "only",
                    to = TState.Published,
                    emits = List(toggledShape),
                    emit = (_, _) => Seq(TEvent.Toggled)
                )
                assertTrue(
                    outcome.label == "only",
                    outcome.to == TState.Published,
                    outcome.emits == List(toggledShape)
                )
            },
            test("no-op outcome (emits = Nil, emit = (_, _) => Seq.empty) yields Seq.empty") {
                val noop = Outcome[TState, TEntity, TCommand, TEvent](
                    label = "noop",
                    to = TState.Draft,
                    emits = Nil,
                    emit = (_, _) => Seq.empty
                )
                assertTrue(noop.emit(entity, TCommand.Ignore) == Seq.empty)
            }
        ),
        suite("Transition — key and defaulted requiresPayload")(
            test("key == (from, on.label)") {
                val outcome = Outcome[TState, TEntity, TCommand, TEvent]("only", TState.Published, Nil, (_, _) => Seq.empty)
                val t = Transition[TestRole, TState, TEntity, TCommand, TEvent](
                    from = TState.Draft,
                    on = toggleMatch,
                    applicableWhen = Guard.always,
                    requires = Guard.always,
                    outcomes = List(outcome),
                    classify = (_, _) => outcome
                )
                assertTrue(t.key == (TState.Draft, "toggle"))
            },
            test("requiresPayload defaults to (_, _) => Guard.always") {
                val outcome = Outcome[TState, TEntity, TCommand, TEvent]("only", TState.Published, Nil, (_, _) => Seq.empty)
                val t = Transition[TestRole, TState, TEntity, TCommand, TEvent](
                    from = TState.Draft,
                    on = toggleMatch,
                    applicableWhen = Guard.always,
                    requires = Guard.always,
                    outcomes = List(outcome),
                    classify = (_, _) => outcome
                )
                val defaultGuard = t.requiresPayload(entity, TCommand.Toggle)
                assertTrue(defaultGuard.check(userCtx(entity)) == Right(()))
            }
        ),
        suite("Transition.simple — terse single-outcome constructor")(
            test("builds a single-outcome transition labelled 'only'") {
                val t = Transition.simple[TestRole, TState, TEntity, TCommand, TEvent](
                    from = TState.Draft,
                    on = toggleMatch,
                    to = TState.Published,
                    emits = List(toggledShape),
                    emit = (_, _) => Seq(TEvent.Toggled)
                )
                assertTrue(
                    t.outcomes.map(_.label) == List("only"),
                    t.outcomes.map(_.to) == List(TState.Published),
                    t.classify(entity, TCommand.Toggle).label == "only"
                )
            },
            test("requiresPayload defaults to (_, _) => Guard.always") {
                val t = Transition.simple[TestRole, TState, TEntity, TCommand, TEvent](
                    from = TState.Draft,
                    on = toggleMatch,
                    to = TState.Published,
                    emits = List(toggledShape),
                    emit = (_, _) => Seq(TEvent.Toggled)
                )
                assertTrue(t.requiresPayload(entity, TCommand.Toggle).check(userCtx(entity)) == Right(()))
            }
        ),
        suite("EngineDefinition — stateOf and transitions round-trip")(
            test("order is preserved") {
                val t1 = Transition.simple[TestRole, TState, TEntity, TCommand, TEvent](
                    TState.Draft, toggleMatch, TState.Published, Nil, (_, _) => Seq.empty
                )
                val t2 = Transition.simple[TestRole, TState, TEntity, TCommand, TEvent](
                    TState.Published, toggleMatch, TState.Draft, Nil, (_, _) => Seq.empty
                )
                val defn = EngineDefinition[TestRole, TState, TEntity, TCommand, TEvent](_.state, List(t1, t2))
                assertTrue(
                    defn.transitions == List(t1, t2),
                    defn.stateOf(TEntity(TState.Draft, ready = true)) == TState.Draft
                )
            }
        ),
        suite("Guard.stripAuth — auth leaf tree-rewrite")(
            test("stripAuth(RequireRole) behaves as always") {
                val g        = Guard.RequireRole[TestRole, TEntity](TestRole.AdministratorZakazek)
                val stripped = Guard.stripAuth(g)
                assertTrue(
                    stripped.check(userCtx(entity, roles = Set.empty)) == Right(()),
                    stripped.describe == GuardDescription.All(Nil)
                )
            },
            test("stripAuth(RequireIdentity) behaves as always") {
                val g        = Guard.RequireIdentity[TestRole, TEntity]("assigned", _ => false)
                val stripped = Guard.stripAuth(g)
                assertTrue(
                    stripped.check(userCtx(entity)) == Right(()),
                    stripped.describe == GuardDescription.All(Nil)
                )
            },
            test("stripAuth(Requirement) is unchanged") {
                val req      = Guard.Requirement(Predicate[TEntity]("ready", _.ready), notReadyMsg)
                val stripped = Guard.stripAuth(req)
                assertTrue(stripped == req)
            },
            test("stripAuth(And(RequireRole, Requirement)) — auth leaf removed, Requirement still gates") {
                val failing  = Guard.Requirement[TestRole, TEntity](Predicate[TEntity]("neverReady", _ => false), notReadyMsg)
                val guard    = Guard.RequireRole[TestRole, TEntity](TestRole.AdministratorZakazek) and failing
                val stripped = Guard.stripAuth(guard)
                assertTrue(
                    stripped.check(userCtx(entity, roles = Set.empty)) == Left(GuardFailure.Leaf("neverReady", notReadyMsg))
                )
            },
            test(
                "stripAuth(Or(RequireRole, Requirement)) PASSES under Skip even when the Requirement fails " +
                    "(proves leaf-rewrite, not collapse-time failure-stripping)"
            ) {
                val failing  = Guard.Requirement[TestRole, TEntity](Predicate[TEntity]("neverReady", _ => false), notReadyMsg)
                val guard    = Guard.RequireRole[TestRole, TEntity](TestRole.AdministratorZakazek) or failing
                val stripped = Guard.stripAuth(guard)
                assertTrue(stripped.check(userCtx(entity, roles = Set.empty)) == Right(()))
            },
            test("stripAuth recurses through nested And/Or") {
                val innerOr = Guard.RequireRole[TestRole, TEntity](TestRole.AdministratorZakazek) or
                    Guard.RequireIdentity[TestRole, TEntity]("assigned", _ => false)
                val nested  = innerOr and Guard.Requirement(Predicate[TEntity]("ready", _.ready), notReadyMsg)
                val stripped = Guard.stripAuth(nested)
                // both auth leaves rewritten; only the Requirement (which passes for `entity`) remains
                assertTrue(stripped.check(userCtx(entity, roles = Set.empty)) == Right(()))
            }
        ),
        suite("AuthEnforcement / Rejection — interpreter result vocabulary")(
            test("AuthEnforcement.Skip and Enforce are distinct") {
                assertTrue(AuthEnforcement.Skip != AuthEnforcement.Enforce)
            },
            test("Rejection cases construct as specified") {
                val u: Rejection   = Rejection.Unhandled
                val inv: Rejection = Rejection.Invalid(MessageId("test.x"))
                val una: Rejection = Rejection.Unauthorized("SomeRole")
                assertTrue(
                    u == Rejection.Unhandled,
                    inv == Rejection.Invalid(MessageId("test.x")),
                    una == Rejection.Unauthorized("SomeRole")
                )
            }
        ),
        suite("WorkflowInterpreter.decide — MATCH stage")(
            test("no transition for the current state/command → Left(Unhandled)") {
                val t = Transition.simple[TestRole, WState, WEntity, WCommand, WEvent](
                    WState.Draft, publishMatch, WState.Published, List(publishedShape), (_, _) => Seq(WEvent.PublishedEvt)
                )
                val defn = EngineDefinition[TestRole, WState, WEntity, WCommand, WEvent](_.state, List(t))
                val e    = WEntity(WState.Published, ready = true, approved = true)
                assertTrue(
                    WorkflowInterpreter.decide(defn, AuthEnforcement.Skip)(wCtx(e), WCommand.Publish) ==
                        Left(Rejection.Unhandled)
                )
            },
            test("matching transition whose applicableWhen fails → Left(Unhandled)") {
                val t = Transition.simple[TestRole, WState, WEntity, WCommand, WEvent](
                    from = WState.Draft,
                    on = publishMatch,
                    to = WState.Published,
                    emits = List(publishedShape),
                    emit = (_, _) => Seq(WEvent.PublishedEvt),
                    applicableWhen = Guard.Requirement(readyPredicate, notReadyMsg)
                )
                val defn = EngineDefinition[TestRole, WState, WEntity, WCommand, WEvent](_.state, List(t))
                val e    = WEntity(WState.Draft, ready = false, approved = true)
                assertTrue(
                    WorkflowInterpreter.decide(defn, AuthEnforcement.Skip)(wCtx(e), WCommand.Publish) ==
                        Left(Rejection.Unhandled)
                )
            },
            test("first-applicable ordering: first transition whose applicableWhen fails is skipped") {
                val t1 = Transition.simple[TestRole, WState, WEntity, WCommand, WEvent](
                    from = WState.Draft,
                    on = publishMatch,
                    to = WState.Archived,
                    emits = Nil,
                    emit = (_, _) => Seq(WEvent.ArchivedEvt),
                    applicableWhen = Guard.Requirement(readyPredicate, notReadyMsg)
                )
                val t2 = Transition.simple[TestRole, WState, WEntity, WCommand, WEvent](
                    WState.Draft, publishMatch, WState.Published, List(publishedShape), (_, _) => Seq(WEvent.PublishedEvt)
                )
                val defn = EngineDefinition[TestRole, WState, WEntity, WCommand, WEvent](_.state, List(t1, t2))
                val e    = WEntity(WState.Draft, ready = false, approved = true)
                assertTrue(
                    WorkflowInterpreter.decide(defn, AuthEnforcement.Skip)(wCtx(e), WCommand.Publish) ==
                        Right(Seq(WEvent.PublishedEvt))
                )
            },
            test("first-applicable ordering: when both would pass, the first transition in list order wins") {
                val t1 = Transition.simple[TestRole, WState, WEntity, WCommand, WEvent](
                    WState.Draft, publishMatch, WState.Archived, Nil, (_, _) => Seq(WEvent.ArchivedEvt)
                )
                val t2 = Transition.simple[TestRole, WState, WEntity, WCommand, WEvent](
                    WState.Draft, publishMatch, WState.Published, List(publishedShape), (_, _) => Seq(WEvent.PublishedEvt)
                )
                val defn = EngineDefinition[TestRole, WState, WEntity, WCommand, WEvent](_.state, List(t1, t2))
                val e    = WEntity(WState.Draft, ready = true, approved = true)
                assertTrue(
                    WorkflowInterpreter.decide(defn, AuthEnforcement.Skip)(wCtx(e), WCommand.Publish) ==
                        Right(Seq(WEvent.ArchivedEvt))
                )
            }
        ),
        suite("WorkflowInterpreter.decide — GATE stage")(
            test("requires-fail → Left(Invalid(exact MessageId))") {
                val t = Transition.simple[TestRole, WState, WEntity, WCommand, WEvent](
                    from = WState.Draft,
                    on = publishMatch,
                    to = WState.Published,
                    emits = List(publishedShape),
                    emit = (_, _) => Seq(WEvent.PublishedEvt),
                    requires = Guard.Requirement(approvedPredicate, notApprovedMsg)
                )
                val defn = EngineDefinition[TestRole, WState, WEntity, WCommand, WEvent](_.state, List(t))
                val e    = WEntity(WState.Draft, ready = true, approved = false)
                assertTrue(
                    WorkflowInterpreter.decide(defn, AuthEnforcement.Skip)(wCtx(e), WCommand.Publish) ==
                        Left(Rejection.Invalid(notApprovedMsg))
                )
            },
            test("requiresPayload-fail → Left(Invalid(...))") {
                val missingNoteMsg = MessageId("test.missingNote")
                val t = Transition[TestRole, WState, WEntity, WCommand, WEvent](
                    from = WState.Draft,
                    on = CommandMatch[WCommand]("publishWithNote", { case WCommand.PublishWithNote(_) => () }),
                    applicableWhen = Guard.always,
                    requires = Guard.always,
                    requiresPayload = (_, cmd) =>
                        cmd match
                            case WCommand.PublishWithNote(note) =>
                                Guard.Requirement(Predicate[WEntity]("hasNote", _ => note.nonEmpty), missingNoteMsg)
                            case _ => Guard.always,
                    outcomes = List(
                        Outcome[WState, WEntity, WCommand, WEvent](
                            "only",
                            WState.Published,
                            List(EventShape[WEvent]("publishedWithNote", { case WEvent.PublishedWithNoteEvt(_) => () })),
                            (_, cmd) =>
                                cmd match
                                    case WCommand.PublishWithNote(note) => Seq(WEvent.PublishedWithNoteEvt(note))
                                    case _                              => Seq.empty
                        )
                    ),
                    classify = (_, _) =>
                        Outcome[WState, WEntity, WCommand, WEvent](
                            "only",
                            WState.Published,
                            List(EventShape[WEvent]("publishedWithNote", { case WEvent.PublishedWithNoteEvt(_) => () })),
                            (_, cmd) =>
                                cmd match
                                    case WCommand.PublishWithNote(note) => Seq(WEvent.PublishedWithNoteEvt(note))
                                    case _                              => Seq.empty
                        )
                )
                val defn = EngineDefinition[TestRole, WState, WEntity, WCommand, WEvent](_.state, List(t))
                val e    = WEntity(WState.Draft, ready = true, approved = true)
                assertTrue(
                    WorkflowInterpreter.decide(defn, AuthEnforcement.Skip)(wCtx(e), WCommand.PublishWithNote("")) ==
                        Left(Rejection.Invalid(missingNoteMsg)),
                    WorkflowInterpreter.decide(defn, AuthEnforcement.Skip)(wCtx(e), WCommand.PublishWithNote("ok")) ==
                        Right(Seq(WEvent.PublishedWithNoteEvt("ok")))
                )
            },
            test("auth-only requires failure under Enforce → Left(Unauthorized(label)); unreachable under Skip") {
                val t = Transition.simple[TestRole, WState, WEntity, WCommand, WEvent](
                    from = WState.Draft,
                    on = publishMatch,
                    to = WState.Published,
                    emits = List(publishedShape),
                    emit = (_, _) => Seq(WEvent.PublishedEvt),
                    requires = Guard.RequireRole[TestRole, WEntity](TestRole.AdministratorZakazek)
                )
                val defn = EngineDefinition[TestRole, WState, WEntity, WCommand, WEvent](_.state, List(t))
                val e    = WEntity(WState.Draft, ready = true, approved = true)
                assertTrue(
                    WorkflowInterpreter.decide(defn, AuthEnforcement.Enforce)(wCtx(e, roles = Set.empty), WCommand.Publish) ==
                        Left(Rejection.Unauthorized("AdministratorZakazek")),
                    WorkflowInterpreter.decide(
                        defn,
                        AuthEnforcement.Enforce
                    )(wCtx(e, roles = Set(TestRole.AdministratorZakazek)), WCommand.Publish) ==
                        Right(Seq(WEvent.PublishedEvt)),
                    WorkflowInterpreter.decide(defn, AuthEnforcement.Skip)(wCtx(e, roles = Set.empty), WCommand.Publish) ==
                        Right(Seq(WEvent.PublishedEvt))
                )
            },
            test("collapse — Conjunction of two data Leafs → Invalid with the DFS-first Leaf message") {
                val msg1 = MessageId("test.msg1")
                val msg2 = MessageId("test.msg2")
                val t = Transition.simple[TestRole, WState, WEntity, WCommand, WEvent](
                    from = WState.Draft,
                    on = publishMatch,
                    to = WState.Published,
                    emits = List(publishedShape),
                    emit = (_, _) => Seq(WEvent.PublishedEvt),
                    requires = Guard.Requirement(readyPredicate, msg1) and Guard.Requirement(approvedPredicate, msg2)
                )
                val defn = EngineDefinition[TestRole, WState, WEntity, WCommand, WEvent](_.state, List(t))
                val e    = WEntity(WState.Draft, ready = false, approved = false)
                assertTrue(
                    WorkflowInterpreter.decide(defn, AuthEnforcement.Skip)(wCtx(e), WCommand.Publish) ==
                        Left(Rejection.Invalid(msg1))
                )
            },
            test("collapse — Conjunction mixing a Leaf and an Unauthorized → Invalid (data message wins)") {
                val msg1 = MessageId("test.msg1")
                val t = Transition.simple[TestRole, WState, WEntity, WCommand, WEvent](
                    from = WState.Draft,
                    on = publishMatch,
                    to = WState.Published,
                    emits = List(publishedShape),
                    emit = (_, _) => Seq(WEvent.PublishedEvt),
                    requires = Guard.RequireRole[TestRole, WEntity](TestRole.AdministratorZakazek) and
                        Guard.Requirement(readyPredicate, msg1)
                )
                val defn = EngineDefinition[TestRole, WState, WEntity, WCommand, WEvent](_.state, List(t))
                val e    = WEntity(WState.Draft, ready = false, approved = true)
                assertTrue(
                    WorkflowInterpreter.decide(defn, AuthEnforcement.Enforce)(wCtx(e, roles = Set.empty), WCommand.Publish) ==
                        Left(Rejection.Invalid(msg1))
                )
            },
            test("collapse — Disjunction of only Unauthorized failures → Unauthorized(first label)") {
                val t = Transition.simple[TestRole, WState, WEntity, WCommand, WEvent](
                    from = WState.Draft,
                    on = publishMatch,
                    to = WState.Published,
                    emits = List(publishedShape),
                    emit = (_, _) => Seq(WEvent.PublishedEvt),
                    requires = Guard.RequireRole[TestRole, WEntity](TestRole.AdministratorZakazek) or
                        Guard.RequireRole[TestRole, WEntity](TestRole.Pracovnik)
                )
                val defn = EngineDefinition[TestRole, WState, WEntity, WCommand, WEvent](_.state, List(t))
                val e    = WEntity(WState.Draft, ready = true, approved = true)
                assertTrue(
                    WorkflowInterpreter.decide(defn, AuthEnforcement.Enforce)(wCtx(e, roles = Set.empty), WCommand.Publish) ==
                        Left(Rejection.Unauthorized("AdministratorZakazek"))
                )
            },
            test("collapse — Disjunction mixing a Leaf and an Unauthorized → Invalid (data message wins)") {
                val msg1 = MessageId("test.msg1")
                val t = Transition.simple[TestRole, WState, WEntity, WCommand, WEvent](
                    from = WState.Draft,
                    on = publishMatch,
                    to = WState.Published,
                    emits = List(publishedShape),
                    emit = (_, _) => Seq(WEvent.PublishedEvt),
                    requires = Guard.RequireRole[TestRole, WEntity](TestRole.AdministratorZakazek) or
                        Guard.Requirement(readyPredicate, msg1)
                )
                val defn = EngineDefinition[TestRole, WState, WEntity, WCommand, WEvent](_.state, List(t))
                val e    = WEntity(WState.Draft, ready = false, approved = true)
                assertTrue(
                    WorkflowInterpreter.decide(defn, AuthEnforcement.Enforce)(wCtx(e, roles = Set.empty), WCommand.Publish) ==
                        Left(Rejection.Invalid(msg1))
                )
            }
        ),
        suite("WorkflowInterpreter.decide — CLASSIFY → EMIT stage")(
            test("success → Right(classified outcome's emit), a raw Seq[Event] (interpreter never folds)") {
                val t = Transition.simple[TestRole, WState, WEntity, WCommand, WEvent](
                    WState.Draft, publishMatch, WState.Published, List(publishedShape), (_, _) => Seq(WEvent.PublishedEvt)
                )
                val defn = EngineDefinition[TestRole, WState, WEntity, WCommand, WEvent](_.state, List(t))
                val e    = WEntity(WState.Draft, ready = true, approved = true)
                assertTrue(
                    WorkflowInterpreter.decide(defn, AuthEnforcement.Skip)(wCtx(e), WCommand.Publish) ==
                        Right(Seq(WEvent.PublishedEvt))
                )
            },
            test("multi-outcome classify selects the correct Outcome per entity/command data") {
                val approvedOutcome = Outcome[WState, WEntity, WCommand, WEvent](
                    "approved",
                    WState.Published,
                    List(publishedShape),
                    (_, _) => Seq(WEvent.PublishedEvt)
                )
                val unapprovedOutcome = Outcome[WState, WEntity, WCommand, WEvent](
                    "unapproved",
                    WState.Archived,
                    Nil,
                    (_, _) => Seq(WEvent.ArchivedEvt)
                )
                val t = Transition[TestRole, WState, WEntity, WCommand, WEvent](
                    from = WState.Draft,
                    on = publishMatch,
                    applicableWhen = Guard.always,
                    requires = Guard.always,
                    outcomes = List(approvedOutcome, unapprovedOutcome),
                    classify = (e, _) => if e.approved then approvedOutcome else unapprovedOutcome
                )
                val defn = EngineDefinition[TestRole, WState, WEntity, WCommand, WEvent](_.state, List(t))
                assertTrue(
                    WorkflowInterpreter.decide(defn, AuthEnforcement.Skip)(
                        wCtx(WEntity(WState.Draft, ready = true, approved = true)),
                        WCommand.Publish
                    ) == Right(Seq(WEvent.PublishedEvt)),
                    WorkflowInterpreter.decide(defn, AuthEnforcement.Skip)(
                        wCtx(WEntity(WState.Draft, ready = true, approved = false)),
                        WCommand.Publish
                    ) == Right(Seq(WEvent.ArchivedEvt))
                )
            }
        )
    )
end WorkflowInterpreterSpec
