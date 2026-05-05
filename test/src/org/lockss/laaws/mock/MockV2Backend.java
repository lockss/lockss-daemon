/*
 * Copyright (c) 2026, Board of Trustees of Leland Stanford Jr. University.
 * All rights reserved.
 */
package org.lockss.laaws.mock;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Thread-safe in-memory state model for {@link MockV2Lockss}.  Holds AUs,
 * artifacts, config files, AU state/config/agreements, users, and an
 * append-only log of every recorded request. */
public class MockV2Backend {

  public static class RecordedArtifact {
    public final String uuid;
    public final String auid;
    public final String url;
    public final int version;
    public final long contentLength;
    public final String digest;
    public final long committedAt;
    public final byte[] content;
    public final String namespace;

    RecordedArtifact(String uuid, String auid, String url, int version,
                     byte[] content, String digest, String namespace,
                     long committedAt) {
      this.uuid = uuid;
      this.auid = auid;
      this.url = url;
      this.version = version;
      this.content = content;
      this.contentLength = content == null ? 0 : content.length;
      this.digest = digest;
      this.namespace = namespace;
      this.committedAt = committedAt;
    }
  }

  static class AuRecord {
    final String auid;
    final AtomicBoolean bulkMode = new AtomicBoolean(false);
    final List<String> artifactUuids = new CopyOnWriteArrayList<>();
    AuRecord(String auid) { this.auid = auid; }
  }

  private final ConcurrentMap<String,AuRecord> aus = new ConcurrentHashMap<>();
  private final ConcurrentMap<String,RecordedArtifact> artifacts =
    new ConcurrentHashMap<>();
  private final ConcurrentMap<String,byte[]> configFiles =
    new ConcurrentHashMap<>();
  private final ConcurrentMap<String,Map<String,String>> auConfigs =
    new ConcurrentHashMap<>();
  private final ConcurrentMap<String,String> auStates =
    new ConcurrentHashMap<>();
  private final ConcurrentMap<String,String> auAgreements =
    new ConcurrentHashMap<>();
  private final ConcurrentMap<String,String> auSuspectUrlVersions =
    new ConcurrentHashMap<>();
  private final ConcurrentMap<String,String> noAuPeers =
    new ConcurrentHashMap<>();
  private final ConcurrentMap<String,Object> users = new ConcurrentHashMap<>();
  private final List<RecordedRequest> requestLog =
    Collections.synchronizedList(new ArrayList<>());

  private volatile String username = "lockss-u";
  private volatile String password = "lockss-p";

  public MockV2Backend() {}

  // ---- Pre-population --------------------------------------------------

  public void registerAu(String auid) {
    aus.computeIfAbsent(auid, AuRecord::new);
  }

  public void registerArtifact(String auid, String url, int version,
                               byte[] content) {
    String uuid = UUID.randomUUID().toString();
    String digest = "SHA-256:" + Integer.toHexString(Arrays.hashCode(content));
    RecordedArtifact ra = new RecordedArtifact(uuid, auid, url, version,
                                               content, digest, "lockss",
                                               System.currentTimeMillis());
    artifacts.put(uuid, ra);
    AuRecord rec = aus.computeIfAbsent(auid, AuRecord::new);
    rec.artifactUuids.add(uuid);
  }

  // ---- Auth ------------------------------------------------------------

  public void setCredentials(String user, String pass) {
    this.username = user;
    this.password = pass;
  }

  public String getUsername() { return username; }
  public String getPassword() { return password; }

  // ---- Internal accessors used by the request handler ------------------

  AuRecord getAuRecord(String auid) { return aus.get(auid); }
  AuRecord getOrCreateAuRecord(String auid) {
    return aus.computeIfAbsent(auid, AuRecord::new);
  }
  Set<String> auIds() { return aus.keySet(); }
  public ConcurrentMap<String,RecordedArtifact> artifactMap() { return artifacts; }
  ConcurrentMap<String,byte[]> configFileMap() { return configFiles; }
  ConcurrentMap<String,Map<String,String>> auConfigMap() { return auConfigs; }
  ConcurrentMap<String,String> auStateMap() { return auStates; }
  ConcurrentMap<String,String> auAgreementsMap() { return auAgreements; }
  ConcurrentMap<String,String> auSuspectMap() { return auSuspectUrlVersions; }
  ConcurrentMap<String,String> noAuPeersMap() { return noAuPeers; }
  public ConcurrentMap<String,Object> userMap() { return users; }

  void recordRequest(RecordedRequest r) { requestLog.add(r); }

  void recordArtifact(RecordedArtifact ra) {
    artifacts.put(ra.uuid, ra);
    AuRecord rec = aus.computeIfAbsent(ra.auid, AuRecord::new);
    rec.artifactUuids.add(ra.uuid);
  }

  // ---- Inspection ------------------------------------------------------

  public boolean hasAu(String auid) { return aus.containsKey(auid); }

  public boolean isBulkMode(String auid) {
    AuRecord r = aus.get(auid);
    return r != null && r.bulkMode.get();
  }

  public List<RecordedArtifact> artifactsFor(String auid) {
    AuRecord r = aus.get(auid);
    if (r == null) return Collections.emptyList();
    List<RecordedArtifact> out = new ArrayList<>();
    for (String uuid : r.artifactUuids) {
      RecordedArtifact ra = artifacts.get(uuid);
      if (ra != null) out.add(ra);
    }
    return out;
  }

  public Map<String,String> getAuConfig(String auid) {
    Map<String,String> m = auConfigs.get(auid);
    return m == null ? null : Collections.unmodifiableMap(m);
  }

  public byte[] getConfigFile(String section) { return configFiles.get(section); }
  public String getAuState(String auid) { return auStates.get(auid); }

  public List<RecordedRequest> recordedRequests() {
    synchronized (requestLog) {
      return new ArrayList<>(requestLog);
    }
  }

  /** Count requests where method matches and path matches the glob pattern.
   * Pattern wildcards: '*' matches any chars except '/'. */
  public int requestCountMatching(String method, String pathPattern) {
    int n = 0;
    java.util.regex.Pattern re = globToRegex(pathPattern);
    synchronized (requestLog) {
      for (RecordedRequest r : requestLog) {
        if (r.method.equalsIgnoreCase(method) && re.matcher(r.path).matches()) {
          n++;
        }
      }
    }
    return n;
  }

  static java.util.regex.Pattern globToRegex(String glob) {
    StringBuilder sb = new StringBuilder("^");
    for (int i = 0; i < glob.length(); i++) {
      char c = glob.charAt(i);
      if (c == '*') sb.append("[^/]*");
      else if ("\\.+()|^$?{}[]".indexOf(c) >= 0) sb.append('\\').append(c);
      else sb.append(c);
    }
    sb.append('$');
    return java.util.regex.Pattern.compile(sb.toString());
  }
}
