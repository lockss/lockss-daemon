/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

import javax.xml.namespace.QName;
import javax.xml.xpath.*;

import org.w3c.dom.*;

/**
 * <p>
 * Provides utilities related to XPath processing.
 * </p>
 * <p>
 * The work horse of {@link XPathExpression},
 * {@link XPathExpression#evaluate(Object, QName)}, returns an {@link Object} of
 * a type implied by one of five {@link QName} constants in
 * {@link XPathConstants}. To avoid having to pair each {@link XPathExpression}
 * instance with a {@link QName} constant and remember the correct cast in
 * response, use the explicitly named and typed methods
 * {@link #evaluateBoolean(XPathExpression, Object)},
 * {@link #evaluateNode(XPathExpression, Object)},
 * {@link #evaluateNodeSet(XPathExpression, Object)},
 * {@link #evaluateNumber(XPathExpression, Object)} and
 * {@link #evaluateString(XPathExpression, Object)}.
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.67.5
 */
public class XPathUtil {

  /**
   * <p>
   * Evaluates the given XPath expression in the context of the given item, with
   * the expectation of a boolean result.
   * </p>
   * 
   * @param xpe
   *          An XPath expression.
   * @param item
   *          A context item.
   * @return The result of the evaluation as a {@link Boolean} instance.
   * @throws XPathExpressionException
   *           if the evaluation fails
   * @since 1.67.5
   * @see XPathConstants#BOOLEAN
   */
  public static Boolean evaluateBoolean(XPathExpression xpe,
                                        Object item)
      throws XPathExpressionException {
    return (Boolean)xpe.evaluate(item, XPathConstants.BOOLEAN);
  }

  /**
   * <p>
   * Evaluates the given XPath expression in the context of the given item, with
   * the expectation of a node result.
   * </p>
   * 
   * @param xpe
   *          An XPath expression.
   * @param item
   *          A context item.
   * @return The result of the evaluation as a {@link Node} instance.
   * @throws XPathExpressionException
   *           if the evaluation fails
   * @since 1.67.5
   * @see XPathConstants#NODE
   */
  public static Node evaluateNode(XPathExpression xpe,
                                  Object item)
      throws XPathExpressionException {
    return (Node)xpe.evaluate(item, XPathConstants.NODE);
  }

  /**
   * <p>
   * Evaluates the given XPath expression in the context of the given item, with
   * the expectation of a node set result.
   * </p>
   * 
   * @param xpe
   *          An XPath expression.
   * @param item
   *          A context item.
   * @return The result of the evaluation as a {@link NodeSet} instance.
   * @throws XPathExpressionException
   *           if the evaluation fails
   * @since 1.67.5
   * @see XPathConstants#NODESET
   */
  public static NodeList evaluateNodeSet(XPathExpression xpe,
                                          Object item)
      throws XPathExpressionException {
    return (NodeList)xpe.evaluate(item, XPathConstants.NODESET);
  }

  /**
   * <p>
   * Evaluates the given XPath expression in the context of the given item, with
   * the expectation of a number (Java <code>double</code>) result.
   * </p>
   * 
   * @param xpe
   *          An XPath expression.
   * @param item
   *          A context item.
   * @return The result of the evaluation as a {@link Double} instance.
   * @throws XPathExpressionException
   *           if the evaluation fails
   * @since 1.67.5
   * @see XPathConstants#NUMBER
   */
  public static Double evaluateNumber(XPathExpression xpe,
                                      Object item)
      throws XPathExpressionException {
    return (Double)xpe.evaluate(item, XPathConstants.NUMBER);
  }

  /**
   * <p>
   * Evaluates the given XPath expression in the context of the given item, with
   * the expectation of a string result.
   * </p>
   * 
   * @param xpe
   *          An XPath expression.
   * @param item
   *          A context item.
   * @return The result of the evaluation as a {@link String}.
   * @throws XPathExpressionException
   *           if the evaluation fails
   * @since 1.67.5
   * @see XPathConstants#STRING
   */
  public static String evaluateString(XPathExpression xpe,
                                      Object item)
      throws XPathExpressionException {
    return (String)xpe.evaluate(item, XPathConstants.STRING);
  }

  /**
   * <p>
   * This class cannot be instantiated.
   * </p>
   * 
   * @since 1.67.5
   */
  private XPathUtil() {
    // Prevent instantiation
  }
  
}
