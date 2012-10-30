/*
 * $Id: PrintfConverter.java,v 1.5 2012-10-30 00:11:44 tlipkis Exp $
 */

/*
 Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin;

import java.util.*;

import org.apache.oro.text.regex.*;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.util.Constants.RegexpContext;

public abstract class PrintfConverter {
  static Logger log = Logger.getLogger("PrintfConverter");

  public static final String RANGE_SUBSTITUTION_STRING = "(.*)";
  public static final String NUM_SUBSTITUTION_STRING = "(\\d+)";

  public static final int MAX_NUM_RANGE_SIZE = 100;

  protected TypedEntryMap paramMap;
  protected Plugin plugin;

  PrintfUtil.PrintfData p_data;
  String format;
  PrintfFormat pf;
  Collection<String> p_args;
  ArrayList substitute_args;
  boolean missingArgs = false;

  private PrintfConverter(ArchivalUnit au) {
    this(au.getPlugin(), au.getProperties());
  }

  private PrintfConverter(Plugin plugin, TypedEntryMap paramMap) {
    this.plugin = plugin;
    this.paramMap = paramMap;
  }

  void convert(String printfString) {
    p_data = PrintfUtil.stringToPrintf(printfString);
    format = p_data.getFormat();
    p_args = p_data.getArguments();
    pf = new PrintfFormat(format);
    substitute_args = new ArrayList(p_args.size());

    for (String key : p_args) {
      Object val = paramMap.getMapElement(key);
      ConfigParamDescr descr = plugin.findAuConfigDescr(key);
      if (val != null) {
	interpArg(key, val, descr);
      } else {
	missingArgs = true;
	log.warning("missing argument for: " + key);
	interpNullArg(key, val, descr);
      }
    }
  }

  void interpArg(String key, Object val, ConfigParamDescr descr) {
    switch (descr != null ? descr.getType()
	    : ConfigParamDescr.TYPE_STRING) {
    case ConfigParamDescr.TYPE_SET:
      interpSetArg(key, val, descr);
      break;
    case ConfigParamDescr.TYPE_RANGE:
      interpRangeArg(key, val, descr);
      break;
    case ConfigParamDescr.TYPE_NUM_RANGE:
      interpNumRangeArg(key, val, descr);
      break;
    default:
      interpPlainArg(key, val, descr);
      break;
    }
  }

  abstract void interpSetArg(String key, Object val,
			     ConfigParamDescr descr);
  abstract void interpRangeArg(String key, Object val,
			       ConfigParamDescr descr);
  abstract void interpNumRangeArg(String key, Object val,
				  ConfigParamDescr descr);
  abstract void interpPlainArg(String key, Object val,
			       ConfigParamDescr descr);
  void interpNullArg(String key, Object val, ConfigParamDescr descr) {
  }

  public static RegexpConverter newRegexpConverter(ArchivalUnit au,
						   RegexpContext context) {
    return newRegexpConverter(au.getPlugin(), au.getProperties(), context);
  }

  public static RegexpConverter newRegexpConverter(Plugin plugin,
						   TypedEntryMap paramMap,
						   RegexpContext context) {
    switch (context) {
    case Url:
      return new UrlRegexpConverter(plugin, paramMap);
    case String:
    default:
      return new RegexpConverter(plugin, paramMap);
    }
  }

  public static class RegexpConverter extends PrintfConverter {
    ArrayList matchArgs = new ArrayList();
    ArrayList matchArgDescrs = new ArrayList();

    private RegexpConverter(ArchivalUnit au) {
      super(au);
    }

    private RegexpConverter(Plugin plugin, TypedEntryMap paramMap) {
      super(plugin, paramMap);
    }

    // Apply regexp quoting (escape meta chars), then 

    String quoteRegexpArg(String val) {
      return Perl5Compiler.quotemeta(val);
    }

    void interpSetArg(String key, Object val, ConfigParamDescr descr) {
      // val must be a list; ok to throw if not
      List<String> vec = (List<String>)val;
      List tmplst = new ArrayList(vec.size());
      for (String ele : vec) {
	tmplst.add(quoteRegexpArg(ele));
      }
      substitute_args.add(StringUtil.separatedString(tmplst, "(?:", "|", ")"));
    }

    void interpRangeArg(String key, Object val, ConfigParamDescr descr) {
      substitute_args.add(RANGE_SUBSTITUTION_STRING);
      matchArgs.add(val);
      matchArgDescrs.add(descr);
    }

    void interpNumRangeArg(String key, Object val, ConfigParamDescr descr) {
      substitute_args.add(NUM_SUBSTITUTION_STRING);
      matchArgs.add(val);
      matchArgDescrs.add(descr);
    }

    void interpPlainArg(String key, Object val, ConfigParamDescr descr) {
      if (val instanceof String) {
	val = quoteRegexpArg((String)val);
      }
      substitute_args.add(val);
    }

    public MatchPattern getMatchPattern(String printfString) {
      convert(printfString);
      if (missingArgs) {
	log.warning("Missing variable arguments: " + p_data);
	return new MatchPattern();
      }
      if (log.isDebug3()) {
	log.debug3("sprintf(\""+format+"\", "+substitute_args+")");
      }
      try {
	return new MatchPattern(pf.sprintf(substitute_args.toArray()),
				matchArgs, matchArgDescrs);
      } catch (Exception e) {
	throw new PluginException.InvalidDefinition(e);
      }
    }

  }

  public static class UrlRegexpConverter extends RegexpConverter {

    private UrlRegexpConverter(ArchivalUnit au) {
      super(au);
    }

    private UrlRegexpConverter(Plugin plugin, TypedEntryMap paramMap) {
      super(plugin, paramMap);
    }

    // Param values substituted into regexps used to match URLs may
    // contain chars that should match various possible URL-encodings.
    // E.g., <space> may be represented in URLs as <space>, "+" (not
    // strictly legal but widespread) or "%20", so we should match any of
    // those.  Other cases are ambiguous (e.g, if there's a % in the URL)
    // so for now we just handle <space>.

    static final String URL_ESCAPED_SPACE_REGEX = "( |\\+|%20)";

    String quoteRegexpArg(String val) {
      val = super.quoteRegexpArg(val);
      // quotemeta (unnecessarily) replaces <space> with
      // <backslash><space>, but don't rely on that.
      if (val.indexOf("\\ ") >= 0) {
	val = StringUtil.replaceString(val, "\\ ", URL_ESCAPED_SPACE_REGEX);
      } else {
	// if no "\ ", either quotemeta didn't escape <space> or there
	// are none
	val = StringUtil.replaceString(val, " ", URL_ESCAPED_SPACE_REGEX);
      }
      return val;
    }
  }

  public static UrlListConverter newUrlListConverter(ArchivalUnit au) {
    return new UrlListConverter(au);
  }

  public static UrlListConverter newUrlListConverter(Plugin plugin,
						     TypedEntryMap paramMap) {
    return new UrlListConverter(plugin, paramMap);
  }

  public static class UrlListConverter extends PrintfConverter {
    boolean haveSets = false;

    private UrlListConverter(ArchivalUnit au) {
      super(au);
    }

    private UrlListConverter(Plugin plugin, TypedEntryMap paramMap) {
      super(plugin, paramMap);
    }

    void haveSets() {
      if (!haveSets) {
	// if this is first set seen, replace all values so far with
	// singleton list of value
	for (int ix = 0; ix < substitute_args.size(); ix++) {
	  substitute_args.set(ix,
			      Collections.singletonList(substitute_args.get(ix)));
	}
	haveSets = true;
      }
    }

    void interpSetArg(String key, Object val, ConfigParamDescr descr) {
      haveSets();
      // val must be a list; ok to throw if not
      List<String> vec = (List<String>)val;
      substitute_args.add(vec);
    }

    void interpRangeArg(String key, Object val, ConfigParamDescr descr) {
      throw new PluginException.InvalidDefinition("String range params are not allowed in URL patterns: " + key);
    }

    void interpNumRangeArg(String key, Object val, ConfigParamDescr descr) {
      haveSets();
      // val must be a list; ok to throw if not
      List<Long> vec = (List<Long>)val;
      long min = vec.get(0).longValue();
      long max = vec.get(1).longValue();
      long size = max - min + 1;
      if (size > MAX_NUM_RANGE_SIZE) {
	log.error("Excessively large numeric range: " + min + "-" + max
		  + ", truncating");
	max = min + MAX_NUM_RANGE_SIZE;
	size = max - min + 1;
      }
      List lst = new ArrayList((int)size);
      for (long x = min; x <= max; x++) {
	lst.add(x);
      }
      substitute_args.add(lst);
    }

    void interpPlainArg(String key, Object val, ConfigParamDescr descr) {
      if (haveSets) {
	substitute_args.add(Collections.singletonList(val));
      } else {
	substitute_args.add(val);
      }
    }

    public List<String> getUrlList(String printfString) {
      convert(printfString);
      if (missingArgs) {
	log.warning("Missing variable arguments: " + p_data);
	return null;
      }
      if (!substitute_args.isEmpty() && haveSets) {
	ArrayList<String> res = new ArrayList<String>();
	for (CartesianProductIterator iter =
	       new CartesianProductIterator(substitute_args);
	     iter.hasNext(); ) {
	  Object[] oneCombo = (Object[])iter.next();
	  if (log.isDebug3()) {
	    log.debug3("sprintf(\""+format+"\", "
		       + StringUtil.separatedString(oneCombo, ", ")+")");
	  }
	  try {
	    res.add(pf.sprintf(oneCombo));
	  } catch (Exception e) {
	    throw new PluginException.InvalidDefinition(format, e);
	  }
	}
	res.trimToSize();
	return res;
      } else {
	if (log.isDebug3()) {
	  log.debug3("sprintf(\""+format+"\", "+substitute_args+")");
	}
	try {
	  return
	    Collections.singletonList(pf.sprintf(substitute_args.toArray()));
	} catch (Exception e) {
	  throw new PluginException.InvalidDefinition(e);
	}
      }
    }
  }

  public static NameConverter newNameConverter(ArchivalUnit au) {
    return new NameConverter(au);
  }

  public static NameConverter newNameConverter(Plugin plugin,
					       TypedEntryMap paramMap) {
    return new NameConverter(plugin, paramMap);
  }

  public static class NameConverter extends PrintfConverter {

    private NameConverter(ArchivalUnit au) {
      super(au);
    }

    private NameConverter(Plugin plugin, TypedEntryMap paramMap) {
      super(plugin, paramMap);
    }

    void interpSetArg(String key, Object val, ConfigParamDescr descr) {
      // val must be a list; ok to throw if not
      List<String> vec = (List<String>)val;
      substitute_args.add(StringUtil.separatedString(vec, ", "));
    }

    void interpRangeArg(String key, Object val, ConfigParamDescr descr) {
      // val must be a list; ok to throw if not
      List<String> vec = (List<String>)val;
      substitute_args.add(StringUtil.separatedString(vec, "-"));
    }

    void interpNumRangeArg(String key, Object val, ConfigParamDescr descr) {
      interpRangeArg(key, val, descr);
    }

    void interpPlainArg(String key, Object val, ConfigParamDescr descr) {
      substitute_args.add(val);
    }

    void interpNullArg(String key, Object val, ConfigParamDescr descr) {
      substitute_args.add("(null)");
    }

    public String getName(String printfString) {
      convert(printfString);
      if (missingArgs) {
	log.warning("Missing variable arguments: " + p_data);
      }
      if (log.isDebug3()) {
	log.debug3("sprintf(\""+format+"\", "+substitute_args+")");
      }
      try {
	return pf.sprintf(substitute_args.toArray());
      } catch (Exception e) {
	throw new PluginException.InvalidDefinition(e);
      }
    }
  }

  public static class MatchPattern {
    String regexp;
    List<List> matchArgs;
    List<ConfigParamDescr> matchArgDescrs;

    MatchPattern() {
    }

    MatchPattern(String regexp,
		 List<List> matchArgs,
		 List<ConfigParamDescr> matchArgDescrs) {
      this.regexp = regexp;
      this.matchArgs = matchArgs;
      this.matchArgDescrs = matchArgDescrs;
    }

    public String getRegexp() {
      return regexp;
    }

    public List<List> getMatchArgs() {
      return matchArgs;
    }

    public List<ConfigParamDescr> getMatchArgDescrs() {
      return matchArgDescrs;
    }
  }
}
