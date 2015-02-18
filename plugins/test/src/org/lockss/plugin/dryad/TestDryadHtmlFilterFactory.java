/*
/    * $Id$
 */

/*

 Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.dryad;

import java.io.*;

import org.htmlparser.filters.TagNameFilter;
import org.lockss.util.*;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.test.StringInputStream;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilterTransform;
import org.lockss.test.*;


public class TestDryadHtmlFilterFactory extends LockssTestCase {
  static String ENC = Constants.ENCODING_UTF_8;

  private DryadHtmlFilterFactory fact;
  private MockArchivalUnit mau;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    fact = new DryadHtmlFilterFactory();
    mau = new MockArchivalUnit();
  }

  private static final String withStuff = 
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">" +
"<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"en\" class=\"no-js\">" +
"<head>" +
"<meta content=\"text/html; charset=UTF-8\" http-equiv=\"Content-Type\" />" +
"<meta content=\"IE=edge,chrome=1\" http-equiv=\"X-UA-Compatible\" />" +
"<meta content=\"width=device-width; initial-scale=1.0; maximum-scale=1.0;\" name=\"viewport\" />" +
"<title>Data from: Morphological evolution in the mullberry family (Moraceae) - Dryad</title>" +
"<link rel=\"schema.DCTERMS\" href=\"http://purl.org/dc/terms/\" />" +
"<link rel=\"schema.DC\" href=\"http://purl.org/dc/elements/1.1/\" />" +
"<meta name=\"DC.creator\" content=\"Clement, Wendy L\" />" +
"<meta name=\"DC.creator\" content=\"Weiblen, George D\" />" +
"<meta name=\"DCTERMS.dateAccepted\" content=\"2010-09-09T15:42:16Z\" scheme=\"DCTERMS.W3CDTF\" />" +
"<meta name=\"DCTERMS.available\" content=\"2010-09-09T15:42:16Z\" scheme=\"DCTERMS.W3CDTF\" />" +
"<meta name=\"DCTERMS.issued\" content=\"2009\" scheme=\"DCTERMS.W3CDTF\" />" +
"<meta name=\"DC.identifier\" content=\"doi:10.5061/dryad.1970\" />" +
"<meta name=\"DCTERMS.bibliographicCitation\" content=\"Clement WL, Weiblen GD (2009) Morphological evolution in the mullberry family (Moraceae). Systematic Botany 34: 530-552.\" />" +
"<meta name=\"DC.identifier\" content=\"http://hdl.handle.net/10255/dryad.1970\" scheme=\"DCTERMS.URI\" />" +
"<meta name=\"DCTERMS.abstract\" content=\"The mulberry family Moraceae comprises 37 genera and ...\" xml:lang=\"en\" />" +
"<meta name=\"DC.relation\" content=\"34;3;2009\" />" +
"<meta name=\"DCTERMS.hasPart\" content=\"doi:10.5061/dryad.1970/1\" />" +
"<meta name=\"DCTERMS.isReferencedBy\" content=\"doi:10.1600/036364409X472372\" />" +
"<meta name=\"DC.subject\" content=\"Bayesian inference\" xml:lang=\"en\" />" +
"<meta name=\"DC.subject\" content=\"parsimony ratchet\" xml:lang=\"en\" />" +
"<meta name=\"DC.title\" content=\"Data from: Morphological evolution in the mullberry " +
"family (Moraceae)\" xml:lang=\"en\" /><meta name=\"DC.contributor\" content=\"Weiblen, George D\" xml:lang=\"en\" />" +
"</head>" +
"" +
"<div id=\"ds-main\">" +
"<div id=\"ds-header-wrapper\">" +
"<div class=\"clearfix\" id=\"ds-header\">" +
"<a class=\"accessibly-hidden\" href=\"#ds-body\" id=\"skip-nav\">Skip navigation</a>" +
"<a id=\"ds-header-logo-link\" href=\"/\">" +
"<span id=\"ds-header-logo-prod\" class=\"ds-header-logo\"> </span>" +
"<span id=\"ds-header-logo-text\">mirage</span>" +
"</a>" +
"<h1 class=\"pagetitle visuallyhidden\">Data from: Morphological evolution in the mullberry family (Moraceae)</h1>" +
"<h2 class=\"static-pagetitle visuallyhidden\">Dryad Repository</h2>" +
"<div  id=\"sharing-tools\">" +
"<a href=\"http://twitter.com/datadryad\">" +
"<img alt=\"Follow us on Twitter\" src=\"/themes/Mirage/images/dryad_twit_icon.png\" />" +
"</a>" +
"<a href=\"http://www.facebook.com/DataDryad\">" +
"<img alt=\"Find us on Facebook\" src=\"/themes/Mirage/images/dryad_fb_icon2.png\" />" +
"</a>" +
"<a href=\"http://blog.datadryad.org/feed/\">" +
"<img alt=\"RSS feed - Dryad News\" src=\"/themes/Mirage/images/dryad_rss_icon.png\" />" +
"</a>" +
"</div>" +
"<div id=\"main-menu\">" +
"<ul class=\"sf-menu\">" +
"<li>" +
"<a href=\">About</a>" +
"<ul>" +
"<li>" +
"<a href=\"/pages/repository\">Repository features and technology</a>" +
"</li>" +
"<li>" +
"<a href=\"/pages/organization\">The organization</a>" +
"</li>" +
"<li>" +
"<a target=\"_blank\" href=\"http://blog.datadryad.org\">News and views</a>" +
"</li>" +
"<li>" +
"<a href=\"/pages/whoWeAre\">Who we are</a>" +
"</li>" +
"<li>" +
"<a href=\"/pages/faq\">Frequently asked questions</a>" +
"</li>" +
"</ul>" +
"</li>" +
"<li>" +
"<a href=\">For researchers</a>" +
"<ul>" +
"<li>" +
"<a href=\"/pages/faq#depositing\">Submit data</a>" +
"</li>" +
"<li>" +
"<a href=\"/pages/faq#using\">Use data</a>" +
"</li>" +
"<li>" +
"<a href=\"/pages/integratedJournals\">Currently integrated journals</a>" +
"</li>" +
"<li>" +
"<a href=\"/pages/policies\">Terms of service (2013-08-22)</a>" +
"</li>" +
"</ul>" +
"</li>" +
"<li>" +
"<a href=\">For organizations</a>" +
"<ul>" +
"<li>" +
"<a href=\"/pages/journalIntegration\">Journal integration</a>" +
"</li>" +
"<li>" +
"<a href=\"/pages/membershipOverview\">Membership</a>" +
"</li>" +
"<li>" +
"<a href=\"/pages/pricing\">Pricing plans</a>" +
"</li>" +
"</ul>" +
"</li>" +
"<li>" +
"<a href=\"/feedback\">Contact us</a>" +
"</li>" +
"<li class=\"no-hover-highlight\">" +
"<a href=\"/login\">" +
"<span id=\"login-item\">Login</span>" +
"<span class=\"accessibly-hidden\"> or </span>" +
"<span id=\"sign-up-item\">Sign Up</span>" +
"</a>" +
"</li>" +
"</ul>" +
"</div>" +
"</div>" +
"</div>" +
"<div id=\"ds-trail-wrapper\">" +
"<ul id=\"ds-trail\">" +
"<li class=\"ds-trail-link first-link \">" +
"<a href=\"/\">Dryad Digital Repository</a>" +
"</li>" +
//"<li class=\"ds-trail-arrow\">→</li>" +
"<li class=\"ds-trail-link \">" +
"<a href=\"/handle/10255/1\">Main</a>" +
"</li>" +
//"<li class=\"ds-trail-arrow\">→</li>" +
"<li class=\"ds-trail-link \">" +
"<a href=\"/handle/10255/3\">Dryad Data Packages</a>" +
"</li>" +
"<li class=\"ds-trail-arrow\">→</li>" +
"<li class=\"ds-trail-link last-link\">View Item</li>" +
"</ul>" +
"</div>" +
"<div class=\"hidden\" id=\"no-js-warning-wrapper\">" +
"<div id=\"no-js-warning\">" +
"<div class=\"notice failure\">JavaScript is disabled for your browser. Some features of this site may not work without it.</div>" +
"</div>" +
"</div>" +
"<div id=\"ds-content-wrapper\">" +
"<div class=\"clearfix\" id=\"ds-content\">" +
"<div id=\"ds-body\">" +
"<div id=\"org_datadryad_dspace_xmlui_aspect_browse_ItemViewer_div_item-view\" class=\"ds-static-div primary\">" +
"<!-- External Metadata URL: cocoon://metadata/handle/10255/dryad.1970/mets.xml-->" +
"<div class=\"publication-header\">" +
"<p class=\"pub-title\">Data from: Morphological evolution in the mullberry family (Moraceae)</p>" +
"</div>" +
"<div class=\"ds-static-div primary\">" +
"<h2 class=\"ds-list-head\">Files in this package</h2>" +
"<div class=\"file-list\">" +
"<p style=\"font-size: 0.9em;\" class=\"license-badges\">To the extent possible under law, the authors" +
"   have waived all copyright and related or neighboring rights to this data. <a class=\"single-image-link\" target=\"_blank\" href=\"http://creativecommons.org/publicdomain/zero/1.0/\">" +
"<img alt=\"CC0 (opens a new window)\" src=\"/themes/Dryad/images/cc-zero.png\" />" +
"</a>" +
"<a class=\"single-image-link\" target=\"_blank\" href=\"http://opendefinition.org/\">" +
"<img alt=\"Open Data (opens a new window)\" src=\"/themes/Dryad/images/opendata.png\" />" +
"</a>" +
"</p>" +
"<!--External Metadata URL:" +
" cocoon://metadata/handle/10255/dryad.1971/mets.xml-->" +
"<table class=\"package-file-description\">" +
"<tbody>" +
"<tr>" +
"<th>Title</th>" +
"<th>Clement &amp; Weiblen Suppl. appendix1</th>" +
"<tr>" +
"<th>Downloaded</th>" +
"<td>36 times</td>" +
"</tr>" +
"<tr>" +
"<th>Downloaded</th>" +
"<td>1 time</td>" +
"</tr>" +
"</tr>" +
"<tr>" +
"<th>Download</th>" +
"<td>" +
"<a href=\"/bitstream/handle/10255/dryad.1971/Clement%20%26%20Weiblen%20Suppl.%20appendix1.pdf?sequence=1\">Clement &amp; Weiblen Suppl. appendix1.pdf<span class=\"bitstream-filesize\"> (349.3Kb)</span>" +
"</a>" +
"</td>" +
"</tr>" +
"<tr>" +
"<th>Details</th>" +
"<td>" +
"<a href=\"/resource/doi:10.5061/dryad.1970/1\">View File Details</a>" +
"</td>" +
"</tr>" +
"</tbody>" +
"</table>" +
"</div>" +
"</div>" +
"<div class=\"ds-static-div primary\">" +
"<div class=\"secondary\">" +
"<p class=\"ds-paragraph\">When using this data, please cite the original article:</p>" +
"<div class=\"citation-sample\">Clement WL, Weiblen GD (2009) Morphological evolution in the mullberry family (Moraceae). Systematic Botany 34: 530-552. <a href=\"http://dx.doi.org/10.1600/036364409X472372\">doi:10.1600/036364409X472372</a>" +
"</div>" +
"<p class=\"ds-paragraph\">Additionally, please cite the Dryad data package: </p>" +
"<div class=\"citation-sample\">Clement WL, Weiblen GD (2009) Data from: Morphological evolution in the mullberry family (Moraceae). <span>Dryad Digital Repository. </span>" +
"<a href=\"http://dx.doi.org/10.5061/dryad.1970\">doi:10.5061/dryad.1970</a>" +
"</div>" +
"<div style=\"padding-right: 20px; padding-bottom: 5px;\" align=\"right\">" +
"<a title=\"Click to open and close\" id=\"cite\" href=\"/cite\">Cite</a>  |  <a title=\"Click to open and close\" id=\"share\" href=\"/share\">Share</a>" +
"<div id=\"citemediv\">" +
"<table style=\"width: 100%;\">" +
"<tr class=\"ds-table-row even \">" + 
"<td>.dryad.pageviews</td><td>206</td><td /></tr>" +
"<tr class=\"ds-table-row odd \">" +
"<td>.dryad.downloads</td><td>184</td><td /></tr>" +
"<tr>" +
"<td style=\"text-decoration: underline;\" align=\"left\">Download the data package citation in the following formats:</td>" +
"</tr>" +
"<tr>" +
"<td>" +
" " +
" <a href=\"/resource/doi:10.5061/dryad.1970/citation/ris\">RIS </a>" +
"<span class=\"italics\">(compatible with EndNote, Reference Manager, ProCite, RefWorks)</span>" +
"</td>" +
"</tr>" +
"<tr>" +
"<td>" +
" " +
" <a href=\"/resource/doi:10.5061/dryad.1970/citation/bib\">BibTex </a>" +
"<span class=\"italics\">(compatible with BibDesk, LaTeX)</span>" +
"</td>" +
"</tr>" +
"</table>" +
"</div>" +
"<div id=\"sharemediv\">" +
"<table style=\"width: 100%;\">" +
"<tr>" +
"<td>" +
"<a href=\"http://www.delicious.com/save\">" +
"</a>" +
"<script src=\"/themes/Dryad/lib/delicious.js\" type=\"text/javascript\"> </script>" +
"</td>" +
"<td>" +
"</td>" +
"<td>" +
"<a href=\"http://reddit.com/submit\">" +
"<img alt=\"Reddit\" src=\"http://reddit.com/static/spreddit7.gif\" border=\"0px;\" /> </a>" +
"</td>" +
"<td>" +
"<a href=\"http://twitter.com/share\" class=\"twitter-share-button\" data-count=\"none\" data-via=\"datadryad\" data-url=\"http://dx.doi.org/doi:10.5061/dryad.1970\">Tweet</a>" +
"</td>" +
"<td>" +
"<a href=\"http://www.mendeley.com/import/?url=http://datadryad.org/resource/doi:10.5061/dryad.1970\">" +
"<img alt=\"Mendeley\" src=\"http://www.mendeley.com/graphics/mendeley.png\" border=\"0px;\" />" +
"</a>" +
"</td>" +
"</tr>" +
"</table>" +
"</div>" +
"</div>" +
"</div>" +
"</div>" +
"<div class=\"ds-static-div primary\">" +
"<div class=\"item-summary-view-metadata\">" +
"<table class=\"package-metadata\">" +
"<tbody>" +
"<tr>" +
"<th>DOI</th>" +
"<td>doi:10.5061/dryad.1970<tr>" +
"<th>Pageviews</th>" +
"<td>276</td>" +
"</tr>" +
"</td>" +
"</tr>" +
"<tr>" +
"<th>Keywords</th>" +
"<td>Bayesian inference, parsimony ratchet</td>" +
"<td />" +
"</tr>" +
"<tr>" +
"<th>Date Submitted</th>" +
"<td>2010-09-09T15:42:16Z</td>" +
"<td />" +
"</tr>" +
"<tr>" +
"<th>Scientific Names</th>" +
"<td>Antiaropsineae, Maclureae, Malaisia, Sloetia</td>" +
"</tr>" +
"<tr>" +
"<td colspan=\"2\">" +
"<div class=\"article-abstract\">" +
"<b>Abstract</b>" +
"<br />The mulberry family Moraceae comprises 37 genera and ....</div>" +
"</td>" +
"</tr>" +
"</tbody>" +
"</table>" +
"</div>" +
"</div>" +
"<div style=\"padding: 10px; margin-top: 5px; margin-bottom: 5px;\">" +
"<a href=\"?show=full\">Show Full Metadata</a>" +
"</div>" +
"</div>" +
"</div>" +
"<div id=\"ds-options-wrapper\">" +
"<div id=\"ds-options\">" +
"<div class=\"simple-box\" id=\"submit-data-sidebar-box\">" +
"<div id=\"file_news_div_news\" class=\"ds-static-div primary\">" +
"<p class=\"ds-paragraph\">" +
"<a href=\"/handle/10255/3/submit\" class=\"submitnowbutton\">Submit data now</a>" +
"</p>" +
"<p style=\"margin: 1em 0 4px;\">" +
"<a href=\"/pages/faq#deposit\">How and why?</a>" +
"</p>" +
"</div>" +
"</div>" +
"<div class=\"NOT-simple-box\">" +
"<div class=\"home-col-1\">" +
"<h1 class=\"ds-div-head\">Search for data" +
" </h1>" +
"<form method=\"get\" action=\"/discover\" class=\"ds-interactive-div primary\" id=\"aspect_discovery_SiteViewer_div_front-page-search\">" +
"<p class=\"ds-paragraph\">" +
"<input style=\"width: 175px;\" value=\"\" type=\"text\" title=\"Enter keyword, author, title, DOI, etc. Example: herbivory\" placeholder=\"Enter keyword, DOI, etc.\" name=\"query\" class=\"ds-text-field\" id=\"aspect_discovery_SiteViewer_field_query\" />" +
"<input style=\"margin-right: -4px;\" value=\"Go\" type=\"submit\" name=\"submit\" class=\"ds-button-field\" id=\"aspect_discovery_SiteViewer_field_submit\" />" +
"<a href=\"/discover?query=&amp;submit=Search\" style=\"float:left; font-size: 95%;\">Advanced search</a>" +
"</p>" +
"</form>" +
"</div>" +
"</div>" +
"<div class=\"NOT-simple-box\">" +
"<h1 id=\"ds_connect_with_dryad_head\" class=\"ds-div-head ds_connect_with_dryad_head\">Be part of Dryad" +
" </h1>" +
"<div style=\"font-size: 14px;\" class=\"ds-static-div primary\" id=\"ds_connect_with_dryad\">" +
"<p style=\"margin-bottom: 0;\">" +
" Learn more about:" +
" </p>" +
"<ul style=\"list-style: none; margin-left: 1em;\">" +
"<li>" +
"<a href=\"/pages/membershipOverview\">Membership</a>" +
"</li>" +
"<li>" +
"<a href=\"/pages/journalIntegration\">Submission integration</a>" +
"</li>" +
"<li>" +
"<a href=\"/pages/pricing\">Pricing plans</a>" +
"</li>" +
"</ul>" +
"</div>" +
"</div>" +
"</div>" +
"</div>" +
"</div>" +
"</div>" +
"<div id=\"ds-footer-wrapper\">" +
"<div id=\"ds-footer\">" +
"<div id=\"ds-footer-right\">" +
"<a href=\"/pages/policies\">Terms of Service</a>" +
" | " +
" <a href=\"/feedback\">Contact Us</a>" +
"</div>" +
"<p style=\"margin: 0;\">" +
" Dryad is a nonprofit repository for data underlying the international scientific and medical literature.</p>" +
"<p style=\"clear: both; float: right; margin-top: 11px; color: #999;\">Latest build Wed, 21 Aug 2013 21:24:42 EDT. Served by  North Carolina State University</p>" +
"<!--Git Commit Hash: ed09880926e39da72d22fa5912af66247e44ad78-->" +
"<div  style=\"color: #999;\" id=\"ds-footer-left\">Powered by  <a  target=\"_blank\" href=\"http://www.dspace.org/\" class=\"single-image-link\">" +
"<img alt=\"DSpace\" src=\"/themes/Mirage/images/powered-by-dspace.png\" class=\"powered-by\" />" +
"<span class=\"accessibly-hidden\"> (opens in a new window)</span>" +
"</a>" +
"</div>" +
"<a class=\"hidden\" href=\"/htmlmap\">sitemap</a>" +
"</div>" +
"</div>" +
"</div>" +
"<span title=\"ctx_ver=Z39.88-2004&amp;rft.type=Dataset&amp;rft.status=scanned" +
"&amp;rft.dryad=410&amp;rft.dryad=114\" class=\"Z3988\">&nbsp;</span>" +
"<div id=\"ds-system-wide-alert\">\n" + 
"<p>Submissions will be temporarily disabled for regular maintenance.</p>\n" + 
"</div>" +
"</body></html>";

  private static final String withoutStuff = 
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">" +
"<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"en\" class=\"no-js\">" +
"<head>" +
"<meta content=\"text/html; charset=UTF-8\" http-equiv=\"Content-Type\" />" +
"<meta content=\"IE=edge,chrome=1\" http-equiv=\"X-UA-Compatible\" />" +
"<meta content=\"width=device-width; initial-scale=1.0; maximum-scale=1.0;\" name=\"viewport\" />" +
"<title>Data from: Morphological evolution in the mullberry family (Moraceae) - Dryad</title>" +
"<link rel=\"schema.DCTERMS\" href=\"http://purl.org/dc/terms/\" />" +
"<link rel=\"schema.DC\" href=\"http://purl.org/dc/elements/1.1/\" />" +
"<meta name=\"DC.creator\" content=\"Clement, Wendy L\" />" +
"<meta name=\"DC.creator\" content=\"Weiblen, George D\" />" +
"<meta name=\"DCTERMS.dateAccepted\" content=\"2010-09-09T15:42:16Z\" scheme=\"DCTERMS.W3CDTF\" />" +
"<meta name=\"DCTERMS.available\" content=\"2010-09-09T15:42:16Z\" scheme=\"DCTERMS.W3CDTF\" />" +
"<meta name=\"DCTERMS.issued\" content=\"2009\" scheme=\"DCTERMS.W3CDTF\" />" +
"<meta name=\"DC.identifier\" content=\"doi:10.5061/dryad.1970\" />" +
"<meta name=\"DCTERMS.bibliographicCitation\" content=\"Clement WL, Weiblen GD (2009) Morphological evolution in the mullberry family (Moraceae). Systematic Botany 34: 530-552.\" />" +
"<meta name=\"DC.identifier\" content=\"http://hdl.handle.net/10255/dryad.1970\" scheme=\"DCTERMS.URI\" />" +
"<meta name=\"DCTERMS.abstract\" content=\"The mulberry family Moraceae comprises 37 genera and ...\" xml:lang=\"en\" />" +
"<meta name=\"DC.relation\" content=\"34;3;2009\" />" +
"<meta name=\"DCTERMS.hasPart\" content=\"doi:10.5061/dryad.1970/1\" />" +
"<meta name=\"DCTERMS.isReferencedBy\" content=\"doi:10.1600/036364409X472372\" />" +
"<meta name=\"DC.subject\" content=\"Bayesian inference\" xml:lang=\"en\" />" +
"<meta name=\"DC.subject\" content=\"parsimony ratchet\" xml:lang=\"en\" />" +
"<meta name=\"DC.title\" content=\"Data from: Morphological evolution in the mullberry family (Moraceae)\" xml:lang=\"en\" /><meta name=\"DC.contributor\" content=\"Weiblen, George D\" xml:lang=\"en\" />" +
"</head>" +
"" +
"<div id=\"ds-main\">" +
"<div id=\"ds-trail-wrapper\">" +
"<ul id=\"ds-trail\">" +
"<li class=\"ds-trail-link first-link \">" +
"<a href=\"/\">Dryad Digital Repository</a>" +
"</li>" +
//"<li class=\"ds-trail-arrow\">→</li>" +
"<li class=\"ds-trail-link \">" +
"<a href=\"/handle/10255/1\">Main</a>" +
"</li>" +
//"<li class=\"ds-trail-arrow\">→</li>" +
"<li class=\"ds-trail-link \">" +
"<a href=\"/handle/10255/3\">Dryad Data Packages</a>" +
"</li>" +
"<li class=\"ds-trail-arrow\">→</li>" +
"<li class=\"ds-trail-link last-link\">View Item</li>" +
"</ul>" +
"</div>" +
"<div class=\"hidden\" id=\"no-js-warning-wrapper\">" +
"<div id=\"no-js-warning\">" +
"<div class=\"notice failure\">JavaScript is disabled for your browser. Some features of this site may not work without it.</div>" +
"</div>" +
"</div>" +
"<div id=\"ds-content-wrapper\">" +
"<div class=\"clearfix\" id=\"ds-content\">" +
"" +
"<div id=\"ds-body\">" +
"<div id=\"org_datadryad_dspace_xmlui_aspect_browse_ItemViewer_div_item-view\" class=\"ds-static-div primary\">" +
"<!-- External Metadata URL: cocoon://metadata/handle/10255/dryad.1970/mets.xml-->" +
"<div class=\"publication-header\">" +
"<p class=\"pub-title\">Data from: Morphological evolution in the mullberry family (Moraceae)</p>" +
"</div>" +
"<div class=\"ds-static-div primary\">" +
"<h2 class=\"ds-list-head\">Files in this package</h2>" +
"<div class=\"file-list\">" +
"<p style=\"font-size: 0.9em;\" class=\"license-badges\">To the extent possible under law, the authors" +
"   have waived all copyright and related or neighboring rights to this data. <a class=\"single-image-link\" target=\"_blank\" href=\"http://creativecommons.org/publicdomain/zero/1.0/\">" +
"<img alt=\"CC0 (opens a new window)\" src=\"/themes/Dryad/images/cc-zero.png\" />" +
"</a>" +
"<a class=\"single-image-link\" target=\"_blank\" href=\"http://opendefinition.org/\">" +
"<img alt=\"Open Data (opens a new window)\" src=\"/themes/Dryad/images/opendata.png\" />" +
"</a>" +
"</p>" +
"<!--External Metadata URL:" +
" cocoon://metadata/handle/10255/dryad.1971/mets.xml-->" +
"<table class=\"package-file-description\">" +
"<tbody>" +
"<tr>" +
"<th>Title</th>" +
"<th>Clement &amp; Weiblen Suppl. appendix1</th>" +
"</tr>" +
"<tr>" +
"<th>Download</th>" +
"<td>" +
"<a href=\"/bitstream/handle/10255/dryad.1971/Clement%20%26%20Weiblen%20Suppl.%20appendix1.pdf?sequence=1\">Clement &amp; Weiblen Suppl. appendix1.pdf<span class=\"bitstream-filesize\"> (349.3Kb)</span>" +
"</a>" +
"</td>" +
"</tr>" +
"<tr>" +
"<th>Details</th>" +
"<td>" +
"<a href=\"/resource/doi:10.5061/dryad.1970/1\">View File Details</a>" +
"</td>" +
"</tr>" +
"</tbody>" +
"</table>" +
"</div>" +
"</div>" +
"<div class=\"ds-static-div primary\">" +
"<div class=\"secondary\">" +
"<p class=\"ds-paragraph\">When using this data, please cite the original article:</p>" +
"<div class=\"citation-sample\">Clement WL, Weiblen GD (2009) Morphological evolution in the mullberry family (Moraceae). Systematic Botany 34: 530-552. <a href=\"http://dx.doi.org/10.1600/036364409X472372\">doi:10.1600/036364409X472372</a>" +
"</div>" +
"<p class=\"ds-paragraph\">Additionally, please cite the Dryad data package: </p>" +
"<div class=\"citation-sample\">Clement WL, Weiblen GD (2009) Data from: Morphological evolution in the mullberry family (Moraceae). <span>Dryad Digital Repository. </span>" +
"<a href=\"http://dx.doi.org/10.5061/dryad.1970\">doi:10.5061/dryad.1970</a>" +
"</div>" +
"<div style=\"padding-right: 20px; padding-bottom: 5px;\" align=\"right\">" +
"<a title=\"Click to open and close\" id=\"cite\" href=\"/cite\">Cite</a>  |  <a title=\"Click to open and close\" id=\"share\" href=\"/share\">Share</a>" +
"<div id=\"citemediv\">" +
"<table style=\"width: 100%;\">" +
"<tr>" +
"<td style=\"text-decoration: underline;\" align=\"left\">Download the data package citation in the following formats:</td>" +
"</tr>" +
"<tr>" +
"<td>" +
" " +
" <a href=\"/resource/doi:10.5061/dryad.1970/citation/ris\">RIS </a>" +
"<span class=\"italics\">(compatible with EndNote, Reference Manager, ProCite, RefWorks)</span>" +
"</td>" +
"</tr>" +
"<tr>" +
"<td>" +
" " +
" <a href=\"/resource/doi:10.5061/dryad.1970/citation/bib\">BibTex </a>" +
"<span class=\"italics\">(compatible with BibDesk, LaTeX)</span>" +
"</td>" +
"</tr>" +
"</table>" +
"</div>" +
"</div>" +
"</div>" +
"</div>" +
"<div class=\"ds-static-div primary\">" +
"<div class=\"item-summary-view-metadata\">" +
"<table class=\"package-metadata\">" +
"<tbody>" +
"<tr>" +
"<th>DOI</th>" +
"<td>doi:10.5061/dryad.1970" +
"</td>" +
"</tr>" +
"<tr>" +
"<th>Keywords</th>" +
"<td>Bayesian inference, parsimony ratchet</td>" +
"<td />" +
"</tr>" +
"<tr>" +
"<th>Date Submitted</th>" +
"<td>2010-09-09T15:42:16Z</td>" +
"<td />" +
"</tr>" +
"<tr>" +
"<th>Scientific Names</th>" +
"<td>Antiaropsineae, Maclureae, Malaisia, Sloetia</td>" +
"</tr>" +
"<tr>" +
"<td colspan=\"2\">" +
"<div class=\"article-abstract\">" +
"<b>Abstract</b>" +
"<br />The mulberry family Moraceae comprises 37 genera and ....</div>" +
"</td>" +
"</tr>" +
"</tbody>" +
"</table>" +
"</div>" +
"</div>" +
"<div style=\"padding: 10px; margin-top: 5px; margin-bottom: 5px;\">" +
"<a href=\"?show=full\">Show Full Metadata</a>" +
"</div>" +
"</div>" +
"</div>" +
"</div>" +
"</div>" +
"</div>" +
"</body></html>";


  public void testFiltering() throws Exception {
    assertFilterTo(withoutStuff, withStuff);
  }

  private void assertFilterTo(String expected, String str) throws Exception {
    StringInputStream mis = new StringInputStream(expected);
    InputStream filteredStream = new HtmlFilterInputStream (mis, Constants.ENCODING_UTF_8,
         HtmlNodeFilterTransform.exclude(new TagNameFilter("fake")));
    InputStream inA = new ReaderInputStream(new WhiteSpaceFilter(FilterUtil.getReader(
        filteredStream, Constants.ENCODING_UTF_8)));
    String a = StringUtil.fromInputStream(inA);
    InputStream inB = fact.createFilteredInputStream(mau, new StringInputStream(str),
        Constants.ENCODING_UTF_8);
    String b = StringUtil.fromInputStream(inB);
    assertEquals(a,b);
  }

}
