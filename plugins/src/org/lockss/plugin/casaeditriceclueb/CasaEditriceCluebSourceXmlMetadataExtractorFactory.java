/*
 * $Id$
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