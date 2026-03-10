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

package org.lockss.plugin.ijournalpro.kirkukuniv;


import org.lockss.extractor.JsoupHtmlLinkExtractor;
import org.lockss.extractor.LinkExtractor;
import org.lockss.extractor.LinkExtractorFactory;


/*
The publisher will not provide volume/year based manifest page, so we need to extract issues from html source of
https://kujss.uokirkuk.edu.iq/browse?_action=issue for each volume
 */

/*
<div class="toggle ">
 <label>Volume 20 (2025)</label>
 <div class="toggle-content">
	<div class="row">
	   <div class="item-box col-md-3 col-lg-2 col-sm-4 col-xs-12">
		  <figure>
			 <span class="item-hover">
				<span class="overlay dark-5"></span>
				<span class="inner">
				   <!-- lightbox -->
				   <a class="ico-rounded lightbox" href="data/kscis/coversheet/cover_en.jpg" data-plugin-options="{&quot;type&quot;:&quot;image&quot;}">
				   <span class="fa fa-search-plus size-18"></span>
				   </a>
				   <!-- details -->
				   <a class="ico-rounded" href="issue_15176_15451.html">
				   <span class="fa fa-link size-18"></span>
				   </a>
				</span>
			 </span>
			 <img class="img-responsive" src="data/kscis/coversheet/cover_en.jpg" alt="" height="600">
		  </figure>
		  <div class="item-box-desc padding-6" >
			 <h3 class="text-center"><a href="issue_15176_15451.html">Issue 4</a></h3>
			 <ul class="list-inline categories nomargin text-center">
			 </ul>
		  </div>
	   </div>
	   <div class="item-box col-md-3 col-lg-2 col-sm-4 col-xs-12">
		  <figure>
			 <span class="item-hover">
				<span class="overlay dark-5"></span>
				<span class="inner">
				   <!-- lightbox -->
				   <a class="ico-rounded lightbox" href="data/kscis/coversheet/cover_en.jpg" data-plugin-options="{&quot;type&quot;:&quot;image&quot;}">
				   <span class="fa fa-search-plus size-18"></span>
				   </a>
				   <!-- details -->
				   <a class="ico-rounded" href="issue_15176_15403.html">
				   <span class="fa fa-link size-18"></span>
				   </a>
				</span>
			 </span>
			 <img class="img-responsive" src="data/kscis/coversheet/cover_en.jpg" alt="" height="600">
		  </figure>
		  <div class="item-box-desc padding-6" >
			 <h3 class="text-center"><a href="issue_15176_15403.html">Issue 3</a></h3>
			 <ul class="list-inline categories nomargin text-center">
			 </ul>
		  </div>
	   </div>
	   <div class="item-box col-md-3 col-lg-2 col-sm-4 col-xs-12">
		  <figure>
			 <span class="item-hover">
				<span class="overlay dark-5"></span>
				<span class="inner">
				   <!-- lightbox -->
				   <a class="ico-rounded lightbox" href="data/kscis/coversheet/cover_en.jpg" data-plugin-options="{&quot;type&quot;:&quot;image&quot;}">
				   <span class="fa fa-search-plus size-18"></span>
				   </a>
				   <!-- details -->
				   <a class="ico-rounded" href="issue_15176_15376.html">
				   <span class="fa fa-link size-18"></span>
				   </a>
				</span>
			 </span>
			 <img class="img-responsive" src="data/kscis/coversheet/cover_en.jpg" alt="" height="600">
		  </figure>
		  <div class="item-box-desc padding-6" >
			 <h3 class="text-center"><a href="issue_15176_15376.html">Issue 2</a></h3>
			 <ul class="list-inline categories nomargin text-center">
			 </ul>
		  </div>
	   </div>
	   <div class="item-box col-md-3 col-lg-2 col-sm-4 col-xs-12">
		  <figure>
			 <span class="item-hover">
				<span class="overlay dark-5"></span>
				<span class="inner">
				   <!-- lightbox -->
				   <a class="ico-rounded lightbox" href="data/kscis/coversheet/cover_en.jpg" data-plugin-options="{&quot;type&quot;:&quot;image&quot;}">
				   <span class="fa fa-search-plus size-18"></span>
				   </a>
				   <!-- details -->
				   <a class="ico-rounded" href="issue_15176_15177.html">
				   <span class="fa fa-link size-18"></span>
				   </a>
				</span>
			 </span>
			 <img class="img-responsive" src="data/kscis/coversheet/cover_en.jpg" alt="" height="600">
		  </figure>
		  <div class="item-box-desc padding-6" >
			 <h3 class="text-center"><a href="issue_15176_15177.html">Issue 1</a></h3>
			 <ul class="list-inline categories nomargin text-center">
			 </ul>
		  </div>
	   </div>
	</div>
 </div>
</div>
 */



public class KirkukUnivCollegeofScienceHtmlLinkExtractorFactory implements LinkExtractorFactory {

		@Override
		public LinkExtractor createLinkExtractor(String mimeType) {
			LinkExtractor defaultExtractor = new JsoupHtmlLinkExtractor();
			return new KirkukUnivCollegeofScienceVolumeIssueLinkExtractor(defaultExtractor);
		}

}