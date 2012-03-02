/*
 * $Id: AmericanInstituteOfPhysicsMetadataExtractorFactory.java,v 1.1 2012-03-02 00:58:02 dylanrhodes Exp $
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

package org.lockss.plugin.americaninstituteofphysics;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.BaseCachedUrl;


/**
 * Files used to write this class constructed from AIP FTP archive:
 * ~/2010/AIP_xml_9.tar.gz/AIP_xml_9.tar/./APPLAB/vol_96/iss_1/
 */

public class AmericanInstituteOfPhysicsMetadataExtractorFactory
  implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger("AIPMetadataExtractorFactory");

  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
							   String contentType)
      throws PluginException {
    return new AIPXmlMetadataExtractor();
  }

  public static class AIPXmlMetadataExtractor 
    implements FileMetadataExtractor {
	   
    private Pattern[] articleTags = {
    	Pattern.compile("(<title>)|(</title>)"),
    	Pattern.compile("(<author.*<fname>)(.*)(</fname>.*<surname>)(.*)(</surname></author>.*)"),
    	Pattern.compile("(<pubfront>.*issn=\")(.*)(\" jcode.*)"),
    	Pattern.compile("(.*<volume>)(.*)(</volume>.*)"),
    	Pattern.compile("(<issue printdate=\")(.*)(\">.*</issue>.*)"),
    	Pattern.compile("(<issue printdate=\".*\">)(.*)(</issue>.*)"),
    	Pattern.compile("(.*<.xpp co;blue.>)(.*)(<.xpp co;.*)"),
    	Pattern.compile("(<country>)(.*)(</country>.*)"),
    	Pattern.compile("(<addidx>)(.*)(</addidx>.*)"),
    	Pattern.compile("(<abstract>)|(</abstract>.*)"),
    	Pattern.compile("(<cpyrt>)(.*)(</cpyrt>.*)")};
    
    private String[] valueRegex = {
    		"gap", ";$2 $4","$2","$2","$2","$2","$2","$2",";$2","gap","$2"};
    
    private String[] articleValues = initArticleValues();
    
    private MetadataField[] metadataFields = {MetadataField.FIELD_ARTICLE_TITLE, MetadataField.FIELD_AUTHOR,
    		MetadataField.FIELD_ISSN, MetadataField.FIELD_VOLUME, MetadataField.FIELD_DATE, MetadataField.FIELD_ISSUE,
    		MetadataField.FIELD_DOI, MetadataField.DC_FIELD_SOURCE, MetadataField.FIELD_KEYWORDS,
    		MetadataField.DC_FIELD_DESCRIPTION, MetadataField.DC_FIELD_RIGHTS};
    
    /**
     * Use SimpleHtmlMetaTagMetadataExtractor to extract raw metadata, map
     * to cooked fields, then extract extra tags by reading the file.
     */
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
      throws IOException {
    	log.debug3("The MetadataExtractor attempted to extract metadata from cu: "+cu);

      BufferedReader bReader = new BufferedReader(cu.openForReading());
	  ArticleMetadata am = new SimpleHtmlMetaTagMetadataExtractor().extract(target, cu);
	  boolean emitted = false;

      try {
        for (String line = bReader.readLine(); line != null; line = bReader.readLine()) 
        {
          line = line.trim();
          
          if(StringUtil.startsWithIgnoreCase(line, "<body>"))
          {
        	  log.debug3("Emitting metadata for cu: "+cu.getUrl());
        	  putMetadataIn(am);
        	  emitter.emitMetadata(cu,am);
        	  emitted = true;
        	  return;
          }
          else
        	  extractFrom(line,bReader);
        }
      } finally {
    	  if(!emitted) emitter.emitMetadata(cu,am);
        IOUtil.safeClose(bReader);
      }
    }
    
    /**
     * Stores the gathered MetadataField values in the ArticleMetadata
     * so it can be emitted
     * @param am
     */
    private void putMetadataIn(ArticleMetadata am)
    {   
        for(int i = 0; i < articleValues.length; ++i)
        	if(!articleValues[i].equals(""))
        		if(valueRegex[i].contains(";"))
        			am.put(new MetadataField(metadataFields[i],MetadataField.splitAt(";")),
        					articleValues[i]);
        		else
        			am.put(metadataFields[i],articleValues[i]);
    }
 
    /**
     * Extracts metadata from a line if it contains any
     * @param line - String to extract metadata from
     */
    private void extractFrom(String line, BufferedReader reader) throws IOException
    {
    	for(int i = 0; i < articleTags.length; ++i)
    		if(articleTags[i].matcher(line).find())
    			if(!valueRegex[i].equals("gap"))
    				if(!articleTags[i].toString().contains("<author>"))
    					articleValues[i] = articleTags[i].matcher(line).replaceFirst(valueRegex[i]);
    				else
    					articleValues[i] += articleTags[i].matcher(line).replaceFirst(valueRegex[i]);
    			else
    			{
    				articleValues[i] += sanitize(line);
    				
    				if(Pattern.compile("(<title>)(.*)(</title>)").matcher(line).find() )
    					return;
    				
    				while(line != null && !articleTags[i].matcher(line).find())
    				{
    					line = reader.readLine();
    					articleValues[i] += sanitize(line);
    				}
    			}
    }
    
    private String sanitize(String str)
    {
    	return str.replaceAll("\\<.*?>","");
    }
    
    /**
     * Returns a String[] of length articleTags.length whose fields are all null
     * @return
     */
    private String[] initArticleValues()
    {
    	String[] output = new String[articleTags.length];
    	for(int i = 0; i < output.length; ++i)
    		output[i] = "";
    	return output;
    }
  }
}