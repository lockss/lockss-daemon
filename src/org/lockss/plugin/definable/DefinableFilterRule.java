package org.lockss.plugin.definable;

import java.io.*;
import java.util.*;

import org.lockss.filter.*;
import org.lockss.filter.HtmlTagFilter.*;
import org.lockss.plugin.*;
import org.lockss.util.*;

public class DefinableFilterRule implements FilterRule {
  public static final String FILTER_KIND = "filter_kind";
  public static final String STRING_FILTER = "string_filter";
  public static final String STRING_ORIG = "original";
  public static final String STRING_REPL = "replacement";

  public static final String TAG_FILTER = "tag_filter";
  public static final String TAG_COUNT = "tag_count";
  public static final String TAG_IGNORE_CASE = "ignore_case";
  public static final String TAG_PAIR_START = "start_tag";
  public static final String TAG_PAIR_END = "end_tag";

  public static final String WHITESPACE_FILTER = "whitespace_filter";
  public static final String WSFILTER_ON = "filter_on";

  private boolean m_filterWhiteSpace = false;
  private List m_readers;

  /**
   *
   * @param readers List of filterRuleData
   */
  public DefinableFilterRule(List ruleList) {
    m_readers = makeReaders(ruleList);
  }

  /**
   *
   * @param startTag String
   * @param endTag String
   * @param ignoreCase boolean
   * @return TagPair
   */
  public static TagPair makeTagPair(String startTag, String endTag,
                                    boolean ignoreCase) {
    return new HtmlTagFilter.TagPair(startTag, endTag, ignoreCase);
  }

  /**
   *
   * @param filterData List
   * @return List
   */
  public List makeReaders(List filterData) {
    ArrayList readers = new ArrayList();
    if(filterData != null) {
      for (Iterator f_it = filterData.iterator(); f_it.hasNext(); ) {
        String[] descrs = (String[]) f_it.next();
        HashMap d_map = new HashMap();
        for (int i = 0; i < descrs.length; i++) {
          Vector s_vec = StringUtil.breakAt(descrs[i], '=', 2, true, true);
          d_map.put(s_vec.get(0), s_vec.get(1));
        }
        readers.add(makeReaderFromMap(d_map));
      }
    }
    return readers;
  }

  /**
   * makeReaderFromMap
   *
   * @param map HashMap
   * @return FilterReader
   */
  public FilterReader makeReaderFromMap(HashMap map) {
    FilterReader reader = null;
    String kind = (String) map.get(FILTER_KIND);
    if (STRING_FILTER.equals(kind)) {
      String orig = (String) map.get(STRING_ORIG);
      String repl = (String) map.get(STRING_REPL);
      reader = new StringFilterReader(orig, repl);
    }
    else if (TAG_FILTER.equals(kind)) {
      int num_tags = Integer.parseInt( (String) map.get(TAG_COUNT));
      ArrayList tag_list = new ArrayList(num_tags);
      for (int i = 0; i < num_tags; i++) {
        boolean ignore_case =
            Boolean.getBoolean( (String) map.get(TAG_IGNORE_CASE + i));
        String start = (String) map.get(TAG_PAIR_START + i);
        String end = (String) map.get(TAG_PAIR_END + i);
        tag_list.add(makeTagPair(start, end, ignore_case));
      }
      reader = new TagFilterReader(tag_list);
    }
    else if(WHITESPACE_FILTER.equals(kind)) {
      m_filterWhiteSpace = Boolean.getBoolean((String)map.get(WSFILTER_ON));
    }

    return reader;
  }

  /**
   * createFilteredInputStream
   *
   * @param reader Reader
   * @return InputStream
   */
  public InputStream createFilteredInputStream(Reader reader) {
    Reader cur_reader = reader;
    // loop through our list of filters
    for(Iterator it = m_readers.iterator(); it.hasNext(); ) {
      cur_reader = ((FilterReader)it.next()).makeFilterReader(cur_reader);
    }
    // convert to an input stream
    InputStream stream = new ReaderInputStream(cur_reader);
    if (m_filterWhiteSpace) {
      stream = new WhiteSpaceFilter(stream);
    }
    return stream;
  }

  public interface FilterReader {
    public Reader makeFilterReader(Reader reader);
  }

  public static class StringFilterReader
      implements FilterReader {
    String m_origString;
    String m_replString;

    public StringFilterReader(String origString, String replString) {
      m_origString = origString;
      m_replString = replString;
    }

    public Reader makeFilterReader(Reader reader) {
      return new StringFilter(reader, m_origString, m_replString);
    }
  }

  public static class TagFilterReader
      implements FilterReader {
    List m_tagList;

    public TagFilterReader(List tagList) {
      m_tagList = tagList;
    }

    public Reader makeFilterReader(Reader reader) {
      return HtmlTagFilter.makeNestedFilter(reader, m_tagList);
    }
  }
}
