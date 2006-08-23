/*
 * $Id: PdfPageStreamTransform.java,v 1.1 2006-08-23 19:14:06 thib_gc Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.filter.pdf.*;
import org.lockss.util.*;
import org.pdfbox.pdfwriter.ContentStreamWriter;
import org.pdfbox.pdmodel.PDPage;
import org.pdfbox.pdmodel.common.PDStream;
import org.pdfbox.util.PDFStreamEngine;

public class PdfPageStreamTransform extends PDFStreamEngine implements PdfPageTransform {

  protected static Logger logger = Logger.getLoggerWithInitialLevel("PdfPageStreamTransform", Logger.LEVEL_DEBUG3);

  protected Stack /* of ArrayList */ listStack;

  public void splitOutputList() {
    listStack.push(new ArrayList());
  }

  public void mergeOutputList() throws EmptyStackException {
    List oldTop = (List)listStack.peek();
    mergeOutputList(oldTop);
  }

  public void mergeOutputList(List replacement) throws EmptyStackException {
    listStack.pop();
    List newTop = (List)listStack.peek();
    newTop.addAll(replacement);
  }

  protected PdfPageStreamTransform(Properties operatorProcessors) throws IOException {
    super(operatorProcessors);
    this.listStack = new Stack(); /* of List */
  }

  public List getOutputList() {
    return (List)listStack.peek();
  }

  public void signalChange() {
    atLeastOneChange = true;
  }

  protected boolean atLeastOneChange;

  public void reset() {
    atLeastOneChange = false;
    listStack.clear();
    splitOutputList();
  }

  public synchronized void transform(PdfDocument pdfDocument,
                                     PDPage pdfPage)
      throws IOException {
    // Iterate over stream
    reset();
    processStream(pdfPage,
                  pdfPage.findResources(),
                  pdfPage.getContents().getStream());

    // Sanity check
    if (listStack.size() != 1) {
      throw new PdfPageTransformException("Split/Merge mismatch: after processing stream, list stack has size " + listStack.size());
    }

    // Write result
    writeResult(pdfDocument, pdfPage);
  }

  protected void writeResult(PdfDocument pdfDocument,
                             PDPage pdfPage)
      throws IOException {
    if (atLeastOneChange) {
      PDStream resultStream = new PDStream(pdfDocument.getPDDocument());
      OutputStream outputStream = resultStream.createOutputStream();
      ContentStreamWriter tokenWriter = new ContentStreamWriter(outputStream);
      tokenWriter.writeTokens(getOutputList());
      pdfPage.setContents(resultStream);
    }
  }

  private static Properties defaultPropertiesSingleton;

  protected static synchronized Properties getDefaultProperties() throws IOException {
    if (defaultPropertiesSingleton == null) {
      defaultPropertiesSingleton = getProperties("PdfPageStreamTransform.properties");
    }
    return defaultPropertiesSingleton;
  }

  public static PdfPageStreamTransform makeTransform(Properties customOperatorProcessors) throws IOException {
    Properties properties = rewritePropertiesWithDefaults(customOperatorProcessors);
    return new PdfPageStreamTransform(properties);
  }

  protected static Properties rewritePropertiesWithDefaults(Properties customOperatorProcessors) throws IOException {
    Properties properties = new Properties();
    properties.putAll(customOperatorProcessors);
    Properties defaults = getDefaultProperties();
    for (Enumeration enumer = defaults.propertyNames() ; enumer.hasMoreElements() ; ) {
      String key = (String)enumer.nextElement();
      if (properties.getProperty(key) == null) {
        properties.setProperty(key, defaults.getProperty(key));
      }
    }
    return properties;
  }

  public static PdfPageStreamTransform makeIdentityTransform() throws IOException {
    return new PdfPageStreamTransform(getDefaultProperties());
  }

  public static boolean identifyPageStream(PdfDocument pdfDocument,
                                           PDPage pdfPage,
                                           Properties customOperatorProcessors)
      throws IOException {

    // Hack: interrupt stream processing with a runtime exception
    class ReturnTrue extends RuntimeException { }

    PdfPageStreamTransform matcher = new PdfPageStreamTransform(rewritePropertiesWithDefaults(customOperatorProcessors)) {
      public void signalChange() {
        logger.debug3("Match in signalChange()");
        throw new ReturnTrue();
      }
      protected void writeResult(PdfDocument pdfDocument, PDPage pdfPage) throws IOException {
        // Do not do anything
      }
    };

    try {
      matcher.transform(pdfDocument, pdfPage);
    }
    catch (ReturnTrue rt) {
      logger.debug("Matched");
      return true;
    }
    logger.debug("Did not match");
    return false;
  }

  protected static Properties getProperties(String resource)
      throws IOException {
    Properties properties = new Properties();
    Class cla = PdfPageStreamTransform.class;
    InputStream inputStream = cla.getResourceAsStream(resource);
    properties.load(inputStream);
    return properties;
  }

}