/*
 * Copyright (c) 2026, Board of Trustees of Leland Stanford Jr. University.
 * All rights reserved.
 */
package org.lockss.laaws.mock;

import java.util.*;

/** A single HTTP request received by the {@link V2RequestHandler}, captured
 * for later inspection by tests.  Immutable after construction. */
public class RecordedRequest {
  public final long timeMs;
  public final String service;          // "cfg" or "rs"
  public final String method;           // GET, POST, PUT, etc.
  public final String path;             // path without query string
  public final String query;            // raw query string (may be null)
  public final Map<String,List<String>> headers;
  public final byte[] body;             // raw request body (may be empty)

  public RecordedRequest(String service, String method, String path,
                         String query,
                         Map<String,List<String>> headers, byte[] body) {
    this.timeMs = System.currentTimeMillis();
    this.service = service;
    this.method = method;
    this.path = path;
    this.query = query;
    this.headers = headers;
    this.body = body == null ? new byte[0] : body;
  }

  public String getMethod() { return method; }
  public String getPath()   { return path; }
  public String getQuery()  { return query; }
  public byte[] getBody()   { return body; }

  /** Find the first value of the named query parameter, or null. */
  public String queryParam(String name) {
    if (query == null) return null;
    for (String pair : query.split("&")) {
      int eq = pair.indexOf('=');
      String k = eq < 0 ? pair : pair.substring(0, eq);
      String v = eq < 0 ? "" : pair.substring(eq + 1);
      if (k.equals(name)) {
        try {
          return java.net.URLDecoder.decode(v, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
          throw new AssertionError(e);
        }
      }
    }
    return null;
  }

  @Override public String toString() {
    return method + " " + path + (query != null ? "?" + query : "");
  }
}
