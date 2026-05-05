/*
 * Copyright (c) 2026, Board of Trustees of Leland Stanford Jr. University.
 * All rights reserved.
 */
package org.lockss.laaws.mock;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.lockss.laaws.MigrationManager;
import org.lockss.laaws.V2AuMover;
import org.lockss.test.ConfigurationUtil;

/** JUnit-friendly fixture: spawns two JDK HttpServer instances (cfg + rs)
 * on ephemeral ports, exposes a {@link MockV2Backend} for state pre-population
 * and request inspection, and a {@link FailureRules} DSL for failure
 * injection. */
public class MockV2Lockss {

  /** Default thread pool size for the embedded servers.  Sized so the
   * 100k-AU stress test won't deadlock. */
  public static final int DEFAULT_HANDLER_THREADS = 64;

  private final MockV2Backend backend = new MockV2Backend();
  private final FailureRules failures = new FailureRules();
  private final V2RequestHandler routes = new V2RequestHandler(backend, failures);

  private HttpServer cfgServer;
  private HttpServer rsServer;
  private ExecutorService cfgExecutor;
  private ExecutorService rsExecutor;
  private int handlerThreads = DEFAULT_HANDLER_THREADS;
  private boolean started = false;

  public MockV2Lockss() {}

  public void setHandlerThreads(int n) {
    if (started) throw new IllegalStateException("already started");
    this.handlerThreads = n;
  }

  public synchronized void start() throws IOException {
    if (started) return;
    cfgExecutor = Executors.newFixedThreadPool(handlerThreads,
        namedFactory("MockV2-cfg"));
    rsExecutor = Executors.newFixedThreadPool(handlerThreads,
        namedFactory("MockV2-rs"));
    cfgServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    cfgServer.createContext("/", routes.cfgHandler());
    cfgServer.setExecutor(cfgExecutor);
    cfgServer.start();

    rsServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    rsServer.createContext("/", routes.rsHandler());
    rsServer.setExecutor(rsExecutor);
    rsServer.start();

    started = true;
  }

  public synchronized void stop() {
    if (!started) return;
    if (cfgServer != null) cfgServer.stop(0);
    if (rsServer != null) rsServer.stop(0);
    if (cfgExecutor != null) cfgExecutor.shutdownNow();
    if (rsExecutor != null) rsExecutor.shutdownNow();
    started = false;
  }

  public String getHostname() { return "127.0.0.1"; }
  public int getCfgPort() { return cfgServer.getAddress().getPort(); }
  public int getRsPort()  { return rsServer.getAddress().getPort(); }
  public String getUsername() { return backend.getUsername(); }
  public String getPassword() { return backend.getPassword(); }

  public MockV2Backend backend()  { return backend; }
  public FailureRules failures() { return failures; }

  /** Set the {@code v2.migrate.*} config params so V2AuMover points at this
   * harness.  Must be called after {@link #start()}. */
  public void applyMigrationConfig() {
    if (!started) throw new IllegalStateException("call start() first");
    ConfigurationUtil.addFromArgs(
        V2AuMover.PARAM_RS_PORT, Integer.toString(getRsPort()),
        V2AuMover.PARAM_CFG_PORT, Integer.toString(getCfgPort()),
        V2AuMover.PARAM_CFG_UI_PORT, Integer.toString(getCfgPort()));
    ConfigurationUtil.addFromArgs(
        MigrationManager.PARAM_DRY_RUN_ENABLED, "true");
  }

  private static java.util.concurrent.ThreadFactory namedFactory(final String prefix) {
    return new java.util.concurrent.ThreadFactory() {
      private final java.util.concurrent.atomic.AtomicInteger n =
        new java.util.concurrent.atomic.AtomicInteger();
      @Override public Thread newThread(Runnable r) {
        Thread t = new Thread(r, prefix + "-" + n.incrementAndGet());
        t.setDaemon(true);
        return t;
      }
    };
  }
}
