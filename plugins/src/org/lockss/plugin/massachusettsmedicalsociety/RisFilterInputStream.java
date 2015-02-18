/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.massachusettsmedicalsociety;

/* 
 * Using a local version of RisFilterInputStream until the org.lockss.filter version is released
 * (probably 1.63).  Until then you cannot include org.lockss.filter or there will be ambiguity.
 * Once it's available, remove this version
 */
import java.io.*;
import java.util.*;

import org.lockss.util.*;

/*
 * Reads in an RIS file line by line removing any lines that start with any substring on the filterList
 */
public class RisFilterInputStream extends InputStream {
	private static Logger log = Logger.getLogger("RisFilter");
	private ArrayList<String> filterList = new ArrayList<String>();
	private InputStream out;
	private InputStream in;
	private String encoding;
	
	public RisFilterInputStream(InputStream in, String encoding, String filter){
		this(in, encoding);
		filterList.add(filter);
	}
	
	public RisFilterInputStream(InputStream in, String encoding, ArrayList<String> list){
		this(in, encoding);
		filterList = list;
	}
	
	private RisFilterInputStream(InputStream in, String encoding){
		this.in = in;
		this.encoding = encoding;
	}
	
	
	private void filter() throws IOException {
		BufferedReader bReader;
		StringBuilder sb = new StringBuilder();
		bReader = new BufferedReader(new InputStreamReader(in, encoding));
		String line = bReader.readLine();
		String nextLine;
		while(line != null) {
			nextLine = bReader.readLine();
			if(!removeLine(line)){
				sb.append(line);
			}
			if(nextLine != null && !removeLine(nextLine)){
				sb.append("\n");
			}
			line = nextLine;
		}
		out = new ReaderInputStream(new StringReader(sb.toString()), encoding);
	}
	
	private Boolean removeLine(String line) {
		for(String filter: filterList) {
			if(line.trim().startsWith(filter)){
				return true;
			}
		}
		return false;
	}
	
	public String getEncoding() {
		return this.encoding;
	}
	
	public ArrayList<String> getFilterList() {
		return this.filterList;
	}


	  InputStream getOut() throws IOException {
	    if (in == null) {
	      throw new IOException("Attempting to read from a closed InputStream");
	    }
	    if (out == null) {
	      filter();
	    }
	    return out;
	  }
	
	  public int read() throws IOException {
	    return getOut().read();
	  }
	  public int read(byte b[]) throws IOException {
	    return read(b, 0, b.length);
	  }
	  public int read(byte b[], int off, int len) throws IOException {
	    return getOut().read(b, off, len);
	  }
	  public long skip(long n) throws IOException {
	    return getOut().skip(n);
	  }
	  public int available() throws IOException {
	    return getOut().available();
	  }
	  public void mark(int readlimit) {
	    try {
	      getOut().mark(readlimit);
	    } catch (IOException e) {
	      throw new RuntimeException("", e);
	    }
	  }
	  public void reset() throws IOException {
	    getOut().reset();
	  }
	  public boolean markSupported() {
	    try {
	      return getOut().markSupported();
	    } catch (IOException e) {
	      throw new RuntimeException("", e);
	    }
	  }
	
	  public void close() throws IOException {
	    in.close();
	    in = null;
	  }
}
