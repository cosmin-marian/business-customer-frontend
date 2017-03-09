package config

import play.api.Play._
import uk.gov.hmrc.play.config.ServicesConfig

trait ApplicationConfig {

  val defaultBetaFeedbackUrl: String
  def betaFeedbackUrl(service: Option[String], returnUri: String): String
  val betaFeedbackUnauthenticatedUrl: String
  val analyticsToken: Option[String]
  val analyticsHost: String
  val reportAProblemPartialUrl: String
  val reportAProblemNonJSUrl: String
  val defaultTimeoutSeconds: Int
  val timeoutCountdown: Int
  val logoutUrl: String

  def serviceSignOutUrl(service: Option[String]): String
  def validateNonUkClientPostCode(service: String): Boolean
}

object ApplicationConfig extends ApplicationConfig with ServicesConfig {

  private def loadConfig(key: String) = configuration.getString(key).getOrElse(throw new Exception(s"Missing key: $key"))

  private val contactFrontendService = baseUrl("contact-frontend")
  private val contactHost = configuration.getString(s"govuk-tax.$env.contact-frontend.host").getOrElse("")

  val contactFormServiceIdentifier = "BUSINESS-CUSTOMER"

  override lazy val defaultBetaFeedbackUrl = s"$contactHost/contact/beta-feedback"
  override def betaFeedbackUrl(service: Option[String], returnUri: String) = {
    val feedbackUrl = service match {
      case Some(delegatedService) if (!delegatedService.isEmpty()) =>
        configuration.getString(s"govuk-tax.$env.delegated-service.${delegatedService.toLowerCase}.beta-feedback-url").getOrElse(defaultBetaFeedbackUrl)
      case _ => defaultBetaFeedbackUrl
    }
    feedbackUrl + "?return=" + returnUri
  }
  override lazy val betaFeedbackUnauthenticatedUrl = s"$contactHost/contact/beta-feedback-unauthenticated"
  override lazy val analyticsToken: Option[String] = configuration.getString(s"govuk-tax.$env.google-analytics.token")
  override lazy val analyticsHost: String = configuration.getString(s"govuk-tax.$env.google-analytics.host").getOrElse("auto")
  override lazy val reportAProblemPartialUrl = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  override lazy val reportAProblemNonJSUrl = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
  override lazy val defaultTimeoutSeconds: Int = loadConfig("defaultTimeoutSeconds").toInt
  override lazy val timeoutCountdown: Int = loadConfig("timeoutCountdown").toInt
  override lazy val logoutUrl = s"""${configuration.getString(s"govuk-tax.$env.logout.url").getOrElse("/gg/sign-out")}"""

  override def serviceSignOutUrl(service: Option[String]): String = {
    service match {
      case Some(delegatedService) if (!delegatedService.isEmpty()) =>
        configuration.getString(s"govuk-tax.$env.delegated-service.${delegatedService.toLowerCase}.sign-out-url").getOrElse(logoutUrl)
      case _ => logoutUrl
    }
  }

  override def validateNonUkClientPostCode(service: String) = {
    configuration.getBoolean(s"govuk-tax.$env.services.${service.toLowerCase.trim}.validateNonUkClientPostCode").getOrElse(false)
  }
}
