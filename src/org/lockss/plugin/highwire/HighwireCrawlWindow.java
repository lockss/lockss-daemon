package org.lockss.plugin.highwire;

import java.util.*;

import org.lockss.daemon.*;
import org.lockss.plugin.definable.*;

public class HighwireCrawlWindow
    implements DefinableArchivalUnit.ConfigurableCrawlWindow {
  public HighwireCrawlWindow() {}

  public CrawlWindow makeCrawlWindow() {
    // disallow crawls from 6am->9am
    Calendar start = Calendar.getInstance();
    start.set(Calendar.HOUR_OF_DAY, 6);
    start.set(Calendar.MINUTE, 0);
    Calendar end = Calendar.getInstance();
    end.set(Calendar.HOUR_OF_DAY, 9);
    end.set(Calendar.MINUTE, 0);
    CrawlWindow interval = new CrawlWindows.Interval(start, end,
        CrawlWindows.TIME, null);
    // disallow using 'NOT'
    return new CrawlWindows.Not(interval);
  }

}
