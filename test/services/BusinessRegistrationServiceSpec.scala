package services

import java.util.UUID

import builders.{AuthBuilder, TestAudit}
import connectors.{BusinessCustomerConnector, DataCacheConnector}
import models._
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.http.hooks.HttpHook
import uk.gov.hmrc.play.http.logging.SessionId
import uk.gov.hmrc.play.http.ws.{WSGet, WSPost}
import uk.gov.hmrc.play.http._
import utils.BusinessCustomerConstants

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BusinessRegistrationServiceSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  implicit val user = AuthBuilder.createUserAuthContext("userId", "joe bloggs")
  val mockDataCacheConnector = mock[DataCacheConnector]
  val service = "ATED"

  class MockHttp extends WSGet with WSPost {
    override val hooks: Seq[HttpHook] = NoneRequired
  }

  val mockWSHttp = mock[MockHttp]
  val mockBusinessCustomerConnector = mock[BusinessCustomerConnector]

  object TestBusinessRegistrationService extends BusinessRegistrationService {
    val businessCustomerConnector: BusinessCustomerConnector = mockBusinessCustomerConnector
    val dataCacheConnector = mockDataCacheConnector
    val nonUKBusinessType = "Non UK-based Company"
  }
  
  override def beforeEach(): Unit = {
    reset(mockDataCacheConnector)
    reset(mockBusinessCustomerConnector)
    reset(mockWSHttp)
  }

  "BusinessRegistrationService" must {

    "use the correct data cache connector" in {
      BusinessRegistrationService.dataCacheConnector must be(DataCacheConnector)
    }

    "use the correct business Customer Connector" in {
      BusinessRegistrationService.businessCustomerConnector must be(BusinessCustomerConnector)
    }
  }

  "getDetails" must {

    val failureResponse = Json.parse( """{"reason":"Agent not found!"}""")

    val identifier = "JARN1234567"
    val identifierType = "arn"
    implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

    val reviewDetails = ReviewDetails(businessName = "ABC",
      businessType = Some("corporate body"),
      businessAddress = Address(line_1 = "line1", line_2 = "line2", line_3 = None, line_4 = None, postcode = None, country = "GB"),
      sapNumber = "1234567890", safeId = "XW0001234567890",false, agentReferenceNumber = Some("JARN1234567"))

    "for OK response status, return body as Some(BusinessRegistration) from json with a nonUKIdentification" in {
      val successResponseInd = Json.parse(
        """
          |{
          |  "sapNumber":"1234567890", "safeId": "EX0012345678909",
          |  "agentReferenceNumber": "AARN1234567",
          |  "nonUKIdentification": {
          |     "idNumber": "123456",
          |     "issuingInstitution": "France Institution",
          |     "issuingCountryCode": "FR"
          |  },
          |  "isAnIndividual": false,
          |  "isAnAgent": false,
          |  "isEditable": true,
          |  "organisation": {
          |    "organisationName": "MyBusinessName",
          |    "isAGroup": false,
          |    "organisationType": "LLP"
          |  },
          |  "addressDetails": {
          |    "addressLine1": "Melbourne House",
          |    "addressLine2": "Eastgate",
          |    "addressLine3": "Accrington",
          |    "addressLine4": "Lancashire",
          |    "postalCode": "BB5 6PU",
          |    "countryCode": "GB"
          |  },
          |  "contactDetails" : {}
          |}
        """.stripMargin
      )

      when(mockDataCacheConnector.fetchAndGetBusinessDetailsForSession(Matchers.any())).thenReturn(Future.successful(Some(reviewDetails)))
      when(mockBusinessCustomerConnector.getDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponseInd))))
      val result = TestBusinessRegistrationService.getDetails()
      val busReg = await(result)
      busReg.isDefined must be (true)

      busReg.get.businessName must be ("MyBusinessName")
      busReg.get.businessAddress.line_1 must be ("Melbourne House")
      busReg.get.businessAddress.line_2 must be ("Eastgate")
      busReg.get.businessAddress.line_3 must be (Some("Accrington"))
      busReg.get.businessAddress.line_4 must be (Some("Lancashire"))
      busReg.get.businessAddress.postcode must be (Some("BB5 6PU"))
      busReg.get.businessAddress.country must be ("GB")

      busReg.get.businessUniqueId must be (Some("123456"))
      busReg.get.hasBusinessUniqueId must be (Some(true))
      busReg.get.issuingCountry must be (Some("FR"))
      busReg.get.issuingInstitution must be (Some("France Institution"))
    }

    "for OK response status, return body as Some(BusinessRegistration) from json with NO nonUKIdentification" in {
      val successResponseInd = Json.parse(
        """
          |{
          |  "sapNumber":"1234567890", "safeId": "EX0012345678909",
          |  "agentReferenceNumber": "AARN1234567",
          |  "isAnIndividual": false,
          |  "isAnAgent": false,
          |  "isEditable": true,
          |  "organisation": {
          |    "organisationName": "MyBusinessName",
          |    "isAGroup": false,
          |    "organisationType": "LLP"
          |  },
          |  "addressDetails": {
          |    "addressLine1": "Melbourne House",
          |    "addressLine2": "Eastgate",
          |    "addressLine3": "Accrington",
          |    "addressLine4": "Lancashire",
          |    "postalCode": "BB5 6PU",
          |    "countryCode": "GB"
          |  },
          |  "contactDetails" : {}
          |}
        """.stripMargin
      )

      when(mockDataCacheConnector.fetchAndGetBusinessDetailsForSession(Matchers.any())).thenReturn(Future.successful(Some(reviewDetails)))
      when(mockBusinessCustomerConnector.getDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponseInd))))
      val result = TestBusinessRegistrationService.getDetails()
      val busReg = await(result)
      busReg.isDefined must be (true)

      busReg.get.businessName must be ("MyBusinessName")
      busReg.get.businessAddress.line_1 must be ("Melbourne House")
      busReg.get.businessAddress.line_2 must be ("Eastgate")
      busReg.get.businessAddress.line_3 must be (Some("Accrington"))
      busReg.get.businessAddress.line_4 must be (Some("Lancashire"))
      busReg.get.businessAddress.postcode must be (Some("BB5 6PU"))
      busReg.get.businessAddress.country must be ("GB")

      busReg.get.businessUniqueId must be (None)
      busReg.get.hasBusinessUniqueId must be (None)
      busReg.get.issuingCountry must be (None)
      busReg.get.issuingInstitution must be (None)
    }

    "for NOT_FOUND response status, return body as None" in {
      when(mockDataCacheConnector.fetchAndGetBusinessDetailsForSession(Matchers.any())).thenReturn(Future.successful(Some(reviewDetails)))
      when(mockBusinessCustomerConnector.getDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(NOT_FOUND, Some(failureResponse))))
      val result = TestBusinessRegistrationService.getDetails()
      await(result) must be(None)
    }
    "for BAD_REQUEST response status, throw bad request exception" in {
      when(mockDataCacheConnector.fetchAndGetBusinessDetailsForSession(Matchers.any())).thenReturn(Future.successful(Some(reviewDetails)))
      when(mockBusinessCustomerConnector.getDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST)))
      val result = TestBusinessRegistrationService.getDetails()
      val thrown = the[BadRequestException] thrownBy await(result)
      thrown.message must include("Bad Data")
    }
    "getAgentDetails throws InternalServerException exception for call to ETMP, when BadRequest response is received" in {
      when(mockDataCacheConnector.fetchAndGetBusinessDetailsForSession(Matchers.any())).thenReturn(Future.successful(Some(reviewDetails)))
      when(mockBusinessCustomerConnector.getDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR)))
      val result = TestBusinessRegistrationService.getDetails()
      val thrown = the[InternalServerException] thrownBy await(result)
      thrown.message must include("Internal server error")
    }
  }


  "registerBusiness" must {
    val nonUKResponse = BusinessRegistrationResponse(processingDate = "2015-01-01",
              sapNumber = "SAP123123",
              safeId = "SAFE123123",
              agentReferenceNumber = Some("AREF123123"))

    "save the response from the registration" in {
      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

      when(mockBusinessCustomerConnector.register(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future(nonUKResponse))

      val busRegData = BusinessRegistration(businessName = "testName",
        businessAddress = Address("line1", "line2", Some("line3"), Some("line4"), Some("postCode"), "country"),
        hasBusinessUniqueId = Some(true),
        businessUniqueId = Some(s"BUID-${UUID.randomUUID}"),
        issuingInstitution = Some("issuingInstitution"),
        issuingCountry = Some("GB")
      )

      val returnedReviewDetails = new ReviewDetails(businessName = busRegData.businessName, businessType = None, businessAddress = busRegData.businessAddress,
        sapNumber = "sap123", safeId = "safe123", isAGroup = false, agentReferenceNumber = Some("agent123"))
      when(mockDataCacheConnector.saveReviewDetails(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(returnedReviewDetails)))


      val regResult = TestBusinessRegistrationService.registerBusiness(busRegData, isGroup = true, isNonUKClientRegisteredByAgent = false, service)

      val reviewDetails = await(regResult)

      reviewDetails.businessName must be(busRegData.businessName)
      reviewDetails.businessAddress.line_1 must be(busRegData.businessAddress.line_1)
    }

    "save the response from the registration, when an agent registers non-uk based client" in {
      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

      when(mockBusinessCustomerConnector.register(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future(nonUKResponse))
      val busRegData = BusinessRegistration(businessName = "testName",
        businessAddress = Address("line1", "line2", Some("line3"), Some("line4"), Some("postCode"), "country"),
        hasBusinessUniqueId = Some(true),
        businessUniqueId = Some(s"BUID-${UUID.randomUUID}"),
        issuingInstitution = Some("issuingInstitution"),
        issuingCountry = Some("GB")
      )

      val returnedReviewDetails = new ReviewDetails(businessName = busRegData.businessName, businessType = None, businessAddress = busRegData.businessAddress,
        sapNumber = "sap123", safeId = "safe123", isAGroup = false, agentReferenceNumber = Some("agent123"))
      when(mockDataCacheConnector.saveReviewDetails(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(returnedReviewDetails)))


      val regResult = TestBusinessRegistrationService.registerBusiness(busRegData, isGroup = true, isNonUKClientRegisteredByAgent = true, service)

      val reviewDetails = await(regResult)

      reviewDetails.businessName must be(busRegData.businessName)
      reviewDetails.businessAddress.line_1 must be(busRegData.businessAddress.line_1)
    }

    "save the response from the registration when we have no businessUniqueId or issuingInstitution" in {
      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockBusinessCustomerConnector.register(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future(nonUKResponse))
      val busRegData = BusinessRegistration(businessName = "testName",
        businessAddress = Address("line1", "line2", Some("line3"), Some("line4"), Some("postCode"), "country"),
        hasBusinessUniqueId = Some(false),
        businessUniqueId = None,
        issuingInstitution = None,
        issuingCountry = None
      )

      val returnedReviewDetails = new ReviewDetails(businessName = busRegData.businessName, businessType = None, businessAddress = busRegData.businessAddress,
        sapNumber = "sap123", safeId = "safe123", isAGroup = false, agentReferenceNumber = Some("agent123"))
      when(mockDataCacheConnector.saveReviewDetails(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(returnedReviewDetails)))

      val regResult = TestBusinessRegistrationService.registerBusiness(busRegData, isGroup = true, isNonUKClientRegisteredByAgent = false, service)
      val reviewDetails = await(regResult)

      reviewDetails.businessName must be(busRegData.businessName)
      reviewDetails.businessAddress.line_1 must be(busRegData.businessAddress.line_1)
    }

    "throw exception when registration fails" in {
      implicit val hc = new HeaderCarrier(sessionId = None)
      when(mockBusinessCustomerConnector.register(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future(nonUKResponse))
      val busRegData = BusinessRegistration(businessName = "testName",
        businessAddress = Address("line1", "line2", Some("line3"), Some("line4"), Some("postCode"), "country"),
        businessUniqueId = Some(s"BUID-${UUID.randomUUID}"),
        hasBusinessUniqueId = Some(true),
        issuingInstitution = Some("issuingInstitution"),
        issuingCountry = None
      )

      val returnedReviewDetails = new ReviewDetails(businessName = busRegData.businessName, businessType = None, businessAddress = busRegData.businessAddress,
        sapNumber = "sap123", safeId = "safe123", isAGroup = false, agentReferenceNumber = Some("agent123"))
      when(mockDataCacheConnector.saveReviewDetails(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))


      val regResult = TestBusinessRegistrationService.registerBusiness(busRegData, isGroup = true, isNonUKClientRegisteredByAgent = false, service)

      val thrown = the[InternalServerException] thrownBy await(regResult)
      thrown.getMessage must include("Registration Failed")
    }
  }
}
