package models

import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.play.frontend.auth.AuthContext

case class BusinessCustomerContext(request: Request[AnyContent], user: BusinessCustomerUser)

case class BusinessCustomerUser(authContext: AuthContext) {

  def isAgent: Boolean = authContext.principal.accounts.agent.isDefined

  def isSa: Boolean = authContext.principal.accounts.sa.isDefined

  def isOrg: Boolean = authContext.principal.accounts.ct.isDefined || authContext.principal.accounts.org.isDefined

  def authLink: String = {
    (authContext.principal.accounts.org, authContext.principal.accounts.sa, authContext.principal.accounts.agent) match {
      case (Some(orgAccount), _, _) => orgAccount.link
      case (None, Some(saAccount), _) => saAccount.link.replaceAllLiterally("/individual", "")
      case (None, None, Some(agent)) => agent.link
      case _ => throw new RuntimeException("User does not have the correct authorisation")
    }
  }

}
