package api.controllers

import common.repositories.user.CrashRepository
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.json.JsValue

object Crashs extends Controller {

  def getAll() = Action.async { implicit req =>
    CrashRepository.find().map { elts =>
      Ok(Json.toJson(elts))
    }
  }

  def get(crashId: String) = Action.async { implicit req =>
    CrashRepository.get(crashId).map {
      _.map { elt =>
        Ok(Json.toJson(elt))
      }.getOrElse(NotFound)
    }
  }

  def receive() = Action.async(parse.json) { implicit req =>
    CrashRepository.insert(req.body).map { err =>
      if (err.ok) NoContent else InternalServerError
    }
  }

  def receiveBatch() = Action.async(parse.json) { implicit req =>
    req.body.asOpt[List[JsValue]].map { batch =>
      CrashRepository.bulkInsert(batch).map { res =>
        if (res.n == batch.length) Ok(Json.obj("inserted" -> res.n)) else InternalServerError
      }
    }.getOrElse(Future(BadRequest))
  }

  def tmpReveiceEvents() = Action(parse.json) { implicit req =>
    req.body.asOpt[List[JsValue]].map { batch =>
      Ok(Json.obj("inserted" -> batch.length))
    }.getOrElse(BadRequest)
  }

  def delete(crashId: String) = Action.async { implicit req =>
    CrashRepository.delete(crashId).map { err =>
      if (err.ok) NoContent else InternalServerError
    }
  }

}
