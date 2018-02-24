/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.georgthiemeverlag;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang.StringUtils;
import org.lockss.util.*;
import org.lockss.config.TdbAu;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;


/**
 * One of the articles used to get the html source for this plugin is:
 * https://www.thieme-connect.de/ejournals/abstract/10.1055/s-0029-1214947
 */
public class GeorgThiemeVerlagHtmlMetadataExtractorFactory implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger(GeorgThiemeVerlagHtmlMetadataExtractorFactory.class);

  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
      throws PluginException {
    return new GeorgThiemeVerlagHtmlMetadataExtractor();
  }
  
  public static class GeorgThiemeVerlagHtmlMetadataExtractor 
    implements FileMetadataExtractor {
   
   private static final Pattern PROTOCOL_PATTERN = Pattern.compile("^(e)?is(b|s)n:(\\s)*", Pattern.CASE_INSENSITIVE);
	  
    private static MultiMap tagMap = new MultiValueMap();
    static {
      //<meta name="citation_doi" content="10.1055/s-0029-1214474"/>
      tagMap.put("citation_doi", MetadataField.FIELD_DOI);
      //<meta name="citation_publication_date" content="2009/04/27"/>
      tagMap.put("citation_publication_date", MetadataField.FIELD_DATE);
      //<meta name="citation_title" content="Medikamentöse Systemtherapien der Psoriasis"/>
      tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      //<meta name="citation_issn" content="0340-2541"/>
      tagMap.put("citation_issn", MetadataField.FIELD_ISSN);
      //<meta name="citation_volume" content="36"/>
      tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
      //<meta name="citation_issue" content="04"/>
      tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);
      //<meta name="citation_firstpage" content="142"/>
      tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
      //<meta name="citation_author" content="R. Mössner"/>
      tagMap.put("citation_author",MetadataField.FIELD_AUTHOR);
      //<meta name="citation_journal_title" content="Aktuelle Dermatologie" />
      tagMap.put("citation_journal_title", MetadataField.FIELD_PUBLICATION_TITLE);
      //<meta name="citation_publisher" content="..."/> 
      // TDB publisher value will be used by default for citation_publisher (PD-440)
      //<meta name="citation_language" content="de" />
      tagMap.put("citation_language", MetadataField.FIELD_LANGUAGE);
    }
    
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
        throws IOException {
      ArticleMetadata am = new SimpleHtmlMetaTagMetadataExtractor().extract(target, cu);
      am.cook(tagMap);
      
      
      // Only emit if this item is likely to be from this AU
      // Thieme overcrawled a bunch in the past caused contaminated metadata in the DB
      ArchivalUnit au = cu.getArchivalUnit();
      if (!metadataMatchesTdb(cu,au, am)) {
        return;
      }
      
      // PD-440 hardcode publisher value
      if (cu.getArchivalUnit().getTitleConfig() == null) {
        am.replace(MetadataField.FIELD_PUBLISHER, "Georg Thieme Verlag KG");
      }
      emitter.emitMetadata(cu, am);
    }
    
    /*
     * Do some checking against the collected metadata
     * If we can get issn/eissn  isbn/eisbn info then check against that
     * We can't check volume name for Thieme because the AU uses year as param[volume_name]
     * not the known volume number
     */
    private static boolean metadataMatchesTdb(CachedUrl cu, ArchivalUnit au, 
  		  ArticleMetadata am) { 

  	  boolean isInAu = true;
  	  String stringUrl = cu.getUrl();

  	  Boolean isBook = (stringUrl.contains("ebooks")) ? true : false;

  	  TdbAu tdbau = au.getTdbAu();
  	  //
  	  // ISSN or ISBN check
  	  //
  	  String foundEID, foundPID;
  	  String AU_EID, AU_PID;
  	  if (isBook) {
  		  foundEID = normalize_id(am.get(MetadataField.FIELD_EISBN));
  		  foundPID = normalize_id(am.get(MetadataField.FIELD_ISBN));
  		  AU_PID = (tdbau == null) ? null : normalize_id(tdbau.getPrintIsbn());
  		  AU_EID = (tdbau == null) ? null : normalize_id(tdbau.getEisbn());
  	  } else {
  		  foundEID = normalize_id(am.get(MetadataField.FIELD_EISSN));
  		  foundPID = normalize_id(am.get(MetadataField.FIELD_ISSN));
  		  AU_PID = (tdbau == null) ? null : normalize_id(tdbau.getPrintIssn());
  		  AU_EID = (tdbau == null) ? null : normalize_id(tdbau.getEissn());		
  	  }
  	  // not much we can do
  	  if (StringUtils.isEmpty(AU_EID) && StringUtils.isEmpty(AU_PID)) {
  		  return isInAu;
  	  }
  	  // We know we have at least one ID value from the tdb file 
  	  // Now make sure that we have two values from either the TDB 
  	  // or the metadata - if we only have one from each source, then 
  	  // we can't use this check - could be pissn vs eissn
  	  if (!(StringUtils.isEmpty(AU_PID) || StringUtils.isEmpty(AU_EID)) ||
  			!(StringUtils.isEmpty(foundPID) || StringUtils.isEmpty(foundEID))) {
  	      // we know that one pair has two values
  		  if (foundEID != null) { 
  			  if (!(foundEID.equals(AU_EID) || foundEID.equals(AU_PID)) ) {
  				  return false;
  			  }
  		  } else if (foundPID != null) {
  			  // there wasn't an EISSN, so let's check the ISSN
  			  if (!(foundPID.equals(AU_PID) || foundEID.equals(AU_EID)) ) {
  				  return false;
  			  }
  		  } 
  	  }
  	  // If we've come this far without failing and we're a book, good enough
  	  // not sure if a title/publication title check is viable, and no volume/issue
  	  if (isBook) { return isInAu; };

  	  String foundVolume = am.get(MetadataField.FIELD_VOLUME);

  	  // If we found no volume info, nothing further to check
  	  if (StringUtils.isEmpty(foundVolume)) {
  		  return isInAu; //return true, we have no way of knowing
  	  }

  	  // Get the AU's volume name from the AU properties. This must be set
  	  //TypedEntryMap tfProps = au.getProperties();
  	  // Can we get the attr volume?
  	  //String AU_volume = tfProps.getString(ConfigParamDescr.VOLUME_NAME.getKey());
  	  return isInAu;
    }

    public static String normalize_id(String id) {
  	  if (id == null) {return null;}
  	  id = id.trim().replaceAll("-", "");
  	  Matcher protocol_match = PROTOCOL_PATTERN.matcher(id);
  	  if (protocol_match.find()) {
  		  return id.substring(0, protocol_match.end());
  	  }
  	  return id;
    }
    
  }


}
