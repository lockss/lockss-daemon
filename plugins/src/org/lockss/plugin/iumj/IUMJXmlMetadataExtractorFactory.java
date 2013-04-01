/*
 * $Id: IUMJXmlMetadataExtractorFactory.java,v 1.2 2013-04-01 00:42:54 tlipkis Exp $
 */

/*

 Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.iumj;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.collections.map.MultiValueMap;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;
import org.lockss.plugin.*;


/**
 * This class implements a FileMetadataExtractorFactory for IUMJ content
 */
  public class IUMJXmlMetadataExtractorFactory
    implements FileMetadataExtractorFactory {
    static Logger log = 
      Logger.getLogger("IUMJXmlMetadataExtractorFactory");

  public FileMetadataExtractor createFileMetadataExtractor(
      MetadataTarget target, String contentType) throws PluginException {
    return new IUMJXmlMetadataExtractor();
  }

/**
 * This class implements a FileMetadataExtractor for IUMJ content.
 */
  public static class IUMJXmlMetadataExtractor 
    implements FileMetadataExtractor {
    
    
    // Indices for volume, start page, and end page
    // Format: 48 (1999) 139 - 154
    protected static final int VOL_INDEX = 0;
    protected static final int SPAGE_INDEX = 2;
    protected static final int EPAGE_INDEX = 4;
    
    /** Map of raw xpath key to node value function */
    
    static private final Map<String,XPathValue> nodeMap = 
        new HashMap<String,XPathValue>();
    static {
      // Our XmlDomMetadataExtractor doesn't support namespaces, so just
      // find any node in the tree with the name that we want
      nodeMap.put("//*[name()='dc:title']", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("//*[name()='dc:creator']", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("//*[name()='dc:description']", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("//*[name()='dc:publisher']", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("//*[name()='dc:date']", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("//*[name()='dc:type']", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("//*[name()='dc:format']", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("//*[name()='dc:identifier']", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("//*[name()='dc:source']", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("//*[name()='dc:language']", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("//*[name()='dc:relation']", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("//*[name()='dc:coverage']", XmlDomMetadataExtractor.TEXT_VALUE);
      nodeMap.put("//*[name()='dc:rights']", XmlDomMetadataExtractor.TEXT_VALUE);
    }

    /** Map of raw xpath key to cooked MetadataField */
    
    static private final MultiValueMap xpathMap = new MultiValueMap();
    static {
      // Journal article schema
      xpathMap.put("//*[name()='dc:title']", MetadataField.DC_FIELD_TITLE);
      xpathMap.put("//*[name()='dc:title']", MetadataField.FIELD_ARTICLE_TITLE);
      xpathMap.put("//*[name()='dc:creator']", MetadataField.DC_FIELD_CREATOR);
      xpathMap.put("//*[name()='dc:creator']", MetadataField.FIELD_AUTHOR);
      xpathMap.put("//*[name()='dc:description']", MetadataField.DC_FIELD_DESCRIPTION);
      xpathMap.put("//*[name()='dc:publisher']", MetadataField.DC_FIELD_PUBLISHER);
      xpathMap.put("//*[name()='dc:publisher']", MetadataField.FIELD_PUBLISHER);
      xpathMap.put("//*[name()='dc:date']", MetadataField.DC_FIELD_DATE);
      xpathMap.put("//*[name()='dc:date']", MetadataField.FIELD_DATE);
      xpathMap.put("//*[name()='dc:type']", MetadataField.DC_FIELD_TYPE);
      xpathMap.put("//*[name()='dc:format']", MetadataField.DC_FIELD_FORMAT);
      xpathMap.put("//*[name()='dc:identifier']", MetadataField.DC_FIELD_IDENTIFIER);
      xpathMap.put("//*[name()='dc:source']", MetadataField.DC_FIELD_SOURCE);
      xpathMap.put("//*[name()='dc:language']", MetadataField.DC_FIELD_LANGUAGE);
      xpathMap.put("//*[name()='dc:relation']", MetadataField.DC_FIELD_RELATION);
      xpathMap.put("//*[name()='dc:coverage']", MetadataField.DC_FIELD_COVERAGE);
      xpathMap.put("//*[name()='dc:rights']", MetadataField.DC_FIELD_RIGHTS);
    }

    /**
     * Use XmlMetadataExtractor to extract raw metadata, map
     * to cooked fields, then extract extra tags by reading the file.
     * 
     * @param target the MetadataTarget
     * @param cu the CachedUrl from which to read input
     * @param emitter the emitter to output the resulting ArticleMetadata
     */
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
        throws IOException, PluginException {
      try {
        ArticleMetadata am;
        try {
          String xmlUrl = cu.getUrl().replaceFirst("IUMJ/FTDLOAD/([^/]+)/[^/]+/([^/]+)/pdf", "META/$1/$2\\.xml");
          CachedUrl xmlCu = cu.getArchivalUnit().makeCachedUrl(xmlUrl);
	  try {
	    am = new XmlDomMetadataExtractor(nodeMap).extract(target, xmlCu);
	  } finally {
	    AuUtil.safeRelease(xmlCu);
	  }
        } finally {
          AuUtil.safeRelease(cu);
        }
        
        am.cook(xpathMap);
        
        String line = am.get(MetadataField.DC_FIELD_RELATION);
        
        if (line != null) {
          addVolumeAndPages(line, am);
        }
        
        emitter.emitMetadata(cu,  am);
        
      } catch (XPathExpressionException ex) {
        PluginException ex2 = new PluginException("Error parsing XPaths");
        ex2.initCause(ex);
        throw ex2;
      }
    }
    
    protected void addVolumeAndPages(String line, ArticleMetadata ret) {
      String flag = "Indiana Univ. Math. J. ";
      int index = StringUtil.indexOfIgnoreCase(line, flag);
      
      if (index <= 0) {
        log.debug(line + ": flag \"" + flag + "\" not found");
        return;
      }
          
      try {
        index += flag.length();
        String volAndPages = line.substring(index, line.length());
        
        Vector<String> volAndPagesVector = StringUtil.breakAt(volAndPages, ' ');
        
        String vol = volAndPagesVector.elementAt(VOL_INDEX);
        String spage = volAndPagesVector.elementAt(SPAGE_INDEX);
        String epage = volAndPagesVector.elementAt(EPAGE_INDEX);
      
        ret.put(MetadataField.FIELD_VOLUME, vol);
        ret.put(MetadataField.FIELD_START_PAGE, spage);
        ret.put(MetadataField.FIELD_END_PAGE, epage);
        
      } catch (Exception ex) {
        log.debug("Encountered malformed dc.relation value during" +
        		"metadata extraction: " + line);
      }
    }
  }
}