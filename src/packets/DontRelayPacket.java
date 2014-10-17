package packets;

import java.util.Set;

/**
 * A {@code DontRelayPacket} transmits a set of {@code String} elements to the
 * service of the network layer. It is the set of the names of nodes that don't
 * relay messages in a multicast protocol.<br/>
 * Such a packet has no numeric field. All the elements of the set are appended
 * in the set specific order, as a variable number of additional fields.
 * 
 * @author Philippe Chassignet
 * @author INF557, DIX, © 2010-2011 École Polytechnique
 * @version 1.2, 2012/12/07
 */

public class DontRelayPacket extends Packet {
  /**
   * Constructs a {@code DontRelayPacket}. Such a packet has no numeric field.
   * The transmitted set will be retrieved through the inherited {@link #getSet
   * getSet} method.
   * 
   * @param source
   *          the source of the packet
   * @param destination
   *          the destination of the packet
   * @param set
   *          the set to be embarked
   */
  public DontRelayPacket(String source, String destination, Set<String> set) {
    super(source, destination, PacketType.DONT_RELAY, set
        .toArray(new String[0]));
  }
}
