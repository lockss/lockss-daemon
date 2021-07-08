/*
 * $Id$
 */

/*

Copyright (c) 2000-2018 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.pubfactory;

import org.apache.cxf.common.util.StringUtils;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.StringUtil;

import java.io.IOException;
import java.io.InputStream;

public class TestPubFactoryHtmlHashFilterFactory extends LockssTestCase {
  private PubFactoryHtmlHashFilterFactory fact;
  private MockArchivalUnit bau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new PubFactoryHtmlHashFilterFactory();
  }
  
  private void doFilterTest(ArchivalUnit au, 
      FilterFactory fact, String nameToHash, String expectedStr) 
          throws PluginException, IOException {
    InputStream actIn; 
    actIn = fact.createFilteredInputStream(au, 
        new StringInputStream(nameToHash), Constants.DEFAULT_ENCODING);
    String filtered = StringUtil.fromInputStream(actIn);
    //log.info(filtered);
    assertEquals(expectedStr, filtered);

  }
  
  private static final String blockHtml =
    "<div class=\"content-box\"><div class=\"mainBase\" id=\"mainContent\">" +
    "  <div id=\"readPanel\">" +
    "    <a class=\"summary-toggle ico-summary js-summary-toggle phoneOnly\" href=\"#\"><span>Show Summary Details</span></a>" +
    "  </div>" +
    "</div></div>" +
    "<div id=\"headerWrap\">headerWrap content</div>";

  private static final String blockFiltered =
    "<div class=\"content-box\"><div class=\"mainBase\" id=\"mainContent\">" +
     " <div id=\"readPanel\">" +
       " <a class=\"summary-toggle ico-summary js-summary-toggle phoneOnly\" ><span>Show Summary Details</span></a>" +
     " </div>" +
    "</div></div>";

  private static final String ulDataMenu =
    "<ul data-menu-list=\"list-id-5c36bde5-1620-44f8-bd4e-88e4da63b50f\" role=\"listbox\" class=\"List\">";

  private static final String filteredUlDataMenu =
    "<ul role=\"listbox\" class=\"List\">";

  private static final String liDataMenu =
    "<li aria-selected=\"false\" role=\"option\" data-menu-item=\"list-id-69f0550e-0a86-4387-a582-7feb7a5203b6\" class=\"ListItem\">";

  private static final String filteredLiDataMenu =
    "<li aria-selected=\"false\" role=\"option\" class=\"ListItem\">";

  private static final String hTags =
    "<h2 class=\"abstractTitle text-title my-1\" id=\"d3038e2\">Abstract</h2>" +
    "<h3 id=\"d4951423e445\">a. Satellite data</h3>" +
    "<h4 id=\"d4951423e1002\">On what scale does lightning enhancement occur?</h4>" +
    "<h5 id=\"h79834098f6464\">Does this even happen?</h5>" +
    "<h8 id=\"j6984981a43585\">This one can't</h8>";

  private static final String filteredHTags =
    "<h2 class=\"abstractTitle text-title my-1\" >Abstract</h2>" +
    "<h3 >a. Satellite data</h3>" +
    "<h4 >On what scale does lightning enhancement occur?</h4>" +
    "<h5 >Does this even happen?</h5>" +
    "<h8 >This one can't</h8>";

  private static final String debugDiv =
    "<body>" +
    "<div id=\"debug\" style=\"display: none\">" +
    "  <ul>" +
    "    <li id=\"xForwarded\">[171.66.236.212]</li>" +
    "    <li id=\"modifiedRemoteAddr\">171.66.236.212</li>" +
    "  </ul>" +
    "</div>" +
    "</body>" ;

  private static final String filteredDebugDiv =
    "<body>" +
    "</body>" ;

  protected static final String divDP =
    "<div data-popover-fullscreen=\"false\" data-popover-placement=\"\" data-popover-breakpoints=\"\" data-popover=\"607a919f-a0fd-41c2-9100-deaaff9a0862\" class=\"position-absolute display-none\"></div>";

  protected static final String filteredDivDP =
    "<div data-popover-fullscreen=\"false\" data-popover-placement=\"\" data-popover-breakpoints=\"\" class=\"position-absolute display-none\"></div>";

  protected static final String buttonDPA =
    "<button data-popover-anchor=\"0979a884-7df8-4d05-a54\"><span>ClickME!</span></button>";

  protected static final String filteredButtonDPA =
    "<button ><span>ClickME!</span></button>";

  protected static final String divClassContainer =
    "<div class=\"component component-content-item component-container container-body container-tabbed container-wrapper-43131\">" +
    "<p>Some text we might want.</p>" +
    "</div>";

  protected static final String filteredDivClassContainer =
    "<div >" +
    "<p>Some text we might want.</p>" +
    "</div>";

  protected static final String divIDContainer =
    "<div id=\"container-43131-item-43166\" class=\"container-item\"></div>";

  protected static final String filteredDivIDContainer =
    "<div class=\"container-item\"></div>";

  protected static final String navIDcontainer =
    "<nav data-container-tab-address=\"tab_body\" id=\"container-nav-43131\" class=\"container-tabs\"></nav>";

  protected static final String filteredNavIDcontainer =
    "<nav data-container-tab-address=\"tab_body\" class=\"container-tabs\"></nav>";

  protected static final String aDataTabIdHrefContainer =
    "<a data-tab-id=\"abstract-display\" title=\"\" href=\"#container-43131-item-43130\" tabIndex=\"0\" role=\"button\" type=\"button\" class=\" c-Button c-Button--medium \"></a>";

  protected static final String filteredADataTabIdHrefContainer =
    "<a title=\"\" tabIndex=\"0\" role=\"button\" type=\"button\" class=\" c-Button c-Button--medium \"></a>";

  protected static final String imgSrcHash =
    "<img id=\"textarea_icon\" class=\"t-error-icon t-invisible\" alt=\"\" src=\"/assets/b76ba41532fd3780cf2469d2455825eb6f606227/core/spacer.gif\"/>";

  protected static final String filteredImgSrcHash =
    "<img id=\"textarea_icon\" class=\"t-error-icon t-invisible\" alt=\"\"/>";

  protected static final String manyForms = 
    "<div class=\"shell\">" +
    "  <div class=\"t-zone\" id=\"viewAnnotationZone\">" +
    "  </div>" +
    "  <form name=\"viewAnnotationForm\" class=\"annotationsForm hidden\" onsubmit=\"javascript:return Tapestry.waitForPage(event);\" action=\"/configurable/contentpage.viewannotations.viewannotationform?t:ac=journals$002fatsc$002f5$002f4$002fatsc.5.issue-4.xml&amp;t:state:client=H4sIAAAAAAAAAIWVz2sTQRTHJ/2hLSnWBkMLolQoCIobQRSrgtgmxdi0BlI8eJvdfdmMTmbWmbdpcxG92IMHL3rz4MFj/xWPHryJnvUkCD05M6mmke40kGVnv5957+2b+e7s/SCT2xOEkIpWZF2qJKApjToQIE1Bo+pfD5hAUILyQIPqsQh0sMoZCGyC0kyjuVtjwOMWSkUTqHdTvrQO/S+/5t+c/L6/O0bGG6QYyW4qhUHrMZJS4wnt0QqnIqm0UDGR3G6Q6bYNskm78Iw8J4UGmUpNtH/jnRTJciRFmyWZoiEHc29TWyiUcT/oSZ51gWmdgQYjm2oOnqGMkBQNyARF2LKjhcORKquDUE0TyjRhzhYX2OKCFSk5UPFpUb34/H7/5xgpPCaTPcoz2EmJtnURexlHUo6oaCqZKNCa9aDKdMpp34qTjjsxis/qjty+F5le6ropRR8CCyNg0YEZdqTSudFmLNSM2w0mnh5H2RUSbZlLzbmEClnEYTPrhqD8AVtZiAw55FLuDWoxQ98buKwPBWcCmllYNcuUi05ZtK61OI4I84m5YU2M8q1+mp9u2nVNmW3vX8yaMDtNUWQyP+0pC27ZbjVoCDyXK0eSc5pq2JRigzLzR/SsQ2n4NsZ321LF+Y0uuzUDxUA/cubwV+IaVYU2zTiuSdWlmIuecRunRxm3nhrA+XXMdunOfaCx8X4VUuwMwBHz1Y0pE1Clbx8+/n65e9OYr/7XfIqcHnKDXfpq79254tuvr8cIMd40v9GtGmVKGYsfymWva0MQyWXf1wXtQ9k+eOymzBxt2g17mfVopaOb4rSyZ96CZ95Zz7zznnkXPNqSR7vo0S55arnimXfVo13zaDc82rJHu+PR7nq0FY9WO3JvOdnYn5vTsibMmdrC/sEXE8m8toNb7uBaNEfVoqWMLf6LPOHJOu3RHoxWVPgDIcPh3vAHAAA=\" method=\"post\" id=\"viewAnnotationForm\"><div class=\"t-invisible\"><input value=\"journals$002fatsc$002f5$002f4$002fatsc.5.issue-4.xml\" name=\"t:ac\" type=\"hidden\"><input value=\"H4sIAAAAAAAAAIWVz2sTQRTHJ/2hLSnWBkMLolQoCIobQRSrgtgmxdi0BlI8eJvdfdmMTmbWmbdpcxG92IMHL3rz4MFj/xWPHryJnvUkCD05M6mmke40kGVnv5957+2b+e7s/SCT2xOEkIpWZF2qJKApjToQIE1Bo+pfD5hAUILyQIPqsQh0sMoZCGyC0kyjuVtjwOMWSkUTqHdTvrQO/S+/5t+c/L6/O0bGG6QYyW4qhUHrMZJS4wnt0QqnIqm0UDGR3G6Q6bYNskm78Iw8J4UGmUpNtH/jnRTJciRFmyWZoiEHc29TWyiUcT/oSZ51gWmdgQYjm2oOnqGMkBQNyARF2LKjhcORKquDUE0TyjRhzhYX2OKCFSk5UPFpUb34/H7/5xgpPCaTPcoz2EmJtnURexlHUo6oaCqZKNCa9aDKdMpp34qTjjsxis/qjty+F5le6ropRR8CCyNg0YEZdqTSudFmLNSM2w0mnh5H2RUSbZlLzbmEClnEYTPrhqD8AVtZiAw55FLuDWoxQ98buKwPBWcCmllYNcuUi05ZtK61OI4I84m5YU2M8q1+mp9u2nVNmW3vX8yaMDtNUWQyP+0pC27ZbjVoCDyXK0eSc5pq2JRigzLzR/SsQ2n4NsZ321LF+Y0uuzUDxUA/cubwV+IaVYU2zTiuSdWlmIuecRunRxm3nhrA+XXMdunOfaCx8X4VUuwMwBHz1Y0pE1Clbx8+/n65e9OYr/7XfIqcHnKDXfpq79254tuvr8cIMd40v9GtGmVKGYsfymWva0MQyWXf1wXtQ9k+eOymzBxt2g17mfVopaOb4rSyZ96CZ95Zz7zznnkXPNqSR7vo0S55arnimXfVo13zaDc82rJHu+PR7nq0FY9WO3JvOdnYn5vTsibMmdrC/sEXE8m8toNb7uBaNEfVoqWMLf6LPOHJOu3RHoxWVPgDIcPh3vAHAAA=\" name=\"t:state:client\" type=\"hidden\"><input value=\"H4sIAAAAAAAAAEWOsQ4BQRBAl4RGNJQahXqvIkIj0ZCIXHKdbu52rJW7mcvOOHyBn/EBfso/uE71XvXy3l/Tuy/NomA6B3+LkJeYbJkUSVPwuGoC3oGIFTQwif17cCLRbDh6CzUUF7QKNYrG59wWHLEMecuqZmpbYnfBOaRZGrlAkeyWV0GkzZxe0/Fj8ul3TedgBu2HRi6PUKGa0eEKDSQlkE8yjYH8+lGrGf4f9k5+oCj8W8IAAAA=\" name=\"t:formdata\" type=\"hidden\"></div>" +
    "    <input name=\"annotationIds\" type=\"hidden\">" +
    "    <input value=\"/view/journals/atsc/5/4/atsc.5.issue-4.xml\" name=\"redirectOnDeleteSuccess\" type=\"hidden\">" +
    "  </form>" +
    "  <form name=\"editAnnotationForm\" class=\"annotationsForm hidden\" onsubmit=\"javascript:return Tapestry.waitForPage(event);\" action=\"/configurable/contentpage.viewannotations.editannotationform?t:ac=journals$002fatsc$002f5$002f4$002fatsc.5.issue-4.xml&amp;t:state:client=H4sIAAAAAAAAAIWVz2sTQRTHJ/2hLSnWBkMLolQoCIobQRSrgtgmxdi0BlI8eJvdfdmMTmbWmbdpcxG92IMHL3rz4MFj/xWPHryJnvUkCD05M6mmke40kGVnv5957+2b+e7s/SCT2xOEkIpWZF2qJKApjToQIE1Bo+pfD5hAUILyQIPqsQh0sMoZCGyC0kyjuVtjwOMWSkUTqHdTvrQO/S+/5t+c/L6/O0bGG6QYyW4qhUHrMZJS4wnt0QqnIqm0UDGR3G6Q6bYNskm78Iw8J4UGmUpNtH/jnRTJciRFmyWZoiEHc29TWyiUcT/oSZ51gWmdgQYjm2oOnqGMkBQNyARF2LKjhcORKquDUE0TyjRhzhYX2OKCFSk5UPFpUb34/H7/5xgpPCaTPcoz2EmJtnURexlHUo6oaCqZKNCa9aDKdMpp34qTjjsxis/qjty+F5le6ropRR8CCyNg0YEZdqTSudFmLNSM2w0mnh5H2RUSbZlLzbmEClnEYTPrhqD8AVtZiAw55FLuDWoxQ98buKwPBWcCmllYNcuUi05ZtK61OI4I84m5YU2M8q1+mp9u2nVNmW3vX8yaMDtNUWQyP+0pC27ZbjVoCDyXK0eSc5pq2JRigzLzR/SsQ2n4NsZ321LF+Y0uuzUDxUA/cubwV+IaVYU2zTiuSdWlmIuecRunRxm3nhrA+XXMdunOfaCx8X4VUuwMwBHz1Y0pE1Clbx8+/n65e9OYr/7XfIqcHnKDXfpq79254tuvr8cIMd40v9GtGmVKGYsfymWva0MQyWXf1wXtQ9k+eOymzBxt2g17mfVopaOb4rSyZ96CZ95Zz7zznnkXPNqSR7vo0S55arnimXfVo13zaDc82rJHu+PR7nq0FY9WO3JvOdnYn5vTsibMmdrC/sEXE8m8toNb7uBaNEfVoqWMLf6LPOHJOu3RHoxWVPgDIcPh3vAHAAA=\" method=\"post\" id=\"editAnnotationForm\"><div class=\"t-invisible\"><input value=\"journals$002fatsc$002f5$002f4$002fatsc.5.issue-4.xml\" name=\"t:ac\" type=\"hidden\"><input value=\"H4sIAAAAAAAAAIWVz2sTQRTHJ/2hLSnWBkMLolQoCIobQRSrgtgmxdi0BlI8eJvdfdmMTmbWmbdpcxG92IMHL3rz4MFj/xWPHryJnvUkCD05M6mmke40kGVnv5957+2b+e7s/SCT2xOEkIpWZF2qJKApjToQIE1Bo+pfD5hAUILyQIPqsQh0sMoZCGyC0kyjuVtjwOMWSkUTqHdTvrQO/S+/5t+c/L6/O0bGG6QYyW4qhUHrMZJS4wnt0QqnIqm0UDGR3G6Q6bYNskm78Iw8J4UGmUpNtH/jnRTJciRFmyWZoiEHc29TWyiUcT/oSZ51gWmdgQYjm2oOnqGMkBQNyARF2LKjhcORKquDUE0TyjRhzhYX2OKCFSk5UPFpUb34/H7/5xgpPCaTPcoz2EmJtnURexlHUo6oaCqZKNCa9aDKdMpp34qTjjsxis/qjty+F5le6ropRR8CCyNg0YEZdqTSudFmLNSM2w0mnh5H2RUSbZlLzbmEClnEYTPrhqD8AVtZiAw55FLuDWoxQ98buKwPBWcCmllYNcuUi05ZtK61OI4I84m5YU2M8q1+mp9u2nVNmW3vX8yaMDtNUWQyP+0pC27ZbjVoCDyXK0eSc5pq2JRigzLzR/SsQ2n4NsZ321LF+Y0uuzUDxUA/cubwV+IaVYU2zTiuSdWlmIuecRunRxm3nhrA+XXMdunOfaCx8X4VUuwMwBHz1Y0pE1Clbx8+/n65e9OYr/7XfIqcHnKDXfpq79254tuvr8cIMd40v9GtGmVKGYsfymWva0MQyWXf1wXtQ9k+eOymzBxt2g17mfVopaOb4rSyZ96CZ95Zz7zznnkXPNqSR7vo0S55arnimXfVo13zaDc82rJHu+PR7nq0FY9WO3JvOdnYn5vTsibMmdrC/sEXE8m8toNb7uBaNEfVoqWMLf6LPOHJOu3RHoxWVPgDIcPh3vAHAAA=\" name=\"t:state:client\" type=\"hidden\"><input value=\"H4sIAAAAAAAAAJ3QMUvDUBAH8GtRKdZJcXJxqGui1CDoYhFEoUghuLjIJe8anyTvxfcuTVxc/RZ+AnF27+Dmd/ADuDo5+DJIhoLYTnfc8L/f3fMnLJcHEMRajWVSGIxS8k+0YlI8woQOJ5JKVEozstTKek0vhTVwrE3iYY7xDXmMOVk294EXa0OpjFzNcq1clPXOpBCkeiOjY7I2LKJMWutSrh63N6qt15U2tIbQdQw2Or3AjBjWh7c4QT9FlfghG6mSoypnWGsI56Lsw96/7UwVoyF07uBPd4SWvEHkhhjzqaRU9ELiIt+5nHY/Nt++Z7B38ACtGtf53bEobDAvbOaj0xexP/56em8DVHkZQH9uxvWurQ9aYlhtRgsm1UGdH7walo1mAgAA\" name=\"t:formdata\" type=\"hidden\"></div>" +
    "    <input name=\"annotationId\" type=\"hidden\">" +
    "    <input value=\"/view/journals/atsc/5/4/atsc.5.issue-4.xml\" name=\"redirectOnEditSuccess\" type=\"hidden\">" +
    "    <fieldset class=\"p-3 m-0\">" +
    "      <legend> Edit</legend>" +
    "      <div class=\"annotatedText text-body1 bg-grey-light\"></div>" +
    "      <textarea class=\"annotatedTextarea hidden\" id=\"textarea\" name=\"textarea\"></textarea>" +
    "      <img id=\"textarea_icon\" class=\"t-error-icon t-invisible\" alt=\"\" src=\"/assets/0da462c074e5d4d7efe63f4659803cb9fd0e4ace/core/spacer.gif\">" +
    "      <textarea class=\"comment w-1 border border-radius text-body1\" id=\"textarea_0\" name=\"textarea_0\"></textarea>" +
    "      <img id=\"textarea_0_icon\" class=\"t-error-icon t-invisible\" alt=\"\" src=\"/assets/0da462c074e5d4d7efe63f4659803cb9fd0e4ace/core/spacer.gif\">" +
    "      <p class=\"rules text-caption\">" +
    "        Character limit <span class=\"count\">500</span>/500" +
    "      </p>" +
    "      <div class=\"formActions\">" +
    "        <button value=\" Delete\" class=\"delete button delete-btn c-Button c-Button--contained c-Button--primary\" type=\"button\"> Delete</button>" +
    "        <button value=\" Cancel\" class=\"cancel button cancel-btn c-Button c-Button--outlined c-Button--primary\" type=\"button\"> Cancel</button>" +
    "        <button value=\" Save\" class=\"save button save-btn c-Button c-Button--contained c-Button--primary\" type=\"submit\"> Save</button>" +
    "      </div>" +
    "    </fieldset>" +
    "  </form>" +
    "  <form name=\"annotationsForm\" class=\"annotationsForm hidden\" onsubmit=\"javascript:return Tapestry.waitForPage(event);\" action=\"/configurable/contentpage.viewannotations.annotationsform?t:ac=journals$002fatsc$002f5$002f4$002fatsc.5.issue-4.xml&amp;t:state:client=H4sIAAAAAAAAAIWVz2sTQRTHJ/2hLSnWBkMLolQoCIobQRSrgtgmxdi0BlI8eJvdfdmMTmbWmbdpcxG92IMHL3rz4MFj/xWPHryJnvUkCD05M6mmke40kGVnv5957+2b+e7s/SCT2xOEkIpWZF2qJKApjToQIE1Bo+pfD5hAUILyQIPqsQh0sMoZCGyC0kyjuVtjwOMWSkUTqHdTvrQO/S+/5t+c/L6/O0bGG6QYyW4qhUHrMZJS4wnt0QqnIqm0UDGR3G6Q6bYNskm78Iw8J4UGmUpNtH/jnRTJciRFmyWZoiEHc29TWyiUcT/oSZ51gWmdgQYjm2oOnqGMkBQNyARF2LKjhcORKquDUE0TyjRhzhYX2OKCFSk5UPFpUb34/H7/5xgpPCaTPcoz2EmJtnURexlHUo6oaCqZKNCa9aDKdMpp34qTjjsxis/qjty+F5le6ropRR8CCyNg0YEZdqTSudFmLNSM2w0mnh5H2RUSbZlLzbmEClnEYTPrhqD8AVtZiAw55FLuDWoxQ98buKwPBWcCmllYNcuUi05ZtK61OI4I84m5YU2M8q1+mp9u2nVNmW3vX8yaMDtNUWQyP+0pC27ZbjVoCDyXK0eSc5pq2JRigzLzR/SsQ2n4NsZ321LF+Y0uuzUDxUA/cubwV+IaVYU2zTiuSdWlmIuecRunRxm3nhrA+XXMdunOfaCx8X4VUuwMwBHz1Y0pE1Clbx8+/n65e9OYr/7XfIqcHnKDXfpq79254tuvr8cIMd40v9GtGmVKGYsfymWva0MQyWXf1wXtQ9k+eOymzBxt2g17mfVopaOb4rSyZ96CZ95Zz7zznnkXPNqSR7vo0S55arnimXfVo13zaDc82rJHu+PR7nq0FY9WO3JvOdnYn5vTsibMmdrC/sEXE8m8toNb7uBaNEfVoqWMLf6LPOHJOu3RHoxWVPgDIcPh3vAHAAA=\" method=\"post\" id=\"annotationsForm\"><div class=\"t-invisible\"><input value=\"journals$002fatsc$002f5$002f4$002fatsc.5.issue-4.xml\" name=\"t:ac\" type=\"hidden\"><input value=\"H4sIAAAAAAAAAIWVz2sTQRTHJ/2hLSnWBkMLolQoCIobQRSrgtgmxdi0BlI8eJvdfdmMTmbWmbdpcxG92IMHL3rz4MFj/xWPHryJnvUkCD05M6mmke40kGVnv5957+2b+e7s/SCT2xOEkIpWZF2qJKApjToQIE1Bo+pfD5hAUILyQIPqsQh0sMoZCGyC0kyjuVtjwOMWSkUTqHdTvrQO/S+/5t+c/L6/O0bGG6QYyW4qhUHrMZJS4wnt0QqnIqm0UDGR3G6Q6bYNskm78Iw8J4UGmUpNtH/jnRTJciRFmyWZoiEHc29TWyiUcT/oSZ51gWmdgQYjm2oOnqGMkBQNyARF2LKjhcORKquDUE0TyjRhzhYX2OKCFSk5UPFpUb34/H7/5xgpPCaTPcoz2EmJtnURexlHUo6oaCqZKNCa9aDKdMpp34qTjjsxis/qjty+F5le6ropRR8CCyNg0YEZdqTSudFmLNSM2w0mnh5H2RUSbZlLzbmEClnEYTPrhqD8AVtZiAw55FLuDWoxQ98buKwPBWcCmllYNcuUi05ZtK61OI4I84m5YU2M8q1+mp9u2nVNmW3vX8yaMDtNUWQyP+0pC27ZbjVoCDyXK0eSc5pq2JRigzLzR/SsQ2n4NsZ321LF+Y0uuzUDxUA/cubwV+IaVYU2zTiuSdWlmIuecRunRxm3nhrA+XXMdunOfaCx8X4VUuwMwBHz1Y0pE1Clbx8+/n65e9OYr/7XfIqcHnKDXfpq79254tuvr8cIMd40v9GtGmVKGYsfymWva0MQyWXf1wXtQ9k+eOymzBxt2g17mfVopaOb4rSyZ96CZ95Zz7zznnkXPNqSR7vo0S55arnimXfVo13zaDc82rJHu+PR7nq0FY9WO3JvOdnYn5vTsibMmdrC/sEXE8m8toNb7uBaNEfVoqWMLf6LPOHJOu3RHoxWVPgDIcPh3vAHAAA=\" name=\"t:state:client\" type=\"hidden\"><input value=\"H4sIAAAAAAAAAJ2SMUsDMRiGvxZ0qFLQ4uTiUNcrVo9iLWIRxaFI4RDERXJ3X6+Ra3ImX3vn4uq/8BeIs3sHN/+DP8DVycFcS2ulIPamhASePO/35ukDluJDaHhSdHjQV8wNsXIsBaGgNguwPuAYMyEkMeJSaEtjiN5oS0xREjHqagVHUgUWi5jXRYtYhJrUnW15UmHIXbP2IikMUVtn3PdRlNtKeqi103d7XGtDu3rYKiWbL8t5yLVgxdiQkuE56yHBeuuGDVglZCKoOKS4CA6SiKA0NXFSk8u2MYkbUF88CQp/nOMW7gEI1qY3J8Ifc/ehlnFCE2jxt25cAzuT6YS3OisZ27D7bxphYgyQXe+Y4uw/i3OZRqvpmkPm0SnH0C87SP1o+2K48r7x+jXXViqXS9sp/LySXa65qNzctxo++3udz8e3PEASZROpjiZemI1UzUhKQcVvGCcljnIDAAA=\" name=\"t:formdata\" type=\"hidden\"></div>" +
    "    <input name=\"selectionStartXPath\" type=\"hidden\">" +
    "    <input name=\"selectionEndXPath\" type=\"hidden\">" +
    "    <input value=\"0\" name=\"selectionStart\" type=\"hidden\">" +
    "    <input value=\"0\" name=\"selectionEnd\" type=\"hidden\">" +
    "    <input value=\"/view/journals/atsc/5/4/atsc.5.issue-4.xml\" name=\"redirectOnCreateSuccess\" type=\"hidden\">" +
    "    <fieldset class=\"p-3 m-0\">" +
    "      <legend>@!</legend>" +
    "      <div class=\"annotatedText p-2 mb-2 bg-grey-light border-radius text-body1\"></div>" +
    "      <textarea class=\"annotatedTextarea hidden\" id=\"textarea_1\" name=\"textarea_1\"></textarea><img id=\"textarea_1_icon\" class=\"t-error-icon t-invisible\" alt=\"\" src=\"/assets/0da462c074e5d4d7efe63f4659803cb9fd0e4ace/core/spacer.gif\">" +
    "      <textarea class=\"comment w-1 border border-radius text-body1\" id=\"textarea_2\" name=\"textarea_2\"></textarea><img id=\"textarea_2_icon\" class=\"t-error-icon t-invisible\" alt=\"\" src=\"/assets/0da462c074e5d4d7efe63f4659803cb9fd0e4ace/core/spacer.gif\">" +
    "      <p class=\"rules text-caption\">" +
    "        Character limit <span class=\"count\">500</span>/500" +
    "      </p>" +
    "      <div class=\"formActions text-align-right\">" +
    "        <button value=\"Cancel\" class=\"cancel button cancel-btn c-Button c-Button--outlined c-Button--primary\" type=\"button\">" +
    "          Cancel" +
    "        </button>" +
    "        <button value=\"Save\" class=\"save button save-btn c-Button c-Button--contained c-Button--primary\" type=\"submit\">" +
    "          Save" +
    "        </button>" +
    "      </div>" +
    "    </fieldset>" +
    "  </form>" +
    "</div>";

  protected static final String filteredManyForms =
    "<div class=\"shell\">" +
     " <div class=\"t-zone\" id=\"viewAnnotationZone\">" +
     " </div>" +
    " </div>";

  private static final String searchBox =
    "<div class=\"module searchModule search-within s-m-0\">" +
    "  <form class=\"search-form single-query-form doc-search-form c-FormControl c-FormControl--dense c-FormControl--fullWidth\" data-component-formcontrol=\"quickSearchTextfield\" onsubmit=\"javascript:return Tapestry.waitForPage(event);\" action=\"/configurable/contentpage.configurablecontentpagebody.searchwithinform.form?t:ac=journals$002fatsc$002f5$002f4$002fatsc.5.issue-4.xml&amp;t:state:client=H4sIAAAAAAAAAIWTsW/TQBTGLy2BVkGURERlQWJgdiYkEBM0jbBqQqREDGzP9otzcL4zd89OsiBY6MDAAhsDA2P/FUYGNgQzTEhInbhzgCSDU0s+3dk/vffde+87+cHq03OMsX2j2ZHSiQcZRBP0CDI0pOc3PS4JtQThGdQFj9B4B4KjpAFqww3ZXY+jiIekNCTop5m4cYTzL7/231z4fnq8xbYD1ohUmilpUT8m1gqeQAEdATLpDElzmdwJ2O7YBelDis/Yc1YL2E5mo/0/zzJityMlxzzJNYQC7d6ldlCo4rlXKJGnyI3J0aD9bdX8/UYqItawIJdAOHKnq6uROgeLUAMbyhah6cR5Tpx3TymBID9d1y8+vz/9ucVqj1m9AJHjLGPG6WJu2SbWjkAOtEo0GsML7HKTCZi7n/WSO7+O75mJmt6NbC2Nb6WYFbC2BjZKMKeJ0qYy2kUHDeJxwOXTsyjXITlWlVSzTKiJRwL7eRqi3hxwmIfESWAlVd7gMOa06QZl1odScImDPOzaNlWiOw71jZFnEWE10Vxq4iBG86w63W5ZNW3HfnMzD6WdNA3EVXXaSw4cuWoFEKKo5NqREgIyg30lHwC3L9GGPrSWt7G+myodVxe6XfYMNUfzqDTHZiVlobo4hlxQT+kUqBK9Ug5OAVw4Ty3gah17KczuI8TW+13MaLIA18znW1MmqFvfPnz8/fL4ljWf/898ml1ecospfXXy7lrj7dfXW4xZb9pnfVSjXGtr8ZVcbu058A+rT4Z9AgUAAA==\" method=\"post\" id=\"form\"><div class=\"t-invisible\"><input value=\"journals$002fatsc$002f5$002f4$002fatsc.5.issue-4.xml\" name=\"t:ac\" type=\"hidden\"><input value=\"H4sIAAAAAAAAAIWTsW/TQBTGLy2BVkGURERlQWJgdiYkEBM0jbBqQqREDGzP9otzcL4zd89OsiBY6MDAAhsDA2P/FUYGNgQzTEhInbhzgCSDU0s+3dk/vffde+87+cHq03OMsX2j2ZHSiQcZRBP0CDI0pOc3PS4JtQThGdQFj9B4B4KjpAFqww3ZXY+jiIekNCTop5m4cYTzL7/231z4fnq8xbYD1ohUmilpUT8m1gqeQAEdATLpDElzmdwJ2O7YBelDis/Yc1YL2E5mo/0/zzJityMlxzzJNYQC7d6ldlCo4rlXKJGnyI3J0aD9bdX8/UYqItawIJdAOHKnq6uROgeLUAMbyhah6cR5Tpx3TymBID9d1y8+vz/9ucVqj1m9AJHjLGPG6WJu2SbWjkAOtEo0GsML7HKTCZi7n/WSO7+O75mJmt6NbC2Nb6WYFbC2BjZKMKeJ0qYy2kUHDeJxwOXTsyjXITlWlVSzTKiJRwL7eRqi3hxwmIfESWAlVd7gMOa06QZl1odScImDPOzaNlWiOw71jZFnEWE10Vxq4iBG86w63W5ZNW3HfnMzD6WdNA3EVXXaSw4cuWoFEKKo5NqREgIyg30lHwC3L9GGPrSWt7G+myodVxe6XfYMNUfzqDTHZiVlobo4hlxQT+kUqBK9Ug5OAVw4Ty3gah17KczuI8TW+13MaLIA18znW1MmqFvfPnz8/fL4ljWf/898ml1ecospfXXy7lrj7dfXW4xZb9pnfVSjXGtr8ZVcbu058A+rT4Z9AgUAAA==\" name=\"t:state:client\" type=\"hidden\"><input value=\"H4sIAAAAAAAAAFvzloG1PI4hJjk/Ly0zvbQoMSknVd85P68kNa8kIDE91QpZIhkiXgAUT8pPqdQrTk0sSs4ozyzJyMxLyy/KRREoqSxITcvMKUktKi5isMgvStdLLEhMzkjVK0ksSC0uKao01UvOL0rNyUwC0rkF+XlAk4v1ghJTMvPdi/JLC1SCU0tKCyYa7NPZzPf5EBMDow8DN8gBRfk5fom5qSUMQj5ZiWWJ+jmJeen6wSVFmXnp1hUFJQxiEEeEgx0RAnSEG9gRdPCkCRmeNDxVcerUpuC/+5gYGCoKyn0YvKjjyJz8/AKgk/SIdZIPUL2Kqa7/yWuaDNUnB4NjDCOfrP2ctv3yQto4xo8kxwQB5YFSwSWJJaluRfm5wSBeinNOJlBJWGJOaaru9ENXOhgU/kLTKUKikKGOgRGULlk9g4NDXQc4UE0+FaRpuouttqdFoIK8ylXCwO7lHxrk5+hDXdNBhvMNcOiZGQh8sIhb0mMLDj0A7ZzZFTsFAAA=\" name=\"t:formdata\" type=\"hidden\"></div>" +
    "    <input value=\"/journals/atsc/atsc-overview.xml\" name=\"source\" id=\"source\" type=\"hidden\">" +
    "    <div class=\"form-row\">" +
    "      \"<input placeholder=\\\"Search within Journal...\\\" data-component-textfield=\\\"{&quot;id&quot;:&quot;quickSearchTextfield&quot;, &quot;labelText&quot;:&quot;Search within Journal...&quot;}\\\" class=\\\"form-control search-field c-InputField\\\" name=\\\"q\\\" id=\\\"q_within\\\" type=\\\"text\\\">\" +\n<input placeholder=\"Search within Journal...\" data-component-textfield=\"{&quot;id&quot;:&quot;quickSearchTextfield&quot;, &quot;labelText&quot;:&quot;Search within Journal...&quot;}\" class=\"form-control search-field c-InputField\" name=\"q\" id=\"q_within\" type=\"text\">" +
    "      <button data-component-textfield-righticon=\"quickSearchTextfield\" class=\"button submit-btn search-button c-InputAffix c-IconButton c-IconButton--search\" type=\"submit\">" +
    "        <span>Search</span>" +
    "      </button>" +
    "    </div>" +
    "    <input name=\"issueUri\" type=\"hidden\">" +
    "    <fieldset class=\"form-group radio-group\">" +
    "      <label class=\"form-label radio-label\">" +
    "        <input value=\"ISSUE\" name=\"searchWithinTypeFilter\" id=\"radio\" type=\"radio\"> Issue" +
    "      </label>" +
    "      <label class=\"form-label radio-label\">" +
    "        <input value=\"JOURNAL\" name=\"searchWithinTypeFilter\" id=\"radio_0\" type=\"radio\"> Journal" +
    "      </label>" +
    "    </fieldset>" +
    "  </form>" +
    "</div>";

  private static final String filteredSearchBox =
    "";

  private static final String aDataTabId =
    "<a data-tab-id=\"previewPdf-43621\" title=\"\" tabIndex=\"0\" role=\"button\" type=\"button\" class=\" c-Button c-Button--medium \">Link Text</a>";

  private static final String filteredADataTabId =
    "<a title=\"\" tabIndex=\"0\" role=\"button\" type=\"button\" class=\" c-Button c-Button--medium \">Link Text</a>";

  private static final String amsCopyRightP =
    "<contributor-notes encoding-type=\"nlm\">" +
      "<div id=\"n101\">" +
        "<p>" +
          "<sup>" +
            "<label xmlns:ifp=\"http://www.ifactory.com/press\">" +
              "<inline-graphic xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:href=\"wafD190218-op.gif\"></inline-graphic>" +
            "</label>" +
          "</sup>" +
          " Denotes content that is immediately available upon publication as open access." +
        "</p>" +
      "</div>" +
      "<p>" +
        "� 2020 American Meteorological Society. For information regarding reuse of this content and general copyright information, consult the " +
        "<a href=\"http://www.ametsoc.org/PUBSReuseLicenses\" target=\"_blank\">AMS Copyright Policy</a>" +
        " (" +
        "<a href=\"http://www.ametsoc.org/PUBSReuseLicenses\" target=\"_blank\">www.ametsoc.org/PUBSReuseLicenses</a>" +
        ")." +
      "</p>" +
      "<corresp xmlns:ifp=\"http://www.ifactory.com/press\" id=\"cor1\">" +
        "<em>Corresponding author</em>: Byung-Ju Sohn, " +
        "<a href=\"mailto:sohn@snu.ac.kr\">sohn@snu.ac.kr</a>" +
      "</corresp>" +
    "</contributor-notes>";

  private static final String filteredAmsCopyRightP =
      "<contributor-notes encoding-type=\"nlm\">" +
        "<div id=\"n101\">" +
          "<p>" +
            "<sup>" +
              "<label xmlns:ifp=\"http://www.ifactory.com/press\">" +
                "<inline-graphic xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:href=\"wafD190218-op.gif\"></inline-graphic>" +
              "</label>" +
            "</sup>" +
            " Denotes content that is immediately available upon publication as open access." +
          "</p>" +
        "</div>" +
        "<corresp xmlns:ifp=\"http://www.ifactory.com/press\" id=\"cor1\">" +
          "<em>Corresponding author</em>" +
          ": Byung-Ju Sohn, " +
          "<a href=\"mailto:sohn@snu.ac.kr\">sohn@snu.ac.kr</a>" +
        "</corresp>" +
      "</contributor-notes>";

  private static final String ifpBody =
    "<div id=\"s1\" class=\"section border-bottom border-bottom-solid border-bottom-medium\">" +
      "<ifp:body xmlns:ifp=\"http://www.ifactory.com/press\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" encoding-type=\"nlm-3.0\">" +
      "<body>" +
        "<h2>"+
      "</body>" +
      "</ifp:body>" +
    "</div>";

  private static final String filterifpBody =
    "<div id=\"s1\" class=\"section border-bottom border-bottom-solid border-bottom-medium\">" +
      "<body>" +
        "<h2>"+
      "</body>" +
    "</div>";

  private static final String figureImgSrc =
    "<figure>" +
      "<figcaption>" +
        "<span style=\"font-variant: small-caps;\">" +
        "Fig</span>" +
        ". 2." +
      "</figcaption>" +
      "<img data-image-src=\"/view/journals/phoc/50/9/full-jpoD190077-f2.jpg\" height=\"625\" width=\"1023\" src=\"/skin/8f11d65e036447dfc696ad934586604015e8c19b/img/Blank.svg\" class=\"lazy-load\" alt=\"Fig. 2.\"/>" +
    "</figure>";

  private static final String filteredFigureImgSrc =
    "<figure>" +
      "<figcaption>" +
        "<span style=\"font-variant: small-caps;\">" +
        "Fig</span>" +
        ". 2." +
      "</figcaption>" +
      "<img data-image-src=\"/view/journals/phoc/50/9/full-jpoD190077-f2.jpg\" height=\"625\" width=\"1023\" class=\"lazy-load\" alt=\"Fig. 2.\"/>" +
    "</figure>";

  private static final String offSiteText =
    "<ul>" +
      "<li>" +
        "<span class=\" text-body1 off-site-access-message-text \">" +
          "When you leave your institution you will be able to access content until August 26th on this device. Coming back to your campus will extend your access." +
        "</span>" +
      "</li>" +
      "<li>" +
        "<span class=\" off-site-access-message-spacers \">" +
        "</span>" +
      "</li>" +
    "</ul>";

  private static final String filteredOffSiteText =
    "<ul>" +
      "<li>" +
      "</li>" +
      "<li>" +
      "</li>" +
    "</ul>";


  public void testFiltering() throws Exception {
	    doFilterTest(bau, fact, blockHtml, blockFiltered);
  }
  public void testHTagsFiltering() throws Exception {
    doFilterTest(bau, fact, hTags, filteredHTags);
  }
  public void testDebugFiltering() throws Exception {
    doFilterTest(bau, fact, debugDiv, filteredDebugDiv);
  }
  public void testUlDataMenuFiltering() throws Exception {
    doFilterTest(bau, fact, ulDataMenu, filteredUlDataMenu);
  }
  public void testLiDataMenuFiltering() throws Exception {
    doFilterTest(bau, fact, liDataMenu, filteredLiDataMenu);
  }
  public void testDivDPFiltering() throws Exception {
    doFilterTest(bau, fact, divDP, filteredDivDP);
  }
  public void testButtonDPAFiltering() throws Exception {
    doFilterTest(bau, fact, buttonDPA, filteredButtonDPA);
  }
  public void testDivClassContainerFiltering() throws Exception {
    doFilterTest(bau, fact, divClassContainer, filteredDivClassContainer);
  }
  public void testDivIDContainerFiltering() throws Exception {
    doFilterTest(bau, fact, divIDContainer, filteredDivIDContainer);
  }
  public void testNavIdContainerFiltering() throws Exception {
    doFilterTest(bau, fact, navIDcontainer, filteredNavIDcontainer);
  }
  public void testAHrefContainerFiltering() throws Exception {
    doFilterTest(bau, fact, aDataTabIdHrefContainer, filteredADataTabIdHrefContainer);
  }
  /*
  public void testImgSrcHashFiltering() throws Exception {
    doFilterTest(bau, fact, imgSrcHash, filteredImgSrcHash);
  }
  */
  public void testManyFormsFiltering() throws Exception {
    doFilterTest(bau, fact, manyForms, filteredManyForms);
  }
  public void testSearchBoxFiltering() throws Exception {
    doFilterTest(bau, fact, searchBox, filteredSearchBox);
  }
  public void testADataTabIdFiltering() throws Exception {
    doFilterTest(bau, fact, aDataTabId, filteredADataTabId);
  }
  public void testCopyRightFiltering() throws Exception {
    doFilterTest(bau, fact, amsCopyRightP, filteredAmsCopyRightP);
  }
  public void testIFPFiltering() throws Exception {
    doFilterTest(bau, fact, ifpBody, filterifpBody);
  }
  public void testFISFiltering() throws Exception {
    doFilterTest(bau, fact, figureImgSrc, filteredFigureImgSrc);
  }
  public void testOffsiteTextFiltering() throws Exception {
    doFilterTest(bau, fact, offSiteText, filteredOffSiteText);
  }
}