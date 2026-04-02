/*

Copyright (c) 2000-2026, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.ijournalpro.univofanbar;


import org.lockss.extractor.JsoupHtmlLinkExtractor;
import org.lockss.extractor.LinkExtractor;
import org.lockss.extractor.LinkExtractorFactory;

/*
The publisher will not provide volume/year based manifest page, so we need to extract issues from html source of
https://ajas.uoanbar.edu.iq/browse?_action=issue for each volume
 */

/*
<section id="contentList" class="issueList">
	<div class="page-header">
	   <h1>By Issue</h1>
	</div>
	<hr>
	<div style="margin-bottom: 25px">Click on favorite issue to see related articles.</div>
	<article>
	   <div class="title">
		  <h3 class="pull-left">Volume 23 (2025)</h3>
		  <i class="pull-right fa fa-angle-down"></i>
		  <div class="clearfix"></div>
	   </div>
	   <div class="content">
		  <div class="issueInfo">
			 <img src="data/aagrs/coversheet/731767567945.jpeg" class="img-responsive pull-left">
			 <div class="issueDetail pull-left">
				<h4><a href="issue_15363_15506.html"> Issue 2</a></h4>
				<small></small>
			 </div>
			 <div class="clearfix"></div>
		  </div>
		  <div class="issueInfo">
			 <img src="data/aagrs/coversheet/561758229391.jpeg" class="img-responsive pull-left">
			 <div class="issueDetail pull-left">
				<h4><a href="issue_15363_15364.html"> Issue 1</a></h4>
				<small></small>
			 </div>
			 <div class="clearfix"></div>
		  </div>
	   </div>
	</article>
	...
</<section>
 */



public class UnivofAnbarCollegeofAgHtmlLinkExtractorFactory implements LinkExtractorFactory {

		@Override
		public LinkExtractor createLinkExtractor(String mimeType) {
			LinkExtractor defaultExtractor = new JsoupHtmlLinkExtractor();
			return new UnivofAnbarCollegeofAgVolumeIssueLinkExtractor(defaultExtractor);
		}

}