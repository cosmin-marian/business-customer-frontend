package connectors


import java.util.UUID

import connectors.BusinessCustomerConnector
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.SessionId
import forms._
import uk.gov.hmrc.play.http.logging.SessionId
import scala.Some

class BusinessCustomerConnectorSpec extends PlaySpec with OneServerPerSuite {

  "BusinessCustomerConnector" must {

    "services are running" must {

      val matchSuccessResponse = Json.parse("""{"businessName":"ACME","businessType":"Unincorporated body","businessAddress":"23 High Street\nPark View\nThe Park\nGloucester\nGloucestershire\nABC 123","businessTelephone":"201234567890","businessEmail":"contact@acme.com"}""")
      val matchFailureResponse = Json.parse("""{"error": "Sorry. Business details not found."}""")


      "for a successful match, return business details" in {

        val businessDetails = BusinessDetails("UIB", SoleTraderMatch(None, None, None), LimitedCompanyMatch(None, None), UnincorporatedMatch(Some("ACME"), Some(1111111111)), OrdinaryBusinessPartnershipMatch(None, None), LimitedLiabilityPartnershipMatch(None, None))
        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        val result = BusinessCustomerConnector.lookup(businessDetails)
        await(result).as[JsValue] must be(matchSuccessResponse)

      }

      "for unsuccessful match, return error message" in {
        val businessDetails = BusinessDetails("UIB", SoleTraderMatch(None, None, None), LimitedCompanyMatch(None, None), UnincorporatedMatch(Some("ACME"), Some(1111111112)), OrdinaryBusinessPartnershipMatch(None, None), LimitedLiabilityPartnershipMatch(None, None))
        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        val result = BusinessCustomerConnector.lookup(businessDetails)
        await(result).as[JsValue] must be(matchFailureResponse)
      }



    }

  }

}
