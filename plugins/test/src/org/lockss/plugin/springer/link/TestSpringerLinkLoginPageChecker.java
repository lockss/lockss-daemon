/*
 * $Id:
 */

/*

Copyright (c) 2019 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.plugin.springer.link;

import java.io.*;

import org.lockss.daemon.PluginException;
import org.lockss.test.*;
import org.lockss.util.*;

public class TestSpringerLinkLoginPageChecker extends LockssTestCase {
  
  public static String articleNoLoginPage = "" +
      "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">" +
      "<html><head>" +
      "</body>" +
      "</html>";
  
  public static String articleLoginPageText = "" +
      "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">" +
        "<html><head>" +
        "<div class=\"buybox article__buybox\" data-webtrekkTrackId=\"196033507532344\" data-webtrekkContentId=\"SL-article\">" +
        "<div class=\"buybox__header\"> <span class=\"buybox__login\">Log in to check access</span>" +
        "</div>" +
        "<div class=\"buybox__body\">" +
            "<form class=\"buybox__form buybox__article\" action=\"https://checkout.hello.com/checkout/add?utm_source=helloworld&amp;utm_medium=referral&amp;utm_campaign=sl-buybox_articlePage_article\" method=\"POST\">" +
                "<input type=\"hidden\" name=\"type\" value=\"article\"/>" +
                "<input type=\"hidden\" name=\"doi\" value=\"10.1007/s41127-018-0018-9\"/>" +
                "<input type=\"hidden\" name=\"isxn\" value=\"2365-631X\"/>" +
                "<input type=\"hidden\" name=\"contenttitle\" value=\"ArticleName\"/>" +
                "<input type=\"hidden\" name=\"copyrightyear\" value=\"2018\"/>" +
                "<input type=\"hidden\" name=\"year\" value=\"2018\"/>" +
                "<input type=\"hidden\" name=\"authors\" value=\"Joe Sharma\"/>" +
                "<input type=\"hidden\" name=\"title\" value=\"New Technology\"/>" +
                "<input type=\"hidden\" name=\"returnurl\" value=\"http://link.hello.com/article/10.1007/s41127-018-0018-9\"/>" +
            "<button class=\"buybox__buy-button c-button c-button--blue\"" +
            "data-product-id=\"10.1007/s41127-018-0018-9_effi\"" +
            "data-tracking-category=\"ppv\"" +
            "data-eCommerceParam_11=\"article-page\"" +
            "data-gtm=\"buybox__buy-button\">" +
                "<span class=\"buybox__buy\" data-gtm=\"buybox__buy-button\">Buy article (PDF)</span>" +
            "</button>" +
        "</div>"+
        "</html>";

  public static String bookNoLoginPage = "" +
      "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">" +
      "<html><head>" +
      "</body>" +
      "</html>";

  public static String bookLoginPageText = "" +
      "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">" +
        "<html><head>" +
        "<div class=\"buybox book__buybox\" data-webtrekkTrackId=\"196033507532344\" data-webtrekkContentId=\"SL-book\">" +
            "<div class=\"buybox__header\">" +
                "<span class=\"buybox__login\">Log in to check access</span>" +
            "</div>" +
            "<div class=\"buybox__body\">" +
                "<div class=\"buybox__section\">" +
                    "<div class=\"buybox__buttons\">" +
                    "<a data-product-id=\"978-1-137-43543-9_Public Culture, Cultural Identity,\" " +
                        "data-tracking-category=\"ebook\""+
                        "data-eCommerceParam_11=\"book-page\""+
                        "href=\"https://checkout.springer.com/checkout/add?utm_source=springerlink&amp;utm_medium=referral&amp;utm_campaign=sl-buybox_bookPage_ebook&isbn=978-1-137-43543-9\""+
                        "class=\"buybox__buy-button c-button c-button--blue\""+
                        "data-gtm=\"buybox__buy-button-ebook\">"+
                        "<span class=\"buybox__buy\" data-gtm=\"buybox__buy-button\">Buy eBook</span>"+
                    "</a>"+
                    "<div class=\"buybox__price\" data-gtm=\"buybox__buy-button\"> USD&nbsp;89.00"+
                "</div>"+
            "</div>"+
        "</div>"+
        "</html>";

  public static final String newFormatJournalsBuyArticleOption =
      "<article class=\"c-box\" data-test-id=\"buy-article\">\n" +
          "  <h3 class=\"c-box__heading\">Buy single article</h3>\n" +
          "  <div class=\"c-box__body\">\n" +
          "   <div class=\"buybox__info\">\n" +
          "    <p>Instant access to the full article PDF.</p>\n" +
          "   </div>\n" +
          "   <div class=\"buybox__buy\">\n" +
          "    <p class=\"buybox__price\">US$ 49.95</p>\n" +
          "    <p class=\"buybox__price-info\">Tax calculation will be finalised during checkout.</p>\n" +
          "    <form action=\"https://order.springer.com/public/checkout?abtest=v2\" method=\"post\">\n" +
          "     <input type=\"hidden\" name=\"type\" value=\"article\">\n" +
          "     <input type=\"hidden\" name=\"doi\" value=\"10.1007/s40274-019-5989-0\">\n" +
          "     <input type=\"hidden\" name=\"isxn\" value=\"1179-2043\">\n" +
          "     <input type=\"hidden\" name=\"contenttitle\" value=\"Global health procurement\">\n" +
          "     <input type=\"hidden\" name=\"copyrightyear\" value=\"2019\">\n" +
          "     <input type=\"hidden\" name=\"year\" value=\"2019\">\n" +
          "     <input type=\"hidden\" name=\"authors\" value=\"\">\n" +
          "     <input type=\"hidden\" name=\"title\" value=\"PharmacoEconomics &amp; Outcomes News\">\n" +
          "     <input type=\"hidden\" name=\"mac\" value=\"5EAFA4A6BAED691CBF1B9D14BD6BF317\">\n" +
          "     <input type=\"submit\" class=\"c-box__button\" onclick=\"dataLayer.push({&quot;event&quot;:&quot;addToCart&quot;,&quot;ecommerce&quot;:{&quot;currencyCode&quot;:&quot;USD&quot;,&quot;add&quot;:{&quot;products&quot;:[{&quot;name&quot;:&quot;Global health procurement&quot;,&quot;id&quot;:&quot;1179-2043&quot;,&quot;price&quot;:49.95,&quot;brand&quot;:&quot;Springer International Publishing&quot;,&quot;category&quot;:&quot;Medicine &amp; Public Health&quot;,&quot;variant&quot;:&quot;ppv-article&quot;,&quot;quantity&quot;:1}]}}});\" value=\"Buy article PDF\">\n" +
          "    </form>\n" +
          "   </div>\n" +
          "  </div>\n" +
          "  <script>dataLayer.push({\"ecommerce\":{\"currency\":\"USD\",\"impressions\":[{\"name\":\"Global health procurement\",\"id\":\"1179-2043\",\"price\":49.95,\"brand\":\"Springer International Publishing\",\"category\":\"Medicine & Public Health\",\"variant\":\"ppv-article\",\"quantity\":1}]}});</script>\n" +
          " </article>";

  public void testNotLoginPage() throws IOException {
    SpringerLinkLoginPageChecker checker = new SpringerLinkLoginPageChecker();
    try {
      assertFalse(checker.isLoginPage(new CIProperties(),
          new StringReader("blah")));
    } catch (PluginException e) {
    }
  }
  
  public void testIsBookLoginPage() throws IOException {
    SpringerLinkLoginPageChecker checker = new SpringerLinkLoginPageChecker();
    CIProperties props = new CIProperties();
    props.put("Content-Type", "text/html; charset=windows-1252");
    
    StringReader reader = new StringReader(bookLoginPageText);
    
    try {
      assertTrue(checker.isLoginPage(props, reader));
    } catch (PluginException e) {
    }
  }
  
  public void testIsArticleLoginPage() throws IOException {
    SpringerLinkLoginPageChecker checker = new SpringerLinkLoginPageChecker();
    CIProperties props = new CIProperties();
    props.put("Content-Type", "text/html; charset=windows-1252");
    
    StringReader reader = new StringReader(articleLoginPageText);
    
    try {
      assertTrue(checker.isLoginPage(props, reader));
    } catch (PluginException e) {
    }
  }

  public void testNewFormatIsArticleLoginPage() throws IOException {
    SpringerLinkLoginPageChecker checker = new SpringerLinkLoginPageChecker();
    CIProperties props = new CIProperties();
    props.put("Content-Type", "text/html; charset=windows-1252");

    StringReader reader = new StringReader(newFormatJournalsBuyArticleOption);

    try {
      assertTrue(checker.isLoginPage(props, reader));
    } catch (PluginException e) {
    }
  }
  
  public void testIsNotBookLoginPage() throws IOException {
    SpringerLinkLoginPageChecker checker = new SpringerLinkLoginPageChecker();
    CIProperties props = new CIProperties();
    props.put("Content-Type", "text/html; charset=windows-1252");
    
    StringReader reader = new StringReader(bookNoLoginPage);
    
    try {
      assertFalse(checker.isLoginPage(props, reader));
    } catch (PluginException e) {
    }
  }
  
  public void testIsNotArticleLoginPage() throws IOException {
    SpringerLinkLoginPageChecker checker = new SpringerLinkLoginPageChecker();
    CIProperties props = new CIProperties();
    props.put("Content-Type", "text/html; charset=windows-1252");
    
    StringReader reader = new StringReader(articleNoLoginPage);
    
    try {
      assertFalse(checker.isLoginPage(props, reader));
    } catch (PluginException e) {
    }
  }
  
  private static class MyStringReader extends StringReader {
    
    public MyStringReader(String str) {
      super(str);
    }
    
    public int read(char[] cbuf, int off, int len) throws IOException {
      return super.read(cbuf, off, len);
    }
    
  }
}
