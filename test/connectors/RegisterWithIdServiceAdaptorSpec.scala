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
import play.api.libs.json.{JsValue, Json}

class RegisterWithIdServiceAdaptorSpec extends PlaySpec with MockitoSugar with MockitoMatchers with BeforeAndAfter {
  val mockUUIDGenerator = mock[RandomUUIDGenerator]
  val randomGeneratedUUID = java.util.UUID.randomUUID().toString

  val mockClock = mock[Clock]
  val currentDateTimeUTC = DateTime.now(DateTimeZone.UTC)

  val serviceUnderTest = new RegisterWithIdServiceAdaptor {
    override val uuidGenerator = mockUUIDGenerator
    override val clock = mockClock
  }

  val currentInstant: Instant = Instant.now(Clock.systemUTC())

  before {
    when(mockUUIDGenerator.generateUUIDAsString).thenReturn(randomGeneratedUUID)
    when(mockClock.instant()).thenReturn(currentInstant)
  }

  val ExpectedMDGHeader = MDGHeader("MDTP", currentInstant.toString, randomGeneratedUUID)
  val ExpectedMessageTypes = MessageTypes("RegisterWithID")

  def createExpectedReqJson(isAnAgent: Boolean) = {
    Json.parse(
      s"""
         |{
         |   "MDGHeader":{
         |      "originatingSystem":"MDTP",
         |      "requestTimeStamp": "$currentInstant",
         |      "correlationId":"$randomGeneratedUUID"
         |   },
         |   "messageTypes":{
         |      "messageType":"RegisterWithID"
         |   },
         |   "requestParameters":[
         |      {
         |         "paramName":"REGIME",
         |         "paramValue":"CDS"
         |      }
         |   ],
         |   "registrationDetails":{
         |      "idType":"UTR",
         |      "idNumber":"123456789",
         |      "requiresNameMatch":false,
         |      "isAnAgent":$isAnAgent
         |   }
         |}
        """.stripMargin)
  }

  "request adaptor " must {

    "create correct RegisterWithId request when match is by UTR only" in {
      val matchWithUTROnly = MatchBusinessData("", "123456789", requiresNameMatch = false, isAnAgent = false, None, None)
      serviceUnderTest.createRequestFrom(matchWithUTROnly) must be(createExpectedReqJson(false))
    }

    "create correct RegisterWithId request when match is for an Agent" in {
      val matchWithUTROnlyAndIsAgent = MatchBusinessData("", "123456789", requiresNameMatch = false, isAnAgent = true, None, None)
      val requestJs: JsValue = serviceUnderTest.createRequestFrom(matchWithUTROnlyAndIsAgent)
      requestJs must be(createExpectedReqJson(true))
      // time in UTC format 2016-07-08T08:35:13.147Z
      (requestJs \ "MDGHeader" \ "requestTimeStamp").as[String] must fullyMatch.regex("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{3}Z")
    }

    "create correct RegisterWithId request when match is by UTR and Name" in {
      val matchWithUTRAndName = MatchBusinessData("", "123456789", true, false, None, Some(Organisation("businessName", "type")))

      val expectedJson = Json.parse(
        s"""
           |{
           |   "MDGHeader":{
           |      "originatingSystem":"MDTP",
           |      "requestTimeStamp": "$currentInstant",
           |      "correlationId":"$randomGeneratedUUID"
           |   },
           |   "messageTypes":{
           |      "messageType":"RegisterWithID"
           |   },
           |   "requestParameters":[
           |      {
           |         "paramName":"REGIME",
           |         "paramValue":"CDS"
           |      }
           |   ],
           |   "registrationDetails":{
           |      "idType":"UTR",
           |      "idNumber":"123456789",
           |      "requiresNameMatch":true,
           |      "isAnAgent":false,
           |      "organisation":{
           |        "organisationName":"businessName",
           |        "organisationType":"type"
           |       }
           |   }
           |}
        """.stripMargin)

      serviceUnderTest.createRequestFrom(matchWithUTRAndName) must be(expectedJson)
    }
  }

  "response adaptor " must {

    "create matchSuccess response when RegisterWithId Response contains mandatory values with organisation option" in {
      val responseFromRegisterWithIdService = Json.parse(
        """
          |{
          |"MDGHeader": {
          |   "status": "OK",
          |   "processingDate":"2001-12-17T09:30:47.123456Z",
          |   "returnParameters": []
          |},
          | "SAFEID":"XE0000123456789",
          | "isEditable":true,
          | "isAnAgent":false,
          | "isAnIndividual":false,
          | "organisation":{
          |   "organisationName":"orgName",
          |   "isAGroup":false,
          |   "code":"0001"
          | },
          | "address":{
          |  "addressType":"0001",
          |  "addressLine1":"21 Emmbrook Lane",
          |  "addressLine2":"Wokingham",
          |  "countryCode":"GB"
          | },
          | "contactDetails":{}
          |}
        """.stripMargin)

      val dataReturned = Json.toJson(serviceUnderTest.convertToMatchSuccess(responseFromRegisterWithIdService))

      val addressReturned = (dataReturned \ "address").as[Option[EtmpAddress]].get
      addressReturned must be(EtmpAddress("21 Emmbrook Lane", "Wokingham", None, None, None, "GB"))

      (dataReturned \ "safeId").as[String] must be("XE0000123456789")

      (dataReturned \ "isAnIndividual").as[Boolean] mustBe false

      val orgReturned = (dataReturned \ "organisation").as[Option[OrganisationResponse]].get
      orgReturned must be(OrganisationResponse("orgName", Some(false), None))

    }

    "create matchSuccess response when RegisterWithId Response contains optional values as well with organisation option" in {
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

      val orgReturned = (dataReturned \ "organisation").as[OrganisationResponse]
      orgReturned must be(OrganisationResponse("orgName", Some(true), Some("LLP")))

      val addressReturned = (dataReturned \ "address").as[Option[EtmpAddress]].get
      addressReturned must be(EtmpAddress("21 Emmbrook Lane", "Wokingham", Some("adLine3"), Some("adLine4"), Some("RG41 4RR"), "GB"))

      (dataReturned \ "agentReferenceNumber").as[String] must be("agentReferenceNumber123")

      (dataReturned \ "safeId").as[String] must be("XE0000123456789")

      (dataReturned \ "sapNumber").as[String] must be("0123456789")

      (dataReturned \ "isAnIndividual").as[Boolean] must be(false)
    }

    "create matchFailure response when RegisterWithId Fails to match" in {
      val responseFromRegisterWithIdService = Json.parse(
        """
          {
          |"MDGErrorDetail": {
          |    "timestamp" : "2016-07-18 10:26:46.147+0100",
          |    "correlationId": "",
          |    "errorCode": "NOT_OK",
          |    "errorMessage": "Duplicate Submission",
          |    "messageId": "",
          |    "source": "CT-Adapter"
          |   }
          |}""".stripMargin)

      val matchFailureResponse = serviceUnderTest.convertToMatchFailure(responseFromRegisterWithIdService)

      matchFailureResponse must be(Json.parse(
        """
          |{ "Reason": "Duplicate Submission" }
        """.stripMargin))
    }
  }
}
