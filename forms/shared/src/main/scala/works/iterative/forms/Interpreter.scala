package portaly
package forms

trait Interpreter[I, O]:
    /** Interpret the form and the data and return the output
      *
      * @param id
      *   Specific ID for this form
      * @param form
      *   The form descriptor to interpret
      * @param data
      *   The initial data for the form
      * @return
      *   The output of the interpretation
      */
    def interpret(id: FormIdent, form: Form, data: Option[I]): O
end Interpreter
