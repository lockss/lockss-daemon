/*
 * $Id$
 */

/*

 Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.extractor;

import org.lockss.extractor.MetadataField.Cardinality;
import org.lockss.test.*;
import org.lockss.util.*;

import static org.lockss.extractor.MetadataTarget.*;

public class TestMetadataTarget extends LockssTestCase {

  ArticleMetadata am;

  public void setUp() throws Exception {
    super.setUp();
    am = new ArticleMetadata();
  }

  public void testAccessors() {
    MetadataTarget mt = new MetadataTarget("Porpoise")
      .setPurpose("Porpoise")
      .setFormat("4m@")
      .setIncludeFilesChangedAfter(4321);
    assertEquals("Porpoise", mt.getPurpose());
    assertEquals("4m@", mt.getFormat());
    assertEquals(4321, mt.getIncludeFilesChangedAfter());

    assertEquals("Flipper", new MetadataTarget("Flipper").getPurpose());
  }

  public void testPredicates() {
    assertTrue(MetadataTarget.Any().isAny());
    assertTrue(MetadataTarget.Doi().isDoi());
    assertTrue(MetadataTarget.OpenURL().isOpenURL());
    assertTrue(MetadataTarget.Article().isArticle());
  }
}
