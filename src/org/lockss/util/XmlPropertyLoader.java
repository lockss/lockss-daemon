/*
 * $Id: XmlPropertyLoader.java,v 1.1 2004-05-28 04:57:31 smorabito Exp $
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

  private static Logger logger = Logger.getLogger("XmlPropertyLoader");

  /**
   * Load a set of XML properties from the input stream.
   */
  public static synchronized boolean load(PropertyTree props, InputStream istr) {
    boolean isLoaded = false;
    
    try {
      SAXParserFactory factory = SAXParserFactory.newInstance();
      
      factory.setValidating(true);
      factory.setNamespaceAware(false);
      
      SAXParser parser = factory.newSAXParser();
      
      parser.parse(istr, new LockssConfigHandler(props));
      
      isLoaded = true;
    } catch (Exception ex) {
      logger.warning("Unable to parse configuration file: " +
		     ex.toString());
    }
    

    return isLoaded;

  }
}

/**
 * SAX parser handler.
 */

class LockssConfigHandler extends DefaultHandler {

  public static char PROPERTY_SEPARATOR = '.';

  private static final String TAG_PROPERTY  = "property";
  private static final String TAG_VALUE     = "value";
  private static final String TAG_LIST      = "list";
  private static final String TAG_PROPGROUP = "propgroup";
  private static final String TAG_THEN      = "then";
  private static final String TAG_ELSE      = "else";

  private static final String PREFIX = Configuration.PREFIX;
  private static final String PLATFORM = Configuration.PLATFORM;
  private static final String DAEMON = Configuration.PREFIX + "daemon.";

  private static final String PROPERTY_DAEMONVERSION = DAEMON + "version";
  private static final String PROPERTY_PLATFORMVERSION = PLATFORM + "version";
  private static final String PROPERTY_GROUP = PLATFORM + "group";

  // Simple stack that helps us know what our current level in 
  // the property tree is.
  private PropStack m_propStack = new PropStack();
  // When building a list of configuration values for a single key.
  private List m_propList = null;
  // True iff the parser is currently inside a "value" element.
  private boolean m_inValue = false;
  // True iff the parser is currently inside a "list" element.
  private boolean m_inList  = false;
  // True iff the parser is currently inside a "propgroup" element.
  private boolean m_inPropgroup = false;
  // True iff the parser is currently inside an "else" element.
  private boolean m_inElse = false;
  // True iff the parser is currently inside a "then" element.
  private boolean m_inThen = false;

  // True iff the conditions in the propgroup attribute conditionals
  // are satisfied.
  private boolean m_evalPropgroup = false;

  // The property tree we're adding to.
  private PropertyTree m_props;

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
   *      in neither  a <then> or an &lt;else&gt;</li>
   * </ol>
   */
  
  private boolean doEval() {
    return (!m_inPropgroup ||
	    ((m_evalPropgroup && m_inThen) ||
	     (!m_evalPropgroup && m_inElse)) ||
	    (m_evalPropgroup && !m_inThen && !m_inElse));    
  }

  /**
   * Handle the starting tags of elements.  Based on the name of the
   * tag, call the appropriate handler method.
   */
  public void startElement(String namespaceURI, String localName,
			   String qName, Attributes attrs)
      throws SAXException {

    if (TAG_PROPGROUP.equals(qName)) {
      startPropgroupTag(attrs);
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
    }
  }

  /**
   * Handle the ending tags of elements.
   */
  public void endElement(String namespaceURI, String localName,
			 String qName)
      throws SAXException {

    if (TAG_PROPGROUP.equals(qName)) {
      endPropgroupTag();
    } else if (TAG_ELSE.equals(qName)) {
      endElseTag();
    } else if (TAG_LIST.equals(qName)) {
      endListTag();
    } else if (TAG_PROPERTY.equals(qName)) {
      endPropertyTag();
    } else if (TAG_PROPGROUP.equals(qName)) {
      endPropgroupTag();
    } else if (TAG_THEN.equals(qName)) {
      endThenTag();
    } else if (TAG_VALUE.equals(qName)) {
      endValueTag();
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
	  m_props.put(m_propStack.toString(), s);
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
      m_propList = new LinkedList();
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
	m_props.put(m_propStack.toString(), value);
      }
    }
  }

  /**
   * Handle encountering the start of a "propgroup" tag by parsing
   * the conditional attributes and acting on them accordingly.
   */
  private void startPropgroupTag(Attributes attrs) {
    m_inPropgroup = true;

    // Evaluate the attributes of the tag and set the
    // value "m_conditionalTrue" appropriately.

    // Get the XML element attributes
    String group = null;
    Version daemonMin = null;
    Version daemonMax = null;
    Version daemonVer = null;
    Version platformMin = null;
    Version platformMax = null;
    Version platformVer = null;

    if (attrs.getValue("group") != null)
      group = attrs.getValue("group");

    if (attrs.getValue("daemonVersionMin") != null)
      daemonMin = new DaemonVersion(attrs.getValue("daemonVersionMin"));

    if (attrs.getValue("daemonVersionMax") != null)
      daemonMax = new DaemonVersion(attrs.getValue("daemonVersionMax"));

    if (attrs.getValue("daemonVersion") != null)
      daemonVer = new DaemonVersion(attrs.getValue("daemonVersion"));

    if (attrs.getValue("platformVersionMin") != null)
      platformMin = new PlatformVersion(attrs.getValue("platformVersionMin"));

    if (attrs.getValue("platformVersionMax") != null)
      platformMax = new PlatformVersion(attrs.getValue("platformVersionMax"));

    if (attrs.getValue("platformVersion") != null)
      platformVer = new PlatformVersion(attrs.getValue("platformVersion"));

    // Save the current running daemon and platform version
    Version sysPlatformVer = getPlatformVersion();
    Version sysDaemonVer = getDaemonVersion();
    String sysGroup = getGroup();
    
    // Define some condition combos that we should never see.
    boolean badDaemonCombo = ((daemonMin != null ||
			       daemonMax != null) &&
			      daemonVer != null);
    
    boolean badPlatformCombo = ((platformMin != null ||
				 platformMax != null) &&
				platformVer != null);
    
    if (badDaemonCombo || badPlatformCombo) {
      // TODO: Throw some kind of error here, probably...
    }

    /*
     * Group membership checking.
     */
    if (group != null && sysGroup != null) {
      if (sysGroup.equals(group)) m_evalPropgroup = true;
    }
    
    /*
     * Daemon version checking.
     */
    if (daemonMin != null && daemonMax != null && sysDaemonVer != null) {
      // Have both daemon min and max...
      m_evalPropgroup = ((sysDaemonVer.greaterThan(daemonMin) ||
			  sysDaemonVer.equals(daemonMin)) &&
			 (sysDaemonVer.lessThan(daemonMax) ||
			  sysDaemonVer.equals(daemonMax)));
    } else if (daemonMin != null) {
      // Have only daemon min...
      m_evalPropgroup = sysDaemonVer.greaterThan(daemonMin);
    } else if (daemonMax != null) {
      // Have only daemon max...
      m_evalPropgroup = sysDaemonVer.lessThan(daemonMax);
    } else if (daemonVer != null) {
      // Have only unique daemon version...
      m_evalPropgroup = sysDaemonVer.equals(daemonVer);
    }
    
    /*
     * Platform version checking.
     */

    if (platformMin != null && platformMax != null && sysPlatformVer != null) {
      // Have both daemon min and max...
      m_evalPropgroup = ((sysPlatformVer.greaterThan(platformMin) ||
			  sysPlatformVer.equals(platformMin)) &&
			 (sysPlatformVer.lessThan(platformMax) ||
			  sysPlatformVer.equals(platformMax)));    
    } else if (platformMin != null) {
      // Have only daemon min...
      m_evalPropgroup = sysPlatformVer.greaterThan(platformMin);
    } else if (platformMax != null) {
      // Have only daemon max...
      m_evalPropgroup = sysPlatformVer.lessThan(platformMax);
    } else if (platformVer != null) {
      // Have only daemon version...
      m_evalPropgroup = sysPlatformVer.equals(platformVer);
    }

  }

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
      // Add the list elements to the PropTree at the current
      // stack level.
      for (int i = 0; i < m_propList.size() ; i++) {
	m_props.put(m_propStack.toString() + PROPERTY_SEPARATOR + i,
		    m_propList.get(i));
      }
      
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
  private void endPropgroupTag() {
    m_inPropgroup = false;
    m_evalPropgroup = false;
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
    if (!m_inPropgroup || m_evalPropgroup) {
      m_inValue = false;
    }
  }

  private Version getPlatformVersion() {
    String ver = m_props.getProperty(PROPERTY_PLATFORMVERSION);
    return new PlatformVersion(ver);
  }

  private Version getDaemonVersion() {
    String ver = m_props.getProperty(PROPERTY_DAEMONVERSION);
    return new DaemonVersion(ver);
  }

  private String getGroup() {
    return m_props.getProperty(PROPERTY_GROUP);
  }

  /**
   * A simple stack to keep track of where in the property tree
   * the parser is located.  The elements on the stack are individual
   * levels in the property tree, i.e.:  org -> lockss -> log -> etc.
   */
  private class PropStack extends java.util.Stack {

    // Override the default toString to give us a lovely property
    // name.
    public String toString() {
      StringBuffer sb = new StringBuffer();
      int len = size();
      int j = 0;

      for (Iterator i = iterator(); i.hasNext(); ) {
	sb.append((String)i.next());
	if (j < len - 1) {
	  sb.append(PROPERTY_SEPARATOR);
	}

	j++;
      }

      return sb.toString();
    }
  }
}
