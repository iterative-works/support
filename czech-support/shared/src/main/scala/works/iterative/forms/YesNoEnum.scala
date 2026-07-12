// PURPOSE: Czech yes/no enum declaration — extends the Enum companion with a yesno
// PURPOSE: helper producing the ano/ne value pair Czech forms answer questions with
package works.iterative.forms

import works.iterative.ui.model.forms.RelativePath

extension (e: Enum.type)
    def yesno(id: RelativePath, default: Option[Boolean] = None, optional: Boolean = true): Enum =
        Enum(id, List("ano", "ne"), default = default.map(_.toString), optional = optional)
