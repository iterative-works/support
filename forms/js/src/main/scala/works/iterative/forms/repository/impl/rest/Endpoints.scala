package portaly
package forms
package service
package impl.rest

import works.iterative.tapir.endpoints.BaseEndpoint
import works.iterative.tapir.CustomTapir
import CustomTapir.*
import Codecs.given
import FormPersistenceCodecs.given
import works.iterative.tapir.codecs.Codecs.given
import sttp.model.StatusCode
import portaly.forms.repository.Submission
import works.iterative.autocomplete.endpoints.AutocompleteEndpoints
import sttp.model.headers.CookieValueWithMeta
import repository.impl.rest.FormReadRepositoryEndpoints
import works.iterative.tapir.endpoints.FileStoreEndpointsModule
import works.iterative.core.Language
import portaly.forms.repository.SubmissionRepository

trait Endpoints(base: BaseEndpoint) extends CustomTapir:
    object user:
        // TODO: Remove the cookie from here, move to the securedRoutes
        // code to make it into unified input of current user
        val me: Endpoint[Unit, Option[String], Unit, Option[User], Any] =
            base.get
                .in("user" / "me")
                .in(cookie[Option[String]]("dsinfo"))
                .out(jsonBody[Option[User]])

        val clientMe: Endpoint[Unit, Unit, Unit, Option[User], Any] =
            base.get
                .in("user" / "me")
                .out(jsonBody[Option[User]])

        // TODO: These endpoint definitions should not be in /api
        val login: Endpoint[Unit, Unit, Unit, String, Any] =
            base.get
                .in("login")
                .out(statusCode(StatusCode.Found).and(header[String]("Location")))

        val logout
            : Endpoint[Unit, Unit, Unit, (String, CookieValueWithMeta, CookieValueWithMeta), Any] =
            base.get
                .in("logout")
                .out(statusCode(StatusCode.Found).and(header[String]("Location")).and(
                    setCookie("dsinfo")
                ).and(setCookie("timeLimitedId")))
    end user

    object autocomplete extends AutocompleteEndpoints(base)

    object file extends FileStoreEndpointsModule(base)

    object submissions:
        val submit: Endpoint[Unit, (FormContent, Option[Language]), Unit, SubmitResult, Any] =
            base.post
                .in("submissions")
                .in(jsonBody[FormContent])
                .in(query[Option[Language]]("lang"))
                .out(jsonBody[SubmitResult])

        val submitDs: Endpoint[Unit, (String, FormContent), Unit, SubmitResult, Any] =
            base.post
                .in("submissions" / "ds")
                .in(cookie[String]("timeLimitedId"))
                .in(jsonBody[FormContent])
                .out(jsonBody[SubmitResult])

        val clientSubmitDs: Endpoint[Unit, FormContent, Unit, SubmitResult, Any] =
            base.post
                .in("submissions" / "ds")
                .in(jsonBody[FormContent])
                .out(jsonBody[SubmitResult])

        val loadPdf: Endpoint[Unit, (String, String), Unit, Array[
            Byte
        ], Any] =
            base.get
                .in("submissions" / "pdf" / path[String]("id") / path[String]("fileName"))
                .out(byteArrayBody)

        val renderPdf: Endpoint[Unit, (FormContent, String), Unit, Array[Byte], Any] =
            base.get
                .in(jsonBody[FormContent])
                .in("submissions" / path[String]("fileName"))
                .out(byteArrayBody)

        val load: Endpoint[Unit, String, Unit, Option[Submission], Any] = base.get
            .in("view" / "submissions2" / path[String]("id"))
            .out(jsonBody[Option[Submission]])

        val find: Endpoint[Unit, SubmissionRepository.Query, Unit, List[Submission], Any] =
            base.post
                .in("view" / "submissions")
                .in(jsonBody[SubmissionRepository.Query])
                .out(jsonBody[List[Submission]])

        val save: Endpoint[Unit, (String, Submission), Unit, Unit, Any] = base.put
            .in("view" / "submissions" / path[String]("id"))
            .in(jsonBody[Submission])
            .out(statusCode(StatusCode.Accepted))
    end submissions

    object ds:
        val login =
            base.get
                .in("ds" / "login")
                .in(query[String]("sessionId"))
                .in(query[Option[String]]("appToken"))
                .out(
                    statusCode(StatusCode.Found)
                        .and(header[String]("Location"))
                        .and(setCookie("dsinfo"))
                        .and(setCookie("timeLimitedId"))
                )

        val failed =
            base.get.in("ds" / "failed").out(
                statusCode(StatusCode.Found).and(header[String]("Location"))
            )
    end ds

    object forms extends FormReadRepositoryEndpoints(base)
end Endpoints

class AllEndpoints(base: BaseEndpoint) extends Endpoints(base)
    with AresEndpoints(base)
    with ViesEndpoints(base)

object Endpoints extends AllEndpoints(endpoint.in("api"))
