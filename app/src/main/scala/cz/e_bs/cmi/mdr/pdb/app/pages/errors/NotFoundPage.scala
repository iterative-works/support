package cz.e_bs.cmi.mdr.pdb.app.pages.errors

import com.raquo.domtypes.generic.codecs.StringAsIsCodec
import com.raquo.laminar.api.L.{*, given}

def NotFoundPage(url: String, basePath: String): HtmlElement =
  ErrorPage(
    basePath,
    "404 error",
    "Page not found.",
    s"Sorry, but page $url doesn't exist."
  )
