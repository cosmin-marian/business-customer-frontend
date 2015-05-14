package builders

import org.mockito.Matchers
import org.mockito.Mockito._
import uk.gov.hmrc.domain.{Org, Nino}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Accounts
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Authority
import uk.gov.hmrc.play.frontend.auth.connectors.domain.PayeAccount
import uk.gov.hmrc.play.frontend.auth.connectors.domain.OrgAccount
import scala.concurrent.Future

/**
 * Created by dev01 on 13/05/15.
 */
object AuthBuilder {

  def createUserAuthContext(userId: String, userName: String) :AuthContext = {
    val orgAuthority = Authority(userId, Accounts(org = Some(OrgAccount(userId, Org("1234")))), None, None)
    AuthContext(authority = orgAuthority,  nameFromSession = Some(userName))
  }

  def mockAuthorisedUser(userId:String, mockAuthConnector: AuthConnector)  {
    when(mockAuthConnector.currentAuthority(Matchers.any())) thenReturn {
      val orgAuthority = Authority(userId, Accounts(org = Some(OrgAccount(userId, Org("1234")))), None, None)
      Future.successful(Some(orgAuthority))

    }
  }

  def mockUnAuthorisedUser(userId:String, mockAuthConnector: AuthConnector) {
    when(mockAuthConnector.currentAuthority(Matchers.any())) thenReturn {
      val payeAuthority = Authority(userId, Accounts(paye = Some(PayeAccount(userId, Nino("AA026813B")))), None, None)
      Future.successful(Some(payeAuthority))
    }
  }
}
