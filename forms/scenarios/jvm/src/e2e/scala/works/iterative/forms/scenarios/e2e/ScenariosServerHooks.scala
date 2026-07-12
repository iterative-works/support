// PURPOSE: Starts the scenarios server in-process for the browser e2e suite
// PURPOSE: One server per run on the baseUrl port; avoids any dependence on an externally started server

package works.iterative.forms.scenarios.e2e

import com.typesafe.config.ConfigFactory
import io.cucumber.scala.*
import java.util.concurrent.atomic.AtomicReference
import works.iterative.forms.scenarios.ScenariosServer
import zio.*
import zio.http.Server

object ScenariosServerHooks extends ScalaDsl with EN:
    private val runtime = Runtime.default
    private val serverFiber =
        new AtomicReference[Option[Fiber.Runtime[Throwable, Nothing]]](None)

    BeforeAll {
        val baseUrl = ConfigFactory.load().getString("baseUrl")
        val port = java.net.URI.create(baseUrl).getPort()
        val fiber = Unsafe.unsafe { implicit u =>
            runtime.unsafe.fork(
                Server.serve(ScenariosServer.routes)
                    .provide(Server.defaultWithPort(port))
            )
        }
        serverFiber.set(Some(fiber))
        awaitReady(s"$baseUrl/ssrForm/page")
    }

    AfterAll {
        serverFiber.getAndSet(None).foreach { fiber =>
            Unsafe.unsafe { implicit u =>
                runtime.unsafe.run(fiber.interrupt): Unit
            }
        }
    }

    private def awaitReady(
        url: String,
        deadline: Long = java.lang.System.currentTimeMillis() + 30000
    ): Unit =
        val ready =
            try
                val conn = java.net.URI.create(url).toURL.openConnection()
                    .asInstanceOf[java.net.HttpURLConnection]
                conn.setConnectTimeout(1000)
                conn.setReadTimeout(1000)
                val ok = conn.getResponseCode() == 200
                conn.disconnect()
                ok
            catch case _: java.io.IOException => false
        if !ready then
            if java.lang.System.currentTimeMillis() > deadline then
                throw new IllegalStateException(s"Scenarios server did not become ready at $url")
            Thread.sleep(200)
            awaitReady(url, deadline)
    end awaitReady
end ScenariosServerHooks
