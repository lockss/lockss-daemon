/*
 * $Id: TestOaiRequestData.java,v 1.3 2005-10-11 05:49:58 tlipkis Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.oai;

import org.lockss.test.*;

/**
 * This is the test class for org.lockss.oai.OaiRequestData
 */

public class TestOaiRequestData extends LockssTestCase {

  private String handler = "handler";
  private String ns = "ns";
  private String tag = "tag";
  private String setSpec = "setSpec";
  private String prefix = "prefix";

  public TestOaiRequestData(String msg){
    super(msg);
  }

  public void testNullParameter(){
    try {
      OaiRequestData od =
	new OaiRequestData( (String) null, ns, tag, setSpec, prefix);
      fail("OaiRequestData with null Oai request handler url should throw");
    } catch (IllegalArgumentException e) { }
    try {
      OaiRequestData od =
	new OaiRequestData( handler, (String) null, tag, setSpec, prefix);
      fail("OaiRequestData with null metadata namespace url should throw");
    } catch (IllegalArgumentException e) { }
    try {
      OaiRequestData od =
	new OaiRequestData( handler, ns, (String) null, setSpec, prefix);
      fail("OaiRequestData with null url container tag name  should throw");
    } catch (IllegalArgumentException e) { }
//     try {
//       OaiRequestData od =
// 	new OaiRequestData( handler, ns, tag, (String) null, prefix);
//       fail("OaiRequestData with null AU SetSpec should throw");
//     } catch (IllegalArgumentException e) { }
    try {
      OaiRequestData od =
	new OaiRequestData( handler, ns, tag, setSpec, (String) null);
      fail("OaiRequestData with null metadata prefix should throw");
    } catch (IllegalArgumentException e) { }
  }

  public void testSimpleContruction() {
    OaiRequestData oaiData = new OaiRequestData(handler,ns,tag,setSpec,prefix);
    assertEquals(handler, oaiData.getOaiRequestHandlerUrl());
    assertEquals(ns, oaiData.getMetadataNamespaceUrl());
    assertEquals(tag, oaiData.getUrlContainerTagName());
    assertEquals(setSpec, oaiData.getAuSetSpec());
    assertEquals(prefix, oaiData.getMetadataPrefix());
  }

}
