package works.iterative.scalatags

import scalatags.Text.Frag

object components:
    trait ScalatagsComponents extends works.iterative.ui.components.Components[Frag]
    trait ScalatagsStatusDisplayComponents
        extends works.iterative.ui.components.StatusDisplayComponents[Frag]
    trait ScalatagsDataDisplayComponents
        extends works.iterative.ui.components.DataDisplayComponents[Frag]
    trait ScalatagsFormComponents extends works.iterative.ui.components.FormComponents[Frag]
    trait ScalatagsLayoutComponents extends works.iterative.ui.components.LayoutComponents[Frag]
    trait ScalatagsErrorPageComponents
        extends works.iterative.ui.components.ErrorPageComponents[Frag]
    trait ScalatagsAppShell extends works.iterative.ui.components.AppShell[Frag]
    trait ScalatagsActionComponents extends works.iterative.ui.components.ActionComponents[Frag]
end components
