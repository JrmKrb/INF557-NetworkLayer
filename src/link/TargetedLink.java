package link;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import common.Layer;

import packets.Packet;

/**
 * A {@code TargetedLink} is an emulation over UDP of a network interface
 * operating at the link layer with configurable artifacts. In particular,
 * packets intended to be send through this layer, may be artificially lost or
 * delayed, as defined for method {@link Link#send(String,SocketAddress) send}
 * from class {@code Link}. <br/>
 * As the difference with class {@link Link}, an implicit destination address is
 * settled at the construction of a {@code TargetedLink}, to allow an effective
 * implementation of the {@link Layer#send send} method of the
 * {@link common.Layer} interface.
 * <p/>
 * A {@code TargetedLink} that has just been constructed is not ready for
 * working since it is not yet bound. One must first invoke both the
 * {@link #deliverTo deliverTo} method, to specify the {@code Layer} to which
 * the incoming packets will be forwarded, and the {@link #bind bind} method, to
 * specify a local {@code DatagramSocket} which will be operated.
 * 
 * @see MulticastLink
 * @see SwitchedLink
 * 
 * @author Philippe Chassignet
 * @author INF557, DIX, © 2011 École Polytechnique
 * @version 1.2, 2011/09/28
 */
public class TargetedLink extends Link {
  /**
   * The remote socket address used as the implicit destination.
   */
  protected final InetSocketAddress remoteAddress;

  /**
   * Constructs an emulated network interface operating at the link layer and
   * configured to use an implicit destination address. See the documentation of
   * the {@link Link#Link(String) Link constructor} for the prefix based
   * symbolic naming rule.
   * 
   * @param destAddr
   *          a {@code String} to be used as the IP address to which to send
   * @param destPort
   *          a value to be used as the remote port number to which to send
   * @param prefix
   *          used to build a symbolic name which identifies this
   *          {@code TargetedLink} or {@code null} to leave the job to the
   *          {@link #bind bind} method
   */
  public TargetedLink(String destAddr, int destPort, String prefix) {
    super(prefix);
    remoteAddress = new InetSocketAddress(destAddr, destPort);
  }

  /**
   * Sends a {@code String} to the implicit destination address, but this data
   * may be artificially lost or delayed.
   * 
   * @param data
   *          a {@code String} to be sent
   * @see #send(String,SocketAddress)
   */
  protected void send(String data) {
    send(data, remoteAddress);
  }

  /**
   * Sends a {@code Packet} to the implicit destination address, but this packet
   * may be artificially lost or delayed.
   * 
   * @param packet
   *          a {@code Packet} to be sent
   * @see #send(String,SocketAddress)
   */
  @Override
  public void send(Packet packet) {
    send(packet, remoteAddress);
  }

}
