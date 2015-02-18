/*
 * $Id$
 */

/*

Copyright (c) 2012 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.extractor;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.lockss.config.Configuration;
import org.lockss.config.CurrentConfig;
import org.lockss.extractor.JsoupHtmlLinkExtractor.BaseLinkExtractor;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.uiapi.util.Constants;
import org.lockss.util.*;

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * Processing form data for submission:
 * <p/>
 * 1: Identify control name-value pair and part of a submitted form.
 * <p/>
 * 2: Build a form data set:a sequence of control-name/current-value pairs
 * constructed from successful controls
 * <p/>
 * 3: Encode the form data set according to the content type specified by the
 * enctype attribute of the FORM element.
 * <p/>
 * 4: Submit the encoded form data set
 * <p/>
 * The encoded data is sent to the processing agent designated by the action
 * attribute using the protocol specified by the method attribute.
 * <p/>
 * If the method is "get" and the action is an HTTP URI, the user agent
 * 1) takes the value of action,
 * 2) appends a `?' to it,
 * 3) appends the form data set,encoded using the
 * "application/x-www-form-urlencoded" content type.
 * 4) The user agent then traverses the link to this URI. In this scenario,
 * form data are restricted to ASCII codes.
 * <p/>
 * If the method is "post" and the action is an HTTP URI, the user agent
 * 1) conducts an HTTP "post" transaction using the value of the action attribute
 * and a message created according to the content type specified by the
 * enctype attribute.
 * <p/>
 * For any other value of action or method, behavior is unspecified.
 * <p/>
 * application/x-www-form-urlencoded
 * 1) Control names and values are escaped.
 * 2) Space characters are replaced by `+',
 * 3) Reserved characters are escaped as described in [RFC1738], section 2.2:
 * Non-alphanumeric characters are replaced by `%HH'
 * Line breaks are represented as "CR LF" pairs (i.e., `%0D%0A').
 * The control names/values are listed in the order they appear in the document.
 * The name is separated from the value by `='
 * name/value pairs are separated from each other by `&'.
 */
public class HtmlFormExtractor {
  public static final String PREFIX = Configuration.PREFIX +
                                          "extractor.htmlformextractor.";
  public static final String PARAM_MAX_FORM_URLS = PREFIX + "max_form_urls";
  public static final int DEFAULT_MAX_FORM_URLS = 1000000;
  public static final String PARAM_FORM_TAG_CLASS = PREFIX + "form_tag_class";
  public static final String DEFAULT_FORM_TAG_CLASS =
      FormTagLinkExtractor.class.getName();
  public static final String PARAM_FORM_ELEMENT_TAG_CLASS =
      PREFIX + "form_element_tag_class";
  public static final String DEFAULT_FORM_ELEMENT_TAG_CLASS =
      FormElementLinkExtractor.class.getName();

  static final Logger theLogger = Logger.getLogger("HtmlFormExtractor");

  /**
   * The Form Elements which we processs.
   * We don't include layout elements optgroup, fieldset or label
   */
  public static List<String> FORM_TAG_ELEMENTS = Arrays.asList(
    "input",
    "button",
    "select",
    "option",
    "textarea"
  );

  public static final String FORM_TAG = "form";

  /**
   * The Input Types we process
   * We don't include reset,
   */
  public static List<String> InputTypes = Arrays.asList(
    "hidden", "text", "search", "tel", "url", "email", "password",
    "datetime", "date", "month", "week", "time", "datetime-local", "number",
    "range", "color", "checkbox", "radio", "file", "submit", "image",
    "button");

  public static List<String> GeneratedInputTypes = Arrays.asList(
    "text", "search", "tel", "url", "email", "password",
    "datetime", "date", "month", "week", "time", "datetime-local", "number",
    "range", "color"
  )
      ;
  /**
   * Map key values for determining whether or not to process a form
   */
  public static final String FORM_ID = "form_id";
  public static final String FORM_NAME = "form_name";
  public static final String FORM_ACTION = "form_action";
  public static final String SUBMIT_VALUE = "form_submit_value";

  /**
   * The max number of urls a form extractor should generate
   */
  int m_maxFormUrls;

  /**
   * The current number of urls this form has generated.
   */
  int m_numFormUrls;

  /**
   * the encoding of the au
   */
  String m_encoding;


  /**
   * the au for this form processor
   */
  ArchivalUnit m_au;

  /**
   * who to call when we emit a link
   */
  LinkExtractor.Callback m_cb;

  /**
   * the table of forms by form id and the currently open form
   * if the form has no id.
   */
  Map<String, FormUrlGenerator> m_forms =
      new LinkedHashMap<String, FormUrlGenerator>();

  /**
   * the form tag extractor to use with this form processor
   */
  FormTagLinkExtractor m_formExtractor;

  /**
   * the form elements tag extractor to use with this form processor
   */
  private FormElementLinkExtractor m_formElementLinkExtractor;


  /**
   * The table of FormFieldRestrictions by field name
   * key = field name,
   * value = restrictions
   */
  private Map<String, FormFieldRestrictions> m_fieldRestrictions;


  /**
   * The table of FormFieldGenerators key = field name, value generator
   */

  private Map<String, FieldIterator> m_fieldGenerator;

  private List<FormUrlGenerator> urlGenerators;


  /**
   * Construct a new Form Proceesor
   *
   * @param au the au for which we will be processing forms
   * @param cb callback for handling a found link
   * @param encoding the base document encoding
   */
  public HtmlFormExtractor(final ArchivalUnit au,
                           final LinkExtractor.Callback cb,
                           final String encoding,
                           Map<String, FormFieldRestrictions> restrictions,
                           Map<String, FieldIterator> generators) {
    m_au = au;
    m_cb = cb;
    m_encoding = encoding;
    m_forms = new LinkedHashMap<String, FormUrlGenerator>();
    m_maxFormUrls = CurrentConfig.getIntParam(PARAM_MAX_FORM_URLS,
                                              DEFAULT_MAX_FORM_URLS);
    m_numFormUrls = 0;
    if (restrictions != null) {
      m_fieldRestrictions = restrictions;
    } else {
      m_fieldRestrictions = new HashMap<String, FormFieldRestrictions>();
    }
    if (generators != null) {
      m_fieldGenerator = generators;
    } else {
      m_fieldGenerator = new HashMap<String, FieldIterator>();
    }
    urlGenerators = new ArrayList<FormUrlGenerator>();
  }

  /**
   * Initialze the FormExtractor adding any forms for processing
   * and attaching the correct FormLinkExtractor and TagLinkExtractor classes
   * to the doc extractor tag table
   *
   * @param docExtractor the document extractor to which we will add our form
   * extractors
   * @param forms a list of Jsoup FormElement for which we will extract urls
   */
  public void initProcessor(JsoupHtmlLinkExtractor docExtractor,
                            List<FormElement> forms) {

    if (m_formExtractor == null) {
      m_formExtractor = newFormLinkExtractor();
      m_formExtractor.setExtractor(this);
    }

    if (m_formElementLinkExtractor == null) {
      m_formElementLinkExtractor = newTagsLinkExtractor();
      m_formElementLinkExtractor.setExtractor(this);
    }

    for (String el_name : FORM_TAG_ELEMENTS) {
      docExtractor.registerTagExtractor(el_name, m_formElementLinkExtractor);
    }

    if (forms != null) {
      // add all the forms which have
      for (FormElement form : forms) {
        addForm(form);
      }
    }
  }

  /**
   * Add a form field restriction to the field restrictions table
   *
   * @param field the name of the field to attach the restrictions to.
   * @param restrictions the actual restrictions to use for that field
   */
  public void addFieldRestrictions(String field, FormFieldRestrictions
                                                     restrictions) {
    m_fieldRestrictions.put(field, restrictions);
  }

  /**
   * Remove form field restriction from the field restrictions table
   *
   * @param field the name of the field to remove the restrictions from.
   *
   * @return the actual restrictions to used for that field
   */
  public FormFieldRestrictions removeFieldRestrictions(String field) {
    return m_fieldRestrictions.remove(field);
  }

  /**
   * Add a form field restriction to the field generator table
   *
   * @param field the name of the field to attach the generator to.
   * @param iterator the actual field iterator to use
   */
  public void addFieldGenerator(String field, FieldIterator iterator) {
    m_fieldGenerator.put(field, iterator);
  }

  /**
   * Remove form field generator from the field generator table
   *
   * @param field the name of the field to remove the generator from.
   *
   * @return the actual generator used for that field
   */
  public FieldIterator removeFieldGenerator(String field) {
    return m_fieldGenerator.remove(field);
  }

  /**
   * Get the encoding used by this form
   *
   * @return the encoding string for this form
   */
  public String getEncoding() {
    return m_encoding;
  }

  /**
   * Get the form with the given form id
   *
   * @param id the id found in the form element
   *
   * @return the FormUrlGenerator used by that form element
   */
  public FormUrlGenerator getForm(String id) {
    return m_forms.get(id);
  }

  /**
   * Get the form url generators for all forms found in the
   * page
   *
   * @return a list of FormUrlGenerators
   */
  protected List<FormUrlGenerator> getUrlGenerators() {
    return urlGenerators;
  }

  /**
   * Add a form element to the form table.  The form is stored by
   * it's id so this form must have an id.
   *
   * @param el the element to add
   *
   * @throws IllegalArgumentException if el is a form or el has no id.
   */
  public void addForm(Element el) {
    if (!el.tagName().equalsIgnoreCase(FORM_TAG)) {
      throw new IllegalArgumentException(
       "Attempt to add non-form element to form table");
    }
    FormUrlGenerator formUrlGenerator = new FormUrlGenerator(el);
    urlGenerators.add(formUrlGenerator);
    if (!StringUtil.isNullString(el.id())) {
      m_forms.put(el.id(), new FormUrlGenerator(el));
    }
  }

  /**
   * Create a new FormUrlGenerator for a form and if the form has
   * an id at it to the id table.
   *
   * @param fe the element to add
   *
   * @throws IllegalArgumentException if el is a form or el has no id.
   */
  public void addForm(FormElement fe) {
    FormUrlGenerator formUrlGenerator = new FormUrlGenerator(fe);
    urlGenerators.add(formUrlGenerator);
    if (!StringUtil.isNullString(fe.id())) {
      m_forms.put(fe.id(), formUrlGenerator);
    }
    Elements elements = fe.elements();
    for (Element element : elements) {
      formUrlGenerator.addElement(element);
    }
  }

  /**
   * Determine if we should extract forms based on the installed
   * FormFieldRestrictions
   *
   * @param id the id found in the form element
   * @param name the name for this form element
   * @param action the action for this form element
   * @param val the submission value for this form element
   *
   * @return true if none of the above values is in the list of
   * restricted field elements
   */
  protected boolean shouldProcessForm(String id, String name,
                                      String action, String val) {
    FormFieldRestrictions restrictions;
    if (m_fieldRestrictions != null) {
      restrictions = m_fieldRestrictions.get(FORM_ID);
      if (restrictions != null && !restrictions.isAllowed(id)) {
        return false;
      }
      restrictions = m_fieldRestrictions.get(FORM_NAME);
      if (restrictions != null && !restrictions.isAllowed(name)) {
        return false;
      }

      restrictions = m_fieldRestrictions.get(FORM_ACTION);
      if (restrictions != null && !restrictions.isAllowed(action)) {
        return false;
      }
      restrictions = m_fieldRestrictions.get(SUBMIT_VALUE);
      if (restrictions != null && !restrictions.isAllowed(val)) {
        return false;
      }
    }
    return true;
  }

  /**
   * process all forms in the form table.
   */
  public void processForms() {
    for (FormUrlGenerator form : urlGenerators) {
      form.emitLinks(m_au, m_cb);
    }
  }

  /**
   * Get the 'form' tag link extractor for this form processor and attach it to
   * this HtmlFormExtractor.  There is one and only one for each
   * HtmlFormExtractor.  To override the default form extractor
   * subclass FormTagLinkExtractor and override appropriate methods and incl
   * in the au config.
   *
   * @return the FormTagLinkExtractor for this form processor
   */
  public FormTagLinkExtractor newFormLinkExtractor() {
    return new FormTagLinkExtractor();
  }

  /**
   * Get the form elements tag link extractor for this .  There is one and
   * only one for each form processor.  To override the default form extractor
   * subclass FormElementLinkExtractor and override appropriate methods and
   * included in the au config
   *
   * @return the FormElementLinkExtractor for this form processor
   */
  public FormElementLinkExtractor newTagsLinkExtractor() {
    return new FormElementLinkExtractor();
  }

  public FormTagLinkExtractor getFormExtractor() {
    return m_formExtractor;
  }

  public void setFormExtractor(FormTagLinkExtractor formExtractor) {
    m_formExtractor = formExtractor;
  }

  public FormElementLinkExtractor getTagsLinkExtractor() {
    return m_formElementLinkExtractor;
  }

  public void setTagsLinkExtractor(FormElementLinkExtractor tagExtractor) {
    m_formElementLinkExtractor = tagExtractor;
  }

  /**
   * Make an url encoded name=value pair
   *
   * @param name the name to encode
   * @param value the value encode
   *
   * @return a urlencoded name=value string
   */
  static public String makeEncodedString(String name, String value) {
    StringBuilder sb = new StringBuilder();
    String enc_name = name;
    String enc_val = value;
    if (!StringUtil.isNullString(name)) {
      enc_name = UrlUtil.encodeUrl(name);
    }
    if (!StringUtil.isNullString(value)) {
      enc_val = UrlUtil.encodeUrl(value);
    }
    sb.append(enc_name).append("=").append(enc_val);
    return sb.toString();
  }

  /**
   * Make a url encoded assignment of name to multiple values:
   * name=val1&name=val2...name=valn
   *
   * @param name the name to encode
   * @param values the list of values to encode
   *
   * @return a urlencoded sequence of name=val pairings
   */
  static public String makeEncodedString(String name,
                                         Collection<String> values) {
    StringBuilder sb = new StringBuilder();
    String enc_name = UrlUtil.encodeUrl(name);
    String enc_val;
    int amp_ct = values.size();

    for (final String value : values) {
      enc_val = UrlUtil.encodeUrl(value);
      sb.append(enc_name).append("=").append(enc_val);
      if (--amp_ct > 0) {
        sb.append("&");
      }
    }
    return sb.toString();
  }

  /**
   * Interface for all Field Iterators
   */
  public interface FieldIterator extends Iterator<String> {
    /**
     * reset the iterator fields back to the starting position. This
     * allows us to treat a field iterator identically to an array with an
     * index which resets to 0.
     */
    public void reset();

    /**
     * return the last iteration we use this instead of a next() call so we
     * can wrap around sensibly.
     *
     * @return the last result of the cast to next()
     */
    public String last();
  }


  /**
   * The extractor for a "form" tag.
   * This does all of the work necessary to handle the begin and end tags.
   * In addition unlike other LinkExtractors this will requires you to
   * set the HtmlFormExtractor before use.
   */
  public static class FormTagLinkExtractor extends BaseLinkExtractor {

    int open_ct = 0;
    HtmlFormExtractor extractor;

    /**
     * Extract link(s) from this tag.
     *
     * @param node the node containing the link
     * @param au Current archival unit to which this html document belongs.
     * @param cb A callback to record extracted links.
     */
    public void tagBegin(final Node node, final ArchivalUnit au,
                         final LinkExtractor.Callback cb) {
      super.tagBegin(node, au, cb);
      // --- required attributes ---
      open_ct++;
      if (open_ct > 1) {
        // if we have an open form - we can't open a new one.
        if (theLogger.isDebug()) {
          theLogger.debug("form can not be processed: nested form");
        }
      }

    }

    /**
     * Perform any extractions based on end tag processing.
     *
     * @param node the node containing the link
     * @param au Current archival unit to which this html document belongs.
     * @param cb A callback to record extracted links.
     */
    @Override
    public void tagEnd(final Node node, final ArchivalUnit au,
                       final LinkExtractor.Callback cb) {
      super.tagEnd(node, au, cb);
      if (open_ct > 1) {
        // we found a end tag with no matching begin
        if (theLogger.isDebug()) {
          theLogger.debug("HtmlFormExtractor skipping nested close");
        }
      }
      open_ct--;
    }

    protected void setExtractor(HtmlFormExtractor extractor) {
      this.extractor = extractor;
    }
  }


  public static class FormElementLinkExtractor extends BaseLinkExtractor {

    HtmlFormExtractor extractor;

    /**
     * Extract link(s) from a tag found in "form".  These tags should be one
     * of the FORM_TAG_ELEMENTS defined above.
     *
     * @param node the node containing the link
     * @param au Current archival unit to which this html document belongs.
     * @param cb A callback to record extracted links.
     */
    @Override
    public void tagBegin(final Node node, final ArchivalUnit au,
                         final LinkExtractor.Callback cb) {
      super.tagBegin(node, au, cb);
      if (extractor == null) {
        return;
      }

      Element el = (Element) node;
      String name = el.tagName().toLowerCase();

      if (FORM_TAG_ELEMENTS.contains(name)) {
        String f_id = el.attr(FORM_TAG);
        if (!StringUtil.isNullString(f_id)) {
          FormUrlGenerator form = extractor.getForm(f_id);
          if (form != null) {
            form.addElement(el);
          }
        }
      }
    }

    protected void setExtractor(HtmlFormExtractor extractor) {
      this.extractor = extractor;
    }
  }

  /**
   * The actual url generator for a specific form.
   * It collects all of the elements contained within the form or with
   * the the form id.  Call #emitLinks to generate the iterators which will
   * out the the forms links
   */
  public class FormUrlGenerator {
    Element m_form;

    /**
     * Specifies where to send the form-data when a form is submitted. A form
     * without an action is ignored.
     */
    String m_action;

    /**
     * Specifies the character encodings that are to be used for the form
     * submission
     */
    String m_charset;

    /**
     * Specifies how the form-data should be encoded using "post"
     * One of:
     * application/x-www-form-urlencoded (default)
     * multipart/form-data: for file or binary data submission on "put"
     * text/plain: for "put"
     */
    String m_enctype;

    /**
     * Specifies the HTTP method to use when sending form-data:
     * either "put" or "post"
     */
    String m_method;

    /**
     * the form id
     */
    String m_id;

    /**
     * the form name
     */
    String m_name;

    /**
     * the list of submit button or input elements for this form
     */
    List<Element> m_submits;

    /**
     * the table of controls in this form
     */
    Map<String, FormControlElement> m_controls;


    FormUrlGenerator(Element el) {
      m_form = el;
      // the only required attribute
      m_action = el.attr("abs:action");

      // --- optional attributes ---
      // char encoding for sent form
      m_charset = el.attr("accept-charset");
      // method is either "post" or "get"
      m_method = el.attr("method");
      // encoding type for post (url encoding is always used for put)
      m_enctype = el.attr("enctype");
      m_id = el.attr("id");
      m_name = el.attr("name");

      if (StringUtil.isNullString(m_charset) ||
              !Charset.isSupported(m_charset)) {
        m_charset = m_encoding;// default charset is the documents encoding.
      }
      if (StringUtil.isNullString(m_method)) {
        m_method = "get"; // default method type is "get"
      }
      if (StringUtil.isNullString(m_enctype)) {
        m_enctype = Constants.FORM_ENCODING_URL;
      }
      m_submits = new ArrayList<Element>();
      m_controls = new LinkedHashMap<String, FormControlElement>();
    }

    /**
     * Test if a element can be added to the the current FormUrlGenerator
     * This prevents the adding of duplicate elements and elements being
     * added to a form if the 'form' attr is different from the form 'id'
     * @param el the element to add
     * @return true iff the element is not already added AND
     *  if the element has a 'form' attr it equals the form 'id'
     */
    boolean canAddElement(Element el) {
      String el_form = el.attr("form");
      if (StringUtil.isNullString(el_form) || el_form.equals(m_id)) {
        // if an element is the same as an existing element we ignore it
        Elements elements = m_form.getElementsByTag(el.tagName());
        return elements != null && elements.contains(el);
      }
      return false;
    }

    /**
     * Called by the tag begin for an form element tag to add the element to
     * the forms processer
     *
     * @param el The element tag to add
     */
    void addElement(Element el) {
      String el_form = el.attr("form");
      if (StringUtil.isNullString(el_form) || el_form.equals(m_id)) {
        if (el.tagName().equalsIgnoreCase("select")) {
          addSelectElement(el);
        } else if (el.tagName().equalsIgnoreCase("input")) {
          addInputElement(el);
        } else if (el.tagName().equalsIgnoreCase("button")) {
          addButtonElement(el);
        }
      }
    }

    /**
     * Extract any data from an "input" tag and add it to the form
     * processor.  Since image imputs can also have a "src" attribute we
     * make sure to process that link before adding to the submit table.
     *
     * @param el The "input" element tag to add
     */
    void addInputElement(final Element el) {
      // we process explicitly input type: checkbox, radio, hidden, submit
      String name = el.attr("name");
      String type = el.attr("type");
      String val = el.attr("value");
      boolean req = el.hasAttr("required");
      JsoupHtmlLinkExtractor.checkLink(el, m_cb, "src");
      //String src = el.attr("src");
      // unlike buttons which use the img tag, input tags use a "src" attribute
      //if(!StringUtil.isNullString(src)) {
      // m_cb.foundLink(src);
      //}
      if (type.equalsIgnoreCase("submit")) {
        addSubmit(el, name, val);
      } else if (type.equalsIgnoreCase("image")) {
        // input tags of type "image" use a "src" attribute
        //if(src != null) {
        //  m_cb.foundLink(src);
        //}
        // but it acts like a submit button
        addSubmit(el, name, val);
      } else if (type.equalsIgnoreCase("hidden")) {
        // always req, never multisel
        addControl(type, name, val, true, false);
      } else if (type.equalsIgnoreCase("checkbox")) {
        if (StringUtil.isNullString(val)) {
          val = "on"; // default value
        }
        // may be required, always multisel
        addControl(type, name, val, req, true);
      } else if (type.equalsIgnoreCase("radio")) {
        // always req i.e. one radio is always on, never multisel.
        addControl(type, name, val, true, false);
      } else {
        // all other input types, check to see if we have a field generator
        FieldIterator iter = m_fieldGenerator.get(name);
        if (iter != null) {
          addFieldIterator(type, name, req, iter);
        }
      }
    }

    /**
     * Extract any data from an "button" tag and add it to the form
     * processor if it is of type submit.
     *
     * @param el The "button" element tag to add
     */
    void addButtonElement(final Element el) {
      String type = el.attr("type");
      // the only button types we care about is submit
      if (type.equalsIgnoreCase("submit")) {
        addSubmit(el, el.attr("name"), el.attr("value"));
      }
    }

    /**
     * Extract any data from an "option" tag and add it to the form
     * processor if it is of type submit.
     *
     * @param el The "input" element tag to add
     */
    void addSelectElement(final Element el) {

      // selects are based on grouping so we grab them all up front.
      Elements selections = el.getElementsByTag("option");
      // this is an optional attribute
      String name = el.attr("name");

      if (StringUtil.isNullString(name) ||
              (selections == null || selections.isEmpty())) {
        theLogger.debug3("'select' requires 'name' & 'options' attributes");
        return;
      }

      String multiple = el.attr("multiple");
      boolean allow_multi = (multiple != null) &&
                                multiple.equalsIgnoreCase("multiple");
      FormControlElement fc =
          new FormControlElement("select", name, allow_multi, false);
      if (m_fieldRestrictions != null) {
        fc.setRestrictions(m_fieldRestrictions.get(name));
      }
      m_controls.put(name, fc);

      for (Element sel : selections) {
        String value = sel.val();

        if (StringUtil.isNullString(value)) {
          // if a value isn't specified the text of the select is the value.
          value = sel.text();
        }
        fc.addControlElement(value);
      }
    }

    /**
     * Attach a plugin defined FieldIterator to a new FormControlElement for a
     * "input" element.
     *
     * @param type the "input" element type
     * @param name the "input" element name
     * @param req is the element required
     * @param iter the FieldIterator which will generate all values for
     * this field.
     */
    void addFieldIterator(String type, String name,
                          boolean req, FieldIterator iter) {
      FormControlElement fc = m_controls.get(name);
      if (fc == null) {
        fc = new FormControlElement(type, name, false, req, iter);
        m_controls.put(name, fc);
      }
    }

    /**
     * Add a value to the "input" element with this name.  This will create a
     * new FormControlElement if one does not exist;
     *
     * @param type the "input" element type
     * @param name the "input" element name
     * @param value the value for this "input" element
     * @param req is the element required
     * @param multi are multi selections allowed.
     */
    void addControl(String type, String name, String value,
                    boolean req, boolean multi) {
      if (StringUtil.isNullString(name) || StringUtil.isNullString(value)) {
        theLogger.debug3("element requires 'name' & 'value' ");
        return;
      }
      FormControlElement fc = m_controls.get(name);
      if (fc == null) {
        fc = new FormControlElement(type, name, multi, req);
        if (m_fieldRestrictions != null) {
          fc.setRestrictions(m_fieldRestrictions.get(name));
        }
        m_controls.put(name, fc);
      }
      fc.addControlElement(value);
    }

    /**
     * Add a new submit type for an "input" or "button" of type "submit"
     * This will check for duplicates before adding.  We only pay attention to
     * the fields which might effect the url.
     *
     * @param el the element to add to the submit table
     * @param name the name (null ok)
     * @param value the value (null ok)
     */
    void addSubmit(Element el, String name, String value) {
      boolean duplicate = false;
      String action = el.attr("formaction");
      // make sure we have an action that can be used to process this form
      if (StringUtil.isNullString(action) &&
              StringUtil.isNullString(m_action)) {
        //we skip this because we can't act on it.
        return;
      }
      // check for a duplicate and skip it if we don't have different name:
      // value pair or action
      for (Element sub_el : m_submits) {
        String sub_action = sub_el.attr("formaction");
        String sub_name = sub_el.attr("name");
        String sub_val = sub_el.attr("value");
        if (StringUtil.equalStringsIgnoreCase(action, sub_action) &&
                StringUtil.equalStringsIgnoreCase(name, sub_name) &&
                StringUtil.equalStringsIgnoreCase(value, sub_val)) {
          duplicate = true;
          break;
        }
      }
      if (!duplicate) {
        m_submits.add(el);
      }
    }

    /**
     * Called when we have added all of the elements for a form that would need
     * to be processed. For anonymous forms this when we reach the end of the
     * form for all other forms this is when we reach the end of the document.
     *
     * @param au the archival unit
     * @param cb the callback to used when a link is found.
     */
    void emitLinks(ArchivalUnit au, LinkExtractor.Callback cb) {

      // Do not extract links if no submit button is seen in the form.
      if (m_submits.isEmpty()) {
        return;
      }
      // for every submit element we need to generate all permutations.
      for (Element el : m_submits) {
        String method = el.attr("formmethod");
        String action = el.attr("formaction");
        String enctype = el.attr("formenctype");

        // replace the defaults with the local submit buttons commands if
        // necessary
        if (StringUtil.isNullString(method)) {
          method = m_method;
        }
        if (StringUtil.isNullString(action)) {
          action = m_action;
        }
        if (StringUtil.isNullString(enctype)) {
          enctype = m_enctype;
        }
        // make sure to add this submit button to the controls list.
        String name = el.attr("name");
        String type = el.attr("type");
        String val = el.attr("value");
        // make sure we should process this form
        if (shouldProcessForm(m_id, m_name, action, val)) {
          if (!StringUtil.isNullString(name) && !StringUtil.isNullString(val)) {
            // add the name/value pair for this submit
            addControl(type, name, val, true, false);
          }
          FormElementCollector m_collector =
              new FormElementCollector(this, m_controls, action,
                                       method, enctype);
          for (String link : m_collector) {
            if (!StringUtil.isNullString(link)) {
              m_numFormUrls++;
              cb.foundLink(link);
            }
          }
          if (name != null) {
            // now remove any added submit name:value pair
            m_controls.remove(name);
          }
        }
      }
    }

    public int max() {
      return m_maxFormUrls;
    }
  }

  /**
   * This a variant of the power set iterator.
   * instead of using an index into an array it uses multiple iterators
   * and repeatedly calls next. This allows us to iterate through multiple
   * sets generating one url at a time.
   */
  protected static class FormElementCollector implements Iterable<String> {
    FormUrlGenerator m_form;
    Map<String, FormControlElement> m_controls;
    private List<FieldIterator> m_components;
    private int m_maxFormUrls;
    String m_action;
    String m_enctype;
    String m_method;

    FormElementCollector(final FormUrlGenerator form,
                         final Map<String, FormControlElement> controls,
                         final String action,
                         final String method,
                         final String enctype) {
      m_form = form;
      m_controls = controls;
      m_maxFormUrls = form.max();
      m_components = new ArrayList<FieldIterator>();
      m_action = action;
      m_enctype = enctype;
      m_method = method;
    }

    @Override
    public Iterator<String> iterator() {
      return new FormElementIterator();
    }

    // the actual iterator used by the FromElementCollector
    class FormElementIterator implements Iterator<String> {
      private int m_numUrlSeen = 0;
      private boolean m_hasNext = true;
      Collection<String> m_args;

      FormElementIterator() {
        m_numUrlSeen = 0;
        m_args = m_controls.keySet();
        if (m_method.equalsIgnoreCase("post")) {
          m_args = CollectionUtil.asSortedList(m_controls.keySet());
        }
        for (String arg : m_args) {
          FormControlElement control = m_controls.get(arg);
          FieldIterator field_iter = control.getFieldIterator();
          if (field_iter.hasNext()) {
            m_components.add(field_iter);
            // queue up the "next" value so we don't start with null
            field_iter.next();
          }
        }
      }

      @Override
      public boolean hasNext() {
        return (m_numUrlSeen <= m_maxFormUrls) && m_hasNext;

      }

      @Override
      public String next() {
        if (!hasNext()) {
          return null;
        }

        boolean isFirstArgSeen = false;
        StringBuilder url = new StringBuilder().append(m_action);
        for (FieldIterator component : m_components) {
          String arg = component.last();
          if (!StringUtil.isNullString(arg)) {
            url.append(isFirstArgSeen ? '&' : '?');
            url.append(component.last());
            isFirstArgSeen = true;
          }
        }
        incrementPositions();
        return url.toString();
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }

      private void incrementPositions() {
        if (!hasNext()) {
          return;
        }
        m_numUrlSeen++;
        for (int i = 0; i < m_components.size(); ++i) {
          FieldIterator component = m_components.get(i);
          if (component.hasNext()) {
            component.next();
            break;
          } else {
            if (i + 1 == m_components.size()) {
              m_hasNext = false;
              break;
            }
            component.reset();
            component.next();
          }
        }
      }
    }
  }

  /**
   * The wrapper around an element to be added to FormCollector.  This
   * collects a name and all values into a single structure
   */
  protected static class FormControlElement {
    String m_type;
    String m_name;
    List<String> m_values;
    boolean m_multi;
    boolean isRequired;
    private FormFieldRestrictions m_restrictions;
    private FieldIterator m_iterator;

    /**
     * Constructor for an element which will be made of name:values pairs
     *
     * @param type the type of control element
     * @param name the name of for this control element
     * @param multi is multi selection permitted
     * @param req is this element required to have a value
     */
    FormControlElement(String type, String name, boolean multi,
                       boolean req) {
      m_type = type;
      m_name = name;
      m_values = new ArrayList<String>();
      m_multi = multi;
      isRequired = req;
      if (!req && !multi) {
        m_values.add("");
      }
    }

    /**
     * Constructor for an element which will simply use an iterator to
     * generate all values.
     *
     * @param type the type of control element
     * @param name the name of for this control element
     * @param multi is multi selection permitted
     * @param req is this element required to have a value
     * @param iter the iterator for this element
     */
    FormControlElement(String type, String name,
                       boolean multi, boolean req, FieldIterator iter) {
      this(type, name, multi, req);
      m_iterator = iter;
    }

    /**
     * Add another value for this element
     *
     * @param value the value to add
     */
    void addControlElement(String value) {
      if (value == null) {
        value = "";
      }
      if (!m_values.contains(value)) {
        if (m_restrictions == null || m_restrictions.isAllowed(value)) {
          m_values.add(value);
        }
      }
    }

    FieldIterator getFieldIterator() {
      if (m_iterator != null) {
        return m_iterator;
      }
      if (m_multi) {
        return new MultiSelectFieldIterator(m_name, m_values);
      }
      return new SingleSelectFieldIterator(m_name, m_values);
    }

    void setRestrictions(final FormFieldRestrictions restrictions) {
      m_restrictions = restrictions;
    }
  }


  /**
   * Iterator class used for single select fields.
   */
  protected static class SingleSelectFieldIterator implements FieldIterator {
    private String m_last;
    private String m_name;
    Iterator<String> m_iterator;
    private List<String> m_values;

    SingleSelectFieldIterator(String name, List<String> values) {
      m_name = name;
      m_values = values;
      m_iterator = m_values.iterator();
    }

    @Override
    public void reset() {
      m_iterator = m_values.iterator();
    }


    @Override
    public boolean hasNext() {
      return m_iterator.hasNext();
    }

    @Override
    public String next() {
      if (!hasNext()) {
        return null;
      }

      String val = m_iterator.next();
      if (StringUtil.isNullString(val)) {
        m_last = "";
      } else {
        m_last = HtmlFormExtractor.makeEncodedString(m_name, val);
      }
      return m_last;

    }

    @Override
    public String last() {
      return m_last;
    }

    @Override
    public void remove() {
      m_iterator.remove();
    }
  }

  /**
   * Iterator used for multi select fields. We wrap a standard powerset
   * iterator into a FieldIterator which allows resets and a call to last
   */
  protected static class MultiSelectFieldIterator implements FieldIterator {

    PowerSetIterator<String> m_psi;
    String m_last;
    String m_name;
    List<String> m_values;

    protected MultiSelectFieldIterator(String name, List<String> values) {
      m_name = name;
      m_values = values;
      m_psi = new PowerSetIterator<String>(m_values);
    }

    @Override
    public void reset() {
      m_psi.reset();
      m_last = null;
    }

    public String last() {
      return m_last;
    }

    @Override
    public boolean hasNext() {
      return m_psi.hasNext();
    }

    @Override
    public String next() {
      List<String> val = m_psi.next();
      if (val == null || val.isEmpty()) {
        m_last = "";
      } else {
        m_last = HtmlFormExtractor.makeEncodedString(m_name, val);
      }
      return m_last;
    }

    @Override
    public void remove() {
      m_psi.remove();
    }
  }

  //  --------------------------------------------------
  //    Form Field Restrictions and Field Generators
  //  --------------------------------------------------

  /**
   * Restrictions for a form field.
   * this is the union of the include list (if non-null) less the exclude list
   * (if non-null)
   */
  public static class FormFieldRestrictions {
    Set<String> m_include;
    Set<String> m_exclude;

    public FormFieldRestrictions(Set<String> include, Set<String> exclude) {
      m_include = include;
      m_exclude = exclude;
    }

    /**
     * Take a list of values and exclude any items in the exclude list and
     * keep any items not excluded or in the include list if such a list
     * exists.
     *
     * @param in the incoming collection of items to include
     *
     * @return the set of items which remain
     */
    public Collection<String> restrict(Collection<String> in) {
      Set<String> permitted = new TreeSet<String>(in);
      if (m_exclude != null) {
        permitted.removeAll(m_exclude);
      }
      if (m_include != null) {
        permitted.retainAll(m_include);
      }
      return permitted;
    }


    /**
     * Is this element allowed ie in the included list and not in the exclude
     * list.
     *
     * @param value the value to check
     *
     * @return true iff the item is not excluded and is either in the include
     * list or no such list exists.
     */
    public boolean isAllowed(String value) {
      boolean allow = true;

      if (m_exclude != null) {
        allow = !m_exclude.contains(value);
      }
      if (allow && m_include != null) {
        allow = m_include.contains(value);
      }
      return allow;
    }

    public Set<String> getInclude() {
      return m_include;
    }

    public void setInclude(final Set<String> include) {
      m_include = include;
    }

    public Set<String> getExclude() {
      return m_exclude;
    }

    public void setExclude(final Set<String> exclude) {
      m_exclude = exclude;
    }
  }

  /**
   * FieldIterator used to step through a list of elements returning each one.
   */
  public static class FixedListFieldGenerator implements FieldIterator {
    List<String> m_list;
    Iterator<String> m_iterator;
    String m_name;
    String m_last;

    public FixedListFieldGenerator(String name, List<String> elements) {
      m_list = elements;
      m_iterator = elements.iterator();
      m_name = name;
    }

    public boolean hasNext() {
      return m_iterator.hasNext();
    }

    public String next() {
      String val = m_iterator.next();

      if (val == null || val.isEmpty()) {
        m_last = "";
      } else {
        m_last = HtmlFormExtractor.makeEncodedString(m_name, val);
      }
      return m_last;
    }

    public void reset() {
      m_iterator = m_list.iterator();
    }

    @Override
    public String last() {
      return m_last;
    }

    public void remove() {
      m_iterator.remove();
    }
  }

  /**
   * Field Iterator used to step through a list of integer numbers between
   * min an max a increment at a time.
   */
  public static class IntegerFieldIterator implements FieldIterator {
    private Integer nextValue;
    private Integer minValue;
    private Integer maxValue;
    private Integer incrValue;
    private Integer lastValue;
    private String m_name;

    IntegerFieldIterator(String name, Integer min, Integer max, Integer incr) {
      minValue = min;
      maxValue = max;
      nextValue = min;
      incrValue = incr;
      m_name = name;
    }

    public boolean hasNext() {
      return nextValue.compareTo(maxValue) <= 0;
    }

    public String next() {
      lastValue = nextValue;
      nextValue = lastValue + incrValue;
      return HtmlFormExtractor.makeEncodedString(m_name, lastValue.toString());
    }

    public void reset() {
      nextValue = minValue;
    }

    @Override
    public String last() {
      return HtmlFormExtractor.makeEncodedString(m_name, lastValue.toString());
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Field Iterator used to step through a list of floating pt numbers between
   * min an max a increment at a time.
   */
  public static class FloatFieldIterator implements FieldIterator {
    private Float nextValue;
    private Float minValue;
    private Float maxValue;
    private Float incrValue;
    private Float lastValue;
    private String m_name;

    FloatFieldIterator(String name, Float min, Float max, Float incr) {
      minValue = min;
      maxValue = max;
      nextValue = min;
      incrValue = incr;
      m_name = name;
    }

    public boolean hasNext() {
      return nextValue.compareTo(maxValue) <= 0;
    }

    public String next() {
      lastValue = nextValue;
      nextValue = lastValue + incrValue;
      return HtmlFormExtractor.makeEncodedString(m_name, lastValue.toString());
    }

    public void reset() {
      nextValue = minValue;
    }

    @Override
    public String last() {
      return HtmlFormExtractor.makeEncodedString(m_name, lastValue.toString());
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * FieldIterator used to step through a calendar from start date to end date.
   * This requires the field to use for stepping (month, day, year) and the
   * size of each step.  In addition it needs a format string to use for
   * turning the date into a string.
   */
  public static class CalendarFieldGenerator implements FieldIterator {
    Calendar startDate;
    Calendar endDate;
    Calendar nextDate;
    String m_name;
    String lastValue;
    int incrField;
    int incrValue;
    SimpleDateFormat formatter;


    public CalendarFieldGenerator(String name, Calendar start, Calendar end,
                                  int field,
                                  int incr, String format) {
      m_name = name;
      startDate = start;
      endDate = end;
      nextDate = start;
      incrField = field;
      incrValue = incr;
      formatter = new SimpleDateFormat(format);
      if (theLogger.isDebug2()) {
        theLogger.debug2("start:" + formatter.format(start.getTime()) +
                         "end:" + formatter.format(end.getTime()));
        theLogger.debug2("nextDate:" + formatter.format(nextDate.getTime()));
      }
    }

    public CalendarFieldGenerator(String name, Date start, Date end,
                                  int field, int incr, String format) {
      m_name = name;
      startDate = Calendar.getInstance();
      startDate.setTime(start);
      endDate = Calendar.getInstance();
      endDate.setTime(end);
      incrField = field;
      incrValue = incr;
      formatter = new SimpleDateFormat(format);
    }

    public boolean hasNext() {
      return !nextDate.after(endDate);
      //return nextDate.compareTo(endDate) <= 0;
    }

    public String next() {
      if (theLogger.isDebug3()) {
        theLogger.debug3("next() in: " +
                         formatter.format(nextDate.getTime()));
      }
      lastValue = HtmlFormExtractor.makeEncodedString(m_name,
          formatter.format(nextDate.getTime()));
      nextDate.add(incrField, incrValue);
      if (theLogger.isDebug3()) {
        theLogger.debug3("next() out: " +
                         formatter.format(nextDate.getTime()));
      }
      return lastValue;
    }

    public void reset() {
      nextDate = startDate;
    }

    @Override
    public String last() {
      return lastValue;
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

}
