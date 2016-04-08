/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.filter.html;

import org.htmlparser.tags.*;

/**
 * Collection of additional simple HtmlParser tags.
 * @see HtmlFilterInputStream#makeParser()
 * @see HtmlFilterInputStream#registerTag(org.htmlparser.Tag)
 */
public class HtmlTags {

  /**
   * The ARTICLE tag (HTML5).
   * @since 1.69 
   */
  public static class Article extends CompositeTag {
    private static final String[] mIds = new String[] {"ARTICLE"};
    public String[] getIds() { return mIds; }
  }

  /**
   * The ASIDE tag (HTML5).
   * @since 1.64
   */
  public static class Aside extends CompositeTag {
    private static final String[] mIds = new String[] {"aside"};
    public String[] getIds() { return mIds; }
  }
  
  /**
   * The AUDIO tag (HTML5).
   * @since 1.70
   */
  public static class Audio extends CompositeTag {
    private static final String[] mIds = new String[] {"audio"};
    public String[] getIds() { return mIds; }
  }
  
  /**
   * The BUTTON tag.
   * @since 1.70 
   */
  public static class Button extends CompositeTag {
    private static final String[] mIds = new String[] {"button"};
    public String[] getIds() { return mIds; }
  }

  /**
   * The CANVAS tag (HTML5).
   * @since 1.70
   */
  public static class Canvas extends CompositeTag {
    private static final String[] mIds = new String[] {"canvas"};
    public String[] getIds() { return mIds; }
  }
  
  /**
   * The CENTER tag.
   * @since 1.67.4 
   */
  public static class Center extends CompositeTag {
    private static final String[] mIds = new String[] {"CENTER"};
    public String[] getIds() { return mIds; }
  }

  /**
   * The DATALIST tag (HTML5).
   * @since 1.66
   */
  public static class DataList extends CompositeTag {
    private static final String[] mIds = new String[] {"datalist"};
    public String[] getIds() { return mIds; }
  }
  
  /**
   * The DETAILS tag (HTML5).
   * @since 1.66
   */
  public static class Details extends CompositeTag {
    private static final String[] mIds = new String[] {"details"};
    public String[] getIds() { return mIds; }
  }
  
  /**
   * The DIALOG tag (HTML5).
   * @since 1.66
   */
  public static class Dialog extends CompositeTag {
    private static final String[] mIds = new String[] {"dialog"};
    public String[] getIds() { return mIds; }
  }
  
  /**
   * The FIGCAPTION tag (HTML5).
   * @since 1.70
   */
  public static class FigCaption extends CompositeTag {
    private static final String[] mIds = new String[] {"figcaption"};
    public String[] getIds() { return mIds; }
  }
  
  /**
   * The FIGURE tag (HTML5).
   * @since 1.70
   */
  public static class Figure extends CompositeTag {
    private static final String[] mIds = new String[] {"figure"};
    public String[] getIds() { return mIds; }
  }
  
  /**
   * The FONT tag.
   */
  public static class Font extends CompositeTag {
    private static final String[] mIds = new String[] {"FONT"};
    public String[] getIds() { return mIds; }
  }

  /**
   * The FOOTER tag (HTML5).
   * @since 1.64
   */
  public static class Footer extends CompositeTag {
    private static final String[] mIds = new String[] {"footer"};
    public String[] getIds() { return mIds; }
  }
  
  /**
   * The HEADER tag (HTML5).
   * @since 1.64
   */
  public static class Header extends CompositeTag {
    private static final String[] mIds = new String[] {"header"};
    public String[] getIds() { return mIds; }
  }
  
  /**
   * The IFRAME tag.
   */
  public static class Iframe extends CompositeTag {
    private static final String[] mIds = new String[] {"IFRAME"};
    public String[] getIds() { return mIds; }
  }

  /**
   * The MAIN tag (HTML5).
   * @since 1.70
   */
  public static class Main extends CompositeTag {
    private static final String[] mIds = new String[] {"main"};
    public String[] getIds() { return mIds; }
  }
  
  /**
   * The MARK tag (HTML5).
   * @since 1.70
   */
  public static class Mark extends CompositeTag {
    private static final String[] mIds = new String[] {"mark"};
    public String[] getIds() { return mIds; }
  }
  
  /**
   * The MENU tag (HTML5).
   * @since 1.66
   */
  public static class Menu extends CompositeTag {
    private static final String[] mIds = new String[] {"menu"};
    public String[] getIds() { return mIds; }
  }
  
  /**
   * The MENUITEM tag (HTML5).
   * @since 1.66
   */
  public static class MenuItem extends CompositeTag {
    private static final String[] mIds = new String[] {"menuitem"};
    public String[] getIds() { return mIds; }
  }
  
  /**
   * The METER tag (HTML5).
   * @since 1.66
   */
  public static class Meter extends CompositeTag {
    private static final String[] mIds = new String[] {"meter"};
    public String[] getIds() { return mIds; }
  }
  
  /**
   * The NAV tag (HTML5).
   * @since 1.66
   */
  public static class Nav extends CompositeTag {
    private static final String[] mIds = new String[] {"nav"};
    public String[] getIds() { return mIds; }
  }
  
  /**
   * The NOSCRIPT tag.
   */
  public static class NoScript extends CompositeTag {
    private static final String[] mIds = new String[] {"NOSCRIPT"};
    public String[] getIds() { return mIds; }
  }

  /**
   * The PROGRESS tag (HTML5).
   * @since 1.66
   */
  public static class Progress extends CompositeTag {
    private static final String[] mIds = new String[] {"progress"};
    public String[] getIds() { return mIds; }
  }
  
  /**
   * The SECTION tag (HTML5).
   * @since 1.64
   */
  public static class Section extends CompositeTag {
    private static final String[] mIds = new String[] {"section"};
    public String[] getIds() { return mIds; }
  }
  
  /**
   * The SUMMARY tag (HTML5)
   * @since 1.66
   */
  public static class Summary extends CompositeTag {
    private static final String[] mIds = new String[] {"summary"};
    public String[] getIds() { return mIds; }
  }
  
  /** Overridden to add TR as an additional ender.  */
  public static class MyTableRow extends TableRow {
    /**
     * The set of tag names that indicate the end of this tag.
     */
    private static final String[] mEnders =
      new String[] {"TBODY", "TFOOT", "THEAD", "TR"};
    
    /**
     * The set of end tag names that indicate the end of this tag.
     */
    private static final String[] mEndTagEnders =
      new String[] {"TBODY", "TFOOT", "THEAD", "TABLE"};

    public MyTableRow () {
      super();
    }

    /**
     * Return the set of tag names that cause this tag to finish.
     * @return The names of following tags that stop further scanning.
     */
    public String[] getEnders () {
      return (mEnders);
    }

    /**
     * Return the set of end tag names that cause this tag to finish.
     * @return The names of following end tags that stop further scanning.
     */
    public String[] getEndTagEnders () {
      return (mEndTagEnders);
    }

  }

  /**
   * The TIME tag (HTML5).
   * @since 1.66
   */
  public static class Time extends CompositeTag {
    private static final String[] mIds = new String[] {"time"};
    public String[] getIds() { return mIds; }
  }
  
  /**
   * The VIDEO tag (HTML5).
   * @since 1.70
   */
  public static class Video extends CompositeTag {
    private static final String[] mIds = new String[] {"video"};
    public String[] getIds() { return mIds; }
  }
  
}
