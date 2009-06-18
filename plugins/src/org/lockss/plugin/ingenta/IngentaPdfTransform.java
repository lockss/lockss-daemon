package org.lockss.plugin.ingenta;

import java.io.IOException;
import java.io.OutputStream;

import org.lockss.filter.pdf.DocumentTransform;
import org.lockss.filter.pdf.OutputDocumentTransform;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.PdfDocument;
import org.lockss.util.PdfUtil;
import org.pdfbox.cos.COSArray;
import org.pdfbox.cos.COSDictionary;
import org.pdfbox.cos.COSName;
import org.pdfbox.cos.COSString;

public class IngentaPdfTransform implements OutputDocumentTransform{

  public IngentaPdfTransform(ArchivalUnit au) {
    // Do nothing
  }

  public boolean transform(PdfDocument pdfDocument,
                           OutputStream outputStream) {
    return PdfUtil.applyAndSave(this, pdfDocument, outputStream);
  }

  public boolean transform(PdfDocument pdfDocument) throws IOException {
    // Remove creation and modifcation dates
    pdfDocument.removeCreationDate();
    pdfDocument.removeModificationDate();
    
    // Normalize trailer ID
    COSDictionary trailer = pdfDocument.getTrailer();
    if (trailer != null) {
      COSArray id = new COSArray();
      id.add(new COSString("12345678901234567890123456789012"));
      id.add(id.get(0));
      trailer.setItem(COSName.getPDFName("ID"), id);
    }

    return true;
  }
  
}
