package metrics

import com.codahale.metrics.Timer
import com.codahale.metrics.Timer.Context
import uk.gov.hmrc.play.graphite.MicroserviceMetrics
import metrics.MetricsEnum.MetricsEnum

trait Metrics {

  def startTimer(api: MetricsEnum): Timer.Context

  def incrementSuccessCounter(api: MetricsEnum): Unit

  def incrementFailedCounter(api: MetricsEnum): Unit

}

object Metrics extends Metrics  with MicroserviceMetrics {
  val registry = metrics.defaultRegistry
  val timers = Map(
    MetricsEnum.GG_AGENT_ENROL -> registry.timer("gg-enrol-agent-ated-response-timer")
  )

  val successCounters = Map(
    MetricsEnum.GG_AGENT_ENROL -> registry.counter("gg-enrol-agent-ated-success-counter")
  )

  val failedCounters = Map(
    MetricsEnum.GG_AGENT_ENROL -> registry.counter("gg-enrol-agent-ated-failed-counter")
  )

  override def startTimer(api: MetricsEnum): Context = timers(api).time()

  override def incrementSuccessCounter(api: MetricsEnum): Unit = successCounters(api).inc()

  override def incrementFailedCounter(api: MetricsEnum): Unit = failedCounters(api).inc()

}
