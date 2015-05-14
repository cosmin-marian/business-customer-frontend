package controllers.auth

import uk.gov.hmrc.play.frontend.auth.connectors.domain.Accounts
import uk.gov.hmrc.play.frontend.auth.{AuthenticationProvider, TaxRegime}

case class BusinessCustomerRegime(serviceName: String) extends TaxRegime {

  override def isAuthorised(accounts: Accounts): Boolean = accounts.org.isDefined

  override def authenticationType: AuthenticationProvider = BusinessCustomerGovernmentGateway(serviceName)

  override def unauthorisedLandingPage: Option[String] = Some(controllers.routes.ApplicationController.unauthorised().url)

}
