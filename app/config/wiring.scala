package config

import uk.gov.hmrc.play.audit.http.config.LoadAuditingConfig
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.{AppName, RunMode}
import uk.gov.hmrc.play.frontend.filters.SessionCookieCryptoFilter
import uk.gov.hmrc.play.http.ws.{WSDelete, WSGet, WSPost, WSPut}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.partials.{HeaderCarrierForPartialsConverter, CachedStaticHtmlPartial, FormPartial}

object BusinessCustomerFrontendAuditConnector extends AuditConnector with AppName with RunMode {
  override lazy val auditingConfig = LoadAuditingConfig(s"$env.auditing")
}

object WSHttp extends WSGet with WSPut with WSPost with WSDelete with AppName with RunMode {
  override val auditConnector = BusinessCustomerFrontendAuditConnector
}

object FormPartialProvider extends FormPartial with SessionCookieCryptoFilterWrapper {
  override val httpGet = WSHttp
  override val crypto = encryptCookieString _
}

object CachedStaticHtmlPartialProvider extends CachedStaticHtmlPartial {
  override val httpGet = WSHttp
}

object FrontendAuthConnector extends AuthConnector with ServicesConfig {
  val serviceUrl = baseUrl("auth")
  lazy val http = WSHttp
}

object BusinessCustomerHeaderCarrierForPartialsConverter extends HeaderCarrierForPartialsConverter with SessionCookieCryptoFilterWrapper {
  override val crypto = encryptCookieString _
}

trait SessionCookieCryptoFilterWrapper {

  def encryptCookieString(cookie: String) : String = {
    SessionCookieCryptoFilter.encryptCookieString(cookie).value
  }
}
