/*
 * $Id: EncodedProperty.java,v 1.4 2003-03-01 01:04:49 troberts Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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

*/package org.lockss.util;

import java.util.Properties;
import java.util.Set;
import java.util.Collection;
import java.util.StringTokenizer;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.*;
import org.mortbay.util.B64Code;

/**
 * @author Claire Griffin
 * @version 1.0
 */

public class EncodedProperty extends Properties {

  private static String DEFAULT_ENCODING = "UTF-8";


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
    decodeToMap(encBytes, DEFAULT_ENCODING, this);
  }

  /**
   * decode the string to a property table using the given char set.
   * @param s the string to decode
   * @param charset the charecter set to use in decoding the string
   */
  public void decode(byte[] encodedBytes, String charset)
      throws java.io.IOException {
    decodeToMap(encodedBytes, charset, this);
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
    return encodeFromMap(charset, this);
  }

   /**
   * decode a string of name-value pairs in a given charecter set to a property
   * table.
   * @param srcBytes - the bytes to be decoded
   * @param charset - the charecter set (encoding) of the bytes
   * @param map - the property map in which to store the data
   */
  public static void decodeToMap(byte[] srcBytes, String charset,
                                 Properties map)
      throws java.io.IOException {
    if (charset==null)
      charset=DEFAULT_ENCODING;

    // convert our data
    byte[] converted_bytes = new String(srcBytes,charset).getBytes();

    //then load it into our properties
    ByteArrayInputStream in = new ByteArrayInputStream(converted_bytes);
    map.load(in);
  }

  /**
   *
   * @param charset - the charecter set to use for encoding the bytes
   * @param map - the property map which we will be converting
   * @return a byte array of the encoded properties
   */
  public static byte[] encodeFromMap(String charset, Properties map)
      throws java.io.IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    map.store(out,null);

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
      ret = new Boolean(value).booleanValue();
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

  public void putBoolean(String key, boolean value) {
    setProperty(key, String.valueOf(value));
  }

  public void putByteArray(String key, byte[] value) {
    char[] encoded = B64Code.encode(value);
    setProperty(key,String.valueOf(encoded));
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
}
