package common.services

import common.Utils
import java.util.concurrent.TimeUnit
import common.repositories.conference.ConferenceRepository
import conferences.models.Conference
import conferences.services.TwittFactory
import org.joda.time.{DateTimeConstants, DateTime}
import play.api.libs.json.Json
import play.libs.Akka
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import play.api.libs.concurrent.Execution.Implicits._

object Scheduler {
  def init(): Unit = {
    if(Utils.isProd()){
      NewsletterScheduler.init()
      TwittScheduler.init()
    }
  }
}

object NewsletterScheduler {
  def init(): Unit = {
    val now = new DateTime()
    val next = SchedulerHelper.nextWeekDateTime(now, DateTimeConstants.MONDAY, 10)
    play.Logger.info("NewsletterScheduler: next newsletter on "+next)
    Akka.system.scheduler.schedule(Duration(next.getMillis - now.getMillis, TimeUnit.MILLISECONDS), Duration(7, TimeUnit.DAYS))(sendNewsletter)
  }
  def sendNewsletter(): Unit = {
    play.Logger.info("NewsletterScheduler.sendNewsletter()")
    getNewsletterInfos(new DateTime()).map { case (closingCFPs, incomingConferences, newData) =>
      if(closingCFPs.length + incomingConferences.length + newData.length > 0) {
        MailChimpSrv.createAndSendCampaign(MailChimpCampaign.conferenceListNewsletter(closingCFPs, incomingConferences, newData)).map { url =>
          TwitterSrv.twitt(TwittFactory.newsletterSent(url))
          play.Logger.info("newsletter sent")
        }
      }
    }
  }
  def getNewsletterInfos(date: DateTime): Future[(List[Conference], List[Conference], List[(Conference, Map[String, Boolean])])] = {
    val closingCFPsFut = ConferenceRepository.find(Json.obj("cfp.end" -> Json.obj("$gt" -> date, "$lt" -> date.plusDays(14))), Json.obj("cfp.end" -> 1))
    val incomingConferencesFut = ConferenceRepository.find(Json.obj("start" -> Json.obj("$gt" -> date, "$lt" -> date.plusDays(7))), Json.obj("start" -> 1))
    val conferencesFut = ConferenceRepository.find()
    val oldConferencesFut = ConferenceRepository.findInPast(date.minusDays(7))
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

object TwittScheduler {
  def init(): Unit = {
    val now = new DateTime()
    val next = if(SchedulerHelper.isBeforeTime(now, 8)) now.withTime(8, 0, 0, 0) else now.plusDays(1).withTime(8, 0, 0, 0)
    play.Logger.info("TwittScheduler: next twitts on "+next)
    Akka.system.scheduler.schedule(Duration(next.getMillis - now.getMillis, TimeUnit.MILLISECONDS), Duration(1, TimeUnit.DAYS))(sendTwitts)
  }
  def sendTwitts(): Unit = {
    play.Logger.info("TwittScheduler.sendTwitts()")
    getTwitts(new DateTime()).map { twitts =>
      play.Logger.info(if(twitts.length > 0) twitts.length+" twitts à envoyer :" else "aucun twitt à envoyer")
      twitts.map(t => play.Logger.info("  - "+t))
      twitts.map(TwitterSrv.twitt)
    }
  }
  def getTwitts(date: DateTime): Future[List[String]] = {
    val today = date.withTime(0, 0, 0, 0)
    val nearClosingCFPsFut = ConferenceRepository.find(Json.obj("$or" -> Json.arr(
      Json.obj("cfp.end" -> Json.obj("$eq" -> today.plusDays(1))), // cfp closes tomorrow
      Json.obj("cfp.end" -> Json.obj("$eq" -> today.plusDays(3))), // cfp closes in 3 days
      Json.obj("cfp.end" -> Json.obj("$eq" -> today.plusDays(7))), // cfp closes in 1 week
      Json.obj("cfp.end" -> Json.obj("$eq" -> today.plusDays(14))) // cfp closes in 2 weeks
    )))
    val nearStartingConfsFut = ConferenceRepository.find(Json.obj("$or" -> Json.arr(
      Json.obj("start" -> Json.obj("$eq" -> today)), // conf starts today
      Json.obj("start" -> Json.obj("$eq" -> today.plusDays(1))), // conf starts tomorrow
      Json.obj("start" -> Json.obj("$eq" -> today.plusDays(7))) // conf starts in 1 week
    )))
    for {
      nearClosingCFPs <- nearClosingCFPsFut
      nearStartingConfs <- nearStartingConfsFut
    } yield {
      val twitts = nearClosingCFPs.map { c =>
        (TwittFactory.closingCFP(c, today), c.cfp.get.end.getMillis)
      } ++ nearStartingConfs.map { c =>
        (TwittFactory.startingConference(c, today), c.start.getMillis)
      }
      twitts.sortBy(_._2).map(_._1)
    }
  }
}

object SchedulerHelper {
  def nextWeekDateTime(date: DateTime, weekDay: Int, hour: Int, minutes: Int = 0, seconds: Int = 0): DateTime = {
    if(date.getDayOfWeek == weekDay && SchedulerHelper.isBeforeTime(date, hour, minutes, seconds)) date.withTime(hour, minutes, seconds, 0)
    else SchedulerHelper.nextDayOfWeek(date, weekDay).withTime(hour, minutes, seconds, 0)
  }
  def nextDayOfWeek(date: DateTime, weekDay: Int): DateTime = date.plusDays((7 + weekDay - date.getDayOfWeek - 1) % 7 + 1)
  def isBeforeTime(date: DateTime, hours: Int, minutes: Int = 0, seconds: Int = 0): Boolean = date.isBefore(date.withTime(hours, minutes, seconds, 0))
  def displayDuration(millis: Long): String = {
    val secs = millis/1000
    val millisRes = millis-(secs*1000)
    val mins = secs/60
    val secsRes = secs-(mins*60)
    val hours = mins/60
    val minsRes = mins-(hours*60)
    val days = hours/24
    val hoursRes = hours-(days*24)
    s"$days days $hoursRes hours $minsRes mins $secsRes secs $millisRes millis"
  }
}