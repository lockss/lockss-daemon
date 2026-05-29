/*

Copyright (c) 2000-2026, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.bmp;

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

public class BMPArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory{
    
    protected static Logger log = Logger.getLogger(BMPArticleIteratorFactory.class);

    protected static Pattern TOC_PATTERN = Pattern.compile("https://agridergisi\\.com/issue/([0-9]+)$", Pattern.CASE_INSENSITIVE);

    protected static String TOC_REPLACEMENT = "https://agridergisi\\.com/issue/$1";

    protected static String ROLE_TOC = "TOC";

    @Override
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) throws PluginException {
        ArrayList<ArticleFiles> articles = new ArrayList<>();

        /*Find all the TOCs*/
        SubTreeArticleIteratorBuilder sb = new SubTreeArticleIteratorBuilder(au);
        Spec spec = new Spec();
        spec.setPattern(TOC_PATTERN);
        sb.setSpec(spec);
        sb.addAspect(TOC_PATTERN, TOC_REPLACEMENT, ROLE_TOC);
        
        /*Parse the TOC to find all articles */
        Iterator<ArticleFiles> tocIterator = sb.getSubTreeArticleIterator();
        for(ArticleFiles tocAF : IteratorUtils.asIterable(tocIterator)){
            log.debug3("I found a TOC " + tocAF.getFullTextUrl());
            CachedUrl tocCU = tocAF.getFullTextCu();
            parseToc(tocCU, articles, au);
        }
        
        return articles.iterator();
    }


    public void parseToc(CachedUrl tocCU, ArrayList<ArticleFiles> results, ArchivalUnit au){

        CachedUrl pdfUrl = null;
        CachedUrl abstractsUrl = null;
        CachedUrl doisUrl = null;

        try{
            Document doc = Jsoup.parse(tocCU.getUncompressedInputStream(), AuUtil.getCharsetOrDefault(tocCU.getProperties()), tocCU.getUrl());
            Elements articles = doc.select("div.span9>section[id*=cat]>div");
            ArrayList<String> rolesForMetadata = new ArrayList<>();
            for (Element article : articles) {
                ArticleFiles af = new ArticleFiles();

                Elements PDFs = article.select("ul.article-nav-bottom>li>a:contains(PDF)");
                pdfUrl = au.makeCachedUrl("https://agridergisi.com" + PDFs.attr("href").trim());

                Elements abstracts = article.select("h3>a[href*=abstract]");
                abstractsUrl = au.makeCachedUrl("https://agridergisi.com" + abstracts.attr("href").trim());

                //needs fixing
                Elements DOIs = article.select("a[href*=doi]");
                doisUrl = au.makeCachedUrl(DOIs.attr("href").trim());

                addToListOfRoles(pdfUrl, af, ArticleFiles.ROLE_FULL_TEXT_PDF);
                addToListOfRoles(abstractsUrl, af, ArticleFiles.ROLE_ABSTRACT);
                addToListOfRoles(doisUrl, af, ArticleFiles.ROLE_ARTICLE_HANDLE);
                af.setFullTextCu(pdfUrl);

                if(af.getFullTextCu() == null){
                    log.debug3("There is no full text. The abstract CU is " + abstractsUrl.toString());
                }

                if ((abstractsUrl != null) && abstractsUrl.hasContent()) {
                    log.debug3("Setting metadata role to found abstracts page.");
                    af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, abstractsUrl);
                }else{
                    log.debug3("There is no abstracts page, setting metadata role to TOC.");
                    af.setRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA, tocCU);
                }
                results.add(af);
            }
        }catch(IOException ioe){
            log.debug("Error parsing CU", ioe);
        }finally{
            AuUtil.safeRelease(pdfUrl);
            AuUtil.safeRelease(abstractsUrl);
        }
        
    }

    public void addToListOfRoles(CachedUrl cu, ArticleFiles af, String role){
        if(cu.hasContent()){
            log.debug3("The cu has content");
            af.setRole(role,cu);
            log.debug3("The  URL is " + cu.toString() + " and the role is " + role);
        } return;
    }

    @Override
    public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
        throws PluginException {
        return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
    }
}
