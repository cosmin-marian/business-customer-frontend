package controllers.auth

import play.api.Play
import play.api.Play.current
import uk.gov.hmrc.play.config.RunMode

object ExternalUrls extends RunMode {

  val companyAuthHost = s"${Play.configuration.getString(s"govuk-tax.$env.services.auth.company-auth.host").getOrElse("")}"

  val loginCallback = Play.configuration.getString(s"govuk-tax.$env.services.auth.login-callback.url").getOrElse("/business-customer")

  def continueURL(serviceName: String) = s"$loginCallback/$serviceName"

  val loginPath = s"${Play.configuration.getString(s"govuk-tax.$env.services.auth.login-path").getOrElse("sign-in")}"

  val loginURL = s"$companyAuthHost/gg/$loginPath"

  def signIn(serviceName: String) = s"$companyAuthHost/gg/$loginPath?continue=${continueURL(serviceName)}"

  val signOut = s"$companyAuthHost/gg/sign-out"

  def agentConfirmationPath(service:String) :String = {
    Play.configuration.getString(s"govuk-tax.$env.services.${service.toLowerCase}.agentConfirmationUrl")
      .getOrElse("/ated-subscription/agent-confirmation")
  }

  def serviceWelcomePath(service: String): String = {
    Play.configuration.getString(s"govuk-tax.$env.services.${service.toLowerCase}.serviceStartUrl").getOrElse("#")
  }

  def serviceAccountPath(service: String): String = {
    Play.configuration.getString(s"govuk-tax.$env.services.${service.toLowerCase}.accountSummaryUrl").getOrElse("#")
  }

  val addClientEmailPath = Play.configuration.getString(s"govuk-tax.$env.services.agent-client-mandate-frontend.select-service").getOrElse("#")

}

