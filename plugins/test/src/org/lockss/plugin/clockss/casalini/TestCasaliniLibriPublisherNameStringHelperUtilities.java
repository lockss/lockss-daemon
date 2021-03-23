package org.lockss.plugin.clockss.casalini;

import org.lockss.plugin.clockss.MetadataStringHelperUtilities;
import org.lockss.plugin.clockss.TestMetadataStringHelperUtilities;
import org.lockss.plugin.clockss.MetadataStringHelperUtilities;
import org.lockss.test.LockssTestCase;
import org.lockss.util.Logger;

import java.util.ArrayList;
import java.util.List;
import static org.lockss.plugin.clockss.casalini.CasaliniLibriPublisherNameStringHelperUtilities.*;

import static org.lockss.plugin.clockss.casalini.CasaliniLibriPublisherNameStringHelperUtilities.matchPublisherName;

public class TestCasaliniLibriPublisherNameStringHelperUtilities extends LockssTestCase {

    private static final Logger log = Logger.getLogger(TestMetadataStringHelperUtilities.class);

    /*
    020_metadata.txt:  publisher: "L'Erma" di Bretschneider  ;
    2020_metadata.txt:  publisher: "L'Erma" di Bretschneider
    2020_metadata.txt:  publisher: AIB
    2020_metadata.txt:  publisher: Accademia University Press
    2020_metadata.txt:  publisher: Accademia di Romania
    2020_metadata.txt:  publisher: Agorà & Co
    2020_metadata.txt:  publisher: Alpes
    2020_metadata.txt:  publisher: Alpes Italia
    2020_metadata.txt:  publisher: Altralinea
    2020_metadata.txt:  publisher: Altralinea edizioni
    2020_metadata.txt:  publisher: Aluvión editorial
    2020_metadata.txt:  publisher: Amalthea
    2020_metadata.txt:  publisher: Antenore :
    2020_metadata.txt:  publisher: Anthropos 
    2020_metadata.txt:  publisher: Anthropos  ;
    2020_metadata.txt:  publisher: Anthropos
    2020_metadata.txt:  publisher: Anthropos Editorial
    2020_metadata.txt:  publisher: Associazione di studi storici Elio Conti
    2020_metadata.txt:  publisher: Associazione italiana biblioteche
    2020_metadata.txt:  publisher: BIBLIOGRAFICA
    2020_metadata.txt:  publisher: Bibliografica
    2020_metadata.txt:  publisher: Biblioteca dei Leoni
    2020_metadata.txt:  publisher: Bononia University Press
    2020_metadata.txt:  publisher: CELID
    2020_metadata.txt:  publisher: CLUEB  ;
    2020_metadata.txt:  publisher: CLUEB
    2020_metadata.txt:  publisher: CLUEB :
    2020_metadata.txt:  publisher: CPL editions
    2020_metadata.txt:  publisher: Cadmo  ;
    2020_metadata.txt:  publisher: Cadmo
    2020_metadata.txt:  publisher: Casalini
    2020_metadata.txt:  publisher: Casalini Libri
    2020_metadata.txt:  publisher: Casalini libri
    2020_metadata.txt:  publisher: Celid
    2020_metadata.txt:  publisher: Centro per la filosofia italiana :
    2020_metadata.txt:  publisher: Clichy
    2020_metadata.txt:  publisher: Clueb
    2020_metadata.txt:  publisher: Comares
    2020_metadata.txt:  publisher: Comune di Falconara Marittima
    2020_metadata.txt:  publisher: Di che cibo 6?
    2020_metadata.txt:  publisher: Diderotiana Editrice
    2020_metadata.txt:  publisher: Diderotiana editrice
    2020_metadata.txt:  publisher: Dipartimento di filosofia Università di Bologna
    2020_metadata.txt:  publisher: EDITRICE BIBLIOGRAFIA
    2020_metadata.txt:  publisher: EGEA
    2020_metadata.txt:  publisher: EGEA :
    2020_metadata.txt:  publisher: ETS
    2020_metadata.txt:  publisher: EUNSA
    2020_metadata.txt:  publisher: École française d'Athènes
    2020_metadata.txt:  publisher: Editore Ulrico Hoepli
    2020_metadata.txt:  publisher: Editore XY.IT
    2020_metadata.txt:  publisher: Editorial Comares
    2020_metadata.txt:  publisher: Editrice Bibliografica
    2020_metadata.txt:  publisher: Edizione di Storia e Letteratura
    2020_metadata.txt:  publisher: Edizioni Clichy
    2020_metadata.txt:  publisher: Edizioni ETS
    2020_metadata.txt:  publisher: Edizioni Epoké
    2020_metadata.txt:  publisher: Edizioni Quasar
    2020_metadata.txt:  publisher: Edizioni Storia e Letteratura
    2020_metadata.txt:  publisher: Edizioni Studium
    2020_metadata.txt:  publisher: Edizioni del Galluzzo per la Fondazione Ezio Franceschini
    2020_metadata.txt:  publisher: Edizioni dell'Ateneo
    2020_metadata.txt:  publisher: Edizioni di storia e letteratura  ;
    2020_metadata.txt:  publisher: Edizioni di storia e letteratura
    2020_metadata.txt:  publisher: Edizioni di storia e letteratura :
    2020_metadata.txt:  publisher: Egea
    2020_metadata.txt:  publisher: Eum
    2020_metadata.txt:  publisher: Eurilink
    2020_metadata.txt:  publisher: Eurilink University Press
    2020_metadata.txt:  publisher: F. Angeli
    2020_metadata.txt:  publisher: F. Serra
    2020_metadata.txt:  publisher: F.Angeli
    2020_metadata.txt:  publisher: Fabrizio Serra
    2020_metadata.txt:  publisher: Fabrizio Serra Editore
    2020_metadata.txt:  publisher: Fabrizio Serra editore
    2020_metadata.txt:  publisher: Faenza editrice
    2020_metadata.txt:  publisher: Fondazione Ignazio Mormino del Banco di Sicilia  ;
    2020_metadata.txt:  publisher: Franco Angeli
    2020_metadata.txt:  publisher: FrancoAngeli
    2020_metadata.txt:  publisher: G. Giappichelli
    2020_metadata.txt:  publisher: G. Giappichelli Editore
    2020_metadata.txt:  publisher: G. Giappichelli editore
    2020_metadata.txt:  publisher: Genova University Press
    2020_metadata.txt:  publisher: Giannini
    2020_metadata.txt:  publisher: Giannini Editore
    2020_metadata.txt:  publisher: Giappichelli
    2020_metadata.txt:  publisher: Giappichelli editore
    2020_metadata.txt:  publisher: Giardini
    2020_metadata.txt:  publisher: Giardini editori e stampatori
    2020_metadata.txt:  publisher: Gruppo editoriale internazionale
    2020_metadata.txt:  publisher: Guida
    2020_metadata.txt:  publisher: Guida editori
    2020_metadata.txt:  publisher: Herder
    2020_metadata.txt:  publisher: Hoepli
    2020_metadata.txt:  publisher: IF Press
    2020_metadata.txt:  publisher: IFAC - Istituto di Fisica Applicata Nello Carrara
    2020_metadata.txt:  publisher: If Press
    2020_metadata.txt:  publisher: If press
    2020_metadata.txt:  publisher: Il Calamo
    2020_metadata.txt:  publisher: Il Lavoro Editoriale
    2020_metadata.txt:  publisher: Il calamo :
    2020_metadata.txt:  publisher: Il lavoro editoriale
    2020_metadata.txt:  publisher: Il poligrafo
    2020_metadata.txt:  publisher: InSchibboleth
    2020_metadata.txt:  publisher: Infinito
    2020_metadata.txt:  publisher: Infinito edizioni
    2020_metadata.txt:  publisher: Inschibboleth
    2020_metadata.txt:  publisher: Istituti editoriali e poligrafici internazionali  ;
    2020_metadata.txt:  publisher: Istituti editoriali e poligrafici internazionali
    2020_metadata.txt:  publisher: Jaca book  ;
    2020_metadata.txt:  publisher: L'Erma di Bretschneider
    2020_metadata.txt:  publisher: L'asino d'oro
    2020_metadata.txt:  publisher: L'asino d'oro edizioni
    2020_metadata.txt:  publisher: L. S. Olschki
    2020_metadata.txt:  publisher: L.S. Olschki  ;
    2020_metadata.txt:  publisher: L.S. Olschki
    2020_metadata.txt:  publisher: L.S. Olschki :
    2020_metadata.txt:  publisher: La Ergástula
    2020_metadata.txt:  publisher: La vita felice
    2020_metadata.txt:  publisher: Latium
    2020_metadata.txt:  publisher: Ledizioni
    2020_metadata.txt:  publisher: Ledizioni LediPublishing
    2020_metadata.txt:  publisher: Leo S. Olschki
    2020_metadata.txt:  publisher: Leo S. Olschki S. A. éditeur
    2020_metadata.txt:  publisher: Leo S. Olschki editore
    2020_metadata.txt:  publisher: Leone
    2020_metadata.txt:  publisher: Leone editore
    2020_metadata.txt:  publisher: Libreria musicale italiana
    2020_metadata.txt:  publisher: Licosia
    2020_metadata.txt:  publisher: Licosia edizioni
    2020_metadata.txt:  publisher: Loffredo Editore
    2020_metadata.txt:  publisher: Mandragora
    2020_metadata.txt:  publisher: Marco Saya edizioni
    2020_metadata.txt:  publisher: Matauro
    2020_metadata.txt:  publisher: Metauro
    2020_metadata.txt:  publisher: Mimesis
    2020_metadata.txt:  publisher: Ministero per i beni e le attività culturali Direzione generale per gli archivi
    2020_metadata.txt:  publisher: Morcelliana
    2020_metadata.txt:  publisher: Morlacchi
    2020_metadata.txt:  publisher: Nardini
    2020_metadata.txt:  publisher: New Digital Frontiers
    2020_metadata.txt:  publisher: New Digital Press
    2020_metadata.txt:  publisher: Nicomp
    2020_metadata.txt:  publisher: Officina libraria
    2020_metadata.txt:  publisher: Orthotes
    2020_metadata.txt:  publisher: PM
    2020_metadata.txt:  publisher: PM Edizioni
    2020_metadata.txt:  publisher: PM edizioni
    2020_metadata.txt:  publisher: Paolo Loffredo
    2020_metadata.txt:  publisher: Paolo Loffredo iniziative editoriali
    2020_metadata.txt:  publisher: Partagées
    2020_metadata.txt:  publisher: Passigli
    2020_metadata.txt:  publisher: Patron
    2020_metadata.txt:  publisher: Pàtron
    2020_metadata.txt:  publisher: Pendragon
    2020_metadata.txt:  publisher: Pesaro
    2020_metadata.txt:  publisher: Petite plaisance  ;
    2020_metadata.txt:  publisher: Plaza y Valdés
    2020_metadata.txt:  publisher: Plaza y Valdés Editores
    2020_metadata.txt:  publisher: Prospettiva
    2020_metadata.txt:  publisher: Prospettiva edizioni
    2020_metadata.txt:  publisher: Qiqajon
    2020_metadata.txt:  publisher: Qiqajon - Comunità di Bose
    2020_metadata.txt:  publisher: Quasar
    2020_metadata.txt:  publisher: Quasar :
    2020_metadata.txt:  publisher: ROSENBERG & SELLER
    2020_metadata.txt:  publisher: Regione Emilia-Romagna :
    2020_metadata.txt:  publisher: Reus
    2020_metadata.txt:  publisher: Rosenberg & Sellier
    2020_metadata.txt:  publisher: Rosenberg Sellier
    2020_metadata.txt:  publisher: SISMEL :
    2020_metadata.txt:  publisher: SISMEL edizioni del Galluzzo
    2020_metadata.txt:  publisher: SeL
    2020_metadata.txt:  publisher: Settegiorni
    2020_metadata.txt:  publisher: Settenove
    2020_metadata.txt:  publisher: Sillabe
    2020_metadata.txt:  publisher: Sovera edizioni
    2020_metadata.txt:  publisher: Stilo
    2020_metadata.txt:  publisher: Stilo Editrice
    2020_metadata.txt:  publisher: Stilo editrice
    2020_metadata.txt:  publisher: Storia e letteratura
    2020_metadata.txt:  publisher: Studium
    2020_metadata.txt:  publisher: TAB
    2020_metadata.txt:  publisher: TAB edizioni
    2020_metadata.txt:  publisher: Tangram
    2020_metadata.txt:  publisher: Tangram Edizioni Scientifiche
    2020_metadata.txt:  publisher: Tangram edizioni scientifiche
    2020_metadata.txt:  publisher: The Wolfsonian Foundation  ;
    2020_metadata.txt:  publisher: Tra le righe
    2020_metadata.txt:  publisher: Tra le righe libri
    2020_metadata.txt:  publisher: Trama Editorial
    2020_metadata.txt:  publisher: Trama editorial
    2020_metadata.txt:  publisher: U. Hoepli
    2020_metadata.txt:  publisher: Ulrico Hoepli
    2020_metadata.txt:  publisher: Università La Sapienza
    2020_metadata.txt:  publisher: Università degli studi di Macerata
    2020_metadata.txt:  publisher: Uranoscopo
    2020_metadata.txt:  publisher: Urbaniana University Press
    2020_metadata.txt:  publisher: Urbaniana university press
    2020_metadata.txt:  publisher: V&P strumenti
    2020_metadata.txt:  publisher: V&P università
    2020_metadata.txt:  publisher: Visor Libros
    2020_metadata.txt:  publisher: Vita e Pensiero
    2020_metadata.txt:  publisher: Vita e Pensiero Università
    2020_metadata.txt:  publisher: XY.IT
    2020_metadata.txt:  publisher: Zanichelli 
    2020_metadata.txt:  publisher: Zanichelli
    2020_metadata.txt:  publisher: Zanichelli[2009]
    2020_metadata.txt:  publisher: [s.n.]
    2020_metadata.txt:  publisher: il lavoro editoriale
     */

    public void testcleanupPublisherName() throws Exception {

        List<String> testGoodKey = new ArrayList<>();

        testGoodKey.add("Antenore :");
        testGoodKey.add("Anthropos  ;");
        testGoodKey.add("G. Giappichelli Editore");
        testGoodKey.add("XY.IT");
        testGoodKey.add("[s.n.]");
        testGoodKey.add("Di che cibo 6?");
        testGoodKey.add("G. Giappichelli");
        testGoodKey.add("G. Giappichelli Editore");
        testGoodKey.add("Agorà & Co.");
        testGoodKey.add("CPL - Centro Primo Levi");

        for(String originalStr : testGoodKey) {

            String cleanStr = cleanupKey(originalStr);
            String publisherShortCut = matchPublisherName(cleanStr);

            //log.info("########Good originalStr = " + originalStr + ", cleanStr = " + cleanStr + ", publisherShortCut = " + publisherShortCut);
            assertNotNull(publisherShortCut);
        }
    }

    public void testNullPublisherName() throws Exception {

        List<String> testNullKey = new ArrayList<>();

        testNullKey.add("\"L'Erma\" di Bretschneider  ;");
        testNullKey.add("Zanichelli[2009]");

        for(String originalStr : testNullKey) {

            String cleanStr = CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey(originalStr);
            String publisherShortCut = matchPublisherName(cleanStr);

            //log.info("########BAD originalStr = " + originalStr + ", cleanStr = " + cleanStr + ", publisherShortCut = " + publisherShortCut);

            assertNull(publisherShortCut);
        }
    }
}
