/*
 * $Id:$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.atypon.markallen;

import java.util.regex.Pattern;

import org.lockss.plugin.atypon.BaseAtyponHttpResponseHandler;


public class MarkAllenHttpResponseHandler extends BaseAtyponHttpResponseHandler {
  
  

  //
  // Mark Allen Group provides a CDP "Continuing Professional Development" document
  // at the top of issue TOCs. In many cases it seems to be only in PDF, but there is a link
  // generated for the html which isn't there. This returns a 500 instead of a 404
  // so add this to the default Atypon pattern
  
  // see: http://www.magonlinelibrary.com/doi/abs/10.12968/johv.2013.1.9.CPD1
  //   from:    http://www.magonlinelibrary.com/toc/johv/1/9
  //
  private static final String MAG_ADD_ON = "doi/abs/[0-9.]+/[^/]+\\.CPD1$";
  // add parens to be sure to have the or work as desired
  protected static final Pattern MAG_NON_FATAL_PAT = 
      Pattern.compile( "(" + DEFAULT_NON_FATAL_PAT.pattern() + ")|" + MAG_ADD_ON); 
    
  
  // Use a getter so that this can be overridden by a child plugin
  protected Pattern getNonFatal500Pattern() {    
    return MAG_NON_FATAL_PAT;   
  }
  
}