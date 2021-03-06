package common.services

import common.Config
import common.models.FileImportConfig
import common.models.event.Event
import common.models.event.Attendee
import common.models.event.Session
import common.models.event.Exponent
import common.repositories.event.EventRepository
import common.repositories.event.AttendeeRepository
import common.repositories.event.SessionRepository
import common.repositories.event.ExponentRepository
import org.joda.time.format.DateTimeFormat
import scala.util.Try
import java.io.Reader
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import com.github.tototoshi.csv._
import common.Config.Application.csvFormat

object FileImporter {
  val datePattern = Config.Application.datetimeFormat
  val dateFormat = DateTimeFormat.forPattern(datePattern)

  /*def importAttendees(importedFile: Reader, cfg: FileImportConfig, eventId: String): Future[(Int, List[String])] = {
    importData(Attendee.fromMap(eventId), () => AttendeeRepository.deleteByEvent(eventId).map(_.ok), AttendeeRepository.bulkInsert)(importedFile, cfg)
  }

  def importSessions(importedFile: Reader, cfg: FileImportConfig, eventId: String): Future[(Int, List[String])] = {
    importData(Session.fromMap(eventId), () => SessionRepository.deleteByEvent(eventId).map(_.ok), SessionRepository.bulkInsert)(importedFile, cfg)
  }

  def importExponents(importedFile: Reader, cfg: FileImportConfig, eventId: String): Future[(Int, List[String])] = {
    importData(Exponent.fromMap(eventId), () => ExponentRepository.deleteByEvent(eventId).map(_.ok), ExponentRepository.bulkInsert)(importedFile, cfg)
  }*/

  private def importData[T](formatData: Map[String, String] => Try[T], deleteOld: () => Future[Boolean], bulkInsert: List[T] => Future[Int])(importedFile: Reader, cfg: FileImportConfig): Future[(Int, List[String])] = {
    val lines = CSVReader.open(importedFile).allWithHeaders().map { _.map { case (key, value) => (key, value.replace("\\r", "\r").replace("\\n", "\n")) } }
    val results = lines.map { line => formatData(line) }
    val elts = results.filter(_.isSuccess).map(_.get)
    val errors = results.zipWithIndex.filter(_._1.isFailure).map { case (err, i) => err.failed.get.toString() + " on line " + (i + 2) } // editors starts at line 1 and have one header line !

    (if (cfg.shouldClean) { deleteOld() } else { Future(true) }).flatMap { _ =>
      // TODO : perform bulk upsert instead of insert to avoir uuid duplication !!!
      bulkInsert(elts).map { inserted => (inserted, errors) }
    }
  }
}
