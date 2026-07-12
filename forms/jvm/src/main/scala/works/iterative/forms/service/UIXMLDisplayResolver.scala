package works.iterative.forms
package service

import zio.*
import scala.xml.*
import works.iterative.ui.model.forms.FormState

trait UIXMLDisplayResolver extends DisplayResolver[FormState, UIO[NodeSeq]]
