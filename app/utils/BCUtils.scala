package utils

import java.util.Properties

import scala.io.Source

object BCUtils {

  val p = new Properties
  p.load(Source.fromURL(getClass.getResource("/country-code.properties")).bufferedReader())

  private val ZERO = 0
  private val ONE = 1
  private val TWO = 2
  private val THREE = 3
  private val FOUR = 4
  private val FIVE = 5
  private val SIX = 6
  private val SEVEN = 7
  private val EIGHT = 8
  private val NINE = 9
  private val TEN = 10

  def validateUTR(utr: Option[String]): Boolean = {
    utr match {
      case Some(utr) => {
        utr.trim.length == TEN && utr.trim.forall(_.isDigit) && {
          val actualUtr = utr.trim.toList
          val checkDigit = actualUtr.head.asDigit
          val restOfUtr = actualUtr.tail
          val weights = List(SIX, SEVEN, EIGHT, NINE, TEN, FIVE, FOUR, THREE, TWO)
          val weightedUtr = for ((w1, u1) <- weights zip restOfUtr) yield {
            w1 * (u1.asDigit)
          }
          val total = weightedUtr.sum
          val remainder = total % 11
          isValidUtr(remainder, checkDigit)
        }
      }
      case None => false
    }
  }

  private def isValidUtr(remainder: Int, checkDigit: Int): Boolean = {
    val mapOfRemainders = Map(ZERO -> TWO, ONE -> ONE, TWO -> NINE, THREE -> EIGHT, FOUR -> SEVEN, FIVE -> SIX,
      SIX -> FIVE, SEVEN -> FOUR, EIGHT -> THREE, NINE -> TWO, TEN -> ONE)
    Some(checkDigit) == mapOfRemainders.get(remainder)
  }

  def getIsoCodeTupleList: List[(String, String)] = {
    val keys = p.propertyNames()
    val listOfCountryCodes: scala.collection.mutable.MutableList[(String, String)] = scala.collection.mutable.MutableList()
    while (keys.hasMoreElements) {
      val key = keys.nextElement().toString
      listOfCountryCodes.+=:((key , p.getProperty(key)))
    }
    listOfCountryCodes.toList.sortBy(_._2)
  }

  def getSelectedCountry(isoCode: String): String = {
    p.getProperty(isoCode.toUpperCase)
  }

}
