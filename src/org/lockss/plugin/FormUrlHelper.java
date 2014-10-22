/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.plugin;

import java.util.Collections;
import java.util.ArrayList;
import java.lang.StringBuilder;

import org.apache.commons.lang3.StringUtils;
import org.lockss.util.Logger;

/**
 * <p>This class is used by {@link FormUrlNormalizer} to convert between a
 * string representation of a form URL and the object representation. Once in
 * object form, performing modifications will be easier. </p>
 */
public class FormUrlHelper {
  String m_baseUrl;
  ArrayList<FormUrlInput> m_inputs;
  boolean m_valid = false;
  
  private static final Logger logger = Logger.getLogger(FormUrlHelper.class);


  public FormUrlHelper() {
    this("");
  }

  public FormUrlHelper(String baseUrl) {
    setBaseUrl(baseUrl);
  }

  public void setBaseUrl(String baseUrl) {
    m_baseUrl = baseUrl;
    m_valid = true;
    m_inputs = new ArrayList<FormUrlInput>();
  }

  //add a single key/value pair to the url in order
  public void add(String key, String value) {
    if (value == null) { value = "";}
    if (key == null) { key = "";}
    m_inputs.add(new FormUrlInput(key, value));
  }

  public boolean isValid() {
    return m_valid;
  }

  /**
   * convert from a unencoded string url This routine should almost never be
   * called because  it is impossible to tell whether equal signs and ampersands
   * are part of the form parameters or being used as separators. Use
   * convertFromEncodedString instead.
   *
   * @param url
   * @return true if the url is valid
   */
  public boolean convertFromString(String url) {
    // an invalid url if starts with ?, doesn't contain ?
    if (StringUtils.indexOf(url, "?") == -1
        || StringUtils.indexOf(url, "?") == 0) {
      m_valid = false;
      return m_valid;
    }

    String prefix = StringUtils.substringBefore(url, "?");
    setBaseUrl(prefix);
    String rest = StringUtils.substringAfter(url, "?");
    String key;
    String value;
    if (logger.isDebug3()) logger.debug3("rest=" + rest);
    while (rest != null && rest.length() > 0) {
      if (logger.isDebug3()) logger.debug3("rest2=" + rest);

      if (StringUtils.indexOf(rest, "=") > 0) {
        key = StringUtils.substringBefore(rest, "=");
        rest = StringUtils.substringAfter(rest, "=");
        if (logger.isDebug3()) logger.debug3("rest3=" + rest);
        if (rest != null && StringUtils.indexOf(rest, "&") != -1) {
          value = StringUtils.substringBefore(rest, "&");
          add(key, value);
          rest = StringUtils.substringAfter(rest, "&");
        }
        else {
          // last value
          value = rest;
          add(key, value);
          rest = "";
        }
      }
      else {
        //This indicates a form url missing the equals  sign,
        // stop processing at this point
        m_valid = false;
        rest = "";
      }
    }
    return m_valid;
  }

  /**
   * Since this is now identical to convertFromString
   * @deprecated
   * @param url the url to encode
   * @return true if the string is valid (encodable) url.
   */
  public boolean convertFromEncodedString(String url) {
    convertFromString(url);
    return m_valid;
  }

  /**
   * construct a POST or debug URL string
   *
   * @return the reconstructed url as a string
   */
  public String toString() {
    StringBuilder url = new StringBuilder(m_baseUrl);
    int end_i = m_inputs.size() - 1;
    url.append("?");
    for (int i = 0; i < m_inputs.size(); i++) {
      url.append(m_inputs.get(i).getRawName()).append("=");
      url.append(m_inputs.get(i).getRawValue());
      if (i != end_i) {
        url.append("&");
      }
    }
    return url.toString();
  }

  /**
   * construct an encoded GET URL string
   *
   * @return a encoded url string
   */
  public String toEncodedString() {
    //if (m_alreadyencoded) { return toString();}
    StringBuilder url = new StringBuilder(m_baseUrl);
    int end_i = m_inputs.size() - 1;
    url.append("?");
    for (int i = 0; i < m_inputs.size(); i++) {
      url.append(m_inputs.get(i).getEncodedName()).append("=");
      url.append(m_inputs.get(i).getEncodedValue());
      if (i != end_i) {
        url.append("&");
      }
    }
    return url.toString();
  }

  /**
   *  Reorder the keys/values pairs to put them in alphabetically key sorted
   * order.
   */
  public void sortKeyValues() {
    Collections.sort(m_inputs);
  }

  /**
   * limit the number of values for a parameter to the first limit This expects
   * the unencoded key value because that is what the client passed in on the
   * add call.
   *
   * @param key   the parameter to limit
   * @param limit the max number of values to allow
   */
  public void applyLimit(String key, int limit) {
    //iterate through the list.  After limit values, start deleting them.
    int seen = 0;
    if (logger.isDebug2())
      logger.debug2("m_inputs.size()=" + m_inputs.size() + ",key=" + key);
    for (int i = 0; i < m_inputs.size(); i++) {
      if (m_inputs.get(i).getRawName().equals(key)) {
        seen++;
        if (seen > limit) {
          if (logger.isDebug3()) logger.debug3("removing element at i=" + i);
          m_inputs.remove(i);
          i--; //looks ugly
        }
      }
    }
  }
}
