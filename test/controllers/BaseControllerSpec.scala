package controllers

import builders.AuthBuilder
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.test.FakeRequest
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

class BaseControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val mockAuthConnector = mock[AuthConnector]

  override def beforeEach = {
    reset(mockAuthConnector)
  }

  object TestBaseController extends BaseController {
    override val authConnector = mockAuthConnector
  }


  "BaseController" must {
    "bcContext2Request - gives request out of bc context" in {
      implicit val user = AuthBuilder.createUserAuthContext("user-id", "user-name")
      TestBaseController.bcContext2Request must be(FakeRequest())
    }

    "bcContext2AuthContext - gives auth context out of bc context" in {
      implicit val user = AuthBuilder.createUserAuthContext("user-id", "user-name")
      TestBaseController.bcContext2AuthContext must be(user.user.authContext)
    }
  }

}
