package works.iterative
package ui.components

import works.iterative.core.MessageCatalogue
import works.iterative.core.auth.UserProfile
import com.raquo.airstream.core.Signal

/** Context containing services needed in all parts of the application
  */
// TODO: this is Laminar-specific, we need to make it generic
// Also, it is hard to mock, as there is too much stuff in it
// There has been an attempt to use a typeclass to provide the services, like
// Env: CurrentUser etc, but it was unclear how the typeclass would be used with the Dispatcher
// as it needs Env
// So for now, it is everything in one place
trait ComponentContext[+Env]:
  def currentUser: Signal[Option[UserProfile]]
  def messages: MessageCatalogue
  def modal: Modal
  def dispatcher: ZIODispatcher[Env]

  def nested(prefixes: String*): ComponentContext[Env] =
    ComponentContext.Nested[Env](this, prefixes)

object ComponentContext:
  case class Nested[App](parent: ComponentContext[App], prefixes: Seq[String])
      extends ComponentContext[App]:
    export parent.{messages => _, *}

    override lazy val messages: MessageCatalogue =
      parent.messages.nested(prefixes*)
