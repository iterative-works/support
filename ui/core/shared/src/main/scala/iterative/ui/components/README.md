# UI Component System Pattern

This document describes the UI component system pattern used in this project. The pattern combines the cake pattern, builder pattern, and style system to create a flexible, composable UI component framework.

## Core Concepts

The UI component system is built on several key concepts:

1. **Cake Pattern**: Components are organized into self-contained modules that can be mixed together
2. **Builder Pattern**: Components use immutable builders for configuration
3. **Style System**: A consistent styling approach with support for overrides
4. **Technology Agnosticism**: Components are parameterized over the output type

## Architecture

### UIComponentBuilder

The `UIComponentBuilder` trait is the foundation for all component builders:

```scala
trait UIComponentBuilder[T, Self <: UIComponentBuilder[T, Self]]:
  self: Self =>

  protected val attributes: Map[String, String]
  protected val classes: Seq[String]
  protected val dataset: Map[String, String]

  // Common modifier methods
  def withAttr(name: String, value: String): Self
  def withAttrs(attrs: Map[String, String]): Self
  def withClass(cls: String): Self
  def withClasses(cls: Seq[String]): Self
  def withData(name: String, value: String): Self

  // Abstract methods for implementation
  protected def withAttributes(newAttrs: Map[String, String]): Self
  protected def withCls(newClasses: Seq[String]): Self
  protected def withDataset(newDataset: Map[String, String]): Self

  // Final render method
  def render: T
```

### UIStylesModule

The styling system is based on the `UIStylesModule` trait:

```scala
trait UIStylesModule[T]:
  def getStyle(componentType: String, variant: String = "default", state: String = "default"): Map[String, String]
  def getClasses(componentType: String, variant: String = "default", state: String = "default"): Seq[String]
  def createStyleOverride(): StyleOverride
```

With the `StyleOverride` class allowing per-instance customization:

```scala
class StyleOverride:
  def overrideStyle(componentType: String, variant: String = "default", state: String = "default")
                   (styles: Map[String, String]): StyleOverride
  def overrideClasses(componentType: String, variant: String = "default", state: String = "default")
                     (classes: Seq[String]): StyleOverride
```

### StylableUIComponentBuilder

For components that support styling overrides:

```scala
trait StylableUIComponentBuilder[T, Self <: StylableUIComponentBuilder[T, Self]] extends UIComponentBuilder[T, Self]:
  protected val stylesModule: UIStylesModule[T]
  protected val styleOverride: UIStylesModule[T]#StyleOverride

  def withStyleOverride(componentType: String, styles: Map[String, String]): Self
  def withClassOverride(componentType: String, classes: Seq[String]): Self
  def withPartStyle(part: String, styles: Map[String, String]): Self
  def withPartClasses(part: String, classes: Seq[String]): Self
```

### UI Modules

Each UI component category is defined as a module trait:

```scala
trait ModuleNameUIModule[T]:
  this: UIStylesModule[T] =>

  // Component builders
  case class ComponentBuilder(...) extends StylableUIComponentBuilder[T, ComponentBuilder]:
    // Component-specific customization methods

    def render: T = renderComponent(this)

  // Factory methods
  def component(...): ComponentBuilder = ...

  // Protected rendering methods (to be implemented)
  protected def renderComponent(builder: ComponentBuilder): T
```

## Implementing UI Modules

Each UI technology implementation (Scalatags, Laminar, etc.) provides concrete implementations:

```scala
trait ScalatagsSomeUIModule extends SomeUIModule[Frag] with ScalatagsUIStylesModule:
  protected def renderComponent(builder: ComponentBuilder): Frag =
    // Scalatags-specific rendering implementation
```

## Composition

The full UI system is composed by mixing modules together:

```scala
// Base UI system
trait UISystem[T]:
  this: ComponentAUIModule[T] & ComponentBUIModule[T] & ... & UIStylesModule[T] =>
  // Common functionality across all components

// Default implementation
trait DefaultUISystem[T] extends UISystem[T] with
  DefaultComponentAUIModule[T] with
  DefaultComponentBUIModule[T] with
  ...
  DefaultUIStylesModule[T]

// Technology-specific implementation
trait ScalatagsUISystem extends DefaultUISystem[Frag] with
  ScalatagsComponentAUIModule with
  ScalatagsComponentBUIModule with
  ...
  ScalatagsUIStylesModule
```

## Usage Examples

### Creating a basic component

```scala
val button = primaryButton("Submit")
  .withAttr("type", "submit")
  .withClass("large")
  .render
```

### Styling overrides

```scala
val customTable = table(columns, data)
  .withHeaderClasses(Seq("sticky-top"))
  .withRowStyle(Map("border-bottom" -> "1px solid #eee"))
  .render
```

### Form fields with validation

```scala
val nameField = textField("name", "Full Name", value = user.name)
  .withRequired(true)
  .withData("validate", "required")
  .render
```

## Extending the System

To add new component types:

1. Define a new `SomeUIModule[T]` trait with builders and factory methods
2. Create a `DefaultSomeUIModule[T]` with common implementations
3. Implement concrete technology-specific versions
4. Mix the new module into your `UISystem`
