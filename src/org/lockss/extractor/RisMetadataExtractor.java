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

package org.lockss.extractor;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

import java.io.*;

import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.util.*;

/**
 * Pulls metadata from a .ris file in the format
 * TY - JOUR
 * T1 - Article Tile of the Article
 * ...
 * first line of data should always be the TY (reference type)
 * there may be empty lines before the first line
 */
public class RisMetadataExtractor implements FileMetadataExtractor {
	
	static Logger log = Logger.getLogger(RisMetadataExtractor.class);
	private MultiMap risTagToMetadataField;
	protected static final String REFTYPE_JOURNAL = "Journal";
	protected static final String REFTYPE_BOOK = "Book";
	protected static final String REFTYPE_OTHER = "Other";
	private String delimiter = "-";
	
	/**
	 *Create a RisMetadataExtractor with a default RIS Type To MetadataField map
	 */
	public RisMetadataExtractor(){
		risTagToMetadataField = new MultiValueMap();
		risTagToMetadataField.put("T1", MetadataField.FIELD_ARTICLE_TITLE);
		risTagToMetadataField.put("AU", MetadataField.FIELD_AUTHOR);
		risTagToMetadataField.put("JF", MetadataField.FIELD_PUBLICATION_TITLE);
		risTagToMetadataField.put("DO", MetadataField.FIELD_DOI);
		risTagToMetadataField.put("PB", MetadataField.FIELD_PUBLISHER);
		risTagToMetadataField.put("VL", MetadataField.FIELD_VOLUME);
		risTagToMetadataField.put("IS", MetadataField.FIELD_ISSUE);
		risTagToMetadataField.put("SP", MetadataField.FIELD_START_PAGE);
		risTagToMetadataField.put("EP", MetadataField.FIELD_END_PAGE);
		risTagToMetadataField.put("DA", MetadataField.FIELD_DATE);
	}
	
	
	/**
	 * Create a RisMetadataExtractor with a RIS tag To MetadataField map of fieldMap
	 * overriding default fieldMap
	 * @param fieldMap
	 */
	public RisMetadataExtractor(MultiValueMap fieldMap){
		risTagToMetadataField = fieldMap;
	}
	
	/**
	 * Create a RisMetadataExtractor with a RIS tag To MetadataField map of default
	 * fieldMap and adding the specified metadata field Ris tag pair
	 * @param risTag
	 * @param field
	 */
	public RisMetadataExtractor(String risTag, MetadataField field){
		this();
		addRisTag(risTag, field);
	}
	
	/**
	 * Add the specified metadata field Ris tag pair to the RIS tag To MetadataField map
	 * @param risTag
	 * @param field
	 */
	public void addRisTag(String risTag, MetadataField field){
		risTagToMetadataField.put(risTag, field);
	}
	
	/**
	 * Remove entry associated with the Ris tag from the the RIS tag To MetadataField map
	 * @param risTag
	 */
	public void removeRisTag(String risTag){
		risTagToMetadataField.remove(risTag);
	}
	
	/**
	 * Check for the Ris tag in the the RIS tag To MetadataField map
	 * @param risTag
	 */
	public boolean containsRisTag(String risTag){
		return risTagToMetadataField.containsKey(risTag);
	}
	
	/*	
   	public void setdelimiter(String delim){
		delimiter = delim;
	}
	*/
	
	@Override
	public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
	    throws IOException, PluginException {
	  ArticleMetadata md = extract(target, cu);
	  if (md != null) {
	    emitter.emitMetadata(cu, md);
	  }
	}	

	/**
	 * Extract metadata from the content of the cu, which should be an RIS file.
	 * Reads line by line inserting the 2 character code and value into the raw map.
	 * The first line should be a material type witch if it is book or journal will 
	 * determine if we interpret the SN tag as IS beltSN or ISBN.
	 * @param target
	 * @param cu
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
	    	if(!containsRisTag("TY")){
 	    	            String value = null;
			    while(refType == null && (line = bReader.readLine()) != null) {
			    	if(line.trim().toUpperCase().startsWith("TY") && line.contains(delimiter) && !line.endsWith(delimiter)) {
			    		value = line.substring(line.indexOf(delimiter) + 1).trim().toUpperCase();
			    		if(value.contentEquals("JOUR")) {
			    			refType = REFTYPE_JOURNAL;
			    		}
			    		else if(value.contentEquals("BOOK") || value.contentEquals("CHAP")) {
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
			    md.putRaw("TY", value);
	    	}
	        while ((line = bReader.readLine()) != null) {
	        	line = line.trim();
	        	if(!line.contentEquals("") && line.contains(delimiter)  && !line.endsWith(delimiter)){
	        		String value = line.substring(line.indexOf(delimiter) + 1);
	        		String key = line.substring(0,line.indexOf(delimiter) - 1);
        			key = key.trim().toUpperCase();
        			md.putRaw(key, value.trim());
        			if(!containsRisTag("SN") && key.contentEquals("SN")){
        				if(refType.contentEquals(REFTYPE_BOOK)) {
        					addRisTag("SN", MetadataField.FIELD_ISBN);
        				}
        				else {
        					addRisTag("SN", MetadataField.FIELD_ISSN);
        				}
        			} 
	        	}
	        }
	    } finally {
	      IOUtil.safeClose(bReader);
	    }
	    md.cook(risTagToMetadataField);
		return md;
	  }

}
