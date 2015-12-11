package builders

import org.mockito.Matchers
import org.mockito.Mockito._
import uk.gov.hmrc.domain._
import uk.gov.hmrc.play.frontend.auth._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain._
import scala.concurrent.Future

object AuthBuilder {

  def createUserAuthContext(userId: String, userName: String) :AuthContext = {
    AuthContext(authority = createUserAuthority(userId),  nameFromSession = Some(userName))
  }

  def createSaAuthContext(userId: String, userName: String) :AuthContext = {
    AuthContext(authority = createSaAuthority(userId), nameFromSession = Some(userName))
  }

  def createAgentAuthContext(userId: String, userName: String) :AuthContext = {
    AuthContext(authority = createAgentAuthority(userId, AgentAdmin), nameFromSession = Some(userName))
  }

  def createAgentAssistantAuthContext(userId: String, userName: String) :AuthContext = {
    AuthContext(authority = createAgentAuthority(userId, AgentAssistant), nameFromSession = Some(userName))
  }
  def createInvalidAuthContext(userId: String, userName: String) :AuthContext = {
    AuthContext(authority = createInvalidAuthority(userId),  nameFromSession = Some(userName))
  }

  def mockAuthorisedUser(userId:String, mockAuthConnector: AuthConnector)  {
    when(mockAuthConnector.currentAuthority(Matchers.any())) thenReturn {
      Future.successful(Some(createUserAuthority(userId)))
    }
  }

  def mockAuthorisedAgent(userId:String, mockAuthConnector: AuthConnector)  {
    when(mockAuthConnector.currentAuthority(Matchers.any())) thenReturn {
      Future.successful(Some(createAgentAuthority(userId, AgentAdmin)))
    }
  }

  def mockUnAuthorisedUser(userId:String, mockAuthConnector: AuthConnector) {
    when(mockAuthConnector.currentAuthority(Matchers.any())) thenReturn {
      Future.successful(Some(createInvalidAuthority(userId)))
    }
  }

  private def createInvalidAuthority(userId: String) :Authority = {
    Authority(userId, Accounts(paye = Some(PayeAccount("paye/AA026813", Nino("AA026813B")))), None, None, CredentialStrength.Weak, ConfidenceLevel.L50)
  }

  private def createUserAuthority(userId: String) :Authority = {
    Authority(userId, Accounts(org = Some(OrgAccount("org/1234", Org("1234")))), None, None, CredentialStrength.Weak, ConfidenceLevel.L50)
  }

  private def createSaAuthority(userId: String) :Authority = {
    Authority(userId, Accounts(sa = Some(SaAccount("sa/individual/8040200778", SaUtr("8040200778")))), None, None, CredentialStrength.Weak, ConfidenceLevel.L50)
  }


  private def createAgentAuthority(userId: String, agentRole : AgentRole) :Authority = {
    val agentAccount = AgentAccount(link = "agent/1234",
      agentCode = AgentCode(""),
      agentUserId = AgentUserId(userId),
      agentUserRole = agentRole,
      payeReference = None)
    Authority(userId, Accounts(agent = Some(agentAccount)), None, None, CredentialStrength.Weak, ConfidenceLevel.L50)
  }

}

