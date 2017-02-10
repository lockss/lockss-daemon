/*
 * $Id$
 */

/*

Copyright (c) 2012 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

*/
package org.lockss.exporter.kbart;

import java.util.concurrent.*;

/**
 * A ThreadPoolExecutor with the following properties:
 * <ul>
 *   <li>Fixed size pool, but the size can be adjusted.</li>
 *   <li>The default pool size is one larger than the number of available processors.</li>
 *   <li>Prestarts all threads, for consistent performance including first usage.</li>
 * </ul>
 * <p>
 * As with Executors.newFixedThreadPool():
 * <br/><br/>
 * The thread pool reuses a fixed number of threads operating off a shared
 * unbounded queue. At any point, at most nThreads threads will be active
 * processing tasks. If additional tasks are submitted when all threads are
 * active, they will wait in the queue until a thread is available. If any
 * thread terminates due to a failure during execution prior to shutdown, a
 * new one will take its place if needed to execute subsequent tasks. The
 * threads in the pool will exist until it is explicitly shutdown.
 * </p>
 *
 * @author Neil Mayo
 */
public class AdjustableFixedSizeThreadPoolExecutor extends ThreadPoolExecutor {

  /** Default pool size is one greater than the number of processors. */
  private static final int DEFAULT_CORE_POOL_SIZE =
      Runtime.getRuntime().availableProcessors() + 1;
  /** Keep alive time is zero so excess idle threads are terminated as soon as
   * the pool size changes. */
  private static final int DEFAULT_KEEP_ALIVE_TIME = 0;
  private static final TimeUnit DEFAULT_KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;

  /** The current fixed pool size. */
  private volatile int fixedPoolSize = DEFAULT_CORE_POOL_SIZE;

  /**
   * Create a thread pool executor with a pool of the default size.
   */
  public AdjustableFixedSizeThreadPoolExecutor() {
    this(DEFAULT_CORE_POOL_SIZE);
  }

  /**
   * Create a thread pool executor with a pool of the specified size.
   * @param maxPoolSize
   */
  public AdjustableFixedSizeThreadPoolExecutor(int maxPoolSize) {
    super(
        maxPoolSize,
        maxPoolSize,
        DEFAULT_KEEP_ALIVE_TIME,
        DEFAULT_KEEP_ALIVE_TIME_UNIT,
        new LinkedBlockingQueue()
    );
    this.fixedPoolSize = maxPoolSize;
    this.prestartAllCoreThreads();
  }

  /**
   * Set a new fixed pool size. This will have no effect if the new size is the
   * same as the current size. If the new size is less than 1, the request is
   * ignored. If the pool size was changed, prestartAllCoreThreads() is called.
   * @param newPoolSize the new fixed pool size
   * @return whether the pool was resized
   */
  public synchronized boolean setFixedPoolSize(int newPoolSize) {
    if (newPoolSize==fixedPoolSize || newPoolSize<1) return false;
    fixedPoolSize = newPoolSize;
    // Set the core pool size first to avoid IllegalArgumentException on setMax
    // Set using the superclass method to avoid recursion
    super.setCorePoolSize(fixedPoolSize);
    super.setMaximumPoolSize(fixedPoolSize);
    prestartAllCoreThreads();
    return true;
  }

  /**
   * Get the current prescribed fixed pool size.
   * @return
   */
  public int getFixedPoolSize() {
    return fixedPoolSize;
  }

  // Override the set pool size methods so that clients cannot accidentally
  // upset the fixed pool size. Note that setFixedPoolSize must not call these
  // versions of the methods, only the superclass version.

  @Override
  public void setCorePoolSize(int i) {
    setFixedPoolSize(i);
  }

  @Override
  public void setMaximumPoolSize(int i) {
    setFixedPoolSize(i);
  }
}
