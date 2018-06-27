/*
 * $Id$
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

package org.lockss.daemon;

import org.apache.commons.collections4.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.extractor.*;
import org.lockss.rewriter.*;
import org.lockss.test.*;

/**
 * This is the test class for org.lockss.daemon.MimeTypeMap
 */
public class TestMimeTypeMap extends LockssTestCase {

  MimeTypeMap map;
  MimeTypeInfo.Mutable mti;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    map = new MimeTypeMap();
    mti = new MimeTypeInfo.Impl();
  }

  public void testGetMimeTypeInfo() {
    MimeTypeInfo mt1 = new MimeTypeInfo.Impl();
    MimeTypeInfo mt2 = new MimeTypeInfo.Impl();
    map.putMimeTypeInfo("text/html", mt1);
    map.putMimeTypeInfo("text/css", mt2);
    assertSame(mt1, map.getMimeTypeInfo("text/html"));
    assertSame(mt1, map.getMimeTypeInfo("text/HTML; charset=illegible"));
    assertSame(mt2, map.getMimeTypeInfo("text/CSS; charset=illegible"));
    assertNull(map.getMimeTypeInfo("text/plaim; charset=illegible"));
  }

  public void testInherit() {
    MimeTypeMap child = new MimeTypeMap(map);
    MimeTypeInfo mt1 = new MimeTypeInfo.Impl();
    MimeTypeInfo mt2 = new MimeTypeInfo.Impl();
    MimeTypeInfo mt3 = new MimeTypeInfo.Impl();
    map.putMimeTypeInfo("text/html", mt2);
    map.putMimeTypeInfo("text/css", mt3);
    assertSame(mt2, child.getMimeTypeInfo("text/html"));
    assertSame(mt3, child.getMimeTypeInfo("text/css"));
    child.putMimeTypeInfo("text/html", mt1);
    assertSame(mt1, child.getMimeTypeInfo("text/html"));
    assertSame(mt2, map.getMimeTypeInfo("text/html"));
  }

  public void testDefault() {
    // -------- CSS --------
    MimeTypeInfo mt1 = MimeTypeMap.DEFAULT.getMimeTypeInfo("text/css");
    assertNull(mt1.getHashFilterFactory());
    assertNull(mt1.getCrawlFilterFactory());
    assertTrue(mt1.getLinkExtractorFactory() instanceof RegexpCssLinkExtractor.Factory);
    assertTrue(mt1.getLinkRewriterFactory() instanceof RegexpCssLinkRewriterFactory);

    ConfigurationUtil.setFromArgs(MimeTypeMap.PARAM_DEFAULT_CSS_EXTRACTOR_FACTORY,
                                  "No.Such.Class");
    assertClass(RegexpCssLinkExtractor.Factory.class, mt1.getLinkExtractorFactory());

    ConfigurationUtil.setFromArgs(MimeTypeMap.PARAM_DEFAULT_CSS_REWRITER_FACTORY,
                                  "No.Such.Class");
    assertClass(RegexpCssLinkRewriterFactory.class, mt1.getLinkRewriterFactory());

    ConfigurationUtil.setFromArgs(MimeTypeMap.PARAM_DEFAULT_CSS_REWRITER_FACTORY,
                                  "org.lockss.rewriter.StringFilterCssLinkRewriterFactory");
    assertClass(StringFilterCssLinkRewriterFactory.class, mt1.getLinkRewriterFactory());

    // -------- HTML --------
    MimeTypeInfo mt2 = MimeTypeMap.DEFAULT.getMimeTypeInfo("text/html");
    assertNull(mt2.getHashFilterFactory());
    assertNull(mt2.getCrawlFilterFactory());
    assertTrue(mt2.getLinkExtractorFactory() instanceof GoslingHtmlLinkExtractor.Factory);
    assertTrue(mt2.getLinkRewriterFactory() instanceof NodeFilterHtmlLinkRewriterFactory);

    MimeTypeInfo mt3 = MimeTypeMap.DEFAULT.getMimeTypeInfo("application/xhtml+xml");
    assertNull(mt3.getHashFilterFactory());
    assertNull(mt3.getCrawlFilterFactory());
    assertTrue(mt3.getLinkExtractorFactory() instanceof GoslingHtmlLinkExtractor.Factory);
    assertTrue(mt3.getLinkRewriterFactory() instanceof NodeFilterHtmlLinkRewriterFactory);

    // -------- XML --------
    MimeTypeInfo mt4 = MimeTypeMap.DEFAULT.getMimeTypeInfo("text/xml");
    assertNull(mt4.getHashFilterFactory());
    assertNull(mt4.getCrawlFilterFactory());
    assertTrue(mt4.getLinkExtractorFactory() instanceof XmlLinkExtractorFactory);
    assertNull(mt4.getLinkRewriterFactory());

    MimeTypeInfo mt5 = MimeTypeMap.DEFAULT.getMimeTypeInfo("application/xml");
    assertNull(mt5.getHashFilterFactory());
    assertNull(mt5.getCrawlFilterFactory());
    assertTrue(mt5.getLinkExtractorFactory() instanceof XmlLinkExtractorFactory);
    assertNull(mt5.getLinkRewriterFactory());

    // -------- ALL --------
    MimeTypeInfo mt6 = MimeTypeMap.DEFAULT.getMimeTypeInfo("*/*");
    assertNull(mt6.getHashFilterFactory());
    assertNull(mt6.getCrawlFilterFactory());
    assertNull(mt6.getLinkExtractorFactory());
    assertNull(mt6.getLinkRewriterFactory());
    assertTrue(mt6.getContentValidatorFactory() instanceof MimeTypeContentValidatorFactory);

    MimeTypeInfo mta = MimeTypeMap.DEFAULT.getMimeTypeInfo("*/*");
    ConfigurationUtil.setFromArgs(MimeTypeMap.PARAM_DEFAULT_ALL_MIME_TYPE_VALIDATION_FACTORY,
                                  MyValidatorFact.class.getName());

    assertClass(MyValidatorFact.class, mta.getContentValidatorFactory());
  }

  static class MyValidatorFact implements ContentValidatorFactory {
    public ContentValidator createContentValidator(ArchivalUnit au,
						   String contentType) {
      return null;
    }
  }

  public void testModifyMimeTypeInfo() {
    MimeTypeMap DEFAULT = MimeTypeMap.DEFAULT;
    MimeTypeMap child = new MimeTypeMap(DEFAULT);
    MimeTypeInfo.Mutable mt1 = child.modifyMimeTypeInfo("text/html");
    assertTrue(DEFAULT.getMimeTypeInfo("TEXT/html").getLinkExtractorFactory()
	       instanceof GoslingHtmlLinkExtractor.Factory);
    assertTrue(mt1.getLinkExtractorFactory()
	       instanceof GoslingHtmlLinkExtractor.Factory);
    LinkExtractorFactory uf = new MockLinkExtractorFactory();
    mt1.setLinkExtractorFactory(uf);
    assertTrue(DEFAULT.getMimeTypeInfo("TEXT/html").getLinkExtractorFactory()
	       instanceof GoslingHtmlLinkExtractor.Factory);
    assertEquals(uf, mt1.getLinkExtractorFactory());
  }

  public void testWildSubType() {
    String wild = "image/*";
    assertEquals(wild, MimeTypeMap.wildSubType("image/gif"));
    assertSame(wild, MimeTypeMap.wildSubType("image/*"));
    assertEquals("image", MimeTypeMap.wildSubType("image"));
    assertEquals("image/bad/mime", MimeTypeMap.wildSubType("image/bad/mime"));
  }

  public void testHasAnyThat() {
    map = new MimeTypeMap(MimeTypeMap.DEFAULT);
    assertTrue(map.hasAnyThat(new Predicate<MimeTypeInfo>() {
	public boolean evaluate(MimeTypeInfo mti) {
	  return mti.getLinkExtractorFactory() != null;
	}}));
    assertFalse(map.hasAnyThat(new Predicate<MimeTypeInfo>() {
	public boolean evaluate(MimeTypeInfo mti) {
	  return mti.getCrawlFilterFactory() != null;
	}}));
    assertTrue(map.hasAnyThat(new Predicate<MimeTypeInfo>() {
	public boolean evaluate(MimeTypeInfo mti) {
	  return mti.getContentValidatorFactory() != null;
	}}));

    MimeTypeInfo.Mutable mt1 = new MimeTypeInfo.Impl();
    MimeTypeInfo.Mutable mt2 = new MimeTypeInfo.Impl();

    ContentValidatorFactory cv = new MockContentValidatorFactory();
    mt1.setContentValidatorFactory(cv);
    FilterFactory cff = new MockFilterFactory();
    mt2.setCrawlFilterFactory(cff);
    map.putMimeTypeInfo("text/html", mt1);
    map.putMimeTypeInfo("image/*", mt2);

    assertTrue(map.hasAnyThat(new Predicate<MimeTypeInfo>() {
	public boolean evaluate(MimeTypeInfo mti) {
	  return mti.getCrawlFilterFactory() != null;
	}}));
    assertTrue(map.hasAnyThat(new Predicate<MimeTypeInfo>() {
	public boolean evaluate(MimeTypeInfo mti) {
	  return mti.getContentValidatorFactory() != null;
	}}));
  }

  public void testHtmlParserLinkExtractor()
  {
	  MimeTypeInfo mt1 = MimeTypeMap.DEFAULT.getMimeTypeInfo("text/html");
	  assertNull(mt1.getHashFilterFactory());
	  assertNull(mt1.getCrawlFilterFactory());
	  assertTrue(mt1.getLinkExtractorFactory()
				   instanceof GoslingHtmlLinkExtractor.Factory);
  }
}
