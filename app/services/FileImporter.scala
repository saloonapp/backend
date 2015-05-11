package services

import models.ImportConfig
import models.Event
import models.Session
import models.Exponent
import infrastructure.repository.EventRepository
import infrastructure.repository.ExponentRepository
import infrastructure.repository.SessionRepository
import org.joda.time.format.DateTimeFormat
import java.io.Reader
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import com.github.tototoshi.csv._
import common.Defaults.csvFormat

object FileImporter {
  val datePattern = "dd/MM/yyyy HH:mm"
  val dateFormat = DateTimeFormat.forPattern(datePattern)

  def importEvents(importedFile: Reader, cfg: ImportConfig): Future[Int] = {
    val lines = CSVReader.open(importedFile).allWithHeaders()
    val elts = lines.map { _.map { case (key, value) => (key, value.replace("\\r", "\r").replace("\\n", "\n")) } }.map { line => Event.fromMap(line) }.flatten

    if (cfg.shouldClean) {
      EventRepository.drop().flatMap { dropped =>
        EventRepository.bulkInsert(elts)
      }
    } else {
      EventRepository.bulkInsert(elts)
    }
  }

  def importExponents(importedFile: Reader, cfg: ImportConfig, eventId: String): Future[Int] = {
    val lines = CSVReader.open(importedFile).allWithHeaders()
    val elts = lines.map { _.map { case (key, value) => (key, value.replace("\\r", "\r").replace("\\n", "\n")) } }.map { line => Exponent.fromMap(line, eventId) }.flatten

    if (cfg.shouldClean) {
      ExponentRepository.deleteByEvent(eventId).flatMap { dropped =>
        ExponentRepository.bulkInsert(elts)
      }
    } else {
      ExponentRepository.bulkInsert(elts)
    }
  }

  def importSessions(importedFile: Reader, cfg: ImportConfig, eventId: String): Future[Int] = {
    val lines = CSVReader.open(importedFile).allWithHeaders()
    val elts = lines.map { _.map { case (key, value) => (key, value.replace("\\r", "\r").replace("\\n", "\n")) } }.map { line => Session.fromMap(line, eventId) }.flatten

    if (cfg.shouldClean) {
      SessionRepository.deleteByEvent(eventId).flatMap { dropped =>
        SessionRepository.bulkInsert(elts)
      }
    } else {
      SessionRepository.bulkInsert(elts)
    }
  }
}
