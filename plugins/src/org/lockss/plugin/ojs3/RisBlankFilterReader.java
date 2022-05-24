package org.lockss.plugin.ojs3;

import org.apache.commons.lang3.StringUtils;
import org.lockss.filter.RisFilterReader;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;

/**
 * This is a copy-paste of atypon.RisBlankFilterReader
 * @author mark
 */
public class RisBlankFilterReader extends RisFilterReader {
  public RisBlankFilterReader(InputStream inputStream, String encoding, String... tags)
      throws UnsupportedEncodingException {
    super(inputStream, encoding, tags);
  }

  @Override
  public String rewriteLine(String line) {
    // filter out blank lines as well - unimportant for hashed comparison
    if (StringUtils.isBlank(line)) {
      return null;
    }
    Matcher mat = tagPattern.matcher(line);
    if (mat.find()) {
      String tag = getTag(mat);
      removingTag = tagSet.contains(tag);
    }
    return removingTag ? null : line;

  }
}
