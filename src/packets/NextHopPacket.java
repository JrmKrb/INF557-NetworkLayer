package packets;

/**
 * A {@code NextHopPacket} is used at the network layer to transmit a nested
 * packet for a single step in a multi-hop network. Such a packet has a numeric
 * field, used as the TTL, and all the fields of the nested packet are pushed as
 * a variable number of additional fields.
 * 
 * @author Philippe Chassignet
 * @author INF557, DIX, © 2010-2011 École Polytechnique
 * @version 1.2, 2011/10/13
 */

public class NextHopPacket extends Packet {
  /**
   * Constructs a {@code NextHopPacket}. The TTL is handled as the numeric field
   * and will be retrieved through the inherited {@link #getNum() getNum}
   * method. The nested packet will be retrieved through the inherited
   * {@link #getNestedPacket getNestedPacket} method.
   * 
   * @param source
   *          the source of the packet
   * @param destination
   *          the destination of the packet
   * @param ttl
   *          the integer value for the TTL
   * @param packet
   *          the packet to be embarked as the payload
   */
  public NextHopPacket(String source, String destination, int ttl, Packet packet) {
    super(source, destination, PacketType.NEXTHOP, ttl, packet.fields);
  }
}
