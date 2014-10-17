package link;

import packets.Packet;

import common.Layer;
import common.TopLayer;

/**
 * This program displays informations on every packet received from the
 * specified instance of the link layer. The number of arguments to the
 * {@code main} function determines which type of link is used.
 * <ul>
 * <li>With zero or two arguments, a {@link MulticastLink} is used:
 * <ul>
 * <li><tt>java link/Listener [ multicastAddr multicastPort ]</tt></li>
 * </ul>
 * When the optional arguments are both given, they define the multicast socket
 * address. When the optional arguments are both omitted, the default
 * {@code MulticastLink} is used, with address
 * {@value link.MulticastLink#DEFAULT_MULTICAST_ADDRESS} and port
 * {@value link.MulticastLink#DEFAULT_MULTICAST_PORT}.</li>
 * <p/>
 * <li>With exactly three arguments, a {@link SwitchedLink} is used:
 * <ul>
 * <li><tt>java link/Listener serverAddr serverPort listenerName</tt></li>
 * </ul>
 * The two first arguments define the socket address of the <i>central switching
 * node</i> and the third argument is used to register this {@code Listener}
 * with the central switching node.</li>
 * </ul>
 * 
 * @author Philippe Chassignet
 * @author INF557, DIX, © 2011 École Polytechnique
 */
public class Listener {

  private Listener() { // Don't instantiate this class.
  }

  /** See the class documentation above. */
  public static void main(String[] args) {
    if (args.length != 0 && args.length != 2 && args.length != 3) {
      System.err
          .println("syntax : java link/Listener [ multicastAddr multicastPort ] [ serverAddr serverPort listenerName ]");
      return;
    }
    TargetedLink link = null;
    if (args.length == 0) {
      System.out.println("launched on default multicast group");
      link = new MulticastLink();
    } else if (args.length == 2) {
      link = new MulticastLink(args[0], Integer.parseInt(args[1]));
    } else
      link = new SwitchedLink(args[0], Integer.parseInt(args[1]), -1, args[2]);

    // packets received by the link will be thrown to this new Layer
    link.deliverTo(new TopLayer() {
      public void receive(Packet packet, Layer from) {
        System.out.println(packet);
        /*
         * System.out.println("packet is \"" + packet + '"');
         * System.out.println("source " + packet.getSource());
         * System.out.println("destination " + packet.getDestination());
         * System.out.println("type " + packet.getType());
         * System.out.println("raw type " + packet.getRawType());
         * System.out.println("num " + packet.getNum());
         * System.out.println("content \"" + packet.getContent() + '"');
         * System.out.println("sub packet \"" + packet.getInnerPacket() + '"');
         */
      }
    });
  }

}
