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
    " ";

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

  String toc_podcast =
    "<div class=\"card--podcast-item--featured card--podcast--column\">\n" +
      "<div class=\"multi-search\"><div>\n" +
      "<div class=\"card card--podcast flex-column\">\n" +
      "\t<a href=\"/content/podcast/potty-training-cows-and-sardines-swimming-ecological-trap\" title=\"Potty training cows, and sardines swimming into an ecological trap\" data-rel=\"animation-underline\" class=\"card-img overflow-hidden mb-3\">\n" +
      "\t\t<img src=\"/do/10.1126/science.acx9120/card-type2/_20210917_pod-sardines2.jpg\" class=\"w-100 animation-image-scale-down\" alt=\"sardines in a swirling bait ball\">\n" +
      "\t</a>\n" +
      "\t<div class=\"card-content\">\n" +
      "\t\t<div class=\"card-meta text-uppercase d-inline-block text-darker-gray\">\n" +
      "\t\t\t<time class=\"text-uppercase pl-0 pr-2 text-darker-gray\">16 Sep 2021</time><div class=\"card-contribs authors d-inline-block border-left pl-2\">By <ul title=\"list of authors\" class=\"list-inline\"><li class=\"list-inline-item\">Sarah Crespi, David Grimm</li></ul>\n" +
      "\t\t\t</div>\n" +
      "\t\t</div>\n" +
      "\t\t<div class=\"card-header\">\n" +
      "\t\t\t<div class=\"card__title sans-serif\">\n" +
      "\t\t\t\t<a href=\"/content/podcast/potty-training-cows-and-sardines-swimming-ecological-trap\" title=\"Potty training cows, and sardines swimming into an ecological trap\" class=\"text-reset animation-underline\">Potty training cows, and sardines swimming into an ecological trap</a>\n" +
      "\t\t\t</div>\n" +
      "\t\t</div>\n" +
      "\t\t<div class=\" mt-3 \">\n" +
      "\t\t\t<div class=\"audio-player\" data-audio=\"https://traffic.omny.fm/d/clips/aaea4e69-af51-495e-afc9-a9760146922b/95ab13e7-f709-4a58-acad-aaea01775538/6d467157-398d-4299-a102-ada500ee64d1/audio.mp3\">\n" +
      "\t\t\t\t<div class=\"audio-player__row d-flex align-items-center\">\n" +
      "\t\t\t\t\t<button role=\"button\" class=\"btn audio-player__trigger\">\n" +
      "\t\t\t\t\t\t<i class=\"icon-play audio-player__trigger-play\"></i>\n" +
      "\t\t\t\t\t\t<i class=\"icon-pause audio-player__trigger-pause\"></i>\n" +
      "\t\t\t\t\t</button>\n" +
      "\t\t\t\t\t<div class=\"audio-player__progress\">\n" +
      "\t\t\t\t\t\t<div class=\"audio-player__progress-buffer\"></div>\n" +
      "\t\t\t\t\t\t<div class=\"audio-player__progress-play\"></div>\n" +
      "\t\t\t\t\t\t<div class=\"audio-player__progress-tooltip text-uppercase text-xss\"></div>\n" +
      "\t\t\t\t\t\t<label for=\"audio-progress-39\" class=\"sr-only\"></label>\n" +
      "\t\t\t\t\t\t<input id=\"audio-progress-39\" type=\"range\" name=\"track\" max=\"100\" value=\"50\" class=\"audio-player__progress-current\"> \n" +
      "\t\t\t\t\t</div>\n" +
      "\t\t\t\t\t<div class=\"audio-player__volume\">\n" +
      "\t\t\t\t\t\t<button class=\"btn audio-player__volume-btn\">\n" +
      "\t\t\t\t\t\t\t<i class=\"icon-soundon audio-player__volume-btn-on\"></i>\n" +
      "\t\t\t\t\t\t\t<i class=\"icon-soundoff audio-player__volume-btn-off\"></i>\n" +
      "\t\t\t\t\t\t</button>\n" +
      "\t\t\t\t\t\t<div class=\"audio-player__volume-control\">\n" +
      "\t\t\t\t\t\t\t<label for=\"volume-control-8\" class=\"sr-only\"></label>\n" +
      "\t\t\t\t\t\t\t<input id=\"volume-control-8\" type=\"range\" name=\"track\" min=\"0.0\" max=\"1.0\" value=\"0.5\" step=\"0.1\" class=\"audio-player__volume-control-slider\">\n" +
      "\t\t\t\t\t\t</div>\n" +
      "\t\t\t\t\t</div>\n" +
      "\t\t\t\t\t<div class=\"audio-player__time d-flex text-xxs text-dark-gray\">\n" +
      "\t\t\t\t\t\t<div class=\"audio-player__time-current border-right pr-2 mr-2\">00:00</div>\n" +
      "\t\t\t\t\t\t<div class=\"audio-player__time-total\">16:58</div>\n" +
      "\t\t\t\t\t</div>\n" +
      "\t\t\t\t</div>\n" +
      "\t\t\t\t<div class=\"audio-player__row d-flex align-items-center mt-2 pt-2 justify-content-between\">\n" +
      "\t\t\t\t\t<a href=\"#\" class=\"text-xs audio-player__transcript\"></a>\n" +
      "\t\t\t\t\t<div class=\"d-flex align-items-center\">\n" +
      "\t\t\t\t\t\t<div class=\"audio-player__dropdown dropdown\">\n" +
      "\t\t\t\t\t\t\t<a href=\"#\" data-toggle=\"dropdown\" aria-haspopup=\"true\" aria-expanded=\"false\" class=\"btn text-xxs btn-outline-secondary py-1 px-2 text-uppercase dropdown-toggle\">\n" +
      "\t\t\t\t\t\t\t\t<i class=\"icon-plus text-xs\"></i>\n" +
      "\t\t\t\t\t\t\t\tSubscribe\n" +
      "\t\t\t\t\t\t\t</a>\n" +
      "\t\t\t\t\t\t\t<div class=\"dropdown-menu text-xs audio-player__dropdown-menu\">\n" +
      "\t\t\t\t\t\t\t\t<div class=\"dropdown-content\">\n" +
      "\t\t\t\t\t\t\t\t\t<a href=\"https://podcasts.apple.com/us/podcast/science-magazine-podcast/id120329020\" class=\"dropdown-item pl-2\">\n" +
      "\t\t\t\t\t\t\t\t\t\t<img src=\"/specs/products/aaas/releasedAssets/images/logo-apple.svg\" class=\"ml-1 mr-2\" height=\"16\">\n" +
      "\t\t\t\t\t\t\t\t\t\t<span>Apple</span>\n" +
      "\t\t\t\t\t\t\t\t\t</a>\n" +
      "\t\t\t\t\t\t\t\t\t<a href=\"https://www.stitcher.com/show/science-magazine-podcast\" class=\"dropdown-item pl-2\">\n" +
      "\t\t\t\t\t\t\t\t\t\t<img src=\"/specs/products/aaas/releasedAssets/images/logo-stitcher.svg\" class=\"ml-1 mr-2\" height=\"16\">\n" +
      "\t\t\t\t\t\t\t\t\t\t<span>Stitcher</span>\n" +
      "\t\t\t\t\t\t\t\t\t</a>\n" +
      "\t\t\t\t\t\t\t\t\t<a href=\"https://open.spotify.com/show/6S1RCtUXQ7UvKUTB77x7xH\" class=\"dropdown-item pl-2\">\n" +
      "\t\t\t\t\t\t\t\t\t\t<img src=\"/specs/products/aaas/releasedAssets/images/logo-spotify.svg\" class=\"ml-1 mr-2\" height=\"16\">\n" +
      "\t\t\t\t\t\t\t\t\t\t<span>Spotify</span>\n" +
      "\t\t\t\t\t\t\t\t\t</a>\n" +
      "\t\t\t\t\t\t\t\t\t<a href=\"https://podcasts.google.com/feed/aHR0cHM6Ly93d3cuc2NpZW5jZW1hZy5vcmcvcnNzL3BvZGNhc3QueG1s\" class=\"dropdown-item pl-2\">\n" +
      "\t\t\t\t\t\t\t\t\t\t<img src=\"/specs/products/aaas/releasedAssets/images/logo-google.svg\" class=\"ml-1 mr-2\" height=\"16\">\n" +
      "\t\t\t\t\t\t\t\t\t\t<span>Google</span>\n" +
      "\t\t\t\t\t\t\t\t\t</a>\n" +
      "\t\t\t\t\t\t\t\t</div>\n" +
      "\t\t\t\t\t\t\t</div>\n" +
      "\t\t\t\t\t\t</div>\n" +
      "\t\t\t\t\t</div>\n" +
      "\t\t\t\t</div>\n" +
      "\t\t\t</div>\n" +
      "\t\t</div>\n" +
      "\t</div>\n" +
      "</div></div></div>\n" +
    "</div>";
  
  public void testH4Filtering() throws Exception {
    assertEquals(toc_h4w_id_filtered, filterString(toc_h4w_id));
  }

  public void testCardFiltering() throws Exception {
    String filtered1 = filterString(toc_div_card_footer);
    log.info(filtered1);
    assertEquals(filterString(toc_div_card_footer2), filtered1);
  }

  public void testTocPodcastFiltering() throws Exception {
    String filtered1 = filterString(toc_podcast);
    log.info(filtered1);
    assertEquals(filtered1, "");
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
