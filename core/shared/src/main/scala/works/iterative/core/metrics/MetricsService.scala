// PURPOSE: Abstraction for recording application metrics (counters, timers, gauges)
// PURPOSE: Provides standard interface for observability - implementation may be no-op or backed by metrics system

package works.iterative.core.metrics

import zio.*

/** Service for recording application metrics.
  *
  * This trait provides a common interface for recording metrics throughout the application.
  * Implementations may send metrics to monitoring systems (Prometheus, Datadog, etc.) or
  * be no-op for testing/development.
  *
  * Metric types:
  * - Counter: Monotonically increasing value (e.g., request count, error count)
  *   Use for events that only increase: requests processed, errors encountered, items created
  * - Timer: Duration measurements (e.g., request latency, operation duration)
  *   Use for measuring how long operations take: API response time, database query duration
  * - Gauge: Point-in-time value that can go up or down (e.g., queue size, active connections)
  *   Use for current state values: memory usage, queue depth, number of active sessions
  *
  * Tags allow adding dimensional data to metrics for filtering and aggregation.
  *
  * Current implementations:
  * - NoOpMetricsService: Discards all metrics (for testing/development)
  * - Production implementation backed by ZIO Metrics/Micrometer to be added in Phase 5
  */
trait MetricsService:
  /** Record an increment to a counter metric.
    *
    * Use counters for values that only increase (requests, errors, events).
    *
    * @param name Metric name (use dot notation: "auth.login.success")
    * @param tags Optional key-value pairs for metric dimensions
    */
  def recordCounter(name: String, tags: Map[String, String] = Map.empty): UIO[Unit]

  /** Record a duration measurement.
    *
    * Use timers for measuring how long operations take (request latency, processing time).
    *
    * @param name Metric name (use dot notation: "permission.check.duration")
    * @param duration The measured duration
    * @param tags Optional key-value pairs for metric dimensions
    */
  def recordTimer(name: String, duration: Duration, tags: Map[String, String] = Map.empty): UIO[Unit]

  /** Record a point-in-time value.
    *
    * Use gauges for values that fluctuate (queue depth, cache size, active sessions).
    *
    * @param name Metric name (use dot notation: "cache.size")
    * @param value The current value
    * @param tags Optional key-value pairs for metric dimensions
    */
  def recordGauge(name: String, value: Double, tags: Map[String, String] = Map.empty): UIO[Unit]
end MetricsService

object MetricNames:
  /** Duration of permission check operations (timer) */
  val PermissionCheckDuration = "permission.check.duration"

  /** Counter for permission check infrastructure failures (counter) */
  val PermissionCheckInfrastructureFailure = "permission.check.infrastructure_failure"

  /** Counter for successful authentication events (counter) */
  val AuthLoginSuccess = "auth.login.success"

  /** Counter for failed authentication attempts (counter) */
  val AuthLoginFailure = "auth.login.failure"
end MetricNames

