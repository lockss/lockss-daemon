/*
 * $Id$
 */

/*

 Copyright (c) 2015 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.servlet;

import java.util.HashMap;
import java.util.Map;
import org.lockss.test.LockssTestCase;

/**
 * Test class for org.lockss.servlet.SubscriptionManagmentr.
 * 
 * @author Fernando Garcia-Loygorri
 */
public class TestSubscriptionManagement extends LockssTestCase {

  /**
   * Check the behavior of getTriBoxValue().
   */
  public final void testGetTriBoxValue() {
    SubscriptionManagement sm = new SubscriptionManagement();
    assertNull(sm.getTriBoxValue(null, null));

    Map<String,String> parameterMap = new HashMap<String, String>();
    assertNull(sm.getTriBoxValue(parameterMap, null));
    assertNull(sm.getTriBoxValue(parameterMap, ""));
    assertNull(sm.getTriBoxValue(parameterMap, "   "));

    String id = "testId";
    assertNull(sm.getTriBoxValue(parameterMap, id));

    parameterMap.put(id, null);
    assertNull(sm.getTriBoxValue(parameterMap, id));

    String hiddenId =
	id + SubscriptionManagement.TRI_STATE_WIDGET_HIDDEN_ID_SUFFIX;

    parameterMap.put(hiddenId, null);
    assertNull(sm.getTriBoxValue(parameterMap, id));

    parameterMap.put(hiddenId,
	SubscriptionManagement.TRI_STATE_WIDGET_HIDDEN_ID_UNSET_VALUE);
    assertNull(sm.getTriBoxValue(parameterMap, id));

    parameterMap.put(hiddenId, Boolean.FALSE.toString());
    assertEquals(Boolean.FALSE, sm.getTriBoxValue(parameterMap, id));

    parameterMap.put(hiddenId, Boolean.TRUE.toString());
    assertEquals(Boolean.TRUE, sm.getTriBoxValue(parameterMap, id));
  }
}
