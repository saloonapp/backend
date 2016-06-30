package conferences.controllers

import common.{Utils, Defaults}
import common.repositories.conference.ConferenceRepository
import common.services._
import conferences.models._
import conferences.services.TwittFactory
import org.joda.time.DateTime
import play.api.data.Form
import play.api.libs.json.{JsNull, JsObject, Json}
import play.api.libs.ws.WS
import play.api.mvc._
import play.api.Play.current
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

object Application extends Controller {
  val conferenceForm = Form(ConferenceData.fields)

  def list = Action.async { implicit req =>
    val conferenceListFut = ConferenceRepository.find(Json.obj("end" -> Json.obj("$gte" -> new DateTime())), Json.obj("start" -> 1))
    //val tagsFut = ConferenceRepository.getTags()
    for {
      conferenceList <- conferenceListFut
      //tags <- tagsFut
    } yield Ok(conferences.views.html.conferenceList("future", conferenceList))
  }
  def search(section: Option[String], q: Option[String], period: Option[String], before: Option[String], after: Option[String], tags: Option[String], cfp: Option[String], tickets: Option[String], videos: Option[String]) = Action.async { implicit req =>
    val (pAfter, pBefore) = period.map(_.split(" - ") match {
      case Array(a, b) => (Some(a), Some(b))
      case _ => (None, None)
    }).getOrElse((None, None))
    val conferenceListFut = ConferenceRepository.find(buildFilter(q, before.orElse(pBefore), after.orElse(pAfter), tags, cfp, tickets, videos))
    //val tagsFut = ConferenceRepository.getTags()
    for {
      conferenceList <- conferenceListFut
      //tags <- tagsFut
    } yield Ok(conferences.views.html.conferenceList(section.getOrElse("search"), conferenceList))
  }
  def fullHistory = Action.async { implicit req =>
    ConferenceRepository.findHistory().map { conferenceList =>
      Ok(conferences.views.html.conferenceListHistory(conferenceList))
    }
  }
  def detail(id: ConferenceId) = detailPrivate(id, None)
  def detailVersion(id: ConferenceId, created: Long) = detailPrivate(id, Some(created))
  private def detailPrivate(id: ConferenceId, created: Option[Long]) = Action.async { implicit req =>
    created.map(c => ConferenceRepository.get(id, new DateTime(c))).getOrElse(ConferenceRepository.get(id)).map { conferenceOpt =>
      conferenceOpt.map { conference =>
        Ok(conferences.views.html.conferenceDetail(conference))
      }.getOrElse {
        NotFound("Not Found !")
      }
    }
  }
  def create = Action.async { implicit req =>
    ConferenceRepository.getTags().map { tags =>
      Ok(conferences.views.html.conferenceForm(conferenceForm, tags))
    }
  }
  def doCreate = Action.async { implicit req =>
    conferenceForm.bindFromRequest.fold(
      formWithErrors => ConferenceRepository.getTags().map { tags =>
        BadRequest(conferences.views.html.conferenceForm(formWithErrors, tags))
      },
      formData => {
        val conference = ConferenceData.toModel(formData)
        ConferenceRepository.insert(conference).map { success =>
          if(conference.start.isAfterNow){
            TwitterSrv.twitt(TwittFactory.newConference(conference))
          }
          Redirect(conferences.controllers.routes.Application.detail(conference.id))
        }
      }
    )
  }
  def edit(id: ConferenceId) = editPrivate(id, None)
  def editVersion(id: ConferenceId, created: Long) = editPrivate(id, Some(created))
  private def editPrivate(id: ConferenceId, created: Option[Long]) = Action.async { implicit req =>
    val conferenceOptFut = created.map(c => ConferenceRepository.get(id, new DateTime(c))).getOrElse(ConferenceRepository.get(id))
    val tagsFut = ConferenceRepository.getTags()
    for {
      conferenceOpt <- conferenceOptFut
      tags <- tagsFut
    } yield {
      conferenceOpt.map { conference =>
        Ok(conferences.views.html.conferenceForm(conferenceForm.fill(ConferenceData.fromModel(conference)), tags))
      }.getOrElse {
        NotFound("Not Found !")
      }
    }
  }
  def doEdit(id: ConferenceId) = Action.async { implicit req =>
    conferenceForm.bindFromRequest.fold(
      formWithErrors => ConferenceRepository.getTags().map { tags =>
        BadRequest(conferences.views.html.conferenceForm(formWithErrors, tags))
      },
      formData => {
        val conference = ConferenceData.toModel(formData)
        ConferenceRepository.get(id).flatMap { oldConferenceOpt =>
          ConferenceRepository.update(id, conference).map { success =>
            oldConferenceOpt.filter(_.videosUrl.isEmpty && conference.videosUrl.isDefined).map { _ =>
              TwitterSrv.twitt(TwittFactory.publishVideos(conference))
            }
            oldConferenceOpt.filter(_.cfp.isEmpty && conference.cfp.map(_.end.isAfterNow).getOrElse(false)).map { _ =>
              TwitterSrv.twitt(TwittFactory.openCfp(conference))
            }
            Redirect(conferences.controllers.routes.Application.detail(id))
          }
        }
      }
    )
  }
  def history(id: ConferenceId) = Action.async { implicit req =>
    ConferenceRepository.getHistory(id).map { conferenceList =>
      if(conferenceList.length > 0){
        Ok(conferences.views.html.conferenceHistory(conferenceList))
      } else {
        NotFound("Not Found !")
      }
    }
  }
  def doDelete(id: ConferenceId, created: Long) = Action.async { implicit req =>
    ConferenceRepository.deleteVersion(id, new DateTime(created)).map { _ =>
      Redirect(req.headers("referer"))
    }
  }
  /*def doDeleteAll(id: ConferenceId) = Action.async { implicit req =>
    ConferenceRepository.delete(id).map { _ =>
      Redirect(conferences.controllers.routes.Application.list)
    }
  }*/

  def apiList = Action.async { implicit req =>
    ConferenceRepository.find().map { conferences =>
      Ok(Json.obj("result" -> conferences.map(c => c.copy(createdBy = c.createdBy.filter(_.public)))))
    }
  }
  def apiSearch(section: Option[String], q: Option[String], before: Option[String], after: Option[String], tags: Option[String], cfp: Option[String], tickets: Option[String], videos: Option[String]) = Action.async { implicit req =>
    ConferenceRepository.find(buildFilter(q, before, after, tags, cfp, tickets, videos)).map { conferences =>
      Ok(Json.obj("result" -> conferences.map(c => c.copy(createdBy = c.createdBy.filter(_.public)))))
    }
  }
  def testNewsletter(emailOpt: Option[String], date: Option[String]) = Action.async { implicit req =>
    NewsletterScheduler.getNewsletterInfos(date.map(d => DateTime.parse(d, Defaults.dateFormatter)).getOrElse(new DateTime())).flatMap { case (closingCFPs, incomingConferences, newData) =>
      emailOpt.map { email =>
        MailChimpSrv.createAndTestCampaign(MailChimpCampaign.conferenceListNewsletterTest(closingCFPs, incomingConferences, newData), List(email)).map { url =>
          Ok(Json.obj(
            "newsletterUrl" -> url,
            "closingCFPs" -> closingCFPs,
            "incomingConferences" -> incomingConferences,
            "newData" -> newData.map{case (c, d) => Json.obj("conference" -> c, "data" -> d)}
          ))
        }
      }.getOrElse {
        Future(Ok(MailChimpCampaign.conferenceListNewsletterTest(closingCFPs, incomingConferences, newData).contentHtml.unwrap).as(HTML))
      }
    }
  }
  def testTwitts(date: Option[String]) = Action.async { implicit req =>
    TwittScheduler.getTwitts(date.map(d => DateTime.parse(d, Defaults.dateFormatter)).getOrElse(new DateTime())).map { twitts =>
      Ok(Json.obj("twitts" -> twitts))
    }
  }
  def importFromProd = Action.async { implicit req =>
    if(Utils.isProd()){ throw new IllegalStateException("You can't import data in prod !!!") }
    WS.url("http://saloonapp.herokuapp.com/api/conferences").get().flatMap { response =>
      val conferences = (response.json \ "result").as[List[Conference]]
      ConferenceRepository.importData(conferences).map { res =>
        Ok(Json.obj(
          "nbImported" -> res.n,
          "result" -> conferences
        ))
      }
    }
  }

  private def buildFilter(q: Option[String], before: Option[String], after: Option[String], tags: Option[String], cfp: Option[String], tickets: Option[String], videos: Option[String]): JsObject = {
    def reduce(l: List[Option[JsObject]]): Option[JsObject] = l.flatten.headOption.map(_ => l.flatten.reduceLeft(_ ++ _))
    def parseDate(d: String): Option[DateTime] = Try(DateTime.parse(d, Defaults.dateFormatter)).toOption
    def buildTextFilter(q: Option[String]): Option[JsObject] =
      q.map(_.trim).filter(_.length > 0).map { query =>
        Json.obj("$or" -> List(
          "name", "description", "siteUrl", "videosUrl", "tags",
          "location.name", "location.street", "location.postalCode", "location.locality", "location.country",
          "cfp.siteUrl", "tickets.siteUrl", "social.twitter.account", "social.twitter.hashtag"
        ).map(field => Json.obj(field -> Json.obj("$regex" -> query, "$options" -> "i"))))
      }
    def buildDateFilter(before: Option[String], after: Option[String]): Option[JsObject] =
      reduce(List(
        before.flatMap(parseDate).map(d => Json.obj("$lte" -> d)),
        after.flatMap(parseDate).map(d => Json.obj("$gte" -> d))
      )).map(f => Json.obj("end" -> f))
    def buildTagFilter(tagsOpt: Option[String]): Option[JsObject] =
      tagsOpt
        .map(_.split(",").map(_.trim).filter(_.length > 0))
        .filter(_.length > 0)
        .map(tags => Json.obj("tags" -> Json.obj("$in" ->tags)))
    def buildCfpFilter(cfp: Option[String]): Option[JsObject] = cfp match {
      case Some("on") => Some(Json.obj("cfp.end" -> Json.obj("$gte" -> new DateTime())))
      case _ => None
    }
    def buildTicketsFilter(tickets: Option[String]): Option[JsObject] = tickets match {
      case Some("on") => Some(Json.obj("end" -> Json.obj("$lte" -> new DateTime()), "tickets.siteUrl" -> Json.obj("$exists" -> true)))
      case _ => None
    }
    def buildVideosFilter(videos: Option[String]): Option[JsObject] = videos match {
      case Some("on") => Some(Json.obj("videosUrl" -> Json.obj("$exists" -> true, "$ne" -> JsNull)))
      case _ => None
    }
    val filters = List(
      buildTextFilter(q),
      buildDateFilter(before, after),
      buildTagFilter(tags),
      buildCfpFilter(cfp),
      buildTicketsFilter(tickets),
      buildVideosFilter(videos)
    ).flatten
    if(filters.length == 0) Json.obj()
    else if(filters.length == 1) filters.head
    else Json.obj("$and" -> filters)
  }
}