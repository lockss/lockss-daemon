/*
 * $Id: MathematicalSciencesPublishersHtmlMetadataExtractorFactory.java,v 1.1 2013-08-23 02:26:23 etenbrink Exp $
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.mathematicalsciencespublishers;

//import java.io.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang.StringEscapeUtils;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;


/*
 * Metadata on an abstract page http://msp.org/camcos/2012/7-2/p01.xhtml
 * in the form of:
 * <meta name="citation_publisher" content="Mathematical Sciences Publishers"/>
 * <meta name="citation_title" content="Discontinuous Galerkin method with the spectral deferred correction time-integration scheme and a modified moment limiter for adaptive grids"/>
 * <meta name="citation_journal_title" content="Communications in Applied Mathematics and Computational Science"/>
 * <meta name="citation_volume" content="7"/>
 * <meta name="citation_issue" content="2"/>
 * <meta name="citation_firstpage" content="133"/>
 * <meta name="citation_lastpage" content="174"/>
 * <meta name="citation_publication_date" content="2012-10-16"/>
 * <meta name="citation_pdf_url" content="http://msp.org/camcos/2012/7-2/camcos-v7-n2-p01-s.pdf"/>
 * <meta name="citation_doi" content="10.2140/camcos.2012.7.133"/>
 * <meta name="citation_issn" content="2157-5452"/>
 * <meta name="citation_author" content="Gryngarten, Leandro"/>
 * <meta name="citation_author" content="Smith, Andrew"/>
 * <meta name="citation_author" content="Menon, Suresh"/>
 */

public class MathematicalSciencesPublishersHtmlMetadataExtractorFactory implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger("MathematicalSciencesPublishersHtmlMetadataExtractorFactory");

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
        String contentType)
      throws PluginException {
    return new MathematicalSciencesPublishersHtmlMetadataExtractor();
  }

  public static class MathematicalSciencesPublishersHtmlMetadataExtractor
    extends SimpleHtmlMetaTagMetadataExtractor {

    // Map HTML meta tag names to cooked metadata fields
    private static MultiMap tagMap = new MultiValueMap();
    static {
      tagMap.put("citation_journal_title", MetadataField.FIELD_JOURNAL_TITLE);
      tagMap.put("citation_publisher", MetadataField.FIELD_PUBLISHER);
      tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
      tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);
      tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
      tagMap.put("citation_lastpage", MetadataField.FIELD_END_PAGE);
      tagMap.put("citation_doi", MetadataField.FIELD_DOI);
      tagMap.put("citation_issn", MetadataField.FIELD_ISSN);
      tagMap.put("citation_publication_date", MetadataField.FIELD_DATE);
      tagMap.put("citation_author",
          new MetadataField(MetadataField.FIELD_AUTHOR,
                            MetadataField.splitAt(",")));
      tagMap.put("citation_pdf_url", MetadataField.FIELD_ACCESS_URL);
      tagMap.put("scraped_doi", MetadataField.FIELD_DOI);
      tagMap.put("scraped_title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("scraped_publication_date", MetadataField.FIELD_DATE);
    }

    @Override
    public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
        throws IOException {
      ArticleMetadata am = supeRextract(target, cu); // super.extract(target, cu);
      
      // attempt to get doi. etc from xhtml file
      if ((am.getRaw("citation_doi") == null) || 
          (am.getRaw("citation_title") == null)) {
        
        String colContent="";
        // get the content
        BufferedReader bReader = new BufferedReader(cu.openForReading());     
        
        try {
          Matcher matcher;
          // go through the cached URL content line by line
          for (String line = bReader.readLine(); line != null; line = bReader.readLine()) {
            line = line.trim();
            //<td id="content-area" class="content-column">
            if (!((line.startsWith("<td class=\"content-column\"")) ||
                  (line.startsWith("<td id=\"content-area\"")))) {
              continue;
            }
            // save the next block of text in textContent
            while((line != null) && !line.toLowerCase().contains("<h5>abstract</h5>")){
              if(!line.equalsIgnoreCase("")){
                colContent += " " + line.trim();
              }
              line = bReader.readLine();
            }
          }
          
          // regexes to extract DOI, etc. from articles
          if (am.getRaw("citation_doi") == null) {
            // find DOI with optional anchor tag
            Pattern patternDoi = Pattern.compile(
                "[ >]DOI:.*?(?:<a href= \"http:.*?\">)?(10.2140/[^ <]*?)[ <]", 
                Pattern.CASE_INSENSITIVE);
            matcher = patternDoi.matcher(colContent);
            if (matcher.find()) {
              putValue(am, "scraped_doi", matcher.group(1).trim());
            }
            else {
              log.debug("No DOI found in content");
            }
          }
          if (am.getRaw("citation_title") == null) {
            // find article title 
            Pattern patternTitleAuth = Pattern.compile(
                "<div class=\"title\">(.*?)</div>", 
                Pattern.CASE_INSENSITIVE);
            matcher = patternTitleAuth.matcher("");
            matcher.reset(colContent);
            if (matcher.find()) {
              putValue(am, "scraped_title", matcher.group(1).trim());
            }
            else {
              log.debug("No ARTICLE_TITLE found in content");
            }
          }
        } catch (Exception e) {
          log.debug(e + " : Malformed Pattern");
        }
        finally {
          IOUtil.safeClose(bReader);
        }
      }
      
      am.cook(tagMap);
      
      return am;
    }
    
    private ArticleMetadata supeRextract(MetadataTarget target, CachedUrl cu)
        throws IOException {
      if (cu == null) {
        throw new IllegalArgumentException("extract() called with null CachedUrl");
      }
      ArticleMetadata ret = new ArticleMetadata();
      BufferedReader bReader =
        new BufferedReader(cu.openForReading());
      for (String line = bReader.readLine();
     line != null;
     line = bReader.readLine()) {
        int i = StringUtil.indexOfIgnoreCase(line, "<meta ");
        while (i >= 0) {
          // recognize end of tag character preceded by optional '/', 
          // preceded by a double-quote that is separated by zero or more 
          // whitespace characters
          int j = i+1;
          while (true) {
            j = StringUtil.indexOfIgnoreCase(line, ">", j);
            if (j < 0) break;
            String s = line.substring(i,j);
            if (s.endsWith("/")) {
              s = s.substring(0,s.length()-1);
            }
            if (s.trim().endsWith("\"")) {
              break;
            }
            j++;
          }
          if (j < 0) {
            // join next line with tag end
            String nextLine = bReader.readLine();
            if (nextLine == null) {
              break;
            }
            // XXX here we want to trim leading spaces
            if (line.endsWith("=") && nextLine.startsWith(" ")) {
              nextLine = nextLine.replaceFirst("\\s+", "");
            }
            line += nextLine;
            continue;
          }
          String meta = line.substring(i, j+1);
          if (log.isDebug3()) log.debug3("meta: " + meta);
          addTag(meta, ret);
          i = StringUtil.indexOfIgnoreCase(line, "<meta ", j+1);
        }
      }
      IOUtil.safeClose(bReader);
      return ret;
    }
    
    private void addTag(String line, ArticleMetadata ret) {
      String nameFlag = "name=\"";
      int nameBegin = StringUtil.indexOfIgnoreCase(line, nameFlag);
      if (nameBegin <= 0) {
        if (log.isDebug3()) log.debug3(line + " : no " + nameFlag);
        return;
      }
      nameBegin += nameFlag.length();
      int nameEnd = line.indexOf('"', nameBegin + 1);
      if (nameEnd <= nameBegin) {
        log.debug2(line + " : " + nameFlag + " unterminated");
        return;
      }
      String name = line.substring(nameBegin, nameEnd);
      String contentFlag = "content=\"";
      int contentBegin = StringUtil.indexOfIgnoreCase(line, contentFlag);
      if (contentBegin <= 0) {
        if (log.isDebug3()) log.debug3(line + " : no " + contentFlag);
        return;
      }
      if (nameBegin <= contentBegin && nameEnd >= contentBegin) {
        log.debug2(line + " : " + contentFlag + " overlaps " + nameFlag);
        return;
      }
      contentBegin += contentFlag.length();
      int contentEnd = line.indexOf('"', contentBegin + 1);
      if (log.isDebug3()) {
        log.debug3(line + " name [" + nameBegin + "," + nameEnd + "] cont [" +
       contentBegin + "," + contentEnd + "]");
      }
      if (contentEnd <= contentBegin) {
        log.debug2(line + " : " + contentFlag + " unterminated");
        return;
      }
      if (contentBegin <= (nameBegin - nameFlag.length()) && 
          contentEnd >= (nameBegin - nameFlag.length())) {
        log.debug2(line + " : " + nameFlag + " overlaps " + contentFlag);
        return;
      }
        
      String content = line.substring(contentBegin, contentEnd);
      putValue(ret, name, content);
    }
    
    private void putValue(ArticleMetadata ret, String name, String content) {
      // filter raw HTML tags embedded within content -- publishers get sloppy
      content = HtmlUtil.stripHtmlTags(content);
      // remove character entities from content
      content = StringEscapeUtils.unescapeHtml(content);
      // normalize multiple whitespace chars to a single space character
      content = content.replaceAll("\\s+", " ");
      
      if (log.isDebug3()) log.debug3("Add: " + name + " = " + content);
      ret.putRaw(name, content);
    }
  }
  
}
