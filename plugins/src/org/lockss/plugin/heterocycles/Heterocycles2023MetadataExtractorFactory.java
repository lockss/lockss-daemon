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
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
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
public class Heterocycles2023MetadataExtractorFactory
  implements FileMetadataExtractorFactory {
  
  static Logger log = Logger.getLogger(Heterocycles2023MetadataExtractorFactory.class);
  
  public FileMetadataExtractor createFileMetadataExtractor(
      MetadataTarget target, String contentType) throws PluginException {
    return new Heterocycles2023MetadataExtractor();
  }

  // Gets default metadata from tdb: date, journal.title, publisher, 
  public static class Heterocycles2023MetadataExtractor
    implements FileMetadataExtractor {

    static String PDF_ACCESS_URL = "pdf_access_url";

    private Pattern ISSUE_PATTERN = Pattern.compile(
        "/libraries/journal/([^/]+)/([^/]+)$", Pattern.CASE_INSENSITIVE);

    ArrayList<String> issuePages = new ArrayList<String>();

    @Override
    public void extract(MetadataTarget target, 
        CachedUrl cu, Emitter emitter) throws IOException {

      //Metadata - cachedurl pdf cu:http://www.heterocycles.jp/clockss/downloads/PDF/27526/102/12
      log.debug3("Metadata - cachedurl pdf cu:" + cu.getUrl());

      if (!issuePages.contains(cu.getUrl())) {

        log.debug3("Metadata - adding unique cachedurl pdf cu:" + cu.getUrl());

        issuePages.add(cu.getUrl());

        getAdditionalMetadata(cu, emitter);

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
          String articleSelector = "div#mainContainer > div#mainContent > div.contentBox:nth-child(" + Integer.toString(i) + ")";


          String pdfSelector = "div#mainContainer > div#mainContent > div.contentBox:nth-child(" + Integer.toString(i) + ") > a";
          pdfElement = doc.select(pdfSelector);

          if ( pdfElement != null) {
            for (Element pdfLinkElement : pdfElement) {
              pdfLink = pdfLinkElement.attr("href").trim().toLowerCase();
              finalPDFLink = url.substring(0, url.indexOf("/clockss")) + pdfLink;
              log.debug3("final pdf text: = " + finalPDFLink + ", url = " + url );
              if (finalPDFLink != null && finalPDFLink.length() > 0 && finalPDFLink.contains("downloads/pdf")) {

                log.debug3("final pdf text: = " + finalPDFLink + ", url = " + url  + ", set FIELD_ACCESS_URL");

                //Has to do this, otherwise,
                am.put(MetadataField.FIELD_ACCESS_URL, finalPDFLink.trim().replace("/pdf/","/PDF/"));
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
            log.debug3("=========Processing DIV: Article div#" + Integer.toString(i) + ", for url = " + url + ", nthDivHolder = " + nthDivHolderElement.text());
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

                articleTitle = articleElement.text().replace("<span>", "").replace("</span>", "").trim().toLowerCase();
                log.debug3("Raw article text: = " + articleTitle + ", url = " + url + ", div#" + Integer.toString(i));
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
 
