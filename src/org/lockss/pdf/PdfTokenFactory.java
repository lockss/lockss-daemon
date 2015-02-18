/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

/**
 * <p>
 * Abstraction for a PDF token (PDF data type) factory. This 
 * interface defines an upcast facility complementary to the
 * characterization and external downcast facilities defined by
 * {@link PdfToken}.
 * </p>
 * @author Thib Guicherd-Callin
 * @since 1.56
 * @see PdfDocumentFactory
 * @see PdfToken
 */
public interface PdfTokenFactory {

  /**
   * <p>
   * Convenience method to make an empty PDF array.
   * </p>
   * @return An empty PDF array.
   * @since 1.56
   */
  PdfToken makeArray();
  
  /**
   * <p>
   * Creates a PDF array from the given list of PDF tokens.
   * </p>
   * @param arrayElements A non-<code>null</code> list of PDF tokens
   *          (possibly empty).
   * @return A PDF token.
   * @since 1.56
   */
  PdfToken makeArray(List<PdfToken> arrayElements);
  
  /**
   * <p>
   * Creates a PDF boolean from the given value.
   * </p>
   * @param value A <code>boolean</code> value.
   * @return A PDF token.
   * @since 1.56
   */
  PdfToken makeBoolean(boolean value);
  
  /**
   * <p>
   * Convenience method to make an empty PDF dictionary.
   * </p>
   * @return An empty PDF dictionary.
   * @since 1.56
   */
  PdfToken makeDictionary();
  
  /**
   * <p>
   * Creates a PDF array from the given map from strings (PDF names)
   * to PDF tokens.
   * </p>
   * @param mapping A non-<code>null</code> map from strings to PDF
   *          tokens (possibly empty).
   * @return A PDF token.
   * @since 1.56
   */
  PdfToken makeDictionary(Map<String, PdfToken> mapping);
  
  /**
   * <p>
   * Creates a PDF float from the given value.
   * </p>
   * @param value A <code>float</code> value.
   * @return A PDF token.
   * @since 1.56
   */
  PdfToken makeFloat(float value);

  /**
   * <p>
   * Creates a PDF integer from the given value.
   * </p>
   * @param value A <code>long</code> value.
   * @return A PDF token.
   * @since 1.56
   */
  PdfToken makeInteger(long value);
  
  /**
   * <p>
   * Creates a PDF name from the given value.
   * </p>
   * @param value A non-<code>null</code> string value.
   * @return A PDF token.
   * @since 1.56
   */
  PdfToken makeName(String value);
  
  /**
   * <p>
   * Convenience method to obtain a PDF null object.
   * </p>
   * @return The PDF null object.
   * @since 1.56
   */
  PdfToken makeNull();
  
  /**
   * <p>
   * Creates a PDF object from the given value.
   * </p>
   * @param value A non-<code>null</code> PDF token.
   * @return A PDF token.
   * @since 1.56.3
   */
  PdfToken makeObject(PdfToken value);
  
  /**
   * <p>
   * Creates a PDF operator from the given value.
   * </p>
   * @param value A non-<code>null</code> string value.
   * @return A PDF token.
   * @since 1.56
   */
  PdfToken makeOperator(String operator);
  
  /**
   * <p>
   * Creates a PDF string from the given value.
   * </p>
   * @param value A non-<code>null</code> string value.
   * @return A PDF token.
   * @since 1.56
   */
  PdfToken makeString(String value);
  
}
