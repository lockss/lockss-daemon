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

package org.lockss.plugin.casaeditriceclueb;

import java.io.*;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;
import java.util.*;
import org.apache.commons.collections.map.*;
import javax.xml.xpath.XPathExpressionException;

/**
 * This file implements a FileMetadataExtractor for Casa Editrice Clue
 * B Source .xml files
 * 
 * Files used to write this class constructed from Clueb FTP archive:
 * ~/2010/CLUEB_CHAPTERS.zip!/8849112416/10.1400_52474.xml
 */

public class CasaEditriceCluebSourceXmlMetadataExtractorFactory
  implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger("CluebXmlMetadataExtractor");

  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
							   String contentType)
      throws PluginException {
    return new CluebXmlMetadataExtractor();
  }

  public static class CluebXmlMetadataExtractor 
    implements FileMetadataExtractor {
	    
	    /** Map of raw xpath key to node value function */
	    static private final Map<String,XPathValue> nodeMap = 
	        new HashMap<String,XPathValue>();
	    static {
	      nodeMap.put("/ONIXDOIMonographChapterWorkRegistrationMessage/DOIMonographChapterWork/ContentItem/Contributor/PersonNameInverted", XmlDomMetadataExtractor.TEXT_VALUE);
	      nodeMap.put("/ONIXDOIMonographChapterWorkRegistrationMessage/DOIMonographChapterWork/DOI", XmlDomMetadataExtractor.TEXT_VALUE);
	      nodeMap.put("/ONIXDOIMonographChapterWorkRegistrationMessage/DOIMonographChapterWork/MonographicPublication/MonographicWork/Title/TitleText", XmlDomMetadataExtractor.TEXT_VALUE);
              nodeMap.put("/ONIXDOIMonographChapterWorkRegistrationMessage/DOIMonographChapterWork/MonographicPublication/MonographicProduct/ProductIdentifier/IDValue", XmlDomMetadataExtractor.TEXT_VALUE);
	      nodeMap.put("/ONIXDOIMonographChapterWorkRegistrationMessage/DOIMonographChapterWork/MonographicPublication/MonographicProduct/Publisher/PublisherName", XmlDomMetadataExtractor.TEXT_VALUE);
	      nodeMap.put("/ONIXDOIMonographChapterWorkRegistrationMessage/DOIMonographChapterWork/ContentItem/Title/TitleText", XmlDomMetadataExtractor.TEXT_VALUE);
	      nodeMap.put("/ONIXDOIMonographChapterWorkRegistrationMessage/DOIMonographChapterWork/ContentItem/PublicationDate", XmlDomMetadataExtractor.TEXT_VALUE);
	      nodeMap.put("/ONIXDOIMonographChapterWorkRegistrationMessage/DOIMonographChapterWork/ContentItem/SequenceNumber", XmlDomMetadataExtractor.TEXT_VALUE);
	    }

	    /** Map of raw xpath key to cooked MetadataField */
	    static private final MultiValueMap xpathMap = new MultiValueMap();
	    static {
	      // normal journal article schema
              xpathMap.put("/ONIXDOIMonographChapterWorkRegistrationMessage/DOIMonographChapterWork/ContentItem/Contributor/PersonNameInverted", MetadataField.FIELD_AUTHOR);
	      xpathMap.put("/ONIXDOIMonographChapterWorkRegistrationMessage/DOIMonographChapterWork/DOI", MetadataField.FIELD_DOI);
	      xpathMap.put("/ONIXDOIMonographChapterWorkRegistrationMessage/DOIMonographChapterWork/MonographicPublication/MonographicWork/Title/TitleText", MetadataField.FIELD_JOURNAL_TITLE);
              xpathMap.put("/ONIXDOIMonographChapterWorkRegistrationMessage/DOIMonographChapterWork/MonographicPublication/MonographicProduct/ProductIdentifier/IDValue", MetadataField.FIELD_ISBN);
	      xpathMap.put("/ONIXDOIMonographChapterWorkRegistrationMessage/DOIMonographChapterWork/MonographicPublication/MonographicProduct/Publisher/PublisherName", MetadataField.FIELD_PUBLISHER);
	      xpathMap.put("/ONIXDOIMonographChapterWorkRegistrationMessage/DOIMonographChapterWork/ContentItem/Title/TitleText", MetadataField.FIELD_ARTICLE_TITLE);
	      xpathMap.put("/ONIXDOIMonographChapterWorkRegistrationMessage/DOIMonographChapterWork/ContentItem/PublicationDate", MetadataField.FIELD_DATE);
	      xpathMap.put("/ONIXDOIMonographChapterWorkRegistrationMessage/DOIMonographChapterWork/ContentItem/SequenceNumber", MetadataField.FIELD_START_PAGE);
	    }

	    /**
	     * Use XmlMetadataExtractor to extract raw metadata, map
	     * to cooked fields, then extract extra tags by reading the file.
	     * 
	     * @param target the MetadataTarget
	     * @param cu the CachedUrl from which to read input
	     * @param emitter the emiter to output the resulting ArticleMetadata
	     */
	    @Override
	    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
	        throws IOException, PluginException {
	      log.debug3("The MetadataExtractor attempted to extract metadata from cu: "+cu);
	      ArticleMetadata am = do_extract(target, cu, emitter);
	      emitter.emitMetadata(cu,  am);
	    }

	    /**
	       * Use XmlMetadataExtractor to extract raw metadata, map
	       * to cooked fields, then extract extra tags by reading the file.
	       * 
	       * @param target the MetadataTarget
	       * @param in the Xml input stream to parse
	       * @param emitter the emitter to output the resulting ArticleMetadata
	       */
	      public ArticleMetadata do_extract(MetadataTarget target, CachedUrl cu, Emitter emit)
	          throws IOException, PluginException {
	        try {
	          ArticleMetadata am = 
	            new XmlDomMetadataExtractor(nodeMap).extract(target, cu);
	          am.cook(xpathMap);
	          return am;
	        } catch (XPathExpressionException ex) {
	          PluginException ex2 = new PluginException("Error parsing XPaths");
	          ex2.initCause(ex);
	          throw ex2;
	        }
	      }
	  }
}