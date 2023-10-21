package works.iterative.core
package service
package impl

import works.iterative.tapir.{BaseUri, CustomTapir}
import zio.*
import sttp.client3.*

class ConsulKeyValueStore(
    token: ConsulToken,
    baseUri: BaseUri,
    backend: CustomTapir.Backend
) extends KeyValueStore[String, String]:

  private val defaultBase = "http://localhost:8500"
  private val base = baseUri.value.map(_.toString).getOrElse(defaultBase)

  private def addAuth[U[_], T, R](
      request: RequestT[U, T, R]
  ): RequestT[U, T, R] =
    request.header("X-Consul-Token", token.token)

  override def get(key: String): UIO[Option[String]] = {
    for
      response <- addAuth(basicRequest.get(uri"$base/v1/kv/${key}?raw=true"))
        .send(backend)
    yield response.body.toOption
  }.orDie

  override def put(key: String, value: String): UIO[Unit] = {
    addAuth(basicRequest.put(uri"$base/v1/kv/${key}").body(value))
      .send(backend)
      .unit
  }.orDie

  override def remove(key: String): UIO[Unit] = {
    addAuth(basicRequest.delete(uri"$base/v1/kv/${key}"))
      .send(backend)
      .unit
  }.orDie

object ConsulKeyValueStore:
  val layer: URLayer[
    ConsulToken & BaseUri & CustomTapir.Backend,
    StringKeyValueStore
  ] =
    ZLayer {
      for
        token <- ZIO.service[ConsulToken]
        baseUri <- ZIO.service[BaseUri]
        backend <- ZIO.service[CustomTapir.Backend]
      yield ConsulKeyValueStore(token, baseUri, backend)
    }

  val fromEnv
      : ZLayer[CustomTapir.Backend, SecurityException, StringKeyValueStore] =
    ConsulToken.fromEnv >>> ZLayer {
      for
        token <- ZIO.service[ConsulToken]
        backend <- ZIO.service[CustomTapir.Backend]
        addr <- System.envOrElse("CONSUL_ADDR", "http://localhost:8500")
      yield ConsulKeyValueStore(token, BaseUri(addr), backend)
    }
