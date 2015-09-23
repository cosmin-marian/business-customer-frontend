package builders

import java.util.concurrent.ConcurrentLinkedQueue

import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.audit.model.{AuditAsMagnet, Audit}
import config.BusinessCustomerFrontendAuditConnector
import uk.gov.hmrc.play.audit.model.Audit._

class TestAudit() extends Audit("test", BusinessCustomerFrontendAuditConnector) {
  var capturedTxName: String = ""
  var capturedInputs: Map[String, String] = Map.empty
  private val dataEvents = new ConcurrentLinkedQueue[DataEvent]

  override def as[A](auditMagnet: AuditAsMagnet[A])(body: Body[A])(implicit hc: HeaderCarrier): A = {
    this.capturedTxName = auditMagnet.txName
    this.capturedInputs = auditMagnet.inputs
    super.as(auditMagnet)(body)
  }

  def capturedDataEvents: Seq[DataEvent] = dataEvents.toArray(new Array[DataEvent](0)).toSeq

  def captureDataEvent(event: DataEvent) = {
    this.dataEvents.add(event)
    ()
  }

  override def sendDataEvent: (DataEvent) => Unit = captureDataEvent
}

