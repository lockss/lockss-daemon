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

package org.lockss.plugin.iumj;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.collections.map.MultiValueMap;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.extractor.XmlDomMetadataExtractor.TextValue;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;
import org.lockss.plugin.*;


/**
 * This class implements a FileMetadataExtractorFactory for IUMJ content
 */
public class IUMJXmlMetadataExtractorFactory
implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger("IUMJXmlMetadataExtractorFactory");
  
  @Override 
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
    static private final Pattern exPat = Pattern.compile(
        "Indiana Univ. Math. J. ([^ ]+) [(][0-9]{4}[)] ([^ ]+) - ([^ ]+)");
    
    // extend XPathValue, needed because some fields have extra CR chars
    static private final XPathValue TRIM_VALUE = new TextValue() {
      @Override
      public String getValue(String s) {
        s = s.replace("\n", " ");
        s = s.trim();
        return s;
      }
    };
    
    
    /*  The following Map maps raw xpath keys to raw node values 
     *  the first value in the put are XPath expressions that search the XML  
     *  file for the named metadata, for example:
     *  "//*[name()='dc:title']" select all nodes whose name='dc:title'
     */
    
    static private final Map<String,XPathValue> nodeMap = 
        new HashMap<String,XPathValue>();
    static {
      // Our XmlDomMetadataExtractor doesn't support namespaces, so just
      // find any node in the tree with the name that we want
      nodeMap.put("//*[name()='dc:title']", TRIM_VALUE);
      nodeMap.put("//*[name()='dc:creator']", TRIM_VALUE);
      nodeMap.put("//*[name()='dc:description']", TRIM_VALUE);
      nodeMap.put("//*[name()='dc:publisher']", TRIM_VALUE);
      nodeMap.put("//*[name()='dc:date']", TRIM_VALUE);
      nodeMap.put("//*[name()='dc:type']", TRIM_VALUE);
      nodeMap.put("//*[name()='dc:format']", TRIM_VALUE);
      nodeMap.put("//*[name()='dc:identifier']", TRIM_VALUE);
      nodeMap.put("//*[name()='dc:source']", TRIM_VALUE);
      nodeMap.put("//*[name()='dc:language']", TRIM_VALUE);
      nodeMap.put("//*[name()='dc:relation']", TRIM_VALUE);
      nodeMap.put("//*[name()='dc:coverage']", TRIM_VALUE);
      nodeMap.put("//*[name()='dc:rights']", TRIM_VALUE);
    }
    
    /*  The following Map maps raw xpath keys to cooked MetadataField
     *  the first value in the put are XPath expressions that search the XML 
     *  file for the named metadata; here it's stored as both dc and FIELD_*
     */
    static private final MultiValueMap xpathMap = new MultiValueMap();
    static {
      // Journal article schema
      xpathMap.put("//*[name()='dc:title']", MetadataField.DC_FIELD_TITLE);
      xpathMap.put("//*[name()='dc:title']", MetadataField.FIELD_ARTICLE_TITLE);
      xpathMap.put("//*[name()='dc:creator']", MetadataField.DC_FIELD_CREATOR);
      xpathMap.put("//*[name()='dc:creator']", MetadataField.FIELD_AUTHOR);
      xpathMap.put("//*[name()='dc:description']", MetadataField.DC_FIELD_DESCRIPTION);
      xpathMap.put("//*[name()='dc:publisher']", MetadataField.DC_FIELD_PUBLISHER);
      // get publisher name from tdb, as the metadata was inconsistent
      // xpathMap.put("//*[name()='dc:publisher']", MetadataField.FIELD_PUBLISHER);
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
      xpathMap.put("//*[name()='dc:identifier']", MetadataField.FIELD_DOI);
      xpathMap.put("scraped_oaicite_issue", MetadataField.FIELD_ISSUE);
      xpathMap.put("//*[name()='dc:relation']", new MetadataField(
          MetadataField.FIELD_VOLUME, MetadataField.extract(exPat, 1)));
      xpathMap.put("//*[name()='dc:relation']", new MetadataField(
          MetadataField.FIELD_START_PAGE, MetadataField.extract(exPat, 2)));
      xpathMap.put("//*[name()='dc:relation']", new MetadataField(
          MetadataField.FIELD_END_PAGE, MetadataField.extract(exPat, 3)));
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
        ArticleMetadata am = null;
        Pattern patternUrl = Pattern.compile(
            "META/(.+)/([0-9]+)([0-9]{3})[.]xml");
        Matcher urlmat = patternUrl.matcher(cu.getUrl());
        if (!urlmat.find()) {
          Exception ex = new Exception(
              "Error: metatdata pattern does not match " + cu.getUrl());
          throw ex;
        }
        String citeUrl = urlmat.replaceFirst("oai/$1/$2/$2$3/$2$3.html");
        CachedUrl citeCu = cu.getArchivalUnit().makeCachedUrl(citeUrl); 
        
        am = new XmlDomMetadataExtractor(nodeMap).extract(target, cu);
        
        BufferedReader bReader = null;
        try {
          bReader = new BufferedReader(citeCu.openForReading());
          
          //<span  class="ent">     issue</span> = 1,
          Pattern patternIssue = Pattern.compile(
              "<span [^>]+>[^<]*issue[^<]*</span> = ([^,]*),",
              Pattern.CASE_INSENSITIVE);
          // go through the cached URL content line by line
          for (String line = bReader.readLine(); line != null; line = bReader.readLine()) {
            Matcher matcher = patternIssue.matcher(line);
            if (matcher.find()) {
              am.putRaw("scraped_oaicite_issue", matcher.group(1).trim());
              break;
            }
          }
        } catch (Exception e) {
          log.debug(e + " : Missing/malformed cite");
        }
        finally {
          IOUtil.safeClose(bReader);
          AuUtil.safeRelease(citeCu);
        }
        
        am.cook(xpathMap);
        emitter.emitMetadata(cu,  am);
        
      } catch (XPathExpressionException ex) {
        PluginException ex2 = new PluginException("Error parsing XPaths");
        ex2.initCause(ex);
        throw ex2;
      } catch (Exception e) {
        log.debug(e + " : Missing/malformed metadata");
      }
      finally {
        AuUtil.safeRelease(cu);
      }
    }
  }
  
}
