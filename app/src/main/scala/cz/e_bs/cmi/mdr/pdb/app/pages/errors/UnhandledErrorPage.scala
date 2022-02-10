package cz.e_bs.cmi.mdr.pdb.app.pages.errors

import com.raquo.domtypes.generic.codecs.StringAsIsCodec
import com.raquo.laminar.api.L.{*, given}

def UnhandledErrorPage(
    basePath: String,
    errorName: Option[String],
    errorMessage: Option[String]
): HtmlElement =
  ErrorPage(
    basePath,
    "Unexpected error occurred",
    errorName.getOrElse("Uh oh!"), // TODO: translations, better text than uh oh
    errorMessage.getOrElse("This wasn't supposed to happen! Please try again.")
  )
