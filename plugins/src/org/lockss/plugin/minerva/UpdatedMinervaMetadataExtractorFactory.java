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

package org.lockss.plugin.minerva;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.pdfbox.pdmodel.PDDocument;
import org.pdfbox.pdmodel.common.PDStream;
import org.pdfbox.util.PDFTextStripper;

/**
 * One of the articles used to get the source for this plugin is:
 * http://minerva.mic.ul.ie/Vol%2015/index.html
 */
public class UpdatedMinervaMetadataExtractorFactory
  implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger("UpdatedMinervaMetadataExtractorFactory");

  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
							   String contentType)
      throws PluginException {
    return new UpdatedMinervaMetadataExtractor();
  }

  public static class UpdatedMinervaMetadataExtractor 
    implements FileMetadataExtractor {
	  
	private Pattern[] patterns = {Pattern.compile("ISSN ([^-]+-[^-]+)"), 
		Pattern.compile("Minerva - An Internet Journal of Philosophy ([\\d]+) .[\\d]+.: [\\d]+-[\\d]+"),
		Pattern.compile("Minerva - An Internet Journal of Philosophy [\\d]+ .([\\d]+).: [\\d]+-[\\d]+"),
		Pattern.compile("Minerva - An Internet Journal of Philosophy [\\d]+ .[\\d]+.: ([\\d]+)-[\\d]+"),
		Pattern.compile("Minerva - An Internet Journal of Philosophy [\\d]+ .[\\d]+.: [\\d]+-([\\d]+)"),
	};
	
    /**
     * Extracts metadata directly by reading the html file
     */
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
      throws IOException {
    	log.debug("The MetadataExtractor attempted to extract metadata from cu: "+cu);
    	
    	if(!cu.hasContent())
    		return;
    	
    	ArticleMetadata am = extractFrom(cu.openForHashing());      
    	emitter.emitMetadata(cu, am);
    }
    
    protected ArticleMetadata extractFrom(InputStream in) throws IOException {    	
    	ArticleMetadata am = new ArticleMetadata();
	
		PdfDocument pdf = new PdfDocument(in);
		PDDocument doc = new PDDocument(pdf.getCosDocument());
		PDFTextStripper strip = new PDFTextStripper();
		
		String text = strip.getText(doc), line = "";
				
		text = text.substring(0, text.indexOf("Abstract") == -1 ? text.length()-1 : text.indexOf("Abstract"));
		BufferedReader reader = new BufferedReader(new StringReader(text));
		
		extractBasicMetadata(line, reader, am);
		
		if(line != null) 
			extractAuthorAndTitle(line, reader, am);
		
		pdf.close();
		doc.close();
    	
    	return am;
    }
    
    private void extractBasicMetadata(String line, BufferedReader reader, ArticleMetadata am) throws IOException {
		while(!patterns[1].matcher(line).find()) {
			line = reader.readLine();
			
			if (patterns[0].matcher(line).find()) {
				am.put(MetadataField.FIELD_ISSN, patterns[0].matcher(line).replaceAll("$1"));
			} else if (patterns[1].matcher(line).find()) {
				am.put(MetadataField.FIELD_JOURNAL_TITLE, "Minerva - An Internet Journal of Philosophy");
				am.put(MetadataField.FIELD_VOLUME, patterns[1].matcher(line).replaceAll("$1"));
				am.put(MetadataField.FIELD_DATE, patterns[2].matcher(line).replaceAll("$1"));
				am.put(MetadataField.FIELD_START_PAGE, patterns[3].matcher(line).replaceAll("$1"));
				am.put(MetadataField.FIELD_END_PAGE, patterns[4].matcher(line).replaceAll("$1"));
				
				line = reader.readLine().trim();
				return;
			}
		}
    }
    
    private void extractAuthorAndTitle(String line, BufferedReader reader, ArticleMetadata am) throws IOException {
    	while(line.length() == 0 || line.charAt(0) == '_')
				line = reader.readLine().trim();
			   				
			am.put(MetadataField.FIELD_AUTHOR, line);
			line = reader.readLine().trim();
			
			while(line.length() == 0)
				line = reader.readLine().trim();
			line = reader.readLine().trim();
			while(line.length() == 0)
				line = reader.readLine().trim();
			
			am.put(MetadataField.FIELD_ARTICLE_TITLE, line+" "+reader.readLine().trim());
    }
  }
}