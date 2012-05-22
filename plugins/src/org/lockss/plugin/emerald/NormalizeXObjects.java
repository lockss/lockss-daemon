/*
 * $Id: NormalizeXObjects.java,v 1.1 2012-05-22 23:30:46 wkwilson Exp $
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

package org.lockss.plugin.emerald;

import java.io.IOException;
import java.util.List;

import org.lockss.filter.pdf.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.*;
import org.pdfbox.util.operator.OperatorProcessor;

public class NormalizeXObjects extends AggregatePageTransform {

  public static class NormalizeReferences extends PageStreamTransform {

    public static class ProcessEndTextObject extends ConditionalMergeOperatorProcessor {

      @Override
      public List getReplacement(List tokens) {
        // Replace by an empty text object
        return ListUtil.list(// Known to be "BT"
                             tokens.get(0),
                             // Known to be "ET"
                             tokens.get(tokens.size() - 1));
      }

      @Override
      public boolean identify(List tokens) {
        boolean ret = false;
        int progress = 0;
        // Iterate from the end
        iteration: for (int tok = tokens.size() - 1 ; tok >= 0 ; --tok) {
          switch (progress) {
            case 0:
              // End of subsequence
              if (tok != tokens.size() - 1) { break iteration; }
              // ET
              if (PdfUtil.isEndTextObject(tokens, tok)) { ++progress; }
              break;
            case 1:
              // Not BT
              if (PdfUtil.isBeginTextObject(tokens,tok)) { break iteration; }
              // Tj and its argument ends with the string
              if (PdfUtil.matchShowTextMatches(tokens, tok, "References: This document contains references to [0-9]+ other documents.*")) { ++progress; }
              break;
            case 2:
              // BT; beginning of subsequence
              if (PdfUtil.isBeginTextObject(tokens,tok)) { ret = (tok == 0); break iteration; }
              break;
          }
        }
        return ret;
      }

    }

    public NormalizeReferences(final ArchivalUnit au) throws IOException {
      super(new OperatorProcessorFactory() {
              public OperatorProcessor newInstanceForName(String className) throws LinkageError, ExceptionInInitializerError, ClassNotFoundException, IllegalAccessException, InstantiationException, SecurityException {
                return (OperatorProcessor)au.getPlugin().newAuxClass(className,
                                                                     OperatorProcessor.class);
              }
            },
            PdfUtil.INVOKE_NAMED_XOBJECT, FormXObjectOperatorProcessor.class,
            PdfUtil.BEGIN_TEXT_OBJECT, SplitOperatorProcessor.class,
            PdfUtil.END_TEXT_OBJECT, NormalizeReferences.ProcessEndTextObject.class);
    }

  }

  public static class NormalizeDownloaded extends PageStreamTransform {

    public static class ProcessEndTextObject extends ConditionalMergeOperatorProcessor {

      @Override
      public List getReplacement(List tokens) {
        // Replace by an empty text object
        return ListUtil.list(// Known to be "BT"
                             tokens.get(0),
                             // Known to be "ET"
                             tokens.get(tokens.size() - 1));
      }

      @Override
      public boolean identify(List tokens) {
        boolean ret = false;
        int progress = 0;
        // Iterate from the end
        iteration: for (int tok = tokens.size() - 1 ; tok >= 0 ; --tok) {
          switch (progress) {
            case 0:
              // End of subsequence
              if (tok != tokens.size() - 1) { break iteration; }
              // ET
              if (PdfUtil.isEndTextObject(tokens, tok)) { ++progress; }
              break;
            case 1:
              // Not BT
              if (PdfUtil.isBeginTextObject(tokens,tok)) { break iteration; }
              // Tj and its argument begins with the string "by", contains a date, and ends with the string "For personal use only."
              if (PdfUtil.matchShowTextMatches(tokens, tok, "This document has been downloaded [0-9]+ times.*")) { ++progress; }
              break;
            case 2:
              // BT; beginning of subsequence
              if (PdfUtil.isBeginTextObject(tokens,tok)) { ret = (tok == 0); break iteration; }
              break;
          }
        }
        return ret;
      }

    }
    public NormalizeDownloaded(final ArchivalUnit au) throws IOException {
        super(new OperatorProcessorFactory() {
                public OperatorProcessor newInstanceForName(String className) throws LinkageError, ExceptionInInitializerError, ClassNotFoundException, IllegalAccessException, InstantiationException, SecurityException {
                  return (OperatorProcessor)au.getPlugin().newAuxClass(className,
                                                                       OperatorProcessor.class);
                }
              },
              PdfUtil.INVOKE_NAMED_XOBJECT, FormXObjectOperatorProcessor.class,
              PdfUtil.BEGIN_TEXT_OBJECT, SplitOperatorProcessor.class,
              PdfUtil.END_TEXT_OBJECT, NormalizeDownloaded.ProcessEndTextObject.class);
      }

    }
    
    public static class NormalizeDate extends PageStreamTransform {

        public static class ProcessEndTextObject extends ConditionalMergeOperatorProcessor {

          @Override
          public List getReplacement(List tokens) {
            // Replace by an empty text object
            return ListUtil.list(// Known to be "BT"
                                 tokens.get(0),
                                 // Known to be "ET"
                                 tokens.get(tokens.size() - 1));
          }

          @Override
          public boolean identify(List tokens) {
            boolean ret = false;
            int progress = 0;
            // Iterate from the end
            iteration: for (int tok = tokens.size() - 1 ; tok >= 0 ; --tok) {
              switch (progress) {
                case 0:
                  // End of subsequence
                  if (tok != tokens.size() - 1) { break iteration; }
                  // ET
                  if (PdfUtil.isEndTextObject(tokens, tok)) { ++progress; }
                  break;
                case 1:
                  // Not BT
                  if (PdfUtil.isBeginTextObject(tokens,tok)) { break iteration; }
                  // Tj and its argument begins with the string "by", contains a date, and ends with the string "For personal use only."
                  if (PdfUtil.matchShowTextMatches(tokens, tok, "Downloaded on: [0-9]{2}-[0-9]{2}-[0-9]{4}.*")) { ++progress; }
                  break;
                case 2:
                  // BT; beginning of subsequence
                  if (PdfUtil.isBeginTextObject(tokens,tok)) { ret = (tok == 0); break iteration; }
                  break;
              }
            }
            return ret;
          }

        }
        
        public NormalizeDate(final ArchivalUnit au) throws IOException {
            super(new OperatorProcessorFactory() {
                    public OperatorProcessor newInstanceForName(String className) throws LinkageError, ExceptionInInitializerError, ClassNotFoundException, IllegalAccessException, InstantiationException, SecurityException {
                      return (OperatorProcessor)au.getPlugin().newAuxClass(className,
                                                                     OperatorProcessor.class);
                    }
                  },
                  PdfUtil.INVOKE_NAMED_XOBJECT, FormXObjectOperatorProcessor.class,
                  PdfUtil.BEGIN_TEXT_OBJECT, SplitOperatorProcessor.class,
                  PdfUtil.END_TEXT_OBJECT, NormalizeDate.ProcessEndTextObject.class);
          }

        }
       
        
    public static class NormalizeProvided extends PageStreamTransform {

        public static class ProcessEndTextObject extends ConditionalMergeOperatorProcessor {

          @Override
          public List getReplacement(List tokens) {
            // Replace by an empty text object
            return ListUtil.list(// Known to be "BT"
                                 tokens.get(0),
                                 // Known to be "ET"
                                 tokens.get(tokens.size() - 1));
          }

          @Override
          public boolean identify(List tokens) {
            boolean ret = false;
            int progress = 0;
            // Iterate from the end
            iteration: for (int tok = tokens.size() - 1 ; tok >= 0 ; --tok) {
              switch (progress) {
                case 0:
                  // End of subsequence
                  if (tok != tokens.size() - 1) { break iteration; }
                  // ET
                  if (PdfUtil.isEndTextObject(tokens, tok)) { ++progress; }
                  break;
                case 1:
                  // Not BT
                  if (PdfUtil.isBeginTextObject(tokens,tok)) { break iteration; }
                  // Tj and its argument begins with the string "by", contains a date, and ends with the string "For personal use only."
                  if (PdfUtil.matchShowTextMatches(tokens, tok, "Access to this document was granted through an Emerald subscription provided by.*")) { ++progress; }
                  break;
                case 2:
                  // BT; beginning of subsequence
                  if (PdfUtil.isBeginTextObject(tokens,tok)) { ret = (tok == 0); break iteration; }
                  break;
              }
            }
            return ret;
          }

        }

    public NormalizeProvided(final ArchivalUnit au) throws IOException {
      super(new OperatorProcessorFactory() {
              public OperatorProcessor newInstanceForName(String className) throws LinkageError, ExceptionInInitializerError, ClassNotFoundException, IllegalAccessException, InstantiationException, SecurityException {
                return (OperatorProcessor)au.getPlugin().newAuxClass(className,
                                                               OperatorProcessor.class);
              }
            },
            PdfUtil.INVOKE_NAMED_XOBJECT, FormXObjectOperatorProcessor.class,
            PdfUtil.BEGIN_TEXT_OBJECT, SplitOperatorProcessor.class,
            PdfUtil.END_TEXT_OBJECT, NormalizeProvided.ProcessEndTextObject.class);
    }

  }

  public NormalizeXObjects(ArchivalUnit au) throws IOException {
    super(new NormalizeReferences(au),
    	  new NormalizeDate(au),
    	  new NormalizeDownloaded(au));
    super.add(new NormalizeProvided(au));
  }

}