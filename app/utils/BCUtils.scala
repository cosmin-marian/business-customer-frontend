package utils

object BCUtils {

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
        utr.trim.length == 10 && utr.trim.forall(_.isDigit) && {
          val actualUtr = utr.trim.toList
          val checkDigit = actualUtr.head.asDigit
          val restOfUtr = actualUtr.tail
          val weights = List(6, 7, 8, 9, 10, 5, 4, 3, 2)
          val weightedUtr = for((w1,u1) <- weights zip restOfUtr) yield {
            w1*(u1.asDigit)
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
    remainder match {
      case ZERO => checkDigit == TWO
      case ONE => checkDigit == ONE
      case TWO => checkDigit == NINE
      case THREE => checkDigit == EIGHT
      case FOUR => checkDigit == SEVEN
      case FIVE => checkDigit == SIX
      case SIX => checkDigit == FIVE
      case SEVEN => checkDigit == FOUR
      case EIGHT => checkDigit == THREE
      case NINE => checkDigit == TWO
      case TEN => checkDigit == ONE
    }
  }

}
