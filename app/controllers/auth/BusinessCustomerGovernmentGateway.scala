package controllers.auth

import uk.gov.hmrc.play.frontend.auth.GovernmentGateway

case class BusinessCustomerGovernmentGateway(serviceName: String) extends GovernmentGateway {

//  override val login = ExternalUrls.signIn(serviceName)
  override def loginURL = ExternalUrls.loginURL
  override def continueURL = ExternalUrls.continueURL(serviceName)
}
