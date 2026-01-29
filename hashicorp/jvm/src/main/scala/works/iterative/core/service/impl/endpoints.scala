package works.iterative.core.service.impl

import zio.*
import sttp.tapir.EndpointIO.annotations.*
import sttp.tapir.Schema
import zio.json.*

final case class ConsulToken(token: Option[String])

object ConsulToken:
    def fromEnv: ZLayer[Any, SecurityException, ConsulToken] = ZLayer {
        System.env("CONSUL_TOKEN").map(ConsulToken(_))
    }
end ConsulToken

@jsonMemberNames(PascalCase)
final case class ConsulMetadata(
    createIndex: Long,
    modifyIndex: Long,
    lockIndex: Long,
    key: String,
    flags: Long,
    value: String,
    session: Option[String]
) derives JsonCodec,
      Schema

@endpointInput("v1/kv/{key}")
final case class GetQuery(
    @path
    @description("Key to get from the KV store")
    key: String,
    @query
    @description("Datacenter to query")
    dc: Option[String] = None,
    // TODO: This would not work type-wise properly, as the result type of the call would differ depending on the query parameters
    // These are missing, split to different queries to enable.
    /*
    @query
    @description(
      "Specifies if the lookup should be recursive and treat key as a prefix instead of a literal match."
    )
    recurse: Option[Boolean] = None,
    @query
    @description(
      "Specifies the response is just the raw value of the key, without any encoding or metadata."
    )
    raw: Option[Boolean] = None,
    @query
    @description(
      "Specifies to return only keys (no values or metadata). Specifying this parameter implies recurse."
    )
    keys: Option[Boolean] = None,
    @query
    @description(
      "Specifies the string to use as a separator for recursive key lookups. This option is only used when paired with the keys parameter to limit the prefix of keys returned, only up to the given separator."
    )
    separator: Option[String] = None,
     */
    @query
    @description("Enterprise only. Specifies the namespace to query.")
    ns: Option[String] = None
)

@endpointInput("v1/kv/{key}")
final case class PutQuery(
    @path
    @description("Key to put into the KV store")
    key: String,
    @query
    @description("Datacenter to query")
    dc: Option[String] = None,
    @query
    @description(
        "Specifies an unsigned value between 0 and (2^64)-1 to store with the key. API consumers can use this field any way they choose for their application."
    )
    flags: Option[Long] = None,
    @query
    @description(
        "Specifies to use a Check-And-Set operation. This is very useful as a building block for more complex synchronization primitives. If the index is 0, Consul will only put the key if it does not already exist. If the index is non-zero, the key is only set if the index matches the ModifyIndex of that key."
    )
    cas: Option[Long] = None,
    @query
    @description("Supply a session ID to use in a lock acquisition operation.")
    acquire: Option[String] = None,
    @query
    @description("Supply a session ID to use in a release operation.")
    release: Option[String] = None,
    @query
    @description("Enterprise only. Specifies the namespace to query.")
    ns: Option[String] = None
)

@endpointInput("v1/kv/{key}")
final case class DeleteQuery(
    @path
    @description("Key to delete in the KV store")
    key: String,
    @query
    @description("Datacenter to query")
    dc: Option[String] = None,
    @query
    @description(
        "Specifies if the lookup should be recursive and treat key as a prefix instead of a literal match."
    )
    recurse: Option[Boolean] = None,
    @query
    @description(
        "Specifies to use a Check-And-Set operation. This is very useful as a building block for more complex synchronization primitives. If the index is 0, Consul will only put the key if it does not already exist. If the index is non-zero, the key is only set if the index matches the ModifyIndex of that key."
    )
    cas: Option[Long] = None,
    @query
    @description("Enterprise only. Specifies the namespace to query.")
    ns: Option[String] = None
)

/*
sealed trait ConsulGetError
object ConsulGetError:
  case object NotFound extends ConsulGetError derives JsonCodec, Schema
  case object Forbidden extends ConsulGetError derives JsonCodec, Schema
  case class ServerError(msg: String) extends ConsulGetError derives JsonCodec, Schema

trait ConsulKVEndpoints extends CustomTapir:
  private val kvBase = endpoint
    .in("v1" / "kv")
    .securityIn(
      auth.apiKey(header[Option[String]]("X-Consul-Token").mapTo[ConsulToken])
    )

  val getKV: Endpoint[ConsulToken, GetQuery, Unit, ConsulMetadata, Any] =
    kvBase.get
      .in(EndpointInput.derived[GetQuery])
      .out(jsonBody[ConsulMetadata])
      .errorOut(
        oneOf[ConsulGetError](
          oneOfVariant(
            statusCode(StatusCode.NotFound).and(jsonBody[ConsulGetError.NotFound.type].description("not found"))
          ),oneOfVariant(
            statusCode(StatusCode.Forbidden).and(jsonBody[ConsulGetError.NotFound.type].description("not found"))
          ),oneOfDefaultVariant(
            statusCode(StatusCode.InternalServerError).and(jsonBody[ServerError].description("not found"))
          )
        )
      )
  val putKV: Endpoint[ConsulToken, PutQuery, Unit, Boolean, Any] =
    kvBase.put.in(EndpointInput.derived[PutQuery]).out(jsonBody[Boolean])
  val deleteKV: Endpoint[ConsulToken, DeleteQuery, Unit, Boolean, Any] =
    kvBase.delete.in(EndpointInput.derived[DeleteQuery]).out(jsonBody[Boolean])

 */
