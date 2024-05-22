package works.iterative.tapir

import zio.*
import zio.test.*
import sttp.capabilities.zio.ZioStreams

object LiveClientEndpointFactorySpec extends ZIOSpecDefault with CustomTapir:

    val testFactory = ZIO
        .service[ClientEndpointFactory]
        .map(_.asInstanceOf[LiveClientEndpointFactory])

    override def spec = suite("ClientEndpointFactory")(
        test("can make a request for a secure endpoint") {
            for factory <- testFactory
            yield
                val _ = factory.makeRequest(endpoint.get.in("api" / "test"))
                assertCompletes
        },
        test("can make a request for a WS secure endpoint") {
            for factory <- testFactory
            yield
                val _ = factory.makeRequest(
                    endpoint.get
                        .in("api" / "test")
                        .out(
                            webSocketBodyRaw(ZioStreams)
                        )
                )
                assertCompletes
        },
        test("can make an effect for a infallible endpoint") {
            for factory <- testFactory
            yield
                val _ = factory.make(endpoint.get.in("api" / "test"))
                assertCompletes
        },
        test("can make an effect for a fallible endpoint") {
            for factory <- testFactory
            yield
                val _ = factory.make(
                    endpoint.get.in("api" / "test").errorOut(stringBody)
                )
                assertCompletes
        },
        test("can make an effect for secure endpoint") {
            for factory <- testFactory
            yield
                val _ = factory.make(
                    endpoint.get
                        .securityIn(auth.apiKey(header[String]("X-Access-Token")))
                        .in("api" / "test")
                        .errorOut(stringBody)
                )
                assertCompletes
        }
    ).provideShared(
        ZLayer.succeed(BaseUri("http://localhost:8080")),
        LiveClientEndpointFactory.default
    ) @@ TestAspect.ignore
end LiveClientEndpointFactorySpec
