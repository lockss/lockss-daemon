/*
 * $Id: XmlPropertyLoader.java,v 1.4 2004-06-17 22:44:19 smorabito Exp $
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
import javax.xml.parsers.*;
import org.mortbay.tools.PropertyTree;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import org.lockss.daemon.Configuration;

public class XmlPropertyLoader {

  public static char PROPERTY_SEPARATOR = '.';

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

  private static Logger log = Logger.getLogger("XmlPropertyLoader");

  /**
   * Load a set of XML properties from the input stream.
   */
  public boolean load(PropertyTree props, InputStream istr) {
    boolean isLoaded = false;

    try {
      SAXParserFactory factory = SAXParserFactory.newInstance();

      factory.setValidating(true);
      factory.setNamespaceAware(false);

      SAXParser parser = factory.newSAXParser();

      parser.parse(istr, new LockssConfigHandler(props));

      isLoaded = true;
    } catch (ParserConfigurationException ex) {
      // Really shouldn't ever happen.
      log.warning("parser configuration exception: " + ex);
    } catch (SAXException ex) {
      log.warning("SAX Exception: " + ex);
    } catch (IOException ex) {
      log.warning("IO Exception: " + ex);
    }

    return isLoaded;
  }

  public Version getDaemonVersion() {
    return Configuration.getDaemonVersion();
  }

  public Version getPlatformVersion() {
    return Configuration.getPlatformVersion();
  }

  public String getPlatformHostname() {
    return Configuration.getPlatformHostname();
  }

  public String getPlatformGroup() {
    return Configuration.getPlatformGroup();
  }

  /**
   * SAX parser handler.
   */
  class LockssConfigHandler extends DefaultHandler {

    // Simple stack that helps us know what our current level in
    // the property tree is.
    private Stack m_propStack = new Stack();
    // When building a list of configuration values for a single key.
    private List m_propList = null;

    // True iff the parser is currently inside a "value" element.
    private boolean m_inValue = false;
    // True iff the parser is currently inside a "list" element.
    private boolean m_inList  = false;
    // True iff the parser is currently inside an "if" element.
    private boolean m_inIf = false;
    // True iff the parser is currently inside an "else" element.
    private boolean m_inElse = false;
    // True iff the parser is currently inside a "then" element.
    private boolean m_inThen = false;
    // True iff the parser is currently inside an "and" element.
    private boolean m_inAnd = false;
    // True iff the parser is currently inside an "or" element.
    private boolean m_inOr = false;
    // True iff the parser is currently inside a "not" element.
    private boolean m_inNot = false;
    // True iff the parser is currently inside a "test" element.
    private boolean m_inTest = false;

    // False iff the conditions in the propgroup attribute conditionals
    // are not satisfied.
    private boolean m_evalIf = false;

    // The property tree we're adding to.
    private PropertyTree m_props;

    // Save the current running daemon and platform version
    private Version m_sysPlatformVer = getPlatformVersion();
    private Version m_sysDaemonVer = getDaemonVersion();
    private String m_sysGroup = getPlatformGroup();
    private String m_sysHostname = getPlatformHostname();

    /**
     * Default constructor.
     */
    public LockssConfigHandler(PropertyTree props) {
      super();
      m_props = props;
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
      return (!m_inIf ||
	      ((m_evalIf && m_inThen) ||
	       (!m_evalIf && m_inElse)) ||
	      (m_evalIf && !m_inThen && !m_inElse));
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
	; // do nothing
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
	; // do nothing
      } else {
	throw new IllegalArgumentException("Unexpected tag: " + qName);
      }
    }

    /**
     * Handle character data encountered in a tag.  The character data
     * should never be anything other than a property value.
     */
    public void characters(char[] ch, int start, int length)
	throws SAXException {

      if (doEval()) {
	String s = (new String(ch, start, length)).trim();

	// The only character data in the property file should be
	// inside "value" tags!  It doesn't belong anywhere else.
	if (m_inValue) {
	  if (m_inList) {
	    // If we're inside a list, we need to add this value to the
	    // current temporary property list.
	    m_propList.add(s);
	  } else {
	    // Otherwise, just add the property key and value to the prop
	    // tree.
	    setProperty(s);
	  }
	}
      }
    }

    /**
     * Handle encountering the start of an "else" tag.
     */
    private void startElseTag() {
      m_inElse = true;
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
      m_inIf = true;
      m_evalIf = evaluateAttributes(attrs);
    }

    /**
     * Handle encountering a starting "then" tag.
     */
    private void startThenTag() {
      m_inThen = true;
    }

    /**
     * Handle encountering a starting "value" tag.
     */
    private void startValueTag() {
      if (doEval()) {
	// Inside a "value" element.
	m_inValue = true;
      }
    }

    /**
     * Handle encountering a starting "and" tag.
     */
    private void startAndTag() {
      m_inAnd = true;
      m_evalIf = true; // 'and' evaluates to true by default.
    }

    /**
     * Handle encountering a starting "or" tag.
     */
    private void startOrTag() {
      m_inOr = true;
      m_evalIf = false; // 'or' evaluates to false by default.
    }

    /**
     * Handle encountering a starting "not" tag.
     */
    private void startNotTag() {
      m_inNot = true;
      m_evalIf = true; // 'not' evaluates to true by default.
    }


    /**
     * Depending on whether we are evaluating a test in an "and", an
     * "or", or a "not", do the right thing.
     */
    private void startTestTag(Attributes attrs) {
      m_inTest = true;
      if (m_inAnd) {
	m_evalIf &= evaluateAttributes(attrs);
      } else if (m_inOr) {
	m_evalIf |= evaluateAttributes(attrs);
      } else if (m_inNot) {
	m_evalIf ^= evaluateAttributes(attrs);
      }
    }

    /**
     * Handle encoutering the end of an "else" tag.
     */
    private void endElseTag() {
      m_inElse = false;
    }

    /**
     * Handle encountering the end of a "list" tag.
     */
    private void endListTag() {
      if (doEval()) {
	setProperty(m_propList);

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
      m_inIf = false;
      m_evalIf = false; // Reset the evaluation boolean.
    }


    /**
     * Handle encountering the end of a "then" tag.
     */
    private void endThenTag() {
      m_inThen = false;
    }

    /**
     * Handle encountering the end of a "value" tag.
     */
    private void endValueTag() {
      if (doEval()) {
	m_inValue = false;
      }
    }

    /**
     * Handle encountering the end of an "and" tag.
     */
    private void endAndTag() {
      m_inAnd = false;
    }

    /**
     * Handle encountering the end of an "or" tag.
     */
    private void endOrTag() {
      m_inOr = false;
    }

    /**
     * Handle encountering the end of a "not" tag.
     */
    private void endNotTag() {
      m_inNot = false;
    }

    /**
     * Handle encountering the end of a "test" tag.
     */
    private void endTestTag() {
      m_inTest = false;
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
    public void setProperty(Object value) {
      String prop = getPropname();
      if (m_props.get(prop) != null) {
	log.warning("Overwriting property '" + prop + "'.  Was: '" +
		    m_props.get(prop) + "'," + "Now: '" + value + "'");

      }
      m_props.put(prop, value);
    }

    /**
     * Evaluate the attributes of this test (whether an <if...> or a
     * <test...> tag) and return the boolean value.
     */
    public boolean evaluateAttributes(Attributes attrs) {
      boolean returnVal = true;

      // If we don't have any attributes, short-circuit.
      if (attrs.getLength() == 0) {
	return returnVal;
      }

      // Evaluate the attributes of the tag and set the
      // value "returnVal" appropriately.

      // Get the XML element attributes
      String group = null;
      String hostname = null;
      Version daemonMin = null;
      Version daemonMax = null;
      Version platformMin = null;
      Version platformMax = null;

      group = attrs.getValue("group");
      hostname = attrs.getValue("hostname");

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

      /*
       * Group membership checking.
       */
      if (group != null && m_sysGroup != null) {
	returnVal &= StringUtil.equalStringsIgnoreCase(m_sysGroup, group);
      }

      /*
       * Hostname checking.
       */
      if (hostname != null && m_sysHostname != null) {
	returnVal &= StringUtil.equalStringsIgnoreCase(m_sysHostname, hostname);
      }

      /*
       * Daemon version checking.
       */
      if (m_sysDaemonVer != null) {
	if (daemonMin != null && daemonMax != null) {
	  // Have both daemon min and max...
	  returnVal &= ((m_sysDaemonVer.toLong() >= daemonMin.toLong()) &&
			  (m_sysDaemonVer.toLong() <= daemonMax.toLong()));
	} else if (daemonMin != null) {
	  // Have only daemon min...
	  returnVal &= (m_sysDaemonVer.toLong() >= daemonMin.toLong());
	} else if (daemonMax != null) {
	  // Have only daemon max...
	  returnVal &= (m_sysDaemonVer.toLong() <= daemonMax.toLong());
	}
      }

      /*
       * Platform version checking.
       */
      if (m_sysPlatformVer != null) {
	if (platformMin != null && platformMax != null) {
	  // Have both platform min and max...
	  returnVal &= ((m_sysPlatformVer.toLong() >= platformMin.toLong()) &&
			  (m_sysPlatformVer.toLong() <= platformMax.toLong()));
	} else if (platformMin != null) {
	  // Have only platform min...
	  returnVal &= (m_sysPlatformVer.toLong() >= platformMin.toLong());
	} else if (platformMax != null) {
	  // Have only platform max...
	  returnVal &= (m_sysPlatformVer.toLong() <= platformMax.toLong());
	}
      }

      return returnVal;
    }
  }
}
