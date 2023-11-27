/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.jstor;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;


/*
 * This RIS extractor is used by both legacy JSTOR as well as JSTOR Current Scholarship
 * Legacy: https://www.jstor.org/action/downloadSingleCitationSec?format=refman&doi=10.5325/chaucerrev.49.1.0001
 * New: http://www.jstor.org/citation/ris/10.2307/40835452
 * 
 * For Legacy:
 * The RIS url is manufactured by a link extractor using the DOI on the TOC page 
 * before collecting and emitting, make sure the corresponding pdf or 
 * full text html is in the AU
 * The RIS file will exist even for a bogus DOI, it's just that the contents
 * will be blank. So do a validation check on the RIS file.
 * 
 * For new:
 * The RIS comes from a link so is valid. Do the check anyway, because why not
 * 
 * Leave the ISSN uncooked and it will come from the TDB more consistently
 * 
 * If the DOI is missing - you can get it from the metadata url 
 */

/*
TY  - JOUR
JO  - The American Biology Teacher
TI  - From the President
VL  - 72
IS  - 9
PB  - University of California Press on behalf of the National Association of Biology Teachers
SN  - 00027685
UR  - http://www.jstor.org/stable/10.1525/abt.2010.72.9.1
AU  - Jaskot, Bunny
DO  - 10.1525/abt.2010.72.9.1
T3  - 
Y1  - 2010/11/01
SP  - 532
CR  - Copyright &#169; 2010 National Association of Biology Teachers
M1  - ArticleType: research-article / Full publication date: November/December 2010 / Copyright © 2010 National Association of Biology Teachers
ER  -
 */ 

/* BOGUS RIS FILE (due to bad DOI)
 * Provider: JSTOR http://www.jstor.org
Database: JSTOR
Content: text/plain
TY  - JOUR
JO  - 
TI  - 
VL  - 
IS  - 
PB  - 
SN  - 
UR  - 
DO  - 
T3  - 
Y1  - 
SP  - 
M1  - ArticleType:  / Full publication date:  / 
ER  - 
 */

public class JstorRisMetadataExtractorFactory implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger(JstorRisMetadataExtractorFactory.class);

  // The filename for an article is *usually* full doi, but may be just 2nd part of doi
  // but it is consistent within an issue - so pull it from the given UR
  // to check that we have full content version of the same name
  final static Pattern FILE_PATTERN = Pattern.compile("(.*)/stable/(view/|info/)?(.*)$", Pattern.CASE_INSENSITIVE);

//https://www.jstor.org/action/downloadSingleCitationSec?format=refman&doi=10.5325/chaucerrev.49.1.0001
//  or
// http://www.jstor.org/citation/ris/10.2307/40835452
  final static Pattern RIS_URL_PATTERN = Pattern.compile("(doi=|citation/ris/)(.*)$", Pattern.CASE_INSENSITIVE);

  
  /*
   * The base RisMetadataExtractor defines the following set tags, but Jstor
   * uses a different set.
   * BASE:
   *  T1-FIELD_ARTICLE_TITLE, AU-FIELD_AUTHOR, JF-FIELD_JOURNAL_TITLE, DO-FIELD_DOI,
   *  PB-FIELD_PUBLISHER,VL-FIELD_VOLUME, IS-FIELD_ISSUE, SP-FIELD_START_PAGE, 
   *  EP-FIELD_END_PAGE,DA-FIELD_DATE
   *  
   *  In Jstor original, the 
   *      FIELD_DATE is Y1 and not DA
   *      FIELD_ARTICLE_TITLE is TI and not T1
   *      FIELD_PUBLICATION_TITLE is JO
   *  we are assuming they won't also use the other tags as well
   *  
   *  In Jstor Current Scholarship, the
   *    FIELD_DATE (year) is PY
   *    FIELD_PUBLICATION_TITLE is T2
   *    FIELD_ARTICLE_TITLE is TI (as before)
   *    The rest of the tags are standard
   *    If, for whatever reason, the DO isn't set, the url has the DOI
   *      
   * Don't cook the SN (issn value) both because in Current Scholarship it is often
   * both values, comma separated which is invalid and would need ot be parsed
   * but also because we don't know which is issn vs eissn and the information will
   * exist in the TDB and get picked up from there       
   */
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    JstorRisMetadataExtractor jris = new JstorRisMetadataExtractor();

    jris.addRisTag("JO", MetadataField.FIELD_PUBLICATION_TITLE);
    jris.addRisTag("T2", MetadataField.FIELD_PUBLICATION_TITLE);
    jris.addRisTag("Y1", MetadataField.FIELD_DATE);
    jris.addRisTag("PY", MetadataField.FIELD_DATE);
    jris.addRisTag("TI", MetadataField.FIELD_ARTICLE_TITLE); // weird, but true in some cases
    // UR will point to a stable URL that we won't have, don't cook this
    // so that we can put in a correct value
    // jris.addRisTag("UR", MetadataField.FIELD_ACCESS_URL);
    return jris;
  }

  public static class JstorRisMetadataExtractor
  extends RisMetadataExtractor {

    /* 
     * Because the RIS URLs were generated off DOI's listed on the TOC
     * do additional validation to make sure we actually have article associated
     * with the DOI in question.
     * full text articles can either be listed as:
     *   http://www.jstor.org/stable/pdfplus/10.1525/abt.2010.72.9.1.pdf
     *     where 10.1525/abt.2010.72.9.1 is the entire doi
     * or
     *   http://www.jstor.org/stable/pdfplus/746318.pdf
     *     where 746318 is the second part of the doi only
     * or possibly a full html listed under ...stable/full/....
     *   http://www.jstor.org/stable/full/10.1525/abt.2010.72.9.1  
     */
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, FileMetadataExtractor.Emitter emitter) 
        throws IOException, PluginException {
      ArticleMetadata am = super.extract(target, cu); //extracts and cooks
      // we have metadata to emit
      if (am != null) {
        Matcher doiMat = RIS_URL_PATTERN.matcher(cu.getUrl());
        String doiVal;
        if (doiMat.find()) {
            doiVal = doiMat.group(2);
            log.debug3("doiVal from url is: " + doiVal);
            //sometimes the doi isn't in the RIS file
            am.putIfBetter(MetadataField.FIELD_DOI, doiVal);
        }
        if (am.get(MetadataField.FIELD_ISSN) == null) {
          String sn_val = am.getRaw("SN");
          if (sn_val != null && sn_val.contains(",")) {
            String one_val = sn_val.substring( 0, sn_val.indexOf(","));
            am.put(MetadataField.FIELD_ISSN, one_val.trim());
          }
        }
        log.debug3("SN val is " + am.get(MetadataField.FIELD_ISSN));

        /* the UR line in the RIS file gives us the filename used for this document 
         * it will be one of two formats:
         *   UR  - http://www.jstor.org/stable/41495857
         *   UR  - http://www.jstor.org/stable/10.1525/ncm.2013.37.2.fm
         * and the corresponding content file will be 
         *   http://www.jstor.org/stable/(pdf|pdfplus)/41495857.pdf or 
         *   http://www.jstor.org/stable/full/41495857 
         * or 
         *   http://www.jstor.org/stable/(pdf|pdfplus)/10.1525/ncm.2013.37.2.fm.pdf
         *   http://www.jstor.org/stable/full/10.1525/ncm.2013.37.2.fm
         */

        /* Get the filename from the UR field in the metadata */
//        String full_UR = am.get(MetadataField.FIELD_ACCESS_URL);
        /*
         * This check is from JSTOR original
         * but leave it in for JSTOR Current Scholarship
         * since we're iterating only on the RIS and XML
         */
        String full_UR = am.getRaw("ur");
        if ((full_UR != null)) {
          Matcher accessUrlMat = FILE_PATTERN.matcher(full_UR);
          if (accessUrlMat.matches()) {
            String filename = accessUrlMat.group(3);
            String base = cu.getArchivalUnit().getConfiguration().get(ConfigParamDescr.BASE_URL.getKey()) + "stable/";
            if (setFullForm(base, filename, cu, am)) {
              emitter.emitMetadata(cu,  am);
            }
          }
        }
      }
      /* either no metadata, or no matching content file, so don't emit */
      log.debug3("cu: " + cu.getUrl() + " had no matching content file");
      return; 
    }

    // build up several possible full form file urls and check if they exist
    // in the AU. If they do, set the FIELD_ACCESS_URL and return
    private static boolean setFullForm(String base, String filename, 
        CachedUrl cu,
        ArticleMetadata am) {
      String newUrl;
      ArchivalUnit jau = cu.getArchivalUnit();
      CachedUrl fileCu;

      // Okay to use am.put because nothing was cooked in to field during extract
      //1. pdfplus?
      newUrl = base + "pdfplus/" + filename + ".pdf";
      fileCu = jau.makeCachedUrl(newUrl);
      if (fileCu != null && (fileCu.hasContent())) {
        am.put(MetadataField.FIELD_ACCESS_URL, newUrl);
        return true;
      }
      //2. pdf
      newUrl = base + "pdf/" + filename + ".pdf";
      fileCu = jau.makeCachedUrl(newUrl);
      if (fileCu != null && (fileCu.hasContent())) {
        am.put(MetadataField.FIELD_ACCESS_URL, newUrl); 
        return true;
      }
      //3. full html?
      newUrl = base + "full/" + filename; // no suffix
      fileCu = jau.makeCachedUrl(newUrl);
      if (fileCu != null && (fileCu.hasContent())) {
        am.put(MetadataField.FIELD_ACCESS_URL, newUrl); 
        return true;
      }
      return false;
    }
  }
}
