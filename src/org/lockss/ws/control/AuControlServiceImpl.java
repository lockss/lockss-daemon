/*
 * $Id$
 */

/*

 Copyright (c) 2015 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.ws.control;

import java.util.ArrayList;
import java.util.List;
import javax.jws.WebService;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.lockss.account.UserAccount;
import org.lockss.app.LockssDaemon;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.servlet.DebugPanel;
import org.lockss.state.AuState;
import org.lockss.state.SubstanceChecker;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import org.lockss.ws.cxf.AuthorizationInterceptor;
import org.lockss.ws.entities.CheckSubstanceResult;
import org.lockss.ws.entities.LockssWebServicesFault;

/**
 * The AU Control web service implementation.
 */
@WebService
public class AuControlServiceImpl implements AuControlService {
  static String MISSING_AU_ID_ERROR_MESSAGE = "Missing auId";
  static String NO_SUBSTANCE_ERROR_MESSAGE =
      "No substance patterns defined for plugin";
  static String NO_SUCH_AU_ERROR_MESSAGE = "No such Archival Unit";
  static String UNEXPECTED_ERROR_MESSAGE = "Error in SubstanceChecker; see log";

  private static Logger log = Logger.getLogger(AuControlServiceImpl.class);

  /**
   * Provides an indication of whether an Archival Unit has substance.
   * 
   * @param auId
   *          A String with the identifier (auid) of the archival unit.
   * @return a CheckSubstanceResult with the result of the operation.
   * @throws LockssWebServicesFault
   */
  public CheckSubstanceResult checkSubstanceById(String auId)
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "checkSubstanceById(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auId = " + auId);

    // Add to the audit log a reference to this operation, if necessary.
    audit(DebugPanel.ACTION_CHECK_SUBSTANCE, auId);

    // Handle a missing auId.
    if (StringUtil.isNullString(auId)) {
      return new CheckSubstanceResult(auId, null, null,
	  MISSING_AU_ID_ERROR_MESSAGE);
    }

    // Get the Archival Unit to be checked.
    ArchivalUnit au =
	LockssDaemon.getLockssDaemon().getPluginManager().getAuFromId(auId);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "au = " + au);

    // Handle a missing Archival Unit.
    if (au == null) {
      return new CheckSubstanceResult(auId, null, null,
	  NO_SUCH_AU_ERROR_MESSAGE);
    }

    AuState auState = null;
    SubstanceChecker.State oldState = null;
    SubstanceChecker.State newState = null;
    String errorMessage = null;

    try {
      // Create the substance checker.
      SubstanceChecker subChecker = new SubstanceChecker(au);
      if (!subChecker.isEnabled()) {
	return new CheckSubstanceResult(auId, null, null,
	    NO_SUBSTANCE_ERROR_MESSAGE);
      }

      // Get the cached substance check state.
      auState = AuUtil.getAuState(au);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auState = " + auState);

      if (auState != null) {
	oldState = auState.getSubstanceState();
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "oldState = " + oldState);
      }

      // Get the actual substance check state.
      newState = subChecker.findSubstance();
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "newState = " + newState);
    } catch (Exception e) {
      errorMessage = e.getMessage();
    }

    // Record the result and populate the response.
    if (newState != null) {
      switch (newState) {
      case Unknown:
	log.error("Shouldn't happen: SubstanceChecker returned Unknown");
	errorMessage = UNEXPECTED_ERROR_MESSAGE;
	break;
      case Yes:
	if (auState != null) {
	  auState.setSubstanceState(SubstanceChecker.State.Yes);
	}

	break;
      case No:
	if (auState != null) {
	  auState.setSubstanceState(SubstanceChecker.State.No);
	}

	break;
      }
    } else {
      if (errorMessage == null) {
	log.error("Shouldn't happen: SubstanceChecker returned null");
	errorMessage = UNEXPECTED_ERROR_MESSAGE;
      }
    }

    CheckSubstanceResult result = new CheckSubstanceResult(auId,
	convertToWsState(oldState), convertToWsState(newState), errorMessage);
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);

    return result;
  }

  /**
   * Provides an indication of whether the archival units defined by a list with
   * their identifiers have substance.
   * 
   * @param auIds
   *          A List<String> with the identifiers (auids) of the archival units.
   * @return a List<CheckSubstanceResult> with the results of the operation.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<CheckSubstanceResult> checkSubstanceByIdList(List<String> auIds)
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "checkSubstanceByIdList(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auIds = " + auIds);

    List<CheckSubstanceResult> results =
	new ArrayList<CheckSubstanceResult>(auIds.size());

    // Loop  through all the Archival Unit identifiers.
    for (String auId : auIds) {
      results.add(checkSubstanceById(auId));
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "results = " + results);
    return results;
  }

  /**
   * Adds to the audit log a reference to this operation, if necessary.
   * 
   * @param action
   *          A String with the name of the operation.
   * @param auId
   *          A String with the identifier (auid) of the archival unit.
   * @throws LockssWebServicesFault
   */
  private void audit(String action, String auId) throws LockssWebServicesFault {
    final String DEBUG_HEADER = "audit(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "action = " + action);
      log.debug2(DEBUG_HEADER + "auId = " + auId);
    }

    // Get the user account.
    UserAccount userAccount = AuthorizationInterceptor
	.getUserAccount(PhaseInterceptorChain.getCurrentMessage());

    if (userAccount != null && !DebugPanel.noAuditActions.contains(action)) {
      userAccount.auditableEvent("Called AuControl web service operation '"
	  + action + "' AU ID: " + auId);
    }
  }

  /**
   * Provides a conversion from a SubstanceChecker.State to the corresponding
   * CheckSubstanceResult.State.
   * 
   * @param state
   *          A SubstanceChecker.State with the state to be converted.
   * @return a CheckSubstanceResult.State with the converted state.
   */
  private CheckSubstanceResult.State convertToWsState(
      SubstanceChecker.State state) {
    if (state != null) {
      switch (state) {
      case Unknown:
	return CheckSubstanceResult.State.Unknown;
      case Yes:
	return CheckSubstanceResult.State.Yes;
      case No:
	return CheckSubstanceResult.State.No;
      }
    }

    return null;
  }
}
