/*
 * $Id$
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

import java.io.IOException;
import java.util.List;

import org.lockss.filter.pdf.DocumentTransformUtil.IdentityDocumentTransform;
import org.lockss.filter.pdf.PageTransformUtil.IdentityPageTransform;
import org.lockss.util.*;

@Deprecated
public class MockTransforms {

  /**
   * <p>A PDF transform that remembers how many times it has been
   * called.</p>
   * @author Thib Guicherd-Callin
   */
  public static class RememberDocumentTransform extends IdentityDocumentTransform {

    protected int callCount;

    protected List rememberDocumentTransforms;

    public RememberDocumentTransform(boolean returnValue,
                                     List rememberDocumentTransforms) {
      super(returnValue);
      this.rememberDocumentTransforms = rememberDocumentTransforms;
      this.callCount = 0;
    }

    public RememberDocumentTransform(List rememberDocumentTransforms) {
      this(true,
           rememberDocumentTransforms);
    }

    public int getCallCount() {
      return callCount;
    }

    /* Inherit documentation */
    public boolean transform(PdfDocument pdfDocument) throws IOException {
      ++callCount;
      rememberDocumentTransforms.add(this);
      return super.transform(pdfDocument);
    }

  }

  /**
   * <p>A PDF page transform that remembers how many times it has been
   * called.</p>
   * @author Thib Guicherd-Callin
   */
  public static class RememberPagePageTransform extends IdentityPageTransform {

    protected int callCount = 0;

    protected List rememberPages;

    public RememberPagePageTransform(boolean returnValue,
                                 List rememberPages) {
      super(returnValue);
      this.rememberPages = rememberPages;
      this.callCount = 0;
    }

    public RememberPagePageTransform(List rememberPages) {
      this(true,
           rememberPages);
    }

    public int getCallCount() {
      return callCount;
    }

    /* Inherit documentation */
    public boolean transform(PdfPage pdfPage) throws IOException {
      ++callCount;
      rememberPages.add(pdfPage);
      return super.transform(pdfPage);
    }

  }

  /**
   * <p>A PDF page transform that remembers how many times it has been
   * called.</p>
   * @author Thib Guicherd-Callin
   */
  public static class RememberTransformPageTransform extends IdentityPageTransform {

    protected int callCount = 0;

    protected List rememberPageTransforms;

    public RememberTransformPageTransform(boolean returnValue,
                                          List rememberPageTransforms) {
      super(returnValue);
      this.rememberPageTransforms = rememberPageTransforms;
      this.callCount = 0;
    }

    public RememberTransformPageTransform(List rememberPageTransforms) {
      this(true,
           rememberPageTransforms);
    }

    public int getCallCount() {
      return callCount;
    }

    /* Inherit documentation */
    public boolean transform(PdfPage pdfPage) throws IOException {
      ++callCount;
      rememberPageTransforms.add(this);
      return super.transform(pdfPage);
    }

  }

}
