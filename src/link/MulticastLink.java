package link;

import java.io.IOException;
import java.net.MulticastSocket;
import java.net.SocketAddress;

/**
 * A {@code MulticastLink} is an emulation over UDP of a network interface
 * operating at the link layer with configurable artifacts. In particular,
 * packets intended to be send through this layer, may be artificially lost or
 * delayed, as defined for method {@link Link#send(String,SocketAddress) send}
 * from class {@code Link}. <br/>
 * A {@code MulticastLink} simulates a network with a <i>flat</i> topology where
 * <b>any</b> connected host is directly reachable and will receive the packets
 * that are send by <b>any</b> other host.<br/>
 * It is implemented by listening and sending to a <i>multicast</i> group which
 * is specified by a class D IP address and any standard UDP port number. Class
 * D IP addresses are in the range <tt>224.0.0.0</tt> to
 * <tt>239.255.255.255</tt>, inclusive. The address <tt>224.0.0.0</tt> is
 * reserved and should not be used.
 * <p/>
 * A {@code MulticastLink} that has just been constructed is not ready for
 * working since it is not yet bound upward. One must first invoke the
 * {@link #deliverTo deliverTo} method, to specify the {@code Layer} to which
 * the incoming packets will be forwarded.
 * <p/>
 * In the case where the multicast UDP packets are not routed between different
 * parts of the network (but point to point UDP packets are), one can replace
 * each {@code MulticastLink} by a {@link SwitchedLink} registering with a
 * <i>central switching node</i> that is configured as a virtual hub.
 * 
 * @see SwitchedLink
 * 
 * @author Thomas Clausen
 * @author Philippe Chassignet
 * @author Florian Richoux
 * @author Vincent Siles
 * @author INF557, DIX, © 2010-2011 École Polytechnique
 * @version 1.2, 2011/09/28
 */
public class MulticastLink extends TargetedLink {
  /**
   * Constructs a {@code MulticastLink} configured to use the multicast group
   * defined by the specified class D IP address and the specified the port
   * number. Class D IP addresses are in the range <tt>224.0.0.0</tt> to
   * <tt>239.255.255.255</tt>, inclusive. The address <tt>224.0.0.0</tt> is
   * reserved and should not be used. <br/>
   * 
   * <p/>
   * A {@code MulticastLink} that has just been constructed is not ready for
   * working since it is not yet bound upward. One must first invoke the
   * {@link #deliverTo deliverTo} method, to specify the {@code Layer} to which
   * the incoming packets will be forwarded.
   * 
   * @param groupAddr
   *          a {@code String} to be used as the class D IP address
   * @param groupPort
   *          any standard UDP port number to be used as the multicast port
   *          number
   */
  public MulticastLink(String groupAddr, int groupPort) {
    super(groupAddr, groupPort, "multicast-" + groupAddr + '/' + groupPort);
    MulticastSocket localSocket = null;
    try {
      localSocket = new MulticastSocket(groupPort);
      localSocket.setTimeToLive(4); // configure for sending
      localSocket.joinGroup(remoteAddress.getAddress()); // configure for
                                                         // receiving
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
    bind(localSocket);
  }

  /**
   * The class D IP address {@value} which is used by default for configuring a
   * {@code MulticastLink}.
   * 
   * @see #MulticastLink() MulticastLink()
   * @see #MulticastLink(int) MulticastLink(int)
   */
  public static final String DEFAULT_MULTICAST_ADDRESS = "225.6.7.8";

  /**
   * Constructs a {@code MulticastLink} configured to use the multicast group
   * defined by {@value #DEFAULT_MULTICAST_ADDRESS}, as the default class D IP
   * address, and the specified the port number.
   * 
   * @param groupPort
   *          any standard UDP port number to be used as the multicast port
   *          number
   * @see #MulticastLink(String,int) MulticastLink(String,int)
   */
  public MulticastLink(int groupPort) {
    this(DEFAULT_MULTICAST_ADDRESS, groupPort);
  }

  /**
   * The port number {@value} which is used by default for configuring a
   * {@code MulticastLink}.
   * 
   * @see #MulticastLink() MulticastLink()
   */
  public static final int DEFAULT_MULTICAST_PORT = 11111;

  /**
   * Constructs a {@code MulticastLink} configured to use the multicast group
   * defined by {@value #DEFAULT_MULTICAST_ADDRESS}, as the default class D IP
   * address, and {@value #DEFAULT_MULTICAST_PORT}, as the default port number.
   * 
   * @see #MulticastLink(String,int) MulticastLink(String,int)
   */
  public MulticastLink() {
    this(DEFAULT_MULTICAST_PORT);
  }

}
