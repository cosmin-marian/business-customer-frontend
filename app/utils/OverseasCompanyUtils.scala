package utils

import models.OverseasCompanyDisplayDetails
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._

/**
  * Created by paulcarrielies on 11/01/2017.
  */
object OverseasCompanyUtils {
  def displayDetails(isAgent: Boolean, addClient: Boolean) = {

    (isAgent, addClient) match {
      case (true, true) =>
        OverseasCompanyDisplayDetails(
          Messages("bc.nonuk.overseas.agent.add-client.title"),
          Messages("bc.nonuk.overseas.agent.add-client.header"),
          Messages("bc.nonuk.overseas.agent.add-client.subheader"),
          addClient)
      case (true, false) =>
        OverseasCompanyDisplayDetails(
          Messages("bc.nonuk.overseas.agent.title"),
          Messages("bc.nonuk.overseas.agent.header"),
          Messages("bc.nonuk.overseas.agent.subheader"),
          addClient)
      case (_, _) =>
        OverseasCompanyDisplayDetails(
          Messages("bc.nonuk.overseas.client.title"),
          Messages("bc.nonuk.overseas.client.header"),
          Messages("bc.nonuk.overseas.client.subheader"),
          addClient)
    }

  }
}
