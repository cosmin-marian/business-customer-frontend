/*
 * Copyright 2016 HM Revenue & Customs
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

package connectors

import java.time.{Clock, Instant}

import models._
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.specs2.mock.mockito.MockitoMatchers
import play.api.libs.json.Json

class RegisterWithIdServiceAdaptorSpec extends PlaySpec with MockitoSugar with MockitoMatchers with BeforeAndAfter {
  val mockUUIDGenerator = mock[RandomUUIDGenerator]
  val randomGeneratedUUID = java.util.UUID.randomUUID().toString

  val mockClock = mock[Clock]
  val currentDateTimeUTC = DateTime.now(DateTimeZone.UTC)

  val serviceUnderTest = new RegisterWithIdServiceAdaptor {
    override val uuidGenerator = mockUUIDGenerator
    override val clock = mockClock
  }

  val in: Instant = Instant.now(Clock.systemUTC())

  before {
    when(mockUUIDGenerator.generateUUIDAsString).thenReturn(randomGeneratedUUID)
    when(mockClock.instant()).thenReturn(in)
  }

  val ExpectedMDGHeader = MDGHeader("MDTP", in.toString, randomGeneratedUUID)
  val ExpectedMessageTypes = MessageTypes("RegisterWithID")

  "request adaptor " must {

    "create correct RegisterWithId request when match is by UTR only" in {
      val matchWithUTROnly = MatchBusinessData("", "123456789", requiresNameMatch = false, isAnAgent = false, None, None)

      serviceUnderTest.createRequestFrom(matchWithUTROnly) must be(
        Json.toJson(RegisterWithIdRequest(ExpectedMDGHeader,
          ExpectedMessageTypes,
          RegistrationDetails(IDNumber = "123456789", requiresNameMatch = false, isAnAgent = false)))
      )
    }

    "create correct RegisterWithId request when match is for an Agent" in {
      val matchWithUTROnlyAndIsAgent = MatchBusinessData("", "123456789", requiresNameMatch = false, isAnAgent = true, None, None)

      serviceUnderTest.createRequestFrom(matchWithUTROnlyAndIsAgent) must be(
        Json.toJson(RegisterWithIdRequest(ExpectedMDGHeader,
          ExpectedMessageTypes,
          RegistrationDetails(IDNumber = "123456789", requiresNameMatch = false, isAnAgent = true)))
      )
    }

    "create correct RegisterWithId request when match is by UTR and Name" in {
      val matchWithUTRAndName = MatchBusinessData("", "123456789", true, false, None, Some(Organisation("businessName", "type")))

      serviceUnderTest.createRequestFrom(matchWithUTRAndName) must be(
        Json.toJson(RegisterWithIdRequest(ExpectedMDGHeader,
          ExpectedMessageTypes,
          RegistrationDetails(IDNumber = "123456789", requiresNameMatch = true, isAnAgent = false, organisation = Some(Organisation("businessName", "type")))))
      )
    }
  }

  "response adaptor " must {

    "create matchSuccess response when RegisterWithId Response contains mandatory values" in {
      val responseFromRegisterWithIdService = Json.parse(
        """
          |{
          |"MDGHeader": {
          |   "returnParameters": []
          |},
          | "SAFEID":"XE0000123456789",
          | "isEditable":true,
          | "isAnAgent":false,
          | "isAnIndividual":true,
          | "address":{
          |  "addressType":"0001",
          |  "addressLine1":"21 Emmbrook Lane",
          |  "addressLine2":"Wokingham",
          |  "countryCode":"GB"
          | },
          | "contactDetails":{
          | }
          |
          |}
        """.stripMargin)

      val dataReturned = Json.toJson(serviceUnderTest.convertToMatchSuccess(responseFromRegisterWithIdService))

      val addressReturned = (dataReturned \ "address").as[Option[EtmpAddress]].get

      addressReturned.addressLine1 must be("21 Emmbrook Lane")
      addressReturned.addressLine2 must be("Wokingham")
      addressReturned.addressLine3 must be(None)
      addressReturned.addressLine4 must be(None)
      addressReturned.postalCode must be(None)
      addressReturned.countryCode must be("GB")

      (dataReturned \ "safeId").as[String] must be("XE0000123456789")
      (dataReturned \ "isAnIndividual").as[Boolean] mustBe true
    }

    "create matchSuccess response when RegisterWithId Response contains optional values as well" in {
      val responseFromRegisterWithIdService = Json.parse(
        """
          {
          | "MDGHeader": {
          |   "status": "OK",
          |   "processingDate":"2001-12-17T09:30:47.123456Z",
          |   "returnParameters": [{
          |      "paramName": "SAP_NUMBER",
          |      "paramValue": "0123456789"
          |    }]
          | },
          | "SAFEID":"XE0000123456789",
          | "ARN" : "agentReferenceNumber123",
          | "isEditable":true,
          | "isAnAgent":false,
          | "isAnIndividual":false,
          | "organisation":{
          |   "organisationName":"orgName",
          |   "isAGroup":true,
          |   "organisationType":"LLP",
          |   "code":"0001"
          | },
          | "address":{
          |   "addressType":"0001",
          |   "addressLine1":"21 Emmbrook Lane",
          |   "addressLine2":"Wokingham",
          |   "addressLine3":"adLine3",
          |   "addressLine4":"adLine4",
          |   "postalCode":"RG41 4RR",
          |   "countryCode":"GB"
          | },
          | "contactDetails":{
          |   "phoneNumber":"011 5666 4444",
          |   "mobileNo":"011 5666 4444",
          |   "faxNo":"011 5666 4444",
          |   "emailAddress":"fred.flintstone@org.co.uk"
          |  }
          |}
        """.stripMargin)

      val dataReturned = Json.toJson(serviceUnderTest.convertToMatchSuccess(responseFromRegisterWithIdService))

      val organisation = (dataReturned \ "organisation").as[OrganisationResponse]
      organisation.organisationName must be("orgName")
      organisation.isAGroup must be(Some(true))
      organisation.organisationType must be(Some("LLP"))

      val addressReturned = (dataReturned \ "address").as[Option[EtmpAddress]].get

      addressReturned.addressLine1 must be("21 Emmbrook Lane")
      addressReturned.addressLine2 must be("Wokingham")
      addressReturned.addressLine3 must be(Some("adLine3"))
      addressReturned.addressLine4 must be(Some("adLine4"))
      addressReturned.postalCode must be(Some("RG41 4RR"))
      addressReturned.countryCode must be("GB")

      (dataReturned \ "agentReferenceNumber").as[String] must be("agentReferenceNumber123")
      (dataReturned \ "safeId").as[String] must be("XE0000123456789")
      (dataReturned \ "sapNumber").as[String] must be("0123456789")
      (dataReturned \ "isAnIndividual").as[Boolean] must be(false)

    }

    "create matchFailure response when RegisterWithId Failure response is received" in {
      val responseFromRegisterWithIdService = Json.parse(
        """
          |{
          | "MDGHeader": {
          |   "status": "NOT_OK",
          |   "processingDate":"2001-12-17T09:30:47.123456Z",
          |   "returnParameters": [
          |     {
          |       "paramName": "ERRORCODE",
          |       "paramValue": "002"
          |     },
          |     {
          |       "paramName": "ERRORTEXT",
          |       "paramValue": "No match found"
          |     }
          |   ]
          | }
          |}""".stripMargin)

      val matchFailureResponse = serviceUnderTest.convertToMatchFailure(responseFromRegisterWithIdService)

      matchFailureResponse must be(Json.parse(
        """
          |{ "Reason": "No match found" }
        """.stripMargin))
    }

    "create matchFailure response when RegisterWithId Failure response is received with no error details" in {
      val responseFromRegisterWithIdService = Json.parse(
        """
          |{
          | "MDGHeader": {
          |   "status": "NOT_OK",
          |   "processingDate":"2001-12-17T09:30:47.123456Z",
          |   "returnParameters": []
          | }
          |}""".stripMargin)

      val matchFailureResponse = serviceUnderTest.convertToMatchFailure(responseFromRegisterWithIdService)

      matchFailureResponse must be(Json.parse(
        """
          |{ "Reason": "" }
        """.stripMargin))
    }
  }

}
