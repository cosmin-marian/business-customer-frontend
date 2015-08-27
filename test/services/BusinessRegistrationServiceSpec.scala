package services

import _root_.java.util.UUID

import builders.{TestAudit, AuthBuilder}
import connectors.{DataCacheConnector, BusinessCustomerConnector}
import models._
import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.{Json, JsValue}
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.InternalServerException
import uk.gov.hmrc.play.http.logging.SessionId
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class BusinessRegistrationServiceSpec  extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfter {

  implicit val user = AuthBuilder.createUserAuthContext("userId", "joe bloggs")
  val mockDataCacheConnector = mock[DataCacheConnector]

  object TestBusinessRegistrationService extends BusinessRegistrationService {
    val businessCustomerConnector: BusinessCustomerConnector = TestConnector
    val dataCacheConnector = mockDataCacheConnector
    val nonUKbusinessType = "Non UK-based Company"
    override val audit: Audit = new TestAudit
    override val appName: String = "Test"
  }

  object TestConnector extends BusinessCustomerConnector {
    override def registerNonUk(registerData: BusinessRegistrationRequest)(implicit user: AuthContext, headerCarrier: HeaderCarrier): Future[BusinessKRegistrationResponse] = {
      val nonUKResponse =  BusinessKRegistrationResponse(processingDate = "2015-01-01",
        sapNumber = "SAP123123",
        safeId = "SAFE123123",
        agentReferenceNumber = Some("AREF123123"))

      Future(nonUKResponse)
    }
  }

  "BusinessRegistrationService" must {

    "use the correct data cache connector" in {
      BusinessRegistrationService.dataCacheConnector must be(DataCacheConnector)
    }

    "use the correct business Customer Connector" in {
      BusinessRegistrationService.businessCustomerConnector must be(BusinessCustomerConnector)
    }

    "save the response from the registration" in {
      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

      val busRegData = BusinessRegistration(businessName = "testName",
        businessAddress = Address("line1", "line2", Some("line3"), Some("line4"), Some("postCode"), "country"),
        businessUniqueId = Some(s"BUID-${UUID.randomUUID}"),
        issuingInstitution = Some("issuingInstitution")
      )

      val returnedReviewDetails = new ReviewDetails(businessName=busRegData.businessName, businessType=None, businessAddress=busRegData.businessAddress,
        sapNumber="sap123", safeId="safe123", agentReferenceNumber=Some("agent123"))
      when(mockDataCacheConnector.saveReviewDetails(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(returnedReviewDetails)))


      val regResult = TestBusinessRegistrationService.registerBusiness(busRegData)

      val reviewDetails = await(regResult)

      reviewDetails.businessName must be (busRegData.businessName)
      reviewDetails.businessAddress.line_1 must be (busRegData.businessAddress.line_1)
    }

    "save the response from the registration when we have no businessUniqueId or issuingInstitution" in {
      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

      val busRegData = BusinessRegistration(businessName = "testName",
        businessAddress = Address("line1", "line2", Some("line3"), Some("line4"), Some("postCode"), "country"),
        businessUniqueId = None,
        issuingInstitution = None
      )

      val returnedReviewDetails = new ReviewDetails(businessName=busRegData.businessName, businessType=None, businessAddress=busRegData.businessAddress,
        sapNumber="sap123", safeId="safe123", agentReferenceNumber=Some("agent123"))
      when(mockDataCacheConnector.saveReviewDetails(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(returnedReviewDetails)))

      val regResult = TestBusinessRegistrationService.registerBusiness(busRegData)
      val reviewDetails = await(regResult)

      reviewDetails.businessName must be (busRegData.businessName)
      reviewDetails.businessAddress.line_1 must be (busRegData.businessAddress.line_1)
    }

    "save the response fails from the registration" in {
      implicit val hc = new HeaderCarrier(sessionId = None)

      val busRegData = BusinessRegistration(businessName = "testName",
        businessAddress = Address("line1", "line2", Some("line3"), Some("line4"), Some("postCode"), "country"),
        businessUniqueId = Some(s"BUID-${UUID.randomUUID}"),
        issuingInstitution = Some("issuingInstitution")
      )

      val returnedReviewDetails = new ReviewDetails(businessName=busRegData.businessName, businessType=None, businessAddress=busRegData.businessAddress,
        sapNumber="sap123", safeId="safe123", agentReferenceNumber=Some("agent123"))
      when(mockDataCacheConnector.saveReviewDetails(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))


      val regResult = TestBusinessRegistrationService.registerBusiness(busRegData)

      val thrown = the[InternalServerException] thrownBy await(regResult)
      thrown.getMessage must include("Registration Failed")
    }
  }
}
