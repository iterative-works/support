// PURPOSE: JUnit entry point running the form scenario Cucumber features in a real browser
// PURPOSE: Glue combines the shared e2e framework hooks with the local server and step definitions

package works.iterative.forms.scenarios.e2e

import io.cucumber.junit.{Cucumber, CucumberOptions}
import org.junit.runner.RunWith

@RunWith(classOf[Cucumber])
@CucumberOptions(
    features = Array("classpath:features"),
    glue = Array("works.iterative.testing.e2e", "works.iterative.forms.scenarios.e2e"),
    plugin = Array("pretty")
)
class FormsE2eSuite
