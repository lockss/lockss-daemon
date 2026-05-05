/*
 * Copyright (c) 2026, Board of Trustees of Leland Stanford Jr. University.
 * All rights reserved.
 */
package org.lockss.laaws.mock;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Failure-injection DSL.  Tests register rules; the request handler
 * consults the first matching rule before generating a normal response. */
public class FailureRules {

  private final List<Rule> rules = new CopyOnWriteArrayList<>();

  public Rule when(String method, String pathPattern) {
    Rule r = new Rule(method, pathPattern);
    rules.add(r);
    return r;
  }

  public void clear() { rules.clear(); }

  /** Internal: find the first non-expired rule that matches the request. */
  Rule findMatch(String method, String path) {
    for (Rule r : rules) {
      if (r.matches(method, path) && r.consume()) return r;
    }
    return null;
  }

  public class Rule {
    private final String method;
    private final java.util.regex.Pattern pathRe;
    private volatile int maxApplications = Integer.MAX_VALUE;
    private final AtomicInteger applied = new AtomicInteger(0);
    private final AtomicInteger seen = new AtomicInteger(0);
    private volatile int afterCallN = 0;

    private volatile Integer status;
    private volatile String body;
    private volatile long delayMs = 0;
    private volatile boolean closeConnection = false;
    private volatile boolean malformedJson = false;
    private volatile int truncateAt = -1;

    Rule(String method, String pathPattern) {
      this.method = method;
      this.pathRe = MockV2Backend.globToRegex(pathPattern);
    }

    boolean matches(String m, String p) {
      return method.equalsIgnoreCase(m) && pathRe.matcher(p).matches();
    }

    /** Returns true if the rule should fire for this request, and updates
     * counters.  After {@code timesN()} firings the rule expires. */
    boolean consume() {
      int s = seen.incrementAndGet();
      if (s <= afterCallN) return false;
      int a = applied.incrementAndGet();
      if (a > maxApplications) return false;
      return true;
    }

    public Rule timesN(int n) { this.maxApplications = n; return this; }
    public Rule afterCallN(int n) { this.afterCallN = n; return this; }
    public Rule respondWithStatus(int s) { this.status = s; return this; }
    public Rule respondWithBody(String b) { this.body = b; return this; }
    public Rule delayMs(long ms) { this.delayMs = ms; return this; }
    public Rule closeConnection() { this.closeConnection = true; return this; }
    public Rule malformedJson() { this.malformedJson = true; return this; }
    public Rule truncateResponseAt(int n) { this.truncateAt = n; return this; }

    Integer getStatus() { return status; }
    String getBody() { return body; }
    long getDelayMs() { return delayMs; }
    boolean isCloseConnection() { return closeConnection; }
    boolean isMalformedJson() { return malformedJson; }
    int getTruncateAt() { return truncateAt; }
  }
}
