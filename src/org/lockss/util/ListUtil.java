/*
 * $Id: ListUtil.java,v 1.1 2002-08-31 06:39:29 tal Exp $
 *

Copyright (c) 2000-2002 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.util;

import java.util.*;

/**
 * Create a list of Object from a call list. */
public class ListUtil {
    /**
     * Don't construct. */
    private ListUtil() {
    }
    
    /**
     * Create list from arg list. */
    public static List list() {
	return new ArrayList();
    }

    /**
     * Create list from arg list. */
    public static List list(Object object1) {
	List l = new ArrayList();
	l.add(object1);
	return l;
    }

    /**
     * Create list from arg list. */
    public static List list(Object object1, Object object2) {
	List l = list(object1);
	l.add(object2);
	return l;
    }

    /**
     * Create list from arg list. */
    public static List list(Object object1, 
			    Object object2,
			    Object object3) {
	List l = list(object1, object2);
	l.add(object3);
	return l;
    }

    /**
     * Create list from arg list. */
    public static List list(Object object1, 
			    Object object2,
			    Object object3,
			    Object object4) {
	List l = list(object1, object2, object3);
	l.add(object4);
	return l;
    }

    /**
     * Create list from arg list. */
    public static List list(Object object1, 
			    Object object2,
			    Object object3,
			    Object object4,
			    Object object5) {
	List l = list(object1, object2, object3, object4);
	l.add(object5);
	return l;
    }

    /**
     * Create list from arg list. */
    public static List list(Object object1, 
			    Object object2,
			    Object object3,
			    Object object4,
			    Object object5,
			    Object object6) {
	List l = list(object1, object2, object3, object4, object5);
	l.add(object6);
	return l;
    }
    /**
     * Create list from arg list. */
    public static List list(Object object1, 
			    Object object2,
			    Object object3,
			    Object object4,
			    Object object5,
			    Object object6,
			    Object object7) {
	List l = list(object1, object2, object3, object4, object5,
		      object6);
	l.add(object7);
	return l;
    }
    /**
     * Create list from arg list. */
    public static List list(Object object1, 
			    Object object2,
			    Object object3,
			    Object object4,
			    Object object5,
			    Object object6,
			    Object object7,
			    Object object8) {
	List l = list(object1, object2, object3, object4, object5,
		      object6, object7);
	l.add(object8);
	return l;
    }
    /**
     * Create list from arg list. */
    public static List list(Object object1, 
			    Object object2,
			    Object object3,
			    Object object4,
			    Object object5,
			    Object object6,
			    Object object7,
			    Object object8,
			    Object object9) {
	List l = list(object1, object2, object3, object4, object5,
		      object6, object7, object8);
	l.add(object9);
	return l;
    }
    /**
     * Create list from arg list. */
    public static List list(Object object1, 
			    Object object2,
			    Object object3,
			    Object object4,
			    Object object5,
			    Object object6,
			    Object object7,
			    Object object8,
			    Object object9,
			    Object object10) {
	List l = list(object1, object2, object3, object4, object5,
		      object6, object7, object8, object9);
	l.add(object10);
	return l;
    }

  /** Create a list containing the elements of an array */
    public static List fromArray(Object array[]) {
	List l = list();
	for (int i = 0; i < array.length; i++) {
	    l.add(array[i]);
	}
	return l;
    }

  /** Create a list containing the elements of a comma separated string */
    public static List fromCSV(String csv) {
	List res = list();
	StringTokenizer st = new StringTokenizer(csv, ",");
	while (st.hasMoreTokens()) {
	    String id = (String)st.nextToken();
	    res.add(id);
	}
	return res;
    }
}
