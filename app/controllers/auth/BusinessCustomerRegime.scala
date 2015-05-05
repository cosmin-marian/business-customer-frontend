package controllers.auth

import uk.gov.hmrc.play.auth.frontend.connectors.domain.Accounts
import uk.gov.hmrc.play.frontend.auth.{AuthenticationProvider, TaxRegime}

object BusinessCustomerRegime extends TaxRegime {

  override def isAuthorised(accounts: Accounts): Boolean = accounts.org.isDefined

  override def authenticationType: AuthenticationProvider = BusinessCustomerGovernmentGateway

  override def unauthorisedLandingPage: Option[String] = Some(controllers.routes.ApplicationController.unauthorised().url)

}
