package actors.applications.dixie

import actors.Application
import reactivemongo.bson._
/**
 * User: aloise
 * Date: 22.11.14
 * Time: 19:44
 */
class DixieApplication( application:models.Application ) extends Application( application ) {

  import actors.applications.dixie.DixieApplication._

}

object DixieApplication {
  type GameCard = String
}