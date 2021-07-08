package org.lockss.plugin.illiesia;

import org.lockss.daemon.ConfigParamDescr;
import org.lockss.extractor.LinkExtractor;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestIlliesiaHtmlLinkExtractor extends LockssTestCase {

  public static List<String> expectedLinks = Arrays.asList(
    "http://illiesia.speciesfile.org/html/illiesia.ico",
    "http://illiesia.speciesfile.org/html/illiesia.ico",
    "http://illiesia.speciesfile.org/html/illiesia.gif",
    "http://illiesia.speciesfile.org/html/logo_sp.gif",
    "http://illiesia.speciesfile.org/html/about.html",
    "http://illiesia.speciesfile.org/html/about.gif",
    "http://illiesia.speciesfile.org/html/instructions.html",
    "http://illiesia.speciesfile.org/html/instructions.gif",
    "http://illiesia.speciesfile.org/html/papers_m.gif" ,
    "http://illiesia.speciesfile.org/index.html",
    "http://illiesia.speciesfile.org/html/home.gif",
    "http://illiesia.speciesfile.org/html/svetlo_zg.gif",
    "http://illiesia.speciesfile.org/html/2018.html" ,
    "http://illiesia.speciesfile.org/html/2017.html" ,
    "http://illiesia.speciesfile.org/html/2016.html" ,
    "http://illiesia.speciesfile.org/html/2015.html" ,
    "http://illiesia.speciesfile.org/html/2014.html" ,
    "http://illiesia.speciesfile.org/html/2013.html" ,
    "http://illiesia.speciesfile.org/html/2012.html" ,
    "http://illiesia.speciesfile.org/html/2011.html" ,
    "http://illiesia.speciesfile.org/html/2010.html" ,
    "http://illiesia.speciesfile.org/html/2009.html" ,
    "http://illiesia.speciesfile.org/html/2008.html" ,
    "http://illiesia.speciesfile.org/html/2007.html" ,
    "http://illiesia.speciesfile.org/html/2006.html" ,
    "http://illiesia.speciesfile.org/html/2005.html" ,
    "http://illiesia.speciesfile.org/html/papers.html" ,
    "http://illiesia.speciesfile.org/html/monographs_1.html" ,
    "http://illiesia.speciesfile.org/html/editorial_2013.html" ,
    "http://illiesia.speciesfile.org/papers/Illiesia01-05.pdf" ,
    "http://illiesia.speciesfile.org/papers/Illiesia01-04.pdf" ,
    "http://illiesia.speciesfile.org/papers/Illiesia01-03.pdf" ,
    "http://illiesia.speciesfile.org/papers/Illiesia01-02.pdf" ,
    "http://illiesia.speciesfile.org/papers/Illiesia01-01.pdf" ,
    "http://illiesia.speciesfile.org/html/temno_sp.gif" ,
    "http://illiesia.speciesfile.org/html/svetlo_sp.gif"
  );

  public void testExtract2015Urls() throws Exception {

    IlliesiaHtmlLinkExtractor ile = new IlliesiaHtmlLinkExtractor();
    List<String> out = doExtractUrls(ile,
        TestIlliesiaCharsetUtil.utf162005html
    );
    assertTrue(out.size() != 0);
    assertEquals(out, expectedLinks);
  }

  protected List<String> doExtractUrls(LinkExtractor le,
                                       String input)
      throws Exception {
    MockArchivalUnit mau = new MockArchivalUnit();
    mau.setConfiguration(ConfigurationUtil.fromArgs(
        ConfigParamDescr.BASE_URL.getKey(), "http://www.illiesia.speciesfile.org/"));
    final List<String> out = new ArrayList<String>();
    le.extractUrls(
        mau,
        new StringInputStream(input),
        TestIlliesiaCharsetUtil.declaredCharset,
        "http://illiesia.speciesfile.org/html/2005.html",
        new LinkExtractor.Callback() {
          @Override public void foundLink(String url) {
            out.add(url);
          }
        });
    return out;
  }
}
