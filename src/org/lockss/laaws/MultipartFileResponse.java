package org.lockss.laaws;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import okhttp3.Headers;
import okhttp3.MultipartReader;
import okio.BufferedSource;
import okio.Okio;
import org.apache.commons.io.input.CountingInputStream;
import org.lockss.util.StringUtil;

public class MultipartFileResponse {
  public static final String CONTENT_DISPOSITION = "Content-Disposition";
  public static final String CONTENT_TYPE = "Content-Type";
  public static final String BOUNDARY_MARKER = "boundary=";

  File mpFile;
  Headers responseHeaders;
  CountingInputStream cis;

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

  public MultipartReader getMultipartReader() throws IOException {
    MultipartReader reader=null;
    if(mpFile != null) {
      String boundary = getBoundary();
      if(boundary != null) {
        cis = new CountingInputStream(new FileInputStream(mpFile));
        BufferedSource bs = Okio.buffer(Okio.source(cis));
        reader = new MultipartReader(bs, boundary);
      }
    }
    return reader;
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
}
