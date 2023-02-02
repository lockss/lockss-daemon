/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.msue;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.*;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

public class MichiganStateUniversityExtensionHtmlMetadataExtractorFactory
  implements FileMetadataExtractorFactory {
  static Logger log = 
    Logger.getLogger(MichiganStateUniversityExtensionHtmlMetadataExtractorFactory.class);

  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                 String contentType)
      throws PluginException {
    return new MichiganStateUniversityExtensionHtmlMetadataExtractor();
  }
  
  

  public static class MichiganStateUniversityExtensionHtmlMetadataExtractor
    implements FileMetadataExtractor {
      
    private static ArrayList<ArticleMetadata> metadataList = 
      new ArrayList<ArticleMetadata>();
    private static ArrayList<String[]> authorList = new ArrayList<String[]>();
    private static ArrayList<String> dateList = new ArrayList<String>();
    private static ArrayList<String> titleList = new ArrayList<String>();
    private static ArrayList<String> urlList = new ArrayList<String>();
    private static ArrayList<String> issueList = new ArrayList<String>();
    private static String journalTitle = "Michigan State University Extension Bulletin";
    
    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
      throws IOException { 
        
      // This publication doesn't make use of meta tags, so we need
      // to extract metadata from the HTML source
      
      BufferedReader bReader = new BufferedReader(cu.openForReading());
      String dateLine = null;
      String authorLine = null;
      String renamedLine = null;
      String issueLine = null;
      String mainArticleTitleLine = null;
      Integer curArt = 0;
      metadataList.clear();
      authorList.clear();
      dateList.clear();
      titleList.clear();
      urlList.clear();
      
      try {
        for (String line = bReader.readLine();
             line != null; line = bReader.readLine()) {
          line = line.trim();
          
          // Bulletin number
          if (StringUtil.startsWithIgnoreCase(line, "<title>")) {
            issueLine = line;
          }
          
          // Title of the article
          if (StringUtil.startsWithIgnoreCase(line, "<h3>")) {
            mainArticleTitleLine = line;
          }
          
          String urlLine = cu.getUrl();
          
          if (cu.getUrl().contains("index.html")) {
            urlLine = cu.getUrl().substring(0, cu.getUrl().indexOf("index.html"));
          }
          
          // Date, author, and title for each revision of the article,
          if (line.contains(urlLine)) {
            if (StringUtil.startsWithIgnoreCase(dateLine, "Issued") ||
                StringUtil.startsWithIgnoreCase(dateLine, "Revised") ||
                StringUtil.startsWithIgnoreCase(dateLine, "Reprinted")) {
                  addDate(dateLine, curArt);
                  addAuthors(authorLine, curArt);
                  if (StringUtil.startsWithIgnoreCase(renamedLine, "<B>Renamed")) {
                      addRenamedArticleTitle(renamedLine, curArt);
                  } else addArticleTitle(mainArticleTitleLine, curArt);
                  addUrl(line, curArt);
            }
            addIssue(issueLine, curArt);
            curArt++;
          }
          
          // Publication date is one line above the line containing the article
          // URL, and author names are one line above publication date.
          renamedLine = authorLine;
          authorLine = dateLine;
          dateLine = line;
        }
      } finally {
        IOUtil.safeClose(bReader);
      }
      
      Integer totalArticles = curArt;
      for (int i = 0; i < totalArticles; i++) {
          ArticleMetadata am = new ArticleMetadata();
          am.put(MetadataField.FIELD_ARTICLE_TITLE, titleList.get(i));
          am.put(MetadataField.FIELD_DATE, dateList.get(i));
          am.put(MetadataField.FIELD_ACCESS_URL, urlList.get(i));
          am.put(MetadataField.FIELD_JOURNAL_TITLE, journalTitle);
          for (int j = 0; j < authorList.get(i).length; j++) {
              am.put(MetadataField.FIELD_AUTHOR, authorList.get(i)[j]);
          }
          metadataList.add(am);
      }

      if (metadataList.size() == 0) {
        ArticleMetadata am = new ArticleMetadata();
        metadataList.add(am);
        emitter.emitMetadata(cu, am);
      }
      
      Iterator<ArticleMetadata> iter = metadataList.iterator();
      while (iter.hasNext()) {
        emitter.emitMetadata(cu, iter.next());
      }
    }
    
    protected void addIssue(String line, Integer index) {
      String issue = null;

      Pattern pattern = Pattern.compile("$e[0-9]+^");

      Matcher matcher = pattern.matcher(line);
      while (matcher.find()) {
          issue = matcher.group();
      }
      issueList.add(index, issue);
    }
    
    
    protected void addArticleTitle(String line, Integer index) {
      String articleTitle = StringUtil.getTextBetween(line, "<h3>", "</h3>");
        
      if (articleTitle == null) {
        log.debug(line + ": <h3></h3> article title tags not found");
        return;
      }

      articleTitle = articleTitle.trim();
      titleList.add(index, articleTitle);
    }
    
    protected void addRenamedArticleTitle(String line, Integer index) {
      String renamedArticleTitle = StringUtil.getTextBetween(line, "<B>Renamed:", "</b>");
        
      if (renamedArticleTitle == null) {
          log.debug(line + ": <B></b> renamed article title tags not found");
          return;
      }

      renamedArticleTitle = renamedArticleTitle.trim();
      titleList.add(index, renamedArticleTitle);
    }
    
    protected void addAuthors(String line, Integer index) {
      String[] authors = null;
      Set<String> formattedAuthors = new HashSet<String>();
      String name = "";
        
      /*
       * The first HTML line containing author names begins with <p>,
       * but subsequent lines do not. After stripping the HTML
       * tags, we split the author list (semicolon delimited), and then
       * strip the author's department (comma delimited) from their name
       * before adding it to the metadata database.
       */
      
      if (line.startsWith("<p>"))
        line = StringUtil.getTextBetween(line, "<p>", "<br>");
      else line = line.substring(0, line.indexOf("<br>"));
        
      if (line.contains(";")) {
        authors = line.split(";");
        for (int i = 0; i < authors.length; i++) {
          authors[i] = authors[i].trim();
          if (authors[i].contains(",")) {
            name = authors[i].substring(0, authors[i].indexOf(","));
            formattedAuthors.add(name);
          }
          else {
            name = authors[i];
            formattedAuthors.add(name);
          }
        }
      }
      else if (line.contains(",")) {
        name = line.substring(0, line.indexOf(","));
        formattedAuthors.add(name);
      }
      else {
        name = line;
          formattedAuthors.add(name);
        }
        authors = formattedAuthors.toArray(new String[0]);
        authorList.add(index, authors);
    }
    
    protected void addDate(String line, Integer index) {
      String[] dateMetadata = null;
      String month;
      String year;
      String date;
      Integer MONTH_POS = 1;
      Integer YEAR_POS = 2;
        
      /*
       * The HTML line containing the publication month and year is
       * oddly formatted with &nbsp; delimiters.
       */
        
      dateMetadata = line.split("&nbsp;");
      month = dateMetadata[MONTH_POS].trim();
      year = dateMetadata[YEAR_POS].trim();
        
      if (month == null || year == null) {
        log.debug("Error parsing date metadata");
          return;
      }  
      date = month + " " + year;
      dateList.add(index, date);
    }
    
    protected void addUrl(String line, Integer index) {
      String url = null;

      Pattern pattern = Pattern.compile("http.+\\.pdf");

      Matcher matcher = pattern.matcher(line);
      while (matcher.find()) {
          url = matcher.group();
      }
      urlList.add(index, url);
    }
  }
}