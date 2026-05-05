/*
 * Copyright (c) 2026, Board of Trustees of Leland Stanford Jr. University.
 * All rights reserved.
 */
package org.lockss.laaws.mock;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** {@link HttpHandler} implementations for the cfg and rs services.
 * Records every request to {@link MockV2Backend}, consults
 * {@link FailureRules} before producing a normal response. */
public class V2RequestHandler {

  private final MockV2Backend backend;
  private final FailureRules failures;

  public V2RequestHandler(MockV2Backend backend, FailureRules failures) {
    this.backend = backend;
    this.failures = failures;
  }

  public HttpHandler cfgHandler() {
    return new BaseHandler("cfg") {
      @Override void route(Ctx ctx) throws IOException {
        cfgRoute(ctx);
      }
    };
  }

  public HttpHandler rsHandler() {
    return new BaseHandler("rs") {
      @Override void route(Ctx ctx) throws IOException {
        rsRoute(ctx);
      }
    };
  }

  // ---- routing ---------------------------------------------------------

  private void cfgRoute(Ctx ctx) throws IOException {
    String p = ctx.path;
    String m = ctx.method;
    if (m.equals("GET") && p.equals("/status")) {
      ctx.json(200, statusJson("Configuration Service"));
      return;
    }
    if (m.equals("GET") && p.equals("/config/lastupdatetime")) {
      ctx.json(200, "{\"lastupdatetime\":0}");
      return;
    }
    if (m.equals("PUT") && p.startsWith("/config/file/")) {
      String section = p.substring("/config/file/".length());
      backend.configFileMap().put(section, ctx.body);
      ctx.empty(200);
      return;
    }
    // Collection-level cfg AusApi endpoints first; the {auid}-specific
    // catch-all below would otherwise greedily match these literal segments.
    if (m.equals("POST") && p.equals("/aus/add")) {
      backend.auConfigMap().put("__add-" + System.nanoTime(),
                                parseAuConfigBody(ctx.body));
      ctx.empty(200);
      return;
    }
    if (m.equals("DELETE") && p.equals("/aus/delete")) {
      ctx.json(200, "[]");
      return;
    }
    if (m.equals("PUT") && p.equals("/aus/deactivate")) {
      ctx.json(200, "[]");
      return;
    }
    if (m.equals("PUT") && p.equals("/aus/reactivate")) {
      ctx.json(200, "[]");
      return;
    }
    if (m.equals("GET") && p.equals("/aus")) {
      List<String> auids = new ArrayList<>(backend.auConfigMap().keySet());
      ctx.json(200, auConfigListJson(auids));
      return;
    }
    // cfg AusApi: GET/PUT/DELETE /aus/{auid} — single path segment after /aus/
    if (p.startsWith("/aus/") && p.indexOf('/', "/aus/".length()) < 0) {
      String auid = decode(p.substring("/aus/".length()));
      if (m.equals("GET")) {
        Map<String,String> cfg = backend.auConfigMap().get(auid);
        if (cfg == null) { ctx.json(404, "{\"message\":\"not found\"}"); return; }
        ctx.json(200, auConfigJson(auid, cfg));
        return;
      }
      if (m.equals("PUT")) {
        backend.auConfigMap().put(auid, parseAuConfigBody(ctx.body));
        ctx.empty(200);
        return;
      }
      if (m.equals("DELETE")) {
        Map<String,String> cfg = backend.auConfigMap().remove(auid);
        if (cfg == null) { ctx.json(404, "{\"message\":\"not found\"}"); return; }
        ctx.json(200, auConfigJson(auid, cfg));
        return;
      }
    }
    if (m.equals("GET") && p.startsWith("/austatuses/")) {
      String auid = decode(p.substring("/austatuses/".length()));
      ctx.json(200, "{\"volume\":\"" + jsonEscape(auid) + "\",\"status\":\"Ok\"}");
      return;
    }
    if (m.equals("GET") && p.startsWith("/austates/")) {
      String auid = decode(p.substring("/austates/".length()));
      String s = backend.auStateMap().get(auid);
      if (s == null) { ctx.json(404, "{}"); return; }
      ctx.json(200, s);
      return;
    }
    if (m.equals("PATCH") && p.startsWith("/austates/")) {
      String auid = decode(p.substring("/austates/".length()));
      backend.auStateMap().put(auid, new String(ctx.body, StandardCharsets.UTF_8));
      ctx.empty(200);
      return;
    }
    if (m.equals("GET") && p.startsWith("/auagreements/")) {
      String auid = decode(p.substring("/auagreements/".length()));
      String s = backend.auAgreementsMap().get(auid);
      if (s == null) { ctx.json(404, "{}"); return; }
      ctx.json(200, s);
      return;
    }
    if (m.equals("PATCH") && p.startsWith("/auagreements/")) {
      String auid = decode(p.substring("/auagreements/".length()));
      backend.auAgreementsMap().put(auid, new String(ctx.body, StandardCharsets.UTF_8));
      ctx.empty(200);
      return;
    }
    if (m.equals("GET") && p.startsWith("/aususpecturls/")) {
      String auid = decode(p.substring("/aususpecturls/".length()));
      String s = backend.auSuspectMap().get(auid);
      if (s == null) { ctx.json(404, "{}"); return; }
      ctx.json(200, s);
      return;
    }
    if (m.equals("PUT") && p.startsWith("/aususpecturls/")) {
      String auid = decode(p.substring("/aususpecturls/".length()));
      backend.auSuspectMap().put(auid, new String(ctx.body, StandardCharsets.UTF_8));
      ctx.empty(200);
      return;
    }
    if (m.equals("GET") && p.startsWith("/noaupeers/")) {
      String auid = decode(p.substring("/noaupeers/".length()));
      String s = backend.noAuPeersMap().get(auid);
      if (s == null) { ctx.json(404, "{}"); return; }
      ctx.json(200, s);
      return;
    }
    if (m.equals("PUT") && p.startsWith("/noaupeers/")) {
      String auid = decode(p.substring("/noaupeers/".length()));
      backend.noAuPeersMap().put(auid, new String(ctx.body, StandardCharsets.UTF_8));
      ctx.empty(200);
      return;
    }
    if (m.equals("POST") && p.equals("/users")) {
      backend.userMap().put("batch-" + System.nanoTime(),
                            new String(ctx.body, StandardCharsets.UTF_8));
      ctx.empty(200);
      return;
    }
    ctx.json(404, "{\"message\":\"cfg endpoint not implemented: " + m + " " + p + "\"}");
  }

  private void rsRoute(Ctx ctx) throws IOException {
    String p = ctx.path;
    String m = ctx.method;
    if (m.equals("GET") && p.equals("/status")) {
      ctx.json(200, statusJson("Repository Service"));
      return;
    }
    if (m.equals("GET") && p.equals("/repoinfo")) {
      ctx.json(200, repoInfoJson());
      return;
    }
    if (m.equals("GET") && p.equals("/namespaces")) {
      ctx.json(200, "[\"lockss\"]");
      return;
    }
    if (m.equals("GET") && p.equals("/checksumalgorithms")) {
      ctx.json(200, "[\"SHA-256\",\"SHA-1\",\"MD5\"]");
      return;
    }
    if (m.equals("GET") && p.equals("/aus")) {
      // Paginated list of auids in v2.  No pagination needed for tests.
      List<String> auids = new ArrayList<>(backend.auIds());
      ctx.json(200, auidPageInfoJson(auids));
      return;
    }
    if (m.equals("GET") && p.matches("^/aus/[^/]+/size$")) {
      ctx.json(200, "{\"totalAllVersions\":0,\"totalLatestVersions\":0,\"totalWarcSize\":0}");
      return;
    }
    if (m.equals("POST") && p.matches("^/aus/[^/]+/bulk$")) {
      String auid = decode(p.substring("/aus/".length(), p.length() - "/bulk".length()));
      String op = ctx.queryParam("op");
      MockV2Backend.AuRecord rec = backend.getOrCreateAuRecord(auid);
      if ("start".equals(op)) rec.bulkMode.set(true);
      else if ("finish".equals(op)) rec.bulkMode.set(false);
      ctx.empty(200);
      return;
    }
    if (m.equals("GET") && p.equals("/artifacts")) {
      // Query-by-url for a CU
      String auid = ctx.queryParam("auid");
      String url = ctx.queryParam("url");
      String version = ctx.queryParam("version");
      // V2AuMover sends version="all" or "latest"; numeric => exact match.
      Integer wantVersion = null;
      if (version != null && !"all".equalsIgnoreCase(version)
          && !"latest".equalsIgnoreCase(version)) {
        try { wantVersion = Integer.parseInt(version); }
        catch (NumberFormatException ignore) { /* leave null */ }
      }
      List<MockV2Backend.RecordedArtifact> matches = new ArrayList<>();
      for (MockV2Backend.RecordedArtifact ra : backend.artifactMap().values()) {
        if (auid != null && !auid.equals(ra.auid)) continue;
        if (url != null && !url.equals(ra.url)) continue;
        if (wantVersion != null && wantVersion.intValue() != ra.version) continue;
        matches.add(ra);
      }
      ctx.json(200, artifactPageInfoJson(matches));
      return;
    }
    if (m.equals("POST") && p.equals("/artifacts")) {
      MockV2Backend.RecordedArtifact ra = parseAndStoreCreatedArtifact(ctx);
      ctx.json(201, artifactJson(ra, false));
      return;
    }
    if (m.equals("PUT") && p.matches("^/artifacts/[^/]+$")) {
      String uuid = p.substring("/artifacts/".length());
      MockV2Backend.RecordedArtifact ra = backend.artifactMap().get(uuid);
      if (ra == null) { ctx.json(404, "{\"message\":\"unknown artifact\"}"); return; }
      ctx.json(200, artifactJson(ra, true));
      return;
    }
    if (m.equals("GET") && p.matches("^/artifacts/[^/]+$")) {
      String uuid = p.substring("/artifacts/".length());
      MockV2Backend.RecordedArtifact ra = backend.artifactMap().get(uuid);
      if (ra == null) { ctx.json(404, "{\"message\":\"unknown artifact\"}"); return; }
      writeArtifactMultipart(ctx, ra);
      return;
    }
    ctx.json(404, "{\"message\":\"rs endpoint not implemented: " + m + " " + p + "\"}");
  }

  // ---- common request shell --------------------------------------------

  /** Holds per-exchange data, exposes JSON/empty/raw response helpers. */
  static class Ctx {
    final HttpExchange ex;
    final String service;
    final String method;
    final String path;
    final String query;
    final byte[] body;
    Ctx(HttpExchange ex, String service, String method, String path,
        String query, byte[] body) {
      this.ex = ex; this.service = service; this.method = method;
      this.path = path; this.query = query; this.body = body;
    }
    String queryParam(String name) {
      if (query == null) return null;
      for (String pair : query.split("&")) {
        int eq = pair.indexOf('=');
        String k = eq < 0 ? pair : pair.substring(0, eq);
        if (k.equals(name)) {
          return eq < 0 ? "" : decode(pair.substring(eq + 1));
        }
      }
      return null;
    }
    void json(int code, String body) throws IOException {
      byte[] b = body.getBytes(StandardCharsets.UTF_8);
      ex.getResponseHeaders().add("Content-Type", "application/json");
      ex.sendResponseHeaders(code, b.length);
      try (OutputStream os = ex.getResponseBody()) { os.write(b); }
    }
    void empty(int code) throws IOException {
      ex.sendResponseHeaders(code, -1);
      ex.close();
    }
    void raw(int code, String contentType, byte[] body) throws IOException {
      ex.getResponseHeaders().add("Content-Type", contentType);
      ex.sendResponseHeaders(code, body.length);
      try (OutputStream os = ex.getResponseBody()) { os.write(body); }
    }
  }

  abstract class BaseHandler implements HttpHandler {
    private final String service;
    BaseHandler(String service) { this.service = service; }

    @Override public void handle(HttpExchange ex) throws IOException {
      String method = ex.getRequestMethod();
      String path = ex.getRequestURI().getRawPath();
      String query = ex.getRequestURI().getRawQuery();

      // Read full request body so handlers can inspect it.
      byte[] body;
      try (InputStream is = ex.getRequestBody();
           ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) > 0) baos.write(buf, 0, n);
        body = baos.toByteArray();
      }

      // Headers map: lower-cased keys, original list values.
      Map<String,List<String>> hdrs = new LinkedHashMap<>();
      ex.getRequestHeaders().forEach((k, v) -> hdrs.put(k.toLowerCase(), v));

      backend.recordRequest(new RecordedRequest(service, method, path, query,
                                                hdrs, body));

      // Auth check (skip /status which is sometimes pre-auth).
      String auth = first(hdrs.get("authorization"));
      if (!isAuthOk(auth)) {
        ex.getResponseHeaders().add("WWW-Authenticate", "Basic realm=\"lockss\"");
        ex.sendResponseHeaders(401, -1);
        ex.close();
        return;
      }

      // Failure-rule consultation.
      FailureRules.Rule rule = failures.findMatch(method, path);
      if (rule != null) {
        if (rule.getDelayMs() > 0) {
          try { Thread.sleep(rule.getDelayMs()); }
          catch (InterruptedException ignored) {}
        }
        if (rule.isCloseConnection()) {
          // Close without writing any response.
          ex.close();
          return;
        }
        // If a rule sets only a delay (no status, body, malformed, or
        // truncate), fall through to normal handling so the request
        // succeeds — just slowly. This makes delayMs(N) alone mean
        // "honour this request, but slowly," matching test intent.
        boolean ruleHasResponseOverride =
            rule.getStatus() != null
            || rule.getBody() != null
            || rule.isMalformedJson()
            || rule.getTruncateAt() >= 0;
        if (!ruleHasResponseOverride) {
          // Fall through to route().
        } else {
        int code = rule.getStatus() != null ? rule.getStatus() : 500;
        String body0 = rule.isMalformedJson()
          ? "}{not json"
          : (rule.getBody() != null ? rule.getBody() : "");
        byte[] out = body0.getBytes(StandardCharsets.UTF_8);
        if (rule.getTruncateAt() >= 0 && out.length > rule.getTruncateAt()) {
          byte[] truncated = new byte[rule.getTruncateAt()];
          System.arraycopy(out, 0, truncated, 0, truncated.length);
          out = truncated;
        }
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(code, out.length == 0 ? -1 : out.length);
        if (out.length > 0) {
          try (OutputStream os = ex.getResponseBody()) { os.write(out); }
        } else {
          ex.close();
        }
        return;
        }
      }

      try {
        route(new Ctx(ex, service, method, path, query, body));
      } catch (Exception e) {
        e.printStackTrace();
        try {
          String msg = "{\"message\":\"handler error: " + e + "\"}";
          byte[] b = msg.getBytes(StandardCharsets.UTF_8);
          ex.sendResponseHeaders(500, b.length);
          try (OutputStream os = ex.getResponseBody()) { os.write(b); }
        } catch (IOException ignored) {}
      }
    }

    abstract void route(Ctx ctx) throws IOException;
  }

  private boolean isAuthOk(String hdr) {
    if (hdr == null) return false;
    if (!hdr.startsWith("Basic ")) return false;
    String dec = new String(Base64.getDecoder().decode(hdr.substring(6).trim()),
                            StandardCharsets.UTF_8);
    int colon = dec.indexOf(':');
    if (colon < 0) return false;
    return dec.substring(0, colon).equals(backend.getUsername())
      && dec.substring(colon + 1).equals(backend.getPassword());
  }

  private static String first(List<String> v) {
    return v == null || v.isEmpty() ? null : v.get(0);
  }

  private static String decode(String s) {
    try {
      return URLDecoder.decode(s, "UTF-8");
    } catch (java.io.UnsupportedEncodingException e) {
      throw new AssertionError(e);
    }
  }

  // ---- response builders -----------------------------------------------

  private String statusJson(String svcName) {
    return "{\"apiVersion\":\"2.0\",\"serviceName\":\"" + svcName + "\","
      + "\"componentName\":\"mock\",\"componentVersion\":\"2.0.90-beta3\","
      + "\"lockssVersion\":\"2.0.90-beta3\",\"ready\":true,"
      + "\"readyTime\":0,\"startupStatus\":\"AUS_STARTED\"}";
  }

  private String repoInfoJson() {
    String store =
      "{\"type\":\"disk\",\"name\":\"mock\",\"path\":\"/mock/store\","
      + "\"components\":[],\"sizeKB\":1000000,\"usedKB\":1000,\"availKB\":999000,"
      + "\"percentUsed\":0.001,\"percentUsedString\":\"0%\"}";
    String index =
      "{\"type\":\"disk\",\"name\":\"mock-index\",\"path\":\"/mock/index\","
      + "\"components\":[],\"sizeKB\":1000000,\"usedKB\":1000,\"availKB\":999000,"
      + "\"percentUsed\":0.001,\"percentUsedString\":\"0%\"}";
    return "{\"storeInfo\":" + store + ",\"indexInfo\":" + index + "}";
  }

  private String auidPageInfoJson(List<String> auids) {
    StringBuilder sb = new StringBuilder("{\"auids\":[");
    for (int i = 0; i < auids.size(); i++) {
      if (i > 0) sb.append(',');
      sb.append('"').append(jsonEscape(auids.get(i))).append('"');
    }
    sb.append("],\"pageInfo\":{\"totalCount\":").append(auids.size())
      .append(",\"itemsInPage\":").append(auids.size())
      .append(",\"continuationToken\":null,\"curLink\":\"\",\"nextLink\":\"\"}}");
    return sb.toString();
  }

  private String artifactPageInfoJson(List<MockV2Backend.RecordedArtifact> arts) {
    StringBuilder sb = new StringBuilder("{\"artifacts\":[");
    for (int i = 0; i < arts.size(); i++) {
      if (i > 0) sb.append(',');
      sb.append(artifactJson(arts.get(i), true));
    }
    sb.append("],\"pageInfo\":{\"totalCount\":").append(arts.size())
      .append(",\"itemsInPage\":").append(arts.size())
      .append(",\"continuationToken\":null,\"curLink\":\"\",\"nextLink\":\"\"}}");
    return sb.toString();
  }

  private String artifactJson(MockV2Backend.RecordedArtifact ra, boolean committed) {
    StringBuilder sb = new StringBuilder("{");
    sb.append("\"uuid\":\"").append(ra.uuid).append("\",");
    sb.append("\"namespace\":\"").append(jsonEscape(ra.namespace)).append("\",");
    sb.append("\"auid\":\"").append(jsonEscape(ra.auid)).append("\",");
    sb.append("\"uri\":\"").append(jsonEscape(ra.url)).append("\",");
    sb.append("\"version\":").append(ra.version).append(",");
    sb.append("\"contentLength\":").append(ra.contentLength).append(",");
    sb.append("\"contentDigest\":\"").append(jsonEscape(ra.digest)).append("\",");
    sb.append("\"collectionDate\":").append(ra.committedAt).append(",");
    sb.append("\"committed\":").append(committed).append(",");
    sb.append("\"storageUrl\":\"mock://artifact/").append(ra.uuid).append("\"");
    sb.append("}");
    return sb.toString();
  }

  private String auConfigListJson(List<String> auids) {
    StringBuilder sb = new StringBuilder("[");
    boolean first = true;
    for (String auid : auids) {
      if (auid.startsWith("__")) continue;
      Map<String,String> cfg = backend.auConfigMap().get(auid);
      if (cfg == null) cfg = Collections.emptyMap();
      if (!first) sb.append(',');
      first = false;
      sb.append(auConfigJson(auid, cfg));
    }
    sb.append(']');
    return sb.toString();
  }

  private String auConfigJson(String auid, Map<String,String> cfg) {
    StringBuilder sb = new StringBuilder("{\"auId\":\"").append(jsonEscape(auid))
      .append("\",\"auConfig\":{");
    boolean first = true;
    for (Map.Entry<String,String> e : cfg.entrySet()) {
      if (!first) sb.append(',');
      first = false;
      sb.append('"').append(jsonEscape(e.getKey())).append("\":\"")
        .append(jsonEscape(e.getValue())).append('"');
    }
    sb.append("}}");
    return sb.toString();
  }

  private static String jsonEscape(String s) {
    if (s == null) return "";
    StringBuilder sb = new StringBuilder(s.length());
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '"': sb.append("\\\""); break;
        case '\\': sb.append("\\\\"); break;
        case '\n': sb.append("\\n"); break;
        case '\r': sb.append("\\r"); break;
        case '\t': sb.append("\\t"); break;
        default: sb.append(c);
      }
    }
    return sb.toString();
  }

  // ---- multipart parsing for POST /artifacts ---------------------------

  private MockV2Backend.RecordedArtifact parseAndStoreCreatedArtifact(Ctx ctx) {
    String contentType = first(ctx.ex.getRequestHeaders().get("Content-Type"));
    Map<String,byte[]> parts = parseMultipart(ctx.body, contentType);
    byte[] propsBytes = parts.get("artifactProps");
    String propsJson = propsBytes == null ? "{}"
      : new String(propsBytes, StandardCharsets.UTF_8);
    Map<String,String> props = parseSimpleJsonObject(propsJson);
    byte[] payload = parts.get("payload");
    if (payload == null) payload = new byte[0];
    String auid = props.getOrDefault("auid", "unknown");
    String uri = props.getOrDefault("uri", "unknown");
    int version = parseIntOr(props.get("version"), 1);
    String namespace = props.getOrDefault("namespace", "lockss");
    String uuid = UUID.randomUUID().toString();
    String digest = sha256Of(payload);
    long collectionDate = parseLongOr(props.get("collectionDate"),
                                      System.currentTimeMillis());
    MockV2Backend.RecordedArtifact ra =
      new MockV2Backend.RecordedArtifact(uuid, auid, uri, version, payload,
                                         digest, namespace,
                                         collectionDate);
    backend.recordArtifact(ra);
    return ra;
  }

  private static long parseLongOr(String s, long dflt) {
    if (s == null) return dflt;
    try { return Long.parseLong(s.trim()); } catch (Exception e) { return dflt; }
  }

  private static String sha256Of(byte[] payload) {
    try {
      java.security.MessageDigest md =
          java.security.MessageDigest.getInstance("SHA-256");
      byte[] dig = md.digest(payload);
      StringBuilder sb = new StringBuilder("SHA-256:");
      for (byte b : dig) sb.append(String.format("%02x", b));
      return sb.toString();
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private void writeArtifactMultipart(Ctx ctx, MockV2Backend.RecordedArtifact ra)
      throws IOException {
    String boundary = "lockss-mock-" + Long.toHexString(System.nanoTime());
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    String CRLF = "\r\n";
    // Part 1: artifactProps
    out.write(("--" + boundary + CRLF).getBytes(StandardCharsets.UTF_8));
    out.write(("Content-Disposition: form-data; name=\"artifactProps\"" + CRLF
               + "Content-Type: application/json" + CRLF + CRLF)
              .getBytes(StandardCharsets.UTF_8));
    out.write(artifactJson(ra, true).getBytes(StandardCharsets.UTF_8));
    out.write(CRLF.getBytes(StandardCharsets.UTF_8));
    // Part 2: payload
    out.write(("--" + boundary + CRLF).getBytes(StandardCharsets.UTF_8));
    out.write(("Content-Disposition: form-data; name=\"payload\"; filename=\"artifact\"" + CRLF
               + "Content-Type: application/http;msgtype=response" + CRLF + CRLF)
              .getBytes(StandardCharsets.UTF_8));
    out.write(ra.content);
    out.write(CRLF.getBytes(StandardCharsets.UTF_8));
    out.write(("--" + boundary + "--" + CRLF).getBytes(StandardCharsets.UTF_8));
    ctx.raw(200, "multipart/form-data; boundary=" + boundary, out.toByteArray());
  }

  /** Minimal RFC2046-ish multipart parser. Only handles the structure the
   * V2RestClient produces (Content-Disposition with name=, optional filename,
   * possibly with Content-Type). */
  static Map<String,byte[]> parseMultipart(byte[] body, String contentType) {
    Map<String,byte[]> parts = new LinkedHashMap<>();
    if (contentType == null) return parts;
    String boundary = null;
    for (String tok : contentType.split(";")) {
      tok = tok.trim();
      if (tok.startsWith("boundary=")) {
        boundary = tok.substring("boundary=".length()).trim();
        if (boundary.startsWith("\"") && boundary.endsWith("\"")) {
          boundary = boundary.substring(1, boundary.length() - 1);
        }
      }
    }
    if (boundary == null) return parts;
    byte[] sep = ("--" + boundary).getBytes(StandardCharsets.UTF_8);
    int idx = 0;
    while (idx < body.length) {
      int hit = indexOf(body, sep, idx);
      if (hit < 0) break;
      int after = hit + sep.length;
      // Could be end marker "--" or part start CRLF.
      if (after + 2 <= body.length && body[after] == '-' && body[after+1] == '-') break;
      // Skip CRLF after boundary.
      if (after + 2 <= body.length && body[after] == '\r' && body[after+1] == '\n') after += 2;
      // Find end of part headers.
      byte[] hdrEnd = new byte[]{'\r','\n','\r','\n'};
      int headEnd = indexOf(body, hdrEnd, after);
      if (headEnd < 0) break;
      String headers = new String(body, after, headEnd - after, StandardCharsets.UTF_8);
      int dataStart = headEnd + 4;
      int nextHit = indexOf(body, sep, dataStart);
      if (nextHit < 0) break;
      // Trim trailing CRLF before the boundary.
      int dataEnd = nextHit;
      if (dataEnd >= 2 && body[dataEnd-2] == '\r' && body[dataEnd-1] == '\n') dataEnd -= 2;
      byte[] partData = Arrays.copyOfRange(body, dataStart, dataEnd);
      String name = parsePartName(headers);
      if (name != null) parts.put(name, partData);
      idx = nextHit;
    }
    return parts;
  }

  private static String parsePartName(String headers) {
    for (String line : headers.split("\r?\n")) {
      String low = line.toLowerCase();
      if (low.startsWith("content-disposition:")) {
        int n = low.indexOf("name=\"");
        if (n < 0) return null;
        int start = n + 6;
        int end = line.indexOf('"', start);
        if (end < 0) return null;
        return line.substring(start, end);
      }
    }
    return null;
  }

  private static int indexOf(byte[] hay, byte[] needle, int from) {
    outer: for (int i = from; i <= hay.length - needle.length; i++) {
      for (int j = 0; j < needle.length; j++) {
        if (hay[i + j] != needle[j]) continue outer;
      }
      return i;
    }
    return -1;
  }

  /** Tiny JSON-object-of-strings parser, sufficient for artifactProps. */
  private static Map<String,String> parseSimpleJsonObject(String s) {
    Map<String,String> out = new LinkedHashMap<>();
    if (s == null) return out;
    int i = 0;
    int n = s.length();
    while (i < n && s.charAt(i) != '{') i++;
    if (i >= n) return out;
    i++;
    while (i < n) {
      while (i < n && Character.isWhitespace(s.charAt(i))) i++;
      if (i < n && s.charAt(i) == '}') break;
      if (i < n && s.charAt(i) == ',') { i++; continue; }
      if (i >= n || s.charAt(i) != '"') break;
      int ks = ++i;
      while (i < n && s.charAt(i) != '"') i++;
      String key = s.substring(ks, i);
      i++;
      while (i < n && (Character.isWhitespace(s.charAt(i)) || s.charAt(i) == ':')) i++;
      String val;
      if (i < n && s.charAt(i) == '"') {
        ++i;
        StringBuilder sb = new StringBuilder();
        while (i < n) {
          char c = s.charAt(i);
          if (c == '\\' && i + 1 < n) {
            char esc = s.charAt(i + 1);
            switch (esc) {
              case '"':  sb.append('"');  i += 2; break;
              case '\\': sb.append('\\'); i += 2; break;
              case '/':  sb.append('/');  i += 2; break;
              case 'n':  sb.append('\n'); i += 2; break;
              case 't':  sb.append('\t'); i += 2; break;
              case 'r':  sb.append('\r'); i += 2; break;
              case 'b':  sb.append('\b'); i += 2; break;
              case 'f':  sb.append('\f'); i += 2; break;
              case 'u':
                if (i + 6 <= n) {
                  sb.append((char) Integer.parseInt(s.substring(i + 2, i + 6), 16));
                  i += 6;
                } else {
                  i = n;
                }
                break;
              default:   sb.append(esc); i += 2; break;
            }
            continue;
          }
          if (c == '"') break;
          sb.append(c); i++;
        }
        val = sb.toString();
        i++;
      } else {
        int vs = i;
        while (i < n && ",}".indexOf(s.charAt(i)) < 0) i++;
        val = s.substring(vs, i).trim();
      }
      out.put(key, val);
    }
    return out;
  }

  private static int parseIntOr(String s, int dflt) {
    if (s == null) return dflt;
    try { return Integer.parseInt(s.trim()); } catch (Exception e) { return dflt; }
  }

  private static Map<String,String> parseAuConfigBody(byte[] body) {
    // The body is JSON of shape {"auId":"...","auConfig":{...}}; we stash
    // the whole thing into a single key for inspection.
    Map<String,String> out = new LinkedHashMap<>();
    String s = new String(body, StandardCharsets.UTF_8);
    out.put("__rawJson", s);
    return out;
  }
}
