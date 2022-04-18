/*
 * 2022, Board of Trustees of Leland Stanford Jr. University,
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.lockss.laaws;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.activation.DataSource;
import javax.mail.internet.MimeMultipart;
import okhttp3.Headers;
import org.apache.commons.io.FileUtils;
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
    MimeMultipart multipart = null;
    String contentType = null;
    if (mpFile != null) {
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
    if (responseHeaders != null) {
      String contentType = responseHeaders.get(CONTENT_TYPE);
      if (!StringUtil.isNullString(contentType)) {
        boundary = contentType.split(";")[1];
        int b_indx = boundary.indexOf(BOUNDARY_MARKER);
        boundary = boundary.substring(b_indx + BOUNDARY_MARKER.length()).trim();
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
    if (mpFile != null) {
      FileUtils.deleteQuietly(mpFile);
      mpFile = null;
    }
  }

  private void writeHeaderFile(Headers hdrs) {

  }

  private static class InputStreamDataSource implements DataSource {

    private final InputStream inputStream;
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
