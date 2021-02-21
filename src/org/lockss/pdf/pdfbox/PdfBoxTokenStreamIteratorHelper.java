package org.lockss.pdf.pdfbox;

import org.lockss.pdf.PdfTokenStreamIteratorHelper;

public abstract class PdfBoxTokenStreamIteratorHelper<Element>
    extends PdfTokenStreamIteratorHelper<PdfBoxToken,
                                         PdfBoxOperandsAndOperator,
                                         PdfBoxTokenStreamIterator,
                                         Element> {

  public PdfBoxTokenStreamIteratorHelper(PdfBoxTokenStreamIterator initial) {
    super(initial);
  }
  
}
