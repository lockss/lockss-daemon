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
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.ArticleIteratorFactory;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.SubTreeArticleIterator.Spec;
import org.lockss.util.Logger;
import org.lockss.plugin.SubTreeArticleIteratorBuilder;

public class Ojs3TocParsingArticleIteratorFactory implements ArticleIteratorFactory{

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
            CachedUrl tocCU = tocAF.getFullTextCu();
            parseToc(tocCU, articles);
        }
        return sb.getSubTreeArticleIterator();
    }

    public void parseToc(CachedUrl tocCU, ArrayList<ArticleFiles> results){

        /* Examples of where articles are located on TOC pages:
         *  https://raccefyn.co/index.php/raccefyn/issue/view/197 - <div class = article-summary>
         *  https://blakequarterly.org/index.php/blake/issue/view/84 - <article class = article>
         *  https://journals.whitingbirch.net/index.php/SWSSR/issue/view/196 - li inside of <ul class = cmp_article_list articles>
         */
        try{
            Document doc = Jsoup.parse(tocCU.getUnfilteredInputStream(), AuUtil.getCharsetOrDefault(tocCU.getProperties()), tocCU.getUrl());
            Elements articles = doc.select("div.article-summary,article.article,ul.articles>li");
            for (Element article : articles) {
                ArticleFiles af = new ArticleFiles();
                Elements abstracts = article.select("div.article-summary-title>a,h4.article__title>a,h3.title>a");
                af.setRole(ArticleFiles.ROLE_ABSTRACT,abstracts.attr("href")); //use absURL
                Elements PDFs = article.select("div.article-summary-galleys>a,ul.article__btn-group>li>a.pdf,ul.galleys_links>li>a.pdf");
                af.setRole(ArticleFiles.ROLE_FULL_TEXT_PDF,PDFs.attr("href"));
                Elements HTMLs = article.select("ul.article__btn-group>li>a.file");
                af.setRole(ArticleFiles.ROLE_FULL_TEXT_HTML,HTMLs.attr("href"));
                results.add(af);
            }
        }catch(IOException ioe){
            log.debug("Error parsing CU", ioe);
        }
        
    }
    
}
