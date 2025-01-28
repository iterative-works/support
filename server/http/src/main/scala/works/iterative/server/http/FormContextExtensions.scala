package works.iterative.server.http

import org.http4s.UrlForm

object FormContextExtensions:
    extension (form: UrlForm)
        def toFormContext: FormContext = new FormContext:
            override def getString(id: String): Option[String] =
                form.getFirst(id).filterNot(_.isBlank())
end FormContextExtensions
