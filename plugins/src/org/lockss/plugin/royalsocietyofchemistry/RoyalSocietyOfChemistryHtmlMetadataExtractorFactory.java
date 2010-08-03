package org.lockss.plugin.royalsocietyofchemistry;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.FileMetadataExtractorFactory;
import org.lockss.extractor.SimpleMetaTagMetadataExtractor;
import org.lockss.plugin.ArticleMetadata;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.IOUtil;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

public class RoyalSocietyOfChemistryHtmlMetadataExtractorFactory implements FileMetadataExtractorFactory{

	static Logger log = Logger.getLogger("RoyalSocietyOfChemistryHtmlMetadataExtractorFactory");
	
	public FileMetadataExtractor createFileMetadataExtractor(String contentType) throws PluginException {
		return new RoyalSocietyOfChemistryHtmlExtractor();
	}	
	
	public static class RoyalSocietyOfChemistryHtmlExtractor extends SimpleMetaTagMetadataExtractor {		
		
		private final String doiPrefix = "10.1039"; 
		private String metaContent="";
		
		/**
		 * Extract metadata from the cached URL
		 * @param cu The cached URL to extract the metadat from
		 */
		public ArticleMetadata extract(CachedUrl cu) throws IOException {
			if (cu == null) {
				throw new IllegalArgumentException("extract(null)");
			}
						
			ArticleMetadata ret = super.extract(cu);						
									
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
						ret.putJournalTitle(line.substring(7, line.length()-17)); // take out the 'articles' word at the end of the title						
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
				
				Pattern patternWithoutVol = Pattern.compile("(\\d{4}),\\s*(\\d*)\\s*-\\s*(\\d*),.*<font color=\"#9C0000\">(.*)</font>.*</span><p><strong>(.*)</strong>");
				Matcher matcherWithoutVol = patternWithoutVol.matcher("");
				matcherWithoutVol.reset(metaContent);
								
				// first attempt to extract metadata from content where the volume is not present as this seems to be the most common case
				// In that case the year represents the volume
				if (matcherWithoutVol.find()) {
					year = matcherWithoutVol.group(1).trim(); 
					ret.putDate(year);
					ret.putVolume(year); // where volume is not present, the year represents it.
					ret.putStartPage(matcherWithoutVol.group(2).trim());
					ret.putArticleTitle(matcherWithoutVol.group(4).trim());
					ret.putAuthor(matcherWithoutVol.group(5).trim().replaceAll(" and", ",")); // replace all ands with commas
				}else{ // if the above does not match then the volume is probably present so attempt to extract it along with the rest of the metadata
					Pattern patternWithVol = Pattern.compile("(\\d{4}),.*<strong>(\\d*)</strong>,\\s*(\\d*)\\s*-\\s*(\\d*),.*<font color=\"#9C0000\">(.*)</font>.*</span><p><strong>(.*)</strong>");
					Matcher matcherWithVol = patternWithVol.matcher("");
					matcherWithVol.reset(metaContent);
					
					if(matcherWithVol.find()){
						ret.putDate(matcherWithVol.group(1).trim());														
						ret.putVolume(matcherWithVol.group(2).trim());
						ret.putStartPage(matcherWithVol.group(3).trim());
						ret.putArticleTitle(matcherWithVol.group(5).trim());
						ret.putAuthor(matcherWithVol.group(6).trim().replaceAll(" and", ",")); // replace all ands with commas
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
				ret.putDOI(doiPrefix+"/"+doi);				
			} catch (MalformedURLException e) {
				log.debug(url + " : Malformed URL");
			} catch (NullPointerException npe){
				log.debug(url + " : DOI is not in query arguments");
			}
		}

	}
}
