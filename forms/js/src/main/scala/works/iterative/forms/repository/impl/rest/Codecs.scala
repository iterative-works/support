package portaly
package forms
package service
package impl.rest

import zio.json.*
import works.iterative.tapir.CustomTapir.*
import works.iterative.forms.repository.{Submission, SubmissionRepository}
import works.iterative.tapir.codecs.Codecs.given
import FormPersistenceCodecs.given

trait Codecs extends JsonCodecs with TapirCodecs

trait JsonCodecs:
    given JsonCodec[Submission] = DeriveJsonCodec.gen[Submission]
    given JsonCodec[SubmitResult] = DeriveJsonCodec.gen[SubmitResult]
    given JsonCodec[SubmissionRepository.Query] = DeriveJsonCodec.gen[SubmissionRepository.Query]
end JsonCodecs

trait TapirCodecs:
    given Schema[Submission] = Schema.derived[Submission]
    given Schema[SubmitResult] = Schema.derived[SubmitResult]
    given Schema[SubmissionRepository.Query] = Schema.derived[SubmissionRepository.Query]
end TapirCodecs

object Codecs extends Codecs
