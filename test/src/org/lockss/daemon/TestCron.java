/*
 * $Id: TestCron.java,v 1.6 2007-07-31 07:55:11 tlipkis Exp $
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
    File dir = getTempDir();
    Properties p = new Properties();
    p.put(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, dir.toString());
    p.put(Cron.PARAM_SLEEP, "10");
    p.put(Cron.PARAM_ENABLED, "true");
    ConfigurationUtil.setCurrentConfigFromProps(p);
    TimeBase.setSimulated(1000);
    TestTask task = new TestTask(daemon);
    initCron(task);
    
    cron.startService();
    TimeBase.step(10);
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
    assertEquals(ListUtil.list(new Long(1010), new Long(1120)),
		 task.getTrace());
    assertEquals(1010, cron.getState().getLastTime("TestTask"));
    task.setRet(true);
    TimeBase.step(10);
    // now it should have run again and updated next run time
    assertEquals(1130, cron.getState().getLastTime("TestTask"));
    assertEquals(ListUtil.list(new Long(1010), new Long(1120), new Long(1130)),
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

  public void testMailBackupFileNextMonth() throws Exception {
    ConfigurationUtil.setFromArgs(RemoteApi.PARAM_BACKUP_EMAIL_FREQ,
				  "monthly");
    Cron.MailBackupFile task = new Cron.MailBackupFile(daemon);
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

  public void testMailBackupFileNextWeek() throws Exception {
    ConfigurationUtil.setFromArgs(RemoteApi.PARAM_BACKUP_EMAIL_FREQ,
				  "weekly");
    Cron.MailBackupFile task = new Cron.MailBackupFile(daemon);
    // time 0 is midnight Jan 1, result s.b. Feb 1
    assertEquals(4*Constants.DAY, task.nextTime(0));
    assertIsDate("1/5/1970 0:00", task.nextTime(timeOf("1/1/1970 0:00")));
    // sunday
    assertIsDate("1/3/2005 0:00", task.nextTime(timeOf("1/2/2005 1:00")));
    // monday
    assertIsDate("1/10/2005 0:00", task.nextTime(timeOf("1/3/2005 1:00")));
    assertIsDate("1/10/2005 0:00", task.nextTime(timeOf("1/4/2005 1:00")));
  }

  public void testMailBackupFileMail() {
    MyRemoteApi rmt = new MyRemoteApi();
    daemon.setRemoteApi(rmt);
    Cron.MailBackupFile task = new Cron.MailBackupFile(daemon);
    assertFalse(rmt.sent);
    task.execute();
    assertTrue(rmt.sent);
  }

  static class MyCron extends Cron {
    Cron.Task task = null;
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
  }

  static class TestTask implements Cron.Task {
    List trace = new ArrayList();
    boolean ret = true;

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
      return ret;
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

    public boolean sendMailBackup(boolean evenIfEmpty) throws IOException {
      sent = true;
      return true;
    }
  }

}
