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
 * Abstraction for a PDF token (PDF data type).
 * </p>
 * <p>
 * This API defines a mapping between high-level PDF tokens ({@link
 * PdfToken}) and their external representation (Java types). This
 * interface defines characterization and external downcast; upcast
 * is defined in the sister interface {@link PdfTokenFactory}.
 * </p>
 * <p>
 * The naming used in the methods of this interface reflects the PDF
 * type being represented, even if it seemingly clashes in name with
 * the Java type it is represented by (e.g. {@link #getInteger()}
 * returns a <code>long</code>, because not all PDF integers can
 * fit into a Java <code>int</code>).
 * </p>
 * <table>
 * <thead>
 * <tr>
 * <th>PDF type</th>
 * <th>External representation</th>
 * <th>Characterization</th>
 * <th>External downcast</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 * <td>PDF array</td>
 * <td>{@link List}&lt;{@link PdfToken}&gt;</td>
 * <td>{@link #isArray()}</td>
 * <td>{@link #getArray()}</td>
 * </tr>
 * <tr>
 * <td>PDF boolean</td>
 * <td><code>boolean</code></td>
 * <td>{@link #isBoolean()}</td>
 * <td>{@link #getBoolean()}</td>
 * </tr>
 * <tr>
 * <td>PDF dictionary</td>
 * <td>{@link Map}&lt;{@link String}, {@link PdfToken}&gt;</td>
 * <td>{@link #isDictionary()}</td>
 * <td>{@link #getDictionary()}</td>
 * </tr>
 * <tr>
 * <td>PDF float</td>
 * <td><code>float</code></td>
 * <td>{@link #isFloat()}</td>
 * <td>{@link #getFloat()}</td>
 * </tr>
 * <tr>
 * <td>PDF integer</td>
 * <td><code>long</code></td>
 * <td>{@link #isInteger()}</td>
 * <td>{@link #getInteger()}</td>
 * </tr>
 * <tr>
 * <td>PDF name</td>
 * <td>{@link String}</td>
 * <td>{@link #isName()}</td>
 * <td>{@link #getName()}</td>
 * </tr>
 * <tr>
 * <td>PDF null</td>
 * <td><code>null</code></td>
 * <td>{@link #isNull()}</td>
 * <td>n/a</td>
 * </tr>
 * <tr>
 * <td>PDF object</td>
 * <td>{@link PdfToken}</td>
 * <td>{@link #isObject()}</td>
 * <td>{@link #getObject()}</td>
 * </tr>
 * <tr>
 * <td>PDF operator</td>
 * <td>{@link String}</td>
 * <td>{@link #isOperator()}</td>
 * <td>{@link #getOperator()}</td>
 * </tr>
 * <tr>
 * <td>PDF string</td>
 * <td>{@link String}</td>
 * <td>{@link #isString()}</td>
 * <td>{@link #getString()}</td>
 * </tr>
 * </tbody>
 * </table>
 * <p>
 * This interface does not currently provide a representation for the
 * PDF 'stream' type. This may change in a future version.
 * </p>
 * @author Thib Guicherd-Callin
 * @since 1.56
 * @see PdfTokenFactory
 */
public interface PdfToken {

  /**
   * <p>
   * If {@link #isArray()} is <b>true</b>, downcasts this token to its
   * external representation, otherwise the behavior is undefined.
   * </p>
   * @return A list of PDF tokens.
   * @since 1.56
   */
  List<PdfToken> getArray();

  /**
   * <p>
   * If {@link #isBoolean()} is <b>true</b>, downcasts this token to
   * its external representation, otherwise the behavior is undefined.
   * </p>
   * @return A <code>boolean</code> value.
   * @since 1.56
   */
  boolean getBoolean();
  
  /**
   * <p>
   * If {@link #isDictionary()} is <b>true</b>, downcasts this token to
   * its external representation, otherwise the behavior is undefined.
   * </p>
   * @return A map from strings to PDF tokens.
   * @since 1.56
   */
  Map<String, PdfToken> getDictionary();
 
  /**
   * <p>
   * If {@link #isFloat()} is <b>true</b>, downcasts this token to
   * its external representation, otherwise the behavior is undefined.
   * </p>
   * @return A <code>float</code> value.
   * @since 1.56
   */
  float getFloat();
  
  /**
   * <p>
   * If {@link #isInteger()} is <b>true</b>, downcasts this token to
   * its external representation, otherwise the behavior is undefined.
   * </p>
   * @return A <code>long</code> value.
   * @since 1.56
   */
  long getInteger();
  
  /**
   * <p>
   * If {@link #isName()} is <b>true</b>, downcasts this token to
   * its external representation, otherwise the behavior is undefined.
   * </p>
   * @return A {@link String} value.
   * @since 1.56
   */
  String getName();
  

  /**
   * <p>
   * If {@link #isObject()} is <b>true</b>, downcasts this token to
   * its external representation, otherwise the behavior is undefined.
   * </p>
   * @return A {@link PdfToken} value.
   * @since 1.56.3
   */
  PdfToken getObject();
  
  /**
   * <p>
   * If {@link #isOperator()} is <b>true</b>, downcasts this token to
   * its external representation, otherwise the behavior is undefined.
   * </p>
   * @return A {@link String} value.
   * @since 1.56
   */
  String getOperator();
  
  /**
   * <p>
   * If {@link #isString()} is <b>true</b>, downcasts this token to
   * its external representation, otherwise the behavior is undefined.
   * </p>
   * @return A {@link String} value.
   * @since 1.56
   */
  String getString();
  
  /**
   * <p>
   * Determines if this token is a PDF array.
   * @return <code>true</code> if and only if this token is a PDF
   *         array.
   * @since 1.56
   */
  boolean isArray();
  
  /**
   * <p>
   * Determines if this token is a PDF boolean.
   * @return <code>true</code> if and only if this token is a PDF
   *         boolean.
   * @since 1.56
   */
  boolean isBoolean();
  
  /**
   * <p>
   * Determines if this token is a PDF dictionary.
   * @return <code>true</code> if and only if this token is a PDF
   *         dictionary.
   * @since 1.56
   */
  boolean isDictionary();
  
  /**
   * <p>
   * Determines if this token is a PDF float.
   * @return <code>true</code> if and only if this token is a PDF
   *         float.
   * @since 1.56
   */
  boolean isFloat();
  
  /**
   * <p>
   * Determines if this token is a PDF integer.
   * @return <code>true</code> if and only if this token is a PDF
   *         integer.
   * @since 1.56
   */
  boolean isInteger();
  
  /**
   * <p>
   * Determines if this token is a PDF name.
   * @return <code>true</code> if and only if this token is a PDF
   *         name.
   * @since 1.56
   */
  boolean isName();
  
  /**
   * <p>
   * Determines if this token is a PDF null object.
   * @return <code>true</code> if and only if this token is a PDF
   *         null object.
   * @since 1.56
   */
  boolean isNull();
  
  /**
   * <p>
   * Determines if this token is a PDF object.
   * </p>
   * @return <code>true</code> if and only if this token is a PDF
   *         object.
   * @since 1.56.3
   */
  boolean isObject();
  
  /**
   * <p>
   * Determines if this token is a PDF operator.
   * @return <code>true</code> if and only if this token is a PDF
   *         operator.
   * @since 1.56
   */
  boolean isOperator();
  
  /**
   * <p>
   * Determines if this token is a PDF string.
   * @return <code>true</code> if and only if this token is a PDF
   *         string.
   * @since 1.56
   */
  boolean isString();
  
}
