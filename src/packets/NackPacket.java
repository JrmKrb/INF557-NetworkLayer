package packets;

/**
 * A {@code NackPacket} is used at the transport link layer and it is sent to
 * tell the other side that an expected packet is missing. Such a packet has a
 * sequence number which must be consistent with that of the missing packet.
 * 
 * @see DataPacket
 * @see AckPacket
 * 
 * @author Philippe Chassignet
 * @author INF557, DIX, © 2010-2011 École Polytechnique
 * @version 1.2, 2011/09/29
 */

public class NackPacket extends Packet {
  /**
   * Constructs a {@code NackPacket}. The sequence number is handled as the
   * numeric field (see {@link #truncatedNumeric truncations rules}) and will be
   * retrieved through the inherited {@link #getNum() getNum} method.
   * 
   * @param source
   *          the source of the packet
   * @param destination
   *          the destination of the packet
   * @param seqNum
   *          the sequence number
   */
  public NackPacket(String source, String destination, int seqNum) {
    super(source, destination, PacketType.NACK, seqNum);
  }
}
