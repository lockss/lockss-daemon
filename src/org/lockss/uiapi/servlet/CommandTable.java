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

package org.lockss.uiapi.servlet;

import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;

import org.lockss.util.*;

import org.lockss.uiapi.commands.*;
import org.lockss.uiapi.util.*;


/**
 * Define the API and "cluster control" command set
 */
public class CommandTable implements ApiParameters, ClusterControlParameters {

  private static Logger log = Logger.getLogger("CommandTable");
  /*
   * API and UI package names
   */
  public static final String  API         = "org.lockss.uiapi.commands";
  public static final String  UI          = "org.lockss.ui.pages";

  /*
   * Command flags:
   *
   * INTERNAL     - Understood by the cluster control servlet
   * EXTERNAL     - Understood by individual cluster members
   */
  private static final int    INTERNAL    = (1 << 0);
  private static final int    EXTERNAL    = (1 << 1);

  /*
   * Command name, classes, flags
   */
  private String  command;

  private String  commandClassName;
  private Class   commandClass;

  private String  localCommandClassName;
  private Class   localCommandClass;

  private String  htmlClassName;
  private Class   htmlClass;

  private int     flags;

  /**
   * Command Table <p>New command processors are added here</p>
   */
  private static CommandTable commands[] =
  {
    /*
     * UI functionality "home" pages.
     */
    new CommandTable(CCP_COMMAND_ADMIN_HOME,
                     UI  + ".AdminHome",
                     UI  + ".html4.AdminHomePage",
                     INTERNAL),

    new CommandTable(CCP_COMMAND_STATUS_HOME,
                     UI  + ".StatusHome",
                     UI  + ".html4.StatusHomePage",
                     INTERNAL),

    new CommandTable(CCP_COMMAND_SEARCH_HOME,
                     UI  + ".SearchHome",
                     UI  + ".html4.SearchHomePage",
                     INTERNAL),
    /*
     * Standard commands
     *
     * ClusterControl (internal - executed by the ClusterControl servlet)
     */
    new CommandTable(CCP_COMMAND_NEWCLUSTER,
                     UI  + ".NewCluster",
                     UI  + ".html4.NewClusterPage",
                     INTERNAL),

    new CommandTable(CCP_COMMAND_SETCLUSTER,
                     UI  + ".SetCluster",
                     UI  + ".html4.SetClusterPage",
                     INTERNAL),

    new CommandTable(CCP_COMMAND_SETMEMBER,
                     UI  + ".SetClusterMember",
                     UI  + ".html4.SetClusterMemberPage",
                     INTERNAL),

    new CommandTable(CCP_COMMAND_EDITMEMBER,
                     UI  + ".EditClusterMember",
                     UI  + ".html4.EditClusterMemberPage",
                     INTERNAL),

    new CommandTable(CCP_COMMAND_SETACTIVECLUSTER,
                     UI  + ".SetActiveCluster",
                     UI  + ".html4.SetActiveClusterPage",
                     INTERNAL),

    new CommandTable(CCP_COMMAND_FIELDNAMES,
                     UI  + ".FieldNames",
                     UI  + ".html4.FieldNamesPage",
                     INTERNAL),

    new CommandTable(CCP_COMMAND_JOURNALUPDATE,
                     UI  + ".JournalUpdate",
                     UI  + ".html4.JournalUpdatePage",
                     INTERNAL),

    new CommandTable(CCP_COMMAND_IPACCESSGROUPS,
                     UI  + ".IpAccessGroups",
                     UI  + ".html4.IpAccessGroupsPage",
                     INTERNAL),

    new CommandTable(CCP_COMMAND_IPACCESSGROUPUPDATE,
                     UI  + ".EditIpAccessGroup",
                     UI  + ".html4.EditIpAccessGroupPage",
                     INTERNAL),

    new CommandTable(CCP_COMMAND_ACLMENU,
                     UI  + ".UiNoop",
                     UI  + ".html4.AclMenuPage",
                     INTERNAL),

    new CommandTable(CCP_COMMAND_ACLEDIT,
                     UI  + ".UiNoop",
                     UI  + ".html4.AclEditPage",
                     INTERNAL),

    new CommandTable(CCP_COMMAND_ALERTS,
                     UI  + ".UiNoop",
                     UI  + ".html4.AlertPage",
                     INTERNAL),

    /*
     * Cluster Empty (internal, invoked by ClusterControl)
     */
    new CommandTable(CCP_COMMAND_NOMEMBERS,
                     UI  + ".UiNoop",
                     UI  + ".html4.EmptyClusterErrorPage",
                     INTERNAL),

    /*
     * Feature is not implemented:
     *
     *    ILS           = Integrated Library System link
     *
     * No actual command processing is required - use the "noop" processor.
     */
    new CommandTable(CCP_COMMAND_ILS,
                     UI  + ".Noop",
                     UI  + ".html4.NotImplementedPage",
                     INTERNAL),
    /*
     * UI API (external - executed on each cluster member)
     */
    new CommandTable(AP_COMMAND_NOOP,
                     API + ".Noop",
                     UI  + ".Noop",
                     UI  + ".html4.NoopPage",
                     EXTERNAL),

    new CommandTable(AP_COMMAND_AUMENU,
                     API + ".AuMenu",
                     UI  + ".AuMenu",
                     UI  + ".html4.AuMenuPage",
                     EXTERNAL),

    new CommandTable(AP_COMMAND_ADDAU,
                     API + ".AddAu",
                     UI  + ".AddAu",
                     UI  + ".html4.AddAuPage",
                     EXTERNAL),

    new CommandTable(AP_COMMAND_ADDAUCONFIGURE,
                     API + ".AddAuConfigure",
                     UI  + ".AddAuConfigure",
                     UI  + ".html4.AddAuConfigurePage",
                     EXTERNAL),

    new CommandTable(AP_COMMAND_EDITAU,
                     API + ".EditAuConfigure",
                     UI  + ".EditAuConfigure",
                     UI  + ".html4.EditAuConfigurePage",
                     EXTERNAL),

    new CommandTable(AP_COMMAND_RESTOREAU,
                     API + ".EditAuConfigure",
                     UI  + ".EditAuConfigure",
                     UI  + ".html4.EditAuConfigurePage",
                     EXTERNAL),

    new CommandTable(AP_COMMAND_ECHO,
                     API + ".Echo",
                     UI  + ".Echo",
                     UI  + ".html4.NotImplementedPage",
                     EXTERNAL),

    new CommandTable(AP_COMMAND_GETINFO,
                     API + ".GetMachineSummary",
                     UI  + ".GetMachineSummary",
                     UI  + ".html4.NotImplementedPage",
                     EXTERNAL),

    new CommandTable(AP_COMMAND_CLUSTERSTATUS,
                     API + ".GetMachineSummary",
                     UI  + ".GetMachineSummary",
                     UI  + ".html4.ClusterStatusPage",
                     EXTERNAL),

    new CommandTable(AP_COMMAND_JOURNALDEFINITION,
                     API + ".JournalDefinition",
                     UI  + ".JournalDefinition",
                     UI  + ".html4.JournalDefinitionPage",
                     EXTERNAL),

     new CommandTable(AP_COMMAND_JOURNALDETAIL,
                     API + ".JournalDetail",
                     UI  + ".JournalDetail",
                     UI  + ".html4.JournalDetailPage",
                     EXTERNAL),

    new CommandTable(AP_COMMAND_MACHINEDETAIL,
                     API + ".MachineDetail",
                     UI  + ".MachineDetail",
                     UI  + ".html4.MachineDetailPage",
                     EXTERNAL),

    new CommandTable(AP_COMMAND_AUDETAIL,
                     API + ".AuDetail",
                     UI  + ".AuDetail",
                     UI  + ".html4.AuDetailPage",
                     EXTERNAL),

    new CommandTable(AP_COMMAND_GETTABLE,
                     API + ".GetTable",
                     UI  + ".GetTable",
                     UI  + ".html4.NotImplementedPage",
                     EXTERNAL),

    new CommandTable(AP_COMMAND_PROVIDETITLE,
                     API + ".ProvideTitle",
                     UI  + ".ProvideTitle",
                     UI  + ".html4.ProvideTitlePage",
                     EXTERNAL),
  };

  /*
   * Constructors
   */
  private CommandTable() { }

  /**
   * Create a populated <code>CommandTable</code> object
   * @param command Command name (eg the name supplied by the client)
   * @param commandClassName The name of a class to carry out the remote
   *              client command
   * @param localCommandClassName The name of a class to carry out the "local"
   *              portion of the client command activities
   * @param htmlClassName HTML renderer for this command
   * @param flags Command characteristics
   * <p>
   * Supply <code>org.lockss.ui.pages.html4.NotImplementedPage</code>
   * as the HTML renderer if no "real" renderer exists.
   */
  private CommandTable(String command,
                       String commandClassName,
                       String localCommandClassName,
                       String htmlClassName,
                       int flags) {

    this.command = command;

    this.commandClassName = commandClassName;
    this.commandClass = null;

    this.localCommandClassName = localCommandClassName;
    this.localCommandClass = null;

    this.htmlClassName = htmlClassName;
    this.htmlClass = null;

    this.flags = flags;
  }

  /**
   * Create a populated <code>CommandTable</code> object
   * @param command Command name (eg the name supplied by the client)
   * @param localCommandClassName The name of a class to carry out the remote
   *              client command
   * @param htmlClassName HTML renderer for this command
   * @param flags Command characteristics
   * <p>
   * Supply <code>org.lockss.ui.pages.html4.NotImplementedPage</code>
   * as the HTML renderer if no "real" renderer exists.
   */
  private CommandTable(String command,
                       String localCommandClassName,
                       String htmlClassName,
                       int flags) {
    this(command, null, localCommandClassName, htmlClassName, flags);
  }

  /**
   * Return a new remote responder object for the specified command.
   *<p>
   * Class loading is defered until request time so that only the
   * required classes are loaded.
   *</p>
   * @param entry CommandTable entry
   * @return A new object for this command (null if none)
   */
  public static Object selectApiResponder(CommandTable entry) {
    Object responder = null;

    try {
      synchronized (commands) {
        if (entry.commandClass == null) {
          entry.commandClass = Class.forName(entry.commandClassName);
        }
      }
      responder = entry.commandClass.newInstance();

    } catch (Exception exception) {
      log.error("Failed to instantiate object for class \""
              + entry.commandClassName
              + "\"",
                exception);
      responder = null;
    }
    return responder;
  }

  /**
   * Return the local responder object for the specified command.
   *<p>
   * Class loading is defered until request time so that only the
   * required classes are loaded.
   *</p>
   * @param entry CommandTable entry
   * @return A new object for this command (null if none)
   */
  public static Object selectLocalResponder(CommandTable entry) {
    Object responder = null;

    try {
      synchronized (commands) {
        if (entry.commandClass == null) {
          entry.localCommandClass = Class.forName(entry.localCommandClassName);
        }
      }
      responder = entry.localCommandClass.newInstance();

    } catch (Exception exception) {
      log.error("Failed to instantiate object for class \""
              + entry.localCommandClassName
              + "\"",
                exception);
      responder = null;
    }
    return responder;
  }

  /**
   * Return a new HTML renderer for the specified command.
   *<p>
   * Class loading is defered until request time so that only the
   * required classes are loaded.
   *</p>
   * @param entry CommandTable entry
   * @return A new HTML renderer for this command (null if none)
   */
  public static Object selectHtmlRenderer(CommandTable entry) {
    Object renderer = null;

    try {
      synchronized (commands) {
        if (entry.htmlClass == null) {
          entry.htmlClass = Class.forName(entry.htmlClassName);
        }
      }
      renderer = entry.htmlClass.newInstance();

    } catch (Exception exception) {
      log.error("Failed to instantiate object for class \""
              + entry.htmlClassName
              + "\"",
                exception);
      renderer = null;
    }
    return renderer;
  }

  /**
   * Is this an "internal" command (executed by the cluster control servlet)?
   *
   * @param entry CommandTable entry
   * @return true (if internal)
   */
  public static boolean isInternal(CommandTable entry) {
    return (entry.flags & INTERNAL) == INTERNAL;
  }

  /**
   * Is this an "external" command (sent to each cluster member)?
   *
   * @param entry Command name
   * @return true (if external)
   */
  public static boolean isExternal(CommandTable entry) {
    return (entry.flags & EXTERNAL) == EXTERNAL;
  }

  /**
   * Lookup a command
   *
   * @param command Command name
   * @return CommandTable object (null if command no found)
   */
  public static CommandTable getEntry(String command) {
    CommandTable entry = null;

    for (int i = 0; i < commands.length; i++) {

      if (commands[i].command.equalsIgnoreCase(command)) {
        entry = commands[i];
        break;
      }
    }
    return entry;
  }
}
