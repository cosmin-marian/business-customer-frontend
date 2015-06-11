package builders

import org.mockito.Matchers
import org.mockito.Mockito._
import uk.gov.hmrc.domain._
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain._
import scala.concurrent.Future


object AuthBuilder {

  def createUserAuthContext(userId: String, userName: String) :AuthContext = {
    val orgAuthority = Authority(userId, Accounts(org = Some(OrgAccount(userId, Org("1234")))), None, None)
    AuthContext(authority = orgAuthority,  nameFromSession = Some(userName))
  }

  def mockAuthorisedUser(userId:String, mockAuthConnector: AuthConnector)  {
    when(mockAuthConnector.currentAuthority(Matchers.any())) thenReturn {
      val orgAuthority = Authority(userId, Accounts(org = Some(OrgAccount("org/1234", Org("1234")))), None, None)
      Future.successful(Some(orgAuthority))

    }
  }

  def mockAuthorisedAgent(userId:String, mockAuthConnector: AuthConnector)  {
    when(mockAuthConnector.currentAuthority(Matchers.any())) thenReturn {
      val agentAccount = AgentAccount(link="agent/1234",
        agentCode=AgentCode(""),
        agentUserId = AgentUserId(userId),
        agentUserRole = AgentAdmin,
        payeReference = None)
      val agentAuthority = Authority(userId, Accounts(agent = Some(agentAccount)), None, None)
      Future.successful(Some(agentAuthority))
    }
  }

  def mockUnAuthorisedUser(userId:String, mockAuthConnector: AuthConnector) {
    when(mockAuthConnector.currentAuthority(Matchers.any())) thenReturn {
      val payeAuthority = Authority(userId, Accounts(paye = Some(PayeAccount(userId, Nino("AA026813B")))), None, None)
      Future.successful(Some(payeAuthority))
    }
  }
}
