package org.lockss.pdf.pdfbox;

import java.util.List;

import org.lockss.pdf.PdfOperandsAndOperator;

public class PdfBoxOperandsAndOperator extends PdfOperandsAndOperator<PdfBoxToken> {

  public PdfBoxOperandsAndOperator() {
    super();
  }

  public PdfBoxOperandsAndOperator(List<PdfBoxToken> operands, PdfBoxToken operator) {
    super(operands, operator);
  }

  public PdfBoxOperandsAndOperator(List<PdfBoxToken> operands) {
    super(operands);
  }

  public PdfBoxOperandsAndOperator(PdfBoxToken operator) {
    super(operator);
  }

  
  
}
