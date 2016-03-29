/*
 * $Id$
 */

/*

 Copyright (c) 2016 Board of Trustees of Leland Stanford Jr. University,
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

import static org.lockss.servlet.MetadataMonitor.*;
import java.io.IOException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import org.lockss.db.DbException;
import org.lockss.db.DbManager;
import org.lockss.metadata.MetadataManager;
import org.lockss.util.Logger;

/**
 * Metadata control servlet.
 * 
 * @author Fernando Garcia-Loygorri
 */
@SuppressWarnings("serial")
public class MetadataControl extends LockssServlet {
  private static final Logger log = Logger.getLogger(MetadataControl.class);

  static final String DELETE_PUBLICATION_ISSN_ACTION = "deletePublicationIssn";

  private DbManager dbManager;
  private MetadataManager mdManager;

  /**
   * Initializes the configuration when loaded.
   * 
   * @param config
   *          A ServletConfig with the servlet configuration.
   * @throws ServletException
   */
  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    dbManager = getLockssDaemon().getDbManager();
    mdManager = getLockssDaemon().getMetadataManager();
  }

  /**
   * Processes the user request.
   * 
   * @throws IOException
   *           if any problem occurred writing the page.
   */
  @Override
  public void lockssHandleRequest() throws IOException {
    final String DEBUG_HEADER = "lockssHandleRequest(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    // If the database is not available, display a warning message.
    if (!dbManager.isReady()) {
      displayNotStarted();
      return;
    }

    String action = req.getParameter(ACTION_TAG);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "action = " + action);

    String redirectUrl = req.getParameter(REDIRECT_URL_TAG);
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "redirectUrl = " + redirectUrl);

    try {
      if (DELETE_PUBLICATION_ISSN_ACTION.equals(action)) {
	deletePublicationIssn();
      } else {
	displayWarningInLieuOfPage("Invalid operation '" + action + "'");
	return;
      }
    } catch (Exception e) {
      displayWarningInLieuOfPage("Exception caught: " + e.getMessage());
      return;
    }

    resp.setContentLength(0);
    resp.sendRedirect(redirectUrl);

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Deletes a publication ISSN.
   * 
   * @return a boolean with <code>true</code> if the ISSN was deleted,
   *         <code>false</code> otherwise.
   * 
   * @throws DbException
   *           if any problem occurred accessing the database.
   * @throws IOException
   */
  private boolean deletePublicationIssn() throws DbException, IOException {
    final String DEBUG_HEADER = "deletePublicationIssn(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    Long mdItemSeq = Long.valueOf(req.getParameter("pubId"));
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);

    String issnValue = req.getParameter("issnValue");
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "issnValue = " + issnValue);

    String issnType = req.getParameter("issnType");
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "issnType = " + issnType);

    boolean deleted =
	mdManager.deletePublicationIssn(mdItemSeq, issnValue, issnType);
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "deleted = " + deleted);
    return deleted;
  }
}
