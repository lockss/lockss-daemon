/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2008 James Murty
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jets3t.service.impl.rest.httpclient;

import org.apache.commons.httpclient.HttpMethodBase;

/**
   * Simple container object to store an HttpMethod object representing a request connection, and a
   * count of the byte size of the S3 object associated with the request.
   * <p>
   * This object is used when S3 objects are created to associate the connection and the actual size
   * of the object as reported back by S3.
   *
   * @author James Murty
   */
public class HttpMethodAndByteCount {
    private HttpMethodBase httpMethod = null;
    private long byteCount = 0;

    public HttpMethodAndByteCount(HttpMethodBase httpMethod, long byteCount) {
        this.httpMethod = httpMethod;
        this.byteCount = byteCount;
    }

    public HttpMethodBase getHttpMethod() {
        return httpMethod;
    }

    public long getByteCount() {
        return byteCount;
    }
}
