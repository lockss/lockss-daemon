/*

Copyright (c) 2000-2024, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.daemon;

import java.io.*;

import org.lockss.state.*;

/**
 * <p>Title: TestCreativeCommonsV3PermissionChecker</p>
 * <p>@author Claire Griffin</p>
 * <p>@version 1.0</p>
 */
public class TestCreativeCommonsPermissionChecker
  extends LockssPermissionCheckerTestCase {

  private static final String template =
    "<html>/n<head>/n<title>FOO</title>\n</head>" +
    "some text%smore text\n" +
    "</body>\n</html>\n";

  private static final String VALID_URL_STEM = "http://creativecommons.org/licenses";
  private static final String VALID_URL_STEM_S = "https://creativecommons.org/licenses";

  private String TEST_URL = "http://www.example.com/";

  public void setUp() throws Exception {
    super.setUp();
  }

  // return a CC license URL for the specified license terms and version
  private static String lu(String lic, String ver) {
    return VALID_URL_STEM + "/" + lic + "/" + ver + "/";
  }

  private static String lus(String lic, String ver) {
    return VALID_URL_STEM_S + "/" + lic + "/" + ver + "/";
  }

  // return a CC license tag element
  private String htext(String tag) {
    return String.format(template, tag);
  }

  private void assertPerm(String tag) {
    String text = htext(tag);
    CreativeCommonsPermissionChecker checker =
      new CreativeCommonsPermissionChecker();
    assertTrue(tag + " expected permission, wasn't",
	       checker.checkPermission(mcf,
				       new StringReader(text), TEST_URL));
    assertEquals(AuState.AccessType.OpenAccess, aus.getAccessType());
  }

  private void assertNoPerm(String tag) {
    String text = htext(tag);
    CreativeCommonsPermissionChecker checker =
      new CreativeCommonsPermissionChecker();
    assertFalse(tag + " expected no permission, but was",
	       checker.checkPermission(mcf,
				       new StringReader(text), TEST_URL));
  }


  public void testValidPermissions(String[] licenses,
				   String[] versions) {
    for (String lic : licenses) {
      for (String ver : versions) {
	assertPerm("<a href=\"" + lu(lic, ver) + "\" rel=\"license\" />");
	assertPerm("<a href=\"" + lus(lic, ver) + "\" rel=\"license\" />");
	assertPerm("<link href=\"" + lu(lic, ver) + "\" rel=\"license\" />");
	assertPerm("<link href=\"" + lus(lic, ver) + "\" rel=\"license\" />");
	assertPerm("<a rel=\"license\" href=\"" + lu(lic, ver) + "\" />");
	assertPerm("<a rel=\"license\" href=\"" + lus(lic, ver) + "\" />");
	assertPerm("<link class=\"bar\" rel=\"license\" href=\"" +
		   lu(lic, ver) + "\" />");
	assertPerm("<link class=\"bar\" rel=\"license\" href=\"" +
		   lus(lic, ver) + "\" />");

	assertNoPerm("<a href=\"" + lu(lic + "fnord", ver) + "\" rel=\"license\" />");
	assertNoPerm("<a href=\"" + lus(lic + "fnord", ver) + "\" rel=\"license\" />");
	assertNoPerm("<a href=\"" + lu(lic, ver + "gorp") + "\" rel=\"license\" />");
	assertNoPerm("<a href=\"" + lus(lic, ver + "gorp") + "\" rel=\"license\" />");
      }
    }
  }

  public void testCase() {
    assertPerm("<a href=\"" + lu("by", "4.0") + "\" rel=\"license\" />");
    assertPerm("<a href=\"" + lu("by", "4.0") + "\" rel=\"license\" />".toUpperCase());
    assertPerm("<a href=\"" + lus("by", "4.0") + "\" rel=\"license\" />");
    assertPerm("<a href=\"" + lus("by", "4.0") + "\" rel=\"license\" />".toUpperCase());
  }

  public void testWhitespace() {
    assertPerm("<a href=\"" + lu("by", "4.0") + "\" rel=\"license\" />");
    assertPerm("<a href=\"" + lus("by", "4.0") + "\" rel=\"license\" />");
    assertPerm("<a\nhref=\"" + lu("by", "4.0") + "\"\nrel=\"license\"\n/>");
    assertPerm("<a\thref=\"" + lu("by", "4.0") + "\"\trel=\"license\"\t/>");
    assertPerm("<a\rhref=\"" + lu("by", "4.0") + "\"\rrel=\"license\"\r/>");
    assertPerm("<a\r\nhref=\"" + lu("by", "4.0") + "\"\r\nrel=\"license\"\r\n/>");
  }

  public void testInvalidPermissions() {
    assertPerm("<a href=\"" + lu("by", "4.0") + "\" rel=\"license\" />");
    assertPerm("<a href=\"" + lus("by", "4.0") + "\" rel=\"license\" />");
    assertNoPerm("<img href=\"" + lu("by", "4.0") + "\" rel=\"license\" />");
    assertNoPerm("<img href=\"" + lus("by", "4.0") + "\" rel=\"license\" />");
    assertNoPerm("<a nohref=\"" + lu("by", "4.0") + "\" rel=\"license\" />");
    assertNoPerm("<a href=\"" + lu("not", "4.0") + "\" rel=\"license\" />");
    assertNoPerm("<a href=\"" + lu("by", "5.0") + "\" rel=\"license\" />");
    assertNoPerm("<a href=\"" + lu("by", "4.0") + "\" norel=\"license\" />");
    assertNoPerm("<a href=\"" + lu("by", "4.0") + "\" rel=\"uncle\" />");
    assertNoPerm("<a href=\"" + lu("by", "4.0") + "\" />");
    assertNoPerm("<a href=\"http://example.com\" rel=\"license\" />");
    assertNoPerm("<a href=\"" + lu("", "4.0") + "\" rel=\"license\" />");
    assertNoPerm("<a href=\"" + lu("by", "") + "\" rel=\"license\" />");
  }

  private static final String cssTemplate =
    "<html>/n<head>/n<title>FOO</title>\n</head>\n" +
    "<style>\n" +
    "  .box-metrics label.checked {\n" +
    "    background-image: url('http://not.cc//foo');\n" +
    "  }\n" +
    "  .box-related-articles h2.open, .box-metrics h2.open {\n" +
    "    background: url('" + lu("by", "3.0") + "') top right no-repeat;\n" +
    "  }\n" +
    "</style>\n" +
    "some text more text\n" +
    "</body>\n</html>\n";

  public void testCss() {
    CreativeCommonsPermissionChecker checker =
      new CreativeCommonsPermissionChecker();
    assertFalse(checker.checkPermission(mcf,
					new StringReader(cssTemplate),
					TEST_URL));
  }
  
  protected enum Tag { A, LINK }
  protected enum Attrs { NONE, HREF, REL_X, REL_LICENSE, HREF_REL_X, HREF_REL_LICENSE, REL_X_HREF, REL_LICENSE_HREF }
  protected enum Proto { HTTP, HTTPS }
  protected enum Host { NO_WWW, WWW }
  protected enum License { CC_1_0, CC_2_0, CC_2_1, CC_2_5, CC_3_0, CC_4_0, CC0_1_0, CERT_1_0, PDM_1_0 }
  protected enum Variant { BY, BY_NC, BY_NC_SA, BY_ND, BY_NC_ND, BY_ND_NC, BY_SA, NC, NC_SA, NC_SAMPLING_PLUS, NC_SAMPLING_PLUS_UEL, NC_SAMPLING_PLUS_UEU, ND, NC_ND, ND_NC, SA, SAMPLING, SAMPLING_PLUS, SAMPLING_PLUS_UEL, SAMPLING_PLUS_UEU, DEVNATIONS }
  protected enum Ending { NONE, SLASH, DEED_EN, LEGAL_EN, DEED_LANG, LEGAL_LANG }
  
  public void testCombinatorics() throws Exception {
    for (Tag tag : Tag.values()) {
      for (Attrs attrs : Attrs.values()) {
        for (Proto proto : Proto.values()) {
          for (Host host : Host.values()) {
            for (License lic : License.values()) {
              for (Variant vart : Variant.values()) {
                for (Ending end : Ending.values()) {
                  // Build template
                  StringBuilder sb = new StringBuilder();
                  sb.append("<html><head>");
                  if (tag == Tag.LINK) {
                    createLink(sb, tag, attrs, proto, host, lic, vart, end);
                  }
                  sb.append("</head><body>");
                  if (tag == Tag.A) {
                    createLink(sb, tag, attrs, proto, host, lic, vart, end);
                  }
                  sb.append("</body></html>");
                  // Invoke permission checker
                  String html = sb.toString();
                  boolean expected = isValid(attrs, lic, vart);
                  boolean actual = new CreativeCommonsPermissionChecker().checkPermission(mcf, new StringReader(html), TEST_URL);
                  assertEquals(String.format("Expected %s but was %s: %s", expected, actual, html),
                               expected,
                               actual);
                }
              }
            }
          }
        }
      }
    }
  }
  
  private static void createLink(StringBuilder sb,
                                 Tag tag,
                                 Attrs attrs,
                                 Proto proto,
                                 Host host,
                                 License lic,
                                 Variant vart,
                                 Ending end) {
    sb.append("<");
    sb.append(tag == Tag.A ? "a" : "link");
    switch (attrs) {
      case REL_X: case REL_X_HREF: sb.append(" rel=\"nothing\""); break;
      case REL_LICENSE: case REL_LICENSE_HREF: sb.append(" rel=\"license\""); break;
      default: break; // Intentionally left blank
    }
    switch (attrs) {
      case HREF: case HREF_REL_X: case HREF_REL_LICENSE: case REL_X_HREF: case REL_LICENSE_HREF:
        sb.append(" href=\"");
        sb.append(proto == Proto.HTTP ? "http://" : "https://");
        sb.append(host == Host.WWW ? "www." : "");
        sb.append("creativecommons.org");
        switch (lic) {
          case CC0_1_0: sb.append("/publicdomain/zero/1.0"); break;
          case CERT_1_0: sb.append("/publicdomain/certification/1.0"); break;
          case PDM_1_0: sb.append("/publicdomain/mark/1.0"); break;
          default:
            sb.append("/licenses/");
            switch (vart) {
              case BY: sb.append("by"); break;
              case BY_NC: sb.append("by-nc"); break;
              case BY_NC_ND: sb.append("by-nc-nd"); break;
              case BY_NC_SA: sb.append("by-nc-sa"); break;
              case BY_ND: sb.append("by-nd"); break;
              case BY_ND_NC: sb.append("by-nd-nc"); break;
              case BY_SA: sb.append("by-sa"); break;
              case DEVNATIONS: sb.append("devnations"); break;
              case NC: sb.append("nc"); break;
              case NC_ND: sb.append("nc-nd"); break;
              case NC_SA: sb.append("nc-sa"); break;
              case NC_SAMPLING_PLUS: sb.append("nc-sampling+"); break;
              case NC_SAMPLING_PLUS_UEL: sb.append("nc-sampling%2b"); break;
              case NC_SAMPLING_PLUS_UEU: sb.append("nc-sampling%2B"); break;
              case ND: sb.append("nd"); break;
              case ND_NC: sb.append("nd-nc"); break;
              case SA: sb.append("sa"); break;
              case SAMPLING: sb.append("sampling"); break;
              case SAMPLING_PLUS: sb.append("sampling+"); break;
              case SAMPLING_PLUS_UEL: sb.append("sampling%2b"); break;
              case SAMPLING_PLUS_UEU: sb.append("sampling%2B"); break;
              default: throw new ShouldNotHappenException(vart.toString());
            }
            switch (lic) {
              case CC_1_0: sb.append("/1.0"); break;
              case CC_2_0: sb.append("/2.0"); break;
              case CC_2_1: sb.append("/2.1"); break;
              case CC_2_5: sb.append("/2.5"); break;
              case CC_3_0: sb.append("/3.0"); break;
              case CC_4_0: sb.append("/4.0"); break;
            }
        }
        switch (end) {
          case NONE: break; // Intentionally left blank
          case SLASH: sb.append("/"); break;
          case DEED_EN: sb.append("/deed.en"); break;
          case LEGAL_EN: sb.append("/legalcode.en"); break;
          case DEED_LANG: sb.append("/fr/deed.fr"); break;
          case LEGAL_LANG: sb.append("/fr/legalcode.fr"); break;
          default: throw new ShouldNotHappenException(end.toString());
        }
        sb.append("\"");
        break;
    }
    switch (attrs) {
      case HREF_REL_X: sb.append(" rel=\"nothing\""); break;
      case HREF_REL_LICENSE: sb.append(" rel=\"license\""); break;
      default: break; // Intentionally left blank
    }
    sb.append(tag == Tag.A ? ">LINK TEXT</a>" : " />");
  }
  
  private static boolean isValid(Attrs attrs, License lic, Variant vart) {
    switch (attrs) {
      case HREF_REL_LICENSE: case REL_LICENSE_HREF:
        switch (lic) {
          case CC0_1_0: case CERT_1_0: case PDM_1_0: return true;
          case CC_1_0:
            switch (vart) {
              case DEVNATIONS: return false; // 2.0 only
              case BY_NC_ND: case NC_ND: return true; // We accept this spelling
              default: return true;
            }
          case CC_2_0:
            switch (vart) {
              case BY: case BY_NC: case BY_NC_ND: case BY_NC_SA: case BY_ND: case BY_SA: return true;
              case DEVNATIONS: return true; // 2.0 only
              case NC: case NC_SA: case ND: case ND_NC: case SA: return true; // Japan only
              case NC_ND: return true; // We accept this spelling
              default: return false;
            }
          default:
            switch (vart) {
              case BY: case BY_NC: case BY_NC_ND: case BY_NC_SA: case BY_ND: case BY_SA: return true;
              default: return false;
            }
        }
      default: return false;
    }
  }
    
}
