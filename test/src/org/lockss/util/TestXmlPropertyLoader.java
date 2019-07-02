/*

Copyright (c) 2000-2019 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.util;

import java.util.*;
import java.io.*;
import java.net.URL;
import javax.xml.parsers.*;

import org.xml.sax.helpers.*;
import org.lockss.test.*;
import org.lockss.config.*;
import org.lockss.app.*;

/**
 * Test class for <code>org.lockss.util.XmlPropertyLoader</code> and
 * <code>org.lockss.util.LockssConfigHandler</code>.
 */

public class TestXmlPropertyLoader extends LockssTestCase {

  private PropertyTree m_props = null;
  private MockXmlPropertyLoader m_xmlPropertyLoader = null;

  public void setUp() throws Exception {
    super.setUp();
    m_xmlPropertyLoader = new MockXmlPropertyLoader();
    // Set default values for testing conditionals.
    setDefaultVersions();
    parseXmlProperties();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  static Logger log = Logger.getLogger("TestXmlPropertyLoader");

  /**
   * Parse the XML test configuration and set m_props.
   */
  private void parseXmlProperties() throws Exception {
    String file = "configtest.xml";
    URL url = getResource(file);
    InputStream istr = UrlUtil.openInputStream(url.toString());

    PropertyTree props = new PropertyTree();
    m_xmlPropertyLoader.loadProperties(props, istr);
    m_props = props;
  }

  /**
   * Test known-bad XML.
   */
  public void testUnknownXmlTag() throws Exception {
    PropertyTree props = new PropertyTree();
    StringBuffer sb = new StringBuffer();
    sb.append("<lockss-config>\n");
    sb.append("  <some-unknown-tag name=\"foo\" value=\"bar\" />\n");
    sb.append("</lockss-config>\n");
    InputStream istr =
      new ReaderInputStream(new StringReader(sb.toString()));
    try {
      m_xmlPropertyLoader.loadProperties(props, istr);
      fail("Should have thrown.");
    } catch (Throwable t) {
    }
  }

  /**
   * Test mixing daemon version and daemon min / daemon max (illegal)
   */
  public void testIllegalDaemonVersionCombo() throws Exception {
    PropertyTree props = new PropertyTree();
    StringBuffer sb = new StringBuffer();
    sb.append("<lockss-config>\n");
    sb.append("  <if daemonVersion=\"2.0.0\" daemonVersionMax=\"3.0.0\">\n");
    sb.append("    <property name=\"a\" value=\"foo\" />");
    sb.append("  </if>\n");
    sb.append("  <if daemonVersion=\"2.0.0\" daemonVersionMin=\"1.0.0\">\n");
    sb.append("    <property name=\"b\" value=\"foo\" />");
    sb.append("  </if>\n");
    sb.append("</lockss-config>\n");
    InputStream istr =
      new ReaderInputStream(new StringReader(sb.toString()));
    try {
      m_xmlPropertyLoader.loadProperties(props, istr);
      fail("Should have thrown.");
    } catch (Throwable t) {
    }
  }

  /**
   * Test mixing platform version and platform min / platform max (illegal)
   */
  public void testIllegalPlatformVersionCombo() throws Exception {
    PropertyTree props = new PropertyTree();
    StringBuffer sb = new StringBuffer();
    sb.append("<lockss-config>\n");
    sb.append("  <if platformVersion=\"150\" platformVersionMax=\"200\">\n");
    sb.append("    <property name=\"a\" value=\"foo\" />");
    sb.append("  </if>\n");
    sb.append("  <if platformVersion=\"150\" platformVersionMin=\"100\">\n");
    sb.append("    <property name=\"b\" value=\"foo\" />");
    sb.append("  </if>\n");
    sb.append("</lockss-config>\n");
    InputStream istr =
      new ReaderInputStream(new StringReader(sb.toString()));
    try {
      m_xmlPropertyLoader.loadProperties(props, istr);
      fail("Should have thrown.");
    } catch (Throwable t) {
    }
  }

  /**
   * This test was added to exercise issue# 1526.
   */
  public void testCXSerializerCompatibilityModeSetProperly() throws Exception {
    PropertyTree props;
    InputStream istr;
    StringBuffer sb;

    sb = new StringBuffer();
    sb.append("<lockss-config>\n");
    sb.append("  <property name=\"org.lockss\">\n");
    sb.append("    <if>\n");
    sb.append("      <not>\n");
    sb.append("        <test group=\"dev\" daemonVersionMin=\"1.13.0\"/>\n");
    sb.append("      </not>\n");
    sb.append("      <then>\n");
    sb.append("        <property name=\"serialization.compatibilityMode\" value=\"1\" />\n");
    sb.append("      </then>\n");
    sb.append("    </if>\n");
    sb.append("  </property>\n");
    sb.append("</lockss-config>\n");

    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions("1.13.1", "OpenBSD CD-200", "testhost", "dev");
    m_xmlPropertyLoader.loadProperties(props, istr);
    assertNull(props.getProperty("org.lockss.serialization.compatibilityMode"));

    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions("1.12.3", "OpenBSD CD-200", "testhost", "beta");
    m_xmlPropertyLoader.loadProperties(props, istr);
    assertEquals("1", props.getProperty("org.lockss.serialization.compatibilityMode"));

    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions("1.12.3", "OpenBSD CD-200", "testhost", "dev");
    m_xmlPropertyLoader.loadProperties(props, istr);
    assertEquals("1", props.getProperty("org.lockss.serialization.compatibilityMode"));

    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions("1.13.1", "OpenBSD CD-200", "testhost", "beta");
    m_xmlPropertyLoader.loadProperties(props, istr);
    assertEquals("1", props.getProperty("org.lockss.serialization.compatibilityMode"));

    // This should be equivalent, implicit <and>
    sb = new StringBuffer();
    sb.append("<lockss-config>\n");
    sb.append("  <property name=\"org.lockss\">\n");
    sb.append("    <if>\n");
    sb.append("      <not>\n");
    sb.append("        <test daemonVersionMin=\"1.13.0\"/>\n");
    sb.append("        <test group=\"dev\"/>\n");
    sb.append("      </not>\n");
    sb.append("      <then>\n");
    sb.append("        <property name=\"serialization.compatibilityMode\" value=\"1\" />\n");
    sb.append("      </then>\n");
    sb.append("    </if>\n");
    sb.append("  </property>\n");
    sb.append("</lockss-config>\n");

    // T T
    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions("1.13.1", "OpenBSD CD-200", "testhost", "dev");
    m_xmlPropertyLoader.loadProperties(props, istr);
    assertNull(props.getProperty("org.lockss.serialization.compatibilityMode"));

    // F F
    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions("1.12.3", "OpenBSD CD-200", "testhost", "beta");
    m_xmlPropertyLoader.loadProperties(props, istr);
    assertEquals("1", props.getProperty("org.lockss.serialization.compatibilityMode"));

    // F T
    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions("1.12.3", "OpenBSD CD-200", "testhost", "dev");
    m_xmlPropertyLoader.loadProperties(props, istr);
    assertEquals("1", props.getProperty("org.lockss.serialization.compatibilityMode"));

    // T F
    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions("1.13.1", "OpenBSD CD-200", "testhost", "beta");
    m_xmlPropertyLoader.loadProperties(props, istr);
    assertEquals("1", props.getProperty("org.lockss.serialization.compatibilityMode"));

    // This should be equivalent!!
    sb = new StringBuffer();
    sb.append("<lockss-config>\n");
    sb.append("  <property name=\"org.lockss\">\n");
    sb.append("    <if>\n");
    sb.append("      <not>\n");
    sb.append("        <and>\n");
    sb.append("          <test daemonVersionMin=\"1.13.0\"/>\n");
    sb.append("          <test group=\"dev\"/>\n");
    sb.append("        </and>\n");
    sb.append("      </not>\n");
    sb.append("      <then>\n");
    sb.append("        <property name=\"serialization.compatibilityMode\" value=\"1\" />\n");
    sb.append("      </then>\n");
    sb.append("    </if>\n");
    sb.append("  </property>\n");
    sb.append("</lockss-config>\n");

    // T T
    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions("1.13.1", "OpenBSD CD-200", "testhost", "dev");
    m_xmlPropertyLoader.loadProperties(props, istr);
    assertNull(props.getProperty("org.lockss.serialization.compatibilityMode"));

    // F F
    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions("1.12.3", "OpenBSD CD-200", "testhost", "beta");
    m_xmlPropertyLoader.loadProperties(props, istr);
    assertEquals("1", props.getProperty("org.lockss.serialization.compatibilityMode"));

    // F T
    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions("1.12.3", "OpenBSD CD-200", "testhost", "dev");
    m_xmlPropertyLoader.loadProperties(props, istr);
    assertEquals("1", props.getProperty("org.lockss.serialization.compatibilityMode"));

    // T F
    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions("1.13.1", "OpenBSD CD-200", "testhost", "beta");
    m_xmlPropertyLoader.loadProperties(props, istr);
    assertEquals("1", props.getProperty("org.lockss.serialization.compatibilityMode"));
  }
  
  public void testExplicitAndCombinatorics() throws Exception {
    PropertyTree props;
    InputStream istr;
    StringBuffer sb;

    sb = new StringBuffer();
    sb.append("<lockss-config>\n");
    sb.append("  <if>\n");
    sb.append("    <and>\n");
    sb.append("      <test platformVersion=\"200\" />\n");
    sb.append("      <test group=\"dev\" />\n");
    sb.append("    </and>\n");
    sb.append("   <then>\n");
    sb.append("      <property name=\"test\" value=\"foo\" />\n");
    sb.append("    </then>\n");
    sb.append("    <else>\n");
    sb.append("      <property name=\"test\" value=\"bar\" />\n");
    sb.append("    </else>\n");
    sb.append("  </if>\n");
    sb.append("</lockss-config>\n");

    // T T
    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions("1.13.1", "OpenBSD CD-200", "testhost", "dev");
    m_xmlPropertyLoader.loadProperties(props, istr);
    assertEquals("foo", props.getProperty("test"));
    
    // T F
    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions("1.13.1", "OpenBSD CD-200", "testhost", "beta");
    m_xmlPropertyLoader.loadProperties(props, istr);
    assertEquals("bar", props.getProperty("test"));
    
    // F T
    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions("1.13.1", "OpenBSD CD-500", "testhost", "dev");
    m_xmlPropertyLoader.loadProperties(props, istr);
    assertEquals("bar", props.getProperty("test"));
    
    // F F
    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions("1.13.1", "OpenBSD CD-500", "testhost", "beta");
    m_xmlPropertyLoader.loadProperties(props, istr);
    assertEquals("bar", props.getProperty("test"));
  }
  
  public void testImplicitAndCombinatorics() throws Exception {
    PropertyTree props;
    InputStream istr;
    StringBuffer sb;

    sb = new StringBuffer();
    sb.append("<lockss-config>\n");
    sb.append("  <if>\n");
    sb.append("    <test platformVersion=\"200\" />\n");
    sb.append("    <test group=\"dev\" />\n");
    sb.append("    <then>\n");
    sb.append("      <property name=\"test\" value=\"foo\" />\n");
    sb.append("    </then>\n");
    sb.append("    <else>\n");
    sb.append("      <property name=\"test\" value=\"bar\" />\n");
    sb.append("    </else>\n");
    sb.append("  </if>\n");
    sb.append("</lockss-config>\n");

    // T T
    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions("1.13.1", "OpenBSD CD-200", "testhost", "dev");
    m_xmlPropertyLoader.loadProperties(props, istr);
    assertEquals("foo", props.getProperty("test"));
    
    // T F
    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions("1.13.1", "OpenBSD CD-200", "testhost", "beta");
    m_xmlPropertyLoader.loadProperties(props, istr);
    assertEquals("bar", props.getProperty("test"));
    
    // F T
    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions("1.13.1", "OpenBSD CD-500", "testhost", "dev");
    m_xmlPropertyLoader.loadProperties(props, istr);
    assertEquals("bar", props.getProperty("test"));
    
    // F F
    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions("1.13.1", "OpenBSD CD-500", "testhost", "beta");
    m_xmlPropertyLoader.loadProperties(props, istr);
    assertEquals("bar", props.getProperty("test"));
  }
  
  public void testOrCombinatorics() throws Exception {
    PropertyTree props;
    InputStream istr;
    StringBuffer sb;

    sb = new StringBuffer();
    sb.append("<lockss-config>\n");
    sb.append("  <if>\n");
    sb.append("    <or>\n");
    sb.append("      <test platformVersion=\"200\" />\n");
    sb.append("      <test group=\"dev\" />\n");
    sb.append("    </or>\n");
    sb.append("    <then>\n");
    sb.append("      <property name=\"test\" value=\"foo\" />\n");
    sb.append("    </then>\n");
    sb.append("    <else>\n");
    sb.append("      <property name=\"test\" value=\"bar\" />\n");
    sb.append("    </else>\n");
    sb.append("  </if>\n");
    sb.append("</lockss-config>\n");

    // T T
    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions("1.13.1", "OpenBSD CD-200", "testhost", "dev");
    m_xmlPropertyLoader.loadProperties(props, istr);
    assertEquals("foo", props.getProperty("test"));
    
    // T F
    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions("1.13.1", "OpenBSD CD-200", "testhost", "beta");
    m_xmlPropertyLoader.loadProperties(props, istr);
    assertEquals("foo", props.getProperty("test"));
    
    // F T
    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions("1.13.1", "OpenBSD CD-500", "testhost", "dev");
    m_xmlPropertyLoader.loadProperties(props, istr);
    assertEquals("foo", props.getProperty("test"));
    
    // F F
    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions("1.13.1", "OpenBSD CD-500", "testhost", "beta");
    m_xmlPropertyLoader.loadProperties(props, istr);
    assertEquals("bar", props.getProperty("test"));
  }
  
  public void testSimpleNot() throws Exception {
    PropertyTree props;
    InputStream istr;
    StringBuffer sb;

    sb = new StringBuffer();
    sb.append("<lockss-config>\n");
    sb.append("  <if>\n");
    sb.append("    <not>\n");
    sb.append("      <test platformVersion=\"200\" />\n");
    sb.append("    </not>\n");
    sb.append("    <then>\n");
    sb.append("      <property name=\"test\" value=\"foo\" />\n");
    sb.append("    </then>\n");
    sb.append("    <else>\n");
    sb.append("      <property name=\"test\" value=\"bar\" />\n");
    sb.append("    </else>\n");
    sb.append("  </if>\n");
    sb.append("</lockss-config>\n");
    
    // F
    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions("1.13.1", "OpenBSD CD-500", "testhost", "beta");
    m_xmlPropertyLoader.loadProperties(props, istr);
    assertEquals("foo", props.getProperty("test"));
    
    // T
    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions("1.13.1", "OpenBSD CD-200", "testhost", "beta");
    m_xmlPropertyLoader.loadProperties(props, istr);
    assertEquals("bar", props.getProperty("test"));
  }
  
  public void testNotWithAnd() throws Exception {
    PropertyTree props;
    InputStream istr;
    StringBuffer sb;

    sb = new StringBuffer();
    sb.append("<lockss-config>\n");
    sb.append("  <if>\n");
    sb.append("    <not>\n");
    sb.append("      <and>\n");
    sb.append("        <test platformVersion=\"200\" />\n");
    sb.append("        <test group=\"dev\" />\n");
    sb.append("      </and>\n");
    sb.append("    </not>\n");
    sb.append("    <then>\n");
    sb.append("      <property name=\"test\" value=\"foo\" />\n");
    sb.append("    </then>\n");
    sb.append("    <else>\n");
    sb.append("      <property name=\"test\" value=\"bar\" />\n");
    sb.append("    </else>\n");
    sb.append("  </if>\n");
    sb.append("</lockss-config>\n");
    
    // T T
    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions("1.13.1", "OpenBSD CD-200", "testhost", "dev");
    m_xmlPropertyLoader.loadProperties(props, istr);
    assertEquals("bar", props.getProperty("test"));
    
    // T F
    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions("1.13.1", "OpenBSD CD-200", "testhost", "beta");
    m_xmlPropertyLoader.loadProperties(props, istr);
    assertEquals("foo", props.getProperty("test"));
    
    // F T
    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions("1.13.1", "OpenBSD CD-500", "testhost", "dev");
    m_xmlPropertyLoader.loadProperties(props, istr);
    assertEquals("foo", props.getProperty("test"));
    
    // T F
    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions("1.13.1", "OpenBSD CD-500", "testhost", "beta");
    m_xmlPropertyLoader.loadProperties(props, istr);
    assertEquals("foo", props.getProperty("test"));
  }
  
  public void testNotWithOr() throws Exception {
    PropertyTree props;
    InputStream istr;
    StringBuffer sb;

    sb = new StringBuffer();
    sb.append("<lockss-config>\n");
    sb.append("  <if>\n");
    sb.append("    <not>\n");
    sb.append("      <or>\n");
    sb.append("        <test platformVersion=\"200\" />\n");
    sb.append("        <test group=\"dev\" />\n");
    sb.append("      </or>\n");
    sb.append("    </not>\n");
    sb.append("    <then>\n");
    sb.append("      <property name=\"test\" value=\"foo\" />\n");
    sb.append("    </then>\n");
    sb.append("    <else>\n");
    sb.append("      <property name=\"test\" value=\"bar\" />\n");
    sb.append("    </else>\n");
    sb.append("  </if>\n");
    sb.append("</lockss-config>\n");
    
    // T T
    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions("1.13.1", "OpenBSD CD-200", "testhost", "dev");
    m_xmlPropertyLoader.loadProperties(props, istr);
    assertEquals("bar", props.getProperty("test"));
    
    // T F
    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions("1.13.1", "OpenBSD CD-200", "testhost", "beta");
    m_xmlPropertyLoader.loadProperties(props, istr);
    assertEquals("bar", props.getProperty("test"));
    
    // F T
    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions("1.13.1", "OpenBSD CD-500", "testhost", "dev");
    m_xmlPropertyLoader.loadProperties(props, istr);
    assertEquals("bar", props.getProperty("test"));
    
    // T F
    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions("1.13.1", "OpenBSD CD-500", "testhost", "beta");
    m_xmlPropertyLoader.loadProperties(props, istr);
    assertEquals("foo", props.getProperty("test"));
  }
  
  /**
   * Test basic non-nested property getting from the static config.
   */
  public void testGet() throws Exception {
    assertEquals("foo", m_props.get("a"));
  }

  /**
   * Test a nested property.
   */
  public void testNestedGet() throws Exception {
    assertEquals("foo", m_props.get("b.c"));
  }

  /**
   * Test value tag (not in a list)
   */
  public void testValueTag() throws Exception {
    assertEquals("bar", m_props.get("d"));
  }

  /**
   * Test a non-existent property.
   */
  public void testNullValue() throws Exception {
    assertNull(m_props.get("this.prop.does.not.exist"));
  }

  /**
   * Test getting a list out of the config.
   */
  public void testGetList() throws Exception {
    String s = m_props.getProperty("org.lockss.d");
    assertNotNull(s);
    Vector<String> v = StringUtil.breakAt(s, ';', -1, true, true);
    Collections.sort(v);
    assertEquals("1", (String)v.get(0));
    assertEquals("2", (String)v.get(1));
    assertEquals("3", (String)v.get(2));
    assertEquals("4", (String)v.get(3));
    assertEquals("5", (String)v.get(4));
  }

  /**
   * This test is meant to validate the test 'testListEntities()',
   * which is only meaningful if the XML library currently being used
   * chooses to split character data chunks on entity boundaries
   * (&amp;amp;, &amp;nbsp;, and the like).  Our current library (as
   * of August 2004) exhibits this behavior.  This test will fail if
   * that ever becomes false, and if this test fails, then the
   * 'testListEntities()' test may or may not be meaningful.
   */
  public void testValidateListEntitiesTest() throws Exception {
    SAXParserFactory factory = SAXParserFactory.newInstance();

    factory.setValidating(false);
    factory.setNamespaceAware(false);

    SAXParser parser = factory.newSAXParser();

    StringBuffer sb = new StringBuffer();
    sb.append("<test>a&amp;b&amp;c&amp;d</test>");

    InputStream istr =
      new ReaderInputStream(new StringReader(sb.toString()));

    class TestHandler extends DefaultHandler {
      public int charCallCount = 0;

      public void characters(char[] ch, int start, int len) {
	charCallCount++;
      }
    }

    TestHandler handler = new TestHandler();

    parser.parse(istr, handler);

    // Should have been called 7 times, may be library
    // dependant:   a, &, b, &, c, &, d
    assertEquals(7, handler.charCallCount);
  }

  /**
   * Test to be sure that XML entities don't split list
   * entries.
   */
  public void testListEntities() throws Exception {
    String s = m_props.getProperty("org.lockss.listtest");
    assertNotNull(s);
    Vector v = StringUtil.breakAt(s, ';', -1, true, true);
    assertEquals(1, v.size());
    assertEquals("this&should&be&one&value", v.get(0));
  }

  public void testDaemonVersionEquals() throws Exception {
    assertNull(m_props.get("org.lockss.test.a"));
    assertEquals("foo", m_props.get("org.lockss.test.b"));
  }

  public void testDaemonVersionMax() throws Exception {
    assertNull(m_props.get("org.lockss.test.c"));
    assertEquals("foo", m_props.get("org.lockss.test.d"));
  }

  public void testDaemonVersionMin() throws Exception {
    assertEquals("foo", m_props.get("org.lockss.test.e"));
    assertNull(m_props.get("org.lockss.test.f"));

  }

  public void testDaemonVersionMaxAndMin() throws Exception {
    assertEquals("foo", m_props.get("org.lockss.test.g"));
    assertNull(m_props.get("org.lockss.test.h"));
  }

  public void testPlatformVersionEquals() throws Exception {
    assertEquals("foo", m_props.get("org.lockss.test.i"));
    assertNull(m_props.get("org.lockss.test.j"));

  }

  public void testPlatformVersionMax() throws Exception {
    assertNull(m_props.get("org.lockss.test.k"));
    assertEquals("foo", m_props.get("org.lockss.test.l"));
  }

  public void testPlatformVersionMin() throws Exception {
    assertEquals("foo", m_props.get("org.lockss.test.m"));
    assertNull(m_props.get("org.lockss.test.n"));
  }

  public void testPlatformVersionMinAndMax() throws Exception {
    assertEquals("foo", m_props.get("org.lockss.test.o"));
    assertNull(m_props.get("org.lockss.test.p"));
  }

  public void testGroupMembership() throws Exception {
    assertEquals("foo", m_props.get("org.lockss.test.q"));
    assertNull(m_props.get("org.lockss.test.r"));
  }

  public void testHostname() throws Exception {
    assertEquals("foo", m_props.get("org.lockss.test.s"));
    assertNull(m_props.get("org.lockss.test.t"));
  }

  public void testHostIP() throws Exception {
    assertEquals("foo", m_props.get("org.lockss.test.hostIP.true"));
    assertNull(m_props.get("org.lockss.test.hostIP.false"));
  }

  public void testHostMembership() throws Exception {
    assertNull(m_props.get("org.lockss.test.platformName.linux"));
    assertEquals("openbsd", m_props.get("org.lockss.test.platformName.openbsd"));
  }

  public void testUnknownContionalsAreFalse() throws Exception {
    assertNull(m_props.get("org.lockss.test.unknown.a"));
    assertEquals("bar", m_props.get("org.lockss.test.unknown.b"));
    assertNull(m_props.get("org.lockss.test.unknown.c"));
    assertNull(m_props.get("org.lockss.test.unknown.d"));
  }

  public void testThenElse() {
    assertEquals("foo", m_props.get("org.lockss.test.u"));
    assertEquals("bar", m_props.get("org.lockss.test.v"));
    assertEquals("openbsd", m_props.get("org.lockss.test.ifelse.platformName"));
  }

  public void testConditionalCombo() throws Exception {
    assertEquals("bar", m_props.get("org.lockss.test.w"));
    assertEquals("foo", m_props.get("org.lockss.test.x"));
  }

  public void testTestNonNested() throws Exception {
    assertEquals("bar", m_props.get("org.lockss.test.y"));
    assertEquals("foo", m_props.get("org.lockss.test.z"));
  }

  public void testBooleanAnd() throws Exception {
    assertEquals("foo", m_props.get("org.lockss.and.a"));
    assertNull(m_props.get("org.lockss.and.b"));
    assertEquals("foo", m_props.get("org.lockss.and.c"));
    assertNull(m_props.get("org.lockss.and.d"));
    assertEquals("bar", m_props.get("org.lockss.and.e"));
  }

  public void testBooleanOr() throws Exception {
    assertEquals("foo", m_props.get("org.lockss.or.a"));
    assertNull(m_props.get("org.lockss.or.b"));
    assertEquals("foo", m_props.get("org.lockss.or.c"));
    assertNull(m_props.get("org.lockss.or.d"));
    assertEquals("bar", m_props.get("org.lockss.or.e"));
  }

  public void testBooleanNot() throws Exception {
    assertEquals("foo", m_props.get("org.lockss.not.a"));
    assertNull(m_props.get("org.lockss.not.b"));
    assertEquals("foo", m_props.get("org.lockss.not.c"));
    assertNull(m_props.get("org.lockss.or.d"));
    assertEquals("bar", m_props.get("org.lockss.not.e"));
    assertEquals("foo", m_props.get("org.lockss.not.f"));
    assertEquals("foo", m_props.get("org.lockss.not.g"));
  }

  public void testNestedIfs() throws Exception {
    assertEquals("foo", m_props.get("org.lockss.nestedIf.a"));
    assertEquals("bar", m_props.get("org.lockss.nestedIf.b"));
    assertEquals("baz", m_props.get("org.lockss.nestedIf.c"));
    assertEquals("baz", m_props.get("org.lockss.nestedIf.d"));
    assertEquals("quux", m_props.get("org.lockss.nestedIf.e"));
    assertEquals(null, m_props.get("org.lockss.nestedIf.f.control"));
    assertEquals(null, m_props.get("org.lockss.nestedIf.f"));
  }

  public void testNestedBoolean() throws Exception {
    assertEquals("foo", m_props.get("org.lockss.nested.a"));
    assertEquals("foo", m_props.get("org.lockss.nested.aa"));
    assertEquals("bar", m_props.get("org.lockss.nested.ab"));
    assertEquals("bar", m_props.get("org.lockss.nested.ac"));
    assertEquals("foo", m_props.get("org.lockss.nested.ad"));
    assertEquals("bar", m_props.get("org.lockss.nested.b"));
    assertEquals("bar", m_props.get("org.lockss.nested.c"));
  }

  /**
   * If any of the internal system values (daemon version, platform
   * version, hostname, or group) are null, all tests that depend on
   * them should return false.
   */
  public void testNullHostname() throws Exception {
    setVersions(null, null, null, null);
    parseXmlProperties();
    assertNull(m_props.get("org.lockss.test.s"));
    assertNull(m_props.get("org.lockss.test.t"));
    assertNull(m_props.get("org.lockss.nulltest.a"));
    assertEquals("bar", m_props.get("org.lockss.nulltest.b"));
  }

  public void testNullGroup() throws Exception {
    setVersions(null, null, null, null);
    parseXmlProperties();
    assertNull(m_props.get("org.lockss.test.q"));
    assertNull(m_props.get("org.lockss.test.r"));
    assertNull(m_props.get("org.lockss.nulltest.c"));
    assertEquals("bar", m_props.get("org.lockss.nulltest.d"));
  }

  public void testNullDaemonVersion() throws Exception {
    setVersions(null, null, null, null);
    parseXmlProperties();
    assertNull(m_props.get("org.lockss.test.a"));
    assertNull(m_props.get("org.lockss.test.b"));
    assertNull(m_props.get("org.lockss.nulltest.e"));
    assertEquals("bar", m_props.get("org.lockss.nulltest.f"));
  }

  public void testNullPlatformVersion() throws Exception {
    setVersions(null, null, null, null);
    parseXmlProperties();
    assertNull(m_props.get("org.lockss.test.i"));
    assertNull(m_props.get("org.lockss.test.j"));
    assertNull(m_props.get("org.lockss.nulltest.g"));
    assertEquals("bar", m_props.get("org.lockss.nulltest.h"));
  }

  public void testServiceName() throws Exception {
    // No ServiceDescr
    setVersions(null, null, null, null);
    parseXmlProperties();
    assertEquals("another service",
		 m_props.get("org.lockss.test.serviceName.foo"));
    assertEquals("not service 1",
		 m_props.get("org.lockss.test.serviceAbbrev.foo"));

    // Both name and abbrev false
    setVersions(null, null, null, null, "Name xxx", "Abbrevxxx");
    parseXmlProperties();
    assertEquals("another service",
		 m_props.get("org.lockss.test.serviceName.foo"));
    assertEquals("not service 1",
		 m_props.get("org.lockss.test.serviceAbbrev.foo"));

    // name true, abbrev false
    setVersions(null, null, null, null, "Rest Service 1", "Abbrevxxx");
    parseXmlProperties();
    assertEquals("the service",
		 m_props.get("org.lockss.test.serviceName.foo"));
    assertEquals("not service 1",
		 m_props.get("org.lockss.test.serviceAbbrev.foo"));

    // both true
    setVersions(null, null, null, null, "Rest Service 1", "sabbrev");
    parseXmlProperties();
    assertEquals("the service",
		 m_props.get("org.lockss.test.serviceName.foo"));
    assertEquals("service 1",
		 m_props.get("org.lockss.test.serviceAbbrev.foo"));
}

  /* The following test comes from Issue 2790.  Here's the bug:

     This construct resulted in machines in the test group (which are not
     in the prod group) not getting any of the props in the <then> clause.

    <if>
      <and>
        <test group="test" />
        <not> <test group="prod" /> </not>
      </and>

      <then>
        <property name="id.initialV3PeerList">
        ...
      </then>
    </if>
  */

  public void testNotWithAnd2() throws Exception {
    PropertyTree props;
    InputStream istr;
    StringBuffer sb;

    sb = new StringBuffer();
    sb.append("<if>\n");
    sb.append("  <and>\n");
    sb.append("    <test group=\"test\" />\n");
    sb.append("    <not> <test group=\"prod\" /> </not>\n");
    sb.append("  </and>\n");
    sb.append("  <then>\n");
    sb.append("    <property name=\"result\" value=\"foo\" />\n");
    sb.append("  </then>\n");
    sb.append("  <else>\n");
    sb.append("    <property name=\"result\" value=\"bar\" />\n");
    sb.append("  </else>\n");
    sb.append("</if>\n");

    // T
    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions(null, null, null, "test");
    m_xmlPropertyLoader.loadProperties(props, istr);
    assertEquals("foo", props.getProperty("result"));

    // F, type 1
    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions(null, null, null, "prod");
    m_xmlPropertyLoader.loadProperties(props, istr);
    assertEquals("bar", props.getProperty("result"));

    // F, type 2
    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions(null, null, null, "experimental");
    m_xmlPropertyLoader.loadProperties(props, istr);
    assertEquals("bar", props.getProperty("result"));
  }

  public void testEmptyList() throws Exception {
    PropertyTree props = new PropertyTree();
    InputStream istr;
    String s;
    StringBuilder sb;
    Vector<String> vs;

    sb = new StringBuilder();
    sb.append("<lockss-config>\n");
    sb.append("<property name=\"emptyList\">");
    sb.append("  <list>");
    sb.append("  </list>");
    sb.append("</property>");
    sb.append("</lockss-config>\n");

    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions(null, null, null, "test");
    m_xmlPropertyLoader.loadProperties(props, istr);

    s = props.getProperty("emptyList");
    assertEquals("", s);
    List<String> lst = StringUtil.breakAt(s, ';', -1, true, true);
    assertEmpty(lst);
  }

  public void testEmptyConfig() throws Exception {
    PropertyTree props = new PropertyTree();
    InputStream istr;
    String s;
    StringBuilder sb;
    Vector<String> vs;

    sb = new StringBuilder();
    sb.append("<lockss-config>\n");
    sb.append("</lockss-config>\n");

    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions(null, null, null, "test");
    m_xmlPropertyLoader.loadProperties(props, istr);

    assertEmpty(props);
  }

  /**
   * This is a negative test: We should not accept "<else>" without a "<then>", or
   * a "<then>" after an "<else>".
   */
  public void testElseBeforeThen() throws Exception {
    PropertyTree props;
    InputStream istr;
    StringBuffer sb;

    sb = new StringBuffer();
    sb.append("<if>\n");
    sb.append("  <test group=\"test\" />\n");
    sb.append("  <else>\n");
    sb.append("    <property name=\"result\" value=\"bar\" />\n");
    sb.append("  </else>\n");
    sb.append("  <then>\n");
    sb.append("    <property name=\"result\" value=\"foo\" />\n");
    sb.append("  </then>\n");
    sb.append("</if>\n");

    // T
    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions(null, null, null, "test");
    try {
      m_xmlPropertyLoader.loadProperties(props, istr);
      fail("An else before a then should cause an exception.");
    } catch (IllegalArgumentException e) {
      /*  Passes test */
    }
  }
  
  /**
   * Bug 2798: Add add-to-list functionality to XmlPropertyLoader.
   * 
   * XML read by XmlPropertyLoader.LockssConfigHandler includes the <list> ... 
   * </list> tag. When put inside a <property name="...">, it stores the list in
   * the property.
   * 
   * Tom would like XML files to be able to add lists in multiple
   * points. The following new action would be allowed:
   * 
   *   <property name="d">
   *      <list>
   *        <value>1</value>
   *        <value>2</value>
   *        <value>3</value>
   *        <value>4</value>
   *        <value>5</value>
   *      </list>
   *   </property>
   *
   *     . . . 
   *
   *   <property name="d">
   *      <list append="true">
   *        <value>6</value>
   *        <value>7</value>
   *        <value>8</value>
   *      </list>
   *  </property>
   */
  public void testListAppend() throws Exception {
    PropertyTree props = new PropertyTree();
    InputStream istr;
    String s;
    StringBuilder sb;
    Vector<String> vs;
    
    // Test: list with append adds the element to the series.
    sb = new StringBuilder();
    sb.append("<lockss-config>\n");    
    sb.append("<property name=\"listAppend\">");
    // Original list
    sb.append("  <list>");
    sb.append("    <value>1</value>");
    sb.append("    <value>2</value>");
    sb.append("  </list>");
    // Appended list
    sb.append("  <list append=\"true\">");
    sb.append("    <value>3</value>");
    sb.append("    <value>4</value>");
    sb.append("  </list>");
    sb.append("</property>");
    sb.append("</lockss-config>\n");    
    
    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions(null, null, null, "test");
    m_xmlPropertyLoader.loadProperties(props, istr);
    
    // Get the elements in listAppend.
    s = props.getProperty("listAppend");
    assertNotNull(s);
    vs = StringUtil.breakAt(s, ';', -1, true, true);
    
    // Verify that the original elements are present.
    assertTrue(vs.contains("1"));
    assertTrue(vs.contains("2"));
    
    // Verify that the added elements are present.
    assertTrue(vs.contains("3"));
    assertTrue(vs.contains("4"));
    
    
    // Test: list with append adds the element to the series, with a break in the middle.
    sb = new StringBuilder();
    // Original list
    sb.append("<lockss-config>\n");    
    sb.append("<property name=\"listAppend\">");
    sb.append("  <list>");
    sb.append("    <value>1</value>");
    sb.append("    <value>2</value>");
    sb.append("  </list>");
    sb.append("</property>");
    // Break in the middle.
    sb.append("<property name=\"foobar\">");
    sb.append("  <list>");
    sb.append("    <value>yarf</value>");
    sb.append("    <value>yip</value>");
    sb.append("  </list>");
    sb.append("</property>");   
    // Appended list
    sb.append("<property name=\"listAppend\">");
    sb.append("  <list append=\"true\">");
    sb.append("    <value>3</value>");
    sb.append("    <value>4</value>");
    sb.append("  </list>");
    sb.append("</property>");
    sb.append("</lockss-config>\n");

    
    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions(null, null, null, "test");
    m_xmlPropertyLoader.loadProperties(props, istr);
    
    // Get the elements in listAppend.
    s = props.getProperty("listAppend");
    assertNotNull(s);
    vs = StringUtil.breakAt(s, ';', -1, true, true);
    
    // Verify that the original elements are present.
    assertTrue(vs.contains("1"));
    assertTrue(vs.contains("2"));
    
    // Verify that the added elements are present.
    assertTrue(vs.contains("3"));
    assertTrue(vs.contains("4"));
    
    // Verify that the interrupting elements are NOT present.
    assertFalse(vs.contains("yarf"));
    assertFalse(vs.contains("yip"));

    
    // Test: Lists can have multiple appends
    sb = new StringBuilder();
    
    sb.append("<lockss-config>\n");    
    sb.append("<property name=\"listAppend\">");
    // Original list
    sb.append("  <list>");
    sb.append("    <value>1</value>");
    sb.append("    <value>2</value>");
    sb.append("  </list>");
    // Appended list 1
    sb.append("  <list append=\"true\">");
    sb.append("    <value>3</value>");
    sb.append("    <value>4</value>");
    sb.append("    <value>5</value>");
    sb.append("  </list>");
    // Appended list 2
    sb.append("  <list append=\"true\">");
    sb.append("    <value>6</value>");
    sb.append("  </list>");
    // Appended list 3
    sb.append("  <list append=\"true\">");
    sb.append("    <value>7</value>");
    sb.append("    <value>8</value>");
    sb.append("    <value>9</value>");
    sb.append("    <value>10</value>");
    sb.append("  </list>");
    sb.append("</property>");
    sb.append("</lockss-config>\n");    

    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions(null, null, null, "test");
    m_xmlPropertyLoader.loadProperties(props, istr);
    
    // Get the elements in listAppend.
    s = props.getProperty("listAppend");
    assertNotNull(s);
    vs = StringUtil.breakAt(s, ';', -1, true, true);

    // Verify that original list is present.
    assertTrue(vs.contains("1"));
    assertTrue(vs.contains("2"));
    
    // Verify that appended list 1 is present.
    assertTrue(vs.contains("3"));
    assertTrue(vs.contains("4"));
    assertTrue(vs.contains("5"));

    // Verify that appended list 2 is present.
    assertTrue(vs.contains("6"));
    
    // Verify that appended list 3 is present.
    assertTrue(vs.contains("7"));
    assertTrue(vs.contains("8"));
    assertTrue(vs.contains("9"));
    assertTrue(vs.contains("10"));
    
    
    // Test: Lists can have multiple appends, with breaks...
    sb = new StringBuilder();
    
    sb.append("<lockss-config>\n");    
    // Original list
    sb.append("<property name=\"listAppend\">");
    sb.append("  <list>");
    sb.append("    <value>1</value>");
    sb.append("    <value>2</value>");
    sb.append("  </list>");
    sb.append("</property>");
    // Interrupting list
    sb.append("<property name=\"interrupt\">");
    sb.append("  <list>");
    sb.append("    <value>a</value>");
    sb.append("    <value>b</value>");
    sb.append("  </list>");
    sb.append("</property>");
    // Appended list 1
    sb.append("<property name=\"listAppend\">");
    sb.append("  <list append=\"true\">");
    sb.append("    <value>3</value>");
    sb.append("    <value>4</value>");
    sb.append("    <value>5</value>");
    sb.append("  </list>");
    sb.append("</property>");
    // Interrupting list with appends.
    sb.append("<property name=\"interrupt\">");
    sb.append("  <list append=\"true\">");
    sb.append("    <value>c</value>");
    sb.append("    <value>d</value>");
    sb.append("  </list>");
    sb.append("</property>");
    // Appended list 2
    sb.append("<property name=\"listAppend\">");
    sb.append("  <list append=\"true\">");
    sb.append("    <value>6</value>");
    sb.append("  </list>");
    sb.append("</property>");
    // Different interrupting list.
    sb.append("<property name=\"anotherInterrupt\">");
    sb.append("  <list>");
    sb.append("    <value>foo</value>");
    sb.append("    <value>bar</value>");
    sb.append("  </list>");
    sb.append("</property>");
    // Appended list 3
    sb.append("<property name=\"listAppend\">");
    sb.append("  <list append=\"true\">");
    sb.append("    <value>7</value>");
    sb.append("    <value>8</value>");
    sb.append("    <value>9</value>");
    sb.append("    <value>10</value>");
    sb.append("  </list>");
    sb.append("</property>");
    sb.append("</lockss-config>\n");    

    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions(null, null, null, "test");
    m_xmlPropertyLoader.loadProperties(props, istr);

    // Get the elements in listAppend.
    s = props.getProperty("listAppend");
    assertNotNull(s);
    vs = StringUtil.breakAt(s, ';', -1, true, true);

    // Verify that original list is present.
    assertTrue(vs.contains("1"));
    assertTrue(vs.contains("2"));
    
    // Verify that appended list 1 is present.
    assertTrue(vs.contains("3"));
    assertTrue(vs.contains("4"));
    assertTrue(vs.contains("5"));

    // Verify that appended list 2 is present.
    assertTrue(vs.contains("6"));
    
    // Verify that appended list 3 is present.
    assertTrue(vs.contains("7"));
    assertTrue(vs.contains("8"));
    assertTrue(vs.contains("9"));
    assertTrue(vs.contains("10"));
    
    // Verify that interrupt is NOT present.
    assertFalse(vs.contains("a"));
    assertFalse(vs.contains("b"));
    assertFalse(vs.contains("c"));
    assertFalse(vs.contains("d"));
    
    // Verify that the other interrupting list is NOT present.
    assertFalse(vs.contains("foo"));
    assertFalse(vs.contains("bar"));
    
    
    // Test: appending to a non-existent list causes an error.
    sb = new StringBuilder();
    sb.append("<lockss-config>\n");    
    sb.append("<property name=\"listAppend\">");
    // Append to non-existent list.
    sb.append("  <list append=\"true\">");
    sb.append("    <value>1</value>");
    sb.append("    <value>2</value>");
    sb.append("  </list>");
    sb.append("</property>");
    sb.append("</lockss-config>\n");    

    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions(null, null, null, "test");
    
    try {
      m_xmlPropertyLoader.loadProperties(props, istr);
      fail("Appending to a non-existent list should have caused an error.");
    } catch (IllegalArgumentException e) {
      /* Passes test */
    }
    
    // Test: "append=false" causes a new list to be created.
    sb = new StringBuilder();
    sb.append("<lockss-config>\n");    
    sb.append("<property name=\"listAppend\">");
    // Original list
    sb.append("  <list>");
    sb.append("    <value>1</value>");
    sb.append("    <value>2</value>");
    sb.append("  </list>");
    // NOT-Appended list
    sb.append("  <list append=\"false\">");
    sb.append("    <value>3</value>");
    sb.append("    <value>4</value>");
    sb.append("  </list>");
    sb.append("</property>");
    sb.append("</lockss-config>\n");    

    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions(null, null, null, "test");
    m_xmlPropertyLoader.loadProperties(props, istr);

    // Get the elements in listAppend.
    s = props.getProperty("listAppend");
    assertNotNull(s);
    vs = StringUtil.breakAt(s, ';', -1, true, true);

    // Verify that the original list is not present.
    assertFalse(vs.contains("1"));
    assertFalse(vs.contains("2"));
    
    // Verify that the added elements are present.
    assertTrue(vs.contains("3"));
    assertTrue(vs.contains("4"));
    
    
    // Test: without "append=true", a new list is created.
    sb = new StringBuilder();
    sb.append("<lockss-config>\n");    
    sb.append("<property name=\"listAppend\">");
    // Original list
    sb.append("  <list>");
    sb.append("    <value>1</value>");
    sb.append("    <value>2</value>");
    sb.append("  </list>");
    // NOT-Appended list
    sb.append("  <list>");
    sb.append("    <value>3</value>");
    sb.append("    <value>4</value>");
    sb.append("  </list>");
    sb.append("</property>");
    sb.append("</lockss-config>\n");    

    props = new PropertyTree();
    istr = new ReaderInputStream(new StringReader(sb.toString()));
    setVersions(null, null, null, "test");
    m_xmlPropertyLoader.loadProperties(props, istr);
    
    // Get the elements in listAppend.
    s = props.getProperty("listAppend");
    assertNotNull(s);
    vs = StringUtil.breakAt(s, ';', -1, true, true);

    // Verify that the original list is not present.
    assertFalse(vs.contains("1"));
    assertFalse(vs.contains("2"));
    
    // Verify that the added elements are present.
    assertTrue(vs.contains("3"));
    assertTrue(vs.contains("4"));
  }

  /**
   * Test loading title database information.
   */
  public void testLoadTdb() throws Exception {
    StringBuffer sb = new StringBuffer();
    sb.append("<lockss-config>\n");
    sb.append("  <property name=\"org.lockss\">\n");
    sb.append("    <property name=\"title\">\n");
    sb.append("      <property name=\"BioOneTheAmericanMidlandNaturalist139\">\n");
    sb.append("        <property name=\"attributes.publisher\" value=\"University of Notre Dame\" />\n");
    sb.append("        <property name=\"issn\" value=\"0003-0031\" />\n");
    sb.append("        <property name=\"journalTitle\" value=\"The American Midland Naturalist\" />\n");
    sb.append("        <property name=\"journalId\" value=\"0003-0031\" />\n");
    sb.append("        <property name=\"title\" value=\"The American Midland Naturalist Volume 139\" />\n");
    sb.append("        <property name=\"plugin\" value=\"org.lockss.plugin.bioone.BioOnePlugin\" />\n");
    sb.append("        <property name=\"param.1\">\n");
    sb.append("          <property name=\"key\" value=\"base_url\" />\n");
    sb.append("          <property name=\"value\" value=\"http://www.bioone.org/\" />\n");
    sb.append("        </property>\n");
    sb.append("        <property name=\"param.2\">\n");
    sb.append("          <property name=\"key\" value=\"journal_id\" />\n");
    sb.append("          <property name=\"value\" value=\"0003-0031\" />\n");
    sb.append("        </property>\n");
    sb.append("        <property name=\"param.3\">\n");
    sb.append("          <property name=\"key\" value=\"volume\" />\n");
    sb.append("          <property name=\"value\" value=\"139\" />\n");
    sb.append("        </property>\n");
    sb.append("        <property name=\"param.4\">\n");
    sb.append("          <property name=\"key\" value=\"pub_down\" />\n");
    sb.append("          <property name=\"value\" value=\"true\" />\n");
    sb.append("        </property>\n");
    sb.append("      </property>\n");
    sb.append("    </property>\n");
    sb.append("  </property>\n");
    sb.append("</lockss-config>\n");

    InputStream istr =
      new ReaderInputStream(new StringReader(sb.toString()));
    PropertyTree props = new PropertyTree();
    m_xmlPropertyLoader.loadProperties(props, istr);
    assertFalse(props.isEmpty());
    Object prop = props.get("org.lockss.title.BioOneTheAmericanMidlandNaturalist139.param.3.value");
    assertNotNull(prop);
    assertEquals("139", prop);

    istr = new ReaderInputStream(new StringReader(sb.toString()));
    props = new PropertyTree();
    Tdb tdb = new Tdb();
    m_xmlPropertyLoader.loadProperties(props, tdb, istr);
    assertTrue(props.isEmpty());
    assertFalse(tdb.isEmpty());
    TdbPublisher pub = tdb.getTdbPublisher("University of Notre Dame");
    assertNotNull(pub);
    TdbTitle title = pub.getTdbTitleById("0003-0031");
    assertNotNull(title);
    Collection<TdbAu> aus = title.getTdbAusByName("The American Midland Naturalist Volume 139");
    assertEquals(1, aus.size());
    String volume = aus.iterator().next().getParam("volume");
    assertEquals("139", volume);
  }
  /**
   * Set default values for testing conditionals.
   */
  private void setDefaultVersions() {
    m_xmlPropertyLoader
      .setDaemonVersion("1.2.8")
      .setPlatformVersion("OpenBSD CD-135")
      .setPlatformHostname("testhost")
      .setPlatformGroups("beta")
      .setPlatformHostIP("11.22.33.44");
  }

  /**
   * Convenience method for overriding conditional values.
   */
  private void setVersions(String daemonVersion, String platformVersion,
			   String hostname, String group) {
    m_xmlPropertyLoader
      .setDaemonVersion(daemonVersion)
      .setPlatformVersion(platformVersion)
      .setPlatformHostname(hostname)
      .setPlatformGroups(group);
  }

  private void setVersions(String daemonVersion, String platformVersion,
			   String hostname, String group,
			   String serviceName, String serviceAbbrev) {
    m_xmlPropertyLoader.setVersions(daemonVersion, platformVersion,
				    hostname, group);
    if (serviceName != null || serviceAbbrev != null) {
      m_xmlPropertyLoader.setServiceDescr(new ServiceDescr(serviceName,
							   serviceAbbrev));
    }
  }

}
