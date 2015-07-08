package audit

import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.audit.model.{Audit, EventTypes}

trait Auditable {

  def appName: String

  def audit: Audit

  def sendDataEvent(transactionName: String, path: String = "N/A", tags: Map[String, String] = Map.empty[String, String], detail: Map[String, String])
                   (implicit hc: HeaderCarrier) =
    audit.sendDataEvent(DataEvent(appName, EventTypes.Succeeded,
      tags = hc.toAuditTags(transactionName, path) ++ tags,
      detail = hc.toAuditDetails(detail.toSeq: _*)))

}
