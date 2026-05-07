/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Maintains a binary semaphore (mutex) per key, lazily created and removed
 * when no longer in use.
 *
 * @param <T> The class type of the keys.
 */
public class SemaphoreMap<T> {
  private static final Logger log = Logger.getLogger("SemaphoreMap");

  /** Map from key to semaphore and its usage count. */
  private final Map<T, SemaphoreAndCount> locks = new HashMap<>();

  private static class SemaphoreAndCount {
    private final Semaphore sm;
    private int count;
    private Throwable context;

    SemaphoreAndCount() {
      this.sm = new Semaphore(1, true);
      this.count = 0;
    }

    Semaphore getSemaphore() {
      return sm;
    }

    void incrementCounter() {
      count++;
    }

    int decrementCounter() {
      return --count;
    }

    void setContext() {
      context = new Throwable();
    }

    void eraseContext() {
      context = null;
    }

    Throwable getContext() {
      return context;
    }
  }

  /**
   * Closeable lock token; close() releases the underlying semaphore.
   */
  public class SemaphoreLock implements Closeable {
    private final T key;

    public SemaphoreLock(T key) {
      this.key = key;
    }

    public T getKey() {
      return key;
    }

    @Override
    public void close() throws IOException {
      releaseLock(key);
    }
  }

  /** Called only from code synchronized on locks map */
  private SemaphoreAndCount getSemaphoreAndCount(T key) {
    synchronized (locks) {
      SemaphoreAndCount snc = locks.get(key);
      if (snc == null) {
        snc = new SemaphoreAndCount();
        locks.put(key, snc);
      }
      return snc;
    }
  }

  /**
   * Acquire the lock for the given key, blocking until available.
   */
  public SemaphoreLock getLock(T key) throws InterruptedException {
    SemaphoreAndCount snc;
    synchronized (locks) {
      snc = getSemaphoreAndCount(key);
      snc.incrementCounter();
    }
    try {
      while (!snc.getSemaphore().tryAcquire(1, TimeUnit.HOURS)) {
        if (snc.getContext() != null) {
          log.error("Lock not acquired in an hour: " + key, snc.getContext());
        } else {
          log.error("Lock not acquired in an hour and not held by anybody: "
                    + key);
        }
      }
      snc.setContext();
      log.debug3("Acquired lock: " + key);
    } catch (InterruptedException e) {
      decrementCounter(snc, key);
      log.warning("Interrupted in acquire()", e);
      throw e;
    } catch (Throwable e) {
      decrementCounter(snc, key);
      log.error("Unexpected throwable in acquire()", e);
      throw e;
    }
    return new SemaphoreLock(key);
  }

  /**
   * Like {@link #getLock}, but returns null immediately if the lock is held.
   */
  public SemaphoreLock tryGetLock(T key) {
    synchronized (locks) {
      SemaphoreAndCount snc = getSemaphoreAndCount(key);
      snc.incrementCounter();
      if (!snc.getSemaphore().tryAcquire()) {
        decrementCounter(snc, key);
        return null;
      }
      snc.setContext();
      log.debug3("Acquired lock (try): " + key);
      return new SemaphoreLock(key);
    }
  }

  /** Release the lock for the given key. */
  public void releaseLock(T key) {
    log.debug3("Releasing lock: " + key);
    synchronized (locks) {
      SemaphoreAndCount snc = locks.get(key);
      if (snc == null) {
        log.error("Attempt to release non-existent lock for " + key);
        throw new IllegalStateException("No existing semaphore for: " + key);
      }
      if (snc.count < 1) {
        log.warning("Releasing semaphore with usage counter < 1 [key: " + key
                    + "]", new Throwable());
        throw new IllegalStateException(
            "Releasing semaphore with usage counter < 1 for: " + key);
      }
      snc.getSemaphore().release();
      snc.eraseContext();
      decrementCounter(snc, key);
    }
  }

  void decrementCounter(SemaphoreAndCount snc, T key) {
    synchronized (locks) {
      if (snc.decrementCounter() < 1) {
        if (snc.getSemaphore().hasQueuedThreads()) {
          log.warning("Semaphore still has queued threads [key: " + key + "]");
        }
        locks.remove(key);
      }
    }
  }

  /** Estimate of the usage count (for tests). */
  public Integer getCount(T key) {
    synchronized (locks) {
      SemaphoreAndCount snc = locks.get(key);
      return (snc == null) ? null : snc.count;
    }
  }

  /** Number of semaphores in the map (for tests). */
  public int getSize() {
    synchronized (locks) {
      return locks.size();
    }
  }
}
