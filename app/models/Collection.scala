package models

import play.api.Play.current
import play.modules.reactivemongo.json.collection.{JSONGenericHandlers, JSONCollection}
import reactivemongo.api._
import reactivemongo.api.gridfs._
import reactivemongo.bson._
import play.api.libs.iteratee._
import play.api.mvc._
import play.api.Play.current
import scala.concurrent.{ Future, ExecutionContext }
import play.modules.reactivemongo.ReactiveMongoPlugin
import reactivemongo.api.collections.{GenericQueryBuilder, GenericCollection}
import play.api.libs.json._
import java.text.Format
import scala.reflect._
import mojolly.inflector.Inflector
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json
import reactivemongo.core.commands.{Count, GetLastError, LastError}
import reactivemongo.api.FailoverStrategy
import play.api.libs.json.JsObject
import reactivemongo.api.collections.default.BSONCollection


/**
 * User: aloise
 * Date: 10/14/13
 * Time: 12:17 AM
 */
abstract class Collection[T](collectionName:String) extends JSONGenericHandlers {

  import reactivemongo.bson._
  import play.modules.reactivemongo.json.BSONFormats



  def db: DB = ReactiveMongoPlugin.db

  implicit def failoverStrategy: FailoverStrategy = FailoverStrategy()


  val collection = db[JSONCollection](collectionName)

  def name = collectionName

  val ID = "_id"

  implicit def ec: ExecutionContext = ExecutionContext.Implicits.global

//  implicit val format: json.Format[T]



  /*

  def find(query: (String, JsValueWrapper)*): Future[List[T]] = find(Json.obj(query: _*)).cursor[T].toList
  def findOption(query: (String, JsValueWrapper)*): Future[Option[T]] = find(query).cursor[T].headOption

  def findById(id: BSONObjectID): Future[Option[T]] = findOption(ID -> id)
  def findById(id: String): Future[Option[T]] = findById(BSONObjectID(id))

  def update(id: BSONObjectID, model: T): Future[LastError] = update(Json.obj(ID -> id), model, upsert=true)
  def update(id: String, model: T): Future[LastError] = update(BSONObjectID(id), model)

  def remove(query: (String, JsValueWrapper)*): Future[LastError] = remove(query)
  def remove(id: BSONObjectID): Future[LastError] = remove(Json.obj(ID -> id))
  def remove(id: String): Future[LastError] = remove(BSONObjectID(id))


  def save(doc: JsObject)(implicit ec: ExecutionContext): Future[LastError] = save(doc, GetLastError())
  def save(doc: JsObject, writeConcern: GetLastError)(implicit ec: ExecutionContext): Future[LastError] = {

    (doc \ "_id" match {
      case JsUndefined(_) => insert(doc + ("_id" -> BSONFormats.BSONObjectIDFormat.writes(BSONObjectID.generate)), writeConcern)
      case id             => update(Json.obj("_id" -> id), doc, writeConcern, upsert = true)
    })
  }
  def save[T](doc: T, writeConcern: GetLastError = GetLastError())(implicit ec: ExecutionContext, writer: Writes[T]): Future[LastError] = {
    save(writer.writes(doc).as[JsObject], writeConcern)
  }


  */

  def find(q: JsObject)(implicit reader: Reads[T]): Future[List[T]] = {
    val jsonValuesFuture = collection.find(q).cursor[JsObject].toList()
    // Transform future of JSON values to future of T, but only keep the successfully parsed ones
    jsonValuesFuture map { jsonValues =>
      jsonValues map { json =>
        Json.fromJson[T](json) // JsResult
      } collect { case JsSuccess(transaction, _) => transaction }
    }
  }

  /**
   * Find all items for the given JsValue query.
   * Implicit JsValue -> T must be in scope
   */
  def find[T](q: JsValue)(implicit reader: Reads[T]): Future[List[T]] = find[T](q)

  /**
   * Find one item and maybe return it.
   * Implicit JsValue -> T must be in scope
   */
  /*
  def findOne(q: JsValue)(implicit reader: Reads[T]): Future[Option[T]] = {
    val res: Future[Option[JsValue]] = collection.find(q).cursor.
    res map { jsValueOpt =>
      jsValueOpt map { value =>
        Json.fromJson[T](value).asOpt // JsResult => Option || .getOrElse directly?
      } getOrElse None
    }
  }
  */

  def insert(t: T)(implicit writer: Writes[T]): Future[LastError] =
    collection.insert(Json.toJson(t))

  /*
   * Writes an updated version of a model class to the database.
   */
  //TODO def update(t: T)(implicit writer: Writes[T]): Future[LastError] =
  // collection.update()

  def removeById(id: BSONObjectID)(implicit writer: Writes[BSONObjectID]): Future[LastError] =
    collection.remove(Json.obj("_id" -> id))

  // -- Convenience

  /**
   * Find one item by its _id and maybe return it.
   * Implicit JsValue -> T must be in scope
   * Implicit ID -> JsValue must be in scope
   */
  /*
  def findOneById(id: BSONObjectID)(implicit reader: Reads[T], writer: Writes[BSONObjectID]): Future[Option[T]] =
    findOne(Json.obj("_id" -> id))
  */

  def count(q: Option[BSONDocument] = None): Future[Int] =
    collection.db.command(Count(collectionName = collection.name, query = q))

  def all(implicit reader: Reads[T]): Future[List[T]] =
    find[T](Json.obj())

  // For performance reasons, this is not implemented in terms of findOne, but find().limit()
  /*
  def exists: Future[Boolean] =
    collection.find[JsValue](Json.obj()).headOption.map(_.isDefined)
  */

}
