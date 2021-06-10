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

import org.lockss.plugin.clockss.MetadataStringHelperUtilities;
import org.lockss.util.Logger;
import org.marc4j.*;
import org.xml.sax.InputSource;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class CasaliniLibriPublisherNameStringHelperUtilities {

    private static final Logger log = Logger.getLogger(CasaliniLibriPublisherNameStringHelperUtilities.class);

    protected static final Map<String, String> canonical;
    static {
      canonical = new HashMap<>();
      canonical.put("21 editore", "21 Editore");
      canonical.put("accademia di romania", "Accademia di Romania");
      canonical.put("accademia university press", "Accademia University Press");
      canonical.put("agora & co", "Agorà & Co.");
      canonical.put("agorà & co", "Agorà & Co.");
      canonical.put("agorà & co.", "Agorà & Co.");
      canonical.put("agorà", "Agorà & Co.");
      canonical.put("aib", "AIB - Associazione Italiana Biblioteche");
      canonical.put("aib - associazione italiana biblioteche", "AIB - Associazione Italiana Biblioteche");
      canonical.put("alpes", "Alpes Italia");
      canonical.put("alpes italia", "Alpes Italia");
      canonical.put("altralinea", "Altralinea edizioni");
      canonical.put("altralinea edizioni", "Altralinea edizioni");
      canonical.put("aluvion editorial", "Aluvión Editorial");
      canonical.put("aluvión editorial", "Aluvión Editorial");
      canonical.put("amalthea", "Cadmo");
      canonical.put("amsterdam university press", "Amsterdam University Press");
      canonical.put("antenore", "Istituti Editoriali e Poligrafici Internazionali");
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
      canonical.put("archaeopress", "Archaeopress");
      canonical.put("artemide", "Artemide");
      canonical.put("associazione di studi storici elio conti", "Associazione di studi storici Elio Conti");
      canonical.put("associazione italiana biblioteche", "AIB - Associazione Italiana Biblioteche");
      canonical.put("ateneo pontificio regina apostolorum","Ateneo Pontificio Regina Apostolorum");
      canonical.put("bibliografica", "Editrice Bibliografica");
      canonical.put("biblioteca dei leoni", "Biblioteca dei Leoni");
      canonical.put("bononia university press", "Bononia University Press");
      canonical.put("bookstones", "Bookstones");
      canonical.put("cadmo", "Cadmo");
      canonical.put("cadmo centro mario rossi per gli studi filosofici", "Cadmo");
      canonical.put("casalini", "Casalini Libri");
      canonical.put("casalini libri", "Casalini Libri");
      canonical.put("celid", "Celid");
      canonical.put("centro per la filosofia italiana", "Cadmo");
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
      canonical.put("edisud", "Edisud");
      canonical.put("edisud salerno", "Edisud Salerno");
      canonical.put("editions l'harmattan", "Editions L'Harmattan");
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
      canonical.put("firenze university press", "Firenze University Press");
      canonical.put("faenza editrice", "CLUEB");
      canonical.put("f. angeli", "Franco Angeli");
      canonical.put("f.angeli", "Franco Angeli");
      canonical.put("fondazione ignazio mormino del banco di sicilia", "L'Erma di Bretschneider");
      canonical.put("fondazione ignazio mormino del banco di sicilia l'erma di bretschneider", "L'Erma di Bretschneider");
      canonical.put("franco angeli", "Franco Angeli");
      canonical.put("franco cesati editore", "Cadmo");
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
      canonical.put("italian cultural institute", "Cadmo");
      canonical.put("jaca book", "CLUEB");
      canonical.put("jaca book clueb", "CLUEB");
      canonical.put("jouvence", "Jouvence");
      canonical.put("la ergastula", "La Ergástula");
      canonical.put("la ergástula", "La Ergástula");
      canonical.put("la otra h", "Herder Editorial");
      canonical.put("l'harmattan", "L'Harmattan");
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
      canonical.put("mardaga", "Mardaga");
      canonical.put("mandragora", "Mandragora");
      canonical.put("marco saya edizioni", "Marco Saya Edizioni");
      canonical.put("matauro", "Metauro"); //sic
      canonical.put("metauro", "Metauro");
      canonical.put("mimesis", "Mimesis Edizioni");
      canonical.put("ministero per i beni e le attivita culturali direzione generale per gli archivi", "CLUEB");
      canonical.put("ministero per i beni e le attività culturali direzione generale per gli archivi", "CLUEB");
      canonical.put("morcelliana", "Morcelliana");
      canonical.put("morlacchi", "Morlacchi Editore");
      canonical.put("morlacchi editore u.p.", "Morlacchi Editore");
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
      canonical.put("paris expérimental", "Paris Expérimental");
      canonical.put("passigli", "Passigli");
      canonical.put("patron", "Pàtron Editore");
      canonical.put("pàtron", "Pàtron Editore");
      canonical.put("pendragon", "Edizioni Pendragon");
      canonical.put("pesaro", "Metauro");
      canonical.put("petite plaisance", "CLUEB");
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
      canonical.put("regione emilia-romagna", "CLUEB");
      canonical.put("regione emilia-romagna clueb", "CLUEB");
      canonical.put("reus", "Editorial Reus");
      canonical.put("rosenberg & seller", "Rosenberg & Sellier"); //sic
      canonical.put("rosenberg & sellier", "Rosenberg & Sellier");
      canonical.put("rosenberg sellier", "Rosenberg & Sellier");
      // the original file did double encoded, which cause trouble, and need to this
      // the original string is "Scholé"
      canonical.put("schol\u00e9", "Scholé");
      canonical.put("sel", "Storia e Letteratura");
      canonical.put("settegiorni", "Settegiorni Editore");
      canonical.put("settenove", "Settenove edizioni");
      canonical.put("sillabe", "Sillabe");
      canonical.put("sismel", "SISMEL - Edizioni del Galluzzo");
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
      canonical.put("the wolfsonian foundation", "Cadmo");
      canonical.put("the wolfsonian foundation amalthea", "Cadmo");
      canonical.put("tra le righe", "Tra le righe libri");
      canonical.put("tra le righe libri", "Tra le righe libri");
      canonical.put("trama editorial", "Trama Editorial");
      canonical.put("u. hoepli", "Hoepli");
      canonical.put("ulrico hoepli", "Hoepli");
      canonical.put("unione fitopatologica mediterranea", "Firenze University Press");
      canonical.put("universita degli studi di macerata", "Istituti Editoriali e Poligrafici Internazionali");
      canonical.put("università degli studi di macerata", "Istituti Editoriali e Poligrafici Internazionali");
      canonical.put("università degli studi di milano dipartimento di studi letterari filologici e linguistici","Ledizioni");
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



  public static String getCanonicalPublisherName(String originalString) {
      return canonical.get(originalString.toLowerCase());
    }

    public static String getPublisherNameShortcut2016(String originalString) {
      return shortcut2016.get(originalString);
    }

    public static String cleanupKey(String originalDateString) {
        String publisherCleanName = originalDateString.replaceAll(
                "[^a-zA-Z0-9&]", "").toLowerCase();
        //log.debug3("-------originalDateString = " + originalDateString + ", publisherCleanName = " + publisherCleanName);
        return  publisherCleanName;
    }

//  public static void main(String[] args) throws Exception {
//    String[] files = {
//      "/tmp/m1/2005.mrc",  
//      "/tmp/m1/2014.mrc",  
//      "/tmp/m1/2018.mrc",  
//    };
//    for (String fileStr : files) {
//      MarcReader marcReader = new MarcStreamReader(new FileInputStream(fileStr));
//  //  PrintStream out = System.out;
//      PrintStream out = new PrintStream(new FileOutputStream(fileStr + ".out"));
//      while (marcReader.hasNext()) {
//        String x260b = CasaliniLibriMarcMetadataHelper.getMARCData(marcReader.next(), "260", 'b');
//        if (x260b == null) {
//          x260b = "Casalini Libri";
//        }
//        String key = MetadataStringHelperUtilities.cleanupPublisherName(x260b).toLowerCase();
////        out.println(key);
//        if (!canonical.containsKey(key)) {
//          System.out.println(key);
//        }
//      }
//      out.close();
//    }
//  }
    
}
 