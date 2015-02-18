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

package org.lockss.plugin;

import java.util.*;
import org.lockss.config.*;
import org.lockss.plugin.AuEvent;

/**
 * Handler for AU transition events such as created, deleted, reconfigured.
 * Classes that wish to be notified of a subset of these events will
 * probably want to extend {@link AuEventHandler.Base}, which provides a
 * null implementation, and define only the handlers they need.
 */
public interface AuEventHandler {
  /** Called after the AU is created (either by user action or at daemon
   * start time */
  void auCreated(AuEvent event, ArchivalUnit au);
  /** Called before the AU is deleted */
  void auDeleted(AuEvent event, ArchivalUnit au);
  /** Called after an existing AU's configuration is changed */
  void auReconfigured(AuEvent event, ArchivalUnit au, Configuration oldAuConf);
  /** Called after a change to the AU's content */
  void auContentChanged(AuEvent event, ArchivalUnit au, ChangeInfo info);

  /** Passed to {@link #auContentChanged(ArchivalUnit,ChangeInfo)} to
   * describe the nature of the content change. */
  public class ChangeInfo {
    /** Crawl means new content crawl, Repair means either peer repair or
     * repair crawl */
    public enum Type {
      Crawl, Repair;
    }

    private ArchivalUnit au;
    private Type type;
    private boolean complete;
    private Collection<String> urls;
    private Map<String,Integer> mimeCounts;
    private int numUrls;

    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("[AuChange: ");
      sb.append(type);
      sb.append(", ");
      sb.append(urls == null ? 0 : urls.size());
      sb.append(" urls, ");
      sb.append(mimeCounts != null ? mimeCounts.size() : 0);
      sb.append(" mime types]");
      return sb.toString();
    }      

    public void setType(Type type) {
      this.type = type;
    }

    /** Type of content changes: Crawl means new content crawl, Repair
     * means either peer repair or repair crawl */
    public Type getType() {
      return type;
    }

    public void setAu(ArchivalUnit au) {
      this.au = au;
    }

    /** The AU that changed */
    public ArchivalUnit getAu() {
      return au;
    }

    public void setComplete(boolean complete) {
      this.complete = complete;
    }

    /** True if this is expected to be a partial change (<i>e.g.</i> new
     * content crawl that ended in error and will be repeated/continued
     * soonish */
    public boolean isComplete() {
      return complete;
    }

    public void setUrls(Collection<String> urls) {
      this.urls = urls;
    }

    /** The list of URLs that change, if known and manageable. */
    public Collection<String> getUrls() {
      return urls;
    }

    /** True iff {@link #getUrls()} will return an accurate collection of
     * changed URLs */
    public boolean hasUrls() {
      return urls != null && numUrls == urls.size();
    }

    public void setNumUrls(int numUrls) {
      this.numUrls = numUrls;
    }

    /** The number of URLs that changed.  This is always available even if
     * {@link #hasUrls()} is false */
    public int getNumUrls() {
      return numUrls;
    }

    public void setMimeCounts(Map<String,Integer> mimeCounts) {
      this.mimeCounts = mimeCounts;
    }

    /** Returns a map from MIME type to the count of files of that MIME
     * type that were changed.  If null, the MIME counts aren't
     * available. */
    public Map<String,Integer> getMimeCounts() {
      return mimeCounts;
    }

  }

  /** Convenience class with null handlers for all AuEventHandler events.
   * Specialize this and override the events of interest */
  public class Base implements AuEventHandler {
    public void auCreated(AuEvent event, ArchivalUnit au) {}
    public void auDeleted(AuEvent event, ArchivalUnit au) {}
    public void auReconfigured(AuEvent event, ArchivalUnit au,
			       Configuration oldAuConf) {}
    public void auContentChanged(AuEvent event, ArchivalUnit au,
				 ChangeInfo info) {}
  }
}
