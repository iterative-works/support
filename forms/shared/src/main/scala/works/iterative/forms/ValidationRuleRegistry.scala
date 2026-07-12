// PURPOSE: Binds declared Rule validations to their checks by id — the edge where Rule gets meaning
// PURPOSE: Rules with no binding pass; they validate at other edges (async services, the server)

package works.iterative.forms

import works.iterative.core.UserMessage

trait ValidationRuleRegistry:
    def check(rule: Validation.Rule, value: String, label: => String): Option[UserMessage]

object ValidationRuleRegistry:
    val empty: ValidationRuleRegistry = new ValidationRuleRegistry:
        def check(rule: Validation.Rule, value: String, label: => String): Option[UserMessage] =
            None
end ValidationRuleRegistry
