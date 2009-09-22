package org.lockss.plugin.highwire;

import java.io.*;

import org.lockss.filter.pdf.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.highwire.HighWirePdfFilterFactory.*;
import org.lockss.util.*;


public class AmericanAcademyOfPediatricsPdfTransform implements
    OutputDocumentTransform,
    ArchivalUnitDependent {

  protected ArchivalUnit au;

  public void setArchivalUnit(ArchivalUnit au) {
    this.au = au;
  }

  public boolean transform(PdfDocument pdfDocument,
                           OutputStream outputStream) {
    return PdfUtil.applyAndSave(this,
                                pdfDocument,
                                outputStream);
  }

  public boolean transform(PdfDocument pdfDocument) throws IOException {
    if (au == null) throw new IOException("Uninitialized AU-dependent transform");
    DocumentTransform documentTransform = new ConditionalDocumentTransform(// If on the first page...
                                                                           new TransformFirstPage(// ...collapsing "Downloaded from" and normalizing the hyperlinks succeeds,
                                                                                                  new CollapseDownloadedFromAndNormalizeHyperlinks(au)),
                                                                           // Then on all other pages...
                                                                           new TransformEachPageExceptFirst(// ...collapse "Downloaded from" and normalize the hyperlink,
                                                                                                            new CollapseDownloadedFromAndNormalizeHyperlinks(au)),
                                                                           // ...and normalize the metadata
                                                                           new NormalizeMetadata());
    return documentTransform.transform(pdfDocument);
  }

}
