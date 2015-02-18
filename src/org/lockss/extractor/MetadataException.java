/*
 * $Id$
 */

/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.extractor;

public class MetadataException extends /*Runtime*/Exception {
  String rawVal;
  String normVal;
  MetadataField field;

  public MetadataException() {
    super();
  }

  public MetadataException(String message) {
    super(message);
  }

  public MetadataException(String message, Throwable cause) {
    super(message, cause);
  }

  public MetadataException setField(MetadataField field) {
    this.field = field;
    return this;
  }

  public MetadataException setRawValue(String rawValue) {
    rawVal = rawValue;
    return this;
  }

  public MetadataException setNormalizedValue(String normValue) {
    normVal = normValue;
    return this;
  }

  public MetadataField getField() {
    return field;
  }

  public String getRawValue() {
    return rawVal;
  }

  public String getNormalizedValue() {
    return normVal;
  }

  public static class CardinalityException extends MetadataException {
    private String cookedValue;

    public CardinalityException(String cookedValue) {
      super();
      this.cookedValue = cookedValue;
    }

    public CardinalityException(String cookedValue, String message) {
      super(message);
      this.cookedValue = cookedValue;
    }

    public String getValue() {
      return cookedValue;
    }
  }

  public static class ValidationException extends MetadataException {
    public ValidationException() {
      super();
    }

    public ValidationException(String message) {
      super(message);
    }
  }
}
