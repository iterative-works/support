package portaly.forms
package impl

import com.raquo.laminar.api.L.*

trait HtmlInterpreter extends Interpreter[FormR, LiveForm]:
    def cs: Components

    /** Return an interpreter wrapping the title part in another element */
    def aroundTitle(f: HtmlElement => HtmlElement): HtmlInterpreter
    def withFormMods(mods: HtmlMod): HtmlInterpreter
    def withComponents(components: Components): HtmlInterpreter
    def withAutocompleteContext(context: Map[String, String]): HtmlInterpreter
end HtmlInterpreter
