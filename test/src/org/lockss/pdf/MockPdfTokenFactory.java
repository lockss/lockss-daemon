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

package org.lockss.pdf;

import java.util.*;

public class MockPdfTokenFactory implements PdfTokenFactory {

  @Override
  public PdfToken makeArray() {
    return new MockPdfToken() {
      @Override public List<PdfToken> getArray() { return Collections.<PdfToken>emptyList(); }
      @Override public boolean isArray() { return true; }
    };
  }

  @Override
  public PdfToken makeArray(final List<PdfToken> arrayElements) {
    return new MockPdfToken() {
      @Override public List<PdfToken> getArray() { return new ArrayList<PdfToken>(arrayElements); }
      @Override public boolean isArray() { return true; }
    };
  }

  @Override
  public PdfToken makeBoolean(final boolean value) {
    return new MockPdfToken() {
      @Override public boolean getBoolean() { return value; }
      @Override public boolean isBoolean() { return true; }
    };
  }

  @Override
  public PdfToken makeDictionary() {
    return new MockPdfToken() {
      @Override public Map<String, PdfToken> getDictionary() { return Collections.<String, PdfToken>emptyMap(); }
      @Override public boolean isDictionary() { return true; }
    };
  }

  @Override
  public PdfToken makeDictionary(final Map<String, PdfToken> mapping) {
    return new MockPdfToken() {
      @Override public Map<String, PdfToken> getDictionary() { return new HashMap<String, PdfToken>(mapping); }
      @Override public boolean isDictionary() { return true; }
    };
  }

  @Override
  public PdfToken makeFloat(final float value) {
    return new MockPdfToken() {
      @Override public float getFloat() { return value; }
      @Override public boolean isFloat() { return true; }
    };
  }

  @Override
  public PdfToken makeInteger(final long value) {
    return new MockPdfToken() {
      @Override public long getInteger() { return value; }
      @Override public boolean isInteger() { return true; }
    };
  }

  @Override
  public PdfToken makeName(final String value) {
    return new MockPdfToken() {
      @Override public String getString() { return value; }
      @Override public boolean isString() { return true; }
    };
  }

  @Override
  public PdfToken makeNull() {
    return new MockPdfToken() {
      @Override public boolean isNull() { return true; }
    };
  }

  @Override
  public PdfToken makeObject(final PdfToken value) {
    return new MockPdfToken() {
      @Override public PdfToken getObject() { return value; }
      @Override public boolean isObject() { return true; }
    };
  }
  
  @Override
  public PdfToken makeOperator(final String operator) {
    return new MockPdfToken() {
      @Override public String getOperator() { return operator; }
      @Override public boolean isOperator() { return true; }
    };
  }

  @Override
  public PdfToken makeString(final String value) {
    return new MockPdfToken() {
      @Override public String getString() { return value; }
      @Override public boolean isString() { return true; }
    };
  }

}
