// PURPOSE: Service contract for submitting a form through datová schránka —
// PURPOSE: the Czech government data-box channel
package portaly.forms
package service

import zio.*

trait DsSubmissionService:
    def submitDs(data: FormContent): UIO[SubmitResult]

object DsSubmissionService:
    def submitDs(data: FormContent): URIO[DsSubmissionService, SubmitResult] =
        ZIO.serviceWithZIO(_.submitDs(data))
