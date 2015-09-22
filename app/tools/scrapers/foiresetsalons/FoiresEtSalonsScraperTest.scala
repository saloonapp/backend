package tools.scrapers.foiresetsalons

import tools.utils.ScraperTest
import tools.utils.Scraper
import tools.scrapers.foiresetsalons.models.FoiresEtSalonsEvent
import tools.scrapers.foiresetsalons.models.FoiresEtSalonsAddress
import tools.scrapers.foiresetsalons.models.FoiresEtSalonsStats
import tools.scrapers.foiresetsalons.models.FoiresEtSalonsOrga
import play.api.libs.json.Json

object FoiresEtSalonsScraperTest extends ScraperTest[FoiresEtSalonsEvent] {
  private val emptyEvent = FoiresEtSalonsEvent("", FoiresEtSalonsAddress("", "", ""), "", List(), None, None, List(), List(), FoiresEtSalonsStats(0, 0, 0, 0, ""), FoiresEtSalonsOrga("", "", FoiresEtSalonsAddress("", "", ""), "", ""), "")
  val scraper: Scraper[FoiresEtSalonsEvent] = FoiresEtSalonsScraper
  val tests: Map[String, FoiresEtSalonsEvent] = Map(
    "https://www.foiresetsalons.entreprises.gouv.fr/fichemanif.php?manif=16426&decl=956" -> Json.parse("""{"name":"SLICK Salon d'art contemporain (SLICK)","address":{"name":"PORT DES CHAMPS ELYSEES","street":"Pavillon Eiffel - Pavillon Concorde","city":"75008 PARIS"},"category":"Salon professionnel","access":["Accès payant","Carte d'invitation"],"start":1445378400000,"end":1445724000000,"sectors":["Foire-exposition"],"products":["ART CONTEMPORAIN"],"stats":{"area":1100,"venues":5500,"exponents":35,"visitors":17000,"certified":"EXPOCERT"},"orga":{"name":"SLICK PARIS","sigle":"","address":{"name":"","street":"40 RUE DE RICHELIEU","city":"75001 PARIS"},"phone":"06 69 74 50 13","site":""},"url":"https://www.foiresetsalons.entreprises.gouv.fr/fichemanif.php?manif=16426&decl=956"}""").asOpt[FoiresEtSalonsEvent].getOrElse(emptyEvent),
    "https://www.foiresetsalons.entreprises.gouv.fr/fichemanif.php?manif=16425&decl=47" -> Json.parse("""{"name":"1ER NOEL DE PARI FERMIER","address":{"name":"ESPACE EVENEMENTS DU PARC FLORAL DE PARIS","street":"Esplanade du Château de Vincennes","city":"75012 PARIS"},"category":"Salon ouvert au public","access":["Accès payant","Carte d'invitation"],"start":1450393200000,"end":1450566000000,"sectors":["Agriculture","Horticulture  Sylviculture","Viticulture","Pêche et leurs équipements"],"products":["PRODUITS DU TERROIR"],"stats":{"area":500,"venues":9000,"exponents":70,"visitors":9000,"certified":""},"orga":{"name":"ASSOCIATION NATIONALE CIVAM FERMIER","sigle":"","address":{"name":"","street":"76, RUE D''AGUESSEAU","city":"92100 BOULOGNE BILLANCOURT"},"phone":"09 84 22 12 82","site":"http://www.parifermier.com"},"url":"https://www.foiresetsalons.entreprises.gouv.fr/fichemanif.php?manif=16425&decl=47"}""").asOpt[FoiresEtSalonsEvent].getOrElse(emptyEvent),
    "https://www.foiresetsalons.entreprises.gouv.fr/fichemanif.php?manif=16423&decl=47" -> Json.parse("""{"name":"CONGRES ET SALON DU BATIMENT PASSIF (PASSI'BAT)","address":{"name":"ESPACE EVENEMENTS DU PARC FLORAL DE PARIS","street":"Esplanade du Château de Vincennes","city":"75012 PARIS"},"category":"Salon professionnel","access":["Accès gratuit","Carte d'invitation"],"start":1450306800000,"end":1450393200000,"sectors":["Bâtiment","Travaux publics","Second oeuvre et leurs équipements"],"products":["produits batiment","energie renouvelable","contructeurs","fabricants : solaire thermique","isolation","ventilation ....","bureaux d''études"],"stats":{"area":1980,"venues":3150,"exponents":95,"visitors":2400,"certified":"EXPOCERT"},"orga":{"name":"LA MAISON PASSIVE FRANCE","sigle":"LAMP","address":{"name":"","street":"110 RUE REAUMUR","city":"75002 PARIS"},"phone":"01 42 21 02 61","site":"http://www.passibat.fr"},"url":"https://www.foiresetsalons.entreprises.gouv.fr/fichemanif.php?manif=16423&decl=47"}""").asOpt[FoiresEtSalonsEvent].getOrElse(emptyEvent),
    "https://www.foiresetsalons.entreprises.gouv.fr/fichemanif.php?manif=16166&decl=940" -> Json.parse("""{"name":"PROGICIELS (PROGICIELS)","address":{"name":"","street":"39 route de Thônes","city":"74940 ANNECY LE VIEUX"},"category":"Salon professionnel","access":["Accès gratuit","Carte d'invitation"],"start":1444860000000,"end":1444860000000,"sectors":["Informatique","Télécommunication"],"products":["SALON DES LOGICIELS POUR L''INDUSTRIE et cycle de conférences sur la performance industrielle"],"stats":{"area":1350,"venues":850,"exponents":51,"visitors":900,"certified":"INSTITUT INFORA"},"orga":{"name":"THESAME","sigle":"","address":{"name":"","street":"CS 32444","city":"74041 ANNECY CEDEX"},"phone":"04 50 33 58 21","site":"http://www.thesame-innovation.com"},"url":"https://www.foiresetsalons.entreprises.gouv.fr/fichemanif.php?manif=16166&decl=940"}""").asOpt[FoiresEtSalonsEvent].getOrElse(emptyEvent),
    "https://www.foiresetsalons.entreprises.gouv.fr/fichemanif.php?manif=12889&decl=958" -> Json.parse("""{"name":"NEW ART FAIR (NEW ART FAIR)","address":{"name":"ESPACE PIERRE CARDIN","street":"1 AVE GABRIEL","city":"75008 PARIS"},"category":"Salon professionnel","access":["Accès payant","Carte d'invitation"],"start":1357858800000,"end":1358031600000,"sectors":["Foire-exposition"],"products":["ART CONTEMPORAIN"],"stats":{"area":500,"venues":9100,"exponents":35,"visitors":9000,"certified":""},"orga":{"name":"","sigle":"","address":{"name":"","street":"","city":""},"phone":"","site":""},"url":"https://www.foiresetsalons.entreprises.gouv.fr/fichemanif.php?manif=12889&decl=958"}""").asOpt[FoiresEtSalonsEvent].getOrElse(emptyEvent),
    "https://www.foiresetsalons.entreprises.gouv.fr/fichemanif.php?manif=4422&decl=147" -> Json.parse("""{"name":"FOIRE INTERNATIONALE D'ART CONTEMPORAIN (FIAC)","address":{"name":"","street":"GRAND PALAIS","city":"75008 PARIS"},"category":"Salon professionnel","access":["Accès payant","Carte d'invitation"],"start":1192485600000,"end":1193004000000,"sectors":["Arts","Artisanat d'art","Cadeaux et leurs équipements"],"products":[],"stats":{"area":7651,"venues":64207,"exponents":194,"visitors":0,"certified":"OJS"},"orga":{"name":"REED EXPOSITIONS France","sigle":"REF","address":{"name":"11, rue du Colonel Pierre Avia","street":"BP 571","city":"75726 PARIS"},"phone":"01 41 90 47 99","site":""},"url":"https://www.foiresetsalons.entreprises.gouv.fr/fichemanif.php?manif=4422&decl=147"}""").asOpt[FoiresEtSalonsEvent].getOrElse(emptyEvent))
}