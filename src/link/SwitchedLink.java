package link;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.TimerTask;

import packets.LinkPacket;

/**
 * A {@code SwitchedLink} is an emulation over UDP of a network interface
 * operating at the link layer with configurable artifacts. In particular,
 * packets intended to be send through this layer, may be artificially lost or
 * delayed, as defined for method {@link Link#send(String,SocketAddress) send}
 * from class {@code Link}.
 * <p/>
 * A {@code SwitchedLink} first registers with a <i>central switching node</i>
 * (see {@link Matrix}), specified by the IP address of the host where it runs,
 * and by its listening port number. Then, the {@code SwitchedLink} sends its
 * outgoing UDP packets to the switching node and the switching node forwards
 * any received packet to the <b>subset</b> of registered {@code SwitchedLink}
 * instances, corresponding to the neighborhood currently defined for the
 * sending {@code SwitchedLink}. <br/>
 * Thus, depending on the type of connectivity managed by the central switching
 * node, it is possible to simulate a variety of network topologies, such as a
 * single link, a collection of independent links and even any non-symmetric
 * non-transitive connectivity. <br/>
 * This should not be considered as routing but rather as wired switching, since
 * the central switching node does not analyze the contents of the packets and
 * the forwarding rules are only based on the incoming link.
 * <p/>
 * To register with the central switching node, a {@code SwitchedLink}
 * periodically sends its symbolic name as the source of a special packet of
 * type {@link packets.LinkPacket}. This name is then used as a key to manage
 * the connectivity rules inside the switching node.
 * <p/>
 * A {@code SwitchedLink} that has just been constructed is not ready for
 * working since it is not yet bound upward. One must first invoke the
 * {@link #deliverTo deliverTo} method, to specify the {@code Layer} to which
 * the incoming packets will be forwarded.
 * 
 * @see Matrix
 * @see HubsPool
 * @see MulticastLink
 * 
 * @author Thomas Clausen
 * @author Philippe Chassignet
 * @author Florian Richoux
 * @author Vincent Siles
 * @author INF557, DIX, © 2010-2011 École Polytechnique
 * @version 1.1, 2011/09/28
 */
public class SwitchedLink extends TargetedLink {
  /**
   * The delay (in <b>milliseconds</b>) between two consecutive packets of type
   * {@link packets.LinkPacket} sent by a {@code SwitchedLink} to register with
   * the central switching node; this delay is set to {@value} (milliseconds).
   */
  public static final int LINK_REPEAT_DELAY = 10000; // milliseconds

  /**
   * The symbolic name {@value} used in packets of type
   * {@link packets.LinkPacket} to identify the central switching node as the
   * destination.
   */
  public static final String LINK_DESTINATION = "*LINK*";

  /**
   * Constructs a {@code SwitchedLink}, bound to a local port, and that
   * registers with the central switching node specified by an IP address and a
   * port number. See the documentation of the {@link Link#Link(String) Link
   * constructor} for the prefix based symbolic naming rule. The resulting name
   * is used as the source of the packets of type {@link packets.LinkPacket}
   * which are sent by this {@code SwitchedLink} to register with the central
   * switching node.
   * 
   * @param centralHost
   *          a {@code String} to be used as the IP address of the host where
   *          the central switching node is running
   * @param centralPort
   *          a value to be used as the port number that the central switching
   *          node is listening
   * @param localPort
   *          a value to be used as the local port number. If a value less than
   *          0 is given, any available port will be used.
   * @param prefix
   *          used to build a symbolic name which identifies this
   *          {@code SwitchedLink} or {@code null} to leave the job to the
   *          {@link #bind bind} method (called by this constructor)
   */
  public SwitchedLink(String centralHost, int centralPort, int localPort,
      String prefix) {
    super(centralHost, centralPort, prefix);
    DatagramSocket localSocket = null;
    try {
      if (localPort < 0)
        localSocket = new DatagramSocket();
      else
        localSocket = new DatagramSocket(localPort);
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
    bind(localSocket);
    final DatagramPacket linkPacket = wrap(new LinkPacket(toString(),
        LINK_DESTINATION), remoteAddress);
    TIMER.schedule(new TimerTask() {
      @Override
      public void run() {
        send(linkPacket);
      }
    }, 0, LINK_REPEAT_DELAY);
  }

}
