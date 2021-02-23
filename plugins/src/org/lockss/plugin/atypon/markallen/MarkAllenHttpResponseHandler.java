/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

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
      Pattern.compile( "(" + DEFAULT_NON_FATAL_500_PAT.pattern() + ")|" + MAG_ADD_ON); 
    
  
  // Use a getter so that this can be overridden by a child plugin
  protected Pattern getNonFatal500Pattern() {    
    return MAG_NON_FATAL_PAT;   
  }
  
}