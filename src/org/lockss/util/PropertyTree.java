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

// This is essentially a copy of the old JettyTools PropertyTree, which
// we're still using, but is no longer a part of Jetty.

// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.lockss.util;

import java.io.*;
import java.util.*;

/* ------------------------------------------------------------ */
/** Property map with keys in tree hierarchy.
 * Extension of Properties to allow nesting of Properties and default values
 * This class extends Properties and uses a "." separated notation to allow
 * nesting of property values. Property keys such as "a.b" and "a.c" cat be
 * retrieved (and set) as normal, but it is possible to retrieve a
 * PropertyTree by get("a"), and then use get("b") and get("c") to achieve
 * the same thing (although most times this is unnecessary). This makes it
 * easy to have nested sets of values in the same properties file and iterate
 * over nested keys.
 *
 * Wildcard nodes can be defined with "*" so that keys such as
 * "aa.*.cc", will match gets such as "aa.bb.cc", "aa.X.cc", etc.
 *
 * Values can contain tokens such as %name%, which are expanded
 * as with the results of a call to System.getProperty("name");
 * The % character may be included in a value with %%.
 *
 * To aid in constructing and saving Properties files,
 * <code>getConverter</code> will convert Dictionaries into PropertyTrees
 * recursively.
 */
public class PropertyTree extends Properties
{
  /* ------------------------------------------------------------ */
  class Node extends Hashtable
  {
    Node() {super(13);}
    public String key=null;
    public String toString()
    {
      return  "["+key+":"+super.toString()+"]";
    }
  }

  /* ------------------------------------------------------------ */
  private Node rootNode=new Node();
  private String prefix=null;
  private PropertyTree parent=null;
  private boolean trim=true;

  /* ------------------------------------------------------------ */
  /** Constructor.
   * Equivalent to PropertyTree(true);
   */
  public PropertyTree()
  {}

  /* ------------------------------------------------------------ */
  /** Constructor.
   * @param trimLoadValues If true, all values are trimmed during loads.
   */
  public PropertyTree(boolean trimLoadValues)
  {
    trim=trimLoadValues;
  }

  /* ------------------------------------------------------------ */
  /** Construct from Properties
   * @param properties
   */
  public PropertyTree(Properties properties)
  {
    load(properties);
  }

  /* ------------------------------------------------------------ */
  public void load(InputStream in)
      throws IOException
  {
    Properties parser = new Properties();
    parser.load(in);
    load(parser);
  }

  /* ------------------------------------------------------------ */
  public void load(Properties properties)
  {
    Enumeration e=properties.keys();
    while (e.hasMoreElements())
      {
	Object k=e.nextElement();
	String v=(String)properties.get(k);
	v=expandMacros(v);
	put(k,trim?v.trim():v);
      }
  }

  /* ------------------------------------------------------------ */
  /** Override Hashtable.get() */
  public synchronized Object get(Object key)
  {
    Object realKey=key;
    Object value=super.get(realKey);
    if (value==null)
      {
	realKey=getTokenKey(key.toString());
	if (realKey!=null)
	  value=super.get(realKey);
      }

    return value;
  }

  /* ------------------------------------------------------------ */
  /** Override Properties.getProperty() */
  public String getProperty(String key)
  {
    return (String)get(key);
  }

  /* ------------------------------------------------------------ */
  /** Override Hashtable.put() */
  public synchronized Object put(Object key, Object value)
  {
    String keyStr=key.toString();
    putTokenKey(keyStr,keyStr);
    Object v=null;
    if (parent!=null)
      v=parent.put(parentKey((String)key),value);

    if (key instanceof String) {
      return super.put(intern((String)key),value);
    } else {
      return super.put(key,value);
    }
  }

  /* ------------------------------------------------------------ */
  public Object setProperty(String key,String value)
  {
    return (String)put(key,value);
  }

  /* ------------------------------------------------------------ */
  /** Override Hashtable.remove() */
  public synchronized Object remove(Object key)
  {
    if (parent!=null)
      parent.remove(parentKey((String)key));

    Object value=super.get(key);
    if (value!=null)
      {
	putTokenKey(key.toString(),null);
	return super.remove(key);
      }

    String realKey=getTokenKey(key.toString());
    if (realKey!=null)
      {
	putTokenKey(realKey,null);
	return super.remove(realKey);
      }
    return null;
  }

  /* ------------------------------------------------------------ */
  /** From Properties
   * @deprecated
   */
  public synchronized void save(OutputStream out,String header)
  {
    PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));
    writer.print("# ");
    writer.println(header);
    list(writer);
  }

  /* ------------------------------------------------------------ */
  /** Return a sub tree of the PropertyTree.
   * Changes made in the sub tree are reflected in the original tree,
   * unless the sub tree is cloned.
   * @param key The name of the sub node
   * @return null if none.
   */
  public PropertyTree getTree(String key)
  {
    if (prefix!=null && prefix.endsWith("*.") && key.startsWith("*"))
      return this;
    return new PropertyTree(this,key);
  }

  /* ------------------------------------------------------------ */
  public Enumeration propertyNames()
  {
    return keys();
  }

  /* ------------------------------------------------------------ */
  public Enumeration elements()
  {
    final Enumeration keys=keys();
    return new Enumeration()
      {
	public boolean hasMoreElements(){
	  return keys.hasMoreElements();
	}
	public Object nextElement(){
	  return get(keys.nextElement());
	}
      };
  }


  /* ------------------------------------------------------------ */
  /** Enumerate top level tree node names.
   * @return Enumeration of tree node names.
   */
  public Enumeration getNodes()
  {
    return getNodes("");
  }

  /* ------------------------------------------------------------ */
  /** Enumerate tree node names below given node.
   * @param key Key of the node.
   * @return Enumeration of tree node names.
   */
  public Enumeration getNodes(String key)
  {
    Vector tokens=getTokens(key);
    Node node=rootNode;
    int index=0;
    while(index<tokens.size())
      {
	Node subNode = (Node)node.get(tokens.elementAt(index));
	if (subNode==null)
	  return null;
	node=subNode;
	index++;
      }
    return node.keys();
  }


  /* ------------------------------------------------------------ */
  /** Enumerate non wild tree node names below given node.
   * @return Enumeration of tree node names.
   */
//   public Enumeration getRealNodes()
//   {
//     return getRealNodes("");
//   }

  /* ------------------------------------------------------------ */
  /** Enumerate non wild tree node names below given node.
   * @param key Key of the node.
   * @return Enumeration of tree node names.
   */
//   public Enumeration getRealNodes(String key)
//   {
//     if (!key.endsWith(".") && key.length()>0)
//       key+=".";

//     // find the root tree.
//     PropertyTree tree=this;
//     while (tree.parent!=null)
//       {
// 	key=tree.prefix+key;
// 	tree=tree.parent;
//       }

//     // find not wild keys
//     Hashtable keySet=new Hashtable(tree.size()*2);
//     Enumeration e= tree.keys();
//     while (e.hasMoreElements())
//       {
// 	String k=(String)e.nextElement();
// 	if (!k.startsWith(key))
// 	  continue;
// 	String s=k.substring(key.length());
// 	int d=s.indexOf(".");
// 	if (d>=0)
// 	  s=s.substring(0,d);
// 	keySet.put(s,s);
//       }

//     return keySet.keys();
//   }

  /* ------------------------------------------------------------ */
  /** Get Vector of values.
   * @param key the Value(s) to get.
   * @param separators String of separator charactors
   * @return Vector of values.
   */
  public Vector getVector(String key, String separators)
  {
    String values=getProperty(key);
    if (values==null)
      return null;

    Vector v=new Vector();
    StringTokenizer tok=new StringTokenizer(values,separators);
    while(tok.hasMoreTokens())
      v.addElement(tok.nextToken());
    return v;
  }

  /* ------------------------------------------------------------ */
  /** Get List of values.
   * @param key the Value(s) to get.
   * @param separators String of separator charactors
   * @return List of values.
   */
  public List getList(String key, String separators)
  {
    String values=getProperty(key);
    if (values==null)
      return null;

    List l=new ArrayList(9);
    StringTokenizer tok=new StringTokenizer(values,separators);
    while(tok.hasMoreTokens())
      l.add(tok.nextToken());
    return l;
  }

  /* ------------------------------------------------------------ */
  public boolean getBoolean(String key)
  {
    String value=getProperty(key);
    if (value==null || value.length()==0)
      return false;

    return "1tTyYoO".indexOf(value.charAt(0))>=0;
  }

  /* ------------------------------------------------------------ */
  public boolean getBoolean(String key, boolean inDefault)
  {
    String value=getProperty(key);
    if (value==null || value.length()==0)
      return inDefault;
    return "1tTyYoO".indexOf(value.charAt(0))>=0;
  }


  /* ------------------------------------------------------------ */
  public Object clone()
  {
    PropertyTree pt = new PropertyTree();
    Enumeration e = keys();
    while(e.hasMoreElements())
      {
	String k = (String)e.nextElement();
	pt.put(k,get(k));
      }
    return pt;
  }

  /* ------------------------------------------------------------ */
  private void putTokenKey(String key,String tokenKey)
  {
    Vector tokens=getTokens(key);
    Node node=rootNode;
    int index=0;
    while(index<tokens.size())
      {
	Node subNode = (Node)node.get(tokens.elementAt(index));
	if (subNode==null)
	  {
	    subNode=new Node();
	    node.put(intern((String)tokens.elementAt(index)),subNode);
	  }
	node=subNode;
	index++;
      }
    node.key=tokenKey;
  }

  /* ------------------------------------------------------------ */
  private String intern(String key)
  {
//     return key;
    return StringPool.PROPERTY_TREE.intern(key);
  }

  /* ------------------------------------------------------------ */
  private String getTokenKey(String key)
  {
    Vector tokens=getTokens(key);
    String tokenKey=getTokenKey(rootNode,tokens,0);
    return tokenKey;
  }

  /* ------------------------------------------------------------ */
  private String getTokenKey(Node node, Vector tokens, int index)
  {
    String key=null;
    if (tokens.size()==index)
      key=node.key;
    else
      {
	// expand named nodes
	Node subNode=(Node)node.get(tokens.elementAt(index));
	if (subNode!=null)
	  key=getTokenKey(subNode,tokens,index+1);

	// if no key, try wild expansions
	if (key==null)
	  {
	    subNode=(Node)node.get("*");
	    if (subNode!=null)
	      key=getTokenKey(subNode,tokens,index+1);
	  }

      }
    return key;
  }

  /* ------------------------------------------------------------ */
  private String parentKey(String key)
  {
    return prefix+key;
  }

  /* ------------------------------------------------------------ */
  /** Turn the key into a list of tokens */
  private static Vector getTokens(String key)
  {
    if (key != null)
      {
	Vector v = new Vector(10);
	StringTokenizer tokens = new StringTokenizer(key.toString(), ".");
	while (tokens.hasMoreTokens())
	  v.addElement(tokens.nextToken());
	return v;
      }
    return null;
  }
  /* ------------------------------------------------------------ */
  private PropertyTree(PropertyTree parent,String node)
  {
    this.prefix=node+".";
    Vector tokens=getTokens(node);
    Hashtable keyMap = new Hashtable(parent.size()+13);
    findKeys(keyMap,parent.rootNode,tokens,0,null);
    Enumeration e=keyMap.keys();
    while(e.hasMoreElements())
      {
	String subKey=(String)e.nextElement();
	String key=(String)keyMap.get(subKey);
	put(subKey,parent.get(key));
      }
//     this.parent=parent;
  }

  /* ------------------------------------------------------------ */
  private void findKeys(Hashtable keyMap,
			Node node,
			Vector tokens,
			int index,
			String key)
  {
    // Is this a match?
    if (tokens.size()==index)
      expandNode(keyMap,node,key.length()+1);
    else
      {
	// expand named nodes
	Node subNode=(Node)node.get(tokens.elementAt(index));
	if (subNode!=null)
	  findKeys(keyMap,subNode,tokens,index+1,
		   (key==null)
		   ?((String)tokens.elementAt(index))
		   :(key+"."+tokens.elementAt(index)) );

	// expand wild cards
	subNode=(Node)node.get("*");
	if (subNode!=null)
	  findKeys(keyMap,subNode,tokens,index+1,
		   (key==null)?"*":(key+".*") );
      }
  }

  /* ------------------------------------------------------------ */
  private void expandNode(Hashtable keyMap, Node node, int keyLength)
  {
    Enumeration e=node.elements();
    while(e.hasMoreElements())
      {
	Node n = (Node)e.nextElement();
	if (n.key!=null)
	  {
	    String subKey=n.key.substring(keyLength);
	    if (!keyMap.containsKey(subKey))
	      keyMap.put(subKey,n.key);
	  }
	expandNode(keyMap,n,keyLength);
      }
  }

  /* ------------------------------------------------------------ */
  static private String expandMacros(String v)
  {
    // do limited substitution on values
    for (int i=0; i<v.length();)
      {
	int i1 = v.indexOf('%',i);
	if (i1 < 0) break;
	int i2 = v.indexOf('%',i1+1);
	if (i2 < 0) break;
	i = i2 + 1;

	String sk = v.substring(i1+1,i2);
	String sv = sk.length()==0?"%":System.getProperty(sk);
	if (null == sv) continue;
	i = i1 + sv.length();
	StringBuffer b = new StringBuffer();
	if (0 < i1)
	  b.append(v.substring(0,i1));
	b.append(sv);
	if ((i2+1) < v.length())
	  b.append(v.substring(i2+1));

	v = b.toString();
      }
    return v;
  }
}
