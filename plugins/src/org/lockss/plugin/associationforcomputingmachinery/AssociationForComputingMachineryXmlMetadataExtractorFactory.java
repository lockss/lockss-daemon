/*
 * $Id: AssociationForComputingMachineryXmlMetadataExtractorFactory.java,v 1.1 2012-02-28 10:07:43 dylanrhodes Exp $
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

package org.lockss.plugin.associationforcomputingmachinery;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

/**
 * Files used to write this class constructed from ACM FTP archive:
 * ~/2011/10aug2011/NEW-MAG-QUEUE-V9I7-2001562/NEW-MAG-QUEUE-V9I7-2001562.xml
 *
 */
public class AssociationForComputingMachineryXmlMetadataExtractorFactory
  implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger("ACMXmlMetadataExtractorFactory");
  

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
							   String contentType)
      throws PluginException {
	  log.debug3("An ACMXmlMetadataExtractor was initiated");
    return new ACMXmlMetadataExtractor();
  }

  public static class ACMXmlMetadataExtractor 
    implements FileMetadataExtractor {
	  
	static final int FILE_NAME = 8;
	
	static final int INVALID_TAG = -1;
	static final int REPEATED_TAG = -2;
	static final int ARTICLE_COMPLETE = -3;
	
	private List<String> journalTags = Arrays.asList(new String[]{"journal_name","issn","eissn","volume","issue","issue_date"});
    private String[] journalValues = new String[journalTags.size()];
    private List<String> articleTags = Arrays.asList(new String[]{"article_publication_date","title","page_from",
  		  	"doi_number","language","first_name","middle_name","last_name","fname"});
    private String[] articleValues = new String[articleTags.size()];
    
    private MetadataField[] metadataFields = {MetadataField.FIELD_JOURNAL_TITLE, MetadataField.FIELD_ISSN,
    		MetadataField.FIELD_EISSN, MetadataField.FIELD_VOLUME, MetadataField.FIELD_ISSUE, MetadataField.DC_FIELD_DATE,
    		MetadataField.FIELD_DATE, MetadataField.FIELD_ARTICLE_TITLE, MetadataField.FIELD_START_PAGE,
    		MetadataField.FIELD_DOI, MetadataField.DC_FIELD_LANGUAGE, MetadataField.FIELD_AUTHOR};
    
    private boolean isJournal = true;
    private int lastTag = 0;
    
    /**
     * Use SimpleHtmlMetaTagMetadataExtractor to extract raw metadata, map
     * to cooked fields, then extract extra tags by reading the file.
     */
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
      throws IOException {    	
    	log.debug("The MetadataExtractor attempted to extract metadata from cu: "+cu);

      CachedUrl metadata = cu.getArchivalUnit().makeCachedUrl(getMetadataFile(cu));
      
      if(metadata == null)
      {
    	  log.error("The metadata file does not exist or is not readable.");
    	  return;
      }
      
      BufferedReader bReader = new BufferedReader(metadata.openForReading());
      try {
        for (String line = bReader.readLine(); line != null; line = bReader.readLine()) 
        {
          line = line.trim();
          
          if(extractFrom(line) && articleValues[FILE_NAME] != null)
          {
        	  log.debug("Emitting metadata for url: "+getPdfUrl(cu));
        	  CachedUrl container = cu.getArchivalUnit().makeCachedUrl(getPdfUrl(cu));
        	  ArticleMetadata am = new ArticleMetadata();
        	  putMetadataIn(am);
        	  emitter.emitMetadata(container,am);
        	  clear(articleValues);
          }
          
        }
      } finally {
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
        for(int i = 0; i < journalTags.size(); ++i)
        	if(journalValues[i] != null)
        		am.put(metadataFields[i],journalValues[i]);
        for(int j = 0; j < articleTags.size(); ++j)
        {
        	am.put(metadataFields[j+journalTags.size()],articleValues[j]);
        	
        	if(j == articleTags.indexOf("first_name")) //No separate MetadataField for middle or last names
        		break;
        }
    }
    
    /**
     * Sets all of arr's values to null
     * @param arr
     */
    private void clear(String[] arr)
    {
    	for(int i = 0; i < arr.length; ++i)
    		arr[i] = null;
    }
    
    /**
     * Uses a CachedUrl assumed to be in the directory of the article files to
     * generate the Url of the current article
     * @param cu - pathname of a sibling of the current article
     * @return the current article's pathname
     */
    private String getPdfUrl(CachedUrl cu)
    {
        Pattern pattern = Pattern.compile("(http://clockss-ingest.lockss.org/sourcefiles/[^/]+/[\\d]+/[^/]+/)([^/]+)(/[^/]+.pdf)",Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(cu.getUrl());
        return matcher.replaceFirst("$1$2/"+articleValues[FILE_NAME]);
    }
    
    /**
     * Uses a CachedUrl assumed to be in the directory of the metadata file to find the
     * pathname for the metadata file (and return it)
     * @param cu - address of a sibling of the metadata file
     * @return the metadata file's pathname
     */
    private String getMetadataFile(CachedUrl cu)
    {
        Pattern pattern = Pattern.compile("(http://clockss-ingest.lockss.org/sourcefiles/[^/]+/[\\d]+/[^/]+/)([^/]+)(/[^/]+.pdf)",Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(cu.getUrl());
        return matcher.replaceFirst("$1$2/$2.xml");
    }
    
    /**
     * Extracts metadata from a line if it contains any
     * @param line - String to extract metadata from
     * @return true if all available tags have been filled, false otherwise
     */
    private boolean extractFrom(String line)
    {
    	int tagIndex = getTagIndex(line);
    	
    	if(tagIndex == ARTICLE_COMPLETE)
    	{
    		isJournal = false;
    		return true;
    	}
    	if(tagIndex == INVALID_TAG)
    		return false;
    	if(tagIndex == REPEATED_TAG)
    		if(isJournal)
    			journalValues[lastTag] += getContent(line);
    		else
    			articleValues[lastTag] += getContent(line);
    	else 
    	{
    		if(isJournal)
    			journalValues[tagIndex] = getContent(line);
    		else
    		{
    			if(tagIndex == articleTags.indexOf("first_name") && articleValues[tagIndex] != null)
    				articleValues[tagIndex] += "; "+getContent(line);
    			else
    				articleValues[tagIndex] = getContent(line);
    		}
    		
    		lastTag = tagIndex;
    	}
    		
    	return false;
    }
    
    /**
     * Parses out some common irrelevant text from the line (XML tags, etc.)
     * which is not wanted in the Metadatabase
     * @param line - a line from the metadata file
     * @return the line parsed from tags
     */
    private String getContent(String line)
    {
    	String output = line.substring(line.indexOf(">")+1,line.lastIndexOf("<"));
    	if(output.contains("![CDATA["))
    		return output.substring(output.indexOf("![CDATA[")+8,output.lastIndexOf("]]"));
    	else
    		return output;
    }
    
    /**
     * Returns the index of the current line's tag in the tag List or
     * some other value if the current tag is special in some way
     * @param line
     * @return
     */
    private int getTagIndex(String line)
    {
    	if(line.indexOf("<") < 0 || line.indexOf(">") < 0)
    		return REPEATED_TAG;
    	
    	String tag = line.substring(line.indexOf("<")+1,line.indexOf(">"));
    	
    	if(tag.equals("article_rec"))
    		return ARTICLE_COMPLETE;
    	if(tag.equals("middle_name") || tag.equals("last_name"))
    		return REPEATED_TAG;
    	
    	if(isJournal)
    		if(journalTags.contains(tag))
    			return journalTags.indexOf(tag);
    		else
    			return INVALID_TAG;
    	else
    		if(articleTags.contains(tag))
    			return articleTags.indexOf(tag);	
    		else
    			return INVALID_TAG;
    }
  }
}