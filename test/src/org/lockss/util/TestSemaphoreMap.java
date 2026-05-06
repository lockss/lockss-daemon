/*

Copyright (c) 2000-2026, Board of Trustees of Leland Stanford Jr. University

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

import java.io.*;
import org.lockss.test.LockssTestCase;
import org.lockss.util.SemaphoreMap.SemaphoreLock;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

// Coverage gaps not yet exercised:
//   1. getLock() interrupted while waiting on a held key — should throw
//      InterruptedException and restore count/size to the unwaited state
//      (validates decrementCounter in the InterruptedException catch).
//   2. Multiple concurrent waiters on getLock(): pre-acquire in test thread,
//      start N waiters, verify count=N+1, release, verify they each proceed
//      in turn and the entry is removed only after the final release.
//   3. releaseLock() with count<1 guard: acquire then releaseLock twice; the
//      second call should hit the count<1 branch and throw IllegalStateException.
//   4. Non-String key type (Integer or a custom POJO) to verify equals/hashCode
//      semantics work end-to-end.
// The 1-hour tryAcquire retry loop in getLock and the hasQueuedThreads-with-
// count<1 defensive log in decrementCounter are not worth chasing — they
// require either real-time clock manipulation or bypassing encapsulation.
public class TestSemaphoreMap extends LockssTestCase {
  private final static String TEST_KEY = "test";
  private final static int MAX_THREADS = 10;

  public void testSemaphoreMap() throws Exception {
    SemaphoreMap<String> locks = new SemaphoreMap<>();

    // Assert usage count is null if key is not in map
    assertNull(locks.getCount(TEST_KEY));

    // Releasing a non-existent lock throws IllegalStateException and leaves
    // the map empty
    try {
      locks.releaseLock(TEST_KEY);
      fail("releaseLock on absent key should throw IllegalStateException");
    } catch (IllegalStateException e) {
    }
    assertNull(locks.getCount(TEST_KEY));

    // Acquire lock and assert it is incremented to one
    locks.getLock(TEST_KEY);
    assertEquals(Integer.valueOf(1), locks.getCount(TEST_KEY));

    // Release lock and assert it is decremented to zero
    locks.releaseLock(TEST_KEY);
    assertNull(locks.getCount(TEST_KEY));
  }

  public void testTryGetLock() throws Exception {
    SemaphoreMap<String> locks = new SemaphoreMap<>();

    // Initially absent
    assertNull(locks.getCount(TEST_KEY));
    assertEquals(0, locks.getSize());

    // tryGetLock on a free key acquires immediately
    SemaphoreLock lock = locks.tryGetLock(TEST_KEY);
    assertNotNull(lock);
    assertEquals(TEST_KEY, lock.getKey());
    assertEquals(Integer.valueOf(1), locks.getCount(TEST_KEY));
    assertEquals(1, locks.getSize());

    // While held, a second tryGetLock returns null and does not change state
    assertNull(locks.tryGetLock(TEST_KEY));
    assertEquals(Integer.valueOf(1), locks.getCount(TEST_KEY));
    assertEquals(1, locks.getSize());

    // Closing the lock releases it and removes the entry
    lock.close();
    assertNull(locks.getCount(TEST_KEY));
    assertEquals(0, locks.getSize());

    // After release, tryGetLock can acquire again
    SemaphoreLock reacquired = locks.tryGetLock(TEST_KEY);
    assertNotNull(reacquired);
    assertEquals(Integer.valueOf(1), locks.getCount(TEST_KEY));
    reacquired.close();
    assertNull(locks.getCount(TEST_KEY));
    assertEquals(0, locks.getSize());
  }

  public void testThreadAcquireRelease() throws Exception {
    SemaphoreMap<String> locks = new SemaphoreMap<>();

    // Start threads
    for (int i = 0; i < MAX_THREADS; i++) {
      Thread t = new Thread(new SemaphoreMapTestRunnable(locks));
      t.start();
    }

    // Assert only one thread acquires the semaphore at a time
    for (int i = 0; i < 3*MAX_THREADS; i++) {
      assertEquals(0, queue.take());
    }

    // Assert that the semaphores were deleted from the map
    assertEquals(0, locks.getSize());
  }

  private int counter = 0;
  private BlockingQueue queue = new ArrayBlockingQueue(4*MAX_THREADS);

  private class SemaphoreMapTestRunnable implements Runnable {
    private SemaphoreMap locks;

    public SemaphoreMapTestRunnable(SemaphoreMap locks) {
      this.locks = locks;
    }

    @Override
    public void run() {
      try {
        try (SemaphoreLock lock = locks.getLock(TEST_KEY)) {
          try {
            queue.offer(counter++);
            Thread.sleep(50);
          } finally {
            queue.offer(--counter);
          }
        } catch (IOException e ) {
          throw new RuntimeException(e);
        }
        queue.offer(0); // good enough for test code
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

}
