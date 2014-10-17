package packets;

/**
 * A {@code ClosePacket} is used at the transport link layer and it is sent to
 * notify the other side of the end of the transmission in this direction. Such
 * a packet has a sequence number which must be consistent with that of the
 * preceding packet.
 * 
 * @author Philippe Chassignet
 * @author INF557, DIX, © 2010-2011 École Polytechnique
 * @version 1.2, 2011/09/29
 */

public class ClosePacket extends Packet {
  /**
   * Constructs a {@code ClosePacket}. The sequence number is handled as the
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
  public ClosePacket(String source, String destination, int seqNum) {
    super(source, destination, PacketType.CLOSE, seqNum);
  }
}
