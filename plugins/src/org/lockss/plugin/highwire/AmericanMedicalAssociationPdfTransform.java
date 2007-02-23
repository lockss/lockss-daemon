/*
 * $Id: AmericanMedicalAssociationPdfTransform.java,v 1.3 2007-02-23 22:22:35 thib_gc Exp $
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

package org.lockss.plugin.highwire;

import java.io.IOException;

import org.lockss.filter.pdf.*;
import org.lockss.plugin.highwire.HighWirePdfFilterFactory.*;

public class AmericanMedicalAssociationPdfTransform extends SimpleOutputDocumentTransform {

  public static class Simplified extends TextScrapingDocumentTransform {
    
    public DocumentTransform makePreliminaryTransform() throws IOException {
      return new ConditionalDocumentTransform(// If on the first page...
                                              new TransformFirstPage(// ...collapsing "Downloaded from" succeeds,
                                                                     new CollapseDownloadedFrom()),
                                              // Then on all other pages...
                                              new TransformEachPageExceptFirst(// ...collapse "Downloaded from"
                                                                               new CollapseDownloadedFrom()));
    }
    
  }
  
  public AmericanMedicalAssociationPdfTransform() throws IOException {
    super(new ConditionalDocumentTransform(// If on the first page...
                                           new TransformFirstPage(// ...collapsing "Downloaded from" and normalizing its hyperlink succeeds,
                                                                  new CollapseDownloadedFromAndNormalizeHyperlink()),
                                           // Then on all other pages...
                                           new TransformEachPageExceptFirst(// ...collapse "Downloaded from" and normalize its hyperlink,
                                                                            new CollapseDownloadedFromAndNormalizeHyperlink()),
                                           // ...and normalize the metadata
                                           new NormalizeMetadata()));
  }
  
}
