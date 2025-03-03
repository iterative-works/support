package works.iterative.forms

trait FormError[T]:
    def render: T

object FormError:
    case class Simple[T](message: String)(using renderer: StringRenderer[T]) extends FormError[T]:
        def render: T = renderer.render(message)

    case class Multiple[T](errors: List[FormError[T]])(using renderer: ListRenderer[T])
        extends FormError[T]:
        def render: T = renderer.render(errors.map(_.render))

    trait StringRenderer[T]:
        def render(message: String): T

    trait ListRenderer[T]:
        def render(items: List[T]): T
end FormError

// Then FormErrors becomes generic over T as well
trait FormErrors[T]:
    def forField(field: FormField): Option[FormError[T]]
    def forField(name: String): Option[FormError[T]]
    def hasErrors: Boolean

object FormErrors:
    def apply[T](errors: Map[String, FormError[T]]): FormErrors[T] = SimpleFormErrors(errors)

    def empty[E]: FormErrors[E] = SimpleFormErrors(Map.empty)

    private case class SimpleFormErrors[T](errors: Map[String, FormError[T]]) extends FormErrors[T]:
        def forField(field: FormField): Option[FormError[T]] = forField(field.name)
        def forField(name: String): Option[FormError[T]] = errors.get(name)
        def hasErrors: Boolean = errors.nonEmpty
    end SimpleFormErrors
end FormErrors
