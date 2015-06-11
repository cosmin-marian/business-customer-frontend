package utils

import play.api.i18n.Messages
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.InternalServerException


object AuthLink {

  def getAuthLink()(implicit user: AuthContext) = {
    (user.principal.accounts.org, user.principal.accounts.agent) match {
      case (Some(x), _) => x.link
      case (None, Some(x)) => x.link
      case _ => throw new InternalServerException(Messages("bc.connector.error.not-authorised"))
    }
  }
}
