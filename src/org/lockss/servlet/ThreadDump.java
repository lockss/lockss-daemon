/*
 * $Id: ThreadDump.java,v 1.8.10.1 2008-07-22 06:47:30 tlipkis Exp $
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.servlet;

import java.io.*;
import java.util.*;
import java.util.List;
import javax.servlet.*;
import java.lang.management.*;
import org.mortbay.html.*;

import org.lockss.util.*;

/** ThreadDump servlet
 */
public class ThreadDump extends LockssServlet {

  private static String
    KEY_IS_THREAD_CONTENTION_MONITORING_ENABLED = "Contention";
  private static String KEY_IS_THREAD_CPU_TIME_ENABLED = "Cpu_Time";
  private static String KEY_ACTION = "Action";
  private static String ACTION_CHANGE = "Change";

  private PlatformUtil platInfo;

  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    platInfo = PlatformUtil.getInstance();
  }

  /** Handle a request */
  public void lockssHandleRequest() throws IOException {
    platInfo.threadDump(true);
    dumpMXBean();
  }

  void dump1() throws IOException {
    PrintWriter wrtr = resp.getWriter();
    wrtr.println("JVM threads dumped to stderr");
    resp.setContentType("text/plain");
    try {
      Map<Thread, StackTraceElement[]> map = Thread.getAllStackTraces ();
      List<Thread> lst = new ArrayList<Thread> (map.keySet());
      Collections.sort(lst, new Comparator<Thread>() {
	public int compare(Thread t1, Thread t2) {
	  return t1.getName().compareTo(t2.getName());
	}
      });
      for (Thread t : lst) {
	//	wrtr.println(t.toString());
 	dumpThread(wrtr, t);
	for (StackTraceElement se : map.get(t)) {
	  wrtr.print("\t");
	  wrtr.println(se);
	}
	wrtr.println();
      }
    } catch (RuntimeException e) {
      log.error("Can't generate thread dump", e);
    }
  }

  // Display info about a thread.
  private static void dumpThread(PrintWriter wrtr, Thread t) {
    if (t == null) return;
    wrtr.println("Thread: " + t.getName() +
		 " Pri: " + t.getPriority() +
		 " State: " + t.getState() +
		 (t.isDaemon()?" Daemon":"") +
		 (t.isAlive()?"":" Not Alive"));
  }
  //

  void dumpMXBean() throws IOException {
    ThreadMXBean tmxb = ManagementFactory.getThreadMXBean();
    if ("Change".equals(getParameter(KEY_ACTION))) {
      doForm(tmxb);
    }
    Map<Thread, StackTraceElement[]> map = Thread.getAllStackTraces();
    Map<Long,Thread> idMap = new HashMap();
    for (Thread th : map.keySet()) {
      idMap.put(th.getId(), th);
    }

    Page page = newPage();
    page.add(makeHeader(tmxb));
    page.add(makeThreads(tmxb, idMap));
    layoutFooter(page);
    ServletUtil.writePage(resp, page);
  }

  void doForm(ThreadMXBean tmxb) {
    if (tmxb.isThreadCpuTimeSupported()) {
      String itcpte = getParameter(KEY_IS_THREAD_CPU_TIME_ENABLED);
      tmxb.setThreadCpuTimeEnabled(itcpte != null);
    }
    if (tmxb.isThreadContentionMonitoringSupported()) {
      String itcme = getParameter(KEY_IS_THREAD_CONTENTION_MONITORING_ENABLED);
      tmxb.setThreadContentionMonitoringEnabled(itcme != null);
    }
  }

  private Element makeHeader(ThreadMXBean tmxb) {
    Composite comp = new Composite();
    Form frm = new Form(srvURL(myServletDescr()));
    frm.method("POST");
    Table htbl = new Table(0);
    boolean isForm = false;
    htbl.newRow();
    htbl.addHeading("ThreadCpuTimeSupported", "align='right'");
    htbl.newCell();
    if (tmxb.isThreadCpuTimeSupported()) {
      isForm = true;
      htbl.add("true");
      htbl.newRow();
      htbl.addHeading("ThreadCpuTimeEnabled", "align='right'");
      htbl.newCell();
      htbl.add(checkBox(null, "true",
			KEY_IS_THREAD_CPU_TIME_ENABLED,
			tmxb.isThreadCpuTimeEnabled()));
    } else {
      htbl.add("false");
    }
    htbl.newRow();
    htbl.addHeading("ThreadContentionMonitoringSupported", "align='right'");
    htbl.newCell();
    if (tmxb.isThreadContentionMonitoringSupported()) {
      isForm = true;
      htbl.add("true");
      htbl.newRow();
      htbl.addHeading("ThreadContentionMonitoringEnabled", "align='right'");
      htbl.newCell();
      htbl.add(checkBox(null, "true",
			KEY_IS_THREAD_CONTENTION_MONITORING_ENABLED,
			tmxb.isThreadContentionMonitoringEnabled()));
    } else {
      htbl.add("false");
    }
    if (isForm) {
      htbl.newRow();
      htbl.newCell();
      htbl.newCell();
      Input submit = new Input(Input.Submit, KEY_ACTION, ACTION_CHANGE);
      setTabOrder(submit);
      htbl.add(submit);
    }
    frm.add(htbl);
    comp.add(frm);
    return comp;
  }

  private Element makeThreads(ThreadMXBean tmxb, Map<Long,Thread> idMap) {
    Composite comp = new Composite();
    boolean isThreadContentionMonitoringSupported =
      tmxb.isThreadContentionMonitoringSupported();
    boolean isThreadCpuTimeSupported =
      tmxb.isThreadCpuTimeSupported();
    boolean isThreadContentionMonitoringEnabled =
      isThreadContentionMonitoringSupported &&
      tmxb.isThreadContentionMonitoringEnabled();
    boolean isThreadCpuTimeEnabled =
      isThreadCpuTimeSupported &&
      tmxb.isThreadCpuTimeEnabled();

    Table tbl = new Table(1, "width=\"100%\"");
    tbl.newRow();
    tbl.addHeading("Name");
    tbl.addHeading("Id");
    tbl.addHeading("Prio");
    tbl.addHeading("Status");
    if (isThreadCpuTimeEnabled) {
      tbl.addHeading("Time (user)");
    }
    tbl.addHeading("Block cnt" +
		   (isThreadContentionMonitoringEnabled ? " (time)" : ""));
    tbl.addHeading("Wait cnt" +
		   (isThreadContentionMonitoringEnabled ? " (time)" : ""));

    long[] threadIds = tmxb.getAllThreadIds();
    long curId = Thread.currentThread().getId();
    List deadIds = new ArrayList<Long>();
    if (isThreadContentionMonitoringEnabled) {
      long[] deadlocked = tmxb.findMonitorDeadlockedThreads();
      if (deadlocked != null) {
	for (int ix = 0; ix < deadlocked.length; ix++) {
	  deadIds.add(new Long(deadlocked[ix]));
	}
      }
    }

    StringBuilder stackTxt = new StringBuilder(5000);
    for (int ix = 0; ix < threadIds.length; ix++) {
      long tId = threadIds[ix];
      ThreadInfo tInfo = tmxb.getThreadInfo(tId, 500);
      Thread th = idMap.get(tId);
      String tName;
      if (tInfo != null) {
	tName = tInfo.getThreadName();
      } else {
	tName = "??? (no thread info)";
      }
      tbl.newRow(deadIds.contains(new Long(ix))
		 ? " style='color: #ff0000'" : "");
      tbl.newCell();
      tbl.add(tName);
      tbl.newCell("align = \"right\"");
      tbl.add(Long.toString(tId));
      tbl.newCell("align = \"center\"");
      if (th != null) {
	tbl.add(th.getPriority());
      }
      if (tInfo == null) {
	continue;
      }
      tbl.newCell();
      String status =
// 	"Prio: " + t.getPriority() +
	tInfo.isSuspended() ? " Suspended " : "" + 
	tInfo.getThreadState() +
	(tId == curId ? " Current" : "") +
	(deadIds.contains(new Long(tId)) ? " Deadlocked" : "");
      String tLock = tInfo.getLockName();
      String tOwner = tInfo.getLockOwnerName();
      if (tLock != null || tOwner != null) {
	status += " (" + tLock + (tOwner != null ? ", " + tOwner : "") + ")";
      }
      tbl.add(status);
      if (isThreadCpuTimeEnabled) {
	tbl.newCell("align = \"right\"");
	tbl.add((tmxb.getThreadCpuTime(tId)/1.0e9) +
		" (" + (tmxb.getThreadUserTime(tId)/1.0e9) + ")");;
      }
      tbl.newCell("align = \"right\"");
      tbl.add(tInfo.getBlockedCount() +
	      (isThreadContentionMonitoringEnabled
	       ? " (" + tInfo.getBlockedTime()/1.0e9 + ")"
	       : ""));
      tbl.newCell("align = \"right\"");
      tbl.add(tInfo.getWaitedCount() +
	      (isThreadContentionMonitoringEnabled
	       ? " (" + tInfo.getWaitedTime()/1.0e9 + ")"
	       : ""));

      // Add thread's stack trace to string
      stackTxt.append("\n");
      String foo = tName + " " + status;
      stackTxt.append(foo);
      stackTxt.append("\n");
      StackTraceElement[] bt = tInfo.getStackTrace();

      for (int jx = 0; jx < bt.length; jx++) {
	StackTraceElement ele = bt[jx];
	stackTxt.append("\t");
	stackTxt.append(ele.toString());
	stackTxt.append("\n");
      }
    }
    comp.add(tbl);
    Block blk = new Block("pre");
    blk.add(stackTxt.toString());
    comp.add(blk);
    return comp;
  }
}
