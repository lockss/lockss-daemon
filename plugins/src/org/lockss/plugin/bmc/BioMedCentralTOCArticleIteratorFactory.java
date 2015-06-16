/*
 * $Id:$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.bmc;

import java.io.*;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.MalformedURLException;
import java.util.*;
import java.util.regex.*;

import org.jsoup.Jsoup;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.*;


/*
 * BioMedCentralTOCArticleIterator processes the table of contents collected for
 * an AU (Archival Unit).  It pulls the article facets from this.
 * 
 * The iterator cannot be guaranteed to work backwards from pdf to landing page
 * The landing page isn't easy to identify without risking picking up other stuff
 * The abstract isn't guaranteed to be there
 * So use the TOC to pick up the guaranteed landing page
 *    from this, get the abstract if available
 *    pull the pdf name from either the TOC link or from the metadata (TBD)
 * 
 * Issues page or TOC URL:
 *   there are three variations
 *      <base_url>/content/<vol>/# <--# is issue, but doesn't matter
 *      <base_url>/content/<vol>/Month/<year> <--April, May, etc
 *      <base_url>/supplments/<vol>/S#  <-- supplementary issues
 *      
 *  examples:
 *  http://www.genomebiology.com/content/14/8 (volume 14, issue 8)
 *  http://www.wjso.com/content/12/August/2014  (volume 12, August)
 *  http://www.actavetscand.com/content/52/November/2010 (volume 52)
 *  http://bsb.eurasipjournals.com/content/2014/September/2014 (volume 2014)    
 *  http://www.genomebiology.com/supplements/9/S2
 *  http://www.actavetscand.com/supplements/52/S1
 * 
 * Articles URL:
 *   there are two basic variations but legacy old volumes are inconsistent
 *   <base_url>/content/<vol>/#/<id>  <--where id is usually start page but could be letter/num combo
 *   <base_url>/<year>/<vol>
 *   
 *   examples:
 *   http://www.wjso.com/content/12/1/193 (where volume is a number)
 *   http://bsb.eurasipjournals.com/content/2014/1/18 (where volume is a year)
 *   http://www.genomebiology.com/2013/14/11/314  (where volume is 14 but use year instead of "content")
 *   
 *   supplements articles:
 *   http://www.genomebiology.com/2008/9/S1/S3  (volume 9, year is in place of "content")
 *   http://breast-cancer-research.com/content/14/S1/O2
 *   http://www.actavetscand.com/content/50/S1/S4  (final identifier can have various letters, A2, O1, P3)
 *   
 *   and then the legacy weird ones that make this harder
 *   http://breast-cancer-research.com/content/14/4/R104
 *   http://genomebiology.com/2002/3/7/research/0032
 *           other words used to distinguish type of article - review, reports, comment
 */

public class BioMedCentralTOCArticleIteratorFactory
implements ArticleIteratorFactory,
ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger(BioMedCentralTOCArticleIteratorFactory.class);

  protected static Pattern ISSUE_TOC_PATTERN =
      Pattern.compile("/(content|supplements)/\\d+/((S)?\\d+|(January|February|March|April|May|June|July|August|September|October|November|December)/\\d+)$", Pattern.CASE_INSENSITIVE);

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
      MetadataTarget target)
          throws PluginException {
    String base_url = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
    //String volume_name = au.getConfiguration().get(ConfigParamDescr.VOLUME_NAME.getKey());

    return new BioMedCentralTOCArticleIterator(au,
        new SubTreeArticleIterator.Spec()
    .setTarget(target)
    //.setRoot(root) //unnecessary, root is really just base_url
    .setPattern(ISSUE_TOC_PATTERN), base_url);
  }


  protected static class BioMedCentralTOCArticleIterator extends SubTreeArticleIterator {

    protected MetadataTarget target;
    protected Set<String> alreadyEmitted;
    protected ArticleFiles nextAF = null;
    protected boolean checkedOnce = false;
    protected String base_url;

    private static final String ABSTRACT_LABEL = "abstract";
    private static final String FULLTEXT_LABEL = "full text";
    private static final String PDF_LABEL = "pdf";
    private static final Map<String, String> classLabelMap =
        new HashMap<String,String>();
    static {
      classLabelMap.put("abstract-link", ABSTRACT_LABEL);
      classLabelMap.put("fulltext-link", FULLTEXT_LABEL); 
      classLabelMap.put("pdf-link", PDF_LABEL);
    } 

    private static final Set<String> ASPECT_SET =
        Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(ABSTRACT_LABEL,
                                                                      FULLTEXT_LABEL,
                                                                      PDF_LABEL)));
    // Constructor
    public BioMedCentralTOCArticleIterator(ArchivalUnit au,
        SubTreeArticleIterator.Spec spec, String base_url) {
      super(au, spec);
      this.alreadyEmitted = new HashSet<String>();
      this.base_url = base_url;
    }



    @Override
    protected ArticleFiles createArticleFiles(CachedUrl cu) {

      processToc(cu.getUnfilteredInputStream(), cu.getEncoding(), cu.getUrl());
      return null;

    }


    protected void processToc(InputStream in, String encoding, String url) {

      Elements art_els;
      try {
        Document doc = Jsoup.parse(in, encoding, url);

        art_els = doc.select("td.article-entry"); // td with class="article-entry"
        log.debug3("Processing articles");
      } catch (IOException e) {
        log.debug3("Error parsing TOC", e);
        return;
      }

      for (Element art_el : art_els) {
        log.debug3("Processing one article");
        //log.debug3(art_el.ownText());
        processArticle(art_el);
      }

    }


    protected void processArticle(Element art_el) {
      Map<String, CachedUrl> map = new HashMap<String, CachedUrl>();
      Elements el_links = art_el.select("a[href]"); // a with href - only throws if query is invalid

      for (Element link : el_links) {
        CachedUrl linkCu = null;

        String link_string = link.attr("href");
        if (! link_string.startsWith(base_url)) {
          continue; // ignore relative urls
        }
        String link_class = link.attr("class");
        String label = classLabelMap.get(link_class);
        if (label == null){  
          label = link.text().trim().toLowerCase();
        }
        // Don't pick up links that we don't care about
        // if it's not in the set of aspects we understand then just ignore it
        if (! ASPECT_SET.contains(label) ){
          continue;
        }

        try {
          String linkUrl = UrlUtil.normalizeUrl(link_string,au);
          linkCu = au.makeCachedUrl(linkUrl);
          if (linkCu != null) {
            if (linkCu.hasContent()) {
              if (!map.containsKey(label)) {
                log.debug3(label + " -> " + linkUrl);
                map.put(label, linkCu);
              }
            } else {
              AuUtil.safeRelease(linkCu);
            }
          }
        }
        catch (PluginBehaviorException pbe) {
          log.debug3("Plugin behavior exception", pbe);
          continue; // Ignore
        }
        catch (MalformedURLException mue) {
          log.debug3("Malformed URL exception", mue);
          continue; // Ignore
        }
      }
      // we have picked up all the article items from the article-entry
      // now create the ArticleFiles with what we found

      ArticleFiles af = new ArticleFiles();
      if (map.containsKey(ABSTRACT_LABEL)) {
        CachedUrl cu = map.get(ABSTRACT_LABEL);
        log.debug3("Found candidate abstract: " + cu.getUrl());
        af.setRoleCu(ArticleFiles.ROLE_ABSTRACT, cu);
        af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, cu);
      }
      if (map.containsKey(PDF_LABEL)) {
        CachedUrl cu = map.get(PDF_LABEL);
        log.debug3("Found candidate pdf: " + cu.getUrl());
        af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF, cu);      
        af.setFullTextCu(cu);
      }
      if (map.containsKey(FULLTEXT_LABEL)) {
        CachedUrl cu = map.get(FULLTEXT_LABEL);
        log.debug3("Found candidate full text: " + cu.getUrl());
        af.setRoleCu(ArticleFiles.ROLE_FULL_TEXT_HTML, cu);
        if (af.getFullTextCu() == null) {
          af.setFullTextCu(cu);
        }
        // fallback for metadata when there is no abstract
        if (af.getRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA) == null) {
          af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, cu);
        }
      }  
      //TODO - could guess at alternatives? could look for "additional" for data

      // Emit
      CachedUrl cu = af.getFullTextCu();
      if (cu != null) {
        log.debug3("getfulltextcu url: " + cu.getUrl());

        if (!alreadyEmitted.contains(cu.getUrl())) {
          alreadyEmitted.add(cu.getUrl());
          emitArticleFiles(af);
        }
      }

      // Clean up
      for (CachedUrl releaseCu : map.values()) {
        AuUtil.safeRelease(releaseCu);
      }

    }



  } 

  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {

    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);

  } // createArticleMetadataExtractor

} // BioMedCentralTOCArticleIteratorFactory
