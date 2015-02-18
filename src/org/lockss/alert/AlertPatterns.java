/*
 * $Id$
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.alert;

import java.util.*;
import java.util.regex.*;

import org.lockss.util.*;

/** AlertPatterns is a collection of useful {@link AlertPattern}
 * implementations, and factories for creating them */
public class AlertPatterns {
  private static final Logger log = Logger.getLogger(AlertPatterns.class);

  private static final AlertPattern TRUE = new True();
  private static final AlertPattern FALSE = new False();

  /** Return an AlertPattern that matches every Alert */
  public static AlertPattern True() {
    return TRUE;
  }

  /** Return an AlertPattern that does not matche any Alert */
  public static AlertPattern False() {
    return FALSE;
  }

  /** Return an AlertPattern that matches if all its constituents match */
  public static AlertPattern And(List patterns) {
    return new And(patterns);
  }

  /** Return an AlertPattern that matches if any of its constituents match */
  public static AlertPattern Or(List patterns) {
    return new Or(patterns);
  }

  /** Return an AlertPattern that matches if its constituent does not match */
  public static AlertPattern Not(AlertPattern pattern) {
    return new Not(pattern);
  }

  /** Return a predicate relating the value of an alert attribute to a
   * fixed value.  relation should be one of <code>Predicate.EQ, NE, GT,
   * GE, LT, LE</code>. */
  public static AlertPattern Predicate(String attribute,
				       Predicate.REL relation, Object value) {
    return new Predicate(attribute, relation, value);
  }

  /** Return a predicate that is true if the value of the alert's
   * <code>attribute</code> attribute is equal to <code>value</code> */
  public static AlertPattern EQ(String attribute, Object value) {
    return new Predicate(attribute, Predicate.REL.EQ, value);
  }

  /** Return a predicate that is true if the value of the alert's
   * <code>attribute</code> attribute is not equal to <code>value</code> */
  public static AlertPattern NE(String attribute, Object value) {
    return new Predicate(attribute, Predicate.REL.NE, value);
  }

  /** Return a predicate that is true if the value of the alert's
   * <code>attribute</code> attribute is greater than <code>value</code> */
  public static AlertPattern GT(String attribute, Comparable value) {
    return new Predicate(attribute, Predicate.REL.GT, value);
  }

  /** Return a predicate that is true if the value of the alert's
   * <code>attribute</code> attribute is greater than or equal to
   * <code>value</code> */
  public static AlertPattern GE(String attribute, Comparable value) {
    return new Predicate(attribute, Predicate.REL.GE, value);
  }

  /** Return a predicate that is true if the value of the alert's
   * <code>attribute</code> attribute is less than <code>value</code> */
  public static AlertPattern LT(String attribute, Comparable value) {
    return new Predicate(attribute, Predicate.REL.LT, value);
  }

  /** Return a predicate that is true if the value of the alert's
   * <code>attribute</code> attribute is less than or equal to
   * <code>value</code> */
  public static AlertPattern LE(String attribute, Comparable value) {
    return new Predicate(attribute, Predicate.REL.LE, value);
  }

  /** Return a predicate that is true if the value of the alert's
   * <code>attribute</code> is contained in <code>value</code> */
  public static AlertPattern CONTAINS(String attribute, Object value) {
    return new Predicate(attribute, Predicate.REL.CONTAINS, value);
  }

  /** Return a predicate that is true if the value of the alert's
   * <code>attribute</code> matches the <code>regexp</code> */
  public static AlertPattern MATCHES(String attribute, String regexp) {
    return new Predicate(attribute, Predicate.REL.MATCHES, regexp);
  }

  public static class True implements AlertPattern, LockssSerializable {
    public boolean isMatch(Alert alert) {
      return true;
    }

    public boolean equals(Object o) {
      return o instanceof True;
    }
  }

  public static class False implements AlertPattern, LockssSerializable {
    public boolean isMatch(Alert alert) {
      return false;
    }

    public boolean equals(Object o) {
      return o instanceof False;
    }
  }

  public abstract static class NAry
    implements AlertPattern, LockssSerializable {

    protected List<AlertPattern> patterns;

    public NAry(List<AlertPattern> patterns) {
      this.patterns =
	ListUtil.immutableListOfType(patterns, AlertPattern.class);
    }

    public List<AlertPattern> getPatterns() {
      return patterns;
    }

    public void setPatterns(List<AlertPattern> patterns) {
      this.patterns = patterns;
    }

    public abstract boolean isMatch(Alert alert);

    public boolean equals(Object o) {
      if (o instanceof NAry) {
	NAry n = (NAry)o;
	return patterns.equals(n.patterns);
      }
      return false;
    }
  }

  public static class And extends NAry {

    public And(List<AlertPattern> patterns) {
      super(patterns);
    }

    public boolean isMatch(Alert alert) {
      for (AlertPattern pat : patterns) {
        if (!pat.isMatch(alert)) {
          return false;
        }
      }
      return true;
    }

    public String toString() {
      return "[AlertPatterns.And: " + patterns + "]";
    }
  }

  public static class Or extends NAry {

    public Or(List<AlertPattern> patterns) {
      super(patterns);
    }

    public boolean isMatch(Alert alert) {
      for (AlertPattern pat : patterns) {
        if (pat.isMatch(alert)) {
          return true;
        }
      }
      return false;
    }

    public String toString() {
      return "[AlertPatterns.Or: " + patterns + "]";
    }
  }

  public static class Not implements AlertPattern, LockssSerializable {
    AlertPattern pattern;

    public Not(AlertPattern pattern) {
      this.pattern = pattern;
    }

    public AlertPattern getPattern() {
      return pattern;
    }

    public void setPattern(AlertPattern pattern) {
      this.pattern = pattern;
    }

    public boolean isMatch(Alert alert) {
      return !pattern.isMatch(alert);
    }

    public boolean equals(Object o) {
      if (o instanceof Not) {
	Not n = (Not)o;
	return pattern.equals(n.pattern);
      }
      return false;
    }

    public String toString() {
      return "[AlertPatterns.Not: " + pattern + "]";
    }
  }

  public static class Predicate implements AlertPattern, LockssSerializable {
    static enum REL {EQ, NE, GT, GE, LT, LE, CONTAINS, MATCHES};

    private String attribute;
    private REL relation;
    private Object value;

    // used only for REL.MATCHES
    transient Pattern pat;

    // do NOT add any logic here - deserialization doesn't run constructor
    public Predicate(String attribute, REL relation, Object value)
	throws IllegalArgumentException {
      this.attribute = attribute;
      this.relation = relation;
      this.value = value;

      switch (relation) {
      case MATCHES:
	// This is here only to check the pattern when the constructor is
	// run.  Deserialized instances may still have an invalid pattern.
	try {
	  Pattern.compile((String)value);
	} catch (PatternSyntaxException e) {
	  // don't throw regex package-specific exception
	  throw new IllegalArgumentException(e);
	}
	break;
      }

    }

    public String getAttribute() {
      return attribute;
    }

    public void setAttribute(String attribute) {
      this.attribute = attribute;
    }

    public REL getRelation() {
      return relation;
    }

    public void setRelation(REL relation) {
      this.relation = relation;
    }

    public Object getValue() {
      return value;
    }

    public void setValue(Object value) {
      this.value = value;
    }


    public boolean isMatch(Alert alert) {
      try {
	Object attr = alert.getAttribute(attribute);
	switch (relation) {
	case EQ: return equalObjects(attr, value);
	case NE: return !equalObjects(attr, value);
	case CONTAINS: {
	  if (value instanceof Collection) {
	    return ((Collection)value).contains(attr);
	  }
	  return false;
	}
	case MATCHES:
	  Matcher m = getPattern().matcher((String)attr);
	  return m.find();
	default:
	  int cmp = ((Comparable)attr).compareTo(value);
	  switch (relation) {
	  case GT: return cmp > 0;
	  case GE: return cmp >= 0;
	  case LT: return cmp < 0;
	  case LE: return cmp <= 0;
	  default: return false;
	  }
	}
      } catch (Exception e) {
	log.warning("AlertPredicate.isMatch", e);
	return false;
      }
    }

    private Pattern getPattern() {
      if (pat == null) {
	try {
	  pat = Pattern.compile((String)value);
	} catch (PatternSyntaxException e) {
	  log.error("Illegal Alert pattern: " + (String)value, e);
	  pat = Pattern.compile("UnlikelyToMatchAnything");
	} catch (ClassCastException e) {
	  log.error("Illegal Alert pattern; value must be a String: "
		    + value.getClass());
	  pat = Pattern.compile("UnlikelyToMatchAnything");
	}
      }
      return pat;
    }

    private static boolean equalObjects(Object a, Object b) {
      if (a == null) return b == null;
      return a.equals(b);
    }

    public boolean equals(Object o) {
      if (o instanceof Predicate) {
	Predicate pred = (Predicate)o;
	return relation == pred.relation
	  && equalObjects(attribute, pred.attribute)
	  && equalObjects(value, pred.value);
      }
      return false;
    }

    public String toString() {
      return "[AlertPatterns.Pred: " + attribute + " " + relation +
	" " + value + "]";
    }
  }

}
