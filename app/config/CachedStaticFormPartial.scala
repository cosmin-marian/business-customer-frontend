package config

import uk.gov.hmrc.play.http.HttpGet

object CachedStaticFormPartial extends uk.gov.hmrc.play.partials.CachedStaticFormPartial {
  override def httpGet: HttpGet = WSHttp
}
