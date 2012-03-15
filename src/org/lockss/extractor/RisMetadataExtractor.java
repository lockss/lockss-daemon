
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

package org.lockss.extractor;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

import java.io.*;

import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.util.*;

/*
 * Pulls metadata from a .ris file in the format
 * TY - JOUR
 * T1 - Article Tile of the Article
 * ...
 * first line of data should always be the TY (reference type)
 * there may be empty lines before the first line
 */

public class RisMetadataExtractor extends SimpleFileMetadataExtractor {
	
	static Logger log = Logger.getLogger("RisMetadataExtractor");
	private MultiMap risTypeToMetadataField;
	protected static final String REFTYPE_JOURNAL = "Journal";
	protected static final String REFTYPE_BOOK = "Book";
	protected static final String REFTYPE_OTHER = "Other";
	private String delimiter = "-";
 
	public RisMetadataExtractor(){
		risTypeToMetadataField = new MultiValueMap();
		risTypeToMetadataField.put("T1", MetadataField.FIELD_ARTICLE_TITLE);
		risTypeToMetadataField.put("AU", MetadataField.FIELD_AUTHOR);
		risTypeToMetadataField.put("JF", MetadataField.FIELD_JOURNAL_TITLE);
		risTypeToMetadataField.put("DO", MetadataField.FIELD_DOI);
		risTypeToMetadataField.put("PB", MetadataField.FIELD_PUBLISHER);
		risTypeToMetadataField.put("VL", MetadataField.FIELD_VOLUME);
		risTypeToMetadataField.put("IS", MetadataField.FIELD_ISSUE);
		risTypeToMetadataField.put("SP", MetadataField.FIELD_START_PAGE);
		risTypeToMetadataField.put("EP", MetadataField.FIELD_END_PAGE);
		risTypeToMetadataField.put("DA", MetadataField.FIELD_DATE);
	}
	
	public RisMetadataExtractor(MultiValueMap fieldMap){
		risTypeToMetadataField = fieldMap;
	}
	    
	public void addRisType(String risType, MetadataField field){
		risTypeToMetadataField.put(risType, field);
	}
	
	public boolean containsRisType(String key){
		return risTypeToMetadataField.containsKey(key);
	}
	
	/*	
   	public void setdelimiter(String delim){
		delimiter = delim;
	}
	*/
	
	  public final ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
				    													throws IOException, PluginException {
		if(cu == null) {
			throw new IllegalArgumentException();
		}
		ArticleMetadata md = new ArticleMetadata();
		BufferedReader bReader = new BufferedReader(cu.openForReading());
		String line;
		String refType = null;
	    try {
	    	if(!containsRisType("TY")){
			    while(refType == null && (line = bReader.readLine()) != null) {
			    	if(line.trim().toUpperCase().startsWith("TY") && line.contains(delimiter) && !line.endsWith(delimiter)) {
			    		String value = line.substring(line.indexOf(delimiter) + 1).trim().toUpperCase();
			    		if(value.contentEquals("JOUR")) {
			    			refType = REFTYPE_JOURNAL;
			    		}
			    		else if(value.contentEquals("BOOK")) {
			    			refType = REFTYPE_BOOK;
			    		}
			    		else {
			    			refType = REFTYPE_OTHER;
			    		}
		    		}
			    }
			    if(refType == null){
			    	return md;
			    }
	    	}
	        while ((line = bReader.readLine()) != null) {
	        	line = line.trim();
	        	if(!line.contentEquals("") && line.contains(delimiter)  && !line.endsWith(delimiter)){
	        		String value = line.substring(line.indexOf(delimiter) + 1);
	        		String key = line.substring(0,line.indexOf(delimiter) - 1);
        			key = key.trim().toUpperCase();
        			md.putRaw(key, value.trim());
        			if(!containsRisType("SN") && key.contentEquals("SN")){
        				if(refType.contentEquals(REFTYPE_BOOK)) {
        					addRisType("SN", MetadataField.FIELD_ISBN);
        				}
        				else {
        					addRisType("SN", MetadataField.FIELD_ISSN);
        				}
        			} 
	        	}
	        }
	    } finally {
	      IOUtil.safeClose(bReader);
	    }
	    md.cook(risTypeToMetadataField);
		return md;
	  }

}
