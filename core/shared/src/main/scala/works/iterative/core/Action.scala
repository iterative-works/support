package works.iterative.core

import works.iterative.core.auth.PermissionOp
import works.iterative.core.auth.PermissionTarget

final case class Action(
    op: PermissionOp,
    target: PermissionTarget
)

object Action:
  def apply(op: String, target: String): Validated[Action] =
    for t <- PermissionTarget(target)
    yield Action(PermissionOp(op), t)
