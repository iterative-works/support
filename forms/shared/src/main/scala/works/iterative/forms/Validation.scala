// PURPOSE: The declared validation vocabulary a field carries in the form declaration
// PURPOSE: Closed common cases plus the open Rule escape hatch for client and async validations

package portaly.forms

enum Validation:
    case Required
    case Email
    case Pattern(regex: String)
    case MinLength(min: Int)
    case MaxLength(max: Int)
    case Rule(id: String, params: Map[String, String] = Map.empty)
end Validation
