package packets;

/**
 * A {@code DataPacket} is used at the transport layer and it is sent to
 * transmit a wrapped piece of data. Such a packet has two additional fields, a
 * sequence number and the piece of data as the content field.
 * 
 * @see AckPacket
 * @see NackPacket
 * 
 * @author Philippe Chassignet
 * @author INF557, DIX, © 2010-2012 École Polytechnique
 * @version 1.2, 2012/10/11
 */

public class DataPacket extends Packet {
  /**
   * Constructs a {@code DataPacket}. The sequence number is handled as the
   * numeric field (see {@link #truncatedNumeric truncations rules}) and will be
   * retrieved through the inherited {@link #getNum() getNum} method. The data
   * is handled as the content field and will be retrieved through the inherited
   * {@link #getContent getContent} method.
   * 
   * @param source
   *          the source of the packet
   * @param destination
   *          the destination of the packet
   * @param seqNum
   *          the sender-side sequence number
   * @param data
   *          the transported piece of data
   */
  public DataPacket(String source, String destination, int seqNum, String data) {
    super(source, destination, PacketType.DATA, seqNum, data == null ? ""
        : data.replace(DELIMITER, " "));
  }
}
