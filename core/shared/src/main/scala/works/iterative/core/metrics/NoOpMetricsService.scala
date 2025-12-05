// PURPOSE: No-op implementation of MetricsService for testing and development
// PURPOSE: Silently discards all metrics - use when observability is not needed

package works.iterative.core.metrics

import zio.*

/** No-op metrics service that discards all metrics.
  *
  * This implementation is useful for:
  * - Testing environments where metrics aren't needed
  * - Development when you want to focus on functionality not observability
  * - Temporarily disabling metrics collection
  *
  * All methods return immediately without performing any work.
  */
class NoOpMetricsService extends MetricsService:
  override def recordCounter(name: String, tags: Map[String, String] = Map.empty): UIO[Unit] =
    ZIO.unit

  override def recordTimer(name: String, duration: Duration, tags: Map[String, String] = Map.empty): UIO[Unit] =
    ZIO.unit

  override def recordGauge(name: String, value: Double, tags: Map[String, String] = Map.empty): UIO[Unit] =
    ZIO.unit
end NoOpMetricsService

object NoOpMetricsService:
  val layer: ULayer[MetricsService] =
    ZLayer.succeed[MetricsService](NoOpMetricsService())
end NoOpMetricsService
