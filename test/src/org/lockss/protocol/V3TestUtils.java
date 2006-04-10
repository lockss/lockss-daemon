package org.lockss.protocol;

import java.security.MessageDigest;
import java.util.*;

import org.lockss.util.*;

public class V3TestUtils {
  public static List makeVoteBlockList(int count) {
    ArrayList vbList = new ArrayList();
    for (int ix = 0; ix < count; ix++) {
      String fileName = "/test-" + ix + ".html";
      byte[] hash = V3TestUtils.computeHash(fileName);
      VoteBlock vb =
        new VoteBlock("/test-" + ix + ".html", 1024, 0,
                      1024, 0, hash, hash, VoteBlock.CONTENT_VOTE);
      vbList.add(vb);
    }
    return vbList;
  }  
  
  public static byte[] computeHash(String s) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA");
      digest.update(s.getBytes());
      byte[] hashed = digest.digest();
      return hashed;
    } catch (java.security.NoSuchAlgorithmException e) {
      return new byte[0];
    }
  }
}
