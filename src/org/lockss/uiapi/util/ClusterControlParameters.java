/*
 * $Id$
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.uiapi.util;

/**
 * Common Cluster Control definitions
 */
public interface ClusterControlParameters extends CommonParameters {
  /*
   * Version information
   *
   * Software version
   */
  final static String   CCP_VERSION         = "0.1";

  /*
   * Identifies this variant of our XML document format
   */
  final static String   CCP_XML_VERSION10   = "1.0";
  final static String   CCP_XML_VERSION     = CCP_XML_VERSION10;
  /*
   * Cluster Control namespace information (URI and prefix)
   */
  final static String   CCP_NS_URI          =
                                  "http://lockss.org/clustercontrol";
  final static String   CCP_NS_PREFIX       = "ui";
  /*
   * Internal Commands amd Parameters
   *
   * Home pages
   */
  final static String   CCP_COMMAND_ADMIN_HOME  = "administration_home";
  final static String   CCP_COMMAND_STATUS_HOME = "status_home";
  final static String   CCP_COMMAND_SEARCH_HOME = "search_home";
  /*
   * Cluster commands
   */
  final static String   CCP_COMMAND_NEWCLUSTER  = "newcluster";
  final static String   CCP_COMMAND_SETCLUSTER  = "setcluster";
  final static String   CCP_COMMAND_SETACTIVECLUSTER
                                                = "setactivecluster";
  final static String   CCP_COMMAND_SETMEMBER   = "setclustermember";
  final static String   CCP_COMMAND_EDITMEMBER  = "editclustermember";

  final static String   CCP_COMMAND_CLUSTERSTATUS
                                                = "clusterstatus";
  /*
   * Journal
   */
  final static String   CCP_COMMAND_JOURNALUPDATE
                                                = "journalupdate";
  final static String   CCP_COMMAND_FIELDNAMES  = "fieldnames";
  /*
   * Archival Unit
   */
  final static String   CCP_COMMAND_AUMENU      = "aumenu";
  /*
   * IP Access Group definition and application
   */
  final static String   CCP_COMMAND_IPACCESSGROUPS
                                                = "ipaccessgroups";
  final static String   CCP_COMMAND_IPACCESSGROUPUPDATE
                                                = "ipaccessgroupupdate";
  final static String   CCP_COMMAND_ACLMENU     = "aclmenu";
  final static String   CCP_COMMAND_ACLEDIT     = "acledit";

  final static String   CCP_COMMAND_ALERTS      = "alerts";

  /*
   * The "not implemented" page:
   *
   *  ILS   = Link to an Integrated Library System
   */
  final static String   CCP_COMMAND_ILS         = "ils";
  /*
   * Empty cluster (external command executed and the cluster has no members)
   */
  final static String   CCP_COMMAND_NOMEMBERS   = "noclustermembers";
  /*
   * Common command parameters
   */
  final static String   CCP_PARAM_COMMAND       = "command";
  final static String   CCP_PARAM_FORMAT        = "format";
  final static String   CCP_PARAM_XML           = "xml";
  final static String   CCP_PARAM_CLUSTER       = "cluster";
  /*
   * Rendering formats: HTML4 and XML (_XML and _EXTERNAL are synonyms)
   */
  final static String   CCP_RENDER_HTML         = "html";
  final static String   CCP_RENDER_XML          = "xml";
  final static String   CCP_RENDER_EXTERNAL     = "external";
  /*
   * Help file
   */
  final static String   CCP_HELP                = "Help.html";
  /*
   * Cluster Control servlet specification used to build HTML HREFs.
   */
  final static String   CCP_CONTROLLER_NAME     = "ClusterControl";

  final static String   CCP_HREF                = CCP_CONTROLLER_NAME
                                                + "?"
                                                + CCP_PARAM_COMMAND
                                                + "=";
  /*
   * HTTP session context parameter names
   */
  final static String   CCP_SESSION_PREFIX      = "org.lockss.uiapi.";
  /*
   * Client's intended (or target) URL
   */
  final static String   CCP_TARGETURL           = CCP_SESSION_PREFIX
                                                + "TargetURL";
  /*
   * Session context command parameter list (a HashMap of name/value pairs)
   */
  final static String   CCP_TARGETPARAMS        = CCP_SESSION_PREFIX
                                                + "TargetParameters";
  /*
   * Cluster response elements
   */
  final static String   CCP_E_RESPONSE      = "response";
  final static String   CCP_E_STATUS        = COM_E_STATUS;
  final static String   CCP_E_MESSAGE       = COM_E_MESSAGE;
  final static String   CCP_E_DETAIL        = COM_E_DETAIL;
  /*
   * Attributes
   */
  final static String   CCP_A_COMMAND       = COM_A_COMMAND;
  final static String   CCP_A_SUCCESS       = COM_A_SUCCESS;
  final static String   CCP_A_DATE          = COM_A_DATE;
  final static String   CCP_A_SYSTEM        = COM_A_SYSTEM;
  /*
   * Command options
   */
  final static String   CCP_OPTION_PRESERVE = "preserve";
}
