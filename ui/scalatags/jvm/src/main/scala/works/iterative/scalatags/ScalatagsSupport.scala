package works.iterative.scalatags

trait ScalatagsSupport:
    // Extracted from http4s-scalatags - supported scalatags version lags behind by a year, and it is just a bunch of givens.

    import _root_.scalatags.Text.all.doctype
    import _root_.scalatags.generic.Frag
    import org.http4s.*
    import org.http4s.Charset.`UTF-8`
    import org.http4s.headers.`Content-Type`

    given scalatagsEncoder[F[_], C <: Frag[?, String]](using
        charset: Charset = `UTF-8`
    ): EntityEncoder[F, C] =
        contentEncoder(MediaType.text.html)

    private def contentEncoder[F[_], C <: Frag[?, String]](
        mediaType: MediaType
    )(using charset: Charset): EntityEncoder[F, C] =
        EntityEncoder
            .stringEncoder[F]
            .contramap[C](content => content.render)
            .withContentType(`Content-Type`(mediaType, charset))

    given doctypeEncoder[F[_]](using
        charset: Charset = `UTF-8`
    ): EntityEncoder[F, doctype] =
        doctypeContentEncoder(MediaType.text.html)

    private def doctypeContentEncoder[F[_]](
        mediaType: MediaType
    )(using charset: Charset): EntityEncoder[F, doctype] =
        EntityEncoder
            .stringEncoder[F]
            .contramap[doctype](content => content.render)
            .withContentType(`Content-Type`(mediaType, charset))
end ScalatagsSupport
