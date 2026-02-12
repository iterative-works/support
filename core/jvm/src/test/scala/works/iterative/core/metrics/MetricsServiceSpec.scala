// PURPOSE: Test suite for MetricsService and NoOpMetricsService
// PURPOSE: Verifies metrics recording API and no-op implementation

package works.iterative.core.metrics

import zio.*
import zio.test.*


object MetricsServiceSpec extends ZIOSpecDefault:
  def spec = suite("MetricsService")(
    suite("NoOpMetricsService")(
      test("recordCounter doesn't error"):
        for
          service <- ZIO.service[MetricsService]
          _ <- service.recordCounter("test.counter")
          _ <- service.recordCounter("test.counter", Map("tag1" -> "value1"))
        yield assertCompletes
      ,
      test("recordTimer doesn't error"):
        for
          service <- ZIO.service[MetricsService]
          _ <- service.recordTimer("test.timer", 100.millis)
          _ <- service.recordTimer("test.timer", 200.millis, Map("tag1" -> "value1"))
        yield assertCompletes
      ,
      test("recordGauge doesn't error"):
        for
          service <- ZIO.service[MetricsService]
          _ <- service.recordGauge("test.gauge", 42.0)
          _ <- service.recordGauge("test.gauge", 99.5, Map("tag1" -> "value1"))
        yield assertCompletes
    ).provide(NoOpMetricsService.layer)
  )
