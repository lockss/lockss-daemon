package org.lockss.laaws;

import org.lockss.servlet.MigrateSettings;
import org.lockss.test.LockssTestCase;
import org.lockss.util.CompoundLinearSlope;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class TestV2AuMover extends LockssTestCase {
//   private static Logger log = Logger.getLogger("TestV2AuMover");

  public void testIsEqualUpToFinalSlash() {
    assertFalse(V2AuMover.isEqualUpToFinalSlash(null, null));
    assertFalse(V2AuMover.isEqualUpToFinalSlash("foo", null));
    assertFalse(V2AuMover.isEqualUpToFinalSlash(null, "foo"));
    assertFalse(V2AuMover.isEqualUpToFinalSlash(null, "foo/"));
    assertFalse(V2AuMover.isEqualUpToFinalSlash("foo", "foo"));
    assertTrue(V2AuMover.isEqualUpToFinalSlash("foo", "foo/"));
    assertFalse(V2AuMover.isEqualUpToFinalSlash("foo", "foo//"));
    assertFalse(V2AuMover.isEqualUpToFinalSlash("foo/", "foo"));
    assertTrue(V2AuMover.isEqualUpToFinalSlash("foo", "foo/"));
  }

  public void testParseErrorBodyFlat() {
    String body = "{\"timestamp\":1700000000000,\"status\":500,"
      + "\"error\":\"Internal Server Error\","
      + "\"exception\":\"java.lang.IllegalStateException\","
      + "\"message\":\"No space left on device\","
      + "\"path\":\"/aus/foo\"}";
    LockssRestHttpException lrhe = V2AuMover.parseErrorBody(500, body);
    assertNotNull(lrhe);
    assertEquals(500, lrhe.getStatus());
    assertEquals(1700000000000L, lrhe.getTimestamp());
    assertEquals("Internal Server Error", lrhe.getError());
    assertEquals("java.lang.IllegalStateException", lrhe.getException());
    assertEquals("No space left on device", lrhe.getMessage());
    assertEquals("/aus/foo", lrhe.getPath());
  }

  public void testParseErrorBodyNested() {
    String body = "{\"error\":{\"code\":500,\"message\":"
      + "\"Cannot update the poll agreements for auid = 'x' - boom\"}}";
    LockssRestHttpException lrhe = V2AuMover.parseErrorBody(503, body);
    assertNotNull(lrhe);
    assertEquals(500, lrhe.getStatus());
    assertEquals("Cannot update the poll agreements for auid = 'x' - boom",
                 lrhe.getMessage());
    assertEquals("{\"code\":500,\"message\":"
                 + "\"Cannot update the poll agreements for auid = 'x' - boom\"}",
                 lrhe.getError());

    // code absent: status comes from the HTTP status
    lrhe = V2AuMover.parseErrorBody(507, "{\"error\":{\"message\":\"m\"}}");
    assertEquals(507, lrhe.getStatus());
    assertEquals("m", lrhe.getMessage());

    // message absent: message is the raw body
    lrhe = V2AuMover.parseErrorBody(503, "{\"error\":{\"code\":500}}");
    assertEquals(500, lrhe.getStatus());
    assertEquals("{\"error\":{\"code\":500}}", lrhe.getMessage());
  }

  public void testParseErrorBodyEmpty() {
    LockssRestHttpException lrhe = V2AuMover.parseErrorBody(502, "");
    assertNotNull(lrhe);
    assertEquals(502, lrhe.getStatus());
    assertEquals("", lrhe.getMessage());

    lrhe = V2AuMover.parseErrorBody(502, null);
    assertNotNull(lrhe);
    assertEquals(502, lrhe.getStatus());
    assertEquals("", lrhe.getMessage());

    lrhe = V2AuMover.parseErrorBody(502, "  \n ");
    assertEquals(502, lrhe.getStatus());
    assertEquals("", lrhe.getMessage());
  }

  public void testParseErrorBodyNonJson() {
    // lenient parser accepts a bare word as a string primitive
    LockssRestHttpException lrhe = V2AuMover.parseErrorBody(500, "notjson");
    assertNotNull(lrhe);
    assertEquals(500, lrhe.getStatus());
    assertEquals("notjson", lrhe.getMessage());

    // trailing content makes the parser throw
    lrhe = V2AuMover.parseErrorBody(500, " <html>Bad Gateway</html> ");
    assertEquals(500, lrhe.getStatus());
    assertEquals("<html>Bad Gateway</html>", lrhe.getMessage());

    // JSON but not an object
    lrhe = V2AuMover.parseErrorBody(500, "[1,2]");
    assertEquals(500, lrhe.getStatus());
    assertEquals("[1,2]", lrhe.getMessage());

    // flat shape with a non-numeric status: Gson would throw
    lrhe = V2AuMover.parseErrorBody(500, "{\"status\":\"abc\",\"message\":\"x\"}");
    assertEquals(500, lrhe.getStatus());
    assertEquals("{\"status\":\"abc\",\"message\":\"x\"}", lrhe.getMessage());
  }

  public void testIsNonRetryableResponseNestedNoSpace() {
    String noSpace = "{\"error\":{\"code\":500,\"message\":"
      + "\"Cannot update the poll agreements for auid = 'x' - "
      + "No space left on device\"}}";
    assertTrue(V2AuMover.isNonRetryableResponse
               (500, V2AuMover.parseErrorBody(500, noSpace)));
    assertTrue(V2AuMover.isNonRetryableResponse
               (507, V2AuMover.parseErrorBody(507, noSpace)));
    String other = "{\"error\":{\"code\":500,\"message\":\"deadlock\"}}";
    assertFalse(V2AuMover.isNonRetryableResponse
                (500, V2AuMover.parseErrorBody(500, other)));
    assertFalse(V2AuMover.isNonRetryableResponse
                (500, V2AuMover.parseErrorBody(500, "")));
    assertFalse(V2AuMover.isNonRetryableResponse(500, null));
    assertTrue(V2AuMover.isNonRetryableResponse(404, null));
    assertFalse(V2AuMover.isNonRetryableResponse(503, null));
  }

  public void testDefaultCurves() {
    new CompoundLinearSlope(V2AuMover.DEFAULT_DISK_SPACE_BYTES_CURVE);
    new CompoundLinearSlope(V2AuMover.DEFAULT_DISK_SPACE_ARTIFACTS_CURVE);
    new CompoundLinearSlope(V2AuMover.DEFAULT_DB_SIZE_CHECK_CURVE);
  }



}
