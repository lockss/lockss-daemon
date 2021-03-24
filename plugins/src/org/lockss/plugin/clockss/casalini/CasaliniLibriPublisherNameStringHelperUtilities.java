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

  protected static final Map<String, String> canonical;
  static {
    canonical = new HashMap<>();
    canonical.put("accademia di romania", "Accademia di Romania");
    canonical.put("accademia university press", "Accademia University Press");
    canonical.put("agora & co", "Agorà & Co.");
    canonical.put("agorà & co", "Agorà & Co.");
    canonical.put("aib", "AIB - Associazione Italiana Biblioteche");
    canonical.put("alpes", "Alpes Italia");
    canonical.put("alpes italia", "Alpes Italia");
    canonical.put("altralinea", "Altralinea edizioni");
    canonical.put("altralinea edizioni", "Altralinea edizioni");
    canonical.put("aluvion editorial", "Aluvión Editorial");
    canonical.put("aluvión editorial", "Aluvión Editorial");
    canonical.put("amalthea", "Cadmo");
    canonical.put("antenore la facolta giardini", "Istituti Editoriali e Poligrafici Internazionali");
    canonical.put("antenore la facoltà giardini", "Istituti Editoriali e Poligrafici Internazionali");
    canonical.put("anthropos", "Anthropos Editorial");
    canonical.put("anthropos editorial", "Anthropos Editorial");
    canonical.put("anthropos fundacion caja de madrid", "Anthropos Editorial");
    canonical.put("anthropos fundación caja de madrid", "Anthropos Editorial");
    canonical.put("anthropos fundacion cultural eduardo cohen", "Anthropos Editorial");
    canonical.put("anthropos fundación cultural eduardo cohen", "Anthropos Editorial");
    canonical.put("anthropos universidad autonoma metropolitana", "Anthropos Editorial");
    canonical.put("anthropos universidad autónoma metropolitana", "Anthropos Editorial");
    canonical.put("associazione di studi storici elio conti", "Associazione di studi storici Elio Conti");
    canonical.put("associazione italiana biblioteche", "AIB - Associazione Italiana Biblioteche");
    canonical.put("bibliografica", "Editrice Bibliografica");
    canonical.put("biblioteca dei leoni", "Biblioteca dei Leoni");
    canonical.put("bononia university press", "Bononia University Press");
    canonical.put("cadmo", "Cadmo");
    canonical.put("cadmo centro mario rossi per gli studi filosofici", "Cadmo");
    canonical.put("casalini libri", "Casalini Libri");
    canonical.put("celid", "Celid");
    canonical.put("centro per la filosofia italiana cadmo", "Cadmo");
    canonical.put("clichy", "Edizioni Clichy");
    canonical.put("clueb", "CLUEB");
    canonical.put("clueb cisui", "CLUEB");
    canonical.put("clueb ediciones universidad de salamanca", "CLUEB");
    canonical.put("clueb ediciones universidad salamanca", "CLUEB");
    canonical.put("clueb icm istituto per gli incontriculturali mitteleuropei", "CLUEB");
    canonical.put("clueb regione emilia-romagna", "CLUEB");
    canonical.put("comares", "Editorial Comares");
    canonical.put("comune di falconara marittima", "Metauro");
    canonical.put("cpl editions", "CPL - Centro Primo Levi");
    canonical.put("di che cibo 6", "Di che cibo 6?");
    canonical.put("diderotiana editrice", "Diderotiana Editrice");
    canonical.put("dipartimento di filosofia universita di bologna", "CLUEB");
    canonical.put("dipartimento di filosofia università di bologna", "CLUEB");
    canonical.put("ecole francaise d'athenes", "Ecole française d'Athènes");
    canonical.put("école française d'athènes", "Ecole française d'Athènes");
    canonical.put("editore ulrico hoepli", "Hoepli");
    canonical.put("editore xy.it", "Editore XY.IT");
    canonical.put("editorial comares", "Editorial Comares");
    canonical.put("editrice bibliografia", "Editrice Bibliografica"); //sic
    canonical.put("editrice bibliografica", "Editrice Bibliografica");
    canonical.put("edizione di storia e letteratura", "Storia e Letteratura");
    canonical.put("edizioni clichy", "Edizioni Clichy");
    canonical.put("edizioni del galluzzo per la fondazione ezio franceschini", "SISMEL - Edizioni del Galluzzo");
    canonical.put("edizioni dell'ateneo", "Edizioni dell'Ateneo");
    canonical.put("edizioni di storia e letteratura", "Storia e Letteratura");
    canonical.put("edizioni di storia e letteratura centro tedesco di studi veneziani", "Storia e Letteratura");
    canonical.put("edizioni epoke", "Edizioni Epoké");
    canonical.put("edizioni epoké", "Edizioni Epoké");
    canonical.put("edizioni ets", "ETS");
    canonical.put("edizioni quasar", "Edizioni Quasar");
    canonical.put("edizioni storia e letteratura", "Storia e Letteratura");
    canonical.put("edizioni studium", "Edizioni Studium");
    canonical.put("egea", "EGEA");
    canonical.put("egea universita bocconi", "EGEA");
    canonical.put("ets", "ETS");
    canonical.put("eum", "CLUEB");
    canonical.put("eunsa", "EUNSA - Ediciones Universidad de Navarra");
    canonical.put("eurilink", "Eurilink University Press");
    canonical.put("eurilink university press", "Eurilink University Press");
    canonical.put("fabrizio serra", "Fabrizio Serra Editore");
    canonical.put("fabrizio serra editore", "Fabrizio Serra Editore");
    canonical.put("faenza editrice", "CLUEB");
    canonical.put("f. angeli", "Franco Angeli");
    canonical.put("f.angeli", "Franco Angeli");
    canonical.put("fondazione ignazio mormino del banco di sicilia l'erma di bretschneider", "L'Erma di Bretschneider");
    canonical.put("franco angeli", "Franco Angeli");
    canonical.put("francoangeli", "Franco Angeli");
    canonical.put("f. serra", "Fabrizio Serra Editore");
    canonical.put("genova university press", "Genova University Press");
    canonical.put("g. giappichelli", "Giappichelli Editore");
    canonical.put("g. giappichelli editore", "Giappichelli Editore");
    canonical.put("giannini", "Giannini Editore");
    canonical.put("giannini editore", "Giannini Editore");
    canonical.put("giappichelli", "Giappichelli Editore");
    canonical.put("giappichelli editore", "Giappichelli Editore");
    canonical.put("giardini", "Giardini Editori e Stampatori in Pisa");
    canonical.put("giardini editori e stampatori", "Giardini Editori e Stampatori in Pisa");
    canonical.put("gruppo editoriale internazionale", "Gruppo Editoriale Internazionale");
    canonical.put("guida", "Guida Editori");
    canonical.put("guida editori", "Guida Editori");
    canonical.put("herder", "Herder Editorial");
    canonical.put("hoepli", "Hoepli");
    canonical.put("ifac - istituto di fisica applicata nello carrara", "IFAC - Istituto di Fisica Applicata Nello Carrara");
    canonical.put("if press", "If Press");
    canonical.put("il calamo", "Il Calamo");
    canonical.put("il calamo dipartimento di studi glottoantropologici universita di roma la sapienza", "Il Calamo");
    canonical.put("il lavoro editoriale", "Il Lavoro Editoriale");
    canonical.put("il poligrafo", "Il Poligrafo casa editrice");
    canonical.put("infinito", "Infinito edizioni");
    canonical.put("infinito edizioni", "Infinito edizioni");
    canonical.put("inschibboleth", "Inschibboleth edizioni");
    canonical.put("istituti editoriali e poligrafici internazionali", "Istituti Editoriali e Poligrafici Internazionali");
    canonical.put("istituti editoriali e poligrafici internazionali universita degli studi di macerata", "Istituti Editoriali e Poligrafici Internazionali");
    canonical.put("istituti editoriali e poligrafici internazionali università degli studi di macerata", "Istituti Editoriali e Poligrafici Internazionali");
    canonical.put("jaca book clueb", "CLUEB");
    canonical.put("la ergastula", "La Ergástula");
    canonical.put("la ergástula", "La Ergástula");
    canonical.put("l'asino d'oro", "L'asino d'oro edizioni");
    canonical.put("l'asino d'oro edizioni", "L'asino d'oro edizioni");
    canonical.put("latium", "Edizioni Quasar");
    canonical.put("la vita felice", "La Vita Felice");
    canonical.put("ledizioni", "Ledizioni");
    canonical.put("ledizioni ledipublishing", "Ledizioni");
    canonical.put("leone", "Leone Editore");
    canonical.put("leone editore", "Leone Editore");
    canonical.put("leo s. olschki", "Leo S. Olschki");
    canonical.put("leo s. olschki editore", "Leo S. Olschki");
    canonical.put("leo s. olschki s. a. editeur", "Leo S. Olschki");
    canonical.put("l'erma di bretschneider", "L'Erma di Bretschneider");
    canonical.put("l'erma di bretschneider comune di velletri", "L'Erma di Bretschneider");
    canonical.put("libreria musicale italiana", "LIM - Libreria Musicale Italiana");
    canonical.put("licosia", "Licosia Edizioni");
    canonical.put("licosia edizioni", "Licosia Edizioni");
    canonical.put("loffredo editore", "Loffredo");
    canonical.put("l. s. olschki", "Leo S. Olschki");
    canonical.put("l.s. olschki", "Leo S. Olschki");
    canonical.put("l.s. olschki department of italian the university of w. australia", "Leo S. Olschki");
    canonical.put("l.s. olschki istituto per il lessico intellettuale europeo e storia delle idee", "Leo S. Olschki");
    canonical.put("l.s. olschki regione toscana", "Leo S. Olschki");
    canonical.put("mandragora", "Mandragora");
    canonical.put("marco saya edizioni", "Marco Saya Edizioni");
    canonical.put("matauro", "Metauro"); //sic
    canonical.put("metauro", "Metauro");
    canonical.put("mimesis", "Mimesis Edizioni");
    canonical.put("ministero per i beni e le attivita culturali direzione generale per gli archivi", "CLUEB");
    canonical.put("ministero per i beni e le attività culturali direzione generale per gli archivi", "CLUEB");
    canonical.put("morcelliana", "Morcelliana");
    canonical.put("morlacchi", "Morlacchi Editore");
    canonical.put("nardini", "Nardini editore");
    canonical.put("new digital frontiers", "New Digital Frontiers");
    canonical.put("new digital press", "New Digital Frontiers"); //sic
    canonical.put("nicomp", "Nicomp");
    canonical.put("officina libraria", "Officina Libraria");
    canonical.put("orthotes", "Orthotes Editrice");
    canonical.put("paolo loffredo", "Loffredo");
    canonical.put("paolo loffredo iniziative editoriali", "Loffredo");
    canonical.put("partagees", "Giannini Editore");
    canonical.put("partagées", "Giannini Editore");
    canonical.put("passigli", "Passigli");
    canonical.put("patron", "Pàtron Editore");
    canonical.put("pàtron", "Pàtron Editore");
    canonical.put("pendragon", "Edizioni Pendragon");
    canonical.put("pesaro", "Metauro");
    canonical.put("petite plaisance clueb", "CLUEB");
    canonical.put("plaza y valdes", "Plaza y Valdés Editores");
    canonical.put("plaza y valdés", "Plaza y Valdés Editores");
    canonical.put("plaza y valdes editores", "Plaza y Valdés Editores");
    canonical.put("plaza y valdés editores", "Plaza y Valdés Editores");
    canonical.put("pm", "PM edizioni");
    canonical.put("pm edizioni", "PM edizioni");
    canonical.put("prospettiva", "Prospettiva edizioni");
    canonical.put("prospettiva edizioni", "Prospettiva edizioni");
    canonical.put("qiqajon", "Edizioni Qiqajon");
    canonical.put("qiqajon - comunita di bose", "Edizioni Qiqajon");
    canonical.put("quasar", "Edizioni Quasar");
    canonical.put("quasar associazione giovanni secco suardo", "Edizioni Quasar");
    canonical.put("quasar longo scripta manent di tipogr. mancini", "Edizioni Quasar");
    canonical.put("regione emilia-romagna clueb", "CLUEB");
    canonical.put("reus", "Editorial Reus");
    canonical.put("rosenberg & seller", "Rosenberg & Sellier"); //sic
    canonical.put("rosenberg & sellier", "Rosenberg & Sellier");
    canonical.put("rosenberg sellier", "Rosenberg & Sellier");
    canonical.put("sel", "Storia e Letteratura");
    canonical.put("settegiorni", "Settegiorni Editore");
    canonical.put("settenove", "Settenove edizioni");
    canonical.put("sillabe", "Sillabe");
    canonical.put("sismel edizioni del galluzzo", "SISMEL - Edizioni del Galluzzo");
    canonical.put("s.n.", "CLUEB"); // Not universal, but all s.n. in 2016 and 2020 happen to be CLUEB
    canonical.put("sovera edizioni", "Sovera Edizioni");
    canonical.put("stilo", "Stilo Editrice");
    canonical.put("stilo editrice", "Stilo Editrice");
    canonical.put("storia e letteratura", "Storia e Letteratura");
    canonical.put("studium", "Edizioni Studium");
    canonical.put("tab", "TAB edizioni");
    canonical.put("tab edizioni", "TAB edizioni");
    canonical.put("tangram", "Tangram Edizioni Scientifiche");
    canonical.put("tangram edizioni scientifiche", "Tangram Edizioni Scientifiche");
    canonical.put("the wolfsonian foundation amalthea", "Cadmo");
    canonical.put("tra le righe", "Tra le righe libri");
    canonical.put("tra le righe libri", "Tra le righe libri");
    canonical.put("trama editorial", "Trama Editorial");
    canonical.put("u. hoepli", "Hoepli");
    canonical.put("ulrico hoepli", "Hoepli");
    canonical.put("universita degli studi di macerata", "Istituti Editoriali e Poligrafici Internazionali");
    canonical.put("università degli studi di macerata", "Istituti Editoriali e Poligrafici Internazionali");
    canonical.put("universita la sapienza", "CLUEB");
    canonical.put("università la sapienza", "CLUEB");
    canonical.put("uranoscopo", "CLUEB");
    canonical.put("urbaniana university press", "Urbaniana University Press");
    canonical.put("visor libros", "Visor Libros");
    canonical.put("vita e pensiero", "Vita e Pensiero");
    canonical.put("vita e pensiero universita", "Vita e Pensiero");
    canonical.put("v&p strumenti", "Vita e Pensiero");
    canonical.put("v&p universita", "Vita e Pensiero");
    canonical.put("xy.it", "Editore XY.IT");
    canonical.put("zanichelli", "Zanichelli Editore");
  }

  protected static final Map<String, String> shortcut2016;
  static {
    shortcut2016 = new HashMap<>();
    shortcut2016.put("Edizioni dell'Ateneo", "ATENEO");
    shortcut2016.put("Cadmo", "CADMO");
    shortcut2016.put("Casalini Libri", "CASA");
    shortcut2016.put("CLUEB", "CLUEB");
    shortcut2016.put("Gruppo Editoriale Internazionale", "GEI");
    shortcut2016.put("Giardini Editori e Stampatori in Pisa", "GIARDI");
    shortcut2016.put("Istituti Editoriali e Poligrafici Internazionali", "IEPI");
  }
  
}
 