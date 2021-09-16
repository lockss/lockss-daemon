package org.lockss.plugin.atypon.aaas;

import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.StringUtil;

import java.io.IOException;
import java.io.InputStream;

public class TestAaasHtmlHashFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private AaasHtmlHashFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new AaasHtmlHashFilterFactory();
    mau = new MockArchivalUnit();
  }

  public String filterString(String rawHtml) throws IOException {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(rawHtml),
        Constants.DEFAULT_ENCODING);
    return StringUtil.fromInputStream(actIn);
  }

  String toc_h4w_id =
    "<div class=\"toc\">" +
      "<div data-pb-class-name=\"to-section\" class=\"sections-navigation\"></div>" + "<div class=\"toc__body\">" +
        "<div class=\"toc__body\">\n" +
          "<section class=\"toc__section \">\n" +
            "<h4 id=\"h_d819657e43\" class=\"mb-2x sans-serif border-deep-gray sidebar-article-title--decorated to-section font-weight-bolder\">Research Articles</h4>\n" +
            "<div class=\"card border-bottom pb-3 mb-3\">\n" + "<div class=\"card-content\"></div>" + "</div>" +
          "</section>" +
    "</div>" + "</div>" + "</div>";

  String toc_h4w_id_filtered =
    " Research Articles ";

  String toc_div_card_footer =
    "<div class=\"card-footer \">" +
      "<a href=\"#\" role=\"button\" data-toggle=\"collapse\" aria-expanded=\"false\" aria-controls=\"abstract-d819658e1\" data-target=\"#abstract-d819658e1\" class=\"accordion__toggle collapsed font-weight-bold text-uppercase text-darker-gray\">" +
        "<span>Abstract</span>" + "<i class=\"icon-arrow-down align-middle\"></i>" +
      "</a>" +
    "<div id=\"abstract-d819658e1\" class=\"collapse\">\n" + "</div>" + "</div>";

  String toc_div_card_footer2 =
    "<div class=\"card-footer \">" +
        "<a href=\"#\" role=\"button\" data-toggle=\"collapse\" aria-expanded=\"false\" aria-controls=\"abstract-d2895651e1\" data-target=\"#abstract-d2895651e1\" class=\"accordion__toggle collapsed font-weight-bold text-uppercase text-darker-gray\"><span>Abstract</span><i class=\"icon-arrow-down align-middle\"></i></a><div id=\"abstract-d2895651e1\" class=\"collapse\">\n";

  String article_show_recommend1 =
      "<div class=\"show-recommended related-content pop-notification pop-after-passed\" data-visible-after=\"40%\" aria-hidden=\"true\"><h4 class=\"main-title-2--decorated h4 mb-3 d-flex align-items-center\"><span class=\"flex-fill\">Recommended</span><a href=\"#\" aria-controls=\".pop-notification.related-content\" class=\"text-gray pop-notification__close\"><i aria-hidden=\"true\" class=\"icon-close\"></i><span class=\"sr-only\">Close</span></a></h4><div data-exp-type=\"\" data-query-id=\"10.1126/scisignal.abm3135\" id=\"id1234\"><article class=\"card card-do card-do--news-feature card-do--news-article-aside border-bottom border-light-gray pb-3 mb-3 border-bottom \"><div class=\"card-content\"><div class=\"card-meta mb-1 text-uppercase\"><span class=\"card-meta__category\"><span class=\"primary font-weight-bold\">Research Articles</span></span><time class=\"text-uppercase border-left\">February 2011</time></div><div class=\"card-header mb-2\"><div class=\" \"><a href=\"/doi/full/10.1126/scitranslmed.3001830\" title=\"Human CD3 Transgenic Mice: Preclinical Testing of Antibodies Promoting Immune Tolerance\" class=\"text-reset animation-underline\">Human CD3 Transgenic Mice: Preclinical Testing of Antibodies Promoting Immune Tolerance</a></div></div></div></article><article class=\"card card-do card-do--news-feature card-do--news-article-aside border-bottom border-light-gray pb-3 mb-3 border-bottom \"><div class=\"card-content\"><div class=\"card-meta mb-1 text-uppercase\"><span class=\"card-meta__category\"><span class=\"primary font-weight-bold\">Perspectives</span></span><time class=\"text-uppercase border-left\">February 2016</time></div><div class=\"card-header mb-2\"><div class=\" \"><a href=\"/doi/full/10.1126/science.aaf2167\" title=\"How does the immune system tolerate food?\" class=\"text-reset animation-underline\">How does the immune system tolerate food?</a></div></div></div></article><article class=\"card card-do card-do--news-feature card-do--news-article-aside border-bottom border-light-gray pb-3 mb-3 border-bottom \"><div class=\"card-content\"><div class=\"card-meta mb-1 text-uppercase\"><span class=\"card-meta__category\"><span class=\"primary font-weight-bold\">Research Articles</span></span><time class=\"text-uppercase border-left\">May 2018</time></div><div class=\"card-header mb-2\"><div class=\" \"><a href=\"/doi/full/10.1126/science.aan5931\" title=\"Gut microbiome&amp;#x2013;mediated bile acid metabolism regulates liver cancer via NKT cells\" class=\"text-reset animation-underline\">Gut microbiome&#x2013;mediated bile acid metabolism regulates liver cancer via NKT cells</a></div></div></div></article><article class=\"card card-do card-do--news-feature card-do--news-article-aside border-bottom border-light-gray pb-3 mb-3 border-bottom \"><div class=\"card-content\"><div class=\"card-meta mb-1 text-uppercase\"><span class=\"card-meta__category\"><span class=\"primary font-weight-bold\">Focus</span></span><time class=\"text-uppercase border-left\">December 2017</time></div><div class=\"card-header mb-2\"><div class=\" \"><a href=\"/doi/full/10.1126/sciimmunol.aar2465\" title=\"GAPs in early life facilitate immune tolerance\" class=\"text-reset animation-underline\">GAPs in early life facilitate immune tolerance</a></div></div></div></article><div class=\"pb-dropzone\" data-pb-dropzone=\"Show More link\" title=\"Show More link\"></div></div></div>\n";

  String article_show_recommend2 =
      "<div class=\"show-recommended related-content pop-notification pop-after-passed\" data-visible-after=\"40%\" aria-hidden=\"true\"><h4 class=\"main-title-2--decorated h4 mb-3 d-flex align-items-center\"><span class=\"flex-fill\">Recommended</span><a href=\"#\" aria-controls=\".pop-notification.related-content\" class=\"text-gray pop-notification__close\"><i aria-hidden=\"true\" class=\"icon-close\"></i><span class=\"sr-only\">Close</span></a></h4><div data-exp-type=\"\" data-query-id=\"10.1126/scisignal.abm3135\" id=\"id3448\"><article class=\"card card-do card-do--news-feature card-do--news-article-aside border-bottom border-light-gray pb-3 mb-3 border-bottom \"><div class=\"card-content\"><div class=\"card-meta mb-1 text-uppercase\"><span class=\"card-meta__category\"><span class=\"primary font-weight-bold\">Research Articles</span></span><time class=\"text-uppercase border-left\">February 2011</time></div><div class=\"card-header mb-2\"><div class=\" \"><a href=\"/doi/full/10.1126/scitranslmed.3001830\" title=\"Human CD3 Transgenic Mice: Preclinical Testing of Antibodies Promoting Immune Tolerance\" class=\"text-reset animation-underline\">Human CD3 Transgenic Mice: Preclinical Testing of Antibodies Promoting Immune Tolerance</a></div></div></div></article><article class=\"card card-do card-do--news-feature card-do--news-article-aside border-bottom border-light-gray pb-3 mb-3 border-bottom \"><div class=\"card-content\"><div class=\"card-meta mb-1 text-uppercase\"><span class=\"card-meta__category\"><span class=\"primary font-weight-bold\">Perspectives</span></span><time class=\"text-uppercase border-left\">February 2016</time></div><div class=\"card-header mb-2\"><div class=\" \"><a href=\"/doi/full/10.1126/science.aaf2167\" title=\"How does the immune system tolerate food?\" class=\"text-reset animation-underline\">How does the immune system tolerate food?</a></div></div></div></article><article class=\"card card-do card-do--news-feature card-do--news-article-aside border-bottom border-light-gray pb-3 mb-3 border-bottom \"><div class=\"card-content\"><div class=\"card-meta mb-1 text-uppercase\"><span class=\"card-meta__category\"><span class=\"primary font-weight-bold\">Research Articles</span></span><time class=\"text-uppercase border-left\">May 2018</time></div><div class=\"card-header mb-2\"><div class=\" \"><a href=\"/doi/full/10.1126/science.aan5931\" title=\"Gut microbiome&amp;#x2013;mediated bile acid metabolism regulates liver cancer via NKT cells\" class=\"text-reset animation-underline\">Gut microbiome&#x2013;mediated bile acid metabolism regulates liver cancer via NKT cells</a></div></div></div></article><article class=\"card card-do card-do--news-feature card-do--news-article-aside border-bottom border-light-gray pb-3 mb-3 border-bottom \"><div class=\"card-content\"><div class=\"card-meta mb-1 text-uppercase\"><span class=\"card-meta__category\"><span class=\"primary font-weight-bold\">Focus</span></span><time class=\"text-uppercase border-left\">December 2017</time></div><div class=\"card-header mb-2\"><div class=\" \"><a href=\"/doi/full/10.1126/sciimmunol.aar2465\" title=\"GAPs in early life facilitate immune tolerance\" class=\"text-reset animation-underline\">GAPs in early life facilitate immune tolerance</a></div></div></div></article><div class=\"pb-dropzone\" data-pb-dropzone=\"Show More link\" title=\"Show More link\"></div></div></div>\n";

  String article_cnews_article_aside1 =
    "<article class=\"card-do card-do--news-feature border-bottom border-light-gray card-do--news-article-aside pb-3 mb-3\">\n" +
      "<div class=\"card-content\">\n" +
        "<div class=\"card-meta text-uppercase\">\n" +
          "<span class=\"card-meta__category\">" +
            "<a href=\"/news/scienceinsider\" class=\"text-decoration-none\"><span>ScienceInsider</span></a>" +
          "</span>" +
          "<time class=\"border-left\">15 Sep 2021</time>\n" +
        "</div>\n" +
        "<div class=\"card-header mb-2\">\n" +
          "<a href=\"/content/article/sars-viruses-may-jump-animals-people-hundreds-thousands-times-year\" title=\"SARS-like viruses may jump from animals to people hundreds of thousands of times a year\" class=\"text-reset animation-underline\">\n" +
            "SARS-like viruses may jump from animals to people hundreds of thousands of times a year\n" +
          "</a>\n" +
        "</div>\n" +
      "</div>\n" +
    "</article>\n" +
    "<article class=\"card-do card-do--news-feature border-bottom border-light-gray card-do--news-article-aside pb-3 mb-3\">\n" +
      "<div class=\"card-content\">\n" +
        "<div class=\"card-meta text-uppercase\">\n" +
          "<span class=\"card-meta__category\">" +
            "<a href=\"/news/scienceinsider\" class=\"text-decoration-none\"><span>ScienceInsider</span></a>" +
          "</span>" +
          "<time class=\"border-left\">15 Sep 2021</time>\n" +
        "</div>\n" +
        "<div class=\"card-header mb-2\">\n" +
          "<a href=\"/content/article/study-40-000-people-will-probe-mysteries-long-covid\" title=\"Study of up to 40,000 people will probe mysteries of Long Covid\" class=\"text-reset animation-underline\">\n" +
            "Study of up to 40,000 people will probe mysteries of Long Covid\n" +
          "</a>\n" +
        "</div>\n" +
      "</div>\n" +
    "</article>\n" +
    "<article class=\"card-do card-do--news-feature border-bottom border-light-gray card-do--news-article-aside pb-3 mb-3\">\n" +
      "<div class=\"card-content\">\n" +
        "<div class=\"card-meta text-uppercase\">\n" +
        "<span class=\"card-meta__category\">" +
          "<a href=\"/news/careers-editorial\" class=\"text-decoration-none\"><span>Careers Editorial</span></a>" +
        "</span>" +
        "<time class=\"border-left\">15 Sep 2021</time>\n" +
      "</div>\n" +
      "<div class=\"card-header mb-2\">\n" +
      "</div>\n" +
    "</article>";

  String article_cnews_article_aside2 =
    "<article class=\"card-do card-do--news-feature border-bottom border-light-gray card-do--news-article-aside pb-3 mb-3\">\n" +
      "<div class=\"card-content\">\n" +
        "<div class=\"card-meta text-uppercase\">\n" +
          "<span class=\"card-meta__category\">" +
            "<a href=\"/news/scienceinsider\" class=\"text-decoration-none\"><span>ScienceInsider</span></a>" +
          "</span>" +
          "<time class=\"border-left\">14 Sep 2021</time>\n" +
        "</div>\n" +
        "<div class=\"card-header mb-2\">\n" +
          "<a href=\"/content/article/can-zero-covid-countries-continue-keep-virus-bay-once-they-reopen\" title=\"Can \u0018zero COVID\u0019 countries continue to keep the virus at bay once they reopen?\" class=\"text-reset animation-underline\">\n" +
            "Can \u0018zero COVID\u0019 countries continue to keep the virus at bay once they reopen?\n" +
          "</a>\n" +
        "</div>\n" +
      "</div>\n" +
    "</article>\n" +
    "<article class=\"card-do card-do--news-feature border-bottom border-light-gray card-do--news-article-aside pb-3 mb-3\">\n" +
      "<div class=\"card-content\">\n" +
        "<div class=\"card-meta text-uppercase\">\n" +
          "<span class=\"card-meta__category\">" +
            "<a href=\"/news/scienceinsider\" class=\"text-decoration-none\"><span>ScienceInsider</span></a>" +
          "</span>" +
          "<time class=\"border-left\">14 Sep 2021</time>\n" +
        "</div>\n" +
        "<div class=\"card-header mb-2\">\n" +
          "<a href=\"/content/article/climate-policies-loom-large-german-election\" title=\"Climate policies loom large in German election\" class=\"text-reset animation-underline\">\n" +
            "Climate policies loom large in German election\n" +
          "</a>\n" +
        "</div>\n" +
      "</div>\n" +
    "</article>";
  
  public void testH4Filtering() throws Exception {
    assertEquals(toc_h4w_id_filtered, filterString(toc_h4w_id));
  }

  public void testCardFiltering() throws Exception {
    String filtered1 = filterString(toc_div_card_footer);
    log.info(filtered1);
    assertEquals(filterString(toc_div_card_footer2), filtered1);
  }

  public void testArticleRecFiltering() throws Exception {
    String filtered1 = filterString(article_show_recommend1);
    log.info(filtered1);
    assertEquals(filtered1, " ");
    assertEquals(filterString(article_show_recommend2), filtered1);
  }

  public void testArticleAsideFiltering() throws Exception {
    String filtered1 = filterString(article_cnews_article_aside1);
    log.info(filtered1);
    assertEquals(filtered1, " ");
    assertEquals(filterString(article_cnews_article_aside2), filtered1);
  }

}
