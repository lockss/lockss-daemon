/*
 * $Id$
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.daemon;

import junit.framework.TestCase;
import java.io.*;
import java.util.*;
import java.text.*;
import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.remote.*;
import org.lockss.test.*;

/**
 * This is the test class for org.lockss.daemon.Cron
 */
public class TestCron extends LockssTestCase {
  MockLockssDaemon daemon;
  MyCron cron;

  public void setUp() throws Exception {
    super.setUp();
    daemon = getMockLockssDaemon();
  }

  void initCron() {
    initCron(null);
  }

  void initCron(Cron.Task task) {
    Properties p = new Properties();
    p.put(org.lockss.util.ObjectSerializer.PARAM_FAILED_DESERIALIZATION_MODE,
	  org.lockss.util.ObjectSerializer.FAILED_DESERIALIZATION_IGNORE+"");
    ConfigurationUtil.addFromProps(p);
    cron = new MyCron(task);
    cron.initService(daemon);
    daemon.setDaemonInited(true);
  }

  public void testLoadStateNoFile() {
    initCron();
    cron.loadState(new File("no/such/file"));
    assertEmpty(cron.getState().times);
  }

  public void testState() throws IOException {
    initCron();
    File file = FileTestUtil.tempFile("fff");
    cron.loadState(file);
    assertEmpty(cron.getState().times);
    cron.getState().setLastTime("foo", 1234);
    cron.getState().setLastTime("bar", 777);
    assertEquals(1234, cron.getState().getLastTime("foo"));
    assertEquals(777, cron.getState().getLastTime("bar"));
    cron.storeState(file);
    assertEquals(1234, cron.getState().getLastTime("foo"));
    assertEquals(777, cron.getState().getLastTime("bar"));
    cron.getState().setLastTime("foo", 1);
    cron.getState().setLastTime("bar", 2);
    assertEquals(1, cron.getState().getLastTime("foo"));
    assertEquals(2, cron.getState().getLastTime("bar"));
    cron.loadState(file);
    assertEquals(1234, cron.getState().getLastTime("foo"));
    assertEquals(777, cron.getState().getLastTime("bar"));
  }

  static DateFormat idf = new SimpleDateFormat("MM/dd/yyyy hh:mm");
  static DateFormat odf =
    DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL);
  static {
    idf.setTimeZone(Constants.DEFAULT_TIMEZONE);
    odf.setTimeZone(Constants.DEFAULT_TIMEZONE);
  }

  public void testCron() throws IOException {
    setUpDiskSpace();
    ConfigurationUtil.addFromArgs(Cron.PARAM_SLEEP, "10",
				  Cron.PARAM_ENABLED, "true");
    TimeBase.setSimulated(1000);
    TestTask task = new TestTask(daemon);
    initCron(task);
    
    SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
    cron.setExecSem(sem);
    cron.startService();
    TimeBase.step(10);
    if (!sem.take(TIMEOUT_SHOULDNT)) {
      fail("Task didn't finish");
    }
    assertEquals(ListUtil.list(new Long(1010)), task.getTrace());
    assertEquals(1010, cron.getState().getLastTime("TestTask"));
    TimeBase.step(10);
    assertEquals(ListUtil.list(new Long(1010)), task.getTrace());
    TimeBase.step(10);
    assertEquals(ListUtil.list(new Long(1010)), task.getTrace());
    TimeBase.step(70);
    assertEquals(ListUtil.list(new Long(1010)), task.getTrace());
    task.setRet(false);
    TimeBase.step(20);
    // task should have run again, but not updated next run time
    if (!sem.take(TIMEOUT_SHOULDNT)) {
      fail("Task didn't finish");
    }
    assertEquals(ListUtil.list(new Long(1010), new Long(1120)),
		 task.getTrace());
    assertEquals(1010, cron.getState().getLastTime("TestTask"));
    task.setRet(true);
    TimeBase.step(10);
    // now it should have run again and updated next run time
    if (!sem.take(TIMEOUT_SHOULDNT)) {
      fail("Task didn't finish");
    }
    assertEquals(1130, cron.getState().getLastTime("TestTask"));
    assertEquals(ListUtil.list(new Long(1010), new Long(1120), new Long(1130)),
		 task.getTrace());
  }

  // Ensure a no multiple instances of tasks running
  public void testLongRunning() throws IOException {
    setUpDiskSpace();
    ConfigurationUtil.addFromArgs(Cron.PARAM_SLEEP, "10",
				  Cron.PARAM_ENABLED, "true");
    TimeBase.setSimulated(1000);
    TestTask task = new TestTask(daemon);
    task.setWait(true);
    initCron(task);
    
    SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
    cron.setExecSem(sem);
    cron.startService();
    TimeBase.step(10);
    Long start1 = task.waitStart(TIMEOUT_SHOULDNT);
    assertNotNull("Task didn't start", start1);
    assertEquals(Long.valueOf(1010), start1);

    assertEquals(ListUtil.list(new Long(1010)), task.getTrace());
    // Task is waiting, shouldn't have updated its last run time
    assertEquals(0, cron.getState().getLastTime("TestTask"));
    TimeBase.step(10);
    assertEquals("Task should have run exactly once",
		 1, task.getTrace().size());
    assertEquals("Task should have run exactly once",
		 ListUtil.list(new Long(1010)), task.getTrace());
    TimeBase.step(10);
    assertEquals(ListUtil.list(new Long(1010)), task.getTrace());
    TimeBase.step(70);
    assertEquals(ListUtil.list(new Long(1010)), task.getTrace());
    TimeBase.step(20);
    // task should not run again while still running
    Long start2 = task.waitStart(TIMEOUT_SHOULD);
    assertNull("Task started at " + start2 + ", shouldn't have", start2);
    // and Cron.executeAndRescheduleTask() should not have run
    assertFalse("Cron.exec ran prematurely", sem.take(TIMEOUT_SHOULD));

    task.endWait();
    assertTrue("First task didn't finish", sem.take(TIMEOUT_SHOULDNT));
    assertEquals(1120, cron.getState().getLastTime("TestTask"));
    assertEquals(ListUtil.list(1010L), task.getTrace());
    TimeBase.step(100);
    // now it should have run again and updated next run time
    start2 = task.waitStart(TIMEOUT_SHOULDNT);
    assertNotNull("Task didn't start 2nd time", start2);
    task.endWait();
    assertTrue("Second task didn't finish", sem.take(TIMEOUT_SHOULDNT));
    assertEquals(1220, cron.getState().getLastTime("TestTask"));
    assertEquals(ListUtil.list(new Long(1010), new Long(1220)),
		 task.getTrace());
  }

  void assertIsDate(String date, long time) throws ParseException {
    assertIsDate(idf.parse(date), time);
  }

  void assertIsDate(Date date, long time) {
    if (date.getTime() != time) {
      fail("expected:<" + odf.format(date) + "> but was:<" +
	   odf.format(new Date(time)) + ">");
    }
  }

  long timeOf(String date) throws ParseException {
    return idf.parse(date).getTime();
  }

  public void testNextHour() throws Exception {
    assertEquals(1*Constants.DAY, cron.nextHour(23 * Constants.HOUR));
    assertEquals(23*Constants.HOUR,
		 cron.nextHour(22 * Constants.HOUR + 59 * Constants.MINUTE));
  }

  public void testNextDay() throws Exception {
    assertEquals(1*Constants.DAY, cron.nextDay(0));
    assertIsDate("1/2/1970 0:00", cron.nextDay(timeOf("1/1/1970 0:00")));
    // sunday
    assertIsDate("1/3/2005 0:00", cron.nextDay(timeOf("1/2/2005 1:00")));
    // monday
    assertIsDate("1/4/2005 0:00", cron.nextDay(timeOf("1/3/2005 1:00")));
    assertIsDate("1/5/2005 0:00", cron.nextDay(timeOf("1/4/2005 1:00")));
  }

  public void testNextWeek() throws Exception {
    // time 0 is midnight Jan 1, result s.b. Jan 5
    assertEquals(4*Constants.DAY, cron.nextWeek(0));
    assertIsDate("1/5/1970 0:00", cron.nextWeek(timeOf("1/1/1970 0:00")));
    // sunday
    assertIsDate("1/3/2005 0:00", cron.nextWeek(timeOf("1/2/2005 1:00")));
    // monday
    assertIsDate("1/10/2005 0:00", cron.nextWeek(timeOf("1/3/2005 1:00")));
    assertIsDate("1/10/2005 0:00", cron.nextWeek(timeOf("1/4/2005 1:00")));
  }

  public void testNextMonth() throws Exception {
    // time 0 is midnight Jan 1, result s.b. Feb 1
    assertIsDate("2/1/1970 0:00", cron.nextMonth(0));
    assertEquals(31*Constants.DAY, cron.nextMonth(0));
    assertIsDate("2/1/2005 0:00", cron.nextMonth(timeOf("1/1/2005 0:00")));
    assertIsDate("2/1/2005 0:00", cron.nextMonth(timeOf("1/3/2005 1:00")));
    assertIsDate("2/1/2005 0:00", cron.nextMonth(timeOf("1/31/2005 1:00")));
    assertIsDate("12/1/2007 0:00", cron.nextMonth(timeOf("11/30/2007 4:00")));
    assertIsDate("2/1/2008 0:00", cron.nextMonth(timeOf("1/7/2008 4:00")));
    assertIsDate("1/1/2008 0:00", cron.nextMonth(timeOf("12/7/2007 4:00")));
  }

  public void testCreateBackupFileNextMonth() throws Exception {
    ConfigurationUtil.setFromArgs(RemoteApi.PARAM_BACKUP_EMAIL_FREQ,
				  "monthly");
    RemoteApi.CreateBackupFile task = new RemoteApi.CreateBackupFile(daemon);
    // time 0 is midnight Jan 1, result s.b. Feb 1
    assertIsDate("2/1/1970 0:00", task.nextTime(0));
    assertEquals(31*Constants.DAY, task.nextTime(0));
    assertIsDate("2/1/2005 0:00", task.nextTime(timeOf("1/1/2005 0:00")));
    assertIsDate("2/1/2005 0:00", task.nextTime(timeOf("1/3/2005 1:00")));
    assertIsDate("2/1/2005 0:00", task.nextTime(timeOf("1/31/2005 1:00")));
    assertIsDate("12/1/2007 0:00", task.nextTime(timeOf("11/30/2007 4:00")));
    assertIsDate("2/1/2008 0:00", task.nextTime(timeOf("1/7/2008 4:00")));
    assertIsDate("1/1/2008 0:00", task.nextTime(timeOf("12/7/2007 4:00")));
  }

  public void testCreateBackupFileNextWeek() throws Exception {
    ConfigurationUtil.setFromArgs(RemoteApi.PARAM_BACKUP_EMAIL_FREQ,
				  "weekly");
    RemoteApi.CreateBackupFile task = new RemoteApi.CreateBackupFile(daemon);
    // time 0 is midnight Jan 1, result s.b. Jan 5
    assertEquals(4*Constants.DAY, task.nextTime(0));
    assertIsDate("1/5/1970 0:00", task.nextTime(timeOf("1/1/1970 0:00")));
    // sunday
    assertIsDate("1/3/2005 0:00", task.nextTime(timeOf("1/2/2005 1:00")));
    // monday
    assertIsDate("1/10/2005 0:00", task.nextTime(timeOf("1/3/2005 1:00")));
    assertIsDate("1/10/2005 0:00", task.nextTime(timeOf("1/4/2005 1:00")));
  }

  public void testCreateBackupFileMail() {
    MyRemoteApi rmt = new MyRemoteApi();
    daemon.setRemoteApi(rmt);
    RemoteApi.CreateBackupFile task = new RemoteApi.CreateBackupFile(daemon);
    assertFalse(rmt.sent);
    task.execute();
    assertTrue(rmt.sent);
  }

  static class MyCron extends Cron {
    Cron.Task task = null;
    SimpleBinarySemaphore execSem;

    MyCron() {
      super();
    }
    MyCron(Cron.Task task) {
      super();
      this.task = task;
    }

    void installTasks() {
      if (false) super.installTasks();
      if (task != null) {
	addTask(task);
      }
    }

    @Override
    protected void endExecuteHook(Task task) {
      super.endExecuteHook(task);
      if (execSem != null) {
	execSem.give();
      }
    }

    void setExecSem(SimpleBinarySemaphore sem) {
      execSem = sem;
    }
  }


  static class TestTask implements Cron.Task {
    List trace = new ArrayList();
    boolean ret = true;
    SimpleBinarySemaphore endSem;
    SimpleQueue startTimes = new SimpleQueue.Fifo();

    TestTask(LockssDaemon daemon) {
    }

    public String getId() {
      return "TestTask";
    }

    public long nextTime(long lastTime) {
      return lastTime + 100;
    }

    public boolean execute() {
      trace.add(new Long(TimeBase.nowMs()));
      startTimes.put(TimeBase.nowMs());
      if (endSem != null) {
	endSem.take();
      }
      return ret;
    }

    void setWait(boolean val) {
      if (val) {
	endSem = new SimpleBinarySemaphore();
      } else {
	endSem = null;
      }
    }

    void endWait() {
      endSem.give();
    }

    Long waitStart(long timeout) {
      return (Long)startTimes.get(timeout);
    }

    List getTrace() {
      return trace;
    }

    void setRet(boolean ret) {
      this.ret = ret;
    }
  }


  static class MyRemoteApi extends RemoteApi {
    boolean sent = false;

    @Override
    public void doPeriodicBackupFile() throws IOException {
      sent = true;
    }
  }

}
