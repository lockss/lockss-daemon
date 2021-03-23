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
    static {
      publisherNameShortcutMap = new HashMap<>();
      // From the excel sheet publisher sent
      publisherNameShortcutMap.put(cleanupKey("21 Editore"),"21EDIT");
      publisherNameShortcutMap.put(cleanupKey("Accademia di Romania"),"ROMANIA");
      publisherNameShortcutMap.put(cleanupKey("Accademia University Press"),"AUP");
      publisherNameShortcutMap.put(cleanupKey("Agorà & Co."),"AGORA");
      publisherNameShortcutMap.put(cleanupKey("AIB - Associazione Italiana Biblioteche"),"AIB");
      publisherNameShortcutMap.put(cleanupKey("Alpes Italia"),"ALPES");
      publisherNameShortcutMap.put(cleanupKey("Altralinea edizioni"),"ALTRALIN");
      publisherNameShortcutMap.put(cleanupKey("Aluvión Editorial"),"ALUVION");
      publisherNameShortcutMap.put(cleanupKey("Anthropos Editorial"),"ANTHROP");
      publisherNameShortcutMap.put(cleanupKey("Archaeopress Publishing"),"ARCHAEO");
      publisherNameShortcutMap.put(cleanupKey("Artemide"),"ARTEMIDE");
      publisherNameShortcutMap.put(cleanupKey("Associazione di studi storici Elio Conti"),"ASSTOR");
      publisherNameShortcutMap.put(cleanupKey("Biblioteca dei Leoni"),"BIBLEONI");
      publisherNameShortcutMap.put(cleanupKey("Bononia University Press"),"BUP");
      publisherNameShortcutMap.put(cleanupKey("Bookstones"),"BOOKSTON");
      publisherNameShortcutMap.put(cleanupKey("Cadmo"),"CADMO");
      publisherNameShortcutMap.put(cleanupKey("Casalini Libri"),"CASA");
      publisherNameShortcutMap.put(cleanupKey("CPL - Centro Primo Levi"),"CPL");
      publisherNameShortcutMap.put(cleanupKey("Di che cibo 6?"),"CIBO");
      publisherNameShortcutMap.put(cleanupKey("Diderotiana Editrice"),"DIDEROT");
      publisherNameShortcutMap.put(cleanupKey("École française d'Athènes"),"EFA");
      publisherNameShortcutMap.put(cleanupKey("EdiSud Salerno"),"EDISUD");
      publisherNameShortcutMap.put(cleanupKey("Editore XY.IT"),"XYIT");
      publisherNameShortcutMap.put(cleanupKey("Editorial Comares"),"COMARES");
      publisherNameShortcutMap.put(cleanupKey("Editorial Reus"),"REUS");
      publisherNameShortcutMap.put(cleanupKey("Editrice Bibliografica"),"GRAFICA");
      publisherNameShortcutMap.put(cleanupKey("Edizioni Clichy"),"CLICHY");
      publisherNameShortcutMap.put(cleanupKey("Edizioni dell'Ateneo"),"ATENEO");
      publisherNameShortcutMap.put(cleanupKey("Edizioni Epoké"),"EPOKE");
      publisherNameShortcutMap.put(cleanupKey("Edizioni Pendragon"),"PENDRA");
      publisherNameShortcutMap.put(cleanupKey("Edizioni Qiqajon"),"QIQAJON");
      publisherNameShortcutMap.put(cleanupKey("Edizioni Quasar"),"QUASAR");
      publisherNameShortcutMap.put(cleanupKey("Edizioni Studium"),"STUDIUM");
      publisherNameShortcutMap.put(cleanupKey("EGEA"),"EGEA");
      publisherNameShortcutMap.put(cleanupKey("ETS"),"ETS");
      publisherNameShortcutMap.put(cleanupKey("EUNSA - Ediciones Universidad de Navarra"),"EUNSA");
      publisherNameShortcutMap.put(cleanupKey("Eurilink University Press"),"EURI");
      publisherNameShortcutMap.put(cleanupKey("Fabrizio Serra Editore"),"SERRA");
      publisherNameShortcutMap.put(cleanupKey("Franco Angeli"),"FRANCOA");
      publisherNameShortcutMap.put(cleanupKey("Genova University Press"),"GUP");
      publisherNameShortcutMap.put(cleanupKey("Giannini Editore"),"GIAN");
      publisherNameShortcutMap.put(cleanupKey("Giappichelli Editore"),"GIAPPI");
      publisherNameShortcutMap.put(cleanupKey("Giardini Editori e Stampatori in Pisa"),"GIARDI");
      publisherNameShortcutMap.put(cleanupKey("Gruppo Editoriale Internazionale"),"GEI");
      publisherNameShortcutMap.put(cleanupKey("Guida Editori"),"GUIDA");
      publisherNameShortcutMap.put(cleanupKey("Herder Editorial"),"HERDER");
      publisherNameShortcutMap.put(cleanupKey("Hoepli"),"HOEPLI");
      publisherNameShortcutMap.put(cleanupKey("If Press"),"IFPRESS");
      publisherNameShortcutMap.put(cleanupKey("IFAC - Istituto di Fisica Applicata Nello Carrara"),"IFAC");
      publisherNameShortcutMap.put(cleanupKey("Il Calamo"),"CALAMO");
      publisherNameShortcutMap.put(cleanupKey("Il Lavoro Editoriale"),"LAVORO");
      publisherNameShortcutMap.put(cleanupKey("Il Poligrafo casa editrice"),"POLIGR");
      publisherNameShortcutMap.put(cleanupKey("Infinito edizioni"),"INFINITO");
      publisherNameShortcutMap.put(cleanupKey("Inschibboleth edizioni"),"INSCHIB");
      publisherNameShortcutMap.put(cleanupKey("Istituti Editoriali e Poligrafici Internazionali"),"IEPI");
      publisherNameShortcutMap.put(cleanupKey("Krill Books"),"KRILL");
      publisherNameShortcutMap.put(cleanupKey("La Ergástula"),"ERGAS");
      publisherNameShortcutMap.put(cleanupKey("La Vita Felice"),"VITAF");
      publisherNameShortcutMap.put(cleanupKey("L'asino d'oro edizioni"),"ASINO");
      publisherNameShortcutMap.put(cleanupKey("Ledizioni"),"LEDIZ");
      publisherNameShortcutMap.put(cleanupKey("Leo S. Olschki"),"OLSC");
      publisherNameShortcutMap.put(cleanupKey("Leone Editore"),"LEONE");
      publisherNameShortcutMap.put(cleanupKey("L'Erma di Bretschneider"),"ERMA");
      publisherNameShortcutMap.put(cleanupKey("L'Harmattan"),"HARMA");
      publisherNameShortcutMap.put(cleanupKey("Licosia Edizioni"),"LICO");
      publisherNameShortcutMap.put(cleanupKey("LIM - Libreria Musicale Italiana"),"LIM");
      publisherNameShortcutMap.put(cleanupKey("Loffredo"),"LOFFR");
      publisherNameShortcutMap.put(cleanupKey("Mandragora"),"MANDRA");
      publisherNameShortcutMap.put(cleanupKey("Marco Saya Edizioni"),"SAYA");
      publisherNameShortcutMap.put(cleanupKey("Mardaga"),"MARDAGA");
      publisherNameShortcutMap.put(cleanupKey("Metauro"),"METAU");
      publisherNameShortcutMap.put(cleanupKey("Mimesis Edizioni"),"MIMESIS");
      publisherNameShortcutMap.put(cleanupKey("Morcelliana"),"MORCEL");
      publisherNameShortcutMap.put(cleanupKey("Morlacchi Editore"),"MORLA");
      publisherNameShortcutMap.put(cleanupKey("Nardini editore"),"NARDINI");
      publisherNameShortcutMap.put(cleanupKey("New Digital Frontiers"),"NDF");
      publisherNameShortcutMap.put(cleanupKey("Nicomp"),"NICOMP");
      publisherNameShortcutMap.put(cleanupKey("Officina Libraria"),"OFFICINA");
      publisherNameShortcutMap.put(cleanupKey("Orthotes Editrice"),"ORTHO");
      publisherNameShortcutMap.put(cleanupKey("Paolo Loffredo Editore"),"INIZIAT");
      publisherNameShortcutMap.put(cleanupKey("Paris Expérimental"),"PARISEX");
      publisherNameShortcutMap.put(cleanupKey("Passigli"),"PASSIGLI");
      publisherNameShortcutMap.put(cleanupKey("Pàtron Editore"),"PATRON");
      publisherNameShortcutMap.put(cleanupKey("Plaza y Valdés Editores"),"PLAZA");
      publisherNameShortcutMap.put(cleanupKey("PM edizioni"),"PMEDIZ");
      publisherNameShortcutMap.put(cleanupKey("Prospettiva edizioni"),"PROSP");
      publisherNameShortcutMap.put(cleanupKey("Rosenberg & Sellier"),"ROSENB");
      publisherNameShortcutMap.put(cleanupKey("Settegiorni Editore"),"SETTEGI");
      publisherNameShortcutMap.put(cleanupKey("Settenove edizioni"),"SETTENO");
      publisherNameShortcutMap.put(cleanupKey("Sillabe"),"SILLABE");
      publisherNameShortcutMap.put(cleanupKey("SISMEL - Edizioni del Galluzzo"),"SISMEL");
      publisherNameShortcutMap.put(cleanupKey("Sovera Edizioni"),"SOVERA");
      publisherNameShortcutMap.put(cleanupKey("Stilo Editrice"),"STILO");
      publisherNameShortcutMap.put(cleanupKey("Storia e Letteratura"),"SEL");
      publisherNameShortcutMap.put(cleanupKey("TAB edizioni"),"TAB");
      publisherNameShortcutMap.put(cleanupKey("Tangram Edizioni Scientifiche"),"TANGRAM");
      publisherNameShortcutMap.put(cleanupKey("Tra le righe libri"),"TRALERIG");
      publisherNameShortcutMap.put(cleanupKey("Trama Editorial"),"TRAMA");
      publisherNameShortcutMap.put(cleanupKey("Urbaniana University Press"),"URBAN");
      publisherNameShortcutMap.put(cleanupKey("Visor Libros"),"VISOR");
      publisherNameShortcutMap.put(cleanupKey("Vita e Pensiero"),"VITAE");
      publisherNameShortcutMap.put(cleanupKey("Zanichelli Editore"),"ZANI");


      // From 2016
      publisherNameShortcutMap.put(cleanupKey("Centro per la filosofia italiana"),"CADMO");
      publisherNameShortcutMap.put(cleanupKey("The Wolfsonian Foundation"),"CADMO");
      publisherNameShortcutMap.put(cleanupKey("Amalthea"),"CADMO");
      publisherNameShortcutMap.put(cleanupKey("Jaca book"),"CLUEB");
      publisherNameShortcutMap.put(cleanupKey("Dipartimento di filosofia Università di Bologna"),"CLUEB");
      publisherNameShortcutMap.put(cleanupKey("Petite plaisance"),"CLUEB");
      publisherNameShortcutMap.put(cleanupKey("Eum"),"CLUEB");
      publisherNameShortcutMap.put(cleanupKey("[s.n.]"),"CLUEB");
      publisherNameShortcutMap.put(cleanupKey("Regione Emilia-Romagna"),"CLUEB");
      publisherNameShortcutMap.put(cleanupKey("Ministero per i beni e le attività culturali Direzione generale per gli archivi"),"CLUEB");
      publisherNameShortcutMap.put(cleanupKey("Faenza editrice"),"CLUEB");
      publisherNameShortcutMap.put(cleanupKey("Università La Sapienza"),"CLUEB");
      publisherNameShortcutMap.put(cleanupKey("Uranoscopo"),"CLUEB");
      publisherNameShortcutMap.put(cleanupKey("Giardini"),"GIARDI");
      publisherNameShortcutMap.put(cleanupKey("Università degli studi di Macerata"),"IEPI");
      publisherNameShortcutMap.put(cleanupKey("Antenore"),"IEPI");

      // From 2020
      publisherNameShortcutMap.put(cleanupKey("AIB"),"AIB");
      publisherNameShortcutMap.put(cleanupKey("Alpes"),"ALPES");
      publisherNameShortcutMap.put(cleanupKey("Altralinea"),"ALTRALIN");
      ///////PublisherNameShortcutMap.put("Antenore :"),"2020");
      publisherNameShortcutMap.put(cleanupKey("Anthropos"),"ANTHROP");
      publisherNameShortcutMap.put(cleanupKey("Associazione italiana biblioteche"),"AIB");
      publisherNameShortcutMap.put(cleanupKey("BIBLIOGRAFICA"),null);
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
      publisherNameShortcutMap.put(cleanupKey("F. Angeli"),null);
      publisherNameShortcutMap.put(cleanupKey("F. Serra"),null);
      publisherNameShortcutMap.put(cleanupKey("Fabrizio Serra"),"SERRA");
      publisherNameShortcutMap.put(cleanupKey("Fondazione Ignazio Mormino del Banco di Sicilia  ;"),null);
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
      publisherNameShortcutMap.put(cleanupKey("Infinito"),null);
      publisherNameShortcutMap.put(cleanupKey("L'asino d'oro edizioni"),null);
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
      publisherNameShortcutMap.put(cleanupKey("Mimesis"),null);
      publisherNameShortcutMap.put(cleanupKey("Morlacchi"),"MORLA");
      publisherNameShortcutMap.put(cleanupKey("Nardini"),"NARDINI");
      publisherNameShortcutMap.put(cleanupKey("New Digital Press"),null);
      publisherNameShortcutMap.put(cleanupKey("Orthotes"),null);
      publisherNameShortcutMap.put(cleanupKey("PM"),null);
      publisherNameShortcutMap.put(cleanupKey("Paolo Loffredo"),null);
      publisherNameShortcutMap.put(cleanupKey("Paolo Loffredo iniziative editoriali"),null);
      publisherNameShortcutMap.put(cleanupKey("Partagées"),null);
      publisherNameShortcutMap.put(cleanupKey("Patron"),"PATRON");
      publisherNameShortcutMap.put(cleanupKey("Pàtron"),"PATRON");
      publisherNameShortcutMap.put(cleanupKey("Pendragon"),null);
      publisherNameShortcutMap.put(cleanupKey("Pesaro"),null);
      publisherNameShortcutMap.put(cleanupKey("Plaza y Valdés"),"PLAZA");
      publisherNameShortcutMap.put(cleanupKey("Prospettiva"),"PROSP");
      publisherNameShortcutMap.put(cleanupKey("Qiqajon"),"QIQAJON");
      publisherNameShortcutMap.put(cleanupKey("Qiqajon - Comunità di Bose"),null);
      publisherNameShortcutMap.put(cleanupKey("Quasar"),"QUASAR");
      publisherNameShortcutMap.put(cleanupKey("ROSENBERG & SELLER"),null);
      publisherNameShortcutMap.put(cleanupKey("Reus"),"REUS");
      publisherNameShortcutMap.put(cleanupKey("Rosenberg Sellier"),null);
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
      publisherNameShortcutMap.put(cleanupKey("Tangram"),null);
      publisherNameShortcutMap.put(cleanupKey("Tra le righe"),"TRALERIG");
      publisherNameShortcutMap.put(cleanupKey("U. Hoepli"),null);
      publisherNameShortcutMap.put(cleanupKey("Ulrico Hoepli"),null);
      publisherNameShortcutMap.put(cleanupKey("V&P strumenti"),null);
      publisherNameShortcutMap.put(cleanupKey("V&P università"),null);
      publisherNameShortcutMap.put(cleanupKey("Vita e Pensiero Università"),null);
      publisherNameShortcutMap.put(cleanupKey("XY.IT"),"XYIT");
      publisherNameShortcutMap.put(cleanupKey("Zanichelli"),null);
      publisherNameShortcutMap.put(cleanupKey("Zanichelli"),null);
      publisherNameShortcutMap.put(cleanupKey("Zanichelli[2009]"),null);
      publisherNameShortcutMap.put(cleanupKey("L'Erma\" di Bretschneider"),null);
    }
    
    public static String matchPublisherName(String originalString) {
      return publisherNameShortcutMap.get(originalString);
    }

    public static String cleanupKey(String originalDateString) {
        String publisherCleanName = originalDateString.replaceAll(
                "[^a-zA-Z0-9&]", "").toLowerCase();

        log.debug("-------originalDateString = " + originalDateString + ", publisherCleanName = " + publisherCleanName);

        return  publisherCleanName;
    }

}
