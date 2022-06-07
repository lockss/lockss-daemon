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

package gov.gpo.access.permanent.plugin.environmentalhealthperspectives;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.atypon.BaseAtyponMetadataUtil;
import org.lockss.util.Logger;

import java.io.IOException;

/*
 *  NOTE: I have found the following:
 *  JO is often used (incorrectly) but consistently as the abbreviated form of the journal title, use JF and then T2 in preference
 *  Y1 usually is the same as DA, but not always, use DA if it's there
 *  DO NOT pick up the "UR" because it often points to dx.doi.org stable URL which 
 *  will not exist in the AU.  We must manually set the access_url
TY  - JOUR/BOOK/CHAP/ECHAP/EBOOK/EDBOOK
T1  - <article title>
AU  - <author>
AU  - <other author>
Y1  - <date, often same as DA, often slightly later>
PY  - <year of pub>
DA  - <date of pub>
N1  - doi: 10.1137/100798910
DO  - 10.1137/100798910 
T2  - <journal title>
JF  - <journal title>
JO  - <abbreviated journal title>
SP  - <start page>
EP  - <end page>
VL  - <volume>
IS  - <issue>
PB  - <publisher but possibly imprint>
SN  - <issn> or <isbn>
M3  - doi: 10.1137/100798910
UR  - http://dx.doi.org/10.1137/100798910
Y2  - <later date - meaning?>
ER  -  
 * 
 */
public class EHPRisMetadataExtractorFactory
implements FileMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(EHPRisMetadataExtractorFactory.class);

  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {

    log.debug3("Inside Base Atypon Metadata extractor factory for RIS files");

    BaseAtyponRisMetadataExtractor ba_ris = new BaseAtyponRisMetadataExtractor();

    ba_ris.addRisTag("A1", MetadataField.FIELD_AUTHOR);
    // Do not use UR listed in the ris file! It will get set to full text CU by daemon
    return ba_ris;
  }

  public static class BaseAtyponRisMetadataExtractor
  extends RisMetadataExtractor {

    // override this to do some additional attempts to get valid data before emitting
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
        throws IOException, PluginException {
      ArticleMetadata am = extract(target, cu); 

      /* 
       * if, due to overcrawl, we got to a page that didn't have anything
       * valid, eg "this page not found" html page
       * don't emit empty metadata (because defaults would get put in
       * Must do this after cooking, because it checks size of cooked info
       */
      if (am.isEmpty()) {
        return;
      }

      /*
       * RIS data can be variable.  We don't have any way to add priority to
       * the cooking of data, so fallback to alternate values manually
       *
       * There are differences between books and journals, so fork for titles
       * and metadata check 
       */
      if (am.get(MetadataField.FIELD_DATE) == null) {
        if (am.getRaw("Y1") != null) { // if DA wasn't there, use Y1
          am.put(MetadataField.FIELD_DATE, am.getRaw("Y1"));
        }
      }  
      
      
      /*
       * Determine if this is a book item or a journal item.
       * set the appropriate article type once the daemon passes along the TY
       */
      String ris_type = am.getRaw("TY");
      if (ris_type == null) {
        //pre 1.69, do an alternate check because TY wasn't passed through
        ris_type = "JOUR"; //set a default
        if (am.get(MetadataField.FIELD_ISBN) != null) {
          // it is a bad value, but it was recognized as an isbn because of TY type
          ris_type = "BOOK"; //it could be a chapter but until TY is passed through...
        }
      } 

      // Modify or try alternate RIS tag values based after cooking
      postCookProcess(cu,am,ris_type);
      
      
      // Only emit if this item is likely to be from this AU
      // protect against counting overcrawled articles by checking against
      // values from the TDB file - differentiate between book items and journal itesm
      ArchivalUnit au = cu.getArchivalUnit();
      if ( ris_type.contains("BOOK") || ris_type.contains("CHAP") ) {
        if (!BaseAtyponMetadataUtil.metadataMatchesBookTdb(au, am)) {
          return;
        }
      } else {
        // JOURNAL default is to assume it's a journal for backwards compatibility
        if (!BaseAtyponMetadataUtil.metadataMatchesTdb(au, am)) {
          return;
        }
      }

      /*
       * Fill in DOI, publisher, other information available from
       * the URL or TDB 
       * CORRECT the access.url if it is not in the AU
       */
      BaseAtyponMetadataUtil.completeMetadata(cu, am);
      emitter.emitMetadata(cu, am);
    }
    
    /*
     * isolate the modifications done on the AM after the initial extraction
     * in order to allow child plugins to do override this and do 
     * additional work before calling the pre-emit checking...
     * ArticleMetadata - passed in information from extract/cook
     * ris_type - the TY value or its inferred type (basically, book or journal)
     */
    protected void postCookProcess(CachedUrl cu, ArticleMetadata am, String ris_type) {
      /*
       * RIS data can be variable.  We don't have any way to add priority to
       * the cooking of data, so fallback to alternate values manually
       */
      if (am.get(MetadataField.FIELD_DATE) == null) {
        if (am.getRaw("Y1") != null) { // if DA wasn't there, use Y1
          am.put(MetadataField.FIELD_DATE, am.getRaw("Y1"));
        }
      }      

      /*
       * There are differences between books and journals, so fork for titles
       * and metadata check
       */ 
      if ( ris_type.contains("BOOK") || ris_type.contains("CHAP") ) {
        //BOOK in some form
        // T1 is the primary title - of the chapter for a book chapter, or book for a complete book
        // T2 is the next title up - of the book for a chapter, of the series for a book
        // T3 is the uppermost - of the series for a chapter
        //sometimes they use TI instead of T1... 
        if (am.get(MetadataField.FIELD_ARTICLE_TITLE) == null) {
          if (am.getRaw("TI") != null) { // if T1 wasn't there, use TI
            am.put(MetadataField.FIELD_ARTICLE_TITLE, am.getRaw("TI"));
          }
        }

        if (ris_type.contains("CHAP")) {
          // PROCEEDINGS ISSUE - Proceedings cannot have children of type CHAP
          // so if the publisher is declaring items as CHAP then we shouldn't be calling
          // this a proceeding, it should be a BOOK
          // Note that the addTdbDefaults may override this again 
          if (am.get(MetadataField.FIELD_PUBLICATION_TYPE) == MetadataField.PUBLICATION_TYPE_PROCEEDINGS) {
            log.debug3("Publication type PROCEEDING has child of type BOOK CHAPTER");
            am.replace(MetadataField.FIELD_PUBLICATION_TYPE, MetadataField.PUBLICATION_TYPE_BOOK);
          }
          
          // just one chapter - set the article type correctly
          am.put(MetadataField.FIELD_ARTICLE_TYPE, MetadataField.ARTICLE_TYPE_BOOKCHAPTER);
          if ((am.get(MetadataField.FIELD_PUBLICATION_TITLE) == null) && (am.getRaw("T2") != null)) {
            // the publication and the article titles are just the name of the book
            am.put(MetadataField.FIELD_PUBLICATION_TITLE, am.getRaw("T2"));
          }
          if ((am.get(MetadataField.FIELD_SERIES_TITLE) == null) && (am.getRaw("T3") != null)) {
            // the publication and the article titles are just the name of the book
            am.put(MetadataField.FIELD_SERIES_TITLE, am.getRaw("T3"));
          }
        } else {
          // We're a full book volume - articletitle = publicationtitle
          am.put(MetadataField.FIELD_ARTICLE_TYPE, MetadataField.ARTICLE_TYPE_BOOKVOLUME);
          if (am.get(MetadataField.FIELD_PUBLICATION_TITLE) == null) {
            // the publication and the article titles are just the name of the book
            am.put(MetadataField.FIELD_PUBLICATION_TITLE, am.get(MetadataField.FIELD_ARTICLE_TITLE));
          }
          // series title can be from T2
          if ((am.get(MetadataField.FIELD_SERIES_TITLE) == null) && (am.getRaw("T2") != null)) {
            // the publication and the article titles are just the name of the book
            am.put(MetadataField.FIELD_SERIES_TITLE, am.getRaw("T2"));
          }
        }
      } else {
        // JOURNAL default is to assume it's a journal for backwards compatibility
        if (am.get(MetadataField.FIELD_PUBLICATION_TITLE) == null) {
          if (am.getRaw("T2") != null) {
            am.put(MetadataField.FIELD_PUBLICATION_TITLE, am.getRaw("T2"));
          } else if (am.getRaw("JO") != null) {
            am.put(MetadataField.FIELD_PUBLICATION_TITLE, am.getRaw("JO")); // might be unabbreviated version
          }
        } 
      }
    }

  }

}
