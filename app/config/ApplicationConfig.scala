package config

import play.api.Play._
import uk.gov.hmrc.play.config.ServicesConfig

trait ApplicationConfig {

  val betaFeedbackUrl: String
  val betaFeedbackUnauthenticatedUrl: String
  val analyticsToken: Option[String]
  val analyticsHost: String
  val reportAProblemPartialUrl: String
  val reportAProblemNonJSUrl: String
  val verificationPageBackLink: String
  val verificationPageAgentBackLink: String
  val defaultTimeoutSeconds: Int
  val timeoutCountdown: Int
  val logoutUrl: String

  def serviceSignOutUrl(service: Option[String]): String
}

object ApplicationConfig extends ApplicationConfig with ServicesConfig {

  private def loadConfig(key: String) = configuration.getString(key).getOrElse(throw new Exception(s"Missing key: $key"))

  private val contactFrontendService = baseUrl("contact-frontend")
  private val contactHost = configuration.getString(s"govuk-tax.$env.contact-frontend.host").getOrElse("")

  val contactFormServiceIdentifier = "BUSINESS-CUSTOMER"

  override lazy val betaFeedbackUrl = s"$contactHost/contact/beta-feedback"
  override lazy val betaFeedbackUnauthenticatedUrl = s"$contactHost/contact/beta-feedback-unauthenticated"
  override lazy val analyticsToken: Option[String] = configuration.getString(s"govuk-tax.$env.google-analytics.token")
  override lazy val analyticsHost: String = configuration.getString(s"govuk-tax.$env.google-analytics.host").getOrElse("auto")
  override lazy val reportAProblemPartialUrl = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  override lazy val reportAProblemNonJSUrl = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
  override lazy val verificationPageBackLink = getConfString("ated.serviceReturnUrl", "/ated-subscription/appoint-agent")
  override lazy val verificationPageAgentBackLink = getConfString("ated.serviceAgentReturnUrl", "/ated-subscription/start-agent-subscription")
  override lazy val defaultTimeoutSeconds: Int = loadConfig("defaultTimeoutSeconds").toInt
  override lazy val timeoutCountdown: Int = loadConfig("timeoutCountdown").toInt
  override lazy val logoutUrl = s"""${configuration.getString(s"govuk-tax.$env.logout.url").getOrElse("/gg/sign-out")}"""

  override def serviceSignOutUrl(service: Option[String]): String = {
    service match {
      case Some(delegatedService) if (!delegatedService.isEmpty()) => configuration.getString(s"govuk-tax.$env.delegated-service-sign-out-url.${delegatedService.toLowerCase}").getOrElse(logoutUrl)
      case _ => logoutUrl
    }
  }
}
