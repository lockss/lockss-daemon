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

package edu.fcla.plugin.arkivoc;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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

/*
 No ariticle level page, the metadata
 */
public class ArkivocMetadataExtractorFactory
  implements FileMetadataExtractorFactory {
  
  static Logger log = Logger.getLogger(ArkivocMetadataExtractorFactory.class);
  
  public FileMetadataExtractor createFileMetadataExtractor(
      MetadataTarget target, String contentType) throws PluginException {
    return new ArkivocMetadataExtractor();
  }

  // Gets default metadata from tdb: date, journal.title, publisher, 
  public static class ArkivocMetadataExtractor
    implements FileMetadataExtractor {

    static String PDF_ACCESS_URL = "pdf_access_url";

    private Pattern ISSUE_PATTERN = Pattern.compile(
        "/arkivoc-journal/browse-arkivoc/(\\d+)/(\\d+)$", Pattern.CASE_INSENSITIVE);

    ArrayList<String> issuePages = new ArrayList<String>();

    @Override
    public void extract(MetadataTarget target, 
        CachedUrl cu, Emitter emitter) throws IOException {

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
      <tr>
			<td>
				<font class="page-manuscript">
					<b>1. A tribute to Professor Zbigniew Czarnocki</b> (ZC-1482ST)
					<br><i>Joanna Szawkało</i><br>DOI: <a href="https://doi.org/10.24820/ark.5550190.p001.482" target="_blank">https://doi.org/10.24820/ark.5550190.p001.482</a>					<br>
					Full Text: <a href="/get-file/69581/" target="_blank" style="text-decoration:none;">PDF (375K)</a><br>Export Citation: <a href="/get-ris/1482/" target="_blank" style="text-decoration:none;">RIS</a><br>
					<b>pp. 1 - 8</b>

					<br>
					published May 25 2020;				</font>
			</td>
	  </tr>

	  CSS Selector: body > div:nth-child(1) > table:nth-child(3) > tbody:nth-child(1) > tr:nth-child(1) > td:nth-child(1) > table:nth-child(1) > tbody:nth-child(1) > tr:nth-child(1) > td:nth-child(2) > table:nth-child(1) > tbody:nth-child(1) > tr:nth-child(1) > td:nth-child(1) > table:nth-child(2) > tbody:nth-child(1) > tr:nth-child(1) > td:nth-child(1) > div:nth-child(3) > table:nth-child(3) > tbody:nth-child(1) > tr:nth-child(2) > td:nth-child(1) > font:nth-child(1) > b:nth-child(1)
	  CSS Path: html body div table tbody tr td table tbody tr td table tbody tr td table tbody tr td div table tbody tr td font.page-manuscript b
     */

    protected HashMap<String, String> getMoreValue(InputStream in, String encoding, CachedUrl cu, Emitter emitter) {

      Elements mainContentElement;

      Elements nthTrHolderElement;
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
        
        String trCssSelector = "body > div:nth-child(1) > table:nth-child(3) > tbody:nth-child(1) > tr:nth-child(1) > td:nth-child(1) > table:nth-child(1) > tbody:nth-child(1) > tr:nth-child(1) > td:nth-child(2) > table:nth-child(1) > tbody:nth-child(1) > tr:nth-child(1) > td:nth-child(1) > table:nth-child(2) > tbody:nth-child(1) > tr:nth-child(1) > td:nth-child(1) > div:nth-child(3) > table:nth-child(3) > tbody:nth-child(1) > tr";

        mainContentElement = doc.select(trCssSelector);

        int trCount = 1;

        if ( mainContentElement != null) {
          for (Element tr : mainContentElement) {
              trCount++;
          }
        } else {
          log.debug3("----contentBox count is not selected =========== ");
        }

        log.debug3("----url= " + url + ", contentBox count =========== " + Integer.toString(trCount));

        for (int i = 1; i <= trCount + 1; i++) {

          log.debug3("=========Processing tr#" + Integer.toString(i) + ", for url = " + url);

          //////////////Create ArticleMetadata for each div////////////
          ArticleMetadata am = new ArticleMetadata();


          String nthTrSelector = trCssSelector + ":nth-child(" + Integer.toString(i) + ")";
          nthTrHolderElement = doc.select(nthTrSelector);

          log.debug3("=========Processing Tr Element: nthTrSelector : " + nthTrSelector);
          log.debug3("=========Processing Tr Element: " + nthTrHolderElement);

          //CssSelector and CssPath does not work inside the tr element anyore

          String doiSelector = "trCssSelector(" + Integer.toString(i) + ") > div:nth-child(4)";
          String articleSelector = "trCssSelector(" + Integer.toString(i) + ")";
          String pdfSelector = "trCssSelector(" + Integer.toString(i) + ") > a:nth-child(11)";
          String pdfAlterSelector = "trCssSelector(" + Integer.toString(i) + ") > a:nth-child(9)";
          String pdfAlterSelector2 = "trCssSelector(" + Integer.toString(i) + ") > a:nth-child(7)";

          String[] pdfSelectors = new String[3];
          pdfSelectors[0] = pdfSelector;
          pdfSelectors[1] = pdfAlterSelector;
          pdfSelectors[2] = pdfAlterSelector2;


          for (int pi = 0; pi < pdfSelectors.length; pi++) {

            log.debug3("=========Processing DIV: PDF tr#" + Integer.toString(i) + ", for url = " + url + ", pdfSelectors#" + Integer.toString(pi) + ", pdfSelector = " + pdfSelectors[pi]);

            pdfElement = doc.select(pdfSelectors[pi]);

            if (pdfElement != null) {
              pdfLink = pdfElement.attr("href").trim().toLowerCase();
              finalPDFLink = url.substring(0, url.indexOf("/clockss")) + pdfLink;
              log.debug3("final pdf text: = " + finalPDFLink + ", url = " + url + ", tr#" + Integer.toString(i) + ", pdfSelectors#" + Integer.toString(pi));
              if (finalPDFLink != null && finalPDFLink.length() > 0 && finalPDFLink.contains("downloads/pdf")) {

                log.debug3("final pdf text: = " + finalPDFLink + ", url = " + url + ", tr#" + Integer.toString(i) + ", pdfSelectors#" + Integer.toString(pi) + ", set FIELD_ACCESS_URL");

                am.put(MetadataField.FIELD_ACCESS_URL, finalPDFLink.trim());
              }
            } else {
              log.debug3("=========Processing DIV: PDF tr#" + Integer.toString(i) + ", for url = " + url + ", pdfSelectors#" + Integer.toString(pi) + ", PDFElement null");
            }
          }


          nthTrHolderElement = doc.select(nthTrSelector);
          doiElement = doc.select(doiSelector);

          articleElement = doc.select(articleSelector);

          if (nthTrHolderElement == null) {
            log.debug3("=========Processing DIV: Article tr#" + Integer.toString(i) + ", for url = " + url + ", nthDivHolder = " + nthTrHolderElement.text());
          }

          if (doiElement != null) {

            log.debug3("=========Processing DIV: Article tr#" + Integer.toString(i) + ", for url = " + url + ", nthDivHolder = " + nthTrHolderElement.text());

            doi = doiElement.text().trim().toLowerCase();
            log.debug3("=========Processing DIV: tr#" + Integer.toString(i) + ", raw doi text: = " + doiElement + ", url = " + url);
            if (doi != null && doi.length() > 0) {

              log.debug3("=========Processing DIV: DOI tr#" + Integer.toString(i) + ", for url = " + url + ", raw doi = " + doiElement.text());

              log.debug3("DOI cleaned: = " + doi + ", url = " + url);
              am.put(MetadataField.FIELD_DOI, doi.replace("doi:", "").replace("DOI:", "").trim());


              if (articleElement != null) {

                articleTitle = articleElement.text().replace("<span>", "").replace("</span>", "").trim().toLowerCase();
                log.debug3("Raw article text: = " + articleTitle + ", url = " + url + ", tr#" + Integer.toString(i));
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
 
