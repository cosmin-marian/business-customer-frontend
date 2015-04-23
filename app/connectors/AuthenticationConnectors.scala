package connectors

import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.FrontendAuthConnector

trait AuthenticationConnectors {
  lazy val auditConnector = AuditConnector
  lazy val authConnector = FrontendAuthConnector
}
