package packets;

/**
 * A {@code LinkPacket} is used internally in the link layer emulator by a
 * {@code SwitchedLink} to register with a <i>central switching node</i>. Such a
 * packet has no additional field.
 * 
 * @see link.SwitchedLink
 * @see link.Matrix
 * 
 * @author Philippe Chassignet
 * @author INF557, DIX, © 2010-2011 École Polytechnique
 * @version 1.2, 2011/09/29
 */

public class LinkPacket extends Packet {
  /**
   * Constructs a {@code LinkPacket}.
   * 
   * @param source
   *          the source of the packet
   * @param destination
   *          the destination of the packet
   */
  public LinkPacket(String source, String destination) {
    super(source, destination, PacketType.LINK);
  }
}
