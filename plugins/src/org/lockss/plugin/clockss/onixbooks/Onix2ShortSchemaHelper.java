/*
 * $Id$
 */

/*

 Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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