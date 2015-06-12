package utils

import uk.gov.hmrc.play.frontend.auth.AuthContext

object AuthUtils extends AuthUtils

trait AuthUtils {

  def getAuthLink()(implicit user: AuthContext) = {
    (user.principal.accounts.org, user.principal.accounts.agent) match {
      case (Some(x), _) => x.link
      case (None, Some(x)) => x.link
      case _ => throw new RuntimeException("User does not have the correct authorisation")
    }
  }

  def isAgent()(implicit user: AuthContext) = {
    user.principal.accounts.agent.isDefined
  }
}
