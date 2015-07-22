package utils

import java.util.UUID

object SessionUtils {

  def sessionOrUUID(): String = {
    UUID.randomUUID().toString.replace("-", "")
  }
}
