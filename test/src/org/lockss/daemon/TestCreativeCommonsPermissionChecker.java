/*
 * $Id: TestCreativeCommonsPermissionChecker.java,v 1.1 2004-10-18 02:57:49 smorabito Exp $
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
import org.lockss.test.*;
import java.io.StringReader;

public class TestCreativeCommonsPermissionChecker extends LockssTestCase {
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

  private CreativeCommonsPermissionChecker cc =
    new CreativeCommonsPermissionChecker();

  private StringReader reader;

  public void testCheckGrantedPermissionRDFOnly() throws Exception {
    reader = new StringReader(grantedRDF);
    assertTrue(cc.checkPermission(reader));
    reader.close();
  }

  public void testCheckDeniedPermissionRDFOnly() throws Exception {
    reader = new StringReader(deniedRDF);
    assertFalse(cc.checkPermission(reader));
    reader.close();
  }

  public void testCheckGrantedPermissionHTMLAndRDF() throws Exception {
    reader = new StringReader(htmlPlusGrantedRDF);
    assertTrue(cc.checkPermission(reader));
    reader.close();
  }

  public void testCheckDeniedPermissionHTMLAndRDF() throws Exception {
    reader = new StringReader(htmlPlusDeniedRDF);
    assertFalse(cc.checkPermission(reader));
    reader.close();
  }

  public void testCheckDeniedPermissionInvalidRDF() throws Exception {
    reader = new StringReader(malformedRDF);
    assertFalse(cc.checkPermission(reader));
    reader.close();
  }

  public void testCheckDeniedPermissionNoRDF() throws Exception {
    reader = new StringReader(noRDF);
    assertFalse(cc.checkPermission(reader));
    reader.close();
  }

}
