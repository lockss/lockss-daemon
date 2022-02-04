package org.lockss.plugin.cloudpublish;

import org.apache.commons.io.input.ProxyInputStream;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.util.Logger;

import java.io.IOException;
import java.io.InputStream;

public class CloudPublishOctetStreamFilterFactory implements FilterFactory {

  private static final Logger logger = Logger.getLogger(CloudPublishOctetStreamFilterFactory.class);

  protected FilterFactory pdfFilterFactory = new CloudPublishPdfFilterFactory();

  public static class PdfPeekInputStream extends ProxyInputStream {

    protected boolean isPdf;

    protected byte[] buffer;

    protected int consumed;

    protected int returned;

    protected static final int bufferSize = 4;

    public PdfPeekInputStream(InputStream in) throws IOException {
      super(in);
      buffer = new byte[bufferSize];
      consumed = super.read(buffer);
      returned = 0;
      isPdf = (   (consumed == bufferSize)
          && (buffer[0] == '%')
          && (buffer[1] == 'P')
          && (buffer[2] == 'D')
          && (buffer[3] == 'F'));
    }

    @Override
    public int read() throws IOException {
      if (returned >= consumed) {
        return super.read();
      }
      int ret = buffer[returned];
      ++returned;
      return ret;
    }

    @Override
    public int read(byte[] b) throws IOException {
      return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      if (returned >= consumed) {
        return super.read(b, off, len);
      }
      int available = consumed - returned;
      int toBeProcessed = ((available <= len) ? available : len);
      for (int i = 0 ; i < toBeProcessed ; ++i) {
        b[off + i] = buffer[returned + i];
      }
      returned += toBeProcessed;
      return toBeProcessed;
    }

  }

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    try {
      PdfPeekInputStream pdfpeek = new PdfPeekInputStream(in);
      if (pdfpeek.isPdf) {
        logger.debug2("Detected PDF"); // expected to be PDF
        return pdfFilterFactory.createFilteredInputStream(au, pdfpeek, encoding);
      }
      else {
        logger.debug2("Unfiltered"); // expected to be EPUB
        return pdfpeek;
      }
    }
    catch (IOException ioe) {
      throw new PluginException("Error while peeking into the input stream", ioe);
    }
  }

}
