package works.iterative.ui

trait Module[Model, Action, +Effect]:
    // Define initial model and effect
    def init: (Model, Option[Effect])
    // Define how to handle actions to build new model and run effects
    def handle(action: Action, model: Model): (Model, Option[Effect])
    // Optionally define how to handle failures.
    // To be used by implementations to allow module to display error messages.
    def handleFailure: PartialFunction[Throwable, Option[Action]] =
        PartialFunction.empty
end Module
