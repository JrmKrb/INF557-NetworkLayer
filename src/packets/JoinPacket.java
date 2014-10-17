package packets;

/**
 * A {@code JoinPacket} transmits the name of a multicast channel to which one
 * want to subscribe, toward the source of this channel. The source of the
 * channel must be given as the destination of such a packet and this packet
 * must be nested inside a NEXTHOP packets to be forwarded up to the source.
 * Such a packet has no numeric field. The name of the subscribed channel is
 * given as the content field.
 * 
 * 
 * @author Philippe Chassignet
 * @author INF557, DIX, © 2010-2011 École Polytechnique
 * @version 1.2, 2011/11/24
 */

public class JoinPacket extends Packet {
  /**
   * Constructs a {@code JoinPacket}. Such a packet has no numeric field. The
   * name of the subscribed channel will be retrieved through the inherited
   * {@link #getContent() getContent} method.
   * 
   * @param source
   *          the source of the packet
   * @param destination
   *          the destination of the packet, it must be set to the source of the
   *          channel
   * @param channel
   *          the name of the subscribed channel
   */
  public JoinPacket(String source, String destination, String channel) {
    super(source, destination, PacketType.JOIN, channel);
  }
}
