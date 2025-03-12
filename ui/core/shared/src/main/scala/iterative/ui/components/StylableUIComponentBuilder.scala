package works.iterative.ui.components

// Enhanced component builder that supports style overrides
trait StylableUIComponentBuilder[T, Self <: StylableUIComponentBuilder[T, Self]]
    extends UIComponentBuilder[T, Self]:
    self: Self =>

    // Reference to style module
    protected val stylesModule: UIStylesModule[T]

    // Style override manager
    protected val styleOverride: UIStylesModule[T]#StyleOverride

    // Style and class override methods
    def withStyleOverride(componentType: String, styles: Map[String, String]): Self =
        val newOverride = styleOverride.overrideStyle(componentType)(styles)
        withStyleOverrides(newOverride)

    def withClassOverride(componentType: String, classes: Seq[String]): Self =
        val newOverride = styleOverride.overrideClasses(componentType)(classes)
        withStyleOverrides(newOverride)

    // Helper method for specific component parts
    def withPartStyle(part: String, styles: Map[String, String]): Self =
        withStyleOverride(s"${componentBasePath}.$part", styles)

    def withPartClasses(part: String, classes: Seq[String]): Self =
        withClassOverride(s"${componentBasePath}.$part", classes)

    // Base component path (e.g., "table" for TableBuilder)
    protected def componentBasePath: String

    // Update the style override manager
    protected def withStyleOverrides(newOverride: UIStylesModule[T]#StyleOverride): Self
end StylableUIComponentBuilder
