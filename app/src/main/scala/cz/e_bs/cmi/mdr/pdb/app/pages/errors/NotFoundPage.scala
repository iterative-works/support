package cz.e_bs.cmi.mdr.pdb.app.pages.errors

import com.raquo.domtypes.generic.codecs.StringAsIsCodec
import com.raquo.laminar.api.L.{*, given}
import com.raquo.waypoint.Router
import cz.e_bs.cmi.mdr.pdb.app.Page

def NotFoundPage(url: String)(using
    router: Router[Page]
): HtmlElement =
  ErrorPage(
    "404 error",
    "Page not found.",
    s"Sorry, but page $url doesn't exist."
  )
