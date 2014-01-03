/*
 * $Id: Onix2NAPXmlMetadataExtractorHelper.java,v 1.1 2014-01-03 16:48:57 alexandraohlson Exp $
 */

/*

 Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.ArrayList;

import org.lockss.util.*;

/**
 *  A helper class that defines a schema for XML metadata extraction for
 *  NAP, which uses Onix 2 Long form
 *  with the filenames based on "<RecordReference>.stamped.pdf"
 *  and no deduplication necessary.
 *  @author alexohlson
 */
public final class Onix2NAPXmlMetadataExtractorHelper
extends Onix2BaseXmlMetadataExtractorHelper {
  static Logger log = Logger.getLogger(Onix2NAPXmlMetadataExtractorHelper.class);
  
  // NAP uses Onix2 Long form so set up the appropriate tags
  public Onix2NAPXmlMetadataExtractorHelper() {
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
  
  /**
   * No deDuplication necessary
   * filenames based on RecordReference which is unique 
   */
  @Override
  public String getDeDuplicationXPathKey() {
    return null;
  }

  /**
   * Filenames all end in ".stamped.pdf"
   */
  @Override
  public ArrayList<String> getFilenameSuffixList() {
    ArrayList<String> returnList = new ArrayList<String>();
    returnList.add(".stamped.pdf");
    return returnList;
  }

  /**
   * The filenames are based on the isbn13 value 
   */
  @Override
  public String getFilenameXPathKey() {
    return RecordReference_string;
  }
  
  }
    