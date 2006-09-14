/*
 * $Id: AggregatePageTransform.java,v 1.2 2006-09-14 23:10:39 thib_gc Exp $
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

package org.lockss.filter.pdf;

import java.io.IOException;
import java.util.*;

import org.lockss.util.*;
import org.lockss.util.PdfUtil.ResultPolicy;

/**
 * <p>A page transform made of many other page transforms,
 * applied sequentially.
 * @author Thib Guicherd-Callin
 */
public class AggregatePageTransform implements PageTransform {

  /**
   * <p>A list of registered {@link PageTransform} instances.</p>
   */
  protected List /* of PageTransform */ pageTransforms;

  /**
   * <p>A result policy determining the boolean result of the
   * transform.</p>
   */
  protected ResultPolicy resultPolicy;

  /**
   * <p>Builds a new aggregate page transform using the default
   * result policy.</p>
   * @param resultPolicy   A result policy.
   * @see #AggregatePageTransform(ResultPolicy)
   * @see #POLICY_BY_DEFAULT
   */
  public AggregatePageTransform() {
    this(POLICY_BY_DEFAULT);
  }

  /**
   * <p>Builds a new aggregate page transform using the default
   * result policy and registers the given page transforms.</p>
   * @param resultPolicy   A result policy.
   * @param pageTransform1 A page transform.
   * @see #AggregatePageTransform(ResultPolicy, PageTransform)
   * @see #POLICY_BY_DEFAULT
   */
  public AggregatePageTransform(PageTransform pageTransform1) {
    this(POLICY_BY_DEFAULT,
         pageTransform1);
  }

  /**
   * <p>Builds a new aggregate page transform using the default
   * result policy and registers the given page transforms.</p>
   * @param resultPolicy   A result policy.
   * @param pageTransform1 A page transform.
   * @param pageTransform2 A page transform.
   * @see #AggregatePageTransform(ResultPolicy, PageTransform, PageTransform)
   * @see #POLICY_BY_DEFAULT
   */
  public AggregatePageTransform(PageTransform pageTransform1,
                                PageTransform pageTransform2) {
    this(POLICY_BY_DEFAULT,
         pageTransform1,
         pageTransform2);
  }

  /**
   * <p>Builds a new aggregate page transform using the default
   * result policy and registers the given page transforms.</p>
   * @param resultPolicy   A result policy.
   * @param pageTransform1 A page transform.
   * @param pageTransform2 A page transform.
   * @param pageTransform3 A page transform.
   * @see #AggregatePageTransform(ResultPolicy, PageTransform, PageTransform, PageTransform)
   * @see #POLICY_BY_DEFAULT
   */
  public AggregatePageTransform(PageTransform pageTransform1,
                                PageTransform pageTransform2,
                                PageTransform pageTransform3) {
    this(POLICY_BY_DEFAULT,
         pageTransform1,
         pageTransform2,
         pageTransform3);
  }

  /**
   * <p>Builds a new aggregate page transform using the given
   * result policy.</p>
   * @param resultPolicy   A result policy.
   */
  public AggregatePageTransform(ResultPolicy resultPolicy) {
    this.resultPolicy = resultPolicy;
    this.pageTransforms = new ArrayList();
  }

  /**
   * <p>Builds a new aggregate page transform using the given
   * result policy and registers the given page transforms.</p>
   * @param resultPolicy   A result policy.
   * @param pageTransform1 A page transform.
   * @see #AggregatePageTransform(ResultPolicy)
   */
  public AggregatePageTransform(ResultPolicy resultPolicy,
                                PageTransform pageTransform1) {
    this(resultPolicy);
    add(pageTransform1);
  }

  /**
   * <p>Builds a new aggregate page transform using the given
   * result policy and registers the given page transforms.</p>
   * @param resultPolicy   A result policy.
   * @param pageTransform1 A page transform.
   * @param pageTransform2 A page transform.
   * @see #AggregatePageTransform(ResultPolicy, PageTransform)
   */
  public AggregatePageTransform(ResultPolicy resultPolicy,
                                PageTransform pageTransform1,
                                PageTransform pageTransform2) {
    this(resultPolicy,
         pageTransform1);
    add(pageTransform2);
  }

  /**
   * <p>Builds a new aggregate page transform using the given
   * result policy and registers the given page transforms.</p>
   * @param resultPolicy   A result policy.
   * @param pageTransform1 A page transform.
   * @param pageTransform2 A page transform.
   * @param pageTransform3 A page transform.
   * @see #AggregatePageTransform(ResultPolicy, PageTransform, PageTransform)
   */
  public AggregatePageTransform(ResultPolicy resultPolicy,
                                PageTransform pageTransform1,
                                PageTransform pageTransform2,
                                PageTransform pageTransform3) {
    this(resultPolicy,
         pageTransform1,
         pageTransform2);
    add(pageTransform3);
  }

  /**
   * <p>Registers a new {@link PageTransform} instance with
   * this aggregate page transform.</p>
   * <p>When transforming a PDF page, the actions performed by the
   * registered page tranforms are applied in the order the page
   * transforms were registered with this method.</p>
   * @param pageTransform A {@link PageTransform} instance.
   */
  public synchronized void add(PageTransform pageTransform) {
    pageTransforms.add(pageTransform);
  }

  /* Inherit documentation */
  public boolean transform(PdfPage pdfPage) throws IOException {
    boolean success = resultPolicy.resetResult();
    for (Iterator iter = pageTransforms.iterator() ; iter.hasNext() ; ) {
      PageTransform pageTransform = (PageTransform)iter.next();
      success = resultPolicy.updateResult(success, pageTransform.transform(pdfPage));
      if (!resultPolicy.shouldKeepGoing(success)) {
        break;
      }
    }
    return success;
  }

  /**
   * <p>The default result policy used by this class.</p>
   * @see #AggregatePageTransform()
   * @see #AggregatePageTransform(PageTransform)
   * @see #AggregatePageTransform(PageTransform, PageTransform)
   * @see #AggregatePageTransform(PageTransform, PageTransform, PageTransform)
   */
  public static final ResultPolicy POLICY_BY_DEFAULT = PdfUtil.AND;

  /**
   * <p>A logger for use by this class.</p>
   */
  protected static Logger logger = Logger.getLogger("AggregatePageTransform");

}
