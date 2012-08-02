/*
 *
 *
 * Copyright (c) 2001-2012 Board of Trustees of Leland Stanford Jr. University,
 * all rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 * STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Except as contained in this notice, the name of Stanford University shall not
 * be used in advertising or otherwise to promote the sale,
 * use or other dealings
 * in this Software without prior written authorization from Stanford
 * University.
 *
 * /
 */

package org.lockss.plugin;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.lockss.extractor.LinkExtractor;
import org.lockss.extractor.LinkExtractor.Callback;
import org.lockss.util.Logger;

/**
 * Class used to collect statistics about the Link Extractor
 * The code for timing and cpu sys and user time is from:
 * http://nadeausoftware.com/articles/2008/03/java_tip_how_get_cpu_and_user_time_benchmarking
 * #TimingasinglethreadedtaskusingCPUsystemandusertime
 */
public class LinkExtractorStatisticsManager {
  private HashMap<String, LinkExtractorStatsCallback>
      m_callbacks = new HashMap<String, LinkExtractorStatsCallback>();
  private HashMap<String, Long> m_durations = new HashMap<String, Long>();
  private String m_activeMeasurement = null;
  private long m_startTimeMillis;

  //disabled by default because stats were almost all 0 (or 1ms)
  private boolean m_collectCpuStats = false;
  private HashMap<String, Long> m_userCpus = new HashMap<String, Long>();
  private long m_startUserCpuNs;
  private HashMap<String, Long> m_mSysCpus = new HashMap<String, Long>();
  private long m_startSysCpuNs;


  /** Get user time in nanoseconds. */
  public long getUserTime() {
    ThreadMXBean bean = ManagementFactory.getThreadMXBean();
    return bean.isCurrentThreadCpuTimeSupported() ?
        bean.getCurrentThreadUserTime() : 0L;
  }

  /** Get system time in nanoseconds. */
  public long getSystemTime() {
    ThreadMXBean bean = ManagementFactory.getThreadMXBean();
    return bean.isCurrentThreadCpuTimeSupported() ?
        (bean.getCurrentThreadCpuTime() - bean.getCurrentThreadUserTime()) : 0L;
  }

  public void stopMeasurement() {
    long duration = System.currentTimeMillis() - m_startTimeMillis;
    m_durations.put(m_activeMeasurement, duration);
    if (m_collectCpuStats) {
      long user_cpu_ns = getUserTime() - m_startUserCpuNs;
      m_userCpus.put(m_activeMeasurement, user_cpu_ns);
      long sys_cpu_ns = getSystemTime() - m_startSysCpuNs;
      m_mSysCpus.put(m_activeMeasurement, sys_cpu_ns);
    }
  }

  public void startMeasurement(String name) {
    if (m_activeMeasurement != null) {
      stopMeasurement();
    }
    m_startTimeMillis = System.currentTimeMillis();
    m_activeMeasurement = name;
    if (m_collectCpuStats) {
      m_startUserCpuNs = getUserTime();
      m_startSysCpuNs = getSystemTime();
    }
  }

  //Compares a base extractor to  a alternate extractor
  public void compareExtractors(String base, String alt, String name) {
    if (!m_callbacks.containsKey(base) || !m_callbacks.containsKey(alt)) {
      throw new IllegalArgumentException("invalid base or alternate name");
    }
    System.out.println("Comparing " + base + " and " + alt + " " +
        "for " + name);
    Logger logger = Logger.getLoggerWithInitialLevel("LinkExtractorStats",
        Logger.LEVEL_DEBUG3);
    Set<String> base_urls = m_callbacks.get(base).GetUrls();
    Set<String> alt_urls = m_callbacks.get(alt).GetUrls();
    Set<String> common_urls = new HashSet<String>(alt_urls);
    common_urls.retainAll(base_urls);

    int common_url_count = common_urls.size();
    int base_url_count = base_urls.size() - common_url_count;
    int alt_url_count = alt_urls.size() - common_url_count;
    logger.debug2("Stats for " + name + " Common URLs: " + common_url_count +
        " " + base + " only: " + base_url_count + " " + alt + " only: " +
        alt_url_count);

    if (logger.isDebug3()) {
      if (base_url_count > 0) {
        Set<String> base_only_urls = new HashSet<String>(base_urls);
        base_only_urls.removeAll(common_urls);
        logger.debug3(base + " only urls: " + base_only_urls.toString());

      }
      if (alt_url_count > 0) {
        Set<String> alt_only_urls = new HashSet<String>(alt_urls);
        alt_only_urls.removeAll(common_urls);
        logger.debug3("Alt only urls: " + alt_only_urls.toString());
      }
    }
    if (m_durations.containsKey(base) && m_durations.containsKey(alt)) {
      System.out.println("Durations - " + base + "=" + m_durations.get(base)
          + ", " + alt + "=" + m_durations.get(alt));
      logger.debug2("Durations - " + base + "=" + m_durations.get(base) + ", " +
          "" + alt + "=" + m_durations.get(alt));
    }

    if (m_collectCpuStats) {
      if (m_userCpus.containsKey(base) && m_userCpus.containsKey(alt)) {
        System.out.println("User cpu(ns) - " + base + "=" + m_userCpus.get
            (base) + ", " + alt + "=" + m_userCpus.get(alt));
        logger.debug2("User cpu(ns) - " + base + "=" + m_userCpus.get(base) +
            ", " + alt + "=" + m_userCpus.get(alt));
      }
      if (m_mSysCpus.containsKey(base) && m_mSysCpus.containsKey(alt)) {
        System.out.println("System cpu(ns) - " + base + "=" + m_mSysCpus.get
            (base) + ", " + alt + "=" + m_mSysCpus.get(alt));
        logger.debug2("System cpu(ns) - " + base + "=" + m_mSysCpus.get(base)
            + ", " + alt + "=" + m_mSysCpus.get(alt));
      }
    }
  }

  public Callback wrapCallback(Callback cb, String name) {
    if (!m_callbacks.containsKey(name)) {
      m_callbacks.put(name, new LinkExtractorStatsCallback(cb));
    }
    return m_callbacks.get(name);
  }

  private class LinkExtractorStatsCallback implements LinkExtractor.Callback {
    private Set<String> m_urlsFound = new HashSet<String>();
    private Callback m_cb;

    public LinkExtractorStatsCallback(Callback cb) {
      m_cb = cb;
    }

    public Set<String> GetUrls() {
      return m_urlsFound;
    }

    //If we think the callee is actually doing real work in foundLink,
    // it would make sense to stop timing while it is called.
    public void foundLink(String url) {
      m_urlsFound.add(url);
      m_cb.foundLink(url);
    }
  }
}
