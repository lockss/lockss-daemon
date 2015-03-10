package org.lockss.test;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.lyncode.xoai.model.oaipmh.Header;
import com.lyncode.xoai.serviceprovider.ServiceProvider;
import com.lyncode.xoai.serviceprovider.exceptions.BadArgumentException;
import com.lyncode.xoai.serviceprovider.lazy.ItemIterator;
import com.lyncode.xoai.serviceprovider.model.Context;
import com.lyncode.xoai.serviceprovider.parameters.ListIdentifiersParameters;

public class MockServiceProvider extends ServiceProvider {
  Context context;
  Collection<Header> li;
  
  public MockServiceProvider(Context context) {
    super(context);
    this.context = context;
  }
  
  @Override
  public Iterator<Header> listIdentifiers (ListIdentifiersParameters parameters) throws BadArgumentException {
    if (!parameters.areValid())
        throw new BadArgumentException("ListIdentifiers verb requires the metadataPrefix");
    if (li != null) {
      return li.iterator();
    }
    return new ItemIterator<Header>(null);
  }
  
  public void setIdentifiers( Collection<Header> header) {
    li = header;
  }

}
