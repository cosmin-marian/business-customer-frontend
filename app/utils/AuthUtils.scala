package utils

import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{AgentAdmin, AgentAccount, AgentRole}

object AuthUtils extends AuthUtils

trait AuthUtils {

  def getAuthLink()(implicit user: AuthContext) = {
    (user.principal.accounts.org, user.principal.accounts.agent) match {
      case (Some(org), _) => org.link
      case (None, Some(agent)) => getAgentLink(agent)
      case _ => throw new RuntimeException("User does not have the correct authorisation")
    }
  }

  private def getAgentLink(agentAccount : AgentAccount) = {
    if (agentAccount.agentUserRole.satisfiesRequiredRole(AgentAdmin)) {
      agentAccount.link + "/admin"
    } else {
      agentAccount.link + "/assistant"
    }
  }

  def isAgent()(implicit user: AuthContext) = {
    user.principal.accounts.agent.isDefined
  }
}
