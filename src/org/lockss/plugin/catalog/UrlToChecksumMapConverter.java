package org.lockss.plugin.catalog;

import org.lockss.util.StringUtil;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class UrlToChecksumMapConverter implements Converter {
  @Override
  public boolean canConvert(Class clazz) {
    return clazz.equals(UrlToChecksumMap.class);
  }

  @Override
  public void marshal(Object value, HierarchicalStreamWriter writer,
      MarshallingContext context) {
    UrlToChecksumMap u2c = (UrlToChecksumMap) value;
    // writer.startNode("map");
    for (String url : u2c.keySet()) {
      writer.startNode("entry");
      writer.startNode("url");
      writer.setValue(url);
      writer.endNode();
      writer.startNode("checksum");
      writer.setValue(u2c.get(url));
      writer.endNode();
      writer.endNode();
    }
    // writer.endNode();
  }

  @Override
  public Object unmarshal(HierarchicalStreamReader reader,
      UnmarshallingContext context) {
    UrlToChecksumMap u2c = new UrlToChecksumMap();
    while (reader.hasMoreChildren()) {
      reader.moveDown();
      if ("entry".equals(reader.getNodeName())) {
        String url = null, checksum = null;
        while (reader.hasMoreChildren()) {
          reader.moveDown();
          if ("url".equals(reader.getNodeName())) {
            url = (String) context.convertAnother(u2c, String.class);
          } else if ("checksum".equals(reader.getNodeName())) {
            checksum = (String) context.convertAnother(u2c, String.class);
          }
          reader.moveUp();
          if (!(StringUtil.isNullString(url) || StringUtil.isNullString(checksum)))
            u2c.put(url, checksum);
        }
      }
      reader.moveUp();
    }
    return u2c;
  }
}
