/*
 * $Id: NewEnglandJournalOfMedicinePdfTransform.java,v 1.2 2006-09-22 17:16:39 thib_gc Exp $
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

package org.lockss.plugin.highwire;

import java.io.*;
import java.util.List;

import org.apache.commons.io.output.NullOutputStream;
import org.lockss.filter.pdf.*;
import org.lockss.filter.pdf.DocumentTransformUtil.OutputDocumentTransform;
import org.lockss.filter.pdf.PageTransformUtil.ExtractText;
import org.lockss.plugin.highwire.HighWirePdfFilterFactory.SanitizeMetadata;
import org.lockss.util.*;

public class NewEnglandJournalOfMedicinePdfTransform
    extends ConditionalDocumentTransform
    implements OutputDocumentTransform {

  public static class EraseVariableMessage extends PageStreamTransform {

    public static class ProcessEndTextObject extends ConditionalMergeOperatorProcessor {
      public List getReplacement(List tokens) {
        return ListUtil.list(tokens.get(0), tokens.get(tokens.size() - 1));
      }
      public boolean identify(List tokens) {
        int last = tokens.size() - 1;
        boolean ret = PdfUtil.matchTextObject(tokens, 0, last)
        && PdfUtil.matchShowTextStartsWith(tokens, last - 1, "Downloaded from ");;
        logger.debug3("ProcessEndTextObject candidate match: " + ret);
        return ret;
      }
    }

    public EraseVariableMessage() throws IOException {
      super(PdfUtil.BEGIN_TEXT_OBJECT, SplitOperatorProcessor.class,
            PdfUtil.END_TEXT_OBJECT, ProcessEndTextObject.class);
    }

  }

  public static class Simplified implements OutputDocumentTransform {

    protected OutputStream outputStream;

    public synchronized boolean transform(PdfDocument pdfDocument) throws IOException {
      logger.debug2("Begin simplified document transform");
      if (outputStream == null) {
        outputStream = new NullOutputStream();
      }
      AggregateDocumentTransform documentTransform = new AggregateDocumentTransform(new NewEnglandJournalOfMedicinePdfTransform(),
                                                                                    new TransformEachPage(new ExtractText(outputStream)));
      boolean ret = documentTransform.transform(pdfDocument);
      logger.debug2("Simplified document transform result: " + ret);
      return ret;
    }

    public synchronized boolean transform(PdfDocument pdfDocument,
                                          OutputStream outputStream) {
      try {
        this.outputStream = outputStream;
        return transform(pdfDocument);
      }
      catch (IOException ioe) {
        logger.error("Simplified document transform failed", ioe);
        return false;
      }
      finally {
        this.outputStream = null;
      }
    }

  }

  public NewEnglandJournalOfMedicinePdfTransform() throws IOException {
    super(new TransformFirstPage(new EraseVariableMessage()),
          new TransformEachPageExceptFirst(new EraseVariableMessage()),
          new SanitizeMetadata());
  }

  public boolean transform(PdfDocument pdfDocument,
                           OutputStream outputStream) {
    return PdfUtil.applyAndSave(this,
                                pdfDocument,
                                outputStream);
  }

  private static Logger logger = Logger.getLogger("NewEnglandJournalOfMedicinePdfTransform");

}
