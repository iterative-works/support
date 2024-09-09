package portaly.forms
package impl

import com.raquo.laminar.api.L.*
import works.iterative.core.MessageCatalogue
import works.iterative.core.UserMessage

type Render[+A] = FormCtx ?=> MessageCatalogue ?=> Components ?=> A
type RenderHtml = Render[HtmlElement]
type RenderNode = Render[Node]
type RenderElement[+A <: HtmlElement] = Render[A]
type RenderPart = Render[Part]

type Part = PartSF[FormV, FormR, FormR, FormR]

type PartF[RI, RO, A] = PartSF[Any, RI, RO, A]
type PartSF[S, RI, RO, A] = FormPart[S, RI, Nothing, RO, List[UserMessage], ValidationState[A]]
