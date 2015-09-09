/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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
 */
public class HtmlTags {

  /**
   * A header tag.  Can be registered with registerTag() to cause header
   * to be a CompositeTag.  See code samples in org.htmlparser.tags.
   * @since 1.64
   */
  public static class Header extends CompositeTag {
    
    /**
     * The set of names handled by this tag.
     */
    private static final String[] mIds = new String[] {"header"};
    
    /**
     * Return the set of names handled by this tag.
     * @return The names to be matched that create tags of this type.
     */
    public String[] getIds() {
      return mIds;
    }
    
  }
  
  /**
   * A footer tag.  Can be registered with registerTag() to cause footer
   * to be a CompositeTag.  See code samples in org.htmlparser.tags.
   * @since 1.64
   */
  public static class Footer extends CompositeTag {
    
    /**
     * The set of names handled by this tag.
     */
    private static final String[] mIds = new String[] {"footer"};
    
    /**
     * Return the set of names handled by this tag.
     * @return The names to be matched that create tags of this type.
     */
    public String[] getIds() {
      return mIds;
    }
    
  }
  
  /**
   * A section tag.  Can be registered with registerTag() to cause section
   * to be a CompositeTag.  See code samples in org.htmlparser.tags.
   * @since 1.64
   */
  public static class Section extends CompositeTag {
    
    /**
     * The set of names handled by this tag.
     */
    private static final String[] mIds = new String[] {"section"};
    
    /**
     * Return the set of names handled by this tag.
     * @return The names to be matched that create tags of this type.
     */
    public String[] getIds() {
      return mIds;
    }
    
  }
  
  /**
   * An aside tag.  Can be registered with registerTag() to cause section
   * to be a CompositeTag.  See code samples in org.htmlparser.tags.
   * @since 1.64
   */
  public static class Aside extends CompositeTag {
    
    /**
     * The set of names handled by this tag.
     */
    private static final String[] mIds = new String[] {"aside"};
    
    /**
     * Return the set of names handled by this tag.
     * @return The names to be matched that create tags of this type.
     */
    public String[] getIds() {
      return mIds;
    }
    
  }
  
  /**
   * A datalist tag.  Can be registered with registerTag() to cause datalist
   * to be a CompositeTag.
   * @since 1.66
   */
  public static class Datalist extends CompositeTag {
    
    /**
     * The set of names handled by this tag.
     */
    private static final String[] mIds = new String[] {"datalist"};
    
    /**
     * Return the set of names handled by this tag.
     * @return The names to be matched that create tags of this type.
     */
    public String[] getIds() {
      return mIds;
    }
    
  }
  
  /**
   * A details tag.  Can be registered with registerTag() to cause details
   * to be a CompositeTag.
   * @since 1.66
   */
  public static class Details extends CompositeTag {
    
    /**
     * The set of names handled by this tag.
     */
    private static final String[] mIds = new String[] {"details"};
    
    /**
     * Return the set of names handled by this tag.
     * @return The names to be matched that create tags of this type.
     */
    public String[] getIds() {
      return mIds;
    }
    
  }
  
  /**
   * A dialog tag.  Can be registered with registerTag() to cause dialog
   * to be a CompositeTag.
   * @since 1.66
   */
  public static class Dialog extends CompositeTag {
    
    /**
     * The set of names handled by this tag.
     */
    private static final String[] mIds = new String[] {"dialog"};
    
    /**
     * Return the set of names handled by this tag.
     * @return The names to be matched that create tags of this type.
     */
    public String[] getIds() {
      return mIds;
    }
    
  }
  
  /**
   * A menu tag.  Can be registered with registerTag() to cause menu
   * to be a CompositeTag.
   * @since 1.66
   */
  public static class Menu extends CompositeTag {
    
    /**
     * The set of names handled by this tag.
     */
    private static final String[] mIds = new String[] {"menu"};
    
    /**
     * Return the set of names handled by this tag.
     * @return The names to be matched that create tags of this type.
     */
    public String[] getIds() {
      return mIds;
    }
    
  }
  
  /**
   * A menuitem tag.  Can be registered with registerTag() to cause menuitem
   * to be a CompositeTag.
   * @since 1.66
   */
  public static class Menuitem extends CompositeTag {
    
    /**
     * The set of names handled by this tag.
     */
    private static final String[] mIds = new String[] {"menuitem"};
    
    /**
     * Return the set of names handled by this tag.
     * @return The names to be matched that create tags of this type.
     */
    public String[] getIds() {
      return mIds;
    }
    
  }
  
  /**
   * A meter tag.  Can be registered with registerTag() to cause meter
   * to be a CompositeTag.
   * @since 1.66
   */
  public static class Meter extends CompositeTag {
    
    /**
     * The set of names handled by this tag.
     */
    private static final String[] mIds = new String[] {"meter"};
    
    /**
     * Return the set of names handled by this tag.
     * @return The names to be matched that create tags of this type.
     */
    public String[] getIds() {
      return mIds;
    }
    
  }
  
  /**
   * A nav tag.  Can be registered with registerTag() to cause nav
   * to be a CompositeTag.
   * @since 1.66
   */
  public static class Nav extends CompositeTag {
    
    /**
     * The set of names handled by this tag.
     */
    private static final String[] mIds = new String[] {"nav"};
    
    /**
     * Return the set of names handled by this tag.
     * @return The names to be matched that create tags of this type.
     */
    public String[] getIds() {
      return mIds;
    }
    
  }
  
  /**
   * A progress tag.  Can be registered with registerTag() to cause progress
   * to be a CompositeTag.
   * @since 1.66
   */
  public static class Progress extends CompositeTag {
    
    /**
     * The set of names handled by this tag.
     */
    private static final String[] mIds = new String[] {"progress"};
    
    /**
     * Return the set of names handled by this tag.
     * @return The names to be matched that create tags of this type.
     */
    public String[] getIds() {
      return mIds;
    }
    
  }
  
  /**
   * A summary tag.  Can be registered with registerTag() to cause summary
   * to be a CompositeTag.
   * @since 1.66
   */
  public static class Summary extends CompositeTag {
    
    /**
     * The set of names handled by this tag.
     */
    private static final String[] mIds = new String[] {"summary"};
    
    /**
     * Return the set of names handled by this tag.
     * @return The names to be matched that create tags of this type.
     */
    public String[] getIds() {
      return mIds;
    }
    
  }
  
  /**
   * A time tag.  Can be registered with registerTag() to cause time
   * to be a CompositeTag.
   * @since 1.66
   */
  public static class Time extends CompositeTag {
    
    /**
     * The set of names handled by this tag.
     */
    private static final String[] mIds = new String[] {"time"};
    
    /**
     * Return the set of names handled by this tag.
     * @return The names to be matched that create tags of this type.
     */
    public String[] getIds() {
      return mIds;
    }
    
  }
  
  /**
   * An IFRAME tag.  Registered with PrototypicalNodeFactory to cause iframe
   * to be a CompositeTag.  See code samples in org.htmlparser.tags.
   * @see HtmlFilterInputStream#makeParser()
   */
  public static class Iframe extends CompositeTag {

    /**
     * The set of names handled by this tag.
     */
    private static final String[] mIds = new String[] {"IFRAME"};

    /**
     * Return the set of names handled by this tag.
     * @return The names to be matched that create tags of this type.
     */
    public String[] getIds() {
      return mIds;
    }

  }

  /**
   * @since 1.67.4 
   */
  public static class Center extends CompositeTag {

    private static final String[] mIds = new String[] {"CENTER"};

    public String[] getIds() {
      return mIds;
    }

  }

  /**
   * A NOSCRIPT tag.  Registered with PrototypicalNodeFactory to cause noscript
   * to be a CompositeTag.  See code samples in org.htmlparser.tags.
   * @see HtmlFilterInputStream#makeParser()
   */
  public static class Noscript extends CompositeTag {

    /**
     * The set of names handled by this tag.
     */
    private static final String[] mIds = new String[] {"NOSCRIPT"};

    /**
     * Return the set of names handled by this tag.
     * @return The names to be matched that create tags of this type.
     */
    public String[] getIds() {
      return mIds;
    }

  }

  /**
   * A FONT tag.  Registered with PrototypicalNodeFactory to cause iframe
   * to be a CompositeTag.  See code samples in org.htmlparser.tags.
   * @see HtmlFilterInputStream#makeParser()
   */
  public static class Font extends CompositeTag {

    /**
     * The set of names handled by this tag.
     */
    private static final String[] mIds = new String[] {"FONT"};

    /**
     * Return the set of names handled by this tag.
     * @return The names to be matched that create tags of this type.
     */
    public String[] getIds() {
      return mIds;
    }

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
   * @since 1.69 
   */
  /**
   * An ARTICLE tag.  Registered with PrototypicalNodeFactory to cause article
   * to be a CompositeTag.  See code samples in org.htmlparser.tags.
   * @see HtmlFilterInputStream#makeParser()
   */
  public static class Article extends CompositeTag {
    /**
     * The set of names handled by this tag.
     */
    private static final String[] mIds = new String[] {"ARTICLE"};

    /**
     * Return the set of names handled by this tag.
     * @return The names to be matched that create tags of this type.
     */
    public String[] getIds() {
      return mIds;
    }
  }

}
