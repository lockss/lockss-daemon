/*
 * * $Id$
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
 * Common UI API definitions
 */
public interface ApiParameters extends CommonParameters {
  /*
   * Version information
   *
   * API software version
   */
  final static String   AP_VERSION          = "0.1";

  /* Identifies this variant of our XML document format
   *
   */
  final static String   AP_XML_VERSION10    = "1.0";
  final static String   AP_XML_VERSION      = AP_XML_VERSION10;
  /*
   * XML Element names
   */
  final static String   AP_RESPONSEROOT     = "response";
  final static String   AP_REQUESTROOT      = "request";

  final static String   AP_E_MESSAGE        = COM_E_MESSAGE;
  final static String   AP_E_STATUS         = COM_E_STATUS;
  final static String   AP_E_DETAIL         = COM_E_DETAIL;
  final static String   AP_E_OPTION         = "option";
  final static String   AP_E_COMMENT        = "comment";
  final static String   AP_E_TABLE          = "table";
  final static String   AP_E_NAME           = "name";
  final static String   AP_E_VALUE          = "value";
  final static String   AP_E_KEY            = "key";
  final static String   AP_E_ID             = "id";
  final static String   AP_E_VERSION        = "version";
  final static String   AP_E_INTERNAL       = "internal";
  final static String   AP_E_SIZE           = "size";
  final static String   AP_E_DESCRIPTION    = "description";
  final static String   AP_E_TYPE           = "type";
  final static String   AP_E_INFO           = "information";
  final static String   AP_E_BUILD          = "build";
  final static String   AP_E_TIMESTAMP      = "timestamp";
  final static String   AP_E_TIME           = "time";
  final static String   AP_E_DATE           = "date";
  final static String   AP_E_USER           = "user";
  final static String   AP_E_PW1            = "pw1";
  final static String   AP_E_PW2            = "pw2";
  final static String   AP_E_HOST           = "host";
  final static String   AP_E_PLATFORMVER    = "platformversion";

  final static String   AP_E_SETMEMBER      = "setmember";
  final static String   AP_E_NEWMEMBER      = "new_member";
  final static String   AP_E_NEWPORT        = "new_member_port";
  final static String   AP_E_EXISTINGCLUSTER
                                            = "existing_cluster";
  final static String   AP_E_EXISTINGMEMBER = "existing_member";

  final static String   AP_E_ALLOWMEMBER    = "allow_member";
  final static String   AP_E_DENYMEMBER     = "deny_member";

  final static String   AP_E_TITLE          = "title";
  final static String   AP_E_ISSN           = "issn";
  final static String   AP_E_LOCALID        = "localid";
  final static String   AP_E_SUBSCRIPTION   = "subscription";
  final static String   AP_E_PUBLICATION    = "publication";

  final static String   AP_E_PLUGIN         = "plugin";
  final static String   AP_E_PLUGINNAME     = "pluginname";
  final static String   AP_E_CLASSNAME      = "classname";

  final static String   AP_E_AU             = "archivalunit";

  final static String   AP_E_EDIT           = "edit";
  final static String   AP_E_REMOVE         = "remove";
  final static String   AP_E_RESTORE        = "restore";
  final static String   AP_E_ACTION         = "action";
  final static String   AP_E_TARGET         = "target";
  final static String   AP_E_SYSTEM         = "system";
  final static String   AP_E_PARAMETER      = "parameter";
  final static String   AP_E_RESULT         = "result";

  final static String   AP_E_SETUP          = "setup";

  final static String   AP_E_AUDEFINING     = "defining";
  final static String   AP_E_AUEDIT         = "editable";
  final static String   AP_E_AUID           = "auid";

  final static String   AP_E_ACTIVE         = "active";
  final static String   AP_E_INACTIVE       = "inactive";

  final static String   AP_E_SEARCH1        = "search1";
  final static String   AP_E_SEARCH2        = "search2";
  final static String   AP_E_FIELD1         = "field1";
  final static String   AP_E_FIELD2         = "field2";
  final static String   AP_E_SEARCHOPERATOR = "operator";

  final static String   AP_E_TOTAL          = "total";

  final static String   AP_E_UPTIME         = "uptime";
  final static String   AP_E_DAYS           = "days";
  final static String   AP_E_HOURS          = "hours";
  final static String   AP_E_MINUTES        = "minutes";
  final static String   AP_E_SECONDS        = "seconds";

  final static String   AP_E_DISKSPACE      = "diskspace";
  final static String   AP_E_FREE           = "free";
  final static String   AP_E_INUSE          = "inuse";

  final static String   AP_E_NUMROWS        = "numrows";
  final static String   AP_E_SKIPROWS       = "skiprows";

  final static String   AP_E_ALERT          = "alert";

  final static String   AP_E_DEBUG          = "debug";

  /*
   * Metadata names for HTML form elements
   */
	final static String   AP_MD_NAME          = "metadataname";
	final static String   AP_MD_AUDEFINING    = "metadatadefining";
	final static String   AP_MD_AUEDIT        = "metadataeditable";
  /*
   * XML Attribute names
   */
  final static String   AP_A_COMMAND        = COM_A_COMMAND;
  final static String   AP_A_SUCCESS        = COM_A_SUCCESS;
  final static String   AP_A_DATE           = COM_A_DATE;
  final static String   AP_A_COLOR          = "color";
  final static String   AP_A_SYSTEM         = COM_A_SYSTEM;
  final static String   AP_A_TYPE           = "type";
  final static String   AP_A_NAME           = "name";
  final static String   AP_A_VALUE          = "value";
  /*
   * API namespace information (URI and prefix)
   */
  final static String   AP_NS_URI           = "http://lockss.org/uiapi";
  final static String   AP_NS_PREFIX        = "api";
  /*
   * Servlet name, port, parameter names
   */
  final static String   AP_SERVLET          = "Api";
  final static String   AP_SERVLET_PORT     = "8081";
  final static String   AP_PARAM_COMMAND    = "command";
  final static String   AP_PARAM_XML        = "xml";
  /*
   * External (API) command names
   */
  final static String   AP_COMMAND_NOOP     = "noop";

  final static String   AP_COMMAND_AUMENU   = "aumenu";
  final static String   AP_COMMAND_ADDAU    = "addau";
  final static String   AP_COMMAND_ADDAUCONFIGURE
                                            = "addauconfigure";
  final static String   AP_COMMAND_REMOVEAU = "auremove";
  final static String   AP_COMMAND_EDITAU   = "editau";
  final static String   AP_COMMAND_RESTOREAU
                                            = "restoreau";

  final static String   AP_COMMAND_CLUSTERSTATUS
                                            = "clusterstatus";
  final static String   AP_COMMAND_MACHINEDETAIL
                                            = "machinedetail";
  final static String   AP_COMMAND_JOURNALDETAIL
                                            = "journaldetail";
  final static String   AP_COMMAND_AUDETAIL = "audetail";
  final static String   AP_COMMAND_ECHO     = "echo";
  final static String   AP_COMMAND_GETTABLE = "gettable";
  final static String   AP_COMMAND_GETINFO  = "getinfo";
  final static String   AP_COMMAND_JOURNALDEFINITION
                                            = "journaldefinition";
  final static String   AP_COMMAND_PROVIDETITLE
                                            = "providetitle";

  /*
   * Option values
   */
  final static String   AP_OPTION_PRESERVE  = "preserve";
  /*
   * Some common values
   */
  final static String   AP_VALUE_UNKNOWN    = COM_VALUE_UNKNOWN;

  final static String   AP_TRUE             = COM_TRUE;
  final static String   AP_FALSE            = COM_FALSE;
  /*
   * Response types: message or standard command response (used with AP_A_TYPE)
   */
  final static String   AP_TYPE_MESSAGE     = "messageblock";
  final static String   AP_TYPE_SETUP       = "setup";
  final static String   AP_TYPE_STANDARD    = "standard";
}
