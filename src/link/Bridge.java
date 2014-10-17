package link;

import java.util.HashSet;

import packets.Packet;
import packets.PacketType;

import common.Layer;
import common.TopLayer;

/**
 * This program may constitute one half of an unicast bridge between two
 * multicast groups. It is useful in the case where multicast packets are not
 * routed between two different networks while unicast packets are. Each half is
 * linked twice, to its multicast group through a {@link MulticastLink}, and to
 * the other half of the bridge through a {@link SwitchedLink}. Packets incoming
 * from the multicast side are forwarded to the unicast side and vice versa.
 * <p/>
 * Syntax :
 * <tt>java link/Bridge localPort bridgeAddr bridgePort [ multicastAddr multicastPort ]</tt>
 * <p/>
 * To establish the bridge using an unicast link between <tt>port1</tt> on
 * <tt>host1</tt> and <tt>port2</tt> on <tt>host2</tt>, one should enter the two
 * commands:
 * <ul>
 * <li>on <tt>host1</tt>
 * <ul>
 * <li>
 * <tt>java link/Bridge port1 host2 port2 [ multicastAddr multicastPort ]</tt></li>
 * </ul>
 * </li>
 * <li>on <tt>host2</tt>
 * <ul>
 * <li>
 * <tt>java link/Bridge port2 host1 port1 [ multicastAddr multicastPort ]</tt></li>
 * </ul>
 * </li>
 * </ul>
 * <p/>
 * When the optional arguments are both given, they define the multicast socket
 * address. When the optional arguments are both omitted, the default
 * {@code MulticastLink} is used, with address
 * {@value link.MulticastLink#DEFAULT_MULTICAST_ADDRESS} and port
 * {@value link.MulticastLink#DEFAULT_MULTICAST_PORT}.
 * 
 * <p/>
 * Since the unicast link is implemented as a {@link SwitchedLink}, a single
 * instance of the {@code Bridge} program may also register with a <i>central
 * switching node</i> using a canonical name. In this case, the
 * <tt>localPort</tt> argument is of little concern and the <tt>bridgeAddr</tt>
 * and <tt>bridgePort</tt> arguments are defining the socket address of the
 * central switching node.<br/>
 * This allows, at the central switching node, to consider a large set of nodes
 * connected to the multicast group, as a whole, identified by the canonical
 * name of this {@code Bridge}.
 * 
 * @see Matrix
 * 
 * @author Philippe Chassignet
 * @author INF557, DIX, © 2010-2011 École Polytechnique
 */

public class Bridge {

  private Bridge() { // Don't instantiate this class.
  }

  private static SwitchedLink bridge;
  private static MulticastLink multicast;

  private static HashSet<String> onBridgeSide = new HashSet<String>();
  private static HashSet<String> onMulticastSide = new HashSet<String>();

  /** See the class documentation above. */
  public static void main(String[] args) {
    if (args.length != 3 && args.length != 5) {
      System.err
          .println("syntax : java link/Bridge localPort bridgeAddr bridgePort [ multicastAddr multicastPort ]");
      return;
    }

    if (args.length == 3) {
      System.out.println("Multicast is launched on the default group");
      multicast = new MulticastLink();
    } else {
      String multicastAddr = args[3];
      int multicastPort = Integer.parseInt(args[4]);
      System.out.println("Multicast is launched on group " + multicastPort
          + '@' + multicastAddr);
      multicast = new MulticastLink(multicastAddr, multicastPort);
    }
    multicast.setSuccessRate(100);
    multicast.setMaxDelay(0);

    int localPort = Integer.parseInt(args[0]);
    System.out.println("Local port for bridging is " + localPort);
    String bridgeAddr = args[1];
    int bridgePort = Integer.parseInt(args[2]);
    System.out.println("Remote port for bridging is " + bridgePort + '@'
        + bridgeAddr);
    bridge = new SwitchedLink(bridgeAddr, bridgePort, localPort, null);
    bridge.setSuccessRate(100);
    bridge.setMaxDelay(0);

    bridge.deliverTo(new TopLayer() {
      @SuppressWarnings("synthetic-access")
      public void receive(Packet packet, Layer from) {
        if (packet == null)
          return;
        String source = packet.getSource();
        PacketType type = packet.getType();
        System.out.println("from Bridge : " + packet);
        if (type != PacketType.LINK && !onMulticastSide.contains(source)) {
          onBridgeSide.add(source);
          multicast.send(packet);
        }
      }
    });

    multicast.deliverTo(new TopLayer() {
      @SuppressWarnings("synthetic-access")
      public void receive(Packet packet, Layer from) {
        if (packet == null)
          return;
        String source = packet.getSource();
        PacketType type = packet.getType();
        System.out.println("from Multicast : " + packet);
        if (type != PacketType.LINK && !onBridgeSide.contains(source)) {
          onMulticastSide.add(source);
          bridge.send(packet);
        }
      }
    });
  }
}
