package portaly.forms
package repository
package impl.rest

import works.iterative.tapir.CustomTapir.*
import sttp.capabilities.zio.ZioStreams

trait FormReadRepositoryApi(endpoints: FormReadRepositoryEndpoints):
    val load: ZServerEndpoint[FormReadRepository, ZioStreams] =
        endpoints.load.zServerLogic((id, version) =>
            FormReadRepository.load(
                id,
                FormVersion(version)
            )
        )
end FormReadRepositoryApi
