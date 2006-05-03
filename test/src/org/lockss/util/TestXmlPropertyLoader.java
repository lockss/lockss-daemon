/*
 * $Id: TestXmlPropertyLoader.java,v 1.19 2006-05-03 03:22:42 smorabito Exp $
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

package org.lockss.util;

import java.util.*;
import java.io.*;
import java.net.URL;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import org.lockss.test.*;

/**
 * Test class for <code>org.lockss.util.XmlPropertyLoader</code> and
 * <code>org.lockss.util.LockssConfigHandler</code>.
 */

public class TestXmlPropertyLoader extends LockssTestCase {

  private PropertyTree m_props = null;
  private XmlPropertyLoader m_xmlPropertyLoader = null;

  public static Class testedClasses[] = {
    org.lockss.util.XmlPropertyLoader.class
  };

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
    URL url = getClass().getResource(file);
    assertNotNull(file + " missing.", url);
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
    Vector v = StringUtil.breakAt(s, ';', -1, true, true);
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
    PropertyTree props = new PropertyTree();
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

  public void testHostnameMembership() throws Exception {
    assertEquals("foo", m_props.get("org.lockss.test.s"));
    assertNull(m_props.get("org.lockss.test.t"));
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

  /**
   * Set default values for testing conditionals.
   */
  private void setDefaultVersions() {
    ((MockXmlPropertyLoader)m_xmlPropertyLoader).setVersions("1.2.8", "OpenBSD CD-135", "testhost", "beta");
  }

  /**
   * Convenience method for overriding conditional values.
   */
  private void setVersions(String daemonVersion, String platformVersion,
			   String hostname, String group) {
    ((MockXmlPropertyLoader)m_xmlPropertyLoader).setVersions(daemonVersion, platformVersion, hostname, group);
  }

}
