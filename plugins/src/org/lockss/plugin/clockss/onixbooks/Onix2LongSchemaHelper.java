/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.clockss.onixbooks;

import org.lockss.util.*;

/**
 * Define the strings for Onix2 long form. They layout is defined by 
 * the super class, Onix2BaseSchemaHelper
 * @author alexohlson
 *
 */
public class Onix2LongSchemaHelper
extends Onix2BaseSchemaHelper {
  static Logger log = Logger.getLogger(Onix2LongSchemaHelper.class);

  public Onix2LongSchemaHelper() {
    // define the instance variables needed for the super class which contains
    // the layout of the schema (and is shared between long and short
    // versions.
    IDValue_string = "IDValue";
    ContributorRole_string = "ContributorRole";
    NamesBeforeKey_string = "NamesBeforeKey";
    KeyNames_string = "KeyNames";
    PersonName_string = "PersonName";
    PersonNameInverted_string = "PersonNameInverted";
    CorporateName_string = "CorporateName";
    TitleType_string = "TitleType";
    TitleLevel_val = "01";
    TitleText_string = "TitleText";
    Subtitle_string = "Subtitle";
    ProductForm_string = "ProductForm";
    ProductIdentifier_string = "ProductIdentifier";
    ProductIDType_string = "ProductIDType";
    Contributor_string = "Contributor";
    Publisher_string = "Publisher";
    PublisherName_string = "PublisherName";
    PublicationDate_string = "PublicationDate";
    Series_string = "Series";
    SeriesIdentifier_string = "SeriesIdentifier";
    SeriesIDType_string = "SeriesIDType";
    TitleOfSeries_string = "TitleOfSeries";
    Title_string = "Title";
    ONIXMessage_string = "ONIXMessage";
    Product_string = "Product"; 
    RecordReference_string = "RecordReference";

    /* now tell the parent class to define variables that use these strings */
    defineSchemaPaths();
  }
}    