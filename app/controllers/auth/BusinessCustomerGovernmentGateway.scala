package controllers.auth

import uk.gov.hmrc.play.frontend.auth.GovernmentGateway

object BusinessCustomerGovernmentGateway extends GovernmentGateway {

  override val login = ExternalUrls.signIn
}
