package utils

import org.apache.commons.lang3.RandomStringUtils

object SessionUtils {

  def getUniqueAckNo: String = {
    val length = 32
    val nanoTime = System.nanoTime()
    val restChars = length - nanoTime.toString.length
    val randomChars = RandomStringUtils.randomAlphanumeric(restChars)
    randomChars + nanoTime
  }


}
