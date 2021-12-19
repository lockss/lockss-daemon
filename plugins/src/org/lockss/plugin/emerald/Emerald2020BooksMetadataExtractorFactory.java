/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.plugin.emerald;


import org.jsoup.nodes.Node;
import org.lockss.config.TdbAu;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.IOUtil;
import org.lockss.util.Logger;
import org.lockss.util.MetadataUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Emerald2020BooksMetadataExtractorFactory implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger(Emerald2020BooksMetadataExtractorFactory.class);

  /*
  Found the following content on right side of the pdf landing page:
  https://www.emerald.com/insight/publication/doi/10.1016/S1529-2134(1995)2_Part_2

  <meta name="description" content="Advances in Austrian Economics">

  <div class="row bg-pale-blue mt-3">
    <div class="col-12 mt-3">
       <dl>
          <dt class="h3">DOI</dt>
          <dd class="small">10.1016/S1529-2134(1995)2_Part_2</dd>
          <dt class="intent_book_publication_date_header h3">Publication date</dt>
          <dd class="intent_book_publication_date small">1995-08-31</dd>
          <dt class="h3">Book series</dt>
          <dd class="small">
             <a href="/insight/publication/acronym/AAEC">
             Advances in Austrian Economics
             </a>
          </dd>
          <dt class="h3">Series copyright holder</dt>
          <dd class="small">Emerald Publishing Limited</dd>
          <dt class="h3">ISBN</dt>
          <dd class="small">978-1-84950-516-1</dd>
          <dt class="h3">Book series ISSN</dt>
          <dd class="small">1529-2134</dd>
       </dl>
    </div>
  </div>


  Example 2: https://www.emerald.com/insight/publication/doi/10.1108/9781786359506
  <div class="row bg-pale-blue mt-3">
      <div class="col-12 mt-3">
         <dl>
            <dt class="h3">DOI</dt>
            <dd class="small">10.1108/9781786359506</dd>
            <dt class="intent_book_publication_date_header h3">Publication date</dt>
            <dd class="intent_book_publication_date small">2003-10-17</dd>
            <dt class="intent_book_editor_header h3">Editors</dt>
            <dd class="small">
               <ul class="list-unstyled mt-0 mb-1">
                  <li class="intent_book_editor">
                     <a href="/insight/search?q=Jens Schade">
                     Jens Schade
                     </a>
                  </li>
                  <li class="intent_book_editor">
                     <a href="/insight/search?q=Bernhard Schlag">
                     Bernhard Schlag
                     </a>
                  </li>
               </ul>
            </dd>
            <dt class="h3">ISBN</dt>
            <dd class="small">978-0-08-044199-3</dd>
         </dl>
      </div>
   </div>
  */

  private static Pattern PUBLICATION_TITLE_PAT = Pattern.compile("\\s*<meta name=\"description\" content=\"(.*)\">\\s*");

  private static Pattern DOI_PAT = Pattern.compile("\\s*<dd[^>]*>(10[.][0-9a-z]{4,6}/.*)<\\/dd>\\s*");
  private static Pattern PUBLICATION_DATE_PAT = Pattern.compile("\\s*<dd class=\"intent_book_publication_date.*\">(.*)</dd>\\s*");

  private static Pattern ISBN_FOUND_PAT = Pattern.compile("\\s*<dt[^>]*>\\s*(ISBN)\\s*</dt>\\s*");
  private static Pattern ISBN_PAT = Pattern.compile("\\s*<dd[^>]*>(.*)</dd>\\s*");

  private static Pattern ISSN_FOUND_PAT = Pattern.compile("\\s*<dt[^>]*>\\s*.*(ISSN).*\\s*</dt>\\s*");
  private static Pattern ISSN_PAT = Pattern.compile("\\s*<dd[^>]*>(.*)</dd>\\s*");

  private static Pattern AUTHOR_FOUND_PAT = Pattern.compile("\\s*<dt[^>]*>Editors</dt>\\s*");
  private static Pattern AUTHOR_NAME_FOUND_PAT = Pattern.compile("\\s*<a href=\"/insight/search\\?q=([^>]*)\">\\s*");

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                           String contentType)
          throws PluginException {
    return new Emerald2020BooksMetadataExtractor();
  }

  public static class Emerald2020BooksMetadataExtractor
          implements FileMetadataExtractor {

    private String publicationTitle = null;
    private String doi = null;
    private String publicationDate = null;
    private String isbn = null;
    private String issn = null;
    private List<String> authors = new ArrayList<String>();

    //Keep records of raw html source
    private String publicationTitleRaw = null;
    private String doiRaw = null;
    private String publicationDateRaw = null;
    private String isbnRaw = null;
    private String issnRaw = null;
    private List<String> authorsRaw = new ArrayList<String>();;

    private boolean isPublicationTitleFound = false;
    private boolean isDoiFound = false;
    private boolean isPublicationDateFound = false;
    private boolean isISBNFound = false;
    private boolean isISSNFound = false;
    private boolean isAuthorFound = false;

    private int isbnFoundAtLineNumber = 0;
    private int issnFoundAtLineNumber = 0;
    private int authorFoundAtLineNumber = 0;

    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter) throws IOException, PluginException {

      ArchivalUnit au = cu.getArchivalUnit();
      String bookUri = au.getConfiguration().get("book_uri");
      TdbAu tdbau = cu.getArchivalUnit().getTdbAu();

      log.debug3("Emerald2020BooksMetadataExtractorFactory from cu:" + cu.getUrl() + ", book_uri:" + bookUri);

      if (cu.getUrl().endsWith(bookUri)) {
        log.debug3("Emerald2020BooksMetadataExtractorFactory book landing page FOUND====:" + cu.getUrl() + ", book_uri:" + bookUri);

        ArticleMetadata am = extractMetadataFromHtmlSource(cu);

        if (publicationTitle != null && publicationTitle.length() > 0) {
          am.put(MetadataField.FIELD_PUBLICATION_TITLE, publicationTitle);
          am.putRaw("meta_description_raw_html_source", publicationTitleRaw);
        }

        if (doi != null && !MetadataUtil.isDoi(doi)) {
          am.put(MetadataField.FIELD_DOI, doi);
          am.putRaw("doi_raw_html_source", doiRaw);
        }

        if (publicationDate != null && publicationDate.length() > 0) {
          am.put(MetadataField.FIELD_DATE, publicationDate);
          am.putRaw("publication_date_html_source", publicationDateRaw);
        } else {
          if (tdbau != null) {
            String pubyear = tdbau.getYear();
            log.debug3("publication_date is null, pubyear" + pubyear);
            if (pubyear != null) {
              am.put(MetadataField.FIELD_DATE, pubyear);
              am.putRaw("publication_date_tdb_source", pubyear);
            }
          }
        }

        if (isbn != null && MetadataUtil.isIsbn(isbn)) {
          am.put(MetadataField.FIELD_ISBN, isbn);
          am.putRaw("isbn_raw_html_source", isbnRaw);
        } else {
          if (tdbau != null) {
            String tdbisbn = tdbau.getIsbn();
            if (tdbisbn != null) {
              am.put(MetadataField.FIELD_ISBN, tdbisbn);
              am.putRaw("publication_isbn_tdb_source", tdbisbn);
            }
          }
        }

        if (tdbau != null) {
          String tdbeisbn = tdbau.getEisbn();
          if (tdbeisbn != null) {
            am.put(MetadataField.FIELD_EISBN, tdbeisbn);
            am.putRaw("publication_eisbn_tdb_source", tdbeisbn);
          }
        }

        if (issn != null && MetadataUtil.isIssn(issn)) {
          am.put(MetadataField.FIELD_ISSN, issn);
          am.putRaw("issn_raw_html_source", issnRaw);
        }

        if (authors != null && authors.size() > 0) {
          for (int i = 0; i < authors.size(); i++) {
            am.put(MetadataField.FIELD_AUTHOR, authors.get(i));
            am.putRaw("author_raw_html_source", authorsRaw.get(i));
          }
        }

        // Set up publication title

        // Set publication type
        am.put(MetadataField.FIELD_ARTICLE_TYPE, MetadataField.ARTICLE_TYPE_BOOKVOLUME);
        am.put(MetadataField.FIELD_PUBLICATION_TYPE, MetadataField.PUBLICATION_TYPE_BOOK);


        // Set publisher name
        String publisherName = "Emerald Publishing Limited";

        if (tdbau != null) {
          publisherName = tdbau.getPublisherName();
        }

        am.put(MetadataField.FIELD_PUBLISHER, publisherName);

        emitter.emitMetadata(cu, am);
      }
    }

    private ArticleMetadata extractMetadataFromHtmlSource(CachedUrl cu) throws IOException {

      ArticleMetadata am = new ArticleMetadata();

      if(cu.hasContent()) {
        BufferedReader bReader = null;
        try {

          bReader = new BufferedReader(cu.openForReading());

          String line = bReader.readLine();
          int lineCount = 0;

          while(line != null) {
            //log.debug3("Emerald2020BooksMetadataExtractorFactory line: " + line);

            lineCount++;
            findingMetadata(line, lineCount);
            line = bReader.readLine();
          }
        } finally {
          IOUtil.safeClose(bReader);
        }
      }

      return am;
    }

    private void findingMetadata(String line, int lineCount) {

      Matcher m0 = PUBLICATION_TITLE_PAT.matcher(line);

      // Get doi
      if(!isPublicationTitleFound && m0.matches()){
        isPublicationTitleFound = true;
        publicationTitle = m0.group(1);
        publicationTitleRaw = line.trim();
        log.debug3("Emerald2020BooksMetadataExtractorFactory publication title: " + publicationTitle);

        return;
      }

      Matcher m1 = DOI_PAT.matcher(line);

      // Get doi
      if(!isDoiFound && m1.matches()){
        isDoiFound = true;
        doi = m1.group(1);
        doiRaw = line.trim();
        log.debug3("Emerald2020BooksMetadataExtractorFactory doi: " + doi);

        return;
      }

      // Get publication date
      Matcher m2 = PUBLICATION_DATE_PAT.matcher(line);

      if (!isPublicationDateFound && m2.matches()) {
        isPublicationDateFound = true;
        publicationDate = m2.group(1);
        publicationDateRaw = line.trim();
        log.debug3("Emerald2020BooksMetadataExtractorFactory : publicationDate" + publicationDate);

        return;
      }

      // Get ISBN
      Matcher m3 = ISBN_FOUND_PAT.matcher(line);

      if (!isISBNFound && m3.matches()) {
        isISBNFound = true;
        isbnFoundAtLineNumber = lineCount;
        log.debug3("Emerald2020BooksMetadataExtractorFactory : isISBNFound = true, at line: " + isbnFoundAtLineNumber);

        return;
      }

      // We expect ISBN found at the next line after  <dt class="h3">ISBN</dt>
        Matcher isbn_m = ISBN_PAT.matcher(line);

        if (isISBNFound && isbn_m.matches()) {
          isbn = isbn_m.group(1);
          isbnRaw = line.trim();
          log.debug3("Emerald2020BooksMetadataExtractorFactory : ISBNFound: " + isbn);
          isISBNFound = false; //reset it once found
          return;
        }

      // Get ISSN
      Matcher m4 = ISSN_FOUND_PAT.matcher(line);

      if (!isISSNFound && m4.matches()) {
        isISSNFound = true;
        issnFoundAtLineNumber = lineCount;
        log.debug3("Emerald2020BooksMetadataExtractorFactory ISSN: isISSNFound = true, at line: " + issnFoundAtLineNumber);

        return;
      }

      // We expect ISSN found at the next line after  <dt class="h3">Book series ISSN</dt>

      Matcher issn_m = ISSN_PAT.matcher(line);

      if (isISSNFound && issn_m.matches()) {
        issn = issn_m.group(1);
        issnRaw = line.trim();
        log.debug3("Emerald2020BooksMetadataExtractorFactory ISSN line====: " + line + ", at line#:" + lineCount);

        log.debug3("Emerald2020BooksMetadataExtractorFactory : ISSNFound: " + issn);
        isISSNFound = false;
        return;
      }

      // Get Author
      Matcher m5 = AUTHOR_FOUND_PAT.matcher(line);

      if (!isAuthorFound && m5.matches()) {
        isAuthorFound = true;
        authorFoundAtLineNumber = lineCount;
        log.debug3("Emerald2020BooksMetadataExtractorFactory Author: isAuthorFound = true, at line: " + authorFoundAtLineNumber);

        return;
      }

      // We expect Author found after <dt class="intent_book_editor_header h3">Editors</dt>
      if (lineCount - authorFoundAtLineNumber >= 1) {
        Matcher author_m = AUTHOR_NAME_FOUND_PAT.matcher(line);

        if (isAuthorFound && author_m.matches()) {
          String author = author_m.group(1);
          authorsRaw.add(line.trim());
          authors.add(author);
          log.debug3("Emerald2020BooksMetadataExtractorFactory : Author: " + author);

          return;
        }
      }
    }
  }
}