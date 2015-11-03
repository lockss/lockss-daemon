/*
 * $Id:$
 */

/*

 Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
 all rights reserved.

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of his software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 Except as contained in this notice, the name of Stanford University shall not
 be used in advertising or otherwise to promote the sale, use or other dealings
 in this Software without prior written authorization from Stanford University.

 */

package org.lockss.plugin.atypon.endocrinesociety;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.*;

import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.atypon.BaseAtyponRisMetadataExtractorFactory.BaseAtyponRisMetadataExtractor;
import org.lockss.util.Logger;

/*
 * A subclass of the BaseAtypon version to handle weirdness in the Endocrine Books
 * RIS metadata. 
 * They provide an ISBN using the "SN" key but it isn't actually the isbn or eisbn
 *  So for now, don't use this value and instead pull the value from the DOI
 *  which seems to be of the form XXYZ.eisbn
 *  BOOKS ONLY - so just fall over if it is a journal article
 */
public class EndocrineRisMetadataExtractorFactory implements FileMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(EndocrineRisMetadataExtractorFactory.class);
  
  //(?:\/|%2F) is a non-capturing group for "/" or its normalized %2F version
  private static final Pattern URL_ISBN_PATTERN = Pattern.compile("action/downloadCitation\\?doi=[.0-9]+(?:\\/|%2F)[^.]+\\.([0-9X]{13})");

  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {

    log.debug3("Inside Base Atypon Metadata extractor factory for RIS files");

    EndocrineRisMetadataExtractor endo_ris = new EndocrineRisMetadataExtractor();

    endo_ris.addRisTag("A1", MetadataField.FIELD_AUTHOR);
    // Do not use UR listed in the ris file! It will get set to full text CU by daemon
    return endo_ris;
  }

  public static class EndocrineRisMetadataExtractor
  extends BaseAtyponRisMetadataExtractor {


    /*
     * (non-Javadoc)
     * Endocrine seems to put bogus values in the SN field for books 
     * so pull a valid EISBN from the doi used in the url
     * otherwise the pre-emit check will fail...
     */
    @Override
    protected void postCookProcess(CachedUrl cu, ArticleMetadata am, String ris_type)  {

      // first do the regular stuff
      super.postCookProcess(cu,am,ris_type);
      // and then do a little extra
      if ( ris_type.contains("BOOK") || ris_type.contains("CHAP")) {
        String ris_url = cu.getUrl();
        log.debug3("book/chap ris and url is: " + ris_url);
        Matcher isbn_match = URL_ISBN_PATTERN.matcher(ris_url);
        if (isbn_match.find()) {
          String url_isbn = isbn_match.group(1);
          log.debug3("replacing ris isbn of: " + am.getRaw("SN") + " with " + url_isbn);
          // the isbn will still be wrong, but that's the way it goes...
          am.put(MetadataField.FIELD_EISBN,  url_isbn);
          // we have no way of removing this incorrect isbn, we'll have to leave it
        }
       // if not, there's nothing we can do but let it proceed and possibly not emit
      }
    }

  }

}
