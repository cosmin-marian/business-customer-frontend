package models


/**
 * Created by dev01 on 01/06/15.
 */
import play.api.libs.json.Json

case class SubscriptionDetails(service: String, isAgent: Boolean)

object SubscriptionDetails {
  implicit val formats = Json.format[SubscriptionDetails]
}
