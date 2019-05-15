package org.lockss.plugin.atypon;

import org.lockss.daemon.PluginException;
import org.lockss.pdf.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.util.Logger;
import java.io.*;

public class BaseAtyponPageCountPdfFactory implements FilterFactory {

  private static final Logger log = Logger.getLogger(BaseAtyponPageCountPdfFactory.class);
  private static final String PDF_HASH_STRING_FORMAT = "This PDF file has: %s pages total";

  /**
   * <p>
   * This instance's PDF document factory.
   * </p>
   * @since 1.56
   */
  protected PdfDocumentFactory pdfDocumentFactory;

  /**
   *
   * A filter factory that interprets its input as a PDF document and
   * generate a fixed format string contains its page count
   * @param pdfDocumentFactory
   */
  public BaseAtyponPageCountPdfFactory(PdfDocumentFactory pdfDocumentFactory) {
    this.pdfDocumentFactory = pdfDocumentFactory;
  }

  /**
   * <p>
   * Makes an instance using {@link DefaultPdfDocumentFactory}.
   * </p>
   * @since 1.56
   * @see DefaultPdfDocumentFactory
   */
  public BaseAtyponPageCountPdfFactory() {
    this(DefaultPdfDocumentFactory.getInstance());
  }

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au, InputStream in, String encoding) throws PluginException {
    PdfDocument pdfDocument = null;
    int pageCount = 0;

    try {
      pdfDocument = pdfDocumentFactory.makeDocument(in);
      pageCount = pdfDocument.getNumberOfPages();

      String pageCountHash = String.format(PDF_HASH_STRING_FORMAT, String.valueOf(pageCount));
      InputStream pageCountHashInputStream = new ByteArrayInputStream(pageCountHash.toString().getBytes());

      return pageCountHashInputStream;

    } catch (IOException ioe) {
      throw new PluginException(ioe);
    } catch (PdfException pdfe) {
      throw new PluginException(pdfe);
    } finally {
      PdfUtil.safeClose(pdfDocument);
    }
  }
}
