package utils

import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}

class FeatureSwitchSpec extends PlaySpec with OneServerPerSuite with BeforeAndAfterEach {

  override def beforeEach = {
    System.clearProperty("feature.test")
  }

  "FeatureSwitch" should {

    "generate correct system property name for the feature" in {
      FeatureSwitch.systemPropertyName("test") must be("features.test")
    }

    "be ENABLED if the system property is defined as 'true'" in {
      System.setProperty("features.test", "true")

      FeatureSwitch.forName("test").enabled must be(true)
    }

    "be DISABLED if the system property is defined as 'false'" in {
      System.setProperty("features.test", "false")

      FeatureSwitch.forName("test").enabled must be(false)
    }

    "be DISABLED if the system property is undefined" in {
      System.clearProperty("features.test")

      FeatureSwitch.forName("test").enabled must be(false)
    }

    "support dynamic toggling" in {
      System.setProperty("features.test", "false")
      val testFeatureSwitch = FeatureSwitch("test", enabled = true)
      FeatureSwitch.enable(testFeatureSwitch)
      FeatureSwitch.forName("test").enabled must be(true)

      FeatureSwitch.disable(testFeatureSwitch)
      FeatureSwitch.forName("test").enabled must be(false)
    }

    "BusinessCustomerFeatureSwitches.byName should return Some(feature) or None" in {
      BusinessCustomerFeatureSwitches.byName("xyz") must be(None)
    }

  }

}
