package controllers.auth

import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import uk.gov.hmrc.play.auth.frontend.connectors.domain.Accounts
import controllers.auth.BusinessCustomerRegime._

class BusinessCustomerRegimeSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  "BusinessCustomerRegime" must {

    "define isAuthorised" must {

      val accounts = mock[Accounts](RETURNS_DEEP_STUBS)

      "return true when the user is registered for company tax" in {
        when(accounts.org.isDefined).thenReturn(true)
        isAuthorised(accounts) must be(true)
      }

      "return false when the user is not registered for company tax" in {
        when(accounts.org.isDefined).thenReturn(false)
        isAuthorised(accounts) must be(false)
      }

    }

    "define the authentication type as the BusinessCustomer GG" in {
      authenticationType must be(BusinessCustomerGovernmentGateway)
    }

    "define the unauthorised landing page as /unauthorised" in {
      unauthorisedLandingPage.isDefined must be(true)
      unauthorisedLandingPage.get must be("/business-customer/unauthorised")
    }

  }
  
}
