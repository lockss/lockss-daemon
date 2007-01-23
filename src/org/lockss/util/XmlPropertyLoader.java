/*
 * $Id: XmlPropertyLoader.java,v 1.29 2007-01-23 21:44:36 smorabito Exp $
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

import java.io.*;
import java.util.*;

import javax.xml.parsers.*;

import org.lockss.config.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

public class XmlPropertyLoader {

  public static final char PROPERTY_SEPARATOR = '.';

  private static final String TAG_LOCKSSCONFIG = "lockss-config";
  private static final String TAG_PROPERTY     = "property";
  private static final String TAG_VALUE        = "value";
  private static final String TAG_LIST         = "list";
  private static final String TAG_IF           = "if";
  private static final String TAG_THEN         = "then";
  private static final String TAG_ELSE         = "else";
  private static final String TAG_AND          = "and";
  private static final String TAG_OR           = "or";
  private static final String TAG_NOT          = "not";
  private static final String TAG_TEST         = "test";

  private static final Set conditionals =
    SetUtil.fromArray(new String[] {
      "group", "hostname", "daemonVersion", "daemonVersionMin",
      "daemonVersionMax", "platformName", "platformVersion",
      "platformVersionMin", "platformVersionMax"
    });

  private static XmlPropertyLoader m_instance = null;

  private static Logger log = Logger.getLogger("XmlPropertyLoader");

  public static void load(PropertyTree props, InputStream istr)
      throws ParserConfigurationException, SAXException, IOException {
    if (m_instance == null) {
      m_instance = new XmlPropertyLoader();
    }

    m_instance.loadProperties(props, istr);
  }

  /**
   * Load a set of XML properties from the input stream.
   */
  void loadProperties(PropertyTree props, InputStream istr)
      throws ParserConfigurationException, SAXException, IOException {

    SAXParserFactory factory = SAXParserFactory.newInstance();

    factory.setValidating(true);
    factory.setNamespaceAware(false);

    SAXParser parser = factory.newSAXParser();

    parser.parse(istr, new LockssConfigHandler(props));
  }

  public Version getDaemonVersion() {
    return ConfigManager.getDaemonVersion();
  }

  public PlatformVersion getPlatformVersion() {
    return ConfigManager.getPlatformVersion();
  }

  public String getPlatformHostname() {
    return ConfigManager.getPlatformHostname();
  }

  public String getPlatformGroup() {
    return ConfigManager.getPlatformGroup();
  }

  /**
   * Representation of the currently nested 'if'.
   */
  private static final class If {
    public boolean inThen = false;
    public boolean inElse = false;
    public boolean evalIf = true;
  }

  /**
   * SAX parser handler.
   */
  class LockssConfigHandler extends DefaultHandler {

    // Simple stack that helps us know what our current level in
    // the property tree is.
    private Stack m_propStack = new Stack();

    // Stack of conditionals being evaluated.  Empty if not inside
    // a conditional statement.
    private Stack m_condStack = new Stack();

    // Stack of if's (and corresponding 'then' and 'else' blocks, if
    // present) being evaluated.
    private Stack m_ifStack = new Stack();

    // The state of the current test.  i.e.:
    private boolean m_testEval = true;

    // When building a list of configuration values for a single key.
    private List m_propList = null;

    // True iff the parser is currently inside a "value" element.
    private boolean m_inValue = false;
    // True iff the parser is currently inside a "list" element.
    private boolean m_inList  = false;

    // The property tree we're adding to.
    private PropertyTree m_props;

    // A stringbuffer to hold current character data until it's all
    // been read.
    private StringBuffer m_charBuffer;

    // Save the current running daemon and platform version
    private PlatformVersion m_sysPlatformVer;
    private Version m_sysDaemonVer;
    private String m_sysPlatformName;
    private String m_sysGroup;
    private String m_sysHostname;

    /**
     * Default constructor.
     */
    public LockssConfigHandler(PropertyTree props) {
      super();
      // Conditionals
      m_sysPlatformVer = getPlatformVersion();
      m_sysDaemonVer = getDaemonVersion();
      if (m_sysPlatformVer != null) {
	m_sysPlatformName = m_sysPlatformVer.getName();
      }
      m_sysGroup = getPlatformGroup();
      m_sysHostname = getPlatformHostname();

      m_props = props;
      log.debug2("Conditionals: {platformVer=" + m_sysPlatformVer + "}, " +
		 "{daemonVer=" + m_sysDaemonVer + "}, " +
		 "{group=" + m_sysGroup + "}, " +
		 "{hostname=" + m_sysHostname + "}, " +
		 "{platformName=" + m_sysPlatformName + "}");
    }

    /**
     * <p>Evaluate this property iff:</p>
     * <ol>
     *   <li>We're not in a propgroup conditional, or</li>
     *   <li>We're in a propgroup that we eval to true AND we're
     *      in a &lt;then&gt;</li>
     *   <li>We're in a propgroup that we eval to false AND we're
     *      in an &lt;else&gt;</li>
     *   <li>We're in a propgroup that we eval to true AND we're
     *      in neither  a &lt;then&gt; or an &lt;else&gt;</li>
     * </ol>
     */
    private boolean doEval() {
      if (m_ifStack.empty()) {
        return true;
      }
      If curIf = (If)m_ifStack.peek();
      return ((curIf.evalIf && curIf.inThen) ||
          (!curIf.evalIf && curIf.inElse) ||
          (curIf.evalIf && !curIf.inThen && !curIf.inElse));
    }

    /**
     * Handle the starting tags of elements.  Based on the name of the
     * tag, call the appropriate handler method.
     */
    public void startElement(String namespaceURI, String localName,
			     String qName, Attributes attrs)
	throws SAXException {

      if (TAG_IF.equals(qName)) {
	startIfTag(attrs);
      } else if (TAG_ELSE.equals(qName)) {
	startElseTag();
      } else if (TAG_LIST.equals(qName)) {
	startListTag();
      } else if (TAG_PROPERTY.equals(qName)) {
	startPropertyTag(attrs);
      } else if (TAG_THEN.equals(qName)) {
	startThenTag();
      } else if (TAG_VALUE.equals(qName)) {
	startValueTag();
      } else if (TAG_AND.equals(qName)) {
	startAndTag();
      } else if (TAG_OR.equals(qName)) {
	startOrTag();
      } else if (TAG_NOT.equals(qName)) {
	startNotTag();
      } else if (TAG_TEST.equals(qName)) {
	startTestTag(attrs);
      } else if (TAG_LOCKSSCONFIG.equals(qName)) {
	// do nothing
      } else {
	throw new IllegalArgumentException("Unexpected tag: " + qName);
      }
    }

    /**
     * Handle the ending tags of elements.
     */
    public void endElement(String namespaceURI, String localName,
			   String qName)
	throws SAXException {

      if (TAG_IF.equals(qName)) {
	endIfTag();
      } else if (TAG_ELSE.equals(qName)) {
	endElseTag();
      } else if (TAG_LIST.equals(qName)) {
	endListTag();
      } else if (TAG_PROPERTY.equals(qName)) {
	endPropertyTag();
      } else if (TAG_THEN.equals(qName)) {
	endThenTag();
      } else if (TAG_VALUE.equals(qName)) {
	endValueTag();
      } else if (TAG_AND.equals(qName)) {
	endAndTag();
      } else if (TAG_OR.equals(qName)) {
	endOrTag();
      } else if (TAG_NOT.equals(qName)) {
	endNotTag();
      } else if (TAG_TEST.equals(qName)) {
	endTestTag();
      } else if (TAG_LOCKSSCONFIG.equals(qName)) {
	// do nothing
      }
      // Don't need to throw here.  Unsupported tags will have already
      // thrown in startElement().
    }

    /**
     * Handle character data encountered in a tag.  The character data
     * should never be anything other than a property value.
     */
    public void characters(char[] ch, int start, int length)
	throws SAXException {
      // m_charBuffer shouldn't be null at this point, but
      // just in case...
      if (doEval() && m_charBuffer != null) {
	m_charBuffer.append(ch, start, length);
      }
    }

    /**
     * Handle encountering the start of an "else" tag.
     */
    private void startElseTag() {
      ((If)m_ifStack.peek()).inElse = true;
    }

    /**
     * Handle encountering the start of a "list" tag.
     */
    private void startListTag() {
      if (doEval()) {
	m_inList = true;
	m_propList = new ArrayList();
      }
    }

    /**
     * Handle encountering a starting "property" tag.  Get the
     * property's name and value (if any).  Name is required, value is
     * not.
     */
    private void startPropertyTag(Attributes attrs) {
      if (doEval()) {
	boolean hasValueAttr = false;
	String name = attrs.getValue("name");
	String value = attrs.getValue("value");

	if (value != null) hasValueAttr = true;

	m_propStack.push(name);

	// If we have both a name and a value we can add it to the
	// property tree right away.
	if (hasValueAttr) {
	  setProperty(value);
	}
      }
    }

    /**
     * Handle encountering the start of an "if" tag by parsing
     * the conditional attributes and acting on them accordingly.
     */
    private void startIfTag(Attributes attrs) {
      If curIf = new If();
      // If there's a previous 'if', and it's false, AND we're in a 'then'
      // clause, don't even bother evaluating this one, the parent makes
      // it false.
      if (!m_ifStack.empty() &&
          !((If)m_ifStack.peek()).evalIf &&
          ((If)m_ifStack.peek()).inThen) {
        curIf.evalIf = false;
      } else if (attrs.getLength() > 0) {
	curIf.evalIf = evaluateAttributes(attrs);
      }
      m_ifStack.push(curIf);
      // Reset m_testEval
      m_testEval = true;
    }

    /**
     * Handle encountering a starting "then" tag.
     */
    private void startThenTag() {
      If curIf = (If)m_ifStack.peek();
      curIf.inThen = true;
    }

    /**
     * Handle encountering a starting "value" tag.
     */
    private void startValueTag() {
      if (doEval()) {
	// Inside a "value" element.
	m_inValue = true;

	// Prepare a buffer to hold character data, which may be
	// chunked across several characters() calls
	m_charBuffer = new StringBuffer();
      }
    }

    /**
     * Handle encountering a starting "and" tag.
     */
    private void startAndTag() {
      m_condStack.push(TAG_AND);
      m_testEval = true;
    }

    /**
     * Handle encountering a starting "or" tag.
     */
    private void startOrTag() {
      if (m_condStack.isEmpty()) {
        // 'or' expressions start out false
        ((If)m_ifStack.peek()).evalIf &= false;
      }
      m_testEval = false;
      m_condStack.push(TAG_OR);
    }

    /**
     * Handle encountering a starting "not" tag.
     */
    private void startNotTag() {
      m_condStack.push(TAG_NOT);
    }


    /**
     * Set the state of the test evaluation boolean.
     */
    private void startTestTag(Attributes attrs) {
      if (attrs.getLength() > 0) {
        // Find the curent conditional
        if (m_condStack.isEmpty() ||
            m_condStack.peek() == TAG_AND ||
            m_condStack.peek() == TAG_NOT) {
          m_testEval &= evaluateAttributes(attrs);
        } else if (m_condStack.peek() == TAG_OR) {
          m_testEval |= evaluateAttributes(attrs);
        }
      }
    }

    /**
     * Handle encoutering the end of an "else" tag.
     */
    private void endElseTag() {
      ((If)m_ifStack.peek()).inElse = false;
    }

    /**
     * Handle encountering the end of a "list" tag.
     */
    private void endListTag() {
      if (doEval()) {
	setListProperty(m_propList);

	// Clean-up.
	m_propList = null;
	m_inList = false;
      }
    }

    /**
     * Handle encountering the end of a "property" tag.
     */
    private void endPropertyTag() {
      if (doEval()) {
	m_propStack.pop();
      }
    }

    /**
     * Handle encountering the end of a "propgroup" tag.
     */
    private void endIfTag() {
      m_ifStack.pop();
    }


    /**
     * Handle encountering the end of a "then" tag.
     */
    private void endThenTag() {
      ((If)m_ifStack.peek()).inThen = false;
    }

    /**
     * Handle encountering the end of a "value" tag.
     */
    private void endValueTag() {
      // The only character data in the property file should be
      // inside "value" tags!  It doesn't belong anywhere else.
      if (doEval() && m_inValue && m_charBuffer != null) {
	if (m_inList) {
	  // If we're inside a list, we need to add this value to the
	  // current temporary property list.
	  if (log.isDebug3()) {
	    log.debug3("Adding '" + m_charBuffer.toString().trim() + "' to " +
		       getPropname() + " prop list");
	  }
	  m_propList.add(m_charBuffer.toString().trim());
	} else {
	  // Otherwise, just add the property key and value to the prop
	  // tree.
	  setProperty(m_charBuffer.toString().trim());
	}
      }

      m_inValue = false;
      // reset the char buffer
      m_charBuffer = null;
    }

    private void endAndTag() {
      m_condStack.pop();

      If curIf = (If)m_ifStack.peek();
      curIf.evalIf &= m_testEval;
    }

    private void endOrTag() {
      m_condStack.pop();

      If curIf = (If)m_ifStack.peek();
      curIf.evalIf |= m_testEval;
    }

    private void endNotTag() {
      m_condStack.pop();

      If curIf = (If)m_ifStack.peek();
      curIf.evalIf = !m_testEval;
    }

    
    /**
     * Handle encountering the end of a "test" tag.
     */
    private void endTestTag() {
      if (m_condStack.isEmpty()) {
        // If we're not in a conditional at all, this should be a single
        // <test>, i.e. <if><test foo="bar"/><then>...</then></if>. Just
        // apply the current test results
        ((If)m_ifStack.peek()).evalIf &= m_testEval;
      } else {
        applyTestToCurrentCondStackLevel();
      }
    }

    // Utility method used by endCondTag and endTestTag
    private void applyTestToCurrentCondStackLevel() {
      String cond = (String)m_condStack.peek();
      If curIf = (If)m_ifStack.peek();
      if (cond == TAG_AND) {
        curIf.evalIf &= m_testEval;
      } else if (cond == TAG_OR) {
        curIf.evalIf |= m_testEval;
      } else if (cond == TAG_NOT) {
        curIf.evalIf &= !m_testEval;
      }
    }

    /**
     * Return the current property name.
     */
    private String getPropname() {
      return StringUtil.separatedString(m_propStack, ".");
    }

    /**
     * Log a warning if overwriting an existing property.
     */
    private void setProperty(String value) {
      m_props.put(getPropname(), value);
    }

    /**
     * Set a list of property values.
     */
    private void setListProperty(List list) {
      setProperty(StringUtil.separatedString(list, ";"));
    }

    /**
     * Evaluate the attributes of this test (whether an <if...> or a
     * <test...> tag) and return the boolean value.
     */
    public boolean evaluateAttributes(Attributes attrs) {
      // Evaluate the attributes of the tag and set the
      // value "returnVal" appropriately.

      // If the test contains no conditionals, or if it contains
      // any unexpected conditionals, return false.

      // XXX: This could probably be more efficient!
      int len = attrs.getLength();

      if (len == 0) {
	log.debug("No conditionals to check.");
	return false;
      }

      for (int i = 0; i < len; i++) {
	String attrName = attrs.getQName(i);
	if (!conditionals.contains(attrName)) {
	  log.warning("Found unexpected conditional '" + attrName +
	              "', returning false for test.");
	  return false;
	}
      }

      // Get the XML element attributes
      String group = null;
      String hostname = null;
      String platformName = null;
      Version daemonMin = null;
      Version daemonMax = null;
      Version platformMin = null;
      Version platformMax = null;

      group = attrs.getValue("group");
      hostname = attrs.getValue("hostname");
      platformName = attrs.getValue("platformName");

      if (attrs.getValue("daemonVersionMin") != null) {
	daemonMin = new DaemonVersion(attrs.getValue("daemonVersionMin"));
      }

      if (attrs.getValue("daemonVersionMax") != null) {
	daemonMax = new DaemonVersion(attrs.getValue("daemonVersionMax"));
      }

      if (attrs.getValue("daemonVersion") != null) {
	if (daemonMin != null || daemonMax != null) {
	  throw new IllegalArgumentException("Cannot mix daemonMin, daemonMax " +
					     "and daemonVersion!");
	}
	daemonMin = daemonMax =
	  new DaemonVersion(attrs.getValue("daemonVersion"));
      }

      if (attrs.getValue("platformVersionMin") != null) {
	platformMin = new PlatformVersion(attrs.getValue("platformVersionMin"));
      }

      if (attrs.getValue("platformVersionMax") != null) {
	platformMax = new PlatformVersion(attrs.getValue("platformVersionMax"));
      }

      if (attrs.getValue("platformVersion") != null) {
	if (platformMin != null || platformMax != null) {
	  throw new IllegalArgumentException("Cannot mix platformMin, " +
					     "platformMax and platformVersion!");
	}
	platformMin = platformMax =
	  new PlatformVersion(attrs.getValue("platformVersion"));
      }

      boolean returnVal = true;

      /*
       * Group membership checking.
       */
      if (group != null) {
	returnVal &= StringUtil.equalStringsIgnoreCase(m_sysGroup, group);
      }

      /*
       * Hostname checking.
       */
      if (hostname != null) {
	returnVal &= StringUtil.equalStringsIgnoreCase(m_sysHostname, hostname);
      }

      /*
       * Daemon version checking.
       */
      if (daemonMin != null || daemonMax != null) {
	returnVal &= compareVersion(m_sysDaemonVer, daemonMin, daemonMax);
      }

      /*
       * Platform version checking.
       */
      if (platformMin != null || platformMax != null) {
	returnVal &= compareVersion(m_sysPlatformVer, platformMin, platformMax);
      }

      /*
       * Platform name checking.
       */
      if (platformName != null) {
	returnVal &= StringUtil.equalStringsIgnoreCase(m_sysPlatformName, platformName);
      }

      return returnVal;
    }

    boolean compareVersion(Version sysVersion, Version versionMin, Version versionMax) {
      boolean returnVal = true;

      if (sysVersion == null) {
	return false;
      }

      if (versionMin != null && versionMax != null) {
	// Have both min and max...
	returnVal &= ((sysVersion.toLong() >= versionMin.toLong()) &&
		      (sysVersion.toLong() <= versionMax.toLong()));
      } else if (versionMin != null) {
	// Have min...
	returnVal &= (sysVersion.toLong() >= versionMin.toLong());
      } else if (versionMax != null) {
	// Have max...
	returnVal &= (sysVersion.toLong() <= versionMax.toLong());
      }

      return returnVal;
    }
  }
}
