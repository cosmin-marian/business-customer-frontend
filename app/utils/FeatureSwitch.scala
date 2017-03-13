package utils

import play.api.Play
import play.api.libs.json.Json
import uk.gov.hmrc.play.config.RunMode

case class FeatureSwitch(name: String, enabled: Boolean)

object FeatureSwitch extends RunMode {
  import play.api.Play.current

  def forName(name: String) = {
    FeatureSwitch(name, isEnabled(name))
  }

  def isEnabled(name: String) = {
    val sysPropValue = sys.props.get(systemPropertyName(name))
    sysPropValue match {
      case Some(x) => x.toBoolean
      case None => Play.configuration.getBoolean(confPropertyName(name)).getOrElse(false)
    }
  }

  def enable(switch: FeatureSwitch): FeatureSwitch = {
    setProp(switch.name, true)
  }

  def disable(switch: FeatureSwitch): FeatureSwitch = setProp(switch.name, false)

  def setProp(name: String, value: Boolean): FeatureSwitch = {
    val systemProps = sys.props.+= ((systemPropertyName(name), value.toString))
    forName(name)
  }

  def confPropertyName(name: String) = s"$env.features.$name"
  def systemPropertyName(name: String) = s"features.$name"

  implicit val format = Json.format[FeatureSwitch]
}

object BusinessCustomerFeatureSwitches {

  def byName(name: String): Option[FeatureSwitch] = name match {
    case _ => None
  }

}
