/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.atypon.ammonsscientific;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

/**  typical metadata
  <link rel="schema.DC" href="http://purl.org/DC/elements/1.0/"></link>
  <meta name="dc.Title" content="An analysis of resistance to change exposed in individuals' thoughts and behaviors"></meta>
  <meta name="dc.Creator" content=" Lena M. Forsell"></meta>
  <meta name="dc.Creator" content=" Jan A. Åström"></meta>
  <meta name="dc.Description" content="Abstract Resistance to change can be the cause of difficulty when it is either too strong or too weak. Therapy or information can be used to either strengthen or weaken resistance to change to appropriate levels. The purpose of this article is intended to disclose the relationship between resistance to change and some aspects of human behavior. Resistance to change has affective, cognitive, and behavioral components that create a psychological resistance to making a change in particular situations or overall changes in one's life, and often appears in psychotherapy and/or when organizational alterations are underway. Four subfactors of resistance to change have been found and are related to extraversion and neuroticism in the  “Big Five” personality model. Much indicates that the development of resistance to change begins early in childhood and may be neurophysiologically founded. It can be traced in both macro and micro gestures in body language and is believed to influence general health. Whereas previou..."></meta>
  <meta name="dc.Publisher" content=" Ammons Scientific, Ltd.  P.O. Box 9229, Missoula, MT 59807-9229 USA  "></meta>
  <meta name="dc.Date" scheme="WTN8601" content="2012-12-26"></meta>
  <meta name="dc.Type" content="research-article"></meta>
  <meta name="dc.Format" content="text/HTML"></meta>
  <meta name="dc.Language" content="en"></meta>
  <meta name="dc.Coverage" content=" P.O. Box 9229, Missoula, MT 59807-9229 USA "></meta>
  <meta name="dc.Rights" content="© Jan A. Åström 2012"></meta>
 */
public class ClockssAmmonsScientificHtmlMetadataExtractorFactory
  implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger("ClockssAmmonsScientificMetadataExtractorFactory");
  
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                           String contentType)
      throws PluginException {
    return new ClockssAmmonsMetadataExtractor();
  }
  
  public static class ClockssAmmonsMetadataExtractor 
    implements FileMetadataExtractor {

    // Map Ammons Scientific-specific HTML meta tag names to cooked metadata fields
    private static MultiMap tagMap = new MultiValueMap();
    static { 
      tagMap.put("dc.Title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("dc.Creator", MetadataField.FIELD_AUTHOR);
      tagMap.put("dc.Publisher", MetadataField.FIELD_PUBLISHER);
      tagMap.put("dc.Date", MetadataField.FIELD_DATE);
      tagMap.put("dc.Format", MetadataField.FIELD_FORMAT);
      tagMap.put("dc.Language", MetadataField.FIELD_LANGUAGE);
      tagMap.put("dc.Coverage", MetadataField.FIELD_COVERAGE);
      tagMap.put("dc.Type", MetadataField.DC_FIELD_TYPE);
      tagMap.put("dc.Rights", MetadataField.DC_FIELD_RIGHTS);
      tagMap.put("dc.Description", MetadataField.DC_FIELD_DESCRIPTION);
    }

    /**
     * Use SimpleHtmlMetaTagMetadataExtractor to extract raw metadata, map
     * to cooked fields
     */ 
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
      throws IOException {
        log.debug("The MetadataExtractor attempted to extract metadata from cu: "+cu);
      ArticleMetadata am = 
        new SimpleHtmlMetaTagMetadataExtractor().extract(target, cu);
      am.cook(tagMap);
      
      // handle journal.title
      String copyRightStr = am.getRaw("dc.Rights");
      if (copyRightStr != null) {
        log.debug3("copyRightStr: "+ copyRightStr);
        // unicode of copyright symbol is \u00a9
        // dc.Rights: "© Perceptual and Motor Skills 2011"
        Pattern copyRightPat = Pattern.compile("(\\u00a9\\s)(.+)(\\s\\d{4})");
        Matcher mat = copyRightPat.matcher(copyRightStr);
        if (mat.find()) {
          log.debug3("found match for copyRightStr");
          String journalTitle = mat.replaceFirst("$2");
          log.debug3("journalTitle: " + journalTitle);
          am.put(MetadataField.FIELD_JOURNAL_TITLE, mat.replaceFirst("$2"));
        }
      }
      emitter.emitMetadata(cu, am);
    }
  }
}