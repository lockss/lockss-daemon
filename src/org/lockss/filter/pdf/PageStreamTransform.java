/*
 * $Id: PageStreamTransform.java,v 1.12 2007-11-08 21:03:15 thib_gc Exp $
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.filter.pdf;

import java.io.*;
import java.util.*;

import org.lockss.util.*;
import org.pdfbox.pdfwriter.ContentStreamWriter;
import org.pdfbox.pdmodel.common.PDStream;
import org.pdfbox.util.PDFStreamEngine;
import org.pdfbox.util.PDFStreamEngine.OperatorProcessorFactory;
import org.pdfbox.util.operator.OperatorProcessor;

/**
 * <p>A PDF stream engine (PDF token stream processor) written as a
 * PDF page transform and based on {@link PdfOperatorProcessor}
 * rather than {@link OperatorProcessor}.</p>
 * <p>A PDF page stream transform iterates through a PDF page's token
 * stream, calls a PDF operator processor for each operator
 * encountered, and builds a transformed list of tokens (based on
 * actions taken by the operator processors). The PDF page stream
 * transform is configured using a {@link Properties} object which
 * maps PDF operator strings from {@link PdfUtil#getPdfOperators} to
 * the string name of a PDF operator processor class to use when the
 * operator is encountered. Any such PDF operator string not mapped
 * to a class name string in the {@link Properties} instance used to
 * configure the PDF page stream transform at instantiation will be
 * processed by default by {@link SimpleOperatorProcessor}, which
 * just adds its operands and operator unchanged to the output
 * list.</p>
 * <p>The PDF page stream transform maintains a stack of output lists
 * rather than a single output list. However, when {@link #transform}
 * ends it <em>must</em> be the case that the result of all the
 * operations of the PDF operator processors are such that there is
 * only one output list on the stack, or a
 * {@link PageTransformException} will be thrown. This remaining
 * list is used as the result to rewrite the token stream of the PDF
 * page, if any PDF operator processor has indicated to the transform
 * that it has enacted any change in the output list compared to the
 * original token stream by calling {@link #signalChange} (otherwise
 * the PDF page is unchanged).</p>
 * <p>The equivalent of {@link Stack#peek} is {@link #getOutputList}
 * which is used by PDF operator processors to obtain the current
 * output list, to which they can add tokens. To cause a new, empty
 * list to be pushed onto the stack (akin to {@link Stack#push}),
 * PDF operator processors call {@link #splitOutputList}. Splitting
 * from the list currently on top of the stack is useful to introduce
 * a subpart of the token stream, for example to bracket a subpart
 * from an opening marker up to some closing marker. Typically this
 * is used to isolate
 * {@link PdfUtil#BEGIN_TEXT_OBJECT}/{@link PdfUtil#END_TEXT_OBJECT}
 * blocks.</p>
 * <p>A PDF operator processor can decide to attach the list currently
 * at the top of the stack to the end of the list underneath (a kind
 * of {@link Stack#pop} operation called "merge"). If
 * {@link #mergeOutputList()} is called, the list currently at the top
 * of the stack is added to the end of the list underneath, and the
 * result becomes the new top of the list stack. If
 * {@link #mergeOutputList(List)} is called, the replacement list is
 * used instead of the current top of the stack, which is discarded.
 * This is useful to provide a transformed result for an entire
 * subpart of the stream previously isolated with a split.</p>
 * <p>{@link PdfOperatorProcessor} instances, like
 * {@link OperatorProcessor} instances, <em>must</em> have a
 * no-argument constructor, and are instantiated once per key
 * associated with their class name during a given
 * {@link PageStreamTransform} instantiation.</p>
 * @author Thib Guicherd-Callin
 * @see PDFStreamEngine
 * @see PdfOperatorProcessor
 * @see SplitOperatorProcessor
 */
public class PageStreamTransform extends PDFStreamEngine implements PageTransform {

  /**
   * <p>A page stream transform that never writes back its output list
   * to the PDF page, even if at least one change has been signaled to
   * it by way of {@link PageStreamTransform#signalChange}.</p>
   * @author Thib Guicherd-Callin
   */
  public static class NullPageStreamTransform extends PageStreamTransform {

    /**
     * <p>Builds a new null identity PDF page stream transform that processes
     * all PDF operators with {@link SimpleOperatorProcessor}.</p>
     * @throws IOException if any processing error occurs.
     * @see PageStreamTransform#PageStreamTransform()
     */
    public NullPageStreamTransform() throws IOException {
      super();
    }

    /**
     * <p>Builds a new page stream transform, using the given class
     * name strings to instantiate PDF operator processors for the given
     * PDF operator strings, and imposing a default of
     * {@link SimpleOperatorProcessor} for all others.</p>
     * <p>Though most implementations of {@link PDFStreamEngine} do
     * not, this implementation takes advantage of a {@link Properties}
     * argument that uses the defaults mechanism of the
     * {@link Properties#Properties(Properties)} constructor.</p>
     * @param customOperatorProcessors A {@link Properties} instance
     *                                 that maps PDF operator strings
     *                                 to the string names of
     *                                 {@link PdfOperatorProcessor}
     *                                 classes to use for those PDF
     *                                 operators.
     * @throws NullPointerException if the argument is null.
     * @throws IOException          if any processing error occurs.
     * @see PageStreamTransform#PageStreamTransform(Properties)
     */
    public NullPageStreamTransform(Properties customOperatorProcessors) throws IOException {
      super(customOperatorProcessors);
    }

    /**
     * <p>Builds a new null page stream transform using the given
     * PDF operator processors (represented by its class) to process the
     * given PDF operators (represented by its operator string),
     * and imposing a default of {@link SimpleOperatorProcessor} for all
     * others.</p>
     * @param pdfOperatorString    A PDF operator string.
     * @param pdfOperatorProcessor A PDF operator processor class for
     *                             <code>pdfOperatorString</code>.
     * @throws IOException if any processing error occurs.
     * @see PageStreamTransform#PageStreamTransform(String, Class, String, Class)
     */
    public NullPageStreamTransform(String pdfOperatorString, Class pdfOperatorProcessor)
        throws IOException {
      super(pdfOperatorString, pdfOperatorProcessor);
    }

    /**
     * <p>Builds a new null page stream transform using the given
     * PDF operator processors (represented by their class) to process the
     * given PDF operators (represented by their operator string),
     * and imposing a default of {@link SimpleOperatorProcessor} for all
     * others.</p>
     * @param pdfOperatorString1    A PDF operator string.
     * @param pdfOperatorProcessor1 A PDF operator processor class for
     *                              <code>pdfOperatorString1</code>.
     * @param pdfOperatorString2    A PDF operator string.
     * @param pdfOperatorProcessor2 A PDF operator processor class for
     *                              <code>pdfOperatorString2</code>.
     * @throws IOException if any processing error occurs.
     * @see PageStreamTransform#PageStreamTransform(String, Class, String, Class)
     */
    public NullPageStreamTransform(String pdfOperatorString1, Class pdfOperatorProcessor1,
                                   String pdfOperatorString2, Class pdfOperatorProcessor2)
        throws IOException {
      super(pdfOperatorString1, pdfOperatorProcessor1,
            pdfOperatorString2, pdfOperatorProcessor2);
    }

    /**
     * <p>Builds a new null page stream transform using the given
     * PDF operator processors (represented by their class) to process the
     * given PDF operators (represented by their operator string),
     * and imposing a default of {@link SimpleOperatorProcessor} for all
     * others.</p>
     * @param pdfOperatorString1    A PDF operator string.
     * @param pdfOperatorProcessor1 A PDF operator processor class for
     *                              <code>pdfOperatorString1</code>.
     * @param pdfOperatorString2    A PDF operator string.
     * @param pdfOperatorProcessor2 A PDF operator processor class for
     *                              <code>pdfOperatorString2</code>.
     * @param pdfOperatorString3    A PDF operator string.
     * @param pdfOperatorProcessor3 A PDF operator processor class for
     *                              <code>pdfOperatorString3</code>.
     * @throws IOException if any processing error occurs.
     * @see PageStreamTransform#PageStreamTransform(String, Class, String, Class, String, Class)
     */
    public NullPageStreamTransform(String pdfOperatorString1, Class pdfOperatorProcessor1,
                                   String pdfOperatorString2, Class pdfOperatorProcessor2,
                                   String pdfOperatorString3, Class pdfOperatorProcessor3)
        throws IOException {
      super(pdfOperatorString1, pdfOperatorProcessor1,
            pdfOperatorString2, pdfOperatorProcessor2,
            pdfOperatorString3, pdfOperatorProcessor3);
    }

    /**
     * <p>Builds a new null page stream transform using the given
     * PDF operator processors (represented by their class) to process the
     * given PDF operators (represented by their operator string),
     * and imposing a default of {@link SimpleOperatorProcessor} for all
     * others.</p>
     * @param pdfOperatorString1    A PDF operator string.
     * @param pdfOperatorProcessor1 A PDF operator processor class for
     *                              <code>pdfOperatorString1</code>.
     * @param pdfOperatorString2    A PDF operator string.
     * @param pdfOperatorProcessor2 A PDF operator processor class for
     *                              <code>pdfOperatorString2</code>.
     * @param pdfOperatorString3    A PDF operator string.
     * @param pdfOperatorProcessor3 A PDF operator processor class for
     *                              <code>pdfOperatorString3</code>.
     * @param pdfOperatorString4    A PDF operator string.
     * @param pdfOperatorProcessor4 A PDF operator processor class for
     *                              <code>pdfOperatorString4</code>.
     * @throws IOException if any processing error occurs.
     * @see PageStreamTransform#PageStreamTransform(String, Class, String, Class, String, Class, String, Class)
     */
    public NullPageStreamTransform(String pdfOperatorString1, Class pdfOperatorProcessor1,
                                   String pdfOperatorString2, Class pdfOperatorProcessor2,
                                   String pdfOperatorString3, Class pdfOperatorProcessor3,
                                   String pdfOperatorString4, Class pdfOperatorProcessor4)
        throws IOException {
      super(pdfOperatorString1, pdfOperatorProcessor1,
            pdfOperatorString2, pdfOperatorProcessor2,
            pdfOperatorString3, pdfOperatorProcessor3,
            pdfOperatorString4, pdfOperatorProcessor4);
    }

    protected synchronized void writeResult(PdfPage pdfPage) throws IOException {
      logger.debug2("Null page stream transform; skipping writing the result");
    }

  }

  /**
   * <p>Whether a change has been indicated by a PDF operator
   * processor since the begininng of the current/latest call to
   * {@link #transform}.</p>
   * @see #reset
   */
  protected boolean atLeastOneChange;

  protected PdfPage currentPdfPage;

  /**
   * <p>The output list stack.</p>
   */
  protected Stack /* of ArrayList */ listStack;

  /**
   * <p>Builds a new identity PDF page stream transform that processes
   * all PDF operators with {@link SimpleOperatorProcessor}.</p>
   * @throws IOException if any processing error occurs.
   * @see #PageStreamTransform(Properties)
   * @deprecated Use {@link #PageStreamTransform(OperatorProcessorFactory)}.
   */
  public PageStreamTransform() throws IOException {
    this(new Properties());
  }

  /**
   * <p>Builds a new identity PDF page stream transform that processes
   * all PDF operators with {@link SimpleOperatorProcessor}.</p>
   * @param factory A {@link PdfOperatorProcessor} factory.
   * @throws IOException if any processing error occurs.
   * @see #PageStreamTransform(OperatorProcessorFactory, Properties)
   */
  public PageStreamTransform(OperatorProcessorFactory factory) throws IOException {
    this(factory, new Properties());
  }

  /**
   * <p>Builds a new page stream transform, using the given class
   * name strings to instantiate PDF operator processors for the given
   * PDF operator strings, and imposing a default of
   * {@link SimpleOperatorProcessor} for all others.</p>
   * <p>Though most implementations of {@link PDFStreamEngine} do
   * not, this implementation takes advantage of a {@link Properties}
   * argument that uses the defaults mechanism of the
   * {@link Properties#Properties(Properties)} constructor.</p>
   * @param customOperatorProcessors A {@link Properties} instance
   *                                 that maps PDF operator strings
   *                                 to the string names of
   *                                 {@link PdfOperatorProcessor}
   *                                 classes to use for those PDF
   *                                 operators.
   * @throws NullPointerException if the argument is null.
   * @throws IOException          if any processing error occurs.
   * @see PDFStreamEngine#PDFStreamEngine(Properties)
   * @see PdfUtil#getPdfOperators
   * @see <a href="http://sourceforge.net/tracker/index.php?func=detail&aid=1544943&group_id=78314&atid=552832">PDFBox
   *      Bug #1544943</a>
   * @deprecated Use {@link #PageStreamTransform(OperatorProcessorFactory, Properties)}.
   */
  public PageStreamTransform(Properties customOperatorProcessors) throws IOException {
    super(rewriteProperties(customOperatorProcessors));
    this.listStack = new Stack();
  }

  /**
   * <p>Builds a new page stream transform, using the given class
   * name strings to instantiate PDF operator processors for the given
   * PDF operator strings, and imposing a default of
   * {@link SimpleOperatorProcessor} for all others.</p>
   * <p>Though most implementations of {@link PDFStreamEngine} do
   * not, this implementation takes advantage of a {@link Properties}
   * argument that uses the defaults mechanism of the
   * {@link Properties#Properties(Properties)} constructor.</p>
   * @param factory                  A {@link PdfOperatorProcessor}
   *                                 factory.
   * @param customOperatorProcessors A {@link Properties} instance
   *                                 that maps PDF operator strings
   *                                 to the string names of
   *                                 {@link PdfOperatorProcessor}
   *                                 classes to use for those PDF
   *                                 operators.
   * @throws NullPointerException if the argument is null.
   * @throws IOException          if any processing error occurs.
   * @see PDFStreamEngine#PDFStreamEngine(OperatorProcessorFactory, Properties)
   * @see PdfUtil#getPdfOperators
   * @see <a href="http://sourceforge.net/tracker/index.php?func=detail&aid=1544943&group_id=78314&atid=552832">PDFBox
   *      Bug #1544943</a>
   */
  public PageStreamTransform(OperatorProcessorFactory factory,
                             Properties customOperatorProcessors) throws IOException {
    super(factory, rewriteProperties(customOperatorProcessors));
    this.listStack = new Stack();
  }

  /**
   * <p>Builds a new page stream transform using the given
   * PDF operator processor (represented by its class) to process the
   * given PDF operator (represented by its operator string),
   * and imposing a default of {@link SimpleOperatorProcessor} for all
   * others.</p>
   * @param pdfOperatorString    A PDF operator string.
   * @param pdfOperatorProcessor A PDF operator processor class for
   *                             <code>pdfOperatorString</code>..
   * @throws IOException if any processing error occurs.
   * @see #PageStreamTransform(Properties)
   * @see PropUtil#fromArgs(String, String)
   * @deprecated Use {@link #PageStreamTransform(OperatorProcessorFactory, String, Class)}.
   */
  public PageStreamTransform(String pdfOperatorString, Class pdfOperatorProcessor)
      throws IOException {
    this(PropUtil.fromArgs(pdfOperatorString, pdfOperatorProcessor.getName()));
  }

  /**
   * <p>Builds a new page stream transform using the given
   * PDF operator processor (represented by its class) to process the
   * given PDF operator (represented by its operator string),
   * and imposing a default of {@link SimpleOperatorProcessor} for all
   * others.</p>
   * @param factory              A {@link PdfOperatorProcessor}
   *                             factory.
   * @param pdfOperatorString    A PDF operator string.
   * @param pdfOperatorProcessor A PDF operator processor class for
   *                             <code>pdfOperatorString</code>..
   * @throws IOException if any processing error occurs.
   * @see #PageStreamTransform(OperatorProcessorFactory, Properties)
   * @see PropUtil#fromArgs(String, String)
   */
  public PageStreamTransform(OperatorProcessorFactory factory,
                             String pdfOperatorString, Class pdfOperatorProcessor)
      throws IOException {
    this(factory,
         PropUtil.fromArgs(pdfOperatorString, pdfOperatorProcessor.getName()));
  }

  /**
   * <p>Builds a new page stream transform using the given
   * PDF operator processors (represented by their class) to process the
   * given PDF operators (represented by their operator string),
   * and imposing a default of {@link SimpleOperatorProcessor} for all
   * others.</p>
   * @param pdfOperatorString1    A PDF operator string.
   * @param pdfOperatorProcessor1 A PDF operator processor class for
   *                              <code>pdfOperatorString1</code>.
   * @param pdfOperatorString2    A PDF operator string.
   * @param pdfOperatorProcessor2 A PDF operator processor class for
   *                              <code>pdfOperatorString2</code>.
   * @throws IOException if any processing error occurs.
   * @see #PageStreamTransform(Properties)
   * @see PropUtil#fromArgs(String, String, String, String)
   * @deprecated Use {@link #PageStreamTransform(OperatorProcessorFactory, String, Class, String, Class)}.
   */
  public PageStreamTransform(String pdfOperatorString1, Class pdfOperatorProcessor1,
                             String pdfOperatorString2, Class pdfOperatorProcessor2)
      throws IOException {
    this(PropUtil.fromArgs(pdfOperatorString1, pdfOperatorProcessor1.getName(),
                           pdfOperatorString2, pdfOperatorProcessor2.getName()));
  }

  /**
   * <p>Builds a new page stream transform using the given
   * PDF operator processors (represented by their class) to process the
   * given PDF operators (represented by their operator string),
   * and imposing a default of {@link SimpleOperatorProcessor} for all
   * others.</p>
   * @param factory               A {@link PdfOperatorProcessor}
   *                              factory.
   * @param pdfOperatorString1    A PDF operator string.
   * @param pdfOperatorProcessor1 A PDF operator processor class for
   *                              <code>pdfOperatorString1</code>.
   * @param pdfOperatorString2    A PDF operator string.
   * @param pdfOperatorProcessor2 A PDF operator processor class for
   *                              <code>pdfOperatorString2</code>.
   * @throws IOException if any processing error occurs.
   * @see #PageStreamTransform(OperatorProcessorFactory, Properties)
   * @see PropUtil#fromArgs(String, String, String, String)
   */
  public PageStreamTransform(OperatorProcessorFactory factory,
                             String pdfOperatorString1, Class pdfOperatorProcessor1,
                             String pdfOperatorString2, Class pdfOperatorProcessor2)
      throws IOException {
    this(factory,
         PropUtil.fromArgs(pdfOperatorString1, pdfOperatorProcessor1.getName(),
                           pdfOperatorString2, pdfOperatorProcessor2.getName()));
  }

  /**
   * <p>Builds a new page stream transform using the given
   * PDF operator processors (represented by their class) to process the
   * given PDF operators (represented by their operator string),
   * and imposing a default of {@link SimpleOperatorProcessor} for all
   * others.</p>
   * @param pdfOperatorString1    A PDF operator string.
   * @param pdfOperatorProcessor1 A PDF operator processor class for
   *                              <code>pdfOperatorString1</code>.
   * @param pdfOperatorString2    A PDF operator string.
   * @param pdfOperatorProcessor2 A PDF operator processor class for
   *                              <code>pdfOperatorString2</code>.
   * @param pdfOperatorString3    A PDF operator string.
   * @param pdfOperatorProcessor3 A PDF operator processor class for
   *                              <code>pdfOperatorString3</code>.
   * @throws IOException if any processing error occurs.
   * @see #PageStreamTransform(Properties)
   * @see PropUtil#fromArgs(String, String, String, String, String, String)
   * @deprecated Use {@link #PageStreamTransform(OperatorProcessorFactory, String, Class, String, Class, String, Class)}.
   */
  public PageStreamTransform(String pdfOperatorString1, Class pdfOperatorProcessor1,
                             String pdfOperatorString2, Class pdfOperatorProcessor2,
                             String pdfOperatorString3, Class pdfOperatorProcessor3)
      throws IOException {
    this(PropUtil.fromArgs(pdfOperatorString1, pdfOperatorProcessor1.getName(),
                           pdfOperatorString2, pdfOperatorProcessor2.getName(),
                           pdfOperatorString3, pdfOperatorProcessor3.getName()));
  }

  /**
   * <p>Builds a new page stream transform using the given
   * PDF operator processors (represented by their class) to process the
   * given PDF operators (represented by their operator string),
   * and imposing a default of {@link SimpleOperatorProcessor} for all
   * others.</p>
   * @param factory               A {@link PdfOperatorProcessor}
   *                              factory.
   * @param pdfOperatorString1    A PDF operator string.
   * @param pdfOperatorProcessor1 A PDF operator processor class for
   *                              <code>pdfOperatorString1</code>.
   * @param pdfOperatorString2    A PDF operator string.
   * @param pdfOperatorProcessor2 A PDF operator processor class for
   *                              <code>pdfOperatorString2</code>.
   * @param pdfOperatorString3    A PDF operator string.
   * @param pdfOperatorProcessor3 A PDF operator processor class for
   *                              <code>pdfOperatorString3</code>.
   * @throws IOException if any processing error occurs.
   * @see #PageStreamTransform(OperatorProcessorFactory, Properties)
   * @see PropUtil#fromArgs(String, String, String, String, String, String)
   */
  public PageStreamTransform(OperatorProcessorFactory factory,
                             String pdfOperatorString1, Class pdfOperatorProcessor1,
                             String pdfOperatorString2, Class pdfOperatorProcessor2,
                             String pdfOperatorString3, Class pdfOperatorProcessor3)
      throws IOException {
    this(factory,
         PropUtil.fromArgs(pdfOperatorString1, pdfOperatorProcessor1.getName(),
                           pdfOperatorString2, pdfOperatorProcessor2.getName(),
                           pdfOperatorString3, pdfOperatorProcessor3.getName()));
  }

  /**
   * <p>Builds a new page stream transform using the given
   * PDF operator processors (represented by their class) to process the
   * given PDF operators (represented by their operator string),
   * and imposing a default of {@link SimpleOperatorProcessor} for all
   * others.</p>
   * @param pdfOperatorString1    A PDF operator string.
   * @param pdfOperatorProcessor1 A PDF operator processor class for
   *                              <code>pdfOperatorString1</code>.
   * @param pdfOperatorString2    A PDF operator string.
   * @param pdfOperatorProcessor2 A PDF operator processor class for
   *                              <code>pdfOperatorString2</code>.
   * @param pdfOperatorString3    A PDF operator string.
   * @param pdfOperatorProcessor3 A PDF operator processor class for
   *                              <code>pdfOperatorString3</code>.
   * @param pdfOperatorString4    A PDF operator string.
   * @param pdfOperatorProcessor4 A PDF operator processor class for
   *                              <code>pdfOperatorString4</code>.
   * @throws IOException if any processing error occurs.
   * @see #PageStreamTransform(OperatorProcessorFactory, Properties)
   * @see PropUtil#fromArgs(String, String, String, String, String, String, String, String)
   * @deprecated Use {@link #PageStreamTransform(OperatorProcessorFactory, String, Class, String, Class, String, Class, String, Class)}
   */
  public PageStreamTransform(String pdfOperatorString1, Class pdfOperatorProcessor1,
                             String pdfOperatorString2, Class pdfOperatorProcessor2,
                             String pdfOperatorString3, Class pdfOperatorProcessor3,
                             String pdfOperatorString4, Class pdfOperatorProcessor4)
      throws IOException {
    this(PropUtil.fromArgs(pdfOperatorString1, pdfOperatorProcessor1.getName(),
                           pdfOperatorString2, pdfOperatorProcessor2.getName(),
                           pdfOperatorString3, pdfOperatorProcessor3.getName(),
                           pdfOperatorString4, pdfOperatorProcessor4.getName()));
  }

  /**
   * <p>Builds a new page stream transform using the given
   * PDF operator processors (represented by their class) to process the
   * given PDF operators (represented by their operator string),
   * and imposing a default of {@link SimpleOperatorProcessor} for all
   * others.</p>
   * @param factory               A {@link PdfOperatorProcessor}
   *                              factory.
   * @param pdfOperatorString1    A PDF operator string.
   * @param pdfOperatorProcessor1 A PDF operator processor class for
   *                              <code>pdfOperatorString1</code>.
   * @param pdfOperatorString2    A PDF operator string.
   * @param pdfOperatorProcessor2 A PDF operator processor class for
   *                              <code>pdfOperatorString2</code>.
   * @param pdfOperatorString3    A PDF operator string.
   * @param pdfOperatorProcessor3 A PDF operator processor class for
   *                              <code>pdfOperatorString3</code>.
   * @param pdfOperatorString4    A PDF operator string.
   * @param pdfOperatorProcessor4 A PDF operator processor class for
   *                              <code>pdfOperatorString4</code>.
   * @throws IOException if any processing error occurs.
   * @see #PageStreamTransform(Properties)
   * @see PropUtil#fromArgs(String, String, String, String, String, String, String, String)
   */
  public PageStreamTransform(OperatorProcessorFactory factory,
                             String pdfOperatorString1, Class pdfOperatorProcessor1,
                             String pdfOperatorString2, Class pdfOperatorProcessor2,
                             String pdfOperatorString3, Class pdfOperatorProcessor3,
                             String pdfOperatorString4, Class pdfOperatorProcessor4)
      throws IOException {
    this(factory,
         PropUtil.fromArgs(pdfOperatorString1, pdfOperatorProcessor1.getName(),
                           pdfOperatorString2, pdfOperatorProcessor2.getName(),
                           pdfOperatorString3, pdfOperatorProcessor3.getName(),
                           pdfOperatorString4, pdfOperatorProcessor4.getName()));
  }

  public synchronized boolean getChangeFlag() {
    return atLeastOneChange;
  }

  public PdfPage getCurrentPdfPage() {
    return currentPdfPage;
  }

  /**
   * <p>Gets the output list currently at the top of the output list
   * stack.</p>
   * @return The list of tokens currently at the top of the list stack.
   * @see #splitOutputList
   * @see #mergeOutputList()
   * @see #mergeOutputList(List)
   */
  public synchronized List getOutputList() {
    return (List)listStack.peek();
  }

  /**
   * <p>Pops the output list currently at the top of the output list
   * stack and appends it to the end of the output list immediately
   * underneath.</p>
   * @throws EmptyStackException if there is currently only one output
   *                             list on the stack.
   * @see #mergeOutputList(List)
   */
  public synchronized void mergeOutputList() {
    logger.debug3("Merging");
    List oldTop = (List)listStack.pop();
    List newTop = (List)listStack.peek();
    newTop.addAll(oldTop);
  }

  /**
   * <p>Pops the output list currently at the top of the output list
   * stack and discards it, and appends the given replacement list to
   * the output list immediately underneath.</p>
   * @param replacement A list of tokens to be appended to the output
   *                    list currently immediately under the top of
   *                    the stack.
   * @throws EmptyStackException if there is currently only one output
   *                             list on the stack.
   */
  public synchronized void mergeOutputList(List replacement) {
    logger.debug3("Merging with replacement");
    listStack.pop(); // discard result
    List newTop = (List)listStack.peek();
    newTop.addAll(replacement);
  }

  /**
   * <p>Resets the state of this transform; this clears the stack and
   * pushes an empty list onto it, and clears the flag that indicates
   * there has been at least one change.</p>
   * <p>This method is called at the beginning of
   * {@link #transform}.</p>
   * <p>This method is <em>not</em> called at the end of
   * {@link #transform}. Though the contents of the resulting output
   * list may have been written to the PDF page already, clients
   * may wish to inspect the final result without having to re-parse
   * the token stream of the PDF page. However, the result list may be
   * quite large and the stack will hold on to it until the next call
   * to this method, which may represent a memory issue. Thus clients
   * <em>may</em> call {@link #reset} after a call to
   * {@link #transform} to clear the output list stack.</p>
   */
  public synchronized void reset() {
    logger.debug3("Resetting the page stream transform");
    atLeastOneChange = false;
    listStack.clear();
    listStack.push(new ArrayList()); // FIXME: initial capacity?
  }

  public synchronized void setChangeFlag(boolean atLeastOneChange) {
    logger.debug3(atLeastOneChange ? "Change flag set" : "Change flag cleared");
    this.atLeastOneChange = atLeastOneChange;
  }

  /**
   * <p>Notifies this transform that there has been a change in the
   * output list compared to the original token stream.</p>
   * <p>After a call to this method, the token stream of the PDF
   * page being transformed will always be replaced with the output
   * list.</p>
   */
  public synchronized void signalChange() {
    logger.debug3("Change signaled");
    atLeastOneChange = true;
  }

  /**
   * <p>Pushes an empty list onto the output list stack.</p>
   */
  public synchronized void splitOutputList() {
    logger.debug3("Splitting");
    listStack.push(new ArrayList());
  }

  /**
   * <p>Applies this transform to a PDF page.</p>
   * <p>Using the configured PDF operator processors, a PDF stream
   * engine ({@link PDFStreamEngine} parent class) iterates through
   * the page's token stream, aggregating operands and invoking the
   * appropriate PDF operator processor for each PDF operator
   * encountered. An output list of tokens is created. At the end of
   * the iteration, if any PDF operator processor has indicated to
   * this transform that it has changed the output list compared to
   * the original stream, the PDF page's contents are replaced with
   * those of the output list (otherwise the PDF page is
   * unchanged).</p>
   * @param pdfPage     A PDF page (belonging to the PDF document).
   * @throws PageTransformException if the output list stack does
   *                                   not have exactly one output
   *                                   list at the end of the
   *                                   transform.
   * @throws IOException               if any other processing error
   *                                   occurs.
   * @see PDFStreamEngine#processStream
   * @see #reset
   */
  public synchronized boolean transform(PdfPage pdfPage)
      throws IOException {
    logger.debug3("Begin page stream transform");

    // Iterate over stream
    reset();
    this.currentPdfPage = pdfPage;
    processStream(pdfPage.getPdPage(),
                  pdfPage.findResources(),
                  pdfPage.getContentStream());

    // Sanity check
    if (listStack.size() != 1) {
      String logMessage = "Split/merge mismatch: after processing stream, list stack has size " + listStack.size();
      logger.error(logMessage);
      throw new PageTransformException(logMessage);
    }

    writeResult(pdfPage);
    logger.debug2("Page stream transform result: " + atLeastOneChange);
    return atLeastOneChange;
  }

  /**
   * <p>Rewrites the contents of the PDF page being transformed with
   * those of the output list, if at least one change has been
   * indicated.</p>
   * @param pdfPage     A PDF page (belonging to the PDF document).
   * @throws IOException if any processing error occurs.
   */
  protected synchronized void writeResult(PdfPage pdfPage)
      throws IOException {
    if (atLeastOneChange) {
      logger.debug3("Writing result to PDF page");
      PDStream resultStream = pdfPage.getPdfDocument().makePdStream();
      OutputStream outputStream = resultStream.createOutputStream();
      ContentStreamWriter tokenWriter = new ContentStreamWriter(outputStream);
      tokenWriter.writeTokens(getOutputList());
      pdfPage.setContents(resultStream);
    }
    else {
      logger.debug3("No result to write to PDF page");
    }
  }

  /**
   * <p>A logger for use by this class.</p>
   */
  private static Logger logger = Logger.getLogger("PageStreamTransform");

  public static Properties rewriteProperties(Properties customOperatorProcessors,
                                             String defaultOperatorProcessorClassName) {

    if (customOperatorProcessors == null) {
      throw new NullPointerException("Custom operator processors cannot be specified by a null Properties instance.");
    }
    Properties properties = new Properties();
    for (Iterator iter = PdfUtil.getPdfOperators() ; iter.hasNext() ; ) {
      String key = (String)iter.next();
      properties.setProperty(key,
                             customOperatorProcessors.getProperty(key,
                                                                  defaultOperatorProcessorClassName));
    }
    return properties;
  }

  /**
   * <p>Assembles a new {@link Properties} object that maps keys from
   * {@link PdfUtil#getPdfOperators} not found in the argument to
   * {@link SimpleOperatorProcessor}, flattening the recursive
   * defaults at the same time.</p>
   * @param customOperatorProcessors A {@link Properties} instance
   *                                 that maps PDF operator strings
   *                                 to the string names of
   *                                 {@link PdfOperatorProcessor}
   *                                 classes to use for those PDF
   *                                 operators.
   * @return A flattened {@link Properties} instance that maps each
   *         key of {@link PdfUtil#getPdfOperators} to either the
   *         argument's value for that key (found recursively) or to
   *         {@link SimpleOperatorProcessor}.
   * @throws NullPointerException if the argument is null
   */
  protected static Properties rewriteProperties(Properties customOperatorProcessors) {
    return rewriteProperties(customOperatorProcessors, SimpleOperatorProcessor.class.getName());
  }

}
