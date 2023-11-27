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

package org.lockss.plugin.swjpcc;

import java.io.BufferedReader;
import java.util.Iterator;
import java.util.regex.*;

import org.apache.commons.lang.StringUtils;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.IOUtil;
import org.lockss.util.Logger;

/*
 *  A very custom site - do the best we can
 *  Iterate on the html articles and use then try to scrape the citation information - doi, title
 *  get the PDF from the link
 *  fall back to the url structure
 *  No other metadata so no need to call extractor separately
 *
 * For example: this url
 * http://www.swjpcc.com/critical-care/2012/11/8/fatal-dynamic-hyperinflation-secondary-to-a-blood-clot-actin.html
 * has this in it:
 /*    <p>Reference as: Raschke RA. Fatal dynamic hyperinflation secondary to a blood clot acting as a one-way valve at the internal orifice of a tracheostomy tube. Southwest J Pulm Crit Care 2012;5:256-61. <a href="/storage/manuscripts/volume-5/swjpcc-086-12/SWJPCC%20086-12.pdf">PDF</a></p>
 * as well as this meta tag
 *    <meta property="og:title" content="SOUTHWEST JOURNAL of PULMONARY &amp; CRITICAL CARE - CRITICAL CARE - Fatal Dynamic Hyperinflation Secondary to a Blood Clot Acting As a One-Way Valve at the Internal Orifice of a Tracheostomy&nbsp;Tube" /> 
 */


public class SwjpccArticleIteratorFactory
implements ArticleIteratorFactory,
ArticleMetadataExtractorFactory {

  private static final Logger log =
      Logger.getLogger(SwjpccArticleIteratorFactory.class);


  //http://www.swjpcc.com/critical-care/2012/11/8/fatal-dynamic-hyperinflation-secondary-to-a-blood-clot-actin.html
  private static final Pattern HTML_ART_PATTERN = 
      Pattern.compile("swjpcc\\.com/[^/]+/[\\d]+{4}/[\\d]+/[\\d]+/[^/]+\\.html?$",Pattern.CASE_INSENSITIVE);
  private static final String ISSUE_LANDING = "/issues/";

  /*
   * If it's there (if it has a doi)
   * <p>Reference as: Robbins RA. October 2013 Arizona thoracic society notes. Southwest J Pulm Crit Care. 2013;7(4):253-4. doi: <a href="http://dx.doi.org/10.13175/swjpcc144-13">http://dx.doi.org/10.13175/swjpcc144-13</a> <a href="/storage/manuscripts/volume-7/issue-4-october-2013/144-13/144-13.pdf">PDF</a></p>
   */
  private static final String ogTitleRegExp = "<meta\\s+property=\"og:title\"\\s+.*content=\"([^\"]+)";
  private static Pattern ogTitlePattern = Pattern.compile(ogTitleRegExp, Pattern.CASE_INSENSITIVE);

  
  private static final String doiRegExp = "doi:\\s*<a\\s+href=[^>]+>\\s*https?://(dx\\.)?doi\\.org/([^/]+/[^ <\"]+)"; //group 1 = doi
  private static final String pdfRegExp = "(/storage/(manuscripts|pdf-version-of-articles)/[^\"]+\\.pdf)\">\\s*PDF"; // group 1 == rel link to pdf
  private static Pattern doiPattern = Pattern.compile(doiRegExp, Pattern.CASE_INSENSITIVE);
  private static Pattern pdfPattern = Pattern.compile(pdfRegExp, Pattern.CASE_INSENSITIVE);
  private static final String citeRegExp = "(Cite as|Reference as):(.*)";
  private static Pattern citePattern = Pattern.compile(citeRegExp, Pattern.CASE_INSENSITIVE);
  // Southwest J Pulm Crit Care. 2018;16(2):81-2. 
  private static Pattern volIssuePat = Pattern.compile("[^<]+Southwest J Pulm Crit Care\\.\\s+([0-9]{4});([0-9]+)\\(([0-9]+)\\):([0-9]+)", Pattern.CASE_INSENSITIVE);

  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
      MetadataTarget target)
          throws PluginException {
    return new
        SwjpccArticleIterator(au,
            new SubTreeArticleIterator.Spec()
        .setTarget(target)
        .setPattern(HTML_ART_PATTERN));
  }

  protected static class SwjpccArticleIterator
  extends SubTreeArticleIterator {


    private ArchivalUnit au;

    public SwjpccArticleIterator(ArchivalUnit au,
        SubTreeArticleIterator.Spec spec) {
      super(au, spec);
      this.au = au;
    }

    /*
     * createArticleFiles(CachedUrl cu)
     *   Rather than matching a PDF file (as is common), matching the abstract
     *   to information in the html to identify the associated PDF
     * (non-Javadoc)
     * @see org.lockss.plugin.SubTreeArticleIterator#createArticleFiles(org.lockss.plugin.CachedUrl)
     */
    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {
      String url = cu.getUrl();
      
      log.debug3("createArticleFiles: " + url);
      if (url.contains(ISSUE_LANDING)) {
        // not an article, just a volume issue listing page
        return null;
      }

      // we can't even get here without the pattern match of an html article, so no need to check
      return processFromHtml(cu);
    }

    /*
     * processWithMetadata(CachedUrl absCu)
     *   Given the CachedUrl for the abstract file, 
     *   scrape the information necessary for the doi (if available)
     *   and the PDF link from the html
     */
    private ArticleFiles processFromHtml(CachedUrl absCu) {
      ArticleFiles swaf = new SwjpccArticleFiles();
      String pdflink = null;
      String doi = null;
      CachedUrl pdfCu = null;
      log.debug3("processFromHtl: " + absCu);

      swaf.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_HTML, absCu);
      swaf.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, absCu);
      // a default value if we don't get a PDF
      swaf.setFullTextCu(absCu);
      if (absCu !=null && absCu.hasContent()){
        // get the content
        BufferedReader bReader = new BufferedReader(absCu.openForReading());
        //        BufferedReader bReader = new BufferedReader(openForReading(absCu));

        try {
          Matcher matcher;
          // go through the cached URL content line by line
          for (String line = bReader.readLine(); line != null; line = bReader.readLine()) {
            line = line.trim();
            matcher = ogTitlePattern.matcher(line);
            if (matcher.find()) {
              String title = matcher.group(1);
              log.debug3("LINETITLE: " + title);
              if (title.startsWith("SOUTHWEST JOURNAL of PULMONARY")) {
                  int hyphenIndex = title.indexOf("-");
                  if ((hyphenIndex >= 0 ) && (title.length() > hyphenIndex + 1)) { 
                  title = (StringUtils.substring(title,hyphenIndex+1)).trim(); // get the title starting after first hyphen
                  }
              }
              ((SwjpccArticleFiles) swaf).setFoundTitle(title);
            }
            matcher = citePattern.matcher(line);
            if (matcher.find()) {
              log.debug3("LINECITE: " + line);
              Matcher vMat = volIssuePat.matcher(matcher.group(2));
              if (vMat.find()) {
                log.debug3("FOUNDCITE: " + matcher.group(2));
                String year = vMat.group(1);
                String vol = vMat.group(2);
                String iss = vMat.group(3);
                String start = vMat.group(4);
                ((SwjpccArticleFiles) swaf).setFoundCite(year,vol,iss,start);
              }
            }
            // regexes to extract DOI, etc. from articles
            matcher = doiPattern.matcher(line);
            if (matcher.find()) {
              doi =  matcher.group(2).trim();
              log.debug3("FOUNDDOI: " + doi);
              ((SwjpccArticleFiles) swaf).setFoundDoi(doi);
            }
            matcher = pdfPattern.matcher(line);
            if (matcher.find()) {
              log.debug("FOUND PDFLINK: " + pdflink);
              pdflink = matcher.group(1).trim();
            }
            if (pdflink != null) {
              // doi may or may not exist but if it does doi, citation and title are before the pdf link
              break;
            }
          }
        } catch (Exception e) {
          log.debug(e + " : Malformed Pattern");
        }
        finally {
          IOUtil.safeClose(bReader);
        }
      }
      if (pdflink != null) {
        pdfCu = au.makeCachedUrl("http://www.swjpcc.com" + pdflink);
      }
      if (pdfCu != null && pdfCu.hasContent()) {
        log.debug3("  setROLE_PDF_URL: " + pdfCu);
        swaf.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, pdfCu);
        swaf.setFullTextCu(absCu);
      } else {
        log.debug3("NO pdf found for this cu: "+absCu);
      }
      AuUtil.safeRelease(pdfCu);
      return swaf;
    }

  }

  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new SwjpccArticleMetadataExtractor();

  }
  
  /*                                                                                                                                                                                        
   * An extended version of an ArticleFiles object. It adds an additional                                                                                                                   
   * field - the doi.
   * We have to parse the article to find the PDF link so we also pick up 
   * the doi if we can find it. No reason to reparse.                                                                                                                                  
   */
  public static class SwjpccArticleFiles
  extends ArticleFiles {
    private String foundDoi = null;
    private String foundYear = null;
    private String foundVol = null;
    private String foundIss = null;
    private String foundStart = null;
    private String foundTitle = null;

    SwjpccArticleFiles() {
      super();
    }

    void setFoundDoi(String fdoi) {
      foundDoi = fdoi;
    }

    void setFoundTitle(String title) {
      foundTitle = title;
    }
    
    void setFoundCite(String year, String vol, String iss, String sp) {
      log.debug3("Setting cited scraped MD: " + year + ", " + vol + ", " + iss + ", " + sp);
      foundYear = year;
      foundVol = vol;
      foundIss = iss;
      foundStart = sp;
    }

    String getFoundDoi() {
      return foundDoi;
    }
    String getFoundYear() {
      return foundYear;
    }
    String getFoundVol() {
      return foundVol;
    }
    String getFoundIss() {
      return foundIss;
    }
    String getFoundStart() {
      return foundStart;
    }

    String getFoundTitle() {
      return foundTitle;
    }
  
  
  }


}
