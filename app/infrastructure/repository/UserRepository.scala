package infrastructure.repository

import infrastructure.repository.common.Repository
import infrastructure.repository.common.MongoDbCrudUtils
import models.common.Page
import models.User
import scala.concurrent.Future
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import reactivemongo.api.DB
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.ReactiveMongoPlugin

trait MongoDbUserRepository extends Repository[User] {
  val db = ReactiveMongoPlugin.db
  lazy val collection: JSONCollection = db[JSONCollection](CollectionReferences.USERS)

  private val crud = MongoDbCrudUtils(collection, User.format, List("device.uuid"), "uuid")

  override def findAll(query: String = "", sort: String = ""): Future[List[User]] = crud.findAll(query, sort)
  override def findPage(query: String = "", page: Int = 1, sort: String = ""): Future[Page[User]] = crud.findPage(query, page, sort)
  override def getByUuid(uuid: String): Future[Option[User]] = crud.getByUuid(uuid)
  override def insert(elt: User): Future[Option[User]] = { crud.insert(elt).map(err => if (err.ok) Some(elt) else None) }
  override def update(uuid: String, elt: User): Future[Option[User]] = crud.update(uuid, elt).map(err => if (err.ok) Some(elt) else None)
  override def delete(uuid: String): Future[Option[User]] = crud.delete(uuid).map(err => None) // TODO : return deleted elt !
  
  def getByDevice(deviceId: String): Future[Option[User]] = crud.getBy("device.uuid", deviceId)
}
object UserRepository extends MongoDbUserRepository
