package works.iterative.services.files
package components.tailwind

import com.raquo.laminar.api.L.*

def FileList(files: List[File]): HtmlElement =
  ul(
    role("list"),
    cls("border border-gray-200 rounded-md divide-y divide-gray-200"),
    files.map(_.render)
  )
