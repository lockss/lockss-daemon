package org.lockss.rs.client;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.internal.Util;
import java.io.IOException;
import java.io.InputStream;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

public class InputStreamRequestBody extends RequestBody {
  private final InputStream inputStream;
  private final MediaType contentType;

  public InputStreamRequestBody(MediaType contentType, InputStream inputStream) {
    if (inputStream == null) throw new NullPointerException("inputStream == null");
    this.contentType = contentType;
    this.inputStream = inputStream;
  }

  @Override
  public MediaType contentType() {
    return contentType;
  }

  @Override
  public long contentLength() throws IOException {
    return inputStream.available() == 0 ? -1 : inputStream.available();
  }

  @Override
  public void writeTo(BufferedSink sink) throws IOException {
    Source source = null;
    try {
      source = Okio.source(inputStream);
      sink.writeAll(source);
    } finally {
      Util.closeQuietly(source);
    }

  }
}
