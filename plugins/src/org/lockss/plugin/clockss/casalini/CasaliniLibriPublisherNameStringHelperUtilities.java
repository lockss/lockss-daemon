package org.lockss.plugin.clockss.casalini;

import org.lockss.util.Logger;
import org.lockss.util.NumberUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CasaliniLibriPublisherNameStringHelperUtilities {

    private static final Logger log = Logger.getLogger(CasaliniLibriPublisherNameStringHelperUtilities.class);
    
    public static String matchiPublishNamer(String originalDateString) {

        Map<String,String> PublisherNameShortcutMap = new HashMap<String,String>();

        // From the excel sheet publisher sent
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("21 Editore"),"21EDIT");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Accademia di Romania"),"ROMANIA");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Accademia University Press"),"AUP");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Agorà & Co."),"AGORA");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("AIB - Associazione Italiana Biblioteche"),"AIB");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Alpes Italia"),"ALPES");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Altralinea edizioni"),"ALTRALIN");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Aluvión Editorial"),"ALUVION");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Anthropos Editorial"),"ANTHROP");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Archaeopress Publishing"),"ARCHAEO");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Artemide"),"ARTEMIDE");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Associazione di studi storici Elio Conti"),"ASSTOR");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Biblioteca dei Leoni"),"BIBLEONI");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Bononia University Press"),"BUP");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Bookstones"),"BOOKSTON");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Cadmo"),"CADMO");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Casalini Libri"),"CASA");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("CPL - Centro Primo Levi"),"CPL");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Di che cibo 6?"),"CIBO");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Diderotiana Editrice"),"DIDEROT");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("École française d'Athènes"),"EFA");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("EdiSud Salerno"),"EDISUD");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Editore XY.IT"),"XYIT");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Editorial Comares"),"COMARES");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Editorial Reus"),"REUS");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Editrice Bibliografica"),"GRAFICA");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Edizioni Clichy"),"CLICHY");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Edizioni dell'Ateneo"),"ATENEO");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Edizioni Epoké"),"EPOKE");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Edizioni Pendragon"),"PENDRA");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Edizioni Qiqajon"),"QIQAJON");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Edizioni Quasar"),"QUASAR");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Edizioni Studium"),"STUDIUM");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("EGEA"),"EGEA");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("ETS"),"ETS");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("EUNSA - Ediciones Universidad de Navarra"),"EUNSA");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Eurilink University Press"),"EURI");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Fabrizio Serra Editore"),"SERRA");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Franco Angeli"),"FRANCOA");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Genova University Press"),"GUP");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Giannini Editore"),"GIAN");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Giappichelli Editore"),"GIAPPI");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Giardini Editori e Stampatori in Pisa"),"GIARDI");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Gruppo Editoriale Internazionale"),"GEI");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Guida Editori"),"GUIDA");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Herder Editorial"),"HERDER");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Hoepli"),"HOEPLI");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("If Press"),"IFPRESS");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("IFAC - Istituto di Fisica Applicata Nello Carrara"),"IFAC");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Il Calamo"),"CALAMO");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Il Lavoro Editoriale"),"LAVORO");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Il Poligrafo casa editrice"),"POLIGR");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Infinito edizioni"),"INFINITO");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Inschibboleth edizioni"),"INSCHIB");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Istituti Editoriali e Poligrafici Internazionali"),"IEPI");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Krill Books"),"KRILL");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("La Ergástula"),"ERGAS");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("La Vita Felice"),"VITAF");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("L'asino d'oro edizioni"),"ASINO");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Ledizioni"),"LEDIZ");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Leo S. Olschki"),"OLSC");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Leone Editore"),"LEONE");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("L'Erma di Bretschneider"),"ERMA");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("L'Harmattan"),"HARMA");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Licosia Edizioni"),"LICO");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("LIM - Libreria Musicale Italiana"),"LIM");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Loffredo"),"LOFFR");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Mandragora"),"MANDRA");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Marco Saya Edizioni"),"SAYA");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Mardaga"),"MARDAGA");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Metauro"),"METAU");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Mimesis Edizioni"),"MIMESIS");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Morcelliana"),"MORCEL");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Morlacchi Editore"),"MORLA");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Nardini editore"),"NARDINI");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("New Digital Frontiers"),"NDF");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Nicomp"),"NICOMP");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Officina Libraria"),"OFFICINA");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Orthotes Editrice"),"ORTHO");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Paolo Loffredo Editore"),"INIZIAT");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Paris Expérimental"),"PARISEX");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Passigli"),"PASSIGLI");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Pàtron Editore"),"PATRON");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Plaza y Valdés Editores"),"PLAZA");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("PM edizioni"),"PMEDIZ");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Prospettiva edizioni"),"PROSP");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Rosenberg & Sellier"),"ROSENB");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Settegiorni Editore"),"SETTEGI");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Settenove edizioni"),"SETTENO");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Sillabe"),"SILLABE");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("SISMEL - Edizioni del Galluzzo"),"SISMEL");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Sovera Edizioni"),"SOVERA");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Stilo Editrice"),"STILO");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Storia e Letteratura"),"SEL");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("TAB edizioni"),"TAB");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Tangram Edizioni Scientifiche"),"TANGRAM");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Tra le righe libri"),"TRALERIG");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Trama Editorial"),"TRAMA");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Urbaniana University Press"),"URBAN");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Visor Libros"),"VISOR");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Vita e Pensiero"),"VITAE");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Zanichelli Editore"),"ZANI");


        // From 2016
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Centro per la filosofia italiana"),"CADMO");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("The Wolfsonian Foundation"),"CADMO");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Amalthea"),"CADMO");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Jaca book"),"CLUEB");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Dipartimento di filosofia Università di Bologna"),"CLUEB");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Petite plaisance"),"CLUEB");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Eum"),"CLUEB");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("[s.n.]"),"CLUEB");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Regione Emilia-Romagna"),"CLUEB");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Ministero per i beni e le attività culturali Direzione generale per gli archivi"),"CLUEB");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Faenza editrice"),"CLUEB");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Università La Sapienza"),"CLUEB");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Uranoscopo"),"CLUEB");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Giardini"),"GIARDI");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Università degli studi di Macerata"),"IEPI");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Antenore"),"IEPI");

        // From 2020
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("AIB"),"AIB");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Alpes"),"ALPES");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Altralinea"),"ALTRALIN");
        ///////PublisherNameShortcutMap.put("Antenore :"),"2020");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Anthropos"),"ANTHROP");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Associazione italiana biblioteche"),"AIB");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("BIBLIOGRAFICA"),null);
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("CPL editions"),"???");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Casalini"),"CASA");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Clichy"),"CLICHY");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Clueb"),"CLUEB");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Comares"),"COMARES");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Comune di Falconara Marittima"),null);
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("EDITRICE BIBLIOGRAFIA"),null);
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("EUNSA"),"EUNSA");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Editore Ulrico Hoepli"),null);
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Edizione di Storia e Letteratura"),null);
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Edizioni ETS"),null);
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Edizioni Storia e Letteratura"),null);
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Edizioni del Galluzzo per la Fondazione Ezio Franceschini"),null);
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Edizioni di storia e letteratura"),null);
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Eurilink"),"EURI");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("F. Angeli"),null);
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("F. Serra"),null);
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Fabrizio Serra"),"SERRA");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Fondazione Ignazio Mormino del Banco di Sicilia  ;"),null);
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("FrancoAngeli"),"FRANCOA");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("G. Giappichelli"),"GIAPPI");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("G. Giappichelli Editore"),"GIAPPI");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Giannini"),"GIAN");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Giappichelli"),"GIAPPI");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Giardini editori e stampatori"),"GIARDI");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Guida"),"GUIDA");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Herder"),"HERDER");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Hoepli"),"HOEPLI");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Il poligrafo"),"POLIGR");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("InSchibboleth"),"INSCHIB");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Infinito"),null);
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("L'asino d'oro edizioni"),null);
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("L.S. Olschki"),null);
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Latium"),null);
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Ledizioni LediPublishing"),null);
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Leo S. Olschki S. A. éditeur"),"OLSC");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Leo S. Olschki editore"),"OLSC");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Leone"),"LEONE");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Leone editore"),"LEONE");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Libreria musicale italiana"),"LIM");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Licosia"),"LICO");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Loffredo Editore"),null);
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Matauro"),null);
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Mimesis"),null);
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Morlacchi"),"MORLA");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Nardini"),"NARDINI");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("New Digital Press"),null);
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Orthotes"),null);
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("PM"),null);
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Paolo Loffredo"),null);
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Paolo Loffredo iniziative editoriali"),null);
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Partagées"),null);
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Patron"),"PATRON");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Pàtron"),"PATRON");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Pendragon"),null);
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Pesaro"),null);
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Plaza y Valdés"),"PLAZA");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Prospettiva"),"PROSP");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Qiqajon"),"QIQAJON");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Qiqajon - Comunità di Bose"),null);
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Quasar"),"QUASAR");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("ROSENBERG & SELLER"),null);
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Reus"),"REUS");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Rosenberg Sellier"),null);
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("SISMEL"),"SISMEL");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("SISMEL edizioni del Galluzzo"),null);
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("SeL"),"SEL");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Settegiorni"),"SETTEGI");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Settenove"),"SETTENO");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Sillabe"),"SILLABE");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Sovera edizioni"),"SOVERA");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Stilo"),"STILO");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Stilo Editrice"),"STILO");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Storia e letteratura"),"SEL");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Studium"),"STUDIUM");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("TAB"),"TAB");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Tangram"),null);
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Tra le righe"),"TRALERIG");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("U. Hoepli"),null);
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Ulrico Hoepli"),null);
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("V&P strumenti"),null);
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("V&P università"),null);
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Vita e Pensiero Università"),null);
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("XY.IT"),"XYIT");
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Zanichelli"),null);
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Zanichelli"),null);
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("Zanichelli[2009]"),null);
       PublisherNameShortcutMap.put(CasaliniLibriPublisherNameStringHelperUtilities.cleanupKey("L'Erma\" di Bretschneider"),null);

        return  PublisherNameShortcutMap.get(originalDateString);

    }

    public static String cleanupKey(String originalDateString) {
        String publisherCleanName = originalDateString.replaceAll(
                "[^a-zA-Z0-9&]", "").toLowerCase();

        log.debug("-------originalDateString = " + originalDateString + ", publisherCleanName = " + publisherCleanName);

        return  publisherCleanName;
    }

}
