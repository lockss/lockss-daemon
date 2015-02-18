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
   * If {@link #isString()} is <b>true</b>, downcasts this token to
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
