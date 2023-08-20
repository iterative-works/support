package works.iterative.ui.components.laminar

import zio.*
import zio.test.*
import com.raquo.airstream.core.EventStream
import com.raquo.airstream.core.Observer
import com.raquo.airstream.ownership.Owner

object ZIOLaminarInteropSpec extends ZIOSpecDefault:
  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("ZIO-Laminar interop should")(
      test("run a ZIO effect to EventStream") {
        import LaminarExtensions.*

        given runtime: Runtime[Any] = Runtime.default
        given owner: Owner = new Owner {
          def killAll(): Unit = this.killSubscriptions()
        }
        val ev: EventStream[String] = ZIO.succeed("Hello").toEventStream
        val buffer = collection.mutable.Buffer[String]()
        val subscription = ev.foreach(buffer += _)
        subscription.kill()
        assertTrue(buffer.size == 1, buffer.head == "Hello")
      }
    )
