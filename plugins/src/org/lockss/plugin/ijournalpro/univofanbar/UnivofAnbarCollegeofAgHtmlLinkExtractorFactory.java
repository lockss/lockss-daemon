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
<div class="accordion mb-2" id="">
 <div class="accordion-item">
    <h2 class="accordion-header" id="pVol_15577">
       <button class="accordion-button bg-body collapsed text-primary" type="button" data-bs-toggle="collapse" data-bs-target="#pVol_15577_" aria-expanded="true" aria-controls="pVol_15577_"><b><i class="fa-regular fa-file-lines me-2"></i>Volume 24 (2026)</b></button>
    </h2>
    <div id="pVol_15577_" class="accordion-collapse collapse" aria-labelledby="pVol_15577">
       <div class="accordion-body p-2 row">
          <div class="col-md-3 col-lg-3">
             <div>
                <a class="js_click pointer_cursor" data-handler="loadModal"  data-param_a="Anbar Journal of Agricultural Sciences" data-param_b="./data/aagrs/coversheet/951776116524.jpeg">
                <img src="data/aagrs/coversheet/951776116524.jpeg" alt="Anbar Journal of Agricultural Sciences" class="col-12 shadow-sm"/>
                </a>
             </div>
             <div>
                <h5 class="text-center mt-3"><a href="issue_15577_15578.html">Issue 1</a></h5>
             </div>
          </div>
       </div>
    </div>
 </div>
</div>
<div class="accordion mb-2" id="">
 <div class="accordion-item">
    <h2 class="accordion-header" id="pVol_15363">
       <button class="accordion-button bg-body collapsed text-primary" type="button" data-bs-toggle="collapse" data-bs-target="#pVol_15363_" aria-expanded="true" aria-controls="pVol_15363_"><b><i class="fa-regular fa-file-lines me-2"></i>Volume 23 (2025)</b></button>
    </h2>
    <div id="pVol_15363_" class="accordion-collapse collapse" aria-labelledby="pVol_15363">
       <div class="accordion-body p-2 row">
          <div class="col-md-3 col-lg-3">
             <div>
                <a class="js_click pointer_cursor" data-handler="loadModal"  data-param_a="Anbar Journal of Agricultural Sciences" data-param_b="./data/aagrs/coversheet/731767567945.jpeg">
                <img src="data/aagrs/coversheet/731767567945.jpeg" alt="Anbar Journal of Agricultural Sciences" class="col-12 shadow-sm"/>
                </a>
             </div>
             <div>
                <h5 class="text-center mt-3"><a href="issue_15363_15506.html">Issue 2</a></h5>
             </div>
          </div>
          <div class="col-md-3 col-lg-3">
             <div>
                <a class="js_click pointer_cursor" data-handler="loadModal"  data-param_a="Anbar Journal of Agricultural Sciences" data-param_b="./data/aagrs/coversheet/561758229391.jpeg">
                <img src="data/aagrs/coversheet/561758229391.jpeg" alt="Anbar Journal of Agricultural Sciences" class="col-12 shadow-sm"/>
                </a>
             </div>
             <div>
                <h5 class="text-center mt-3"><a href="issue_15363_15364.html">Issue 1</a></h5>
             </div>
          </div>
       </div>
    </div>
 </div>
</div>
 */



public class UnivofAnbarCollegeofAgHtmlLinkExtractorFactory implements LinkExtractorFactory {

		@Override
		public LinkExtractor createLinkExtractor(String mimeType) {
			LinkExtractor defaultExtractor = new JsoupHtmlLinkExtractor();
			return new UnivofAnbarCollegeofAgVolumeIssueLinkExtractor(defaultExtractor);
		}

}