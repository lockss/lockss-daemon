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

package org.lockss.plugin.heterocycles;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/*
 * No metadata found from the publisher's website.
 * BaseArticleMetadataExtractor provides some default metadata from tdb file.
 * Issue number can be extracted from the pdf url:
 *      <heterocyclesbase>.com/clockss/downloads/PDF/23208/83/1
 * The cached url is pdf.
 * 
 * A sample metadata from daemon:
 * ArticleFiles
 *  Full text CU:  http://www.heterocycles.jp/clockss/downloads/PDF/22574/87/4
 *  Abstract:  http://www.heterocycles.jp/clockss/libraries/fulltext/22574/87/4
 *  ArticleMetadata:  http://www.heterocycles.jp/clockss/libraries/fulltext/22574/87/4
 *  FullTextHtml:  http://www.heterocycles.jp/clockss/libraries/fulltext/22574/87/4
 *  FullTextPdfFile:  http://www.heterocycles.jp/clockss/downloads/PDF/22574/87/4
 *  PdfWithLinks:  http://www.heterocycles.jp/clockss/downloads/PDFwithLinks/22574/87/4
 * Metadata
 *  access.url: http://www.heterocycles.jp/clockss/downloads/PDF/22574/87/4
 *  date: 2013
 *  eissn: 1881-0942
 *  issn: 0385-5414
 *  issue: 4
 *  journal.title: An International Journal for Reviews and Communications in Heterocyclic Chemistry
 *  publisher: The Japan Institute of Heterocyclic Chemistry
 *  doi: 87    
 * Raw Metadata (empty)
 */
public class HeterocyclesHtmlMetadataExtractorFactory implements FileMetadataExtractorFactory {
  
  private static final Logger log = Logger.getLogger(HeterocyclesHtmlMetadataExtractorFactory.class);
  
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                           String contentType)
      throws PluginException {
    return new HeterocyclesMetadataExtractor();
  }

  // Gets default metadata from tdb: date, journal.title, publisher, 
  public static class HeterocyclesMetadataExtractor implements FileMetadataExtractor {

    static String PDF_ACCESS_URL = "pdf_access_url";

    // Group 1: type, either PDF or PDFsi or PDFwithLinks
    // Group 2: volume
    // Group 3: issue
    private static final Pattern PDF_PATTERN =
        Pattern.compile("/(PDF|PDFsi|PDFwithLinks)/[0-9]+/([^/]+)/([^/]+)/?$", Pattern.CASE_INSENSITIVE);
    
    private Pattern ISSUE_PATTERN = Pattern.compile(
        "/libraries/journal/([^/]+)/([^/]+)$", Pattern.CASE_INSENSITIVE);

    ArrayList<String> issuePages = new ArrayList<String>();

    @Override
    public void extract(MetadataTarget target,
                        CachedUrl cu,
                        Emitter emitter)
        throws IOException {
      ArchivalUnit au = cu.getArchivalUnit();
      String tocUrl = cu.getUrl();
      log.debug3(String.format("Processing TOC: %s", tocUrl));
      InputStream in = null;
      try {
        // Get the content
        in = cu.getUnfilteredInputStream();
        if (in == null) {
          log.debug3(String.format("No content for %s", tocUrl));
          return;
        }
        
        // Parse the document
        Document doc = Jsoup.parse(in, cu.getEncoding(), tocUrl);
        
        // Get all the article <div>s
        Elements articleDivs = doc.selectXpath("body/div[@id='mainContainer']/div[@id='mainContent']/div[@class='contentBox']");
        log.debug3(String.format("Number of article <div>s: %d", articleDivs.size()));
        
        // For each article...
        for (Element articleDiv : articleDivs) {
          // Select a PDF link
          Elements pdfWithLinksTags = articleDiv.selectXpath("a[@class='linkrow'][contains(@href, '/PDFwithLinks/')]");
          Elements pdfTags = articleDiv.selectXpath("a[@class='linkrow'][contains(@href, '/PDF/')]");
          Elements pdfSiTags = articleDiv.selectXpath("a[@class='linkrow'][contains(@href, '/PDFsi/')]");
          String accessUrl = null;
          if (!pdfWithLinksTags.isEmpty()) {
            accessUrl = pdfWithLinksTags.get(0).absUrl("href");
            log.debug3(String.format("Selected 'PDFwithLinks': %s", accessUrl));
          }
          else if (!pdfTags.isEmpty()) {
            accessUrl = pdfTags.get(0).absUrl("href");
            log.debug3(String.format("Selected 'PDF': %s", accessUrl));
          }
          else if (!pdfSiTags.isEmpty()) {
            accessUrl = pdfSiTags.get(0).absUrl("href");
            log.debug3(String.format("Selected 'PDFsi': %s", accessUrl));
          }
          else {
            log.debug3(String.format("Could not find a PDF link: %s", articleDiv));
            continue;
          }
          
          // Extract the article title and DOI
          Elements titleTags = articleDiv.selectXpath("h5");
          Elements doiTags = articleDiv.selectXpath("div[starts-with(text(), 'DOI:')]");
          
          ArticleMetadata am = new ArticleMetadata();
          am.putRaw("access_url", accessUrl);
          emitter.emitMetadata(cu /* FIXME */, am);
        }
      }
      catch (IOException ioe) {
        log.debug3(String.format("IOException while processing %s", tocUrl), ioe);
      }
      finally {
        IOUtil.safeClose(in);
      }
    }

    public void parse(InputStream in,
                      String encoding,
                      String url,
                      Emitter emitter)
        throws IOException {
      // Parse the document
      Document doc = Jsoup.parse(in, encoding, url);
      
      // Get all the article <div>s
      Elements articleDivs = doc.selectXpath("body/div[@id='mainContainer']/div[@id='mainContent']/div[@class='contentBox']");
      log.debug3(String.format("Number of article <div>s: %d", articleDivs.size()));
      
      for (Element articleDiv : articleDivs) {
        // Select a PDF link
        Elements pdfWithLinksTags = articleDiv.selectXpath("a[@class='linkrow'][contains(@href, '/PDFwithLinks/')]");
        Elements pdfTags = articleDiv.selectXpath("a[@class='linkrow'][contains(@href, '/PDF/')]");
        Elements pdfSiTags = articleDiv.selectXpath("a[@class='linkrow'][contains(@href, '/PDFsi/')]");
        String accessUrl = null;
        if (!pdfWithLinksTags.isEmpty()) {
          accessUrl = pdfWithLinksTags.get(0).absUrl("href");
          log.debug3(String.format("Selected 'PDFwithLinks': %s", accessUrl));
        }
        else if (!pdfTags.isEmpty()) {
          accessUrl = pdfTags.get(0).absUrl("href");
          log.debug3(String.format("Selected 'PDF': %s", accessUrl));
        }
        else if (!pdfSiTags.isEmpty()) {
          accessUrl = pdfSiTags.get(0).absUrl("href");
          log.debug3(String.format("Selected 'PDFsi': %s", accessUrl));
        }
        else {
          log.debug3(String.format("Could not find a PDF link: %s", articleDiv));
          continue;
        }
        
        // Extract the article title and DOI
        Elements titleTags = articleDiv.selectXpath("h5");
        Elements doiTags = articleDiv.selectXpath("div[starts-with(text(), 'DOI:')]");
        
        ArticleMetadata am = new ArticleMetadata();
        am.putRaw("access_url", accessUrl);
        emitter.emitMetadata(null, am);
      }
    }
    
    
    private void getAdditionalMetadata(CachedUrl cu, Emitter emitter)
    {

      log.debug3("--------getAdditionalMetadata-------");
      InputStream in = cu.getUnfilteredInputStream();
      if (in != null) {
        try {

          getMoreValue(in, cu.getEncoding(), cu,  emitter);
          in.close();

        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    /*
    //https://www.heterocycles.jp/clockss/libraries/journal/102/8

    <html>
    <body>
      <div id="mainContainer" class="home">
         <div id="mainContent">
            <div id="indexBar">12 data found. 1 - 12 listed<span class="next">    </span> </div>
            <div class="contentBox">
               <span class="blue">Contents</span> | Regular issue | Vol 102, No. 8, 2021<br />
               Published online: 30th June, 2021<br/>
               <div>DOI: 10.3987/Contents-21-10208</div>
               <h5 class="regissue">■ <b><span class="htr_normal">Contents</span></b></h5>
               <span class="linkrow"><span class="red">FREE:</span></span><a href="/clockss/downloads/PDF/27379/102/8" class="linkrow">PDF (3.1MB)</a>
            </div>
            <div id="indexBar">12 data found. 1 - 12 listed<span class="next">    </span> </div>
            <br/>
         </div>
         <!--Footer-->
         <div id="footer">
            <div class="footerBar"> The Japan Institute of Heterocyclic Chemistry 1-7-17 Motoakasaka, Minato-ku, Tokyo 107-0051, Japan</div>
            <br class="clearfloat" />
            <span class="copy">Copyright ⓒ 2023 The Japan Institute of Heterocyclic Chemistry </span>
            </p>
         </div>
         <!-- end mainContainer-->
      </div>
   </body>
</html>
     */

    protected HashMap<String, String> getMoreValue(InputStream in, String encoding, CachedUrl cu, Emitter emitter) {

      Elements mainContentElement;

      Elements divHolderElement;
      Elements nthDivHolderElement;
      Elements doiElement;
      Elements articleElement;
      Elements pdfElement;


      String doi = null;
      String articleTitle = null;
      String pdfLink = null;
      String finalPDFLink = null;

      HashMap<String, String> extraMetadata = new HashMap<String, String>();

      try {

        String url = cu.getUrl();
        Document doc = Jsoup.parse(in, encoding, url);

        mainContentElement = doc.select("div#mainContainer > div#mainContent > div.contentBox");

        int divCount = 1;

        if ( mainContentElement != null) {
          for (Element div : mainContentElement) {
            if (div.hasClass("contentBox"))
              divCount++;
          }
        } else {
          log.debug3("----contentBox count is not selected =========== ");
        }

        log.debug3("----url= " + url + ", contentBox count =========== " + Integer.toString(divCount));

        String divHolderSelector = "div#mainContainer > div#mainContent > div.contentBox";

        divHolderElement = doc.select(divHolderSelector);

        //log.debug3("=========Processing DIV: for url = " + url + ", divHolder = " + divHolderElement.text());


        //cssPath and cssSelector for Doi
        //cssPath: html body div#mainContainer.home div#mainContent div.contentBox div
        //cssSelector: div.contentBox:nth-child(3) > div:nth-child(4)

        //cssPath and cssSelector for Article
        //cssPath: html body div#mainContainer.home div#mainContent div.contentBox h5.regissue b span.htr_normal
        //cssSelector: div.contentBox:nth-child(4) > h5:nth-child(5) > b:nth-child(1) > span:nth-child(1)

        //cssPath and cssSelector for PDF file
        //cssPath: html body div#mainContainer.home div#mainContent div.contentBox a.linkrow
        //cssSelector: div.contentBox:nth-child(7) > a:nth-child(11)
        ////

        // cssSelector start index is "1" and the length is inclusive
        for (int i = 1; i <= divCount + 1; i++) {

          log.debug3("=========Processing div#" + Integer.toString(i) + ", for url = " + url);

          //////////////Create ArticleMetadata for each div////////////
          ArticleMetadata am = new ArticleMetadata();


          String nthDivSelector = "div#mainContainer > div#mainContent > div.contentBox:nth-child(" + Integer.toString(i) + ")";
          String doiSelector = "div#mainContainer > div#mainContent > div.contentBox:nth-child(" + Integer.toString(i) + ") > div:nth-child(4)";
          String articleSelector = "div#mainContainer > div#mainContent > div.contentBox:nth-child(" + Integer.toString(i) + ") > h5:nth-child(5)";


          String pdfSelector = "div#mainContainer > div#mainContent > div.contentBox:nth-child(" + Integer.toString(i) + ") > a";
          pdfElement = doc.select(pdfSelector);

          if ( pdfElement != null) {
            for (Element pdfLinkElement : pdfElement) {
              pdfLink = pdfLinkElement.attr("href").trim();
              finalPDFLink = url.substring(0, url.indexOf("/clockss")) + pdfLink;
              log.debug3("final pdf text: = " + finalPDFLink + ", url = " + url );
              if (finalPDFLink != null && finalPDFLink.length() > 0 && (finalPDFLink.contains("downloads/pdf") || finalPDFLink.contains("downloads/PDF")) ) {

                log.debug3("final pdf text: = " + finalPDFLink + ", url = " + url  + ", set FIELD_ACCESS_URL");

                //Has to do this, otherwise, it will fail hascontent check
                am.put(MetadataField.FIELD_ACCESS_URL, finalPDFLink.trim());
                break;
              }
            }
          } else {
            log.debug3("----contentBox count is not selected =========== ");
          }


          nthDivHolderElement = doc.select(nthDivSelector);
          doiElement = doc.select(doiSelector);

          articleElement = doc.select(articleSelector);

          if (nthDivHolderElement == null) {
            log.debug3("=========Processing Empty DIV: Article div#" + Integer.toString(i) + ", for url = " + url + ", nthDivHolder = " + nthDivHolderElement.text());
          }

          if (doiElement != null) {

            log.debug3("=========Processing DIV: Article div#" + Integer.toString(i) + ", for url = " + url + ", nthDivHolder = " + nthDivHolderElement.text());

            doi = doiElement.text().trim().toLowerCase();
            log.debug3("=========Processing DIV: div#" + Integer.toString(i) + ", raw doi text: = " + doiElement + ", url = " + url);
            if (doi != null && doi.length() > 0) {

              log.debug3("=========Processing DIV: DOI div#" + Integer.toString(i) + ", for url = " + url + ", raw doi = " + doiElement.text());

              log.debug3("DOI cleaned: = " + doi + ", url = " + url);
              am.put(MetadataField.FIELD_DOI, doi.replace("doi:", "").replace("DOI:", "").trim());


              if (articleElement != null) {

                String rawArticleTitle = articleElement.text();
                articleTitle = articleElement.text().replace("<span>", "").replace("</span>", "")
                        .replace("<i>", "").replace("</i>", "")
                        .replace("<b>", "").replace("</b>", "")
                        .replace("■", "")
                        .trim().toLowerCase();
                log.debug3("Raw article text: = " + rawArticleTitle + ", cleaned_articleTitle := " + articleTitle + ", url = " + url + ", div#" + Integer.toString(i));
                if (articleTitle != null && articleTitle.length() > 0) {
                  am.put(MetadataField.FIELD_ARTICLE_TITLE, articleTitle.trim());
                }
              }

              Matcher mat = ISSUE_PATTERN.matcher(cu.getUrl());
              if (mat.find()) {
                String issue = mat.group(2);
                am.put(MetadataField.FIELD_ISSUE, issue);
              }

              emitter.emitMetadata(cu, am);
            }
        }

        } // end for loop for div

      } catch (IOException e) {
        return null;
      }
      return extraMetadata;
    }
  }
}
 
