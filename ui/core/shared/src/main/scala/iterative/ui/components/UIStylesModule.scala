package works.iterative.ui.components

// Enhanced styling module that supports overrides
trait UIStylesModule[T]:
    // Base style methods
    def getStyle(
        componentType: String,
        variant: String = "default",
        state: String = "default"
    ): Map[String, String]
    def getClasses(
        componentType: String,
        variant: String = "default",
        state: String = "default"
    ): Seq[String]

    // Create style override manager
    def createStyleOverride(): StyleOverride = new StyleOverride()

    // Style override manager
    class StyleOverride:
        private var styleOverrides: Map[(String, String, String), Map[String, String]] = Map.empty
        private var classOverrides: Map[(String, String, String), Seq[String]] = Map.empty

        // Override styles for a specific component
        def overrideStyle(
            componentType: String,
            variant: String = "default",
            state: String = "default"
        )(styles: Map[String, String]): StyleOverride =
            styleOverrides = styleOverrides + ((componentType, variant, state) -> styles)
            this
        end overrideStyle

        // Override or add classes for a specific component
        def overrideClasses(
            componentType: String,
            variant: String = "default",
            state: String = "default"
        )(classes: Seq[String]): StyleOverride =
            classOverrides = classOverrides + ((componentType, variant, state) -> classes)
            this
        end overrideClasses

        // Get combined styles (base + overrides)
        def getComputedStyle(
            componentType: String,
            variant: String = "default",
            state: String = "default"
        ): Map[String, String] =
            val baseStyles = getStyle(componentType, variant, state)
            val overrides = styleOverrides.getOrElse((componentType, variant, state), Map.empty)
            baseStyles ++ overrides
        end getComputedStyle

        // Get combined classes (base + overrides)
        def getComputedClasses(
            componentType: String,
            variant: String = "default",
            state: String = "default"
        ): Seq[String] =
            val baseClasses = getClasses(componentType, variant, state)
            val overrides = classOverrides.getOrElse((componentType, variant, state), Seq.empty)
            baseClasses ++ overrides
        end getComputedClasses
    end StyleOverride
end UIStylesModule
