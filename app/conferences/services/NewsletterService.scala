package conferences.services

import common.{Config, Utils}
import common.repositories.conference.ConferenceRepository
import common.services.{TwitterSrv, MailChimpCampaign, MailChimpSrv}
import conferences.models.Conference
import org.joda.time.{LocalDate, DateTime}
import play.api.libs.json.Json
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object NewsletterService {
  def sendNewsletter(): Future[Boolean] = {
    if(!Config.Application.isProd){ throw new IllegalAccessException("Method sendNewsletter should be called only on 'prod' environment !") }
    play.Logger.info("NewsletterService.sendNewsletter called")
    getNewsletterInfos(new LocalDate()).flatMap { case (closingCFPs, incomingConferences, newData) =>
      if(closingCFPs.length + incomingConferences.length + newData.length > 0) {
        MailChimpSrv.createAndSendCampaign(MailChimpCampaign.conferenceListNewsletter(closingCFPs, incomingConferences, newData)).map { url =>
          TwitterSrv.sendTwitt(TwittFactory.newsletterSent(url))
          play.Logger.info("NewsletterService.sendNewsletter: newsletter sent")
          true
        }
      } else {
        Future(false)
      }
    }
  }
  def getNewsletterInfos(date: LocalDate): Future[(List[Conference], List[Conference], List[(Conference, Map[String, Boolean])])] = {
    val closingCFPsFut = ConferenceRepository.find(Json.obj("cfp.end" -> Json.obj("$gt" -> date, "$lt" -> date.plusDays(14))), Json.obj("cfp.end" -> 1))
    val incomingConferencesFut = ConferenceRepository.find(Json.obj("start" -> Json.obj("$gt" -> date, "$lt" -> date.plusDays(7))), Json.obj("start" -> 1))
    val conferencesFut = ConferenceRepository.find()
    val oldConferencesFut = ConferenceRepository.findInPast(date.minusDays(7).toDateTimeAtCurrentTime)
    for {
      closingCFPs <- closingCFPsFut
      incomingConferences <- incomingConferencesFut
      conferences <- conferencesFut
      oldConferences <- oldConferencesFut
    } yield {
      val newData = conferences.filterNot(c => closingCFPs.find(_.id == c.id).isDefined || incomingConferences.find(_.id == c.id).isDefined).map { c =>
        (c, Map(
          "created" -> oldConferences.find(_.id == c.id).isEmpty,
          "videos" -> (c.videosUrl.isDefined && oldConferences.find(_.id == c.id).map(old => old.videosUrl != c.videosUrl).getOrElse(true)),
          "cfp" -> (c.cfp.isDefined && oldConferences.find(_.id == c.id).map(old => old.cfp != c.cfp).getOrElse(true))
        ))
      }.filter(_._2.map(_._2).foldLeft(false)(_ || _)) // has at least one 'true'
      (closingCFPs, incomingConferences, newData)
    }
  }
}
