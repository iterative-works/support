package works.iterative.tapir

import sttp.model.Uri
import sttp.model.Uri.*
import sttp.capabilities.zio.ZioStreams

case class BaseUri(value: Option[Uri]):
    def toUri: Option[Uri] = value

    def toWSUri: Option[Uri] = value.map(u =>
        u.scheme match
            case Some("https") => u.scheme("wss")
            case _             => u.scheme("ws")
    )

    private def mapUriOrRoot(f: Uri => Uri): BaseUri = value match
        case Some(u) => BaseUri(f(u))
        case None    => BaseUri(f(BaseUri.RootUri))

    def /(s: String): BaseUri = mapUriOrRoot(_.addPath(s))

    def appendCompletePath(p: String): BaseUri =
        mapUriOrRoot: u =>
            val pWithoutLeadingSlash = if p.startsWith("/") then p.substring(1) else p
            val ps = pWithoutLeadingSlash.split("/", -1).toList
            u.addPath(ps)

    def href: String =
        value.fold("#")(_.toString)

    def orRoot: String =
        value.fold("/")(_.toString)

    def withSuffix(suffix: String): String =
        value.fold(suffix)(_.toString + suffix)
end BaseUri

object BaseUri extends BaseUriPlatformSpecific:
    val RootUri = uri"/"
    val Root = BaseUri(Some(RootUri))
    val Empty = BaseUri(None)

    def apply(s: String): BaseUri = BaseUri(Some(uri"$s"))
    def apply(u: Uri): BaseUri = BaseUri(Some(u))
end BaseUri

trait BaseUriExtractor[-O]:
    def extractBaseUri(using baseUri: BaseUri): Option[Uri]

object BaseUriExtractor extends LowPriorityBaseUriImplicits:
    given extractWSBaseUri[A, B]: BaseUriExtractor[ZioStreams.Pipe[A, B]] with
        def extractBaseUri(using baseUri: BaseUri): Option[Uri] = baseUri.toWSUri

trait LowPriorityBaseUriImplicits:
    given extractBaseUri: BaseUriExtractor[Any] with
        def extractBaseUri(using baseUri: BaseUri): Option[Uri] = baseUri.toUri
