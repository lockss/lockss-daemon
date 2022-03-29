package org.lockss.laaws;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import javax.activation.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.mail.internet.MimeMultipart;
import okhttp3.Headers;
import org.apache.commons.io.FileUtils;
import org.lockss.util.FileUtil;
import org.lockss.util.StringUtil;

public class MultipartFileResponse {
  public static final String CONTENT_TYPE = "Content-Type";
  public static final String BOUNDARY_MARKER = "boundary=";

  File mpFile;
  Headers responseHeaders;

  public MultipartFileResponse(File mpFile, Headers responseHeaders) {
    this.mpFile = mpFile;
    this.responseHeaders = responseHeaders;
  }

  public File getFile() {
    return mpFile;
  }

  public void setFile(File mpFile) {
    this.mpFile = mpFile;
  }

  public Headers getResponseHeaders() {
    return responseHeaders;
  }

  public void setResponseHeaders(Headers responseHeaders) {
    this.responseHeaders = responseHeaders;
  }

  public MimeMultipart getMimeMultipart() throws IOException {
    MimeMultipart multipart =null;
    String contentType = null;
    if(mpFile != null) {
      try {
        if (responseHeaders != null) {
          contentType = responseHeaders.get(CONTENT_TYPE);
        }
        multipart = new MimeMultipart(new InputStreamDataSource(new FileInputStream(mpFile),
            contentType));
      }
      catch (Exception ex) {
        String msg = "Unable to construct multipart from file: " + mpFile.getName();
        throw new IOException(msg, ex);
      }
    }
      return multipart;
  }

  public String getBoundary() {
    String boundary = null;
    if(responseHeaders != null) {
      String contentType =responseHeaders.get(CONTENT_TYPE);
      if(!StringUtil.isNullString(contentType)) {
        boundary = contentType.split(";")[1];
        int b_indx= boundary.indexOf(BOUNDARY_MARKER);
        boundary = boundary.substring(b_indx +BOUNDARY_MARKER.length()).trim();
        // strip any quotes
        if (boundary.startsWith("\"") && boundary.endsWith("\"")) {
          boundary = boundary.substring(1, boundary.length() - 1);
        }
      }
    }
    return boundary;

  }

  public long getSize() {
    return mpFile.length();
  }

  @Override
  public String toString() {
    return "MultipartFileResponse{" +
        "File: " + mpFile.getAbsolutePath() +
        ", responseHeaders: " + responseHeaders +
        '}';
  }
  
  public void delete() {
    if(mpFile != null)   {
      FileUtils.deleteQuietly(mpFile);
      mpFile = null;
    }
  }
  private writeHeaderFile(Headers hdrs) {

  }
  private static class InputStreamDataSource implements DataSource {
    private InputStream inputStream;
    String contentType;

    public InputStreamDataSource(InputStream inputStream, String contentType) {
      this.inputStream = inputStream;
      this.contentType = contentType;
    }

    @Override
    public InputStream getInputStream() throws IOException {
      return new BufferedInputStream(inputStream);
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
      throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public String getContentType() {
      if (contentType != null) {
        return contentType;
      }
      return "multipart/form-data";
    }

    @Override
    public String getName() {
      return "";
    }
  }
}
