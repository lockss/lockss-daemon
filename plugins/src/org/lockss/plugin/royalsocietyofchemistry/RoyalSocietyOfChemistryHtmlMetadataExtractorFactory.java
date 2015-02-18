/*
 * $Id$
 */

/*

 Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.royalsocietyofchemistry;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.IOUtil;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;


/**
 * Two of the articles used to get the html source for this plugin is:
 * http://www.rsc.org/publishing/journals/JC/article.asp?doi=a700024c
 * http://www.rsc.org/publishing/journals/FT/article.asp?doi=a706359h
 * Need to proxy content through beta2.lockss.org or another LOCKSS box.
 * The content online is NOT relevant to this plugin.
 * 
 * ---------------------------------------------- IMPORTANT --------------------------------------------------------
 * This class is not yet complete. Only some of the metadata is extracted. These are:
 * DOI, Vol, Date (date can be the volume when volume is not present), StartPage, Authors, ArticleTitle and JournalTitle
 * The Issue and Issn are not extracted because they are not currently present neither within metadata tags nor in the html content in none of the article pages.
 * An idea is to use the tdb files to extract the Issn from. Extracting the issue is probably going to be harder. Have to come up with ideas on how to go on this.
 * The new RSC site contains metadata tags and it is going to be trivial to extract metadata from there. However, this content is NOT yet collected by LOCKSS amd thus
 * have to work with the old site.
 * -----------------------------------------------------------------------------------------------------------------
 */
public class RoyalSocietyOfChemistryHtmlMetadataExtractorFactory implements FileMetadataExtractorFactory{

	static Logger log = Logger.getLogger("RoyalSocietyOfChemistryHtmlMetadataExtractorFactory");
	
	public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
								 String contentType) throws PluginException {
		return new RoyalSocietyOfChemistryHtmlExtractor();
	}	
	
	public static class RoyalSocietyOfChemistryHtmlExtractor extends SimpleHtmlMetaTagMetadataExtractor {		
		
		private final String doiPrefix = "10.1039"; 
		private String metaContent="";
		
		/**
		 * Extract metadata from the cached URL
		 * @param cu The cached URL to extract the metadata from
		 */
		@Override
		public ArticleMetadata extract(MetadataTarget target,
					       CachedUrl cu) throws IOException {
			if (cu == null) {
				throw new IllegalArgumentException("extract(null)");
			}
						
			ArticleMetadata ret = super.extract(target, cu);						
			
			// extract DOI from URL
			addDOI(cu.getUrl(), ret);
			
			String year="", vol="", startPage="", authors="", articleTitle="", journalTitle="";
			
			// get the content
			BufferedReader bReader = new BufferedReader(cu.openForReading());			
			
			try {		
				
				// go through the cached URL content line by line
				for (String line = bReader.readLine(); line != null; line = bReader.readLine()) {										
					line = line.trim();										
					
					// title is of form: <title>MyJournalTitle articles</title>
					if(StringUtil.startsWithIgnoreCase(line, "<title>")){
						ret.put(MetadataField.FIELD_JOURNAL_TITLE, line.substring(7, line.length()-17)); // take out the 'articles' word at the end of the title						
					}
					
					// It's possible that the chunk of html code is splitted in various lines. Regex cannot match multi line text so constract one string
					// with the relevant html code.
					if (line.contains("<div class=\"onecol\">")) {																										
						while(!line.startsWith("</div><!-- end #content -->")){
							if(!line.equalsIgnoreCase("")){
								metaContent += " "+line;								
							}
							line = bReader.readLine().trim();							
						}
						
					}															
				}				
				
				// regex to extract content from articles where the volume is represented by the year
				Pattern patternWithoutVol = Pattern.compile("(\\d{4}),\\s*(\\d*)\\s*-\\s*(\\d*),.*<font color=\"#9C0000\">(.*)</font>.*</span><p><strong>(.*)</strong>");
				Matcher matcherWithoutVol = patternWithoutVol.matcher("");
				matcherWithoutVol.reset(metaContent);
								
				// first attempt to extract metadata from content where the volume is not present as this seems to be the most common case
				// In that case the year represents the volume
				if (matcherWithoutVol.find()) {
					year = matcherWithoutVol.group(1).trim(); 
					ret.put(MetadataField.FIELD_DATE, year);
					ret.put(MetadataField.FIELD_VOLUME, year); // where volume is not present, the year represents it.
					ret.put(MetadataField.FIELD_START_PAGE, matcherWithoutVol.group(2).trim());
					ret.put(MetadataField.FIELD_ARTICLE_TITLE, matcherWithoutVol.group(4).trim());
					ret.put(MetadataField.FIELD_AUTHOR, matcherWithoutVol.group(5).trim().replaceAll(" and", ",")); // replace all ands with commas
				}else{ // if the above does not match then the volume is probably present so attempt to extract it along with the rest of the metadata
					Pattern patternWithVol = Pattern.compile("(\\d{4}),.*<strong>(\\d*)</strong>,\\s*(\\d*)\\s*-\\s*(\\d*),.*<font color=\"#9C0000\">(.*)</font>.*</span><p><strong>(.*)</strong>");
					Matcher matcherWithVol = patternWithVol.matcher("");
					matcherWithVol.reset(metaContent);
					
					if(matcherWithVol.find()){
						ret.put(MetadataField.FIELD_DATE, matcherWithVol.group(1).trim());														
						ret.put(MetadataField.FIELD_VOLUME, matcherWithVol.group(2).trim());
						ret.put(MetadataField.FIELD_START_PAGE, matcherWithVol.group(3).trim());
						ret.put(MetadataField.FIELD_ARTICLE_TITLE, matcherWithVol.group(5).trim());
						ret.put(MetadataField.FIELD_AUTHOR, matcherWithVol.group(6).trim().replaceAll(" and", ",")); // replace all ands with commas
					}
				}
				
			} finally {
				IOUtil.safeClose(bReader);
			}
			
			return ret;
		}		

		/**
		 * Extract the DOI from the URL. Only the DOI suffix is present in the URL query arguments. The prefix is however the same in all RSC articles
		 * and can be added to the suffix to form the full DOI.
		 * @param url the article url in the form of: http://www.rsc.org/publishing/journals/AC/article.asp?doi=a802128g
		 * @param ret the ArticleMetadata object
		 */
		protected void addDOI(String url, ArticleMetadata ret) {			
			try {
				URL bioUrl = new URL(url);
				String doi = bioUrl.getQuery().split("=")[1];
				// only the DOI suffix is at the URL so we need to concatenate the prefix which is the same in all RSC articles.
				ret.put(MetadataField.FIELD_DOI, doiPrefix+"/"+doi);				
			} catch (MalformedURLException e) {
				log.debug(url + " : Malformed URL");
			} catch (NullPointerException npe){
				log.debug(url + " : DOI is not in query arguments");
			}
		}

	}
}
