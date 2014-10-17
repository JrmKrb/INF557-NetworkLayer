package packets;

/**
 * An {@code HelloPacket} is used at the network layer by a node to tell its own
 * existence and so register with its neighbors. Such a packet has no additional
 * field.
 * 
 * @author Philippe Chassignet
 * @author INF557, DIX, © 2010-2011 École Polytechnique
 * @version 1.2, 2011/10/13
 */

public class HelloPacket extends Packet {
  /**
   * Constructs a {@code LinkPacket}.
   * 
   * @param source
   *          the source of the packet
   * @param destination
   *          the destination of the packet
   */
  public HelloPacket(String source, String destination) {
    super(source, destination, PacketType.HELLO);
  }
}
