/*

Copyright (c) 2000-2020, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.extractor;

import java.util.*;
import org.lockss.util.*;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Describes a field (key-value pair) in metadata: key name, required
 * cardinality and validator, if any.
 */
public class MetadataField {
  
  private static final Logger log = Logger.getLogger(MetadataField.class);
  
  /**
   * A language of the resource. Recommended best practice is to use a
   * controlled vocabulary such as RFC 4646 [RFC4646].
   */
  public static final String KEY_LANGUAGE = "language";
  public static final MetadataField FIELD_LANGUAGE = new MetadataField(
      KEY_LANGUAGE, Cardinality.Single);
  
  /**
   * The file format, physical medium, or dimensions of the resource.
   * Recommended best practice is to use a controlled vocabulary such as the
   * list of Internet Media Types [MIME].
   */
  public static final String KEY_FORMAT = "format";
  public static final MetadataField FIELD_FORMAT = new MetadataField(
      KEY_FORMAT, Cardinality.Single);

  /*
   * The canonical representation of a DOI has key "dc.identifier" and starts
   * with doi:
   */
  public static final String PROTOCOL_DOI = "doi:";
  public static final String KEY_DOI = "doi";
  public static final MetadataField FIELD_DOI = new MetadataField(KEY_DOI,
      Cardinality.Single) {
    @Override
    public String validate(ArticleMetadata am, String val)
        throws MetadataException.ValidationException {
      // normalize away leading "doi:" before checking vaEND_PAGElidity
      String doi = StringUtils.removeStartIgnoreCase(val, PROTOCOL_DOI);
      if (!MetadataUtil.isDoi(doi)) {
        throw new MetadataException.ValidationException("Illegal DOI: " + val);
      }
      return doi;
    }
  };

  public static final String PROTOCOL_ISSN = "issn:";
  public static final String KEY_ISSN = "issn";
  
  private static  Validator issnvalid = new Validator(){
    @Override
    public String validate(ArticleMetadata am,MetadataField field,String val)
        throws MetadataException.ValidationException {
      // normalize away leading "issn:" before checking validity
      String issn = StringUtils.removeStartIgnoreCase(val, PROTOCOL_ISSN);
      if (!MetadataUtil.isIssn(issn)) {
        throw new MetadataException.ValidationException("Illegal ISSN: " + val);
      }
      return issn;
    }
  };
  
  public static final MetadataField FIELD_ISSN = new MetadataField(KEY_ISSN,
      Cardinality.Single,issnvalid);

  public static final String PROTOCOL_EISSN = "eissn:";
  public static final String KEY_EISSN = "eissn";
  private static  Validator eissnvalid = new Validator(){
    @Override
    public String validate(ArticleMetadata am,MetadataField field,String val)
        throws MetadataException.ValidationException {
      // normalize away leading "eissn:" before checking validity
      String issn = StringUtils.removeStartIgnoreCase(val, PROTOCOL_EISSN);
      if (!MetadataUtil.isIssn(issn)) {
        throw new MetadataException.ValidationException("Illegal EISSN: " 
      + val);
      }
      return issn;
    }
  };
  public static final MetadataField FIELD_EISSN = new MetadataField(KEY_EISSN,
      Cardinality.Single,eissnvalid);

  public static final String PROTOCOL_ISBN = "isbn:";
  public static final String KEY_ISBN = "isbn";
  private static  Validator isbnvalid = new Validator(){
    @Override
    public String validate(ArticleMetadata am,MetadataField field, String val)
        throws MetadataException.ValidationException {
      // normalize away leading "isbn:" before checking validity
      String isbn = StringUtils.removeStartIgnoreCase(val, PROTOCOL_ISBN);
      if (!MetadataUtil.isIsbn(isbn, false)) { // ignore publisher malformed
                                               // ISBNs
        throw new MetadataException.ValidationException("Illegal ISBN: " + val);
      }
      return isbn;
    }
  };

  public static final MetadataField FIELD_ISBN = new MetadataField(KEY_ISBN,
      Cardinality.Single,isbnvalid);

  public static final String PROTOCOL_EISBN = "eisbn:";
  public static final String KEY_EISBN = "eisbn";
  private static  Validator eisbnvalid = new Validator(){
    @Override
    public String validate(ArticleMetadata am,MetadataField field, String val)
        throws MetadataException.ValidationException {
      // normalize away leading "isbn:" before checking validity
      String eisbn = StringUtils.removeStartIgnoreCase(val, PROTOCOL_EISBN);
      if (!MetadataUtil.isIsbn(eisbn, false)) { // ignore publisher malformed
                                                // ISBNs
        throw new MetadataException.ValidationException("Illegal EISBN: "
            + val);
      }
      return eisbn;
    }
  };
  
  public static final MetadataField FIELD_EISBN = new MetadataField(KEY_EISBN,
      Cardinality.Single,eisbnvalid);
  
  public static final String KEY_PROVIDER = "provider";
  // You cannot put in an empty string or a null value for publisher
  private static Validator providervalid = new Validator() {
    public String validate(ArticleMetadata am,MetadataField field,String val)
        throws MetadataException.ValidationException {
      // normalize so that it is never set to null or to empty string
      if( (val == null) || val.isEmpty()) {
        throw new MetadataException.ValidationException(
            "Illegal provider: empty string"); 
      }
      return val;
    }
  };
  public static final MetadataField FIELD_PROVIDER = new MetadataField(
      KEY_PROVIDER, Cardinality.Single,providervalid);

  public static final String KEY_PUBLISHER = "publisher";
  // You cannot put in an empty string or a null value for publisher
  private static Validator publishervalid = new Validator() {
    public String validate(ArticleMetadata am,MetadataField field,String val)
        throws MetadataException.ValidationException {
      // normalize so that it is never set to null or to empty string
      if( (val == null) || val.isEmpty()) {
        throw new MetadataException.ValidationException(
            "Illegal publisher: empty string"); 
      }
      return val;
    }
  };
  public static final MetadataField FIELD_PUBLISHER = new MetadataField(
      KEY_PUBLISHER, Cardinality.Single,publishervalid);

  /** Publication is a stand-alone book. */
  public static final String PUBLICATION_TYPE_BOOK = "book";
  /** Publication is part of a book series. */
  public static final String PUBLICATION_TYPE_BOOKSERIES = "bookSeries";
  /** Publication is a journal. */
  public static final String PUBLICATION_TYPE_JOURNAL = "journal";
  /** Publication is a proceedings publication. */
  public static final String PUBLICATION_TYPE_PROCEEDINGS = "proceedings";
  /** Publication is a file. */
  public static final String PUBLICATION_TYPE_FILE = "file";
  /** Publication is an unknown publication. */
  public static final String PUBLICATION_TYPE_UNKNOWN_PUBLICATION =
      "unknown_publication";

  public static final String KEY_PUBLICATION_TYPE = "pubtype";

  /** Publication unit is a journal article */
  public static final String ARTICLE_TYPE_JOURNALARTICLE = "journal_article";
  /** Publication unit is a book chapter */
  public static final String ARTICLE_TYPE_BOOKCHAPTER = "book_chapter";
  /** Publication unit is a whole book or volume */
  public static final String ARTICLE_TYPE_BOOKVOLUME = "book_volume";
  /** Publication unit is a proceedings article */
  public static final String ARTICLE_TYPE_PROCEEDINGSARTICLE =
      "proceedings_article";
  /** Publication unit is a file */
  public static final String ARTICLE_TYPE_FILE = "file";
  /** Publication unit is an unknown article */
  public static final String ARTICLE_TYPE_UNKNOWNARTICLE = "unknown_article";

  public static final String KEY_ARTICLE_TYPE = "articletype";

  // You cannot put in an empty string or a null value for publication type
  private static Validator pubtypevalid = new Validator() {
    public String validate(ArticleMetadata am,MetadataField field,String val)
        throws MetadataException.ValidationException {
      // normalize so that it is never set to null or to empty string
      if( StringUtil.isNullString(val)) {
        throw new MetadataException.ValidationException(
            "Illegal publication type: empty string"); 
      }
      return val;
    }
  };

  /** Publication type, e.g. "book", "bookSeries", "journal" */
  public static final MetadataField FIELD_PUBLICATION_TYPE = new MetadataField(
      KEY_PUBLICATION_TYPE, Cardinality.Single,pubtypevalid);

  /** Publication unit type, e.g. "article" (for journal), "chapter" 
   * (for a book chapter), "volume" (for a whole book or volume) 
   */
  public static final MetadataField FIELD_ARTICLE_TYPE = new MetadataField(
      KEY_ARTICLE_TYPE, Cardinality.Single,pubtypevalid);

  public static final String KEY_VOLUME = "volume";
  public static final MetadataField FIELD_VOLUME = new MetadataField(
      KEY_VOLUME, Cardinality.Single);

  public static final String KEY_ISSUE = "issue";
  public static final MetadataField FIELD_ISSUE = new MetadataField(KEY_ISSUE,
      Cardinality.Single);

  public static final String KEY_START_PAGE = "startpage";
  public static final MetadataField FIELD_START_PAGE = new MetadataField(
      KEY_START_PAGE, Cardinality.Single);

  public static final String KEY_END_PAGE = "endpage";
  public static final MetadataField FIELD_END_PAGE = new MetadataField(
      KEY_END_PAGE, Cardinality.Single);

  /*
   * A date can be just a year, a month and year, or a specific issue date.
   */
  public static final String KEY_DATE = "date";
  public static final MetadataField FIELD_DATE = new MetadataField(KEY_DATE,
      Cardinality.Single);

  public static final String KEY_ARTICLE_TITLE = "article.title";
  public static final MetadataField FIELD_ARTICLE_TITLE = new MetadataField(
      KEY_ARTICLE_TITLE, Cardinality.Single);

  public static final String KEY_PUBLICATION_TITLE = "publication.title";
  /** @deprecated use {@link #KEY_PUBLICATION_TITLE} instead */
  @Deprecated
  public static final String KEY_JOURNAL_TITLE = KEY_PUBLICATION_TITLE;

  // You cannot put in an empty string or a null value for publication title
  private static Validator pubtitlevalid = new Validator() {
    public String validate(ArticleMetadata am,MetadataField field,String val)
        throws MetadataException.ValidationException {
      // Normalize publication title so that it is never set to null or to empty
      // string.
      if( (val == null) || val.isEmpty()) {
        throw new MetadataException.ValidationException(
            "Illegal publication title: empty string"); 
      }
      return val;
    }
  };
  public static final MetadataField FIELD_PUBLICATION_TITLE = new MetadataField(
      KEY_PUBLICATION_TITLE, Cardinality.Single, pubtitlevalid);
  /** @deprecated use {@link #FIELD_PUBLICATION_TITLE} instead */
  @Deprecated
  public static final MetadataField FIELD_JOURNAL_TITLE =
  FIELD_PUBLICATION_TITLE;


  public static final String KEY_SERIES_TITLE = "series.title";

  // You cannot put in an empty string or a null value for publication title
  private static Validator seriestitlevalid = new Validator() {
    public String validate(ArticleMetadata am,MetadataField field,String val)
        throws MetadataException.ValidationException {
      // Normalize publication title so that it is never set to null or to empty
      // string.
      if( (val == null) || val.isEmpty()) {
        throw new MetadataException.ValidationException(
            "Illegal series title: empty string"); 
      }
      return val;
    }
  };

  /** 
   * Proprietary series identifier (e.g. a bookSeries) 
   * for a publication series (e.g. books) 
   */
  public static final MetadataField FIELD_SERIES_TITLE = new MetadataField(
      KEY_SERIES_TITLE, Cardinality.Single, seriestitlevalid);

  public static final String KEY_PROPRIETARY_SERIES_IDENTIFIER =
      "propietary_series_identifier";
  public static final MetadataField FIELD_PROPRIETARY_SERIES_IDENTIFIER =
      new MetadataField(KEY_PROPRIETARY_SERIES_IDENTIFIER, Cardinality.Single);

  /* Author is currently a delimited list of one or more authors. */
  public static final String KEY_AUTHOR = "author";
  private static  Validator authorvalid = new Validator(){
    public String validate(ArticleMetadata am,MetadataField field,String val)
        throws MetadataException.ValidationException {
      // normalize author entries especially with no names .
      // For example : <meta name="citation_authors" content=", "/>
        if(!MetadataUtil.isAuthor(val)) {
          throw new MetadataException.ValidationException("Illegal Author: " 
        + val);
        }
        return val;
        }
   };

  public static final MetadataField FIELD_AUTHOR = new MetadataField(
      KEY_AUTHOR, Cardinality.Multi,authorvalid);
    
  public static final String KEY_ACCESS_URL = "access.url";
  public static final MetadataField FIELD_ACCESS_URL = new MetadataField(
      KEY_ACCESS_URL, Cardinality.Single);
  
  public static final String KEY_FEATURED_URL_MAP = "featured.url.map";
  public static final MetadataField FIELD_FEATURED_URL_MAP = new MetadataField(
    KEY_FEATURED_URL_MAP, Cardinality.Single);

  public static final String KEY_KEYWORDS = "keywords";
  public static final MetadataField FIELD_KEYWORDS = new MetadataField(
      KEY_KEYWORDS, Cardinality.Multi);

  // Dublin code fields. See http://dublincore.org/documents/dces/ for
  // more information on the fields. See also
  // http://scholar.google.com/intl/en/scholar/inclusion.html for
  // recommended usage of citation subfields for serial publications
  // and books. The National Library of Medicine of the National Institutes
  // of Health has also published the NLM Metadata Schema that includes
  // approved Dublin Core elements with approved NLM defined qualifiers.
  // See: http://www.nlm.nih.gov/tsd/cataloging/metafilenew.html.

  /** The chapter of a book (Google Scholar non-standard) */
  public static final String DC_KEY_CITATION_CHAPTER = "dc.citation.chapter";
  public static final MetadataField DC_FIELD_CITATION_CHAPTER = new 
      MetadataField(
      DC_KEY_CITATION_CHAPTER, Cardinality.Single);

  /**
   * The ending page of an article in a serial or a chapter in a book (Google
   * Scholar non-standard).
   */
  public static final String DC_KEY_CITATION_EPAGE = "dc.citation_epage";
  public static final MetadataField DC_FIELD_CITATION_EPAGE = new MetadataField(
      DC_KEY_CITATION_EPAGE, Cardinality.Single);

  /** The issue of a serial publication (Google Scholar non-standard) */
  public static final String DC_KEY_CITATION_ISSUE = "dc.citation.issue";
  public static final MetadataField DC_FIELD_CITATION_ISSUE = new MetadataField(
      DC_KEY_CITATION_ISSUE, Cardinality.Single);

  /**
   * The starting page of an article in a serial or a chapter in a book (Google
   * Scholar non-standard).
   */
  public static final String DC_KEY_CITATION_SPAGE = "dc.citation_spage";
  public static final MetadataField DC_FIELD_CITATION_SPAGE = new MetadataField(
      DC_KEY_CITATION_SPAGE, Cardinality.Single);

  /** The volume of a serial publication (Google Scholar non-standard). */
  public static final String DC_KEY_CITATION_VOLUME = "dc.citation_volume";
  public static final MetadataField DC_FIELD_CITATION_VOLUME =
      new MetadataField(DC_KEY_CITATION_VOLUME, Cardinality.Single);

  /** An entity responsible for making contributions to the resource. */
  public static final String DC_KEY_CONTRIBUTOR = "dc.contributor";
  public static final MetadataField DC_FIELD_CONTRIBUTOR = new MetadataField(
      DC_KEY_CONTRIBUTOR, Cardinality.Multi);

  /**
   * The spatial or temporal topic of the resource, the spatial applicability of
   * the resource, or the jurisdiction under which the resource is relevant.
   */
  public static final String DC_KEY_COVERAGE = "dc.coverage";
  public static final MetadataField DC_FIELD_COVERAGE = new MetadataField(
      DC_KEY_COVERAGE, Cardinality.Single);

  /** An entity primarily responsible for making the resource. */
  public static final String DC_KEY_CREATOR = "dc.creator";
  public static final MetadataField DC_FIELD_CREATOR = new MetadataField(
      DC_KEY_CREATOR, Cardinality.Multi);

  /**
   * A point or period of time associated with an event in the lifecycle of the
   * resource. Recommended best practice is to use an encoding scheme, such as
   * the W3CDTF profile of ISO 8601 [W3CDTF].
   */
  public static final String DC_KEY_DATE = "dc.date";
  public static final MetadataField DC_FIELD_DATE = new MetadataField(
      DC_KEY_DATE, Cardinality.Single);

  /**
   * An account of the resource. May include but is not limited to: an abstract,
   * a table of contents, a graphical representation, or a free-text account of
   * the resource.
   */
  public static final String DC_KEY_DESCRIPTION = "dc.description";
  public static final MetadataField DC_FIELD_DESCRIPTION = new MetadataField(
      DC_KEY_DESCRIPTION, Cardinality.Single);

  /**
   * The file format, physical medium, or dimensions of the resource.
   * Recommended best practice is to use a controlled vocabulary such as the
   * list of Internet Media Types [MIME].
   */
  public static final String DC_KEY_FORMAT = "dc.format";
  public static final MetadataField DC_FIELD_FORMAT = new MetadataField(
      DC_KEY_FORMAT, Cardinality.Single);

  /**
   * An unambiguous reference to the resource within a given context.
   * Recommended best practice is to identify the resource by means of a string
   * conforming to a formal identification system.
   * <p>
   * According to Google Scholar, "If a page shows only the abstract of the
   * paper and you have the full text in a separate file, e.g., in the PDF
   * format, please specify the locations of all full text versions using ...
   * DC.identifier tags. The content of the tag is the absolute URL of the PDF
   * file; for security reasons, it must refer to a file in the same
   * subdirectory as the HTML abstract."
   */
  public static final String DC_KEY_IDENTIFIER = "dc.identifier";
  public static final MetadataField DC_FIELD_IDENTIFIER = new MetadataField(
      DC_KEY_IDENTIFIER, Cardinality.Multi);

  /** The ISSN of the resource (dc qualified: non-standard NIH) */
  public static final String DC_KEY_IDENTIFIER_ISSN = "dc.identifier.issn";
  public static final MetadataField DC_FIELD_IDENTIFIER_ISSN = new
      MetadataField(
      DC_KEY_IDENTIFIER_ISSN, Cardinality.Single);

  /** The EISSN of the resource (dc qualified: non-standard NIH) */
  public static final String DC_KEY_IDENTIFIER_EISSN = "dc.identifier.eissn";
  public static final MetadataField DC_FIELD_IDENTIFIER_EISSN = new 
      MetadataField(
      DC_KEY_IDENTIFIER_EISSN, Cardinality.Single);

  /** The ISSNL of the resource (dc qualified: non-standard NIH) */
  public static final String DC_KEY_IDENTIFIER_ISSNL = "dc.identifier.issnl";
  public static final MetadataField DC_FIELD_IDENTIFIER_ISSNL = new 
      MetadataField(
      DC_KEY_IDENTIFIER_ISSNL, Cardinality.Single);
  
  public static final String DC_KEY_IDENTIFIER_ISSNM = "dc.identifier.issnm";
  public static final MetadataField DC_FIELD_IDENTIFIER_ISSNM = new 
      MetadataField(
      DC_KEY_IDENTIFIER_ISSNM, Cardinality.Multi);  
  

  /** The ISBN of the resource (dc qualified: non-standard NIH) */
  public static final String DC_KEY_IDENTIFIER_ISBN = "dc.identifier.isbn";
  public static final MetadataField DC_FIELD_IDENTIFIER_ISBN = new 
      MetadataField(
      DC_KEY_IDENTIFIER_ISBN, Cardinality.Single);

  /** The ISBN of the resource (dc qualified: non-standard NIH) */
  public static final String DC_KEY_IDENTIFIER_EISBN = "dc.identifier.eisbn";
  public static final MetadataField DC_FIELD_IDENTIFIER_EISBN = new 
      MetadataField(
      DC_KEY_IDENTIFIER_EISBN, Cardinality.Single);

  /**
   * Date of publication, i.e., the date that would normally be cited in
   * references to this paper from other papers. Don't use it for the date of
   * entry into the repository. Provide full dates in the "2010/5/12" format if
   * available; or a year alone otherwise.
   */
  public static final String DC_KEY_ISSUED = "dc.issued";
  public static final MetadataField DC_FIELD_ISSUED = new MetadataField(
      DC_KEY_ISSUED, Cardinality.Single);

  /**
   * A language of the resource. Recommended best practice is to use a
   * controlled vocabulary such as RFC 4646 [RFC4646].
   */
  public static final String DC_KEY_LANGUAGE = "dc.language";
  public static final MetadataField DC_FIELD_LANGUAGE = new MetadataField(
      DC_KEY_LANGUAGE, Cardinality.Single);

  /** An entity responsible for making the resource available. */
  public static final String DC_KEY_PUBLISHER = "dc.publisher";
  public static final MetadataField DC_FIELD_PUBLISHER = new MetadataField(
      DC_KEY_PUBLISHER, Cardinality.Single);

  /**
   * A related resource. Recommended best practice is to identify the related
   * resource by means of a string conforming to a formal identification system.
   */
  public static final String DC_KEY_RELATION = "dc.relation";
  public static final MetadataField DC_FIELD_RELATION = new MetadataField(
      DC_KEY_RELATION, Cardinality.Multi);

  /**
   * The resource of which this resource is a part. For an article in a journal
   * or proceedings, identifies the publication (dc qualified: by Google
   * Scholar).
   */
  public static final String DC_KEY_RELATION_ISPARTOF = "dc.relation.ispartof";
  public static final MetadataField DC_FIELD_RELATION_ISPARTOF = new 
      MetadataField(
      DC_KEY_RELATION_ISPARTOF, Cardinality.Single);

  /**
   * Information about rights held in and over the resource. Typically, rights
   * information includes a statement about various property rights associated
   * with the resource, including intellectual property rights.
   */
  public static final String DC_KEY_RIGHTS = "dc.rights";
  public static final MetadataField DC_FIELD_RIGHTS = new MetadataField(
      DC_KEY_RIGHTS, Cardinality.Single);

  /**
   * A related resource from which the described resource is derived. Typically,
   * the subject will be represented using keywords, key phrases, or
   * classification codes.
   */
  public static final String DC_KEY_SOURCE = "dc.source";
  public static final MetadataField DC_FIELD_SOURCE = new MetadataField(
      DC_KEY_SOURCE, Cardinality.Single);

  /**
   * The topic of the resource. Typically, the subject will be represented using
   * keywords, key phrases, or classification codes.
   */
  public static final String DC_KEY_SUBJECT = "dc.subject";
  public static final MetadataField DC_FIELD_SUBJECT = new MetadataField(
      DC_KEY_SUBJECT, Cardinality.Single);

  /**
   * A name given to the resource. Typically, a Title will be a name by which
   * the resource is formally known.
   */
  public static final String DC_KEY_TITLE = "dc.title";
  public static final MetadataField DC_FIELD_TITLE = new MetadataField(
      DC_KEY_TITLE, Cardinality.Single);

  /**
   * The nature or genre of the resource. Recommended best practice is to use a
   * controlled vocabulary such as the DCMI Type Vocabulary [DCMITYPE]. To
   * describe the file format, physical medium, or dimensions of the resource,
   * use the Format element.
   */
  public static final String DC_KEY_TYPE = "dc.type";
  public static final MetadataField DC_FIELD_TYPE = new MetadataField(
      DC_KEY_TYPE, Cardinality.Single);

  public static final String KEY_COVERAGE = "coverage";
  public static final MetadataField FIELD_COVERAGE =
      new MetadataField(KEY_COVERAGE, Cardinality.Single);

  public static final String KEY_ITEM_NUMBER = "item_number";
  public static final MetadataField FIELD_ITEM_NUMBER =
      new MetadataField(KEY_ITEM_NUMBER, Cardinality.Single);

  public static final String KEY_PROPRIETARY_IDENTIFIER =
      "proprietary_identifier";
  public static final MetadataField FIELD_PROPRIETARY_IDENTIFIER =
      new MetadataField(KEY_PROPRIETARY_IDENTIFIER, Cardinality.Single);

  public static final String KEY_FETCH_TIME = "fetch.time";
  public static final MetadataField FIELD_FETCH_TIME =
      new MetadataField(KEY_FETCH_TIME, Cardinality.Single);

  /**
   * An account of the resource. May include but is not limited to: an abstract,
   * a table of contents, a graphical representation, or a free-text account of
   * the resource.
   */
  public static final String FIELD_KEY_ABSTRACT = "abstract";
  public static final MetadataField FIELD_ABSTRACT = new MetadataField(
      FIELD_KEY_ABSTRACT, Cardinality.Single);

  public static final String KEY_MD_MAP = "md.map";
  public static final MetadataField FIELD_MD_MAP = new MetadataField(
      KEY_MD_MAP, Cardinality.Single);

  // array of fields -- used to populate key-to-object fieldMap.
  // IMPORTANT: update this array when adding a new field!
  private static MetadataField[] fields = { 
    FIELD_ABSTRACT,
    FIELD_ACCESS_URL,
    FIELD_ARTICLE_TITLE,
    FIELD_AUTHOR,
    FIELD_COVERAGE,
    FIELD_DATE,
    FIELD_END_PAGE,
    FIELD_FEATURED_URL_MAP,
    FIELD_FETCH_TIME,
    FIELD_ITEM_NUMBER,
    FIELD_KEYWORDS,
    FIELD_PROPRIETARY_IDENTIFIER,
    FIELD_PROPRIETARY_SERIES_IDENTIFIER,
    FIELD_PUBLICATION_TITLE,
    FIELD_PUBLISHER,
    FIELD_START_PAGE,
    FIELD_VOLUME,
    FIELD_MD_MAP,
    DC_FIELD_CITATION_CHAPTER,
    DC_FIELD_CITATION_EPAGE,
    DC_FIELD_CITATION_ISSUE,
    DC_FIELD_CITATION_SPAGE,
    DC_FIELD_CITATION_VOLUME,
    DC_FIELD_CONTRIBUTOR,
    DC_FIELD_COVERAGE,
    DC_FIELD_CREATOR,
    DC_FIELD_DATE,
    DC_FIELD_DESCRIPTION,
    DC_FIELD_FORMAT,
    DC_FIELD_IDENTIFIER,
    DC_FIELD_IDENTIFIER_ISSN,
    DC_FIELD_IDENTIFIER_EISSN,
    DC_FIELD_IDENTIFIER_ISSNL,
    DC_FIELD_IDENTIFIER_ISSNM,
    DC_FIELD_IDENTIFIER_ISBN,
    DC_FIELD_IDENTIFIER_EISBN,
    DC_FIELD_ISSUED,
    DC_FIELD_LANGUAGE,
    DC_FIELD_PUBLISHER,
    DC_FIELD_RELATION,
    DC_FIELD_RELATION_ISPARTOF,
    DC_FIELD_RIGHTS,
    DC_FIELD_SOURCE,
    DC_FIELD_SUBJECT,
    DC_FIELD_TITLE,
    DC_FIELD_TYPE,
    
  };

  // maps keys to fields
  private static Map<String, MetadataField> fieldMap = 
      new HashMap<String, MetadataField>();
  static {
    for (MetadataField f : fields) {
      fieldMap.put(f.getKey().toLowerCase(), f);
    }
  }

  /**
   * Return the predefined MetadataField with the given key, or null if none
   */
  public static MetadataField findField(String key) {
    return fieldMap.get(key.toLowerCase());
  }

  protected final String key;
  protected final Cardinality cardinality;
  protected final Validator validator;
  protected final Splitter splitter;
  protected final Extractor extractor;

  /**
   * Create a metadata field descriptor with Cardinality.Single
   * 
   * @param key the map key
   */
  public MetadataField(String key) {
    this(key, Cardinality.Single, null, (Splitter) null);
  }

  /**
   * Create a metadata field descriptor
   * 
   * @param key the map key
   * @param cardinality
   */
  public MetadataField(String key, Cardinality cardinality) {
    this(key, cardinality, null, (Splitter) null);
  }

  /**
   * Create a metadata field descriptor
   * 
   * @param key the map key
   * @param cardinality
   * @param validator
   */
  public MetadataField(String key, Cardinality cardinality,
           Validator validator) 
  {
    this(key, cardinality, validator, (Splitter) null);
  }

  /**
   * Create a metadata field descriptor
   * 
   * @param key
   *          the map key
   * @param cardinality
   * @param splitter
   */
  public MetadataField(String key, Cardinality cardinality, 
           Splitter splitter){
    this(key, cardinality, null, splitter);
  }

  /**
   * Create a metadata field descriptor
   * 
   * @param key the map key
   * @param cardinality
   * @param validator
   */
  public MetadataField(String key, Cardinality cardinality,
           Validator validator, Splitter splitter) {
    this.key = key;
    this.cardinality = cardinality;
    this.validator = validator;
    if (cardinality != Cardinality.Multi && splitter != null) {
      throw new IllegalArgumentException(
              "Splitter legal only with Cardinality.Multi");
    }
    this.splitter = splitter;
    this.extractor = null;
  }
  
  /**
   * Create a metadata field descriptor
   * 
   * @param key the map key
   * @param cardinality
   * @param splitter
   */
  public MetadataField(String key, Cardinality cardinality, Extractor extractor)
  {
    this(key, cardinality, null, extractor);
  }

  /**
   * Create a metadata field descriptor
   * 
   * @param key
   *          the map key
   * @param cardinality
   * @param validator
   */
  public MetadataField(String key, Cardinality cardinality,
      Validator validator, Extractor extractor) {
    this.key = key;
    this.cardinality = cardinality;
    this.validator = validator;
    this.splitter = null;
    this.extractor = extractor;
  }

  /**
   * Create a MetadataField that's a copy of another one
   * 
   * @param field
   *          the MetadataField to copy
   * @param splitter
   */
  public MetadataField(MetadataField field) {
    this(field.getKey(), field.getCardinality(), field.getValidator());
  }

  /**
   * Create a MetadataField that's a copy of another one
   * 
   * @param field
   *          the MetadataField to copy
   */
  public MetadataField(MetadataField field, Splitter splitter) {
    this(field.getKey(), field.getCardinality(), field.getValidator(),splitter);
  }

  /**
   * Create a MetadataField that's a copy of another one
   * 
   * @param field
   *          the MetadataField to copy
   */
  public MetadataField(MetadataField field, Extractor extractor) {
    this(field.getKey(), field.getCardinality(), field.getValidator(),
        extractor);
  }

  /** Return the field's key. */
  public String getKey() {
    return key;
  }

  /** Return the field's cardinality. */
  public Cardinality getCardinality() {
    return cardinality;
  }

  private Validator getValidator() {
    return validator;
  }

  /**
   * If a validator is present, apply it to the argument. If valid, return the
   * argument or a normalized value. If invalid, throw
   * MetadataException.ValidationException
   */
  public String validate(ArticleMetadata am, String value)
      throws MetadataException.ValidationException {
    if (validator != null) {
      return validator.validate(am, this, value);
    }
    return value;
  }
 
  /**
   * If a splitter is present, apply it to the argument return a list of
   * strings. If no splitter is present, return a singleton list of the argument
   */
  public List<String> split(ArticleMetadata am, String value) {
    if (splitter != null) {
      return splitter.split(am, this, value);
    }
    return ListUtil.list(value);
  }

  /**
   * If a extractor is present, apply it to the argument return a string. If no
   * extractor is present, return the argument
   */
  public String extract(ArticleMetadata am, String value) {
    if (extractor != null) {
      return extractor.extract(am, this, value);
    }
    return value;
  }

  public boolean hasSplitter() {
    return splitter != null;
  }

  public boolean hasExtractor() {
    return extractor != null;
  }

  /** Cardinality of a MetadataField: single-valued or multi-valued */
  public static enum Cardinality {
    Single, Multi
  };

  static class Default extends MetadataField {
    public Default(String key) {
      super(key, Cardinality.Single);
    }
  }

  /**
   * Validator can be associated with a MetadataField to check and/or normalize
   * values when stored.
   */
  public interface Validator {
    /**
     * Validate and/or normalize value.
     * 
     * @param am
     *          the ArticleMeta being stored into (source of Locale, if
     *          necessary)
     * @param field
     *          the field being stored
     * @param value
     *          the value being stored
     * @return original value or a normalized value to store
     * @throws MetadataField.ValidationException
     *           if the value is illegal for the field
     */
    public String validate(ArticleMetadata am, MetadataField field,String value)
        throws MetadataException.ValidationException;
   }

  /**
   * Splitter can be associated with a MetadataField to split value strings into
   * substring to be stored into a multi-valued field.
   */
  public interface Splitter {
    /**
     * Split a value into a list of values
     * 
     * @param am
     *          the ArticleMeta being stored into (source of Locale, if
     *          necessary)
     * @param field
     *          the field being stored
     * @param value
     *          the value being stored
     * @return list of values
     */
    public List<String> split(ArticleMetadata am, MetadataField field,
        String value);
  }

  /**
   * Return a Splitter that splits substrings separated by the separator string.
   * 
   * @param separator
   *          the separator string
   */
  public static Splitter splitAt(String separator) {
    return new SplitAt(separator, null, null);
  }

  /**
   * Return a Splitter that first removes the delimiter string from the ends of
   * the input, then splits substrings separated by the separator string.
   * 
   * @param separator
   *          the separator string
   * @param delimiter
   *          the delimiter string removed from both ends of the input
   */
  public static Splitter splitAt(String separator, String delimiter) {
    return new SplitAt(separator, delimiter, delimiter);
  }

  /**
   * Return a Splitter that first removes the two delimiter strings from the
   * front and end of the input, respectively, then splits substrings separated
   * by the separator string.
   * 
   * @param separator
   *          the separator string
   * @param delimiter1
   *          the delimiter string removed from the beginning of the input
   * @param delimiter2
   *          the delimiter string removed from the end of the input
   */
  public static Splitter splitAt(String separator, String delimiter1,
      String delimiter2) {
    return new SplitAt(separator, delimiter1, delimiter2);
  }

  /**
   * A Splitter that splits substrings separated by a separator string,
   * optionally after removing delimiters from the beginning and end of the
   * string. Blanks are trimmed from the ends of the input string and from each
   * substring, and empty substrings are discarded.
   */
  public static class SplitAt implements Splitter {
    protected String splitSep;
    protected String splitDelim1;
    protected String splitDelim2;

    public SplitAt(String separator, String delimiter1, String delimiter2) {
      splitSep = separator;
      splitDelim1 = delimiter1;
      splitDelim2 = delimiter2;
    }

    public List<String> split(ArticleMetadata am, MetadataField field,
        String value) {
      value = value.trim();
      if (splitDelim1 != null) {
        if (splitDelim2 == null) {
          splitDelim2 = splitDelim1;
        }
        value = StringUtils.removeStartIgnoreCase(value, splitDelim1);
        value = StringUtils.removeEndIgnoreCase(value, splitDelim2);
      }
      return StringUtil.breakAt(value, splitSep, -1, true, true);
    }
  }
  
  /**
   * Defines an extractor class that extracts the text from given 
   * html-formatted input string.
   * 
   * @author phil
   * 
   */
  public static class HtmlTextExtractor implements Extractor {

    @Override
    public String extract(ArticleMetadata am, MetadataField field, String value) 
    {
      String result = value;
      if (!StringUtil.isNullString(value)) {
        // matches a single html tag
        result = HtmlUtil.stripHtmlTags(value);
        result = StringEscapeUtils.unescapeHtml4(result);
      }
      return result;
    }
  }
  
  static public Extractor htmlTextExtractor() {
    return new HtmlTextExtractor();
  }
  
  static public Extractor extractHtmlText() {
    return htmlTextExtractor();
  }

  /**
   * Defines an extractor class that extracts the matched value from given input
   * string.
   * 
   * @author akansha
   * 
   */
  public static class GroupExtractor implements Extractor {
    /** Pattern matched. */
    
    protected Pattern pattern;
    String matchedValue; 
    int groupNum;
    /**
     * Creates an instance for the pattern 
     *@param pattern
     */
    public GroupExtractor(String pattern) {
      this(Pattern.compile(pattern));
    }
         
    /**
     * Creates an instance for the pattern
     * 
     * @param pattern
     */
    public GroupExtractor(Pattern pattern) {
      this.pattern = pattern;
    }

    public GroupExtractor(Pattern pattern,int gnum) {
     this.pattern = pattern;
     this.groupNum = gnum;
    }
    
    public GroupExtractor(String pattern,int gnum) {
      this(Pattern.compile(pattern),gnum);
    }
    /**
     * Method definition for extracting the match from the input.
     * 
     * @param am
     *          ArticleMetadata
     * @param field
     *          MetadataField
     * @param value
     *          string from which to extract
     * @return extracted string from the field
     * 
     * @throws IndexOutOfBoundsException GroupExtractor if pattern does not 
     *         contain specific group number        
     * 
     */
    public String extract(ArticleMetadata am, MetadataField field, String value) 
    {

      Matcher m = pattern.matcher(value);
      if(m.find()) {
        return m.group(groupNum);
      } else {
        return null;
      }
    }
 }
  
  /**
   * Returns the extractor for this pattern.
   * 
   * @param pattern pattern to match
   * @param groupNum the group number to extract
   * @return Extractor that matches the pattern.
   */
  public static Extractor extract(String pattern,int groupNum) {
        return groupExtractor(pattern, groupNum);
  }

  /**
   * Returns the extractor for this pattern.
   * 
   * @param pattern pattern to match
   * @param groupNum the group number to extract
   * @return Extractor that matches the pattern.
   */
  public static Extractor extract(Pattern pattern,int groupNum) {
        return groupExtractor(pattern, groupNum);
  }

  /**
   * Returns the extractor for this pattern.
   * 
   * @param pattern
   *          pattern to match
   * @return Extractor that matches the pattern.
   */
  public static Extractor groupExtractor(Pattern pattern,int groupNum) {
    return new GroupExtractor(pattern,groupNum);
  }
 
  public static Extractor groupExtractor(String pattern,int groupNum) {
    return new GroupExtractor(pattern,groupNum);
  }
  
  /**
   * Defines an extractor that extracts the string on a pattern.
   * 
   * @author akansha  
      
   * 
   */
  public interface Extractor {
    /**
     * Extract a value embedded in a string
     * 
     * @param am
     *          the ArticleMeta being stored into (source of Locale, if
     *          necessary)
     * @param field
     *          the field being stored
     * @param value
     *          the value being stored
     * @return extracted value or <code>null</code> if value does not match.
     */
    public String extract(ArticleMetadata am, MetadataField field,String value);

  }
}
