/*

Copyright (c) 2000-2024, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.ojs3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.apache.commons.collections4.IteratorUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.ArticleIteratorFactory;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.SubTreeArticleIterator.Spec;
import org.lockss.util.Logger;
import org.lockss.plugin.SubTreeArticleIteratorBuilder;

public class Ojs3TocParsingArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory{

    protected static Logger log = Logger.getLogger(Ojs3TocParsingArticleIteratorFactory.class);

    protected static Pattern SHOW_TOC_PATTERN =
    Pattern.compile("/issue/view/([^/]+)/showToc$", Pattern.CASE_INSENSITIVE);
    protected static Pattern PLAIN_TOC_PATTERN =
    Pattern.compile("/issue/view/([^/]+)$", Pattern.CASE_INSENSITIVE);
    protected static Pattern ALL_TOC_PATTERN = 
    Pattern.compile(SHOW_TOC_PATTERN.pattern() + "|" + PLAIN_TOC_PATTERN.pattern());

    protected static String SHOW_TOC_REPLACEMENT = "/issue/view/$1/showToc";
    protected static String PLAIN_TOC_REPLACEMENT = "/issue/view/$1";

    protected static String ROLE_TOC = "TOC";

    @Override
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) throws PluginException {

        ArrayList<ArticleFiles> articles = new ArrayList<>();

        /*Find all the TOCs*/
        SubTreeArticleIteratorBuilder sb = new SubTreeArticleIteratorBuilder(au);
        Spec spec = new Spec();
        spec.setPattern(ALL_TOC_PATTERN);
        sb.setSpec(spec);
        sb.addAspect(Arrays.asList(SHOW_TOC_PATTERN, PLAIN_TOC_PATTERN), Arrays.asList(SHOW_TOC_REPLACEMENT, PLAIN_TOC_REPLACEMENT), ROLE_TOC);
        
        /*Parse the TOC to find all articles */
        Iterator<ArticleFiles> tocIterator = sb.getSubTreeArticleIterator();
        for(ArticleFiles tocAF : IteratorUtils.asIterable(tocIterator)){
            log.debug3("I found a plain TOC " + tocAF.getFullTextUrl());
            CachedUrl tocCU = tocAF.getFullTextCu();
            parseToc(tocCU, articles, au);
        }
        
        return articles.iterator();
    }

    public void parseToc(CachedUrl tocCU, ArrayList<ArticleFiles> results, ArchivalUnit au){

        /* Examples of where articles are located on TOC pages:
         *  MOST COMMON
         *  https://medicaljournalssweden.se/JPHS/issue/view/2155 <div class="obj_article_summary">
         * 
         * 
         *  https://raccefyn.co/index.php/raccefyn/issue/view/197 - <div class = article-summary>
         *  https://blakequarterly.org/index.php/blake/issue/view/84 - <article class = article>
         *  https://journals.whitingbirch.net/index.php/SWSSR/issue/view/196 - li inside of <ul class = cmp_article_list articles>
         *  https://journals.vilniustech.lt/index.php/BMEE/issue/view/1296 - <div class="article-summary media">
         */
        CachedUrl pdfUrl = null;
        CachedUrl htmlUrl = null;
        CachedUrl xmlUrl = null;
        CachedUrl epubUrl = null;
        CachedUrl abstractsUrl = null;

        try{
            Document doc = Jsoup.parse(tocCU.getUnfilteredInputStream(), AuUtil.getCharsetOrDefault(tocCU.getProperties()), tocCU.getUrl());
            Elements articles = doc.select("div.article-summary,article.article,ul.articles>li,div.one-article-intoc,article.article_summary,div.article-sum,"
                +"ul.it-list>li>div.it-right-zone,article.equal,div.grid-child:has(div>div.media-body),ul.row>li.issue__article,div.page-issue-galleys:has(div.h3:contains(Número completo)),"
                +"div.galleys:has(h2:contains(Número completo)),div.galleys:has(h2:contains(Full Issue)),div.altex-issue-article,div.altex-issue-editorial");
            ArrayList<String> rolesForFullText = new ArrayList<>();
            for (Element article : articles) {
                ArticleFiles af = new ArticleFiles();

                Elements PDFs = article.select("div.article-summary-galleys>a[href*=article]:contains(PDF),ul.article__btn-group>li>a[href*=article]:contains(PDF),ul.galleys_links>li>a[href*=article]:contains(PDF),"
                    +"div.galleys_links>a[href*=article]:contains(PDF),div.btn-group>a[href*=article].pdf,a.indexGalleyLink:contains(PDF),"
                    +"div.galleryLinksWrp>div.btnsLink>a.galley-link:contains(PDF),ul.actions>li.galley-links-items>a:has(i.fa-file-pdf),div.row>div>a.galley-link:has(span.gallery_item_link:contains(PDF)),"
                    +"a[href*=issue/view].btn:contains(PDF),ul.galleys_links>li>a.pdf[href*=issue/view],a[href*=article]:has(span:contains(pdf))");
                pdfUrl = au.makeCachedUrl(PDFs.attr("href").trim());

                Elements HTMLs = article.select("ul.galleys_links>li>a:contains(HTML),ul.article__btn-group>li>a:contains(HTML),div.btn-group>a:contains(HTML),"
                    +"div.galleys_links>a:contains(HTML),a.indexGalleyLink:contains(HTML),h3.altex-issue-article-title>a");
                htmlUrl = au.makeCachedUrl(HTMLs.attr("href").trim());

                Elements XMLs = article.select("ul.galleys_links>li>a:contains(XML),div.btn-group>a.file:contains(XML),"
                    +"div.galleys_links>a:contains(XML),div.article-summary-galleys>a:contains(XML)");
                xmlUrl = au.makeCachedUrl(XMLs.attr("href").trim());

                Elements EPUBs = article.select("ul.galleys_links>li>a.file:contains(epub)");
                epubUrl = au.makeCachedUrl(EPUBs.attr("href".trim()));

                Elements abstracts = article.select("div.article-summary-title>a[href*=article],h4.article__title>a[href*=article],div.obj_article_summary>h3.title>a[href*=article],h3.media-heading>a[href*=article],"+
                    "a.summary_title,span.article-title>a[href*=article],div.obj_article_summary>div.title>a[href*=article],a:has(span.text>h6.article-title),"
                    +"ul.actions>li>a:contains(View Article),div.obj_article_summary>h4.title>a[href*=article],div.obj_article_summary>div.card-body>h3.card-title>a[href*=article],"+
                    "div.card-body>h4.issue-article-title>a");
                abstractsUrl = au.makeCachedUrl(abstracts.attr("href").trim());

                /*
                    August 2024 - We need to be able to emit metadata for articles in different languages. If there are multiple pdfs under a single article
                    on the TOC page, they are likely the same pdf in different languages. 
                    An example can be found here: https://ojs.aut.ac.nz/linksymposium/issue/view/3 

                    There is an example that has different pdfs on the TOC page that are NOT the pdf in different languages. This was confirmed
                    as being ok to emit metadata multiple times. https://ojs.aut.ac.nz/anzjsi/issue/view/1
                */
                if(PDFs.size() > 1){
                    for(int i = 1; i < PDFs.size(); i++){
                        ArticleFiles afTemp = new ArticleFiles();
                        CachedUrl cuTemp = au.makeCachedUrl(PDFs.get(i).attr("href"));
                        afTemp.setRole(ArticleFiles.ROLE_FULL_TEXT_PDF,cuTemp);
                        afTemp.setFullTextCu(cuTemp);
                        if ((abstractsUrl != null) && abstractsUrl.hasContent()) {
                            afTemp.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, abstractsUrl);
                            afTemp.setRole(ArticleFiles.ROLE_ABSTRACT,abstractsUrl);
                        }
                        //log additional pdf urls
                        log.debug3("I am number " + i + " and I am " + PDFs.get(i));
                        results.add(afTemp);
                    }
                }

                addToListOfRoles(pdfUrl, af, rolesForFullText, ArticleFiles.ROLE_FULL_TEXT_PDF);
                addToListOfRoles(htmlUrl, af, rolesForFullText, ArticleFiles.ROLE_FULL_TEXT_HTML);
                addToListOfRoles(xmlUrl, af, rolesForFullText, ArticleFiles.ROLE_FULL_TEXT_XML);
                addToListOfRoles(epubUrl, af, rolesForFullText, ArticleFiles.ROLE_FULL_TEXT_EPUB);
                addToListOfRoles(abstractsUrl, af, rolesForFullText, ArticleFiles.ROLE_ABSTRACT);

                if (rolesForFullText.size() > 0) {
                    for (String role : rolesForFullText) {
                        log.debug3("The role is " + role);
                        CachedUrl foundCu = af.getRoleCu(role);
                        if (foundCu != null) {
                            log.debug2(String.format("Full text CU set to: %s", foundCu.getUrl()));
                            af.setFullTextCu(foundCu);
                            break;
                        }
                    }
                }
                if(af.getFullTextCu() == null){
                    log.debug3("There is no full text. The abstract CU is " + abstractsUrl.toString());
                }

                if ((abstractsUrl != null) && abstractsUrl.hasContent()) {
                    log.debug3("I'm setting up the metadata role.");
                    af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, abstractsUrl);
                }
                results.add(af);
            }
        }catch(IOException ioe){
            log.debug("Error parsing CU", ioe);
        }finally{
            AuUtil.safeRelease(pdfUrl);
            AuUtil.safeRelease(htmlUrl);
            AuUtil.safeRelease(xmlUrl);
            AuUtil.safeRelease(epubUrl);
            AuUtil.safeRelease(abstractsUrl);
        }
        
    }

    public void addToListOfRoles(CachedUrl cu, ArticleFiles af, ArrayList<String> rolesForFullText, String role){
        if(cu.hasContent()){
            log.debug3("The cu has content");
            af.setRole(role,cu);
            log.debug3("The  URL is " + cu.toString() + " and the role is " + role);
            rolesForFullText.add(role);
        } return;
    }

    @Override
    public ArticleMetadataExtractor createArticleMetadataExtractor(
        MetadataTarget target) throws PluginException {
        return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ABSTRACT);
    }
    
}
