/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.clockss.casalini;

import org.lockss.util.Logger;

import java.util.HashMap;
import java.util.Map;

public class CasaliniLibriPublisherNameStringHelperUtilities {

    private static final Logger log = Logger.getLogger(CasaliniLibriPublisherNameStringHelperUtilities.class);

    protected static final Map<String,String> publisherNameShortcutMap;

    protected static final Map<String,String> publisherNameShortcutMap2016;

    static {
      publisherNameShortcutMap = new HashMap<>();
      publisherNameShortcutMap2016 = new HashMap<>();

      // From the excel sheet publisher sent
      publisherNameShortcutMap2016.put(cleanupKey("21 Editore"),"21EDIT");
      publisherNameShortcutMap2016.put(cleanupKey("Accademia di Romania"),"ROMANIA");
      publisherNameShortcutMap2016.put(cleanupKey("Accademia University Press"),"AUP");
      publisherNameShortcutMap2016.put(cleanupKey("Agorà & Co."),"AGORA");
      publisherNameShortcutMap2016.put(cleanupKey("AIB - Associazione Italiana Biblioteche"),"AIB");
      publisherNameShortcutMap2016.put(cleanupKey("Alpes Italia"),"ALPES");
      publisherNameShortcutMap2016.put(cleanupKey("Altralinea edizioni"),"ALTRALIN");
      publisherNameShortcutMap2016.put(cleanupKey("Aluvión Editorial"),"ALUVION");
      publisherNameShortcutMap2016.put(cleanupKey("Anthropos Editorial"),"ANTHROP");
      publisherNameShortcutMap2016.put(cleanupKey("Archaeopress Publishing"),"ARCHAEO");
      publisherNameShortcutMap2016.put(cleanupKey("Artemide"),"ARTEMIDE");
      publisherNameShortcutMap2016.put(cleanupKey("Associazione di studi storici Elio Conti"),"ASSTOR");
      publisherNameShortcutMap2016.put(cleanupKey("Biblioteca dei Leoni"),"BIBLEONI");
      publisherNameShortcutMap2016.put(cleanupKey("Bononia University Press"),"BUP");
      publisherNameShortcutMap2016.put(cleanupKey("Bookstones"),"BOOKSTON");
      publisherNameShortcutMap2016.put(cleanupKey("Cadmo"),"CADMO");
      publisherNameShortcutMap2016.put(cleanupKey("Casalini Libri"),"CASA");
      publisherNameShortcutMap2016.put(cleanupKey("CPL - Centro Primo Levi"),"CPL");
      publisherNameShortcutMap2016.put(cleanupKey("Di che cibo 6?"),"CIBO");
      publisherNameShortcutMap2016.put(cleanupKey("Diderotiana Editrice"),"DIDEROT");
      publisherNameShortcutMap2016.put(cleanupKey("École française d'Athènes"),"EFA");
      publisherNameShortcutMap2016.put(cleanupKey("EdiSud Salerno"),"EDISUD");
      publisherNameShortcutMap2016.put(cleanupKey("Editore XY.IT"),"XYIT");
      publisherNameShortcutMap2016.put(cleanupKey("Editorial Comares"),"COMARES");
      publisherNameShortcutMap2016.put(cleanupKey("Editorial Reus"),"REUS");
      publisherNameShortcutMap2016.put(cleanupKey("Editrice Bibliografica"),"GRAFICA");
      publisherNameShortcutMap2016.put(cleanupKey("Edizioni Clichy"),"CLICHY");
      publisherNameShortcutMap2016.put(cleanupKey("Edizioni dell'Ateneo"),"ATENEO");
      publisherNameShortcutMap2016.put(cleanupKey("Edizioni Epoké"),"EPOKE");
      publisherNameShortcutMap2016.put(cleanupKey("Edizioni Pendragon"),"PENDRA");
      publisherNameShortcutMap2016.put(cleanupKey("Edizioni Qiqajon"),"QIQAJON");
      publisherNameShortcutMap2016.put(cleanupKey("Edizioni Quasar"),"QUASAR");
      publisherNameShortcutMap2016.put(cleanupKey("Edizioni Studium"),"STUDIUM");
      publisherNameShortcutMap2016.put(cleanupKey("EGEA"),"EGEA");
      publisherNameShortcutMap2016.put(cleanupKey("ETS"),"ETS");
      publisherNameShortcutMap2016.put(cleanupKey("EUNSA - Ediciones Universidad de Navarra"),"EUNSA");
      publisherNameShortcutMap2016.put(cleanupKey("Eurilink University Press"),"EURI");
      publisherNameShortcutMap2016.put(cleanupKey("Fabrizio Serra Editore"),"SERRA");
      publisherNameShortcutMap2016.put(cleanupKey("Franco Angeli"),"FRANCOA");
      publisherNameShortcutMap2016.put(cleanupKey("Genova University Press"),"GUP");
      publisherNameShortcutMap2016.put(cleanupKey("Giannini Editore"),"GIAN");
      publisherNameShortcutMap2016.put(cleanupKey("Giappichelli Editore"),"GIAPPI");
      publisherNameShortcutMap2016.put(cleanupKey("Giardini Editori e Stampatori in Pisa"),"GIARDI");
      publisherNameShortcutMap2016.put(cleanupKey("Gruppo Editoriale Internazionale"),"GEI");
      publisherNameShortcutMap2016.put(cleanupKey("Guida Editori"),"GUIDA");
      publisherNameShortcutMap2016.put(cleanupKey("Herder Editorial"),"HERDER");
      publisherNameShortcutMap2016.put(cleanupKey("Hoepli"),"HOEPLI");
      publisherNameShortcutMap2016.put(cleanupKey("If Press"),"IFPRESS");
      publisherNameShortcutMap2016.put(cleanupKey("IFAC - Istituto di Fisica Applicata Nello Carrara"),"IFAC");
      publisherNameShortcutMap2016.put(cleanupKey("Il Calamo"),"CALAMO");
      publisherNameShortcutMap2016.put(cleanupKey("Il Lavoro Editoriale"),"LAVORO");
      publisherNameShortcutMap2016.put(cleanupKey("Il Poligrafo casa editrice"),"POLIGR");
      publisherNameShortcutMap2016.put(cleanupKey("Infinito edizioni"),"INFINITO");
      publisherNameShortcutMap2016.put(cleanupKey("Inschibboleth edizioni"),"INSCHIB");
      publisherNameShortcutMap2016.put(cleanupKey("Istituti Editoriali e Poligrafici Internazionali"),"IEPI");
      publisherNameShortcutMap2016.put(cleanupKey("Krill Books"),"KRILL");
      publisherNameShortcutMap2016.put(cleanupKey("La Ergástula"),"ERGAS");
      publisherNameShortcutMap2016.put(cleanupKey("La Vita Felice"),"VITAF");
      publisherNameShortcutMap2016.put(cleanupKey("L'asino d'oro edizioni"),"ASINO");
      publisherNameShortcutMap2016.put(cleanupKey("Ledizioni"),"LEDIZ");
      publisherNameShortcutMap2016.put(cleanupKey("Leo S. Olschki"),"OLSC");
      publisherNameShortcutMap2016.put(cleanupKey("Leone Editore"),"LEONE");
      publisherNameShortcutMap2016.put(cleanupKey("L'Erma di Bretschneider"),"ERMA");
      publisherNameShortcutMap2016.put(cleanupKey("L'Harmattan"),"HARMA");
      publisherNameShortcutMap2016.put(cleanupKey("Licosia Edizioni"),"LICO");
      publisherNameShortcutMap2016.put(cleanupKey("LIM - Libreria Musicale Italiana"),"LIM");
      publisherNameShortcutMap2016.put(cleanupKey("Loffredo"),"LOFFR");
      publisherNameShortcutMap2016.put(cleanupKey("Mandragora"),"MANDRA");
      publisherNameShortcutMap2016.put(cleanupKey("Marco Saya Edizioni"),"SAYA");
      publisherNameShortcutMap2016.put(cleanupKey("Mardaga"),"MARDAGA");
      publisherNameShortcutMap2016.put(cleanupKey("Metauro"),"METAU");
      publisherNameShortcutMap2016.put(cleanupKey("Mimesis Edizioni"),"MIMESIS");
      publisherNameShortcutMap2016.put(cleanupKey("Morcelliana"),"MORCEL");
      publisherNameShortcutMap2016.put(cleanupKey("Morlacchi Editore"),"MORLA");
      publisherNameShortcutMap2016.put(cleanupKey("Nardini editore"),"NARDINI");
      publisherNameShortcutMap2016.put(cleanupKey("New Digital Frontiers"),"NDF");
      publisherNameShortcutMap2016.put(cleanupKey("Nicomp"),"NICOMP");
      publisherNameShortcutMap2016.put(cleanupKey("Officina Libraria"),"OFFICINA");
      publisherNameShortcutMap2016.put(cleanupKey("Orthotes Editrice"),"ORTHO");
      publisherNameShortcutMap2016.put(cleanupKey("Paolo Loffredo Editore"),"INIZIAT");
      publisherNameShortcutMap2016.put(cleanupKey("Paolo Loffredo"),"INIZIAT");
      publisherNameShortcutMap2016.put(cleanupKey("Paolo Loffredo iniziative editoriali"),"INIZIAT");
      publisherNameShortcutMap2016.put(cleanupKey("Paris Expérimental"),"PARISEX");
      publisherNameShortcutMap2016.put(cleanupKey("Passigli"),"PASSIGLI");
      publisherNameShortcutMap2016.put(cleanupKey("Pàtron Editore"),"PATRON");
      publisherNameShortcutMap2016.put(cleanupKey("Plaza y Valdés Editores"),"PLAZA");
      publisherNameShortcutMap2016.put(cleanupKey("PM edizioni"),"PMEDIZ");
      publisherNameShortcutMap2016.put(cleanupKey("Prospettiva edizioni"),"PROSP");
      publisherNameShortcutMap2016.put(cleanupKey("Rosenberg & Sellier"),"ROSENB");
      publisherNameShortcutMap2016.put(cleanupKey("Settegiorni Editore"),"SETTEGI");
      publisherNameShortcutMap2016.put(cleanupKey("Settenove edizioni"),"SETTENO");
      publisherNameShortcutMap2016.put(cleanupKey("Sillabe"),"SILLABE");
      publisherNameShortcutMap2016.put(cleanupKey("SISMEL - Edizioni del Galluzzo"),"SISMEL");
      publisherNameShortcutMap2016.put(cleanupKey("Sovera Edizioni"),"SOVERA");
      publisherNameShortcutMap2016.put(cleanupKey("Stilo Editrice"),"STILO");
      publisherNameShortcutMap2016.put(cleanupKey("Storia e Letteratura"),"SEL");
      publisherNameShortcutMap2016.put(cleanupKey("TAB edizioni"),"TAB");
      publisherNameShortcutMap2016.put(cleanupKey("Tangram Edizioni Scientifiche"),"TANGRAM");
      publisherNameShortcutMap2016.put(cleanupKey("Tra le righe libri"),"TRALERIG");
      publisherNameShortcutMap2016.put(cleanupKey("Trama Editorial"),"TRAMA");
      publisherNameShortcutMap2016.put(cleanupKey("Urbaniana University Press"),"URBAN");
      publisherNameShortcutMap2016.put(cleanupKey("Visor Libros"),"VISOR");
      publisherNameShortcutMap2016.put(cleanupKey("Vita e Pensiero"),"VITAE");
      publisherNameShortcutMap2016.put(cleanupKey("Zanichelli Editore"),"ZANI");


      // From 2016
      publisherNameShortcutMap2016.put(cleanupKey("Centro per la filosofia italiana"),"CADMO");
      publisherNameShortcutMap2016.put(cleanupKey("The Wolfsonian Foundation"),"CADMO");
      publisherNameShortcutMap2016.put(cleanupKey("Amalthea"),"CADMO");
      publisherNameShortcutMap2016.put(cleanupKey("Jaca book"),"CLUEB");
      publisherNameShortcutMap2016.put(cleanupKey("Dipartimento di filosofia Università di Bologna"),"CLUEB");
      publisherNameShortcutMap2016.put(cleanupKey("Petite plaisance"),"CLUEB");
      publisherNameShortcutMap2016.put(cleanupKey("Eum"),"CLUEB");
      publisherNameShortcutMap2016.put(cleanupKey("[s.n.]"),"CLUEB");
      publisherNameShortcutMap2016.put(cleanupKey("Regione Emilia-Romagna"),"CLUEB");
      publisherNameShortcutMap2016.put(cleanupKey("Ministero per i beni e le attività culturali Direzione generale per gli archivi"),"CLUEB");
      publisherNameShortcutMap2016.put(cleanupKey("Faenza editrice"),"CLUEB");
      publisherNameShortcutMap2016.put(cleanupKey("Università La Sapienza"),"CLUEB");
      publisherNameShortcutMap2016.put(cleanupKey("Uranoscopo"),"CLUEB");
      publisherNameShortcutMap2016.put(cleanupKey("Giardini"),"GIARDI");
      publisherNameShortcutMap2016.put(cleanupKey("Università degli studi di Macerata"),"IEPI");
      publisherNameShortcutMap2016.put(cleanupKey("Antenore"),"IEPI");

      // From 2020
      publisherNameShortcutMap.put(cleanupKey("AIB"),"AIB");
      publisherNameShortcutMap.put(cleanupKey("Alpes"),"ALPES");
      publisherNameShortcutMap.put(cleanupKey("Altralinea"),"ALTRALIN");
      ///////PublisherNameShortcutMap.put("Antenore :"),"2020");
      publisherNameShortcutMap.put(cleanupKey("Anthropos"),"ANTHROP");
      publisherNameShortcutMap.put(cleanupKey("Associazione italiana biblioteche"),"AIB");
      publisherNameShortcutMap.put(cleanupKey("BIBLIOGRAFICA"),"GRAFICA");
      publisherNameShortcutMap.put(cleanupKey("CPL editions"),"???");
      publisherNameShortcutMap.put(cleanupKey("Casalini"),"CASA");
      publisherNameShortcutMap.put(cleanupKey("Clichy"),"CLICHY");
      publisherNameShortcutMap.put(cleanupKey("Clueb"),"CLUEB");
      publisherNameShortcutMap.put(cleanupKey("Comares"),"COMARES");
      publisherNameShortcutMap.put(cleanupKey("Comune di Falconara Marittima"),null);
      publisherNameShortcutMap.put(cleanupKey("EDITRICE BIBLIOGRAFIA"),null);
      publisherNameShortcutMap.put(cleanupKey("EUNSA"),"EUNSA");
      publisherNameShortcutMap.put(cleanupKey("Editore Ulrico Hoepli"),null);
      publisherNameShortcutMap.put(cleanupKey("Edizione di Storia e Letteratura"),null);
      publisherNameShortcutMap.put(cleanupKey("Edizioni ETS"),null);
      publisherNameShortcutMap.put(cleanupKey("Edizioni Storia e Letteratura"),null);
      publisherNameShortcutMap.put(cleanupKey("Edizioni del Galluzzo per la Fondazione Ezio Franceschini"),null);
      publisherNameShortcutMap.put(cleanupKey("Edizioni di storia e letteratura"),null);
      publisherNameShortcutMap.put(cleanupKey("Eurilink"),"EURI");
      publisherNameShortcutMap.put(cleanupKey("F. Angeli"),"FRANCOA");
      publisherNameShortcutMap.put(cleanupKey("F. Serra"),"SERRA");
      publisherNameShortcutMap.put(cleanupKey("Fabrizio Serra"),"SERRA");
      publisherNameShortcutMap.put(cleanupKey("Fondazione Ignazio Mormino del Banco di Sicilia"),null);
      publisherNameShortcutMap.put(cleanupKey("FrancoAngeli"),"FRANCOA");
      publisherNameShortcutMap.put(cleanupKey("G. Giappichelli"),"GIAPPI");
      publisherNameShortcutMap.put(cleanupKey("G. Giappichelli Editore"),"GIAPPI");
      publisherNameShortcutMap.put(cleanupKey("Giannini"),"GIAN");
      publisherNameShortcutMap.put(cleanupKey("Giappichelli"),"GIAPPI");
      publisherNameShortcutMap.put(cleanupKey("Giardini editori e stampatori"),"GIARDI");
      publisherNameShortcutMap.put(cleanupKey("Guida"),"GUIDA");
      publisherNameShortcutMap.put(cleanupKey("Herder"),"HERDER");
      publisherNameShortcutMap.put(cleanupKey("Hoepli"),"HOEPLI");
      publisherNameShortcutMap.put(cleanupKey("Il poligrafo"),"POLIGR");
      publisherNameShortcutMap.put(cleanupKey("InSchibboleth"),"INSCHIB");
      publisherNameShortcutMap.put(cleanupKey("Infinito"),"INFINITO");
      publisherNameShortcutMap.put(cleanupKey("L'asino d'oro edizioni"),"ASINO");
      publisherNameShortcutMap.put(cleanupKey("L.S. Olschki"),null);
      publisherNameShortcutMap.put(cleanupKey("Latium"),null);
      publisherNameShortcutMap.put(cleanupKey("Ledizioni LediPublishing"),null);
      publisherNameShortcutMap.put(cleanupKey("Leo S. Olschki S. A. éditeur"),"OLSC");
      publisherNameShortcutMap.put(cleanupKey("Leo S. Olschki editore"),"OLSC");
      publisherNameShortcutMap.put(cleanupKey("Leone"),"LEONE");
      publisherNameShortcutMap.put(cleanupKey("Leone editore"),"LEONE");
      publisherNameShortcutMap.put(cleanupKey("Libreria musicale italiana"),"LIM");
      publisherNameShortcutMap.put(cleanupKey("Licosia"),"LICO");
      publisherNameShortcutMap.put(cleanupKey("Loffredo Editore"),null);
      publisherNameShortcutMap.put(cleanupKey("Matauro"),null);
      publisherNameShortcutMap.put(cleanupKey("Mimesis"),"MIMESIS");
      publisherNameShortcutMap.put(cleanupKey("Morlacchi"),"MORLA");
      publisherNameShortcutMap.put(cleanupKey("Nardini"),"NARDINI");
      publisherNameShortcutMap.put(cleanupKey("New Digital Press"),null);
      publisherNameShortcutMap.put(cleanupKey("Orthotes"),"ORTHO");
      publisherNameShortcutMap.put(cleanupKey("PM"),"PMEDIZ");
      publisherNameShortcutMap.put(cleanupKey("Paolo Loffredo"),null);
      publisherNameShortcutMap.put(cleanupKey("Partagées"),null);
      publisherNameShortcutMap.put(cleanupKey("Patron"),"PATRON");
      publisherNameShortcutMap.put(cleanupKey("Pàtron"),"PATRON");
      publisherNameShortcutMap.put(cleanupKey("Pendragon"),null);
      publisherNameShortcutMap.put(cleanupKey("Pesaro"),null);
      publisherNameShortcutMap.put(cleanupKey("Prospettiva"),"PROSP");
      publisherNameShortcutMap.put(cleanupKey("Qiqajon"),"QIQAJON");
      publisherNameShortcutMap.put(cleanupKey("Qiqajon - Comunità di Bose"),null);
      publisherNameShortcutMap.put(cleanupKey("Quasar"),"QUASAR");
      publisherNameShortcutMap.put(cleanupKey("Reus"),"REUS");
      publisherNameShortcutMap.put(cleanupKey("SISMEL"),"SISMEL");
      publisherNameShortcutMap.put(cleanupKey("SISMEL edizioni del Galluzzo"),null);
      publisherNameShortcutMap.put(cleanupKey("SeL"),"SEL");
      publisherNameShortcutMap.put(cleanupKey("Settegiorni"),"SETTEGI");
      publisherNameShortcutMap.put(cleanupKey("Settenove"),"SETTENO");
      publisherNameShortcutMap.put(cleanupKey("Sillabe"),"SILLABE");
      publisherNameShortcutMap.put(cleanupKey("Sovera edizioni"),"SOVERA");
      publisherNameShortcutMap.put(cleanupKey("Stilo"),"STILO");
      publisherNameShortcutMap.put(cleanupKey("Stilo Editrice"),"STILO");
      publisherNameShortcutMap.put(cleanupKey("Storia e letteratura"),"SEL");
      publisherNameShortcutMap.put(cleanupKey("Studium"),"STUDIUM");
      publisherNameShortcutMap.put(cleanupKey("TAB"),"TAB");
      publisherNameShortcutMap.put(cleanupKey("Tangram"),"TANGRAM");
      publisherNameShortcutMap.put(cleanupKey("Tra le righe"),"TRALERIG");
      publisherNameShortcutMap.put(cleanupKey("U. Hoepli"),null);
      publisherNameShortcutMap.put(cleanupKey("Ulrico Hoepli"),null);
      publisherNameShortcutMap.put(cleanupKey("V&P strumenti"),null);
      publisherNameShortcutMap.put(cleanupKey("V&P università"),null);
      publisherNameShortcutMap.put(cleanupKey("Vita e Pensiero Università"),null);
      publisherNameShortcutMap.put(cleanupKey("XY.IT"),"XYIT");
      publisherNameShortcutMap.put(cleanupKey("Zanichelli"),null);
      publisherNameShortcutMap.put(cleanupKey("Zanichelli[2009]"),null);
      publisherNameShortcutMap.put(cleanupKey("L'Erma\" di Bretschneider"),null);

      // newly added Mar/23/2021
      publisherNameShortcutMap.put(cleanupKey("Paolo Loffredo"),"INIZIAT");
      publisherNameShortcutMap.put(cleanupKey("ROSENBERG & SELLER"),"ROSENB");
      publisherNameShortcutMap.put(cleanupKey("Rosenberg Sellier"),"ROSENB");
      publisherNameShortcutMap.put(cleanupKey("Plaza y Valdés"),"PLAZA");
      publisherNameShortcutMap.put(cleanupKey("Plaza y Valdés Editores"),"PLAZA");
      publisherNameShortcutMap.put(cleanupKey("Partagées"), null);
      publisherNameShortcutMap.put(cleanupKey("Aluvión editorial"), null);
      publisherNameShortcutMap.put(cleanupKey("La Ergástula"), null);
      publisherNameShortcutMap.put(cleanupKey("École française d'Athènes"), null);
      publisherNameShortcutMap.put(cleanupKey("Università La Sapienza"), null);
      publisherNameShortcutMap.put(cleanupKey("Qiqajon - Comunità di Bose"), null);
      publisherNameShortcutMap.put(cleanupKey("CELID"), null);
      publisherNameShortcutMap.put(cleanupKey("Agorà & Co"), null);
      publisherNameShortcutMap.put(cleanupKey("Edizioni Epoké"), null);
      publisherNameShortcutMap.put(cleanupKey("Università degli studi di Macerata"), null);
      publisherNameShortcutMap.put(cleanupKey("L'asino d'oro"), "ASINO");
      publisherNameShortcutMap.put(cleanupKey("Dipartimento di filosofia, Università di Bologna"),"CLUEB");
      publisherNameShortcutMap.put(cleanupKey("V&P università"), null);
      publisherNameShortcutMap.put(cleanupKey("F.Angeli"), "FRANCOA");
      publisherNameShortcutMap.put(cleanupKey("Leo S. Olschki S. A. éditeur"), null);
      publisherNameShortcutMap.put(cleanupKey("Ministero per i beni e le attività culturali, Direzione generale per gli archivi"), null);
      publisherNameShortcutMap.put(cleanupKey("Vita e Pensiero Universita"), null);


    }
    
    public static String matchPublisherName(String originalString) {
      return publisherNameShortcutMap.get(originalString);
    }

  public static String matchPublisherName2016(String originalString) {
    return publisherNameShortcutMap2016.get(originalString);
  }

    public static String cleanupKey(String originalDateString) {
        String publisherCleanName = originalDateString.replaceAll(
                "[^a-zA-Z0-9&]", "").toLowerCase();
        //log.debug3("-------originalDateString = " + originalDateString + ", publisherCleanName = " + publisherCleanName);
        return  publisherCleanName;
    }

}
