/*
 * $Id: PsmToDot.java,v 1.2 2008-02-15 09:12:46 tlipkis Exp $
 */

/*

 Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.doclet;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import org.apache.commons.collections.*;
import org.apache.commons.collections.map.*;
import org.apache.commons.lang.StringUtils;

import org.lockss.util.*;
import org.lockss.protocol.psm.*;

public class PsmToDot {
  private static Logger log = Logger.getLogger("PsmToDot");

  static String HEADER =
    "digraph StateMachine {\n" +
    "  edge [fontname=\"Times-Italic\"];\n" +
    "  node [fontname=\"Times-Roman\"];\n" +
    "\n";

  static String FOOTER = 
    "  center=true;\n" +
    "\n" +
    "  page=\"8.5,11\";\n" +
    "  size=\"7.5,10\";\n" +
    "}\n";

  static String SUBGRAPH_TEMPLATE =
    "  subgraph <SUBGRAPH_NAME> {\n" +
    "    color=grey;\n" +
    "    fontname=\"Times-Bold\";\n" +
    "    fontsize=\"14\";\n" +
    "    label=\"<LABEL>\";\n" +
    "\n";
    
  static String SUBGRAPH_END = "  }\n";

  /** StateInfo */
  class SI {
    String name;
    PsmState state;
    Set eventNames;
    List<String> msgs = new ArrayList();
    String dotNode;

    SI(PsmState state) {
      this.state = state;
      this.name = state.getName();
    }
  }

  private List<Graph> graphs = new ArrayList();
  private PrintStream pout;

  PsmToDot() {
  }

  String tab(int n) {
    return StringUtils.repeat(" ", n);
  }      

  class Graph {
    String label;
    PsmMachine.Factory factory;
    PsmMachine machine;
    Class actionsClass;

    PsmState[] states;
    private Map<String,String> dotActions = new HashMap();
    private Map<String,SI> infoMap = new HashMap();
    private MultiMap msgRcvrs = new MultiValueMap();

    List<String> getMsgRcvrs(String msg) {
      return (List<String>)msgRcvrs.get(msg);
    }

    void doGraph(int subgraph) {
      String cluster = "cluster" + Integer.toString(subgraph);

      pout.print(replaceStrings(SUBGRAPH_TEMPLATE,
				"<SUBGRAPH_NAME>", cluster,
				"<LABEL>", this.label));
      states = machine.getStates();
      PsmState initialState = machine.getInitialState();
      pout.println();
      for (PsmState state : states) {
	SI sInfo = new SI(state);
	String name = state.getName();
	infoMap.put(name, sInfo);
	StringBuilder sb = new StringBuilder();
	sb.append(" [");
	PsmAction entryAction = state.getEntryAction();
	if (entryAction != null) {
	  List<String> msgs = getMsgs(entryAction);
	  if (msgs != null) {
	    for (String msg : msgs) {
	      sInfo.msgs.add(msg);
	    }
	  }
	}
	List<String> msgEvents = getResponseMsgEvents(state);
	if (msgEvents != null) {
	  for (String evt : msgEvents) {
	    msgRcvrs.put(evt, name);
	  }
	}
	if (true) {
	  sb.append("label=\"");
	  sb.append(name);
	  //       sb.append("\\c");
	  String actionLabel = getActionLabel(entryAction);
	  if (actionLabel != null) {
	    sb.append("\\l ");
	    sb.append(actionLabel);
	  }
	  sb.append("\"");
	} else {
	  sb.append("label=<<TABLE><TR ALIGN=\"CENTER\"><TD>");
	  sb.append("<FONT POINT-SIZE=\"14.0\">");
	  sb.append(name);
// 	  String actionHtml = getActionHtml(entryAction);
// 	  if (actionHtml != null) {
// 	    sb.append(actionHtml);
// 	  }
	  sb.append("</TABLE>");
	  sb.append(">");

	}
	if (state == initialState || state.isFinal()) {
	  sb.append(",shape=egg");
	}
	if (state.isResumable()) {
	  sb.append(",peripheries=2");
	}
	sb.append("];");
	sInfo.dotNode = sb.toString();
	for (PsmResponse resp : state.getResponses()) {
	  if (resp.isTransition()) {
	    outlink(4, state.getName(), resp.getNewState(),
		    eventName(resp.getEvent()),
		    "fontsize=\"10\"");
	  } else if (resp.isAction()) {
	    PsmAction action = resp.getAction();
	    if (action instanceof PsmMethodAction) {
	      PsmMethodAction mthAction = (PsmMethodAction)action;
	      Method method = mthAction.getMethod();
	      String mthName = method.getName();
	      String label = getActionLabel(mthAction);
	      dotActions.put(mthName, label);
	      outlink(4, state.getName(), mthName,
		      eventName(resp.getEvent()),
		      "fontsize=\"10\"");
	    } else if (action instanceof PsmWait) {
	      outlink(4, state.getName(), state.getName(),
		      eventName(resp.getEvent()),
		      "fontsize=\"10\",arrowhead=\"ediamond\"");
	    } else {
	      log.warning("Unhandled action: " + action);
	    }
	  }
	}
      }
      for (Map.Entry<String,SI> ent : infoMap.entrySet()) {
	SI sInfo = ent.getValue();
	pout.println(tab(4) + ent.getKey() + sInfo.dotNode);
      }
      for (Map.Entry<String,String> ent : dotActions.entrySet()) {
	pout.println(tab(4) + ent.getKey() + " [shape=box,label=\"" +
		     ent.getValue() + "\"];");
      }
      for (Map.Entry<String,SI> ent : infoMap.entrySet()) {
	SI sInfo = ent.getValue();
	pout.println(tab(4) + ent.getKey() + sInfo.dotNode);
      }
      pout.println(SUBGRAPH_END);
    }

    void doMsgs(List<Graph> graphs) {
      for (Map.Entry<String,SI> ent : infoMap.entrySet()) {
	SI sInfo = ent.getValue();
	if (sInfo.msgs != null) {
	  for (String msg : sInfo.msgs) {
	    List<String> toStates = new ArrayList();
	    for (Graph graph : graphs) {
// 	      if (graph == this) {
// 		continue;
// 	      }
	      List<String> rcvrs = graph.getMsgRcvrs(msg);
	      if (rcvrs != null) {
		toStates.addAll(rcvrs);
	      }
	    }
// 	    for (String toState : toStates) {
// 	      outlink(2, sInfo.name, toState, msg, "style=\"dotted\"");
// 	    }
	  }
	}
      }
	    
    }

  }

  List<String> getResponseMsgEvents(PsmState state) {
    List<String> res = new ArrayList(4);
    for (PsmResponse resp : state.getResponses()) {
      String evt = eventName(resp.getEvent());
      if (evt != null && evt.startsWith("msg")) {
	res.add(evt);
      }
    }
    return res;
  }

  void outlink(int indent, String from, String to,
	       String label, String attrs) {
    List<String> lst = new ArrayList(2);
    if (!StringUtil.isNullString(label)) {
      lst.add("label=\"" + label + "\"");
    }
    if (attrs != null) {
      lst.add(attrs);
    }
    StringBuilder sb = new StringBuilder();
    sb.append(tab(indent));
    sb.append(from);
    sb.append(" -> ");
    sb.append(to);
    if (!lst.isEmpty()) {
      sb.append(" [");
      sb.append(StringUtil.separatedString(lst, ","));
      sb.append(" ]");
    }
    sb.append(";");
    pout.println(sb.toString());
  }

  private void convert(String argv[]) {
    String fileName = null;
    Writer wrtr;
    File outFile;
    Graph curGraph = null;

    if (argv.length == 0) {
      usage();
    }
    try {
      for (int ix = 0; ix < argv.length; ix++) {
	String arg = argv[ix];
	if (arg.startsWith("-m")) {
	  String className = argv[++ix];
	  try {
	    curGraph = new Graph();
	    Class factoryClass = Class.forName(className);
	    curGraph.factory = (PsmMachine.Factory)factoryClass.newInstance();
	    graphs.add(curGraph);
	  } catch (Exception e) {
	    log.error("Can't create factory: " + className, e);
	    System.exit(1);
	    return;
	  }
	  
	} else if (arg.startsWith("-l")) {
	  if (curGraph == null) {
	    usage();
	    return;
	  }
	  curGraph.label = argv[++ix];
	} else if (arg.startsWith("-a")) {
	  if (curGraph == null) {
	    usage();
	    return;
	  }
	  String actName = argv[++ix];
	  try {
	    curGraph.actionsClass = Class.forName(actName);
	  } catch (Exception e) {
	    log.error("Can't create actions class: " + actName, e);
	    System.exit(1);
	    return;
	  }
	} else if (arg.startsWith("-o")) {
	  fileName = argv[++ix];
	} else {
	  usage();
	}
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      usage();
    }
    if (graphs.isEmpty()) {
      usage();
      return;
    }

    pout = System.out;
    if (fileName != null) {
      try {
	FileOutputStream fos = new FileOutputStream(fileName);
	pout = new PrintStream(fos);
      } catch (Exception e) {
	log.error("Can't open output file", e);
	System.exit(1);
      }
    }

    for (Graph graph : graphs) {
      try {
	if (graph.actionsClass == null) {
	  log.error("No actions class specified for " + graph.factory);
	  usage();
	}
	PsmMachine machine = makeMachine(graph.factory, graph.actionsClass);
	graph.machine = machine;
      } catch (Exception e) {
	log.error("Can't create machine: " + graph.factory, e);
	System.exit(1);
	return;
      }
    }


    pout.print(HEADER);
    int ix = 1;

    for (Graph graph : graphs) {
      graph.doGraph(ix);
      ix++;
    }
    for (Graph graph : graphs) {
      graph.doMsgs(graphs);
    }
    pout.print(FOOTER);
  }

  private String getActionLabel(PsmAction action) {
    if (action instanceof PsmMethodAction) {
      PsmMethodAction mthAction = (PsmMethodAction)action;
      Method method = mthAction.getMethod();
      String label = method.getName();
      List<String> evts = getEvents(method);
      if (evts != null) {
	if (SetUtil.set("evtOk", "evtError").equals(SetUtil.theSet(evts))) {
	  label += ("\\l  <- " + StringUtil.separatedString(evts, ", "));
	} else {
	  for (String evt : evts) {
	    label += ("\\l  <- " + evt);
	  }
	  label += "\\l";
	}
      }
      List<String> msgs = getMsgs(method);
      if (msgs != null) {
	for (String msg : msgs) {
	  label += ("\\l  -> " + msg);
	}
	label += "\\l";
      }
      return label;
    }
    return action.toString();
  }

//   private String getActionHtml(PsmAction action, int border) {
//     if (action instanceof PsmMethodAction) {
//       PsmMethodAction mthAction = (PsmMethodAction)action;
//       Method method = mthAction.getMethod();
//       String label = method.getName();
//       List<String> evts = getEvents(method);
//       if (evts != null) {
// 	if (SetUtil.set("evtOk", "evtError").equals(SetUtil.theSet(evts))) {
// 	  return label + "\\l  -> " + StringUtil.separatedString(evts, ", ");
// 	}
// 	for (String evt : evts) {
// 	  label += ("\\l  -> " + evt);
// 	}
// 	label += "\\l";
//       }
//       return label;
//     }
//     return action.toString();
//   }

  private List getMsgs(PsmAction action) {
    if (action instanceof PsmMethodAction) {
      PsmMethodAction mthAction = (PsmMethodAction)action;
      Method method = mthAction.getMethod();
      List<String> msgs = getMsgs(method);
      if (msgs != null && !msgs.isEmpty()) {
	return msgs;
      }
    }
    return null;
  }

  List<String> getEvents(Method method) {
    ReturnEvents anno = method.getAnnotation(ReturnEvents.class);
    if (anno != null) {
      return StringUtil.breakAt(anno.value(), ",");
    }
    return null;
  }

  List<String> getMsgs(Method method) {
    SendMessages anno = method.getAnnotation(SendMessages.class);
    if (anno != null) {
      return StringUtil.breakAt(anno.value(), ",");
    }
    return null;
  }

  private String eventName(PsmEvent event) {
    String eventName = event.toString();
    int pos = eventName.indexOf("$");
    if (pos >= 0) {
      eventName = eventName.substring(pos+1);
    }
//     eventName = StringUtils.uncapitalize(eventName);
    if (event instanceof PsmMsgEvent) {
      eventName = "msg" + eventName;
    } else {
      eventName = "evt" + eventName;
    }
    return eventName;
  }

  public static void main(String argv[]) throws Exception {
    new PsmToDot().convert(argv);
  }

  private static PsmMachine makeMachine(PsmMachine.Factory factory,
					Class actionsClass)
      throws Exception {
    return factory.getMachine(actionsClass);
  }


  static String replaceStrings(String template,
			       String pat1, String sub1,
			       String pat2, String sub2) {
    return StringUtil.replaceString(StringUtil.replaceString(template, pat1, sub1), pat2, sub2);
  }

  static void usage() {
    System.err.println("Usage: PsmToDot  -m <PsmFactoryClass> -a <ActionsClass> [-l <label>]\n                 [-m & -a & [-l &] ] ...\n                 [-o outfile]");
    System.exit(1);
  }

}
