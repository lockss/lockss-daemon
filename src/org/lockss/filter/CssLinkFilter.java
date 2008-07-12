/*
 * $Id: CssLinkFilter.java,v 1.1 2008-07-12 08:36:02 dshr Exp $
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

package org.lockss.filter;

import java.io.*;
import java.util.List;
import org.lockss.config.*;
import org.lockss.util.*;


/** Filter that works off an array of triples. First element
 *  is a string to match exactly,  Second is a regex to apply to
 *  test following the match,  and third is a replacement for the
 *  regex if it matches, Tricky issue is that we know the
 *  length of str,  but we don't know the length of the
 *  match, or of the result of the replacement. In the CSS
 *  application we know that the length of the match will be
 *  less than the length of the replacement,  and the length
 *  of the result will be less than the length of the initial
 *  tag plus the length of the regex plus the length of the
 *  replacement.
 */
public class CssLinkFilter extends StringFilter {

    private static Logger logger = Logger.getLogger("CssLinkFilter");
    protected String regexStr;
    protected int leftOver = 0;

    public CssLinkFilter(Reader reader, int bufferCapacity,
			 String origStr, String regexStr,
			 String replaceStr) {
	super(reader, (bufferCapacity < (origStr.length() +
					 regexStr.length() +
					 replaceStr.length()) ?
		       (origStr.length() +
			regexStr.length() +
			replaceStr.length()) : bufferCapacity),
	      origStr, replaceStr);
	if (regexStr == null) {
	    throw new IllegalArgumentException("Called with a null regex");
	}
	this.regexStr = regexStr;
    }

    /**
     * Factory method for a series of nested CssLinkFilters,
     * with an array of tag strings, regex strings and
     * their replacements
     * @param reader the source Reader
     * @param strArray 2-dimensional array of tags, regex and replacements
     * @param ignoreCase set for all the filters
     * @return the nested CssLinkFilter
     */
    public static CssLinkFilter makeNestedFilter(Reader reader,
						 String[][] strArray,
						 boolean ignoreCase) {
	if (reader == null) {
	    throw new IllegalArgumentException("Called with a null Reader");
	} else if (strArray == null) {
	    throw new IllegalArgumentException("Called with a null List");
	} else if ((strArray.length <= 0) || (strArray[0].length <= 0)) {
	    throw new IllegalArgumentException("Called with a empty array");
	}
	Reader curReader = reader;
	for (int ix = 0; ix < strArray.length; ix++) {
	    if (strArray[ix].length < 3) {
		throw new IllegalArgumentException("Array[" + ix + "] too short");
	    }
	    String srcStr = strArray[ix][0];
	    String regexStr = strArray[ix][1];
	    String replaceStr = strArray[ix][2];
	    logger.debug3("src " + srcStr + " regex " + regexStr + " repl " +
			  replaceStr);
	    curReader = new CssLinkFilter(curReader, -1, srcStr,
					  regexStr, replaceStr);
	    ((CssLinkFilter)curReader).setIgnoreCase(ignoreCase);
	}
	return (CssLinkFilter)curReader;
    }

    /*
     * The next bufSize chars from the ring are moved to
     * offset off in outputBuf.
     */
    public int read(char[] outputBuf, int off, int bufSize) throws IOException {
	if (isTrace) logger.debug3("read(buf, " + off + ", " + bufSize + ")");
	if (isClosed) throw new IOException("Read from closed StringFilter");
	// bufPtrPlusFree is the index in outputBuf after the
	// moved chars
	int bufPtrPlusFree = off + bufSize;
	if ((off < 0) || (bufSize < 0) || (bufPtrPlusFree > outputBuf.length)) {
	    throw new IndexOutOfBoundsException();
	}
	// We know there's room for the chars in outputBuf.
	// bufFree counts how many more chars we need to move.
	int bufFree = bufSize;
	// leftOver counts how many more chars to move before
	// doing the regex replace
	while (bufFree > 0 && (!streamDone || (ringSize > 0))) {
	    // We need to move more chars and we have more to move.
	    // First make sure there are at least strlen chars in
	    // the ring.
	    logger.debug3("Head bufFree " + bufFree + " ringSize " + ringSize +
			  " done? " + streamDone + " leftOver " + leftOver);
	    if (ringSize < strlen && !streamDone) {
		refillRing();
	    }
	    logger.debug3("Ring: " + charBuffer.toString() + " :end\n");
	    // Are there chars left over?
	    if (leftOver > 0) {
		logger.debug3("leftOver " + leftOver);
		int ncopy = leftOver;
		if (ncopy > bufFree) {
		    leftOver = ncopy - bufFree;
		    ncopy = bufFree;
		} else {
		    leftOver = 0;
		}
		logger.debug3("move " + ncopy + " left over chars");
		charBuffer.remove(outputBuf, bufPtrPlusFree - bufFree, ncopy);
		ringSize -= ncopy;
		bufFree -= ncopy;
		if (leftOver == 0) {
		    logger.debug3("Match - ringSize " + ringSize + " bufFree " +
				  bufFree);
		    // There was a match.  The first char beyond the match
		    // is now the first char in the ring.  Refill it.
		    refillRing();
		    logger.debug3("Match - ringSize " + ringSize + " bufFree " +
				  bufFree);
		    // Do regex replace on the chars at the head of the ring.
		    regexReplace();
		}
		continue;
	    }
	    // Is there a match for str in the ring?
	    int idx = charBuffer.indexOf(str, bufFree, ignoreCase);
	    int ncopy;
	    if (idx < 0) {
		// No match, move enough chars to leave strlen-1
		int max = streamDone ? ringSize : (ringSize - (strlen - 1));
		ncopy = bufFree < max ? bufFree : max;
		logger.debug3("No match - " + regexStr + " ncopy " + ncopy);
		charBuffer.remove(outputBuf, bufPtrPlusFree - bufFree, ncopy);
		ringSize -= ncopy;
		bufFree -= ncopy;
	    } else {
		// There is a match.  Move the chars as far as the
		// end of the match, or at least as far as we can.
		ncopy = idx + strlen;
		if (ncopy > bufFree) {
		    leftOver = ncopy - bufFree;
		    ncopy = bufFree;
		}
		logger.debug3("Match - " + regexStr + " ncopy " + ncopy);
		charBuffer.remove(outputBuf, bufPtrPlusFree - bufFree, ncopy);
		ringSize -= ncopy;
		bufFree -= ncopy;
	    }
	    if (idx >= 0 && leftOver == 0) {
		logger.debug3("Match - ringSize " + ringSize + " bufFree " +
			      bufFree);
		// There was a match.  The first char beyond the match
		// is now the first char in the ring.  Refill it.
		refillRing();
		logger.debug3("Match - ringSize " + ringSize + " bufFree " +
			      bufFree);
		// Do regex replace on the chars at the head of the ring.
		regexReplace();
	    }
	    logger.debug3("Tail bufFree " + bufFree + " ringSize " + ringSize +
			  " done? " + streamDone + " leftOver " + leftOver);
	}
	int numRead = bufSize - bufFree;
	return numRead == 0 ? -1 : numRead;
    }

    private void refillRing() throws IOException {
	if (!streamDone) {
	    streamDone = charBuffer.refillBuffer(reader);
	    ringSize = charBuffer.size();
	}
    }

    private void regexReplace() {
	String orig = charBuffer.toString();
	String replaced = orig.replaceFirst(regexStr, replaceStr);
	if (logger.isDebug3() && !orig.equals(replaced)) {
	    logger.debug3("Original " + orig + " :end\nReplaced " + replaced +
			  " :end\n");
	}
	int ringCapacity = charBuffer.capacity();
	charBuffer = new CharRing(ringCapacity > replaced.length() ?
				  ringCapacity : replaced.length());
	try {
	    charBuffer.add(replaced.toCharArray());
	} catch (CharRing.RingFullException ex) {
	    throw new IllegalArgumentException("CharRing full when shouldn't be");
	}
	ringSize = charBuffer.size();
    }
}
