package works.iterative.core

import works.iterative.core.auth.PermissionOp
import works.iterative.core.auth.PermissionTarget

final case class Action(
    op: PermissionOp,
    target: PermissionTarget
)

object Action:
    def apply(op: String, target: String): Validated[Action] =
        for
            o <- PermissionOp(op)
            t <- PermissionTarget(target)
        yield Action(o, t)

    def unsafe(op: String, target: String): Action =
        apply(PermissionOp.unsafe(op), PermissionTarget.unsafe(target))
end Action
