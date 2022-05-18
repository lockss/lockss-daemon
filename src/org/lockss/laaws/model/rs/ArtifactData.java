/*
 *  Copyright (c) 2022, Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.laaws.model.rs;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import okhttp3.Headers;
import okhttp3.MultipartReader;
import okhttp3.MultipartReader.Part;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.StatusLine;
import org.apache.http.impl.io.HttpTransportMetricsImpl;
import org.apache.http.impl.io.SessionInputBufferImpl;
import org.apache.http.message.BasicLineParser;
import org.lockss.laaws.CuChecker;
import org.lockss.util.FileUtil;
import org.lockss.util.Logger;
import org.springframework.http.HttpHeaders;

/**
 * An {@code ArtifactData} serves as an atomic unit of data archived in the
 * LOCKSS Repository.
 * <br>
 * Reusability and release:<ul>
 * <li>Once an ArtifactData is obtained, it <b>must</b> be released (by
 * calling {@link #release()}, whether or not {@link #getInputStream()} has
 * been called.
 * </ul>
 */
public class ArtifactData implements AutoCloseable {

  private static final Logger log = Logger.getLogger(CuChecker.class);
  public static final String CONTENT_DISPOSITION = "Content-Disposition";
  public static final String MULTIPART_ARTIFACT_REPO_PROPS = "artifact-repo-props";
  public static final String MULTIPART_ARTIFACT_HEADER = "artifact-header";
  public static final String MULTIPART_ARTIFACT_HTTP_STATUS = "artifact-http-status";
  public static final String MULTIPART_ARTIFACT_CONTENT = "artifact-content";
  // Artifact identity
  public static final String ARTIFACT_ID_KEY = "X-LockssRepo-Artifact-Id";
  public static final String ARTIFACT_COLLECTION_KEY = "X-LockssRepo-Artifact-Collection";
  public static final String ARTIFACT_AUID_KEY = "X-LockssRepo-Artifact-AuId";
  public static final String ARTIFACT_URI_KEY = "X-LockssRepo-Artifact-Uri";
  public static final String ARTIFACT_VERSION_KEY = "X-LockssRepo-Artifact-Version";

  // Repository state
  public static final String ARTIFACT_STATE_COMMITTED = "X-LockssRepo-Artifact-Committed";
  public static final String ARTIFACT_STATE_DELETED = "X-LockssRepo-Artifact-Deleted";

  // Repository
  public static final String ARTIFACT_LENGTH_KEY = "X-LockssRepo-Artifact-Length";
  public static final String ARTIFACT_DIGEST_KEY = "X-LockssRepo-Artifact-Digest";

  // Miscellaneous
  public static final String ARTIFACT_ORIGIN_KEY = "X-LockssRepo-Artifact-Origin";
  public static final String ARTIFACT_STORED_DATE = "X-LockssRepo-Artifact-StoredDate";

  // Core artifact attributes
  private ArtifactIdentifier identifier;

  // Artifact data stream
  private InputStream artifactStream;

  // Artifact data properties
  private HttpHeaders artifactMetadata;
  private StatusLine httpStatus;
  private InputStream origInputStream;
  private long contentLength = -1;
  private String contentDigest;

  // Internal repository state
  private ArtifactRepositoryState artifactRepositoryState;
  private URI storageUrl;

  // The collection date.
  private long collectionDate = -1;
  private long storedDate = -1;

  private boolean isReleased;

  Headers respHeaders;
  File contentFile;

  long artifactDataSize;

  public ArtifactData(MultipartReader mpReader, Headers respHeaders) throws IOException {
    this.respHeaders = respHeaders;
    artifactDataSize = respHeaders.byteCount();
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    while (true) {
      try {
        Part part = mpReader.nextPart();
        if (part == null)
          break;
        CountingInputStream cis;
        Headers partHdrs = part.headers();
        String disposition = partHdrs.get(CONTENT_DISPOSITION);
        artifactDataSize += partHdrs.byteCount();

        if (disposition.contains(MULTIPART_ARTIFACT_REPO_PROPS)) {
          cis = new CountingInputStream(part.body().inputStream());
          HttpHeaders hdrs = mapper.readValue(cis, HttpHeaders.class);
          // Set ArtifactIdentifier
          ArtifactIdentifier id = new ArtifactIdentifier(
              hdrs.getFirst(ARTIFACT_ID_KEY),
              hdrs.getFirst(ARTIFACT_COLLECTION_KEY),
              hdrs.getFirst(ARTIFACT_AUID_KEY),
              hdrs.getFirst(ARTIFACT_URI_KEY),
              Integer.valueOf(hdrs.getFirst(ARTIFACT_VERSION_KEY))
          );
          this.identifier = id;
          String committedHeaderValue = hdrs.getFirst(ARTIFACT_STATE_COMMITTED);
          String deletedHeaderValue = hdrs.getFirst(ARTIFACT_STATE_DELETED);
          if (!(StringUtils.isEmpty(committedHeaderValue) || StringUtils.isEmpty(
              deletedHeaderValue))) {
            this.artifactRepositoryState = new ArtifactRepositoryState(
                id,
                Boolean.parseBoolean(hdrs.getFirst(ARTIFACT_STATE_COMMITTED)),
                Boolean.parseBoolean(hdrs.getFirst(ARTIFACT_STATE_DELETED))
            );
          }
          // Set misc. artifact properties
          this.contentLength = Long.parseLong(hdrs.getFirst(ARTIFACT_LENGTH_KEY));
          this.contentDigest = hdrs.getFirst(ARTIFACT_DIGEST_KEY);
          artifactDataSize += cis.getByteCount();
          part.close();
        }
        else if (disposition.contains(MULTIPART_ARTIFACT_HEADER)) {
          cis = new CountingInputStream(part.body().inputStream());
          this.artifactMetadata = mapper.readValue(cis, HttpHeaders.class);
          artifactDataSize += cis.getByteCount();
          part.close();
        }
        else if (disposition.contains(MULTIPART_ARTIFACT_HTTP_STATUS)) {
          cis = new CountingInputStream(part.body().inputStream());
          // Create a SessionInputBuffer and bind the InputStream from the multipart
          SessionInputBufferImpl buffer = new SessionInputBufferImpl(new HttpTransportMetricsImpl(),
              4096);
          buffer.bind(cis);
          // Read and parse HTTP status line
          StatusLine httpStatus = BasicLineParser.parseStatusLine(buffer.readLine(), null);
          this.setHttpStatus(httpStatus);
          artifactDataSize += cis.getByteCount();
          part.close();
        }
        else if (disposition.contains(MULTIPART_ARTIFACT_CONTENT)) {
          cis = new CountingInputStream(part.body().inputStream());
          contentFile = fileFromContentDisposition(disposition);
          BufferedSink sink = Okio.buffer(Okio.sink(contentFile));
          sink.writeAll(Okio.buffer(Okio.source(cis)));
          contentLength = cis.getByteCount();
          artifactDataSize += contentLength;
          sink.close();
          part.close();
        }
      }
      catch (Exception ex) {
        log.error("Unable to read ArtifactData",ex);
        throw new IOException("Unable to read ArtifactData", ex);
      }
    }
  }


  /**
   * Returns additional key-value properties associated with this artifact.
   *
   * @return A {@code HttpHeaders} containing this artifact's additional properties.
   */
  public HttpHeaders getMetadata() {
    return artifactMetadata;
  }

  public void setMetadata(HttpHeaders headers) {
    this.artifactMetadata = headers;
  }

  /**
   * Returns this artifact's HTTP response status if it originated from a web server.
   *
   * @return A {@code StatusLine} containing this artifact's HTTP response status.
   */
  public StatusLine getHttpStatus() {
    return this.httpStatus;
  }

  public void setHttpStatus(StatusLine status) {
    this.httpStatus = status;
  }

  /**
   * Return this artifact data's artifact identifier.
   *
   * @return An {@code ArtifactIdentifier}.
   */
  public ArtifactIdentifier getIdentifier() {
    return this.identifier;
  }

  /**
   * Sets an artifact identifier for this artifact data.
   *
   * @param identifier An {@code ArtifactIdentifier} for this artifact data.
   * @return This {@code ArtifactData} with its identifier set to the one provided.
   */
  public ArtifactData setIdentifier(ArtifactIdentifier identifier) {
    this.identifier = identifier;
    return this;
  }

  /**
   * Returns this artifact's byte stream in a one-time use {@code InputStream}.
   *
   * @return An {@code InputStream} containing this artifact's byte stream.
   */
  public synchronized InputStream getInputStream() {
    // Comment in to log creation point of unused InputStreams
    if (contentFile != null && contentFile.exists() && contentFile.canRead()) {
      try {
        artifactStream = new BufferedInputStream(new FileInputStream(contentFile));
        return artifactStream;
      }
      catch (FileNotFoundException ex) {
        throw new IllegalStateException(
            "Attempt to get InputStream from ArtifactData with no data.");
      }
    }
    return null;
  }

  /**
   * Returns the repository state information for this artifact data.
   *
   * @return A {@code RepositoryArtifactMetadata} containing the repository state information for
   * this artifact data.
   */
  public ArtifactRepositoryState getArtifactRepositoryState() {
    return artifactRepositoryState;
  }

  /**
   * Sets the repository state information for this artifact data.
   *
   * @param metadata A {@code RepositoryArtifactMetadata} containing the repository state
   * information for this artifact.
   * @return this ArtifactData object
   */
  public ArtifactData setArtifactRepositoryState(ArtifactRepositoryState metadata) {
    this.artifactRepositoryState = metadata;
    return this;
  }

  /**
   * Returns the location where the byte stream for this artifact data can be found.
   *
   * @return A {@code String} containing the storage of this artifact data.
   */
  public URI getStorageUrl() {
    return storageUrl;
  }

  /**
   * Sets the location where the byte stream for this artifact data can be found.
   *
   * @param storageUrl A {@code String} containing the location of this artifact data.
   */
  public void setStorageUrl(URI storageUrl) {
    this.storageUrl = storageUrl;
  }

  public String getContentDigest() {
    if (contentDigest == null) {
      throw new RuntimeException("Content digest has not been set");
    }
    return contentDigest;
  }

  public void setContentDigest(String contentDigest) {
    this.contentDigest = contentDigest;
  }

  public long getContentLength() {
    return contentLength;
  }

  public void setContentLength(long contentLength) {
    this.contentLength = contentLength;
  }

  /**
   * Provides the artifact collection date.
   *
   * @return a long with the artifact collection date in milliseconds since the epoch.
   */
  public long getCollectionDate() {
    return collectionDate;
  }

  /**
   * Saves the artifact collection date.
   *
   * @param collectionDate A long with the artifact collection date in milliseconds since the
   * epoch.
   */
  public void setCollectionDate(long collectionDate) {
    if (collectionDate >= 0) {
      this.collectionDate = collectionDate;
    }
  }

  @Override
  public String toString() {
    return "[ArtifactData identifier=" + identifier + ", artifactMetadata="
        + artifactMetadata + ", httpStatus=" + httpStatus
        + ", artifactRepositoryState=" + artifactRepositoryState + ", storageUrl="
        + storageUrl + ", contentDigest=" + contentDigest
        + ", contentLength=" + contentLength + ", collectionDate="
        + getCollectionDate() + "]";
  }

  @Override
  public void close() throws IOException {
    if (artifactStream != null) {
      IOUtils.close(artifactStream);
    }
  }

  /**
   * Releases resources used.
   */
  public synchronized void release() {
    if (!isReleased) {
      IOUtils.closeQuietly(artifactStream);
      if(contentFile != null) {
        FileUtil.safeDeleteFile(contentFile);
        contentFile = null;
      }
      isReleased = true;
    }
  }


  public long getStoredDate() {
    return storedDate;
  }

  public void setStoredDate(long storedDate) {
    this.storedDate = storedDate;
  }

   public long  getSize() {
    return artifactDataSize;
  }

  File fileFromContentDisposition(String contentDisposition) throws IOException{
    String filename = null;
    if (contentDisposition != null && !"".equals(contentDisposition)) {
      // Get filename from the Content-Disposition header.
      Pattern pattern = Pattern.compile("filename=['\"]?([^'\"\\s]+)['\"]?");
      Matcher matcher = pattern.matcher(contentDisposition);
      if (matcher.find()) {
        filename = sanitizeFilename(matcher.group(1));
      }
    }

    String prefix = null;
    String suffix = null;
    if (filename == null) {
      prefix = "download-";
      suffix = "";
    }
    else {
      int pos = filename.lastIndexOf(".");
      if (pos == -1) {
        prefix = filename + "-";
      }
      else {
        prefix = filename.substring(0, pos) + "-";
        suffix = filename.substring(pos);
      }
      // Files.createTempFile requires the prefix to be at least three characters long
      if (prefix.length() < 3) {
        prefix = "download-";
      }
      return FileUtil.createTempFile(prefix, suffix);
    }
    return null;
  }

  /**
   * Sanitize filename by removing path. e.g. ../../sun.gif becomes sun.gif
   *
   * @param filename The filename to be sanitized
   * @return The sanitized filename
   */
  public String sanitizeFilename(String filename) {
    return filename.replaceAll(".*[/\\\\]", "");
  }

}
