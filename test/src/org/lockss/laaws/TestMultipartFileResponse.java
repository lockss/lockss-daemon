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

import java.io.File;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import okhttp3.Headers;
import org.lockss.test.LockssTestCase;
import org.lockss.util.FileUtil;

/**
 * MultipartFileResponse Tester.
 */
public class TestMultipartFileResponse extends LockssTestCase {

  Headers mpHeaders;
  File mpFile;
  MultipartFileResponse mpfResponse;
  String tempDirPath;
  Map<String, String> hdrMap = Stream.of(new String[][]{
      {"Hello", "World"},
      {"John", "Doe"},
  }).collect(Collectors.toMap(data -> data[0], data -> data[1]));
  String mpFileContents = "--SW1NdBeKdGkZY2DundzDWq4eqosBqkOQOMPcZX\n"
      + "Content-Disposition: form-data; name=\"artifact-repo-props\"\n"
      + "Content-Type: application/json\n"
      + "\n"
      + "{\"X-LockssRepo-Artifact-Id\":[\"5d5cf118-256c-4cec-9941-25bc4fc0a0d2\"],\"X-LockssRepo-Artifact-Collection\":[\"lockss\"],\"X-LockssRepo-Artifact-AuId\":[\"org|lockss|plugin|georgthiemeverlag|GeorgThiemeVerlagPlugin&base_url~https%3A%2F%2Fwww%2Ethieme-connect%2Ede%2F&journal_id~10%2E1055%2Fs-00000002&volume_name~2010\"],\"X-LockssRepo-Artifact-Uri\":[\"https://www.thieme-connect.de/lockss.txt\"],\"X-LockssRepo-Artifact-Version\":[\"1\"],\"X-LockssRepo-Artifact-Committed\":[\"true\"],\"X-LockssRepo-Artifact-Deleted\":[\"false\"],\"X-LockssRepo-Artifact-Length\":[\"81\"],\"X-LockssRepo-Artifact-Digest\":[\"SHA-256:979d24c5c88faf425b75f5f5065108ee18e4b651d573af25dc3891981224cfc5\"]}\n"
      + "--SW1NdBeKdGkZY2DundzDWq4eqosBqkOQOMPcZX\n"
      + "Content-Disposition: form-data; name=\"artifact-header\"\n"
      + "Content-Type: application/json\n"
      + "\n"
      + "{\"x-lockss-node-url\":[\"https://www.thieme-connect.de/lockss.txt\"],\"connection\":[\"keep-alive\"],\"x_lockss-server-date\":[\"1632333155297\"],\"accept-ranges\":[\"bytes\"],\"date\":[\"Wed, 22 Sep 2021 17:52:35 GMT\"],\"server\":[\"cloudflare\"],\"content-length\":[\"81\"],\"cf-cache-status\":[\"DYNAMIC\"],\"expect-ct\":[\"max-age=604800, report-uri=\\\"https://report-uri.cloudflare.com/cdn-cgi/beacon/expect-ct\\\"\"],\"cf-ray\":[\"692d63482e4824d0-SJC\"],\"set-cookie\":[\"FIZ-Cookie=204030605.16671.0000; path=/; Httponly; Secure\"],\"etag\":[\"\\\"27b07001-51-51ca1f7b993c0\\\"\"],\"last-modified\":[\"Thu, 06 Aug 2015 10:27:35 GMT\"],\"strict-transport-security\":[\"max-age=31536000 ; includeSubDomains\"],\"org.lockss.version.number\":[\"1\"],\"content-type\":[\"text/plain\"],\"x-lockss-content-type\":[\"text/plain\"]}\n"
      + "--SW1NdBeKdGkZY2DundzDWq4eqosBqkOQOMPcZX\n"
      + "Content-Disposition: form-data; name=\"artifact-http-status\"; filename=\"5d5cf118-256c-4cec-9941-25bc4fc0a0d2\"\n"
      + "Content-Type: application/octet-stream\n"
      + "Content-Length: 17\n"
      + "\n"
      + "HTTP/1.1 200 OK\n"
      + "\n"
      + "--SW1NdBeKdGkZY2DundzDWq4eqosBqkOQOMPcZX\n"
      + "Content-Disposition: form-data; name=\"artifact-content\"; filename=\"5d5cf118-256c-4cec-9941-25bc4fc0a0d2\"\n"
      + "Content-Type: application/octet-stream\n"
      + "Content-Length: 81\n"
      + "\n"
      + "\n"
      + "LOCKSS system has permission to collect, preserve, and serve this Archival Unit\n"
      + "--SW1NdBeKdGkZY2DundzDWq4eqosBqkOQOMPcZX--\n";

  public void setUp() throws Exception {
    super.setUp();
    mpFile = FileUtil.createTempFile("multipart", "");
    mpHeaders = Headers.of(hdrMap);
    mpfResponse = new MultipartFileResponse(mpFile, mpHeaders);
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   * Method: getFile()
   */
  public void testGetFile() throws Exception {
//TODO: Test goes here... 
  }

  /**
   * Method: setFile(File mpFile)
   */
  public void testSetFile() throws Exception {
//TODO: Test goes here... 
  }

  /**
   * Method: getResponseHeaders()
   */
  public void testGetResponseHeaders() throws Exception {
//TODO: Test goes here... 
  }

  /**
   * Method: setResponseHeaders(Headers responseHeaders)
   */
  public void testSetResponseHeaders() throws Exception {
//TODO: Test goes here... 
  }

  /**
   * Method: getMimeMultipart()
   */
  public void testGetMimeMultipart() throws Exception {
//TODO: Test goes here... 
  }

  /**
   * Method: getBoundary()
   */
  public void testGetBoundary() throws Exception {
//TODO: Test goes here... 
  }

  /**
   * Method: getSize()
   */
  public void testGetSize() throws Exception {
//TODO: Test goes here... 
  }

  /**
   * Method: toString()
   */
  public void testToString() throws Exception {
//TODO: Test goes here... 
  }

  /**
   * Method: delete()
   */
  public void testDelete() throws Exception {
//TODO: Test goes here... 
  }

  /**
   * Method: getInputStream()
   */
  public void testGetInputStream() throws Exception {
//TODO: Test goes here... 
  }

  /**
   * Method: getOutputStream()
   */
  public void testGetOutputStream() throws Exception {
//TODO: Test goes here... 
  }

  /**
   * Method: getContentType()
   */
  public void testGetContentType() throws Exception {
//TODO: Test goes here... 
  }

  /**
   * Method: getName()
   */
  public void testGetName() throws Exception {
//TODO: Test goes here... 
  }


} 
