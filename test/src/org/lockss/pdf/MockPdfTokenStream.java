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

import java.io.*;
import java.util.*;

import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.io.RandomAccessBuffer;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.util.PDFOperator;
import org.lockss.test.StringInputStream;

public class MockPdfTokenStream implements PdfTokenStream {

  protected List<PdfToken> pdfTokens;
  
  /**
   * <p>
   * Makes a fake PDF token stream from parsing the given input stream.
   * </p>
   * 
   * @param inputStream
   *          An input stream of PDF token stream source.
   * @throws IOException
   *           if parsing fails or an I/O error occurs.
   * @since 1.67
   */
  public MockPdfTokenStream(InputStream inputStream) throws IOException {
    PDFStreamParser parser = new PDFStreamParser(inputStream, new RandomAccessBuffer());
    parser.parse();
    List<Object> pdfBoxTokens = parser.getTokens();
    this.pdfTokens = new ArrayList<PdfToken>(pdfBoxTokens.size());
    for (Object pdfBoxToken : pdfBoxTokens) {
      this.pdfTokens.add(convert(pdfBoxToken));
    }
  }
  
  @Override
  public PdfPage getPage() {
    return null;
  }

  @Override
  public PdfTokenFactory getTokenFactory() throws PdfException {
    return null;
  }

  @Override
  public List<PdfToken> getTokens() throws PdfException {
    return pdfTokens;
  }

  @Override
  public void setTokens(List<PdfToken> newTokens) throws PdfException {
    this.pdfTokens = newTokens;
  }
  
  /**
   * <p>
   * Converts one PDFBox token into a {@link PdfToken} instance, more
   * specifically a {@link MockPdfToken} instance.
   * </p>
   * 
   * @param pdfBoxToken A token from PDFBox.
   * @return A {@link PdfToken} ({@link MockPdfToken}) instance.
   * @throws IOException if the PDFBox token type is unexpected.
   * @since 1.67
   */
  public static PdfToken convert(Object pdfBoxToken) throws IOException {
    PdfTokenFactory tf = new MockPdfTokenFactory();
    if (pdfBoxToken instanceof COSArray) {
      COSArray cosArray = (COSArray)pdfBoxToken;
      List<PdfToken> lst = new ArrayList<PdfToken>(cosArray.size());
      for (Object obj : cosArray) {
        lst.add(convert(obj));
      }
      return tf.makeArray(lst);
    }
    else if (pdfBoxToken instanceof COSBoolean) {
      return tf.makeBoolean(((COSBoolean)pdfBoxToken).getValue());
    }
    else if (pdfBoxToken instanceof COSDictionary) {
      COSDictionary cosDictionary = (COSDictionary)pdfBoxToken;
      Map<String, PdfToken> map = new LinkedHashMap<String, PdfToken>(cosDictionary.size());
      for (Map.Entry<COSName, COSBase> ent : cosDictionary.entrySet()) {
        map.put(ent.getKey().getName(), convert(ent.getValue()));
      }
      return tf.makeDictionary(map);
    }
    else if (pdfBoxToken instanceof COSFloat) {
      return tf.makeFloat(((COSFloat)pdfBoxToken).floatValue());
    }
    else if (pdfBoxToken instanceof COSInteger) {
      return tf.makeFloat(((COSInteger)pdfBoxToken).intValue());
    }
    else if (pdfBoxToken instanceof COSName) {
      return tf.makeName(((COSName)pdfBoxToken).getName());
    }
    else if (pdfBoxToken instanceof COSNull) {
      return tf.makeNull();
    }
    else if (pdfBoxToken instanceof COSString) {
      return tf.makeString(((COSString)pdfBoxToken).getString());
    }
    else if (pdfBoxToken instanceof PDFOperator) {
      return tf.makeOperator(((PDFOperator)pdfBoxToken).getOperation());
    }
    else {
      throw new IOException(String.format("Unexpected type: %s \"%s\"",
                                          pdfBoxToken.getClass().getName(),
                                          pdfBoxToken.toString()));
    }
  }
  
  /**
   * <p>
   * Convenience method to parse a string into a {@link MockPdfTokenStream}
   * instance.
   * </p>
   * 
   * @param sourceString
   *          A string of PDF token stream source.
   * @return A {@link MockPdfTokenStream} instance from the source string.
   * @throws IOException
   *           if parsing fails or another I/O error occurs.
   * @since 1.67
   */
  public static MockPdfTokenStream parse(String sourceString) throws IOException {
    return new MockPdfTokenStream(new StringInputStream(sourceString));
  }

}
