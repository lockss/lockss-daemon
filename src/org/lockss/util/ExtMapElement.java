package org.lockss.util;

public final class ExtMapElement {
  private String elementKey;
  private Object elementValue;

  public ExtMapElement() {
  }

  public ExtMapElement(String key, Object value) {
    this.elementKey = key;
    this.elementValue = value;
  }

  public String getElementKey() {
    return elementKey;
  }

  public void setElementKey(String key) {
    this.elementKey = key;
  }

  public Object getElementValue() {
    return elementValue;
  }

  public void setElementValue(Object value) {
    this.elementValue = value;
  }
}

