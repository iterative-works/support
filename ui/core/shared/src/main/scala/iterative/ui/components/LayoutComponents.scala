package works.iterative.ui.components

/** Layout components for basic page structure and container elements.
  */
trait LayoutComponents[T] extends Components[T]:
    /** Main container for page content with consistent spacing */
    def pageContainer(content: T*): T

    /** Two-column layout with responsive behavior */
    def twoColumnLayout(left: T, right: T): T

    /** Card component with optional header */
    def card(content: T, header: Option[T] = None): T

    /** Section component with title */
    def section(title: String, content: T): T

    /** Content wrapper with consistent spacing */
    def contentWrapper(content: T*): T

    /** Actions section with standard gap and alignment */
    def actionsSection(actions: T*): T
end LayoutComponents
