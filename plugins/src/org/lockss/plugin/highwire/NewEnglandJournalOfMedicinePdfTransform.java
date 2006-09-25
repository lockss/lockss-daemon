/*
 * $Id: NewEnglandJournalOfMedicinePdfTransform.java,v 1.4 2006-09-25 08:12:14 thib_gc Exp $
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

import org.lockss.filter.pdf.*;
import org.lockss.filter.pdf.DocumentTransformUtil.*;
import org.lockss.filter.pdf.PageTransformUtil.ExtractStringsToOutputStream;
import org.lockss.plugin.highwire.HighWirePdfFilterFactory.SanitizeMetadata;
import org.lockss.util.*;

public class NewEnglandJournalOfMedicinePdfTransform extends SimpleOutputDocumentTransform {

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

  public static class Simplified extends OutputStreamDocumentTransform {

    public static class EraseVariableMessage2 extends PageStreamTransform {

      public static class ProcessShowText extends ReplaceString {

        public String getReplacement(String match) {
          return " ";
        }

        public boolean identify(String candidate) {
          return candidate.startsWith("Downloaded from ");
        }

      }

      public EraseVariableMessage2() throws IOException {
        super(PdfUtil.SHOW_TEXT, ProcessShowText.class);
      }

    }

    public DocumentTransform makeTransform() throws IOException {
      return new ConditionalDocumentTransform(new TransformFirstPage(new EraseVariableMessage2()),
                                              new TransformEachPageExceptFirst(new EraseVariableMessage2()),
                                              new TransformEachPage(new ExtractStringsToOutputStream(outputStream)));
    }

  }

  public NewEnglandJournalOfMedicinePdfTransform() throws IOException {
    super(new ConditionalDocumentTransform(new TransformFirstPage(new EraseVariableMessage()),
                                           new TransformEachPageExceptFirst(new EraseVariableMessage()),
                                           new SanitizeMetadata()));
  }

  private static Logger logger = Logger.getLogger("NewEnglandJournalOfMedicinePdfTransform");

}
