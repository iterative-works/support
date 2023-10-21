package works.iterative.entity

import works.iterative.core.UserMessage

trait AggregateError:
  def userMessage: UserMessage
