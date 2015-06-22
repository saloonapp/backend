package controllers

import common.Utils
import models.event.Event
import models.event.Attendee
import models.event.Session
import models.event.Exponent
import models.user.Device
import infrastructure.repository.EventRepository
import infrastructure.repository.SessionRepository
import infrastructure.repository.ExponentRepository
import infrastructure.repository.DeviceRepository
import services.MailSrv
import services.MandrillSrv
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.libs.json.Json

object Application extends Controller {

  def home = Action { implicit req =>
    Ok(views.html.Application.home())
  }
  def sample = Action { implicit req =>
    Ok(views.html.Application.sample())
  }

  //def migrate = TODO
  def migrate = Action.async {
    migrateAttendee().map { res =>
      Ok(Json.toJson(res))
    }
  }
  private def migrateAttendee() = {
    for {
      sessions <- SessionRepository.findAll()
      exponents <- ExponentRepository.findAll()
    } yield {
      val speakers = sessions.flatMap(s => s.info.speakers.map(_.transform(s.eventId, "speaker")))
      val exponentAttendee = exponents.flatMap(s => s.info.team.map(_.transform(s.eventId, "exposant")))
      val uniqAttendeesByEvent: Map[String, List[Attendee]] = (speakers ++ exponentAttendee).groupBy(_.eventId).map { case (eventId, attendees) => (eventId, attendees.groupBy(_.name).map(_._2.head).toList) }
      val sessionsWithSpeakerIds = sessions.map { e =>
        e.copy(info = e.info.copy(speakers2 = Some(e.info.speakers.map { s =>
          uniqAttendeesByEvent.get(e.eventId).flatMap(_.find(_.name == s.name).map(_.uuid))
        }.flatten)))
      }
      val exponentsWithTeamIds = exponents.map { e =>
        e.copy(info = e.info.copy(team2 = Some(e.info.team.map { s =>
          uniqAttendeesByEvent.get(e.eventId).flatMap(_.find(_.name == s.name).map(_.uuid))
        }.flatten)))
      }
      uniqAttendeesByEvent
    }
  }
  /*def migrate = Action.async {
    for {
      m1 <- migrateEvents()
      m2 <- migrateSessions()
      m3 <- migrateExponents()
      m4 <- migrateDevices()
    } yield {
      Redirect(routes.Application.home).flashing("success" -> "Migrated !")
    }
  }
  private def migrateEvents(): Future[List[Option[Event]]] = {
    EventRepository.findAllOld().flatMap(list => Future.sequence(list.map { e =>
      EventRepository.update(e.uuid, e.transform())
    }))
  }
  private def migrateSessions(): Future[List[Option[Session]]] = {
    SessionRepository.findAllOld().flatMap(list => Future.sequence(list.map { e =>
      SessionRepository.update(e.uuid, e.transform())
    }))
  }
  private def migrateExponents(): Future[List[Option[Exponent]]] = {
    ExponentRepository.findAllOld().flatMap(list => Future.sequence(list.map { e =>
      ExponentRepository.update(e.uuid, e.transform())
    }))
  }
  private def migrateDevices(): Future[Int] = {
    DeviceRepository.findAllUserOld().flatMap { list =>
      DeviceRepository.bulkInsert(list.map(_.transform())).flatMap { inserted =>
        DeviceRepository.dropUserOld().map { done => inserted }
      }
    }
  }*/

  def corsPreflight(all: String) = Action {
    Ok("").withHeaders(
      "Allow" -> "*",
      "Access-Control-Allow-Origin" -> "*",
      "Access-Control-Allow-Methods" -> "GET, POST, PUT, DELETE, OPTIONS",
      "Access-Control-Allow-Headers" -> "Origin, X-Requested-With, Content-Type, Accept, Referrer, User-Agent, userId, timestamp");
  }
}
