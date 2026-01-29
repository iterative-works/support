package works.iterative.core

import zio.*

type HumanReadableError = UserMessage | HasUserMessage
type HER = HumanReadableError

/** This is the type for Workflow effects
  *
  * It has either HumanReadableError, or some output.
  */
type WEff[+A] = IO[HER, A]
type WorkflowEffect[+A] = WEff[A]

/** Workflow effect with environment.
  */
type RWeff[-R, +A] = ZIO[R, HER, A]
