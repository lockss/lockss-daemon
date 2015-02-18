/*
 * $Id$
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.springer;

import org.lockss.plugin.UrlNormalizer;
import org.lockss.test.LockssTestCase;

public class TestSpringerLinkUrlNormalizer extends LockssTestCase {

  protected UrlNormalizer norm;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    norm = new SpringerLinkUrlNormalizer();
  }
  
  public void testCssUrls() throws Exception {
    final String expected = "http://www.springerlink.com/dynamic-file.axd?id=80623e25-67f3-4182-9ee3-9a76475bea6e&m=True";
    doTest(expected);
    doTest(expected, "http://www.springerlink.com/dynamic-file.axd?id=a209de6f-81f0-43df-b579-003b2a6c2c3b&m=True");
    doTest(expected, "http://www.springerlink.com/dynamic-file.axd?id=7e8f2781-87c9-48ff-ac64-b7521267eb3d&m=True");
    doTest(expected, "http://www.springerlink.com/dynamic-file.axd?id=d25b4bf4-02b3-47f8-8cb5-9bdebf5da452&m=True");
    doTest(expected, "http://www.springerlink.com/dynamic-file.axd?id=b4a7f886-a7e7-4dfa-8942-6c13eacc7c38&m=True");
    doTest(expected, "http://www.springerlink.com/dynamic-file.axd?id=555b8adc-82b5-49e4-a40b-5cae3355208c&m=True");
    doTest(expected, "http://www.springerlink.com/dynamic-file.axd?id=be38f38c-6e9c-41c8-8f24-616ce62a7f94&m=True");
  }
  
  public void testJavascript1Urls() throws Exception {
    final String expected = "http://www.springerlink.com/dynamic-file.axd?id=f6fcb095-3d49-4291-984c-1fa1626f3a8a&m=True";
    doTest(expected);
    doTest(expected, "http://www.springerlink.com/dynamic-file.axd?id=505cc7ec-c441-4d6a-a89c-4634a00eba34&m=True");
    doTest(expected, "http://www.springerlink.com/dynamic-file.axd?id=1108e1d4-97ac-4875-80c8-37bf1b132c8b&m=True");
    doTest(expected, "http://www.springerlink.com/dynamic-file.axd?id=71cb314e-b50a-4dc7-bc4d-fe244c65d4f2&m=True");
    doTest(expected, "http://www.springerlink.com/dynamic-file.axd?id=deafc402-eb3e-48e7-b0bb-2baa40586f62&m=True");
    doTest(expected, "http://www.springerlink.com/dynamic-file.axd?id=89fb321a-e8ea-40c3-ac9a-b24727113d7f&m=True");
    doTest(expected, "http://www.springerlink.com/dynamic-file.axd?id=e462af41-20fd-4966-bf4f-113610bfd1ec&m=True");
    doTest(expected, "http://www.springerlink.com/dynamic-file.axd?id=cf0e16f3-cd2d-4cbc-b366-efdc9ec5c6f2&m=True");
    doTest(expected, "http://www.springerlink.com/dynamic-file.axd?id=f1acae59-9f16-4b8c-85cc-c3eaacae39f2&m=True");
    doTest(expected, "http://www.springerlink.com/dynamic-file.axd?id=55d69b78-3e0a-43ba-a150-18ce8a3ceeba&m=True");
    doTest(expected, "http://www.springerlink.com/dynamic-file.axd?id=5d34e280-1ff2-4079-9190-7fc639a99a57&m=True");
    doTest(expected, "http://www.springerlink.com/dynamic-file.axd?id=22c40097-4145-4d58-8cf7-63c1d4439399&m=True");
    doTest(expected, "http://www.springerlink.com/dynamic-file.axd?id=74466dca-978b-4c51-bbb6-02378a4905e1&m=True");
    doTest(expected, "http://www.springerlink.com/dynamic-file.axd?id=99381f0b-4327-49b7-afa4-de3fc33ad947&m=True");
    doTest(expected, "http://www.springerlink.com/dynamic-file.axd?id=48e4d99e-0f75-417e-8e54-611005af3a10&m=True");
    doTest(expected, "http://www.springerlink.com/dynamic-file.axd?id=17d72427-be38-4c61-a23c-d723395cac64&m=True");
    doTest(expected, "http://www.springerlink.com/dynamic-file.axd?id=f7790a4f-b0eb-4bfe-93e8-70470325fb27&m=True");
    doTest(expected, "http://www.springerlink.com/dynamic-file.axd?id=36b4f92d-dee2-442e-992a-36dceaccb4c6&m=True");
    doTest(expected, "http://www.springerlink.com/dynamic-file.axd?id=801d651f-f3be-48b6-9d5c-f6f33b9366c0&m=True");
    doTest(expected, "http://www.springerlink.com/dynamic-file.axd?id=23c7edb8-0e91-47b4-8385-5728f0b0e2d5&m=True");
    doTest(expected, "http://www.springerlink.com/dynamic-file.axd?id=d149bcb2-d576-4cb0-9761-ac538aef6037&m=True");
    doTest(expected, "http://www.springerlink.com/dynamic-file.axd?id=e907c3f2-74ac-4f1f-8529-e9497606323b&m=True");
    doTest(expected, "http://www.springerlink.com/dynamic-file.axd?id=50bd5442-ad0a-4578-9372-c92b928fe728&m=True");
    doTest(expected, "http://www.springerlink.com/dynamic-file.axd?id=d7e19a36-2ce1-4691-95a4-866a4381977e&m=True");
    doTest(expected, "http://www.springerlink.com/dynamic-file.axd?id=3975407c-2da1-455a-99ae-a066f8d78a8c&m=True");
    doTest(expected, "http://www.springerlink.com/dynamic-file.axd?id=e408ae52-e310-4f94-839d-e1dad4df509f&m=True");
    doTest(expected, "http://www.springerlink.com/dynamic-file.axd?id=8272bab5-29b3-4aea-8c03-d6be1e65a7c5&m=True");
    doTest(expected, "http://www.springerlink.com/dynamic-file.axd?id=448d74db-3a49-47cd-a22e-30f88ad429c7&m=True");
    doTest(expected, "http://www.springerlink.com/dynamic-file.axd?id=f0b3b809-2b0c-4996-b73e-3b855bcbdafa&m=True");
    doTest(expected, "http://www.springerlink.com/dynamic-file.axd?id=695015e5-1327-447b-b711-8224b68bd201&m=True");
    doTest(expected, "http://www.springerlink.com/dynamic-file.axd?id=93bd6382-c886-4f5d-a75f-9ef517e06cbb&m=True");
    doTest(expected, "http://www.springerlink.com/dynamic-file.axd?id=76bb4537-0fae-43b1-9c0c-71f06031a6b9&m=True");
  }
  
  public void testJavascript2Urls() throws Exception {
    final String expected = "http://www.springerlink.com/dynamic-file.axd?id=88d24f97-0be5-46cc-ba7a-c97002f465c0&m=True";
    doTest(expected);
    doTest(expected, "http://www.springerlink.com/dynamic-file.axd?id=40594b26-7aa9-4e22-b8b8-c1d26d21b52a&m=True");
    doTest(expected, "http://www.springerlink.com/dynamic-file.axd?id=26cc528d-424b-4f70-93ec-00d6a5ebd0c7&m=True");
    doTest(expected, "http://www.springerlink.com/dynamic-file.axd?id=b0622fd9-cdd1-41f5-9b63-533c6c2421c6&m=True");
    doTest(expected, "http://www.springerlink.com/dynamic-file.axd?id=5120b55b-99e7-41ca-9657-bd9fd6eec7e6&m=True");
    doTest(expected, "http://www.springerlink.com/dynamic-file.axd?id=0f9d5a7b-28ef-4e86-98e4-e5fafc71dd75&m=True");
    doTest(expected, "http://www.springerlink.com/dynamic-file.axd?id=54b6894c-8544-45e8-9ca9-333567e5add3&m=True");
  }

  public void testNoMatchUrls() throws Exception {
    doTest("http://www.lockss.org/");
    doTest("http://www.springerlink.com/favicon.ico");
    doTest("http://www.springerlink.com/dynamic-file.axd?id=40594b26-7aa9-4e22-b8b8-c1d26d21b52a&m=False");
    doTest("http://www.springerlink.com/dynamic-file.axd?id=40594b26-7aa9-4e22-b8b8-c1d26d21b52a");
    doTest("http://www.springerlink.com/dynamic-file.axd?id=40594b26-7aa9-4e22-b8b8-c1d26d21b52&m=True");
    doTest("http://www.springerlink.com/dynamic-file.axd?id=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx&m=True");
    doTest("http://www.springerlink.com/dynamic-file.axd?id=xxxxxxxxXxxxx-xxxx-xxxx-xxxxxxxxxxxx&m=True");
    doTest("http://www.springerlink.com/dynamic-file.axd?id=xxxxxxxx-xxxxXxxxx-xxxx-xxxxxxxxxxxx&m=True");
    doTest("http://www.springerlink.com/dynamic-file.axd?id=xxxxxxxx-xxxx-xxxxXxxxx-xxxxxxxxxxxx&m=True");
    doTest("http://www.springerlink.com/dynamic-file.axd?id=xxxxxxxx-xxxx-xxxx-xxxxXxxxxxxxxxxxx&m=True");
  }
  
  private void doTest(String expected, String url) throws Exception {
    assertEquals(expected, norm.normalizeUrl(url, null));
  }

  private void doTest(String urlAndExcepted) throws Exception {
    doTest(urlAndExcepted, urlAndExcepted);
  }

}
