package builders

import models.SubscriptionDetails
import org.mockito.Matchers
import org.mockito.Mockito._
import services.SubscriptionDetailsService
import uk.gov.hmrc.domain._
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain._
import scala.concurrent.Future


object AuthBuilder {

  def createUserAuthContext(userId: String, userName: String) :AuthContext = {
    AuthContext(authority = createUserAuthority(userId),  nameFromSession = Some(userName))
  }

  def createAgentAuthContext(userId: String, userName: String) :AuthContext = {
    AuthContext(authority = createAgentAuthority(userId), nameFromSession = Some(userName))
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
      Future.successful(Some(createAgentAuthority(userId)))
    }
  }

  def mockUnAuthorisedUser(userId:String, mockAuthConnector: AuthConnector) {
    when(mockAuthConnector.currentAuthority(Matchers.any())) thenReturn {
      Future.successful(Some(createInvalidAuthority(userId)))
    }
  }

  def mockSubscriptionDetailsForUser(service: String, mockSubscriptionDetailsService : SubscriptionDetailsService) = {
    val subscriptionDetails = SubscriptionDetails(service = service, isAgent = false)
    when(mockSubscriptionDetailsService.fetchSubscriptionDetails(Matchers.any())).thenReturn(Future.successful(subscriptionDetails))
  }

  def mockSubscriptionDetailsForAgent(service: String, mockSubscriptionDetailsService : SubscriptionDetailsService) = {
    val subscriptionDetails = SubscriptionDetails(service = service, isAgent = true)
    when(mockSubscriptionDetailsService.fetchSubscriptionDetails(Matchers.any())).thenReturn(Future.successful(subscriptionDetails))
  }

  private def createInvalidAuthority(userId: String) :Authority = {
    Authority(userId, Accounts(paye = Some(PayeAccount("paye/AA026813", Nino("AA026813B")))), None, None)
  }

  private def createUserAuthority(userId: String) :Authority = {
    Authority(userId, Accounts(org = Some(OrgAccount("org/1234", Org("1234")))), None, None)
  }

  private def createAgentAuthority(userId: String) :Authority = {
    val agentAccount = AgentAccount(link = "agent/1234",
      agentCode = AgentCode(""),
      agentUserId = AgentUserId(userId),
      agentUserRole = AgentAdmin,
      payeReference = None)
    Authority(userId, Accounts(agent = Some(agentAccount)), None, None)
  }

}

