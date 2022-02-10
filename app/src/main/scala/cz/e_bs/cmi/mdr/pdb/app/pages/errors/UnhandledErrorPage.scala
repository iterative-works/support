package cz.e_bs.cmi.mdr.pdb.app.pages.errors

import com.raquo.domtypes.generic.codecs.StringAsIsCodec
import com.raquo.laminar.api.L.{*, given}
import com.raquo.waypoint.Router
import cz.e_bs.cmi.mdr.pdb.app.Page

def UnhandledErrorPage(
    errorName: Option[String],
    errorMessage: Option[String]
)(using router: Router[Page]): HtmlElement =
  ErrorPage(
    "Unexpected error occurred",
    errorName.getOrElse("Uh oh!"), // TODO: translations, better text than uh oh
    errorMessage.getOrElse("This wasn't supposed to happen! Please try again.")
  )
