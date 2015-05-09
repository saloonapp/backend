package controllers

import infrastructure.repository.common.Repository
import infrastructure.repository.UserRepository
import models.User
import models.UserData
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.data.Form

object Users extends Controller {
  val form: Form[UserData] = Form(UserData.fields)
  val repository: Repository[User] = UserRepository
  val mainRoute = routes.Users
  val viewList = views.html.Application.Users.list
  val viewDetails = views.html.Application.Users.details
  val viewCreate = views.html.Application.Users.create
  val viewUpdate = views.html.Application.Users.update
  def createElt(data: UserData): User = UserData.toModel(data)
  def toData(elt: User): UserData = UserData.fromModel(elt)
  def updateElt(elt: User, data: UserData): User = UserData.merge(elt, data)
  def successCreateFlash(elt: User) = s"User '${elt.device.uuid}' has been created"
  def errorCreateFlash(elt: UserData) = s"User '${elt.device.uuid}' can't be created"
  def successUpdateFlash(elt: User) = s"User '${elt.device.uuid}' has been modified"
  def errorUpdateFlash(elt: User) = s"User '${elt.device.uuid}' can't be modified"
  def successDeleteFlash(elt: User) = s"User '${elt.device.uuid}' has been deleted"

  def list(query: Option[String], page: Option[Int], sort: Option[String]) = Action.async { implicit req =>
    val curPage = page.getOrElse(1)
    repository.findPage(query.getOrElse(""), curPage, sort.getOrElse("-start")).map { eltPage =>
      if (curPage > 1 && eltPage.totalPages < curPage)
       Redirect(mainRoute.list(query, Some(eltPage.totalPages), sort))
      else
        Ok(viewList(eltPage))
    }
  }

  def create = Action { implicit req =>
    Ok(viewCreate(form))
  }

  def doCreate = Action.async { implicit req =>
    form.bindFromRequest.fold(
      formWithErrors => Future(BadRequest(viewCreate(formWithErrors))),
      formData => repository.insert(createElt(formData)).map {
        _.map { elt =>
          Redirect(mainRoute.list()).flashing("success" -> successCreateFlash(elt))
        }.getOrElse(InternalServerError(viewCreate(form.fill(formData))).flashing("error" -> errorCreateFlash(formData)))
      })
  }

  def details(uuid: String) = Action.async { implicit req =>
    repository.getByUuid(uuid).map {
      _.map { elt =>
        Ok(viewDetails(elt))
      }.getOrElse(NotFound(views.html.error404()))
    }
  }

  def update(uuid: String) = Action.async { implicit req =>
    repository.getByUuid(uuid).map {
      _.map { elt =>
        Ok(viewUpdate(form.fill(toData(elt)), elt))
      }.getOrElse(NotFound(views.html.error404()))
    }
  }

  def doUpdate(uuid: String) = Action.async { implicit req =>
    repository.getByUuid(uuid).flatMap {
      _.map { elt =>
        form.bindFromRequest.fold(
          formWithErrors => Future(BadRequest(viewUpdate(formWithErrors, elt))),
          formData => repository.update(uuid, updateElt(elt, formData)).map {
            _.map { updatedElt =>
              Redirect(mainRoute.details(uuid)).flashing("success" -> successUpdateFlash(updatedElt))
            }.getOrElse(InternalServerError(viewUpdate(form.fill(formData), elt)).flashing("error" -> errorUpdateFlash(elt)))
          })
      }.getOrElse(Future(NotFound(views.html.error404())))
    }
  }

  def delete(uuid: String) = Action.async { implicit req =>
    repository.getByUuid(uuid).map {
      _.map { elt =>
        repository.delete(uuid)
        Redirect(mainRoute.list()).flashing("success" -> successDeleteFlash(elt))
      }.getOrElse(NotFound(views.html.error404()))
    }
  }
}