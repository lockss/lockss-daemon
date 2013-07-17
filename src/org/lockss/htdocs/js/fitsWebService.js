/*
 * $Id: fitsWebService.js,v 1.1.2.1 2013-07-17 10:12:46 easyonthemayo Exp $
 */

/*

 Copyright (c) 2013 Board of Trustees of Leland Stanford Jr. University,
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


/**
 * JS to support pages that use the FITS WebService, reducing
 * page load delays; starts an async call to the WebService to run a FITS
 * report, and then processes the results when available and writes them
 * into elements.
 *
 * @param descElId the element that will hold the content type description
 * @param toolsElId an element that will hold description of the tools that identified the type
 * @param auid the id of the AU
 * @param url the URL of the cached file in the AU
 */
function updateWithFitsReport(descElId, toolsElId, auid, url) {
    jQuery.get("ws/FitsService/getAnalysis?url="+encodeURIComponent(url)
        +"&auid="+encodeURIComponent(auid),
        function(data) {
            var $xml = $(jQuery.parseXML(jQuery(data).find("return").text())),
                $ids = $xml.find("identity"),
                typeStrings = [],
                isConflict = $ids.length > 1,
                $descEl = jQuery('#'+descElId),
                $toolsEl = jQuery('#'+toolsElId);

            // Create strings describing the type(s)
            $ids.each(function (index) {
                typeStrings[index] = this.getAttribute("mimetype")
                    .concat(" (")
                    .concat(this.getAttribute("format")+") ");
            });

            if (!isConflict) {
                var toolStrings = getToolsForId($ids[0]);
                $descEl.html(
                    typeStrings[0].concat(" identified by ").concat(
                        toolStrings.join(", "))
                );
            } else {
                // Update description element
                $descEl.html('<span style="color:#a20033">Disagreement: </span>'
                    .concat(typeStrings.join(" vs ")));
                //$descEl.addClass("conflict");

                // Hide elaboration element and fill then show if necessary
                if (isConflict) {
                    $descEl.append(
                     '<button type="button" id="conflict-btn">'
                         .concat('More info')
                         .concat('</button>')
                    );

                    // Add collapsible details
                    $descEl.append('<div id="conflict-info">');
                    $ids.each(function (index) {
                        $("#conflict-info").append(
                            typeStrings[index]
                                .concat(" identified by ")
                                .concat(getToolsForId(this).join(', '))
                                .concat('<br/>')
                        );
                    });
                    $descEl.append('</div>');

                    jQuery("#conflict-info").hide();
                    jQuery("#conflict-btn").click(function() {
                        jQuery("#conflict-info").slideToggle(200);
                    });

                     // Bootstrap approach
                    /*jQuery('#'+descElId).append(
                        '<button type="button" class="btn btn-danger" data-toggle="collapse" data-target="#demo">'
                            .concat('More info')
                            .concat('</button>')
                    );

                    // Add collapsible details
                    jQuery('#'+toolsElId).html('<div id="demo" class="collapse in">');
                    $ids.each(function (index) {
                        jQuery('#'+toolsElId).append(
                            typeStrings[index]
                                .concat(" identified by ")
                                .concat(getToolsForId(this).join(', '))
                                .concat('<br/>')
                        );
                    });
                    jQuery('#'+toolsElId).append('</div>');
                    //jQuery('#'+toolsElId).show();
                    */
                }
            }
        }, 'xml');
}


/**
 * Get a list of descriptions of the tools that made an identification.
 * @param id an identity element in the FITS results XML
 * @returns {Array}
 */
function getToolsForId(id) {
    var toolStrings = [];
    $(id).find("tool").each(function (index) {
        toolStrings[index] = this.getAttribute("toolname")
            .concat(" (v")
            .concat(this.getAttribute("toolversion"))
            .concat(") ");
    });
    return toolStrings;
}
