/*
 * $Id: EncodedProperty.java,v 1.11 2006-03-01 02:50:13 smorabito Exp $
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
import org.mortbay.util.B64Code;

/**
 *
 * <p>Arbitrarily nested property lists used by LCAP messages.</p>
 *
 * @author Claire Griffin
 * @author Seth Morabito
 * @version 2.0
 */

public class EncodedProperty extends Properties {

  public static final String DEFAULT_ENCODING = "UTF-8";
  public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

  private static final Logger log = Logger.getLogger("EncodedProperty");


  /**
   * Construct a new property map from an existing Properties object.
   *
   * @param  props  The properties object to clone.
   */
  public static EncodedProperty fromProps(Properties props) {
    EncodedProperty res = new EncodedProperty();
    for (Iterator iter = props.keySet().iterator(); iter.hasNext(); ) {
      String key = (String)iter.next();
      res.setProperty(key, props.getProperty(key));
    }
    return res;
  }

  /**
   * Constructs a new, empty property map.
   *
   * @param   defaults the default property map.
   */
  public EncodedProperty(Properties defaults) {
    super(defaults);
  }

  /**
   * Constructs a new, empty property map.
   */
  public EncodedProperty() {
    super();
  }

  /**
   * Decode the string to a property table using the default encoding.
   * @param encBytes the encoded bytes
   */
  public void decode(byte[] encBytes)
      throws java.io.IOException {
    decode(encBytes, null);
  }

  /**
   * decode the string to a property table using the given char set.
   * @param encBytes the bytes to decode
   * @param charset the charecter set to use in decoding the string
   */
  public void decode(byte[] encBytes, String charset)
      throws java.io.IOException {
    if (charset==null)
      charset=DEFAULT_ENCODING;

    // convert our data
    byte[] converted_bytes = new String(encBytes, charset).getBytes();

    // then load it into our properties
    ByteArrayInputStream in = new ByteArrayInputStream(converted_bytes);
    load(in);
  }


  /**
   * encode the property table as a string using the default char set
   * @return a string containing the property table in the default char set
   * encoding
   */
  public byte[] encode() throws java.io.IOException {
    return encode(DEFAULT_ENCODING);
  }

  /**
   * encode the property table as a string in the specified char set
   * @param charset the char set to use for encoding
   * @return a string containing the property table in the requested char set.
   */
  public byte[] encode(String charset) throws java.io.IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    store(out, null);
    return out.toString().getBytes(charset);
  }

  public byte[] encodeString(String str) {
    try {
      return str.getBytes(DEFAULT_ENCODING);
    }
    catch (UnsupportedEncodingException ex) {
      return null;
    }
  }

  public byte[] encodeString(String str, String charset) {
    try {
      return str.getBytes(charset);
    }
    catch (UnsupportedEncodingException ex) {
      return null;
    }
  }

  /*
   * the following methods parallel those which can be found in 1.4
   * Preferences which supercedes the use of Properties.
   */

  public boolean getBoolean(String key, boolean def) {
    boolean ret = def;

    String value = getProperty(key);

    if(value != null) {
      ret = Boolean.valueOf(value).booleanValue();
    }
    return ret;
  }

  public byte[] getByteArray(String key, byte[] def) {
    byte[] ret = def;

    String value = getProperty(key);
    if(value != null) {
      ret = B64Code.decode(value.toCharArray());
    }
    return ret;
  }

  public double getDouble(String key, double def) {
    double ret = def;

    try {
      String value = getProperty(key);
      if(value != null) {
	ret = Double.parseDouble(value);
      }
    }
    catch (NumberFormatException ex) {
    }
    return ret;
  }


  public float getFloat(String key, float def) {
    float ret = def;
    try {
      String value = getProperty(key);
      if(value != null) {
	ret = Float.parseFloat(value);
      }
    }
    catch (NumberFormatException ex) {
    }
    return ret;
  }


  public int getInt(String key, int def) {
    int ret = def;
    try {
      String value = getProperty(key);
      if(value != null) {
	ret = Integer.parseInt(value);
      }
    }
    catch (NumberFormatException ex) {
    }
    return ret;
  }

  public long getLong(String key, long def) {
    long ret = def;

    try {
      String value = getProperty(key);
      if(value != null) {
	ret = Long.parseLong(value);
      }
    }
    catch (NumberFormatException ex) {
    }
    return ret;
  }

  public EncodedProperty getEncodedProperty(String key) {
    byte[] encodedVal = getByteArray(key, EMPTY_BYTE_ARRAY);

    if (encodedVal == EMPTY_BYTE_ARRAY) { // ref equality should be OK.
      return null;
    }

    EncodedProperty ret = new EncodedProperty();
    try {
      ret.decode(encodedVal);
    } catch (IOException ex) {
      log.error("Unexpected IOException while decoding EncodedProperty: " + ex);
    }
    return ret;
  }

  public void putBoolean(String key, boolean value) {
    setProperty(key, String.valueOf(value));
  }

  public void putByteArray(String key, byte[] value) {
    char[] encoded = B64Code.encode(value);
    setProperty(key, String.valueOf(encoded));
  }

  public void putDouble(String key, double value) {
    setProperty(key, Double.toString(value));
  }

  public void putFloat(String key, float value) {
    setProperty(key, Float.toString(value));
  }

  public void putInt(String key, int value) {
    setProperty(key, Integer.toString(value));
  }

  public void putLong(String key, long value) {
    setProperty(key, Long.toString(value));
  }

  public void putEncodedProperty(String key, EncodedProperty value) {
    try {
      putByteArray(key, value.encode());
    } catch (IOException ex) {
      log.error("Unexpected IOException while encoding EncodedPoperty: " + ex);
    }
  }

  /**
   * <p>Store a list of EncodedProperty objects under this key.  Used
   * only by the V3 Protocol to store vote blocks.</p>
   *
   * <p>Internally, the list is represented by zero or more encoded
   * EncodedProperty objects, separated by carriage returns, '\n'.</p>
   */
  public void putEncodedPropertyList(String key, List value)
      throws IOException {
    StringBuffer encodedValues = new StringBuffer();

    // For each value, encode and append to the string buffer.
    int len = value.size();
    int count = 0;
    for (Iterator ix = value.iterator(); ix.hasNext(); ) {
      EncodedProperty props = (EncodedProperty)ix.next();
      encodedValues.append(encodedPropertyToString(props));
      if (count++ < len - 1) {
	encodedValues.append("\n");
      }
    }

    setProperty(key, encodedValues.toString());
  }

  public List getEncodedPropertyList(String key)
      throws IOException {
    String value = getProperty(key);
    if (value == null) {
      return null;
    }

    StringTokenizer tokenizer = new StringTokenizer(value, "\n", false);
    List entries = new ArrayList();

    while (tokenizer.hasMoreTokens()) {
      EncodedProperty p = stringToEncodedProperty(tokenizer.nextToken());
      if (p != null) {
	entries.add(p);
      }
    }

    return entries;
  }


  // Helper methods

 private EncodedProperty stringToEncodedProperty(String s) throws IOException {
    if (s == null || s.length() <= 0) {
      return null;
    }
    EncodedProperty prop = new EncodedProperty();
    prop.decode(B64Code.decode(s.toCharArray()));
    return prop;
  }

  private String encodedPropertyToString(EncodedProperty props) throws IOException {
    if (props == null) {
      return null;
    }

    return String.valueOf(B64Code.encode(props.encode()));
  }
}
