package config

import uk.gov.hmrc.play.audit.http.config.LoadAuditingConfig
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.{AppName, RunMode}
import uk.gov.hmrc.play.http.ws.{WSDelete, WSGet, WSPost, WSPut}
import uk.gov.hmrc.play.partials.CachedStaticFormPartial

object BusinessCustomerFrontendAuditConnector extends AuditConnector with AppName with RunMode {
  override lazy val auditingConfig = LoadAuditingConfig(s"$env.auditing")
}

object WSHttp extends WSGet with WSPut with WSPost with WSDelete with AppName with RunMode {
  override val auditConnector = BusinessCustomerFrontendAuditConnector
}

object CachedStaticHtmlPartialProvider extends CachedStaticFormPartial {
  override val httpGet = WSHttp
}
