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

package org.lockss.laaws.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.security.DigestInputStream;
import java.util.Map;
import java.util.Set;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;
import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.io.output.UnsynchronizedByteArrayOutputStream;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.io.DefaultHttpResponseWriter;
import org.apache.http.impl.io.HttpTransportMetricsImpl;
import org.apache.http.impl.io.SessionOutputBufferImpl;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.lockss.laaws.DigestCachedUrl;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.CIProperties;
import org.lockss.util.Logger;

public class CachedUrlRequestBody extends RequestBody {

  private static final Logger log = Logger.getLogger(CachedUrlRequestBody.class);
  protected static StatusLine STATUS_LINE_OK =
    new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, "OK");

  private final MediaType contentType;
  private final CachedUrl artifactCu;
  private final DigestCachedUrl dcu;
  private CountingInputStream cis;

  public CachedUrlRequestBody(DigestCachedUrl dcu, MediaType contentType) {
    if (dcu == null) {
      throw new NullPointerException("cachedUrl == null");
    }
    this.contentType = contentType;
    this.artifactCu = dcu.getCu();
    this.dcu =dcu;
  }

  /**
   * Adapter that takes an {@code CachedUrl} and returns an InputStream containing an HTTP response stream
   * representation of the artifact.
   *
   * @return An {@code InputStream} containing an HTTP response stream representation of the artifact.
   * @throws IOException
   * @throws HttpException
   */
  public InputStream getHttpResponseStreamFromCachedUrl()
    throws IOException {
    InputStream httpResponse = getHttpResponseStreamFromHttpResponse(
      getHttpResponseFromCachedUrl()
    );
    return httpResponse;
  }

  /**
   * Adapts an {@code HttpResponse} object to an InputStream containing a HTTP response stream representation of the
   * {@code HttpResponse} object.
   *
   * @param response A {@code HttpResponse} to adapt.
   * @return An {@code InputStream} containing a HTTP response stream representation of this {@code HttpResponse}.
   * @throws IOException
   */
  public InputStream getHttpResponseStreamFromHttpResponse(HttpResponse response)
    throws IOException {
    // Return the concatenation of the header and content streams
    return new SequenceInputStream(
      new ByteArrayInputStream(getHttpResponseHeader(response)),
      response.getEntity().getContent()
    );
  }

  /**
   * Return a byte arroy representation of an HttpResponse
   *
   * @param response
   * @return
   * @throws IOException
   */
  public byte[] getHttpResponseHeader(HttpResponse response) throws IOException {
    UnsynchronizedByteArrayOutputStream headerStream = new UnsynchronizedByteArrayOutputStream();

    // Create a new SessionOutputBuffer from the OutputStream
    SessionOutputBufferImpl outputBuffer = new SessionOutputBufferImpl(
      new HttpTransportMetricsImpl(), 4096);
    outputBuffer.bind(headerStream);

    // Write the HTTP response header
    writeHttpResponseHeader(response, outputBuffer);

    // Flush anything remaining in the buffer
    outputBuffer.flush();

    return headerStream.toByteArray();
  }

  /**
   * Writes a {@code HttpResponse} object's HTTP status and headers to an {@code OutputStream}.
   *
   * @param response     A {@code HttpResponse} whose HTTP status and headers will be written to the {@code OutputStream}.
   * @param outputBuffer The {@code OutputStream} to write to.
   * @throws IOException
   */
  private static void writeHttpResponseHeader(HttpResponse response,
    SessionOutputBufferImpl outputBuffer) throws IOException {
    try {
      // Write the HTTP response header
      DefaultHttpResponseWriter responseWriter = new DefaultHttpResponseWriter(outputBuffer);
      responseWriter.write(response);
    } catch (HttpException e) {
      log.error(
        "Caught HttpException while attempting to write the headers of an HttpResponse using DefaultHttpResponseWriter");
      throw new IOException(e);
    }
  }

  /**
   * Adapter that takes an {@code CachedUrl} and returns an Apache {@code HttpResponse} object representation of
   * the artifact.
   *
   * @return An {@code HttpResponse} object containing a representation of the artifact.
   * @throws HttpException
   * @throws IOException
   */
  public HttpResponse getHttpResponseFromCachedUrl() {
    // Craft a new HTTP response object representation from the artifact
    BasicHttpResponse response = new BasicHttpResponse(STATUS_LINE_OK);
    // Create an InputStreamEntity from artifact InputStream
    try {
      cis = new CountingInputStream(artifactCu.getUnfilteredInputStream());
      dcu.setContentCountingInputStream(cis);
      DigestInputStream dis = new DigestInputStream(cis,
                                                    dcu.createMessageDigest());
      response.setEntity(new InputStreamEntity(dis));
      // Add artifact headers into HTTP response
      CIProperties props = artifactCu.getProperties();
      if (props != null) {
        ((Set<String>) ((Map) props).keySet()).forEach(
          key -> response.addHeader(key, props.getProperty(key)));
      }
    }
    catch (Exception ex) {
      log.error("Unable to open input stream for " + artifactCu.getUrl(), ex);
      AuUtil.safeRelease(artifactCu);
    }
    return response;
  }

  @Override
  public MediaType contentType() {
    return contentType;
  }

  @Override
  public long contentLength() throws IOException {
    //return inputStream.available() == 0 ? -1 : inputStream.available();
    return -1L;
  }

  @Override
  public void writeTo(BufferedSink sink) throws IOException {
    Source source = null;
    try {
      InputStream inputStream = getHttpResponseStreamFromCachedUrl();
      log.debug3("Writing " + (inputStream == null ? "(null) " : "") +
                 artifactCu);
      if (inputStream != null) {
        CountingInputStream cis = new CountingInputStream(inputStream);
        dcu.setTotalCountingInputStream(cis);
        source = Okio.source(cis);
        sink.writeAll(source);
        long avail = inputStream.available();
        if (avail > 0) {
          log.error("Still CU bytes available after sink.writeAll(): " + avail);
        }
      }
    } catch (Exception e) {
      log.error("Exception writing CU body to V2: " + artifactCu, e);
      throw e;
    } finally {
      Util.closeQuietly(source);
      AuUtil.safeRelease(artifactCu);
    }
  }
}
