package works.iterative.ui.components

// Base builder trait that handles common customization methods
trait UIComponentBuilder[T, Self <: UIComponentBuilder[T, Self]]:
    // Self-type to handle method chaining correctly
    self: Self =>

    // Common attributes all UI elements might have
    protected val attributes: Map[String, String]
    protected val classes: Seq[String]
    protected val dataset: Map[String, String] // For data-* attributes

    // Common modifier methods
    def withAttr(name: String, value: String): Self =
        withAttributes(attributes + (name -> value))

    def withAttrs(attrs: Map[String, String]): Self =
        withAttributes(attributes ++ attrs)

    def withClass(cls: String): Self =
        withCls(classes :+ cls)

    def withClasses(cls: Seq[String]): Self =
        withCls(classes ++ cls)

    def withData(name: String, value: String): Self =
        withDataset(dataset + (name -> value))

    // Abstract methods that each concrete builder must implement
    protected def withAttributes(newAttrs: Map[String, String]): Self
    protected def withCls(newClasses: Seq[String]): Self
    protected def withDataset(newDataset: Map[String, String]): Self

    // Final render method
    def render: T
end UIComponentBuilder
