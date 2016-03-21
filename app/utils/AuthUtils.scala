package utils

import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{AgentAccount, SaAccount}

object AuthUtils extends AuthUtils

trait AuthUtils {

  def getAuthLink()(implicit user: AuthContext) = {
    (user.principal.accounts.org, user.principal.accounts.sa, user.principal.accounts.agent) match {
      case (Some(orgAccount), _, _) => orgAccount.link
      case (None, Some(saAccount), _) => stripIndividual(saAccount)
      case (None, None, Some(agent)) => getAgentLink(agent)
      case _ => throw new RuntimeException("User does not have the correct authorisation")
    }
  }

  private def getAgentLink(agentAccount: AgentAccount) = {
    agentAccount.link
  }

  private def stripIndividual(saAccount: SaAccount) = {
    saAccount.link.replaceAllLiterally("/individual", "")
  }

  def isAgent()(implicit user: AuthContext) = {
    user.principal.accounts.agent.isDefined
  }

  def isSaAccount()(implicit user: AuthContext) = {
    user.principal.accounts.sa match {
      case Some(_) =>
      {
        println("IN HERE")
        Some(true)
      }
      case None => {
        println("IN HERE2")
        Some(false)
      }
    }
  }
}
