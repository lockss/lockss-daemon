/*

 Copyright (c) 2026 Board of Trustees of Leland Stanford Jr. University,
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
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 Except as contained in this notice, the name of Stanford University shall not
 be used in advertising or otherwise to promote the sale, use or other dealings
 in this Software without prior written authorization from Stanford University.

 */
package org.lockss.db;

import java.sql.Connection;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A DbManager for tests that counts how many times the metadata write lock is
 * taken, so a test can assert that a code path serializes itself against the
 * other metadata writers.
 */
public class LockCountingDbManager extends DbManager {
  private final AtomicInteger lockCount = new AtomicInteger();

  /**
   * Constructor skipping the asynchronous updates, as the test database
   * manager does.
   */
  public LockCountingDbManager() {
    super(true);
  }

  @Override
  public void lockMetadataWrite(Connection conn) throws DbException {
    lockCount.incrementAndGet();
    super.lockMetadataWrite(conn);
  }

  /**
   * Provides the number of times the metadata write lock has been taken.
   *
   * @return an int with the count.
   */
  public int getMetadataWriteLockCount() {
    return lockCount.get();
  }

  /**
   * Resets the count.
   */
  public void resetMetadataWriteLockCount() {
    lockCount.set(0);
  }
}
