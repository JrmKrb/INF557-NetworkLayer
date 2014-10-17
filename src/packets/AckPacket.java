package packets;

/**
 * An {@code AckPacket} is used at the transport link layer and it is sent to
 * acknowledge one or more received packets. Such a packet has a sequence number
 * which must be consistent with that of the acknowledged packets.
 * 
 * @see DataPacket
 * @see NackPacket
 * 
 * @author Philippe Chassignet
 * @author INF557, DIX, © 2010-2011 École Polytechnique
 * @version 1.2, 2011/09/29
 */

public class AckPacket extends Packet {
  /**
   * Constructs an {@code AckPacket}. The sequence number is handled as the
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
  public AckPacket(String source, String destination, int seqNum) {
    super(source, destination, PacketType.ACK, seqNum);
  }
}
