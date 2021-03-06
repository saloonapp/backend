package backend.forms

import common.Config
import common.models.utils.tStringConstraints._
import common.models.values.typed._
import common.models.values.Address
import common.models.values.Link
import common.models.event.Event
import common.models.event.EventId
import common.models.event.EventImages
import common.models.event.EventInfo
import common.models.event.EventInfoSocial
import common.models.event.EventInfoSocialTwitter
import common.models.event.EventEmail
import common.models.event.EventConfig
import common.models.event.EventMeta
import common.models.user.OrganizationId
import org.joda.time.DateTime
import play.api.data.Forms._
import play.api.libs.json._

case class EventCreateData(
  ownerId: OrganizationId,
  name: FullName,
  categories: List[String],
  start: Option[DateTime],
  end: Option[DateTime],
  address: Address,
  website: WebsiteUrl,
  price: Link,
  descriptionHTML: TextHTML,
  logo: ImageUrl,
  landing: ImageUrl,
  twitterHashtag: Option[String],
  twitterAccount: Option[String])
object EventCreateData {
  val fields = mapping(
    "ownerId" -> of[OrganizationId].verifying(nonEmpty),
    "name" -> of[FullName].verifying(nonEmpty),
    "categories" -> list(text),
    "start" -> optional(jodaDate(pattern = Config.Application.datetimeFormat)),
    "end" -> optional(jodaDate(pattern = Config.Application.datetimeFormat)),
    "address" -> Address.fields,
    "website" -> of[WebsiteUrl],
    "price" -> Link.fields,
    "descriptionHTML" -> of[TextHTML],
    "logo" -> of[ImageUrl],
    "landing" -> of[ImageUrl],
    "twitterHashtag" -> optional(text),
    "twitterAccount" -> optional(text))(EventCreateData.apply)(EventCreateData.unapply)

  def toMeta(d: EventCreateData): EventMeta = EventMeta(d.categories, EventStatus.draft, None, None, new DateTime(), new DateTime())
  def toConfig(d: EventCreateData): EventConfig = EventConfig(None, Map(), None)
  def toSocial(d: EventCreateData): EventInfoSocial = EventInfoSocial(EventInfoSocialTwitter(d.twitterHashtag, d.twitterAccount))
  def toInfo(d: EventCreateData): EventInfo = EventInfo(d.website, d.start, d.end, d.address, d.price, toSocial(d))
  def toImages(d: EventCreateData): EventImages = EventImages(d.logo, d.landing)
  def toModel(d: EventCreateData): Event = Event(EventId.generate(), d.ownerId, d.name, d.descriptionHTML.toPlainText, d.descriptionHTML, toImages(d), toInfo(d), EventEmail(None), toConfig(d), toMeta(d))
  def fromModel(d: Event): EventCreateData = EventCreateData(d.ownerId, d.name, d.meta.categories, d.info.start, d.info.end, d.info.address, d.info.website, d.info.price, d.descriptionHTML, d.images.logo, d.images.landing, d.info.social.twitter.hashtag, d.info.social.twitter.account)
  def merge(m: Event, d: EventCreateData): Event = m.copy(ownerId = d.ownerId, name = d.name, description = d.descriptionHTML.toPlainText, descriptionHTML = d.descriptionHTML, images = toImages(d), info = toInfo(d), meta = m.meta.copy(categories = d.categories, updated = new DateTime()))
}
