package edu.gla.kail.ad.agents;

import org.apache.commons.codec.binary.Base64;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Generate a response identifier. This is based on Firestore's Push ID generator.
 * It is from:
 * https://gist.github.com/jfbyers/d142503b2e41556b5684
 */
public class ResponseIdGenerator {

  // Modeled after base64 web-safe chars, but ordered by ASCII.
  private final static String PUSH_CHARS = "-0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz";

  // Timestamp of last push, used to prevent local collisions if you push twice in one ms.
  private static long LAST_PUSH_TIME = 0L;

  // We generate 72-bits of randomness which get turned into 12 characters and
  // appended to the timestamp to prevent collisions with other clients. We store the last
  // characters we generated because in the event of a collision, we'll use those same
  // characters except "incremented" by one.
  private static int[] LAST_RAND_CHARS = new int[12];

  /**
   * Fancy ID generator that creates 20-character string identifiers with the
   * following properties:
   *
   * 1. They're based on timestamp so that they sort *after* any existing ids.
   * 2. They contain 72-bits of random data after the timestamp so that IDs won't
   * collide with other clients' IDs.
   * 3. They sort *lexicographically* (so the timestamp is converted to characters
   * that will sort properly).
   * 4. They're monotonically increasing. Even if you generate more than one in
   * the same timestamp, the
   * latter ones will sort after the former ones. We do this by using the previous
   * random bits
   * but "incrementing" them by 1 (only in the case of a timestamp collision).
   *
   * @return
   */
  public static synchronized String generate() {

    long now = System.currentTimeMillis();
    boolean duplicateTime = now == LAST_PUSH_TIME;
    LAST_PUSH_TIME = now;

    char[] timeStampChars = new char[8];
    for (int i = 7; i >= 0; i--) {
      timeStampChars[i] = PUSH_CHARS.charAt((int)(now % 64));
      now = (long) Math.floor(now / 64);
    }

    if (now != 0) {
      throw new AssertionError("We should have converted the entire timestamp.");
    }

    StringBuilder id = new StringBuilder(20);
    for (char c : timeStampChars) {
      id.append(c);
    }

    if (!duplicateTime) {
      for (int i = 0; i < 12; i++) {
        LAST_RAND_CHARS[i] = (int) Math.floor(Double.valueOf(Math.random() * 64).intValue());
      }
    } else {
      // If the timestamp hasn't changed since last push, use the same random number,
      //except incremented by 1.
      int i=0;
      for (i = 11; i >= 0 && LAST_RAND_CHARS[i] == 63; i--) {
        LAST_RAND_CHARS[i] = 0;
      }
      LAST_RAND_CHARS[i]++;
    }

    for (int i = 0; i < 12; i++) {
      id.append(PUSH_CHARS.charAt(LAST_RAND_CHARS[i]));
    }

    if (id.length() != 20) {
      throw new AssertionError("Length should be 20.");
    }

    return id.toString();
  }

  /**
   * Generate a base-64 encoded string that is a GUID for the response identifier.
   * Consider a response identifier that includes time + randomness, similar to Firestore: 48 bits of timestamp +
   * 72 bits of randomness; see https://gist.github.com/mikelehen/3596a30bd69384624c11.
   * TODO(Jeff): Factor ID generation out into a library for generating identifiers.
   * @return a base-64 encoded string GUID
   */
  public static synchronized String generateBase64UUID() {
    UUID uuid = UUID.randomUUID();
    Base64 base64 = new Base64();
    ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
    bb.putLong(uuid.getMostSignificantBits());
    bb.putLong(uuid.getLeastSignificantBits());
    return base64.encodeBase64URLSafeString(bb.array());
  }


}