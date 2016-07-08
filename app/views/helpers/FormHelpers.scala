package views.helpers

import play.api.data._

object FormHelpers {

  /*
   * This function finds all the errors belonging to the parent that have the child field as an argument.
   */
  def getErrors(field: Field, parent: Field): Seq[FormError] = {
    parent.errors.foldLeft(field.errors) { (errors, error) =>
      error.args.map { arg =>
        parent.name + "." + arg
      }.contains(field.name) match {
        case true => error +: errors
        case _ => errors
      }
    }
  }

  /*
   * This function finds all errors on a form which are either keyed for the field or contain the fields full path in their arguments
   */
  def getErrors(field: Field, form: Form[_]): Seq[FormError] = {
    form.errors.filter { error => error.key == field.name || error.args.contains(field.name) || field.name == error.args.fold(error.key) {
      _ + "." + _
    }
    }
  }

  /*
   * This is a routing function to determine how to resolve the errors on a field depending on whether a parent field is passed or a form reference is in scope
   */
  def getErrors(field: Field, parent: Option[Field] = None)(implicit form: Option[Form[_]] = None): Seq[FormError] = {
    parent match {
      case Some(p) => getErrors(field, p)
      case _ => form match {
        case Some(f) => getErrors(field, f)
        case _ => field.errors
      }
    }
  }
}
