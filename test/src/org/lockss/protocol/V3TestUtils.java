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
      VoteBlock vb = V3TestUtils.makeVoteBlock("/test-" + ix + ".html");
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
  
  public static VoteBlock makeVoteBlock(String url) {
    return V3TestUtils.makeVoteBlock(url, 1);
  }

  public static VoteBlock makeVoteBlock(String url, int versions) {
    VoteBlock vb = new VoteBlock(url, VoteBlock.CONTENT_VOTE);
    for (int ix = 0; ix < versions; ix++) {
      vb.addVersion(0L, 1000L, 0L, 1000L,
                    ByteArray.makeRandomBytes(20),
                    ByteArray.makeRandomBytes(20), false);
    }
    return vb;
  }
}
