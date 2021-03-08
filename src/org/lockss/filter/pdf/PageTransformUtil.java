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

package org.lockss.filter.pdf;

import java.io.*;
import java.util.List;

import org.lockss.filter.pdf.PageStreamTransform.NullPageStreamTransform;
import org.lockss.util.*;
import org.pdfbox.cos.COSString;
import org.pdfbox.util.*;

/**
 * <p>Utility page transforms.</p>
 * @author Thib Guicherd-Callin
 * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
 */
@Deprecated
public class PageTransformUtil {

  /**
   * <p>A null page stream transform that has the side-effect of
   * extracting all string constants found on the page into an
   * output stream.</p>
   * <p>This transform keeps state; beware of re-using instances as
   * they keep a reference to the instantiation output stream.</p>
   * @author Thib Guicherd-Callin
   * @see PageTransformUtil.ExtractStringsToOutputStream.WriteToOutputStream
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public static class ExtractStringsToOutputStream extends NullPageStreamTransform {

    /**
     * <p>A version of {@link ProcessString} that assumes it is being
     * used in the context of a
     * {@link PageTransformUtil.ExtractStringsToOutputStream} page
     * stream transform and writes the bytes of each string
     * encountered to its output stream.</p>
     * <p>This class should be a member nested class, not a static
     * nested class, but the dynamic instantiation semantics of
     * {@link PDFStreamEngine} prevent this.</p>
     * @author Thib Guicherd-Callin
     * @see PageTransformUtil.ExtractStringsToOutputStream
     * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
     */
    @Deprecated
    public static class WriteToOutputStream extends ProcessString {

      /* Inherit documentation */
      public void processString(PageStreamTransform pageStreamTransform,
                                PDFOperator operator,
                                List operands,
                                COSString cosString)
          throws IOException {
        pageStreamTransform.signalChange(); // At least one string processed
        ExtractStringsToOutputStream extractText = (ExtractStringsToOutputStream)pageStreamTransform;
        extractText.outputStream.write(cosString.getBytes());
      }

    }

    /**
     * <p>The output stream associated with this instance.</p>
     * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
     */
    @Deprecated
    protected OutputStream outputStream;

    /**
     * <p>Builds a new transform that will produce output in the given
     * output stream.</p>
     * @param outputStream An output stream for output.
     * @throws IOException if any processing error occurs.
     * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
     */
    @Deprecated
    public ExtractStringsToOutputStream(OutputStream outputStream) throws IOException {
      super(PdfUtil.SHOW_TEXT, WriteToOutputStream.class,
            PdfUtil.SHOW_TEXT_GLYPH_POSITIONING, WriteToOutputStream.class,
            PdfUtil.MOVE_TO_NEXT_LINE_SHOW_TEXT, WriteToOutputStream.class,
            PdfUtil.SET_SPACING_MOVE_TO_NEXT_LINE_SHOW_TEXT, WriteToOutputStream.class);
      this.outputStream = outputStream;
    }

  }

  /**
   * <p>A null page stream transform that has the side-effect of
   * extracting all string constants found on the page into a
   * string buffer.</p>
   * <p>This transform keeps state; beware of re-using instances as
   * they keep a reference to the instantiation string buffer.</p>
   * @author Thib Guicherd-Callin
   * @see PageTransformUtil.ExtractStringsToStringBuffer.AppendToStringBuffer
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public static class ExtractStringsToStringBuffer extends NullPageStreamTransform {

    /**
     * <p>A version of {@link ProcessString} that assumes it is being
     * used in the context of a
     * {@link PageTransformUtil.ExtractStringsToStringBuffer} page
     * stream transform and writes each string encountered to its
     * string buffer.</p>
     * <p>This class should be a member nested class, not a static
     * nested class, but the dynamic instantiation semantics of
     * {@link PDFStreamEngine} prevent this.</p>
     * @author Thib Guicherd-Callin
     * @see PageTransformUtil.ExtractStringsToStringBuffer
     * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
     */
    @Deprecated
    public static class AppendToStringBuffer extends ProcessString {

      /* Inherit documentation */
      public void processString(PageStreamTransform pageStreamTransform,
                                PDFOperator operator,
                                List operands,
                                COSString cosString)
          throws IOException {
        pageStreamTransform.signalChange(); // At least one string processed
        ExtractStringsToStringBuffer extractStrings = (ExtractStringsToStringBuffer)pageStreamTransform;
        extractStrings.buffer.append(PdfUtil.getPdfString(cosString));
      }

    }

    /**
     * <p>The string buffer associated with this instance.</p>
     * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
     */
    @Deprecated
    protected StringBuffer buffer;

    /**
     * <p>Builds a new transform that will produce output in the given
     * string buffer.</p>
     * @param buffer A string buffer for output.
     * @throws IOException if any processing error occurs.
     * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
     */
    @Deprecated
    public ExtractStringsToStringBuffer(StringBuffer buffer) throws IOException {
      super(PdfUtil.SHOW_TEXT, AppendToStringBuffer.class,
            PdfUtil.SHOW_TEXT_GLYPH_POSITIONING, AppendToStringBuffer.class,
            PdfUtil.MOVE_TO_NEXT_LINE_SHOW_TEXT, AppendToStringBuffer.class,
            PdfUtil.SET_SPACING_MOVE_TO_NEXT_LINE_SHOW_TEXT, AppendToStringBuffer.class);
      this.buffer = buffer;
    }

  }

  /**
   * <p>A page transform that does nothing.</p>
   * @author Thib Guicherd-Callin
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public static class IdentityPageTransform implements PageTransform {

    /**
     * <p>The return value for {@link #transform}.</p>
     * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
     */
    @Deprecated
    protected boolean returnValue;

    /**
     * <p>Builds a new identity page transform whose
     * {@link #transform} method always returns the default
     * result value.</p>
     * @see #IdentityPageTransform(boolean)
     * @see #RESULT_DEFAULT
     * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
     */
    @Deprecated
    public IdentityPageTransform() {
      this(RESULT_DEFAULT);
    }

    /**
     * <p>Builds a new identity page transform whose
     * {@link #transform} method always returns the given
     * result value.</p>
     * @param returnValue The return value for {@link #transform}.
     * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
     */
    @Deprecated
    public IdentityPageTransform(boolean returnValue) {
      this.returnValue = returnValue;
    }

    /* Inherit documentation */
    @Deprecated
    public boolean transform(PdfPage pdfPage) throws IOException {
      logger.debug2("Identity page transform result: " + returnValue);
      return returnValue;
    }

    /**
     * <p>The constant return value used by default by this class.</p>
     * @see #IdentityPageTransform()
     * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
     */
    @Deprecated
    public static final boolean RESULT_DEFAULT = true;

  }

  /**
   * <p>A page transform decorator whose {@link #transform} method
   * returns the opposite of its underlying page transform's
   * {@link PageTransform#transform} method.</p>
   * @author Thib Guicherd-Callin
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public static class OppositePageTransform extends PageTransformDecorator {

    /**
     * <p>Builds a new page transform decorating the given
     * page transform.</p>
     * @param pageTransform A page transform to be wrapped.
     * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
     */
    @Deprecated
    public OppositePageTransform(PageTransform pageTransform) {
      super(pageTransform);
    }

    /* Inherit documentation */
    @Deprecated
    public boolean transform(PdfPage pdfPage) throws IOException {
      logger.debug3("Begin opposite page transform based on " + pageTransform.getClass().getName());
      boolean ret = !pageTransform.transform(pdfPage);
      logger.debug2("Opposite page transform result: " + ret);
      return ret;
    }

  }

  /**
   * <p>A base wrapper for another page transform.</p>
   * @author Thib Guicherd-Callin
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public static abstract class PageTransformDecorator implements PageTransform {

    /**
     * <p>The underlying page transform.</p>
     * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
     */
    @Deprecated
    protected PageTransform pageTransform;

    /**
     * <p>Builds a new page transform with the given underlying
     * page transform.</p>
     * @param pageTransform A page transform to be wrapped.
     * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
     */
    @Deprecated
    protected PageTransformDecorator(PageTransform pageTransform) {
      this.pageTransform = pageTransform;
    }

  }

  /**
   * <p>A page transform decorator that throws a
   * {@link PageTransformException} when its underlying page
   * transform fails.</p>
   * @author Thib Guicherd-Callin
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  public static class StrictPageTransform extends PageTransformDecorator {

    /**
     * <p>Builds a new strict page transform decorating the given
     * page transform.</p>
     * @param pageTransform A page transform.
     * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
     */
    @Deprecated
    public StrictPageTransform(PageTransform pageTransform) {
      super(pageTransform);
    }

    /* Inherit documentation */
    @Deprecated
    public boolean transform(PdfPage pdfPage) throws IOException {
      logger.debug3("Begin strict page transform based on " + pageTransform.getClass().getName());
      if (pageTransform.transform(pdfPage)) {
        logger.debug2("Strict page transform result: true");
        return true;
      }
      else {
        logger.debug2("Strict page transform result: throw");
        throw new PageTransformException("Strict page transform did not succeed");
      }
    }

  }

  /**
   * <p>Not publicly instantiable.</p>
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  private PageTransformUtil() { }

  /**
   * <p>A logger for use by this class.</p>
   * @deprecated Moving away from PDFBox 0.7.3 after 1.76.
   */
  @Deprecated
  private static Logger logger = Logger.getLogger(PageTransformUtil.class);

}
