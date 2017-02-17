package models

import play.api.libs.json.Json


case class BackLinkModel(backLink: Option[String])

object BackLinkModel {
  implicit val formats = Json.format[BackLinkModel]
}
