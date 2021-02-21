/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

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
   * Creates a PDF object from the given value, object number and generation
   * number.
   * </p>
   * 
   * @param value
   *          A non-<code>null</code> PDF token.
   * @param objectNumber
   *          A PDF object number.
   * @param generationNumber
   *          A PDF generation number.
   * @return A PDF token.
   * @since 1.74.4
   */
  PdfToken makeObject(PdfToken value,
                      long objectNumber,
                      int generationNumber);
  
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
