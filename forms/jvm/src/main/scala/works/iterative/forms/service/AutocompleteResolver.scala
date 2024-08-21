package portaly.forms
package service

trait AutocompleteResolver:
    /** Return collection id by field type */
    def resolveAutocomplete(fieldType: String): Option[String]

object AutocompleteResolver:
    val empty: AutocompleteResolver = new AutocompleteResolver:
        def resolveAutocomplete(fieldType: String): Option[String] = None
