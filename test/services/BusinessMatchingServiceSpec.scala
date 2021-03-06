/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package services

import java.util.UUID

import connectors.{BusinessMatchingConnector, DataCacheConnector}
import models._
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.domain.{CtUtr, Org, SaUtr}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{CredentialStrength, _}

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.SessionId

class BusinessMatchingServiceSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val testAddress = Address("address line 1", "address line 2", Some("address line 3"), Some("address line 4"), Some("AA1 1AA"), "UK")
  val testReviewDetails = ReviewDetails(businessName = "ACME",
    businessType = Some("Limited"),
    businessAddress = testAddress,
    sapNumber = "1234567890",
    safeId = "EX0012345678909",
    isAGroup = false,
    directMatch = false,
    agentReferenceNumber = Some("01234567890"),
    utr = Some(utr))
  val matchFailureResponse = MatchFailureResponse(reason = "Sorry. Business details not found. Try with correct UTR and/or name.")
  val matchFailureResponseJson = Json.toJson(matchFailureResponse)
  val successOrgJson = Json.parse(
    """
      |{
      |  "sapNumber": "1234567890",
      |  "safeId": "EX0012345678909",
      |  "agentReferenceNumber": "01234567890",
      |  "isEditable": true,
      |  "isAnAgent": false,
      |  "isAnIndividual": false,
      |  "organisation": {
      |    "organisationName": "Real Business Inc",
      |    "isAGroup": true,
      |    "organisationType": "unincorporated body"
      |  },
      |  "address": {
      |    "addressLine1": "address line 1",
      |    "addressLine2": "address line 2",
      |    "addressLine3": "address line 3",
      |    "addressLine4": "address line 4",
      |    "postalCode": "AA1 1AA",
      |    "countryCode": "UK"
      |  },
      |  "contactDetails": {
      |    "phoneNumber": "1234567890"
      |  }
      | }
    """.stripMargin)

  val utr = "1234567890"

  val successOrgReviewDetailsDirect = ReviewDetails("Real Business Inc", Some("unincorporated body"), testAddress, "1234567890", "EX0012345678909",
    isAGroup = true, directMatch = true, Some("01234567890"), utr = Some(utr))

  val successOrgReviewDetails = ReviewDetails("Real Business Inc", Some("unincorporated body"), testAddress, "1234567890", "EX0012345678909",
    isAGroup = true, directMatch = false, Some("01234567890"), utr = Some(utr))

  val successOrgReviewDetailsJsonDirect = Json.toJson(successOrgReviewDetailsDirect)

  val successOrgReviewDetailsJson = Json.toJson(successOrgReviewDetails)

  val successIndividualJson = Json.parse(
    """
      |{
      |  "sapNumber": "1234567890",
      |  "safeId": "EX0012345678909",
      |  "agentReferenceNumber": "01234567890",
      |  "isEditable": true,
      |  "isAnAgent": false,
      |  "isAnIndividual": true,
      |  "individual": {
      |    "firstName": "first name",
      |    "lastName": "last name"
      |  },
      |  "address": {
      |    "addressLine1": "address line 1",
      |    "addressLine2": "address line 2",
      |    "addressLine3": "address line 3",
      |    "addressLine4": "address line 4",
      |    "postalCode": "AA1 1AA",
      |    "countryCode": "UK"
      |  },
      |  "contactDetails": {
      |    "phoneNumber": "1234567890"
      |  }
      |}
    """.stripMargin)

  val successIndReviewDetailsDirect = ReviewDetails("first name last name", Some("Sole Trader"), testAddress, "1234567890", "EX0012345678909",
    isAGroup = false, directMatch = true, Some("01234567890"), Some("first name"), Some("last name"), Some(utr))

  val successIndReviewDetails = ReviewDetails("first name last name", Some("Sole Trader"), testAddress, "1234567890", "EX0012345678909",
    isAGroup = false, directMatch = false, Some("01234567890"), Some("first name"), Some("last name"), Some(utr))

  val successIndReviewDetailsJsonDirect = Json.toJson(successIndReviewDetailsDirect)

  val successIndReviewDetailsJson = Json.toJson(successIndReviewDetails)

  val errorJson = Json.parse( """{"error" : "Some Error"}""")

  val noMatchUtr = "9999999999"

  val testIndividual = Individual("firstName", "lastName", None)

  val testOrganisation = Organisation("org name", "org type")

  implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

  val service = "ated"


  val mockBusinessMatchingConnector = mock[BusinessMatchingConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]

  object TestBusinessMatchingService extends BusinessMatchingService {
    override val businessMatchingConnector: BusinessMatchingConnector = mockBusinessMatchingConnector
    override val dataCacheConnector: DataCacheConnector = mockDataCacheConnector
  }

  override def beforeEach = {
    reset(mockBusinessMatchingConnector)
    reset(mockDataCacheConnector)
  }

  "BusinessMatchingService" must {

    "matchBusinessWithUTR" must {

      "for match found with SA user, return Review details as JsValue" in {

        implicit val saUser = AuthContext(Authority(uri = "testuser", accounts = Accounts(sa = Some(SaAccount(s"/sa/individual/$utr", SaUtr(utr)))),
          None, None, CredentialStrength.Weak, ConfidenceLevel.L50, Some(""), Some(""), Some(""), ""))
        implicit val bcc = BusinessCustomerContext(FakeRequest(), BusinessCustomerUser(saUser))
        when(mockBusinessMatchingConnector.lookup(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(successIndividualJson))
        when(mockDataCacheConnector.saveReviewDetails(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(testReviewDetails)))
        val result = TestBusinessMatchingService.matchBusinessWithUTR(false, service)
        await(result.get) must be(successIndReviewDetailsJsonDirect)
        verify(mockBusinessMatchingConnector, times(1)).lookup(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        verify(mockDataCacheConnector, times(1)).saveReviewDetails(Matchers.any())(Matchers.any())
      }

      "for match Not found with SA user, return Reasons as JsValue" in {
        implicit val saUser = AuthContext(Authority(uri = "testuser", accounts = Accounts(sa = Some(SaAccount(s"/sa/individual/$utr", SaUtr(utr)))),
          None, None, CredentialStrength.Weak, ConfidenceLevel.L50, Some(""), Some(""), Some(""), ""))
        implicit val bcc = BusinessCustomerContext(FakeRequest(), BusinessCustomerUser(saUser))
        when(mockBusinessMatchingConnector.lookup(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(matchFailureResponseJson))
        val result = TestBusinessMatchingService.matchBusinessWithUTR(false, service)
        await(result.get) must be(matchFailureResponseJson)
        verify(mockBusinessMatchingConnector, times(1)).lookup(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        verify(mockDataCacheConnector, times(0)).saveReviewDetails(Matchers.any())(Matchers.any())
      }

      "for match found with CT user, return Review details as JsValue" in {
        implicit val saUser = AuthContext(Authority(uri = "testuser", accounts = Accounts(ct = Some(CtAccount(s"/ct/organisation/$utr", CtUtr(utr)))),
          None, None, CredentialStrength.Weak, ConfidenceLevel.L50, Some(""), Some(""), Some(""), ""))
        implicit val bcc = BusinessCustomerContext(FakeRequest(), BusinessCustomerUser(saUser))
        when(mockBusinessMatchingConnector.lookup(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(successOrgJson))
        when(mockDataCacheConnector.saveReviewDetails(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(testReviewDetails)))
        val result = TestBusinessMatchingService.matchBusinessWithUTR(false, service)
        await(result.get) must be(successOrgReviewDetailsJsonDirect)
        verify(mockBusinessMatchingConnector, times(1)).lookup(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        verify(mockDataCacheConnector, times(1)).saveReviewDetails(Matchers.any())(Matchers.any())
      }

      "for match Not found with CT user, return Reasons as JsValue" in {
        implicit val saUser = AuthContext(Authority(uri = "testuser", accounts = Accounts(ct = Some(CtAccount(s"/ct/organisation/$utr", CtUtr(utr)))),
          None, None, CredentialStrength.Weak, ConfidenceLevel.L50, Some(""), Some(""), Some(""), ""))
        implicit val bcc = BusinessCustomerContext(FakeRequest(), BusinessCustomerUser(saUser))
        when(mockBusinessMatchingConnector.lookup(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(matchFailureResponseJson))
        val result = TestBusinessMatchingService.matchBusinessWithUTR(false, service)
        await(result.get) must be(matchFailureResponseJson)
        verify(mockBusinessMatchingConnector, times(1)).lookup(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        verify(mockDataCacheConnector, times(0)).saveReviewDetails(Matchers.any())(Matchers.any())
      }

      "for ORG user, return None as JsValue" in {
        implicit val saUser = AuthContext(Authority(uri = "testuser", accounts = Accounts(org = Some(OrgAccount("", Org("1234")))),
          None, None, CredentialStrength.Weak, ConfidenceLevel.L50, Some(""), Some(""), Some(""), ""))
        implicit val bcc = BusinessCustomerContext(FakeRequest(), BusinessCustomerUser(saUser))
        val result = TestBusinessMatchingService.matchBusinessWithUTR(false, service)
        result must be(None)
        verify(mockBusinessMatchingConnector, times(0)).lookup(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        verify(mockDataCacheConnector, times(0)).saveReviewDetails(Matchers.any())(Matchers.any())
      }

      "for user with Both SA & CT, return None as JsValue" in {
        implicit val saUser = AuthContext(Authority(uri = "testuser", accounts = Accounts(sa = Some(SaAccount("sa/1234", SaUtr("1111111111"))),
          ct = Some(CtAccount("ct/1234", CtUtr("1111111111")))), None, None, CredentialStrength.Weak, ConfidenceLevel.L50, Some(""), Some(""), Some(""), ""))
        implicit val bcc = BusinessCustomerContext(FakeRequest(), BusinessCustomerUser(saUser))
        val result = TestBusinessMatchingService.matchBusinessWithUTR(false, service)
        result must be(None)
        verify(mockBusinessMatchingConnector, times(0)).lookup(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        verify(mockDataCacheConnector, times(0)).saveReviewDetails(Matchers.any())(Matchers.any())
      }

    }

    "matchBusinessWithIndividualName" must {

      "for match found with SA user, return Review details as JsValue" in {
        implicit val saUser = AuthContext(Authority(uri = "testuser", accounts = Accounts(sa = Some(SaAccount(s"/sa/individual/$utr", SaUtr(utr)))),
          None, None, CredentialStrength.Weak, ConfidenceLevel.L50, Some(""), Some(""), Some(""), ""))
        implicit val bcc = BusinessCustomerContext(FakeRequest(), BusinessCustomerUser(saUser))
        when(mockBusinessMatchingConnector.lookup(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(successIndividualJson))
        when(mockDataCacheConnector.saveReviewDetails(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(testReviewDetails)))
        val result = TestBusinessMatchingService.matchBusinessWithIndividualName(false, testIndividual, utr, service)
        await(result) must be(successIndReviewDetailsJson)
        verify(mockBusinessMatchingConnector, times(1)).lookup(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        verify(mockDataCacheConnector, times(1)).saveReviewDetails(Matchers.any())(Matchers.any())
      }

      "for match Not found with SA user, return Reasons as JsValue" in {
        implicit val saUser = AuthContext(Authority(uri = "testuser", accounts = Accounts(sa = Some(SaAccount(s"/sa/individual/$utr", SaUtr(utr)))),
          None, None, CredentialStrength.Weak, ConfidenceLevel.L50, Some(""), Some(""), Some(""), ""))
        implicit val bcc = BusinessCustomerContext(FakeRequest(), BusinessCustomerUser(saUser))
        when(mockBusinessMatchingConnector.lookup(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(matchFailureResponseJson))
        val result = TestBusinessMatchingService.matchBusinessWithIndividualName(false, testIndividual, utr, service)
        await(result) must be(matchFailureResponseJson)
        verify(mockBusinessMatchingConnector, times(1)).lookup(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        verify(mockDataCacheConnector, times(0)).saveReviewDetails(Matchers.any())(Matchers.any())
      }

    }

    "matchBusinessWithOrganisationName" must {

      "for match found with SA user, return Review details as JsValue" in {
        implicit val saUser = AuthContext(Authority(uri = "testuser", accounts = Accounts(sa = Some(SaAccount(s"/sa/individual/$utr", SaUtr(utr)))),
          None, None, CredentialStrength.Weak, ConfidenceLevel.L50, Some(""), Some(""), Some(""), ""))
        implicit val bcc = BusinessCustomerContext(FakeRequest(), BusinessCustomerUser(saUser))
        when(mockBusinessMatchingConnector.lookup(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(successIndividualJson))
        when(mockDataCacheConnector.saveReviewDetails(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(testReviewDetails)))
        val result = TestBusinessMatchingService.matchBusinessWithOrganisationName(false, testOrganisation, utr, service)
        await(result) must be(successIndReviewDetailsJson)
        verify(mockBusinessMatchingConnector, times(1)).lookup(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        verify(mockDataCacheConnector, times(1)).saveReviewDetails(Matchers.any())(Matchers.any())
      }

      "for match Not found with SA user, return Reasons as JsValue" in {
        implicit val saUser = AuthContext(Authority(uri = "testuser", accounts = Accounts(sa = Some(SaAccount(s"/sa/individual/$utr", SaUtr(utr)))),
          None, None, CredentialStrength.Weak, ConfidenceLevel.L50, Some(""), Some(""), Some(""), ""))
        implicit val bcc = BusinessCustomerContext(FakeRequest(), BusinessCustomerUser(saUser))
        when(mockBusinessMatchingConnector.lookup(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(matchFailureResponseJson))
        val result = TestBusinessMatchingService.matchBusinessWithOrganisationName(false, testOrganisation, utr, service)
        await(result) must be(matchFailureResponseJson)
        verify(mockBusinessMatchingConnector, times(1)).lookup(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        verify(mockDataCacheConnector, times(0)).saveReviewDetails(Matchers.any())(Matchers.any())
      }

      "for match found with SA user, throw an exception when no Safe Id Number" in {
        val successNoSapNo = Json.parse(
          """
            |{
            |  "agentReferenceNumber": "01234567890",
            |  "isEditable": true,
            |  "isAnAgent": false,
            |  "isAnIndividual": true,
            |  "individual": {
            |    "firstName": "first name",
            |    "lastName": "last name"
            |  },
            |  "address": {
            |    "addressLine1": "address line 1",
            |    "addressLine2": "address line 2",
            |    "addressLine3": "address line 3",
            |    "addressLine4": "address line 4",
            |    "postalCode": "AA1 1AA",
            |    "countryCode": "UK"
            |  },
            |  "contactDetails": {
            |    "phoneNumber": "1234567890"
            |  }
            |}
          """.stripMargin)

        implicit val saUser = AuthContext(Authority(uri = "testuser", accounts = Accounts(sa = Some(SaAccount(s"/sa/individual/$utr", SaUtr(utr)))),
          None, None, CredentialStrength.Weak, ConfidenceLevel.L50, Some(""), Some(""), Some(""), ""))
        implicit val bcc = BusinessCustomerContext(FakeRequest(), BusinessCustomerUser(saUser))
        when(mockBusinessMatchingConnector.lookup(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(successNoSapNo))
        when(mockDataCacheConnector.saveReviewDetails(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(testReviewDetails)))
        val result = TestBusinessMatchingService.matchBusinessWithOrganisationName(false, testOrganisation, utr, service)
        val thrown = the[RuntimeException] thrownBy await(result)
        thrown.getMessage must include("No Safe Id returned from ETMP")

      }

      "for match found with SA user, throw an exception when no Address" in {
        val successNoSapNo = Json.parse(
          """
            |{
            |  "sapNumber": "1234567890",
            |  "safeId": "EX0012345678909",
            |  "agentReferenceNumber": "01234567890",
            |  "isEditable": true,
            |  "isAnAgent": false,
            |  "isAnIndividual": true,
            |  "individual": {
            |    "firstName": "first name",
            |    "lastName": "last name"
            |  },
            |  "contactDetails": {
            |    "phoneNumber": "1234567890"
            |  }
            |}
          """.stripMargin)

        implicit val saUser = AuthContext(Authority(uri = "testuser", accounts = Accounts(sa = Some(SaAccount(s"/sa/individual/$utr", SaUtr(utr)))),
          None, None, CredentialStrength.Weak, ConfidenceLevel.L50, Some(""), Some(""), Some(""), ""))
        implicit val bcc = BusinessCustomerContext(FakeRequest(), BusinessCustomerUser(saUser))
        when(mockBusinessMatchingConnector.lookup(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(successNoSapNo))
        when(mockDataCacheConnector.saveReviewDetails(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(testReviewDetails)))
        val result = TestBusinessMatchingService.matchBusinessWithOrganisationName(false, testOrganisation, utr, service)
        val thrown = the[RuntimeException] thrownBy await(result)
        thrown.getMessage must include("No Address returned from ETMP")

      }

    }

  }

}
