package models

import play.api.libs.json.Json

case class KnownFact(`type`: String, value: String)

object KnownFact {
  implicit val formats = Json.format[KnownFact]
}

case class KnownFactsForService(facts: List[KnownFact])

object KnownFactsForService {
  implicit val formats = Json.format[KnownFactsForService]
}
