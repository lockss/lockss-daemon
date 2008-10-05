/*
 * $Id: TestCreativeCommonsPermissionChecker.java,v 1.8 2007-10-04 09:43:40 tlipkis Exp $
 */

/*

Copyright (c) 2000-2002 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.util.*;
import org.lockss.state.*;
import org.lockss.test.*;
import java.io.StringReader;

public class TestCreativeCommonsPermissionChecker
  extends LockssPermissionCheckerTestCase {

  public void setUp() throws Exception {
    super.setUp();
  }

  private static final String grantedRDF =
    "<rdf:RDF xmlns=\"http://web.resource.org/cc/\"\n" +
    "    xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n" +
    "    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n" +
    "<Work rdf:about=\"\">\n" +
    "    <license rdf:resource=\"http://creativecommons.org/licenses/by/2.0/\" />\n" +
    "</Work>\n\n" +
    "<License rdf:about=\"http://creativecommons.org/licenses/by/2.0/\">\n" +
    "    <permits rdf:resource=\"http://web.resource.org/cc/Distribution\" />\n" +
    "    <requires rdf:resource=\"http://web.resource.org/cc/Notice\" />\n" +
    "    <requires rdf:resource=\"http://web.resource.org/cc/Attribution\" />\n" +
    "    <permits rdf:resource=\"http://web.resource.org/cc/DerivativeWorks\" />\n" +
    "    <permits rdf:resource=\"http://web.resource.org/cc/Reproduction\" />\n" +
    "</License>\n\n"+
    "</rdf:RDF>";

  private static final String grantedRDFWithURI =
    "<rdf:RDF xmlns=\"http://web.resource.org/cc/\"\n" +
    "    xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n" +
    "    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n" +
    "<Work rdf:about=\"http://www.lockss.org/registry/\">\n" +
    "    <license rdf:resource=\"http://creativecommons.org/licenses/by/2.0/\" />\n" +
    "</Work>\n\n" +
    "<License rdf:about=\"http://creativecommons.org/licenses/by/2.0/\">\n" +
    "    <permits rdf:resource=\"http://web.resource.org/cc/Distribution\" />\n" +
    "    <requires rdf:resource=\"http://web.resource.org/cc/Notice\" />\n" +
    "    <requires rdf:resource=\"http://web.resource.org/cc/Attribution\" />\n" +
    "    <permits rdf:resource=\"http://web.resource.org/cc/DerivativeWorks\" />\n" +
    "    <permits rdf:resource=\"http://web.resource.org/cc/Reproduction\" />\n" +
    "</License>\n\n"+
    "</rdf:RDF>";

  private static final String deniedRDF =
    "<rdf:RDF xmlns=\"http://web.resource.org/cc/\"\n" +
    "    xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n" +
    "    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n" +
    "<Work rdf:about=\"\">\n" +
    "    <license rdf:resource=\"http://creativecommons.org/licenses/by/2.0/\" />\n" +
    "</Work>\n\n" +
    "<License rdf:about=\"http://creativecommons.org/licenses/by/2.0/\">\n" +
    "    <prohibits rdf:resource=\"http://web.resource.org/cc/Distribution\" />\n" +
    "    <requires rdf:resource=\"http://web.resource.org/cc/Notice\" />\n" +
    "    <requires rdf:resource=\"http://web.resource.org/cc/Attribution\" />\n" +
    "    <permits rdf:resource=\"http://web.resource.org/cc/DerivativeWorks\" />\n" +
    "    <permits rdf:resource=\"http://web.resource.org/cc/Reproduction\" />\n" +
    "</License>\n\n"+
    "</rdf:RDF>";

  private static final String htmlPlusGrantedRDF =
    "<html>\n<head>\n<title>Test Page</title>\n</head>\n<body>\n" +
    "<p>This is a test HTML file with an embedded " +
    "Creative Commons License in a comment.</p>" +
    "<!--\n" +
    grantedRDF +
    "-->\n" +
    "</body>\n</html>";

  private static final String htmlPlusDeniedRDF =
    "<html>\n<head>\n<title>Test Page</title>\n</head>\n<body>\n" +
    "<p>This is a test HTML file with an embedded " +
    "Creative Commons License in a comment.</p>" +
    "<!--\n" +
    deniedRDF +
    "-->\n" +
    "</body>\n</html>";

  private static final String noRDF =
    "This is a test string without any RDF.";

  private static final String malformedRDF =
    "<rdf:RDF xmlns=\"http://web.resource.org/cc/\"\n" +
    "    xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n" +
    "    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n" +
    "<foo rdf:about=\"Meaningless gibberish, no RDF model\" />\n" +
    "<bar rdf:about=\"\">\n" +
    "    <blarg rdf:resource=\"whatever\" />\n" +
    "</bar>\n\n" +
    "</rdf:RDF>";


  private static final String jmirRDF =
    "<!--\n"+
    "\n"+
    "<rdf:RDF xmlns=\"http://web.resource.org/cc/\"\n"+
    "    xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n"+
    "    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n"+
    "<Work rdf:about=\"\">\n"+
    "   <license rdf:resource=\"http://creativecommons.org/licenses/by/2.0/\" />\n"+
    "</Work>\n"+
    "\n"+
    "<License rdf:about=\"http://creativecommons.org/licenses/by/2.0/\">\n"+
    "   <permits rdf:resource=\"http://web.resource.org/cc/Reproduction\" />\n"+
    "   <permits rdf:resource=\"http://web.resource.org/cc/Distribution\" />\n"+
    "   <requires rdf:resource=\"http://web.resource.org/cc/Notice\" />\n"+
    "   <requires rdf:resource=\"http://web.resource.org/cc/Attribution\" />\n"+
    "   <permits rdf:resource=\"http://web.resource.org/cc/DerivativeWorks\" />\n"+
    "</License>\n"+
    "\n"+
    "</rdf:RDF>\n"+
    "\n"+
    "-->\n";
    
  private static final String boneFolderRDF = 
    "<rdf:RDF xmlns=\"http://web.resource.org/cc/\"" + 
    "    xmlns:dc=\"http://purl.org/dc/elements/1.1/\"" + 
    "    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">" +
    "<Work rdf:about=\"\">" +
    "   <dc:type rdf:resource=\"http://purl.org/dc/dcmitype/Text\" />" +
    "   <license rdf:resource=\"http://creativecommons.org/licenses/by-nc-sa/2.5/\" />" +
    "</Work>" +
    "" +
    "<License rdf:about=\"http://creativecommons.org/licenses/by-nc-sa/2.5/\">" +
    "   <permits rdf:resource=\"http://web.resource.org/cc/Reproduction\" />" +
    "   <permits rdf:resource=\"http://web.resource.org/cc/Distribution\" />" +
    "   <requires rdf:resource=\"http://web.resource.org/cc/Notice\" />" +
    "   <requires rdf:resource=\"http://web.resource.org/cc/Attribution\" />" +
    "   <prohibits rdf:resource=\"http://web.resource.org/cc/CommercialUse\" />" + 
    "   <permits rdf:resource=\"http://web.resource.org/cc/DerivativeWorks\" />" +
    "   <requires rdf:resource=\"http://web.resource.org/cc/ShareAlike\" />" +
    "</License>" +
    "" +
    "</rdf:RDF>";

  private static final String entelequiaRDF = 
    "License</a>.<!--/Creative Commons License--><!-- <rdf:RDF\n" +
    "xmlns=\"http://web.resource.org/cc/\"\n" +
    "xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n" +
    "xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n" +
    "        <Work rdf:about=\"\">\n" +
    "                <license\n" +
    "rdf:resource=\"http://creativecommons.org/licenses/by-nc-nd/2.5/\"\n" +
    "/>\n" +
    "        <dc:type rdf:resource=\"http://purl.org/dc/dcmitype/Text\" />\n" +
    "        </Work>\n" +
    "        <License\n" +
    "rdf:about=\"http://creativecommons.org/licenses/by-nc-nd/2.5/\"><permits\n" +
    "rdf:resource=\"http://web.resource.org/cc/Reproduction\"/><permits\n" +
    "rdf:resource=\"http://web.resource.org/cc/Distribution\"/><requires\n" +
    "rdf:resource=\"http://web.resource.org/cc/Notice\"/><requires\n" +
    "rdf:resource=\"http://web.resource.org/cc/Attribution\"/><prohibits\n" +
    "rdf:resource=\"http://web.resource.org/cc/CommercialUse\"/></License></rdf:RDF>\n" +
    "-->";
  
  // Bad start tag:  It reads rdf:RDFF instead of rdf:RDF.  Should not pass.
  private static final String badStartTag =
    "<rdf:RDFF xmlns=\"http://web.resource.org/cc/\"\n" +
    "    xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n" +
    "    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n" +
    "<Work rdf:about=\"\">\n" +
    "    <license rdf:resource=\"http://creativecommons.org/licenses/by/2.0/\" />\n" +
    "</Work>\n\n" +
    "<License rdf:about=\"http://creativecommons.org/licenses/by/2.0/\">\n" +
    "    <permits rdf:resource=\"http://web.resource.org/cc/Distribution\" />\n" +
    "    <requires rdf:resource=\"http://web.resource.org/cc/Notice\" />\n" +
    "    <requires rdf:resource=\"http://web.resource.org/cc/Attribution\" />\n" +
    "    <permits rdf:resource=\"http://web.resource.org/cc/DerivativeWorks\" />\n" +
    "    <permits rdf:resource=\"http://web.resource.org/cc/Reproduction\" />\n" +
    "</License>\n\n"+
    "</rdf:RDF>";
    
  // Imaginary URI.  The permission requires a valid URI for the SAX
  // parser.  If the CC RDF license contains a URI in the <Work
  // rdf:about="..."> attribute, this MUST MATCH IT to be valid.  If
  // the rdf:about attribute is left empty, this is ignored.
  private String pageURI = "http://www.lockss.org/registry/";

  private CreativeCommonsPermissionChecker cc =
    new CreativeCommonsPermissionChecker();
  private StringReader reader;

  public void testNullReader() {
    try {
      cc.checkPermission(pHelper, null, "http://www.example.com/");
      fail("Calling checkPermission(pHelper, null, url) should throw");
    } catch (NullPointerException npe) {
    }
  }

  public void testNoRdf() throws Exception {
    reader = new StringReader("This sentence no RDF");
    assertFalse(cc.checkPermission(pHelper, reader, pageURI));
    reader.close();
    assertNotEquals(AuState.AccessType.OpenAccess, aus.getAccessType());
  }

  public void testCheckGrantedPermissionRDFOnly() throws Exception {
    reader = new StringReader(grantedRDF);
    assertTrue(cc.checkPermission(pHelper, reader, pageURI));
    reader.close();
    assertEquals(AuState.AccessType.OpenAccess, aus.getAccessType());
  }

  public void testCheckGrantedPermissionRDFOnlyWithURI() throws Exception {
    reader = new StringReader(grantedRDFWithURI);
    assertTrue(cc.checkPermission(pHelper, reader, pageURI));
    reader.close();
    assertEquals(AuState.AccessType.OpenAccess, aus.getAccessType());
  }

  public void testCheckDeniedPermissionRDFOnly() throws Exception {
    reader = new StringReader(deniedRDF);
    assertFalse(cc.checkPermission(pHelper, reader, pageURI));
    reader.close();
    assertNotEquals(AuState.AccessType.OpenAccess, aus.getAccessType());
  }

  public void testCheckGrantedPermissionHTMLAndRDF() throws Exception {
    reader = new StringReader(htmlPlusGrantedRDF);
    assertTrue(cc.checkPermission(pHelper, reader, pageURI));
    reader.close();
    assertEquals(AuState.AccessType.OpenAccess, aus.getAccessType());
  }

  public void testCheckDeniedPermissionHTMLAndRDF() throws Exception {
    reader = new StringReader(htmlPlusDeniedRDF);
    assertFalse(cc.checkPermission(pHelper, reader, pageURI));
    reader.close();
    assertNotEquals(AuState.AccessType.OpenAccess, aus.getAccessType());
  }

  public void testCheckDeniedPermissionInvalidRDF() throws Exception {
    reader = new StringReader(malformedRDF);
    assertFalse(cc.checkPermission(pHelper, reader, pageURI));
    reader.close();
    assertNotEquals(AuState.AccessType.OpenAccess, aus.getAccessType());
  }

  public void testCheckDeniedPermissionNoRDF() throws Exception {
    reader = new StringReader(noRDF);
    assertFalse(cc.checkPermission(pHelper, reader, pageURI));
    reader.close();
    assertNotEquals(AuState.AccessType.OpenAccess, aus.getAccessType());
  }

  public void testCheckJMIR() throws Exception {
    reader = new StringReader(jmirRDF);
    assertTrue(cc.checkPermission(pHelper, reader, pageURI));
    reader.close();
    assertEquals(AuState.AccessType.OpenAccess, aus.getAccessType());
  }
  
  public void testCheckBonefolder() throws Exception {
    reader = new StringReader(boneFolderRDF);
    assertTrue(cc.checkPermission(pHelper, reader, pageURI));
    reader.close();
    assertEquals(AuState.AccessType.OpenAccess, aus.getAccessType());
  }

  public void testCheckEntelequia() throws Exception {
    reader = new StringReader(entelequiaRDF);
    assertTrue(cc.checkPermission(pHelper, reader, pageURI));
    reader.close();
    assertEquals(AuState.AccessType.OpenAccess, aus.getAccessType());
  }
  
  public void testCheckBadStartTag() throws Exception {
    reader = new StringReader(badStartTag);
    assertFalse(cc.checkPermission(pHelper, reader, pageURI));
    reader.close();
    assertNotEquals(AuState.AccessType.OpenAccess, aus.getAccessType());
  }
}
