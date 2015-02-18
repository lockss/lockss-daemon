/*
 * $Id$
 */

/*

 Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.ashdin;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

public class AshdinMetadataExtractorFactory implements
    FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger("AshdinMetadataExtractorFactory");

  public FileMetadataExtractor createFileMetadataExtractor(
      MetadataTarget target, String contentType) throws PluginException {
    return new AshdinHtmlMetadataExtractor();
  }

  public static class AshdinHtmlMetadataExtractor implements
      FileMetadataExtractor {
	
	private static final int READ_AHEAD_LIMIT_CHARS = 2000; //characters
	private static final int READ_AHEAD_LIMIT_LINES = 5; //lines
	
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
        throws IOException {
      ArticleMetadata am = new SimpleHtmlMetaTagMetadataExtractor().extract(
          target, cu);
 
      BufferedReader bReader = new BufferedReader(cu.openForReading());
      try {
        for (String line = bReader.readLine(); line != null; line = bReader.readLine()) {
          line = line.trim();
          if (line.contains("doi:")) {
            addDoi(line,am);
          } if (line.contains("class=\"absauthor\"")) {
            addAuthors(line, am, bReader);
            log.debug3("extracting authors");
          } if (line.contains("class=\"abstitle\"")) {
        	addTitle(line, am, bReader);
        	log.debug3("extracting title");
          } if (line.contains("Vol.")) {
        	addVolumeAndDate(line,am);
        	addJournalTitle(line,am);
          }
        }
      } finally {
        IOUtil.safeClose(bReader);
      }
      emitter.emitMetadata(cu, am);
    }
    
    //The end tag for volume number doubles as the beginning tag for the date
    private void addVolumeAndDate(String line, ArticleMetadata ret) {
        String beginVolume = "Vol.", endVolume = " (", endDate = ")";
        int volumeBegin = StringUtil.indexOfIgnoreCase(line, beginVolume);
        if(volumeBegin < 0)
        	return;
        volumeBegin += beginVolume.length();
        
        int volumeEnd = StringUtil.indexOfIgnoreCase(line, endVolume, volumeBegin);
        if(volumeEnd < 0)
        	return;
                
        int dateEnd = StringUtil.indexOfIgnoreCase(line, endDate, volumeEnd);
        if(dateEnd < 0)
      	  return;
                
        ret.put(MetadataField.FIELD_VOLUME,line.substring(volumeBegin, volumeEnd));
        ret.put(MetadataField.FIELD_DATE, line.substring(volumeEnd+endVolume.length(),dateEnd));
    }
    
    private void addJournalTitle(String line, ArticleMetadata am) {
    	Matcher beginTitle = Pattern.compile("<pre( )?>").matcher(line);
    	if(!beginTitle.find())
    		return;
    	int titleBegin = beginTitle.end();

    	String endTitle = "<";
    	int titleEnd = StringUtil.indexOfIgnoreCase(line, endTitle, titleBegin);
    	
    	if(titleEnd < 0)
    		return;
    	
    	am.put(MetadataField.FIELD_JOURNAL_TITLE, line.substring(titleBegin,titleEnd));
    }

    private void addAuthors(String line, ArticleMetadata ret, BufferedReader reader) {
      String beginAuthor = "absauthor\">", endAuthor = "</h3>", auth = line;
      int authorBegin = StringUtil.indexOfIgnoreCase(line, beginAuthor);
      if(authorBegin < 0)
    	  return;
      authorBegin += beginAuthor.length();

      int authorEnd = StringUtil.indexOfIgnoreCase(line, endAuthor, authorBegin);

      while(authorEnd < 0) {
    	  try {
				reader.mark(READ_AHEAD_LIMIT_CHARS);
				int additionalLines = 0;
				
				while(line != null && authorEnd < 0 && additionalLines < READ_AHEAD_LIMIT_LINES) {
				  line = reader.readLine();
				  additionalLines++;
				  auth += line;
				  authorEnd = StringUtil.indexOfIgnoreCase(auth, endAuthor, authorBegin);
				  
				  log.debug3("authorBegin: "+authorBegin+", authorEnd: "+authorEnd+", auth: "+auth);
				}
				
				if(authorEnd < 0)
					return;
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
				reader.reset();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
      }
      
      auth = auth.substring(authorBegin, authorEnd);
      auth = auth.replaceAll("<sup>[^<]+</sup>", "");
      
      ret.put(MetadataField.FIELD_AUTHOR,auth);
    }
    
    private void addTitle(String line, ArticleMetadata ret, BufferedReader reader) throws IOException {
        String beginTitle = "abstitle\">", endTitle = "</h2>", title = line;
        int titleBegin = StringUtil.indexOfIgnoreCase(line, beginTitle);
        if(titleBegin < 0)
        	return;
        titleBegin += beginTitle.length();

        int titleEnd = StringUtil.indexOfIgnoreCase(line, endTitle, titleBegin);
        
        while(titleEnd < 0) {
        	try {
				reader.mark(READ_AHEAD_LIMIT_CHARS);
				int additionalLines = 0;
				
				while(line != null && titleEnd < 0 && additionalLines < READ_AHEAD_LIMIT_LINES) {
				  line = reader.readLine();
				  additionalLines++;
				  title += line;
				  titleEnd = StringUtil.indexOfIgnoreCase(title, endTitle, titleBegin);
				}
				
				if(titleEnd < 0)
					return;
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
				reader.reset();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
        }
        
        ret.put(MetadataField.FIELD_ARTICLE_TITLE,title.substring(titleBegin, titleEnd));
      }

    private void addDoi(String line, ArticleMetadata ret) {
      String beginDoi = "doi:";
      String endDoi = "<";
      int doiBegin = StringUtil.indexOfIgnoreCase(line, beginDoi);
      if(doiBegin < 0)
    	  return;
      doiBegin += beginDoi.length();

      int doiEnd = StringUtil.indexOfIgnoreCase(line, endDoi, doiBegin);
      if(doiEnd < 0)
    	  return;
      
      ret.put(MetadataField.FIELD_DOI, line.substring(doiBegin,doiEnd));
    }
  }
}
