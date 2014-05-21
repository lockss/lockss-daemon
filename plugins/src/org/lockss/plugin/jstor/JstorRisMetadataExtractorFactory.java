/*
 * $Id: JstorRisMetadataExtractorFactory.java,v 1.1 2014-05-21 18:05:19 alexandraohlson Exp $
 */

/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
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

package org.lockss.plugin.jstor;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.cxf.common.util.StringUtils;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;


/*
 * the ROLE_METADATA is a RIS citation file
 * But before collecting and emitting, make sure the corresponding pdf or 
 * full text html is in the AU
 * because the RIS url was generated from a listed DOI
 * The RIS file will exist even for a bogus DOI, it's just that the contents
 * will be blank. So do a validation check on the RIS file.
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

  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    JstorRisMetadataExtractor jris = new JstorRisMetadataExtractor();

    jris.addRisTag("JO", MetadataField.FIELD_PUBLICATION_TITLE);
    jris.addRisTag("UR", MetadataField.FIELD_ACCESS_URL);
    jris.addRisTag("Y1", MetadataField.FIELD_DATE);
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
      String doi_prefix = null;
      String doi_suffix = null;

      ArticleMetadata am = super.extract(target, cu); //extracts and cooks
      // we have metadata to emit
      if (am != null) {
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
        String full_UR = am.get(MetadataField.FIELD_ACCESS_URL);
        if ((full_UR != null)) {
          Matcher accessUrlMat = FILE_PATTERN.matcher(full_UR);
          if (accessUrlMat.matches()) {
            String filename = accessUrlMat.group(3);
            String base = cu.getArchivalUnit().getConfiguration().get("base_url") + "stable/";
            if (aFullFormExists(base, filename, cu, am)) {
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
    private static boolean aFullFormExists(String base, String filename, 
        CachedUrl cu,
        ArticleMetadata am) {
      String newUrl;
      ArchivalUnit jau = cu.getArchivalUnit();
      CachedUrl fileCu;

      //1. pdfplus?
      newUrl = base + "pdfplus/" + filename + ".pdf";
      fileCu = jau.makeCachedUrl(newUrl);
      if (fileCu != null && (fileCu.hasContent())) {
        am.put(MetadataField.FIELD_ACCESS_URL, newUrl); // will this replace???
        return true;
      }
      //2. pdf
      newUrl = base + "pdf/" + filename + ".pdf";
      fileCu = jau.makeCachedUrl(newUrl);
      if (fileCu != null && (fileCu.hasContent())) {
        am.put(MetadataField.FIELD_ACCESS_URL, newUrl); // will this replace???
        return true;
      }
      //3. full html?
      newUrl = base + "full/" + filename; // no suffix
      fileCu = jau.makeCachedUrl(newUrl);
      if (fileCu != null && (fileCu.hasContent())) {
        am.put(MetadataField.FIELD_ACCESS_URL, newUrl); // will this replace???
        return true;
      }
      return false;
    }
  }
}
