package packets;

/**
 * A {@code ConnectPacket} is used at the transport layer and it is sent by a
 * client to a server to request the opening of a connection. Such a packet has
 * two additional fields, a sequence number and a content field being the
 * proposed window size.
 * 
 * @see AcceptPacket
 * 
 * @author Philippe Chassignet
 * @author INF557, DIX, © 2010-2011 École Polytechnique
 * @version 1.2, 2011/09/29
 */

public class ConnectPacket extends Packet {
  /**
   * Constructs a {@code ConnectPacket}. The sequence number is handled as the
   * numeric field (see {@link #truncatedNumeric truncations rules}) and will be
   * retrieved through the inherited {@link #getNum() getNum} method. The window
   * size is handled as the content field and will be retrieved as a
   * {@code String} through the inherited {@link #getContent getContent} method.
   * 
   * @param source
   *          the source of the packet
   * @param destination
   *          the destination of the packet
   * @param seqNum
   *          the (initial) client-side sequence number
   * @param windowSize
   *          the proposed window size
   */
  public ConnectPacket(String source, String destination, int seqNum,
      int windowSize) {
    super(source, destination, PacketType.CONNECT, seqNum, "" + windowSize);
  }
}
