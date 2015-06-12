package utils

import uk.gov.hmrc.play.frontend.auth.AuthContext

object AuthLink extends AuthLink

trait AuthLink {

  def getAuthLink()(implicit user: AuthContext) = {
    (user.principal.accounts.org, user.principal.accounts.agent) match {
      case (Some(x), _) => x.link
      case (None, Some(x)) => x.link
      case _ => throw new RuntimeException("User does not have the correct authorisation")
    }
  }
}
