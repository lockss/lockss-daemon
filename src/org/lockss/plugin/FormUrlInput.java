/*
 *
 *
 * Copyright (c) 2001-2012 Board of Trustees of Leland Stanford Jr. University,
 * all rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 * STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Except as contained in this notice, the name of Stanford University shall not
 * be used in advertising or otherwise to promote the sale, use or other dealings
 * in this Software without prior written authorization from Stanford University.
 *
 * /
 */

package org.lockss.plugin;

import org.lockss.util.UrlUtil;

/** @author mlanken */
public class FormUrlInput implements Comparable<FormUrlInput> {
  private String m_name;
  private String m_value;
  private String m_encodedName;
  private String m_encodedValue;

  public FormUrlInput(String name, String value) {
    if (name == null || name.length() <= 0) {
      m_name = "";
    }
    else try {
      m_name = UrlUtil.decodeUrl(name);
    }
    catch (IllegalArgumentException ex) {
      m_name = name;
    }
    if (value == null || value.equals("")) {
      m_value = "";
    }
    else try {
      m_value = UrlUtil.decodeUrl(value);
    }
    catch (IllegalArgumentException ex) {
      m_value = value;
    }
  }

  public String getRawName() {
    return m_name;
  }

  public String getRawValue() {
    return m_value;
  }

  //application/x-www-form-encoded content requires space be converted to a +
  // .  This will also allow us to match incoming client requests
  public String formEncodeParameter(String url) {
    //url = StringUtil.replaceString(url, " ", "+");
    String encoded_url = UrlUtil.encodeUrl(url);
    return encoded_url;
  }

  public String getName() {
    if (m_encodedName == null)
      m_encodedName = formEncodeParameter(m_name);
    return m_encodedName;
  }

  public String getValue() {
    if (m_encodedValue == null)
      m_encodedValue = formEncodeParameter(m_value);
    return m_encodedValue;
  }

  //default behavior is to behave like a String for GET requests
  public String toString() {
    return getName() + "=" + getValue();
  }

  //using encoded values to match what we see on the other end?
  public int compareTo(FormUrlInput b) {
    int name_compare = getName().compareTo(b.getName());
    if (name_compare != 0) { return name_compare; }
    int value_compare = getValue().compareTo(b.getValue());
    if (value_compare != 0) { return value_compare; }
    return 0;
  }
}
