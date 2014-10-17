package packets;

/**
 * An {@code AcceptPacket} is used at the transport layer and it is sent by a
 * server to a client as the positive answer to a connection request. Such a
 * packet has three additional fields, a sequence number, a content field being
 * the accepted window size and additional content field giving the
 * acknowledgment number for the received {@link ConnectPacket}.
 * 
 * @see ConnectPacket
 * 
 * @author Philippe Chassignet
 * @author INF557, DIX, © 2010-2012 École Polytechnique
 * @version 1.2, 2012/10/09
 */

public class AcceptPacket extends Packet {
  /**
   * Constructs an {@code AcceptPacket}. The sequence number is handled as the
   * numeric field (see {@link #truncatedNumeric truncations rules}) and will be
   * retrieved through the inherited {@link #getNum() getNum} method. The window
   * size is handled as the content field and will be retrieved as a {@code int}
   * through the inherited {@link #getContentAsInteger() getContentAsInteger}
   * method. The acknowledgment number for the received {@link ConnectPacket} is
   * handled as a second content field and will be retrieved as a {@code int}
   * through the inherited {@link #getContentAsInteger(int)
   * getContentAsInteger(1)} method.
   * 
   * @param source
   *          the source of the packet
   * @param destination
   *          the destination of the packet
   * @param seqNum
   *          the (initial) server-side sequence number
   * @param windowSize
   *          the selected window size
   * @param ackNum
   *          the selected window size
   */
  public AcceptPacket(String source, String destination, int seqNum,
      int windowSize, int ackNum) {
    super(source, destination, PacketType.ACCEPT, seqNum, "" + windowSize, ""
        + ackNum);
  }
}
