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

  /**
   * Create a new name:value pair.
   * This will convert an encoded name:value pair.  Since we don't know
   * whether or not the string is encoded we ALWAYS decode.
   * @param name  the name or key
   * @param value the value assigned to the name or key
   */
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

  /**
   * Get the unencoded name
   * @return the unencoded name or key
   */
  public String getRawName() {
    return m_name;
  }

  /**
   * Get the unencoded value
   * @return the unencoded value
   */
  public String getRawValue() {
    return m_value;
  }

  /**
   * Get the encoded name
   * @return the encoded name or key
   */
  public String getEncodedName() {
    if (m_encodedName == null)
      m_encodedName = formEncodeParameter(m_name);
    return m_encodedName;
  }

  /**
   * Get the encoded value
   * @return the encoded value
   */
  public String getEncodedValue() {
    if (m_encodedValue == null)
      m_encodedValue = formEncodeParameter(m_value);
    return m_encodedValue;
  }

  /**
   * Return /application/x-www-form-encoded content.  This require ' ' to be
   * converted to '+'.  This will also allow us to match incoming client
   * requests.
   * @param url string in which ' ' space has been replace with a '+'
   * @return
   */
  public String formEncodeParameter(String url) {
    //url = StringUtil.replaceString(url, " ", "+");
    String encoded_url = UrlUtil.encodeUrl(url);
    return encoded_url;
  }


  /**
   * Returns a string in the form 'key=value'. The default behavior is to behave
   * like a String for GET requests.  The key and value are the encoded values.
   * @return
   */
  public String toString() {
    return getEncodedName() + "=" + getEncodedValue();
  }

  /**
   * Compare to another FormUrlInput.
   * Using encoded values to match what we see on the other end?
   * @param b the value to compare to
   * @return result of name String.compareTo of the name if different or the
   * result of the value compareTo
   */
  public int compareTo(FormUrlInput b) {
    int name_compare = getEncodedName().compareTo(b.getEncodedName());
    if (name_compare != 0) { return name_compare; }
    int value_compare = getEncodedValue().compareTo(b.getEncodedValue());
    //todo: should just return value_compare these are equivalent.
    if (value_compare != 0) { return value_compare; }
    return 0;
  }
}
