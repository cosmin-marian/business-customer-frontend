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

import java.time.Clock
import java.util.UUID

import models._
import play.api.libs.json._
import utils.BusinessCustomerConstants.{CorporateBody, Llp, Partnership, UnincorporatedBody}

trait RegisterWithIdServiceAdaptor {

  def uuidGenerator: RandomUUIDGenerator

  def clock: Clock

  private val OrgTypes = Map(
    Partnership -> "0001",
    Llp -> "0002",
    CorporateBody -> "0003",
    UnincorporatedBody -> "0004")

  def createRequestFrom(requestData: MatchBusinessData): JsValue = {
    Json.toJson(RegisterWithIdRequest(
      MDGHeader("MDTP", clock.instant().toString, uuidGenerator.generateUUIDAsString),
      MessageTypes("RegisterWithID"),
      List(Param("REGIME", "CDS")),

      RegistrationDetails(
        "UTR",
        requestData.utr,
        requestData.requiresNameMatch,
        requestData.isAnAgent,
        None,
        requestData.organisation.map(org => Organisation(org.organisationName, OrgTypes(org.organisationType))))))
  }

  def convertToMatchFailure(json: JsValue): JsValue = {
    val reason: String = json.as[FailureServiceRsp].MDGErrorDetail.errorMessage
    Json.toJson(MatchFailureResponse(reason))
  }


  def convertToMatchSuccess(json: JsValue): JsValue = {
    val rsp = json.as[ServiceResponse]
    val sapNumber = rsp.MDGHeader.returnParameters.find(_.paramName == "SAP_NUMBER").map(_.paramValue)
    Json.toJson(MatchSuccessResponse(rsp.isAnIndividual, rsp.ARN, sapNumber, rsp.SAFEID, rsp.address, rsp.organisation, None))
  }
}

class RandomUUIDGenerator {
  def generateUUIDAsString: String = {
    UUID.randomUUID().toString
  }
}

object RegisterWithIdServiceAdaptor extends RegisterWithIdServiceAdaptor {
  val uuidGenerator: RandomUUIDGenerator = new RandomUUIDGenerator {}
  val clock: Clock = Clock.systemUTC()
}
