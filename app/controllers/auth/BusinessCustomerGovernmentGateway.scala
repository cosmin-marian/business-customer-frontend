package controllers.auth

import uk.gov.hmrc.play.frontend.auth.GovernmentGateway

case class BusinessCustomerGovernmentGateway(serviceName: String) extends GovernmentGateway {

  override val login = ExternalUrls.signIn + s"/$serviceName"
}
