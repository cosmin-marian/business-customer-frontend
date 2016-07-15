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

package models

import play.api.libs.json.Json

case class MDGHeader(originatingSystem: String = "MDTP", requestTimeStamp: String, correlationId: String)

object MDGHeader {
  implicit val formats = Json.format[MDGHeader]
}

case class MessageTypes(messageType: String = "RegisterWithID")

object MessageTypes {
  implicit val formats = Json.format[MessageTypes]
}

case class RegistrationDetails(IDType: String = "UTR",
                               IDNumber: String,
                               requiresNameMatch: Boolean,
                               isAnAgent: Boolean,
                               individual: Option[Individual] = None,
                               organisation: Option[Organisation] = None)

object RegistrationDetails {
  implicit val formats = Json.format[RegistrationDetails]
}

case class RegisterWithIdRequest(MDGHeader: MDGHeader,
                                 messageTypes: MessageTypes = MessageTypes(),
                                 registrationDetails: RegistrationDetails)

object RegisterWithIdRequest {
  implicit val formats = Json.format[RegisterWithIdRequest]
}


case class Param(paramName: String, paramValue: String)

object Param {
  implicit val formats = Json.format[Param]
}

case class MDGHeaderRsp(returnParameters: List[Param])

object MDGHeaderRsp {
  implicit val formats = Json.format[MDGHeaderRsp]
}

case class ServiceResponse(MDGHeader: MDGHeaderRsp,
                           SAFEID: String,
                           ARN: Option[String],
                           isAnIndividual: Boolean,
                           address: EtmpAddress,
                           organisation: Option[OrganisationResponse],
                           individual: Option[Individual] = None)


object ServiceResponse {
  implicit val formats = Json.format[ServiceResponse]
}

case class FailureServiceRsp(MDGHeader: MDGHeaderRsp)

object FailureServiceRsp {
  implicit val formats = Json.format[FailureServiceRsp]
}
