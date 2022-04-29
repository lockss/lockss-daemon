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
 * Define the strings for Onix2 short form. They layout is defined by 
 * the super class, Onix2BaseSchemaHelper
 * @author alexohlson
 *
 */
public class Onix2ShortSchemaHelper
extends Onix2BaseSchemaHelper {
  static Logger log = Logger.getLogger(Onix2ShortSchemaHelper.class);

  /* 
   *  ONIX 2.1 short form specific definitions of instance variables in 
   *  base version of extractor helper 
   */

  public Onix2ShortSchemaHelper() {
    // define the instance variables needed for the super class which contains
    // the layout of the schema (and is shared between long and short
    // versions.
    IDValue_string = "b244";
    ContributorRole_string = "b035";
    NamesBeforeKey_string = "b039";
    KeyNames_string = "b040";
    PersonName_string = "b036";
    PersonNameInverted_string = "b037";
    CorporateName_string = "b047";
    TitleType_string = "b202";
    TitleLevel_val = "01";
    TitleText_string = "b203";
    Subtitle_string = "b029";
    ProductForm_string = "b012";
    ProductIdentifier_string = "productidentifier";
    ProductIDType_string = "b221";
    Contributor_string = "contributor";
    Publisher_string = "publisher";
    PublisherName_string = "b081";
    PublicationDate_string = "b003";
    Series_string = "series";
    SeriesIdentifier_string = "seriesidentifier";
    SeriesIDType_string = "b273";
    TitleOfSeries_string = "b018";
    Title_string = "title";
    ONIXMessage_string = "ONIXMessage";
    Product_string = "product"; 
    RecordReference_string = "a001";

    /* now tell the parent class to define variables that use these strings */
    defineSchemaPaths();
  }
}    