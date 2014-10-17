package packets;

import java.util.regex.Pattern;

/**
 * A {@code RawPacket} is used as a wrapper to keep track of a badly formatted
 * packet. For such a packet, even the required fields, as source, destination
 * and type may be undefined. Several {@code get...} methods are redefined to
 * try to recover the corresponding data from the initial raw packet.
 * 
 * @author Philippe Chassignet
 * @author INF557, DIX, © 2010-2011 École Polytechnique
 * @version 1.2, 2011/09/29
 */

public class RawPacket extends Packet {
  private final String rawContent;
  private final String[] rawFields;

  /**
   * Constructs a {@code RawPacket}.
   * 
   * @param raw
   *          the raw packet
   */
  RawPacket(String raw) {
    super(null, null, PacketType.RAW);
    rawContent = raw;
    rawFields = decode(raw);
  }

  @Override
  public String getSource() {
    return getField(Field.SOURCE, rawFields);
  }

  @Override
  public String getDestination() {
    return getField(Field.DESTINATION, rawFields);
  }

  @Override
  public String getRawType() {
    return getField(Field.TYPE, rawFields);
  }

  private static Pattern NUMBER = Pattern.compile("[0-9]+");

  @Override
  public int getNum() {
    String num = getField(Field.NUMERIC, rawFields);
    // avoid throwing an exception in most cases
    if (num != null && NUMBER.matcher(num).matches())
      try {
        return Integer.parseInt(num);
      } catch (NumberFormatException e) {
        return Integer.MIN_VALUE;
      }
    return -1;
  }

  @Override
  public String getContent() {
    return getField(Field.CONTENT, rawFields);
  }

  @Override
  public Packet getNestedPacket() {
    return buildPacketFrom(encode(rawFields, Field.CONTENT.ordinal()));
  }

  @Override
  public String getEncoding() {
    return rawContent;
  }

}
