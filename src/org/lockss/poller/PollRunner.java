/*
 * $Id: PollRunner.java,v 1.2 2006-06-26 23:55:07 smorabito Exp $
 */

/*

 Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.poller;

import org.lockss.daemon.*;
import org.lockss.util.Logger;

import EDU.oswego.cs.dl.util.concurrent.*;

/**
 * <p>
 * This class implements a scheme to allow polls to serialize their work, so
 * that multiple actions for any single poll are never happening concurrently in
 * separate threads.
 * </p>
 * 
 * <p>
 * The initial naive implementation uses a singled QueuedExecutor to queue up
 * tasks before running them.
 * </p>
 */
public class PollRunner {

  private Executor executor;

  private static final Logger log = Logger.getLogger("PollRunner");

  public PollRunner() {
    this.executor = new QueuedExecutor();
  }

  /**
   * Terminate background thread even if it is currently processing a task. This
   * method uses Thread.interrupt, so relies on tasks themselves responding
   * appropriately to interruption. If the current tasks does not terminate on
   * interruption, then the thread will not terminate until processing current
   * task. A shut down thread cannot be restarted.
   */
  public void stop() {
    log.debug("Terminating poll runner tasks.");
    ((QueuedExecutor)executor).shutdownNow();
    executor = null;
  }

  /**
   * Request a task be run. If the queue is empty, this task will be run right
   * away. Otherwise, it will be queued until tasks already in the queue are
   * completed.
   */
  public void runTask(PollRunner.Task task) {
    try {
      log.debug("Starting poll task " + task.toString());
      executor.execute(task);
      log.debug("Task " + task.toString() + " is now running.");
    } catch (InterruptedException ex) {
      // Don't want to ignore these yet, but they must be handled.
      // We'll log for now.
      log
          .warning("Caught InterruptedException while running task: " + task,
                   ex);
    }
  }

  /**
   * Interface for tasks that are runnable by the PollRunner.
   */
  public static abstract class Task extends LockssRunnable {
    private String pollId;

    public Task(String name, String pollId) {
      super(name);
      this.pollId = pollId;
    }
    
    /** Return the ID of the poll this task is associated with. */
    public String getPollId() {
      return pollId;
    }

    /** Perform the task's action. */
    public abstract void lockssRun();
  }
}
