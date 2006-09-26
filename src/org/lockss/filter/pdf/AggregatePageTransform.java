/*
 * $Id: AggregatePageTransform.java,v 1.5 2006-09-26 07:32:24 thib_gc Exp $
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
   * @see #AggregatePageTransform(ResultPolicy)
   * @see #POLICY_DEFAULT
   */
  public AggregatePageTransform() {
    this(POLICY_DEFAULT);
  }

  /**
   * <p>Builds a new aggregate page transform using the default
   * result policy and registers the given page transforms.</p>
   * @param pageTransform A page transform.
   * @see #AggregatePageTransform(ResultPolicy, PageTransform)
   * @see #POLICY_DEFAULT
   */
  public AggregatePageTransform(PageTransform pageTransform) {
    this(POLICY_DEFAULT,
         pageTransform);
  }

  /**
   * <p>Builds a new aggregate page transform using the default
   * result policy and registers the given page transforms.</p>
   * @param pageTransform1 A page transform.
   * @param pageTransform2 A page transform.
   * @see #AggregatePageTransform(ResultPolicy, PageTransform, PageTransform)
   * @see #POLICY_DEFAULT
   */
  public AggregatePageTransform(PageTransform pageTransform1,
                                PageTransform pageTransform2) {
    this(POLICY_DEFAULT,
         pageTransform1,
         pageTransform2);
  }

  /**
   * <p>Builds a new aggregate page transform using the default
   * result policy and registers the given page transforms.</p>
   * @param pageTransform1 A page transform.
   * @param pageTransform2 A page transform.
   * @param pageTransform3 A page transform.
   * @see #AggregatePageTransform(ResultPolicy, PageTransform, PageTransform, PageTransform)
   * @see #POLICY_DEFAULT
   */
  public AggregatePageTransform(PageTransform pageTransform1,
                                PageTransform pageTransform2,
                                PageTransform pageTransform3) {
    this(POLICY_DEFAULT,
         pageTransform1,
         pageTransform2,
         pageTransform3);
  }

  /**
   * <p>Builds a new aggregate transform using the default result
   * policy and registers the given page transforms.</p>
   * @param pageTransforms     An array of page transforms.
   * @see #AggregatePageTransform(ResultPolicy, PageTransform[])
   * @see #POLICY_DEFAULT
   */
  public AggregatePageTransform(PageTransform[] pageTransforms) {
    this(POLICY_DEFAULT,
         pageTransforms);
  }

  /**
   * <p>Builds a new aggregate page transform using the given
   * result policy.</p>
   * @param resultPolicy   A result policy.
   */
  public AggregatePageTransform(ResultPolicy resultPolicy) {
    if (resultPolicy == null) {
      String logMessage = "Cannot specify a null result policy";
      logger.error(logMessage);
      throw new NullPointerException(logMessage);
    }
    logger.debug2("Setting up result policy " + resultPolicy.toString());
    this.resultPolicy = resultPolicy;
    this.pageTransforms = new ArrayList();
  }

  /**
   * <p>Builds a new aggregate page transform using the given
   * result policy and registers the given page transforms.</p>
   * @param resultPolicy   A result policy.
   * @param pageTransform A page transform.
   * @see #AggregatePageTransform(ResultPolicy)
   */
  public AggregatePageTransform(ResultPolicy resultPolicy,
                                PageTransform pageTransform) {
    this(resultPolicy);
    logger.debug2("Setting up first page transform");
    add(pageTransform);
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
    logger.debug2("Setting up second page transform");
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
    logger.debug2("Setting up third page transform");
    add(pageTransform3);
  }

  /**
   * <p>Builds a new aggregate transform using the given result
   * policy and registers the given page transforms.</p>
   * @param resultPolicy       A result policy.
   * @param pageTransforms     An array of page transforms.
   * @see #AggregatePageTransform(ResultPolicy)
   */
  public AggregatePageTransform(ResultPolicy resultPolicy,
                                PageTransform[] pageTransforms) {
    this(resultPolicy);
    logger.debug2("Setting up " + pageTransforms.length + " page transforms");
    add(pageTransforms);
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
    if (pageTransform == null) {
      String logMessage = "Cannot add a null page transform";
      logger.error(logMessage);
      throw new NullPointerException(logMessage);
    }
    logger.debug3("Adding a page transform");
    pageTransforms.add(pageTransform);
  }

  /**
   * <p>Registers new {@link DocumentTransform} instances with
   * this aggregate document transform.</p>
   * <p>When transforming a PDF document, the actions performed by the
   * registered tranforms are applied in the order the transforms
   * were registered with this method. This method registers the
   * document transforms in the array in array order.</p>
   * @param pageTransforms An array of {@link PageTransform} instances.
   * @see #add(PageTransform)
   */
  public void add(PageTransform[] pageTransforms) {
    if (pageTransforms == null) {
      String logMessage = "Cannot add a null array of page transforms";
      logger.error(logMessage);
      throw new NullPointerException(logMessage);
    }
    for (int tra = 0 ; tra < pageTransforms.length ; ++tra) {
      logger.debug3("Adding page transform at index " + tra);
      add(pageTransforms[tra]);
    }
  }

  /* Inherit documentation */
  public boolean transform(PdfPage pdfPage) throws IOException {
    logger.debug2("Begin aggregate page transform with result policy " + resultPolicy.toString());
    boolean success = resultPolicy.initialValue();
    logger.debug3("Aggregate success flag initially " + success);
    for (Iterator iter = pageTransforms.iterator() ; iter.hasNext() ; ) {
      PageTransform pageTransform = (PageTransform)iter.next();
      success = resultPolicy.updateResult(success, pageTransform.transform(pdfPage));
      logger.debug3("Aggregate success flag now " + success);
      if (!resultPolicy.shouldKeepGoing(success)) {
        logger.debug3("Aggregation should not keep going");
        break;
      }
    }
    logger.debug("Aggregate page transform result: " + success);
    return success;
  }

  /**
   * <p>The default result policy used by this class.</p>
   * @see #AggregatePageTransform()
   * @see #AggregatePageTransform(PageTransform)
   * @see #AggregatePageTransform(PageTransform, PageTransform)
   * @see #AggregatePageTransform(PageTransform, PageTransform, PageTransform)
   * @see #AggregatePageTransform(PageTransform[])
   */
  public static final ResultPolicy POLICY_DEFAULT = PdfUtil.AND;

  /**
   * <p>A logger for use by this class.</p>
   */
  private static Logger logger = Logger.getLogger("AggregatePageTransform");

}
