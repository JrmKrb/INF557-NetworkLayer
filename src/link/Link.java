package link;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import packets.Packet;

import common.Layer;

/**
 * A {@code Link} is an emulation over UDP of a network interface operating at
 * the link layer with configurable artifacts. In particular, packets intended
 * to be send through this layer, may be artificially lost or delayed, see
 * {@link #send(String,SocketAddress) send}. <br/>
 * This class is a raw implementation that is to be rather considered as the
 * ground for more practicable subclasses which better implement the
 * {@link common.Layer Layer} interface. Thus, this class provides methods to
 * send and receive typed packets that are instances of the
 * {@link packets.Packet} abstract class. For their effective delivery, these
 * packets are wrapped inside standard datagram packets.
 * <p/>
 * A {@code Link} that has just been constructed is not ready for working since
 * it is not yet bound. One must first invoke both the {@link #deliverTo
 * deliverTo} method, to specify the {@code Layer} to which the incoming packets
 * will be forwarded, and the {@link #bind bind} method, to specify a local
 * {@code DatagramSocket} which will be operated.
 * 
 * @see TargetedLink
 * @see MulticastLink
 * @see SwitchedLink
 * 
 * @author Thomas Clausen
 * @author Philippe Chassignet
 * @author Florian Richoux
 * @author Vincent Siles
 * @author INF557, DIX, © 2010-2013 École Polytechnique
 * @version 1.3, 2013/10/15
 */
public class Link implements Layer {

  // the random number generator used for sending artifacts
  private static final Random RAND = new Random();

  /**
   * The maximal value for the sending success rate, it is set at {@value}
   * meaning <em>always succeed</em> (more precisely, no artificial loss).
   * 
   * @see #send(Packet,SocketAddress) send
   * @see #setSuccessRate setSuccessRate
   */
  public static final int MAX_SUCCESS_RATE = 100;

  /**
   * The maximal absolute limit (in <b>milliseconds</b>) for the random delay
   * before sending a packet, it is set at {@value} (milliseconds).
   * 
   * @see #send(Packet,SocketAddress) send
   * @see #setMaxDelay setMaxDelay
   */
  public static final int MAX_SEND_DELAY = 400; // milliseconds

  /**
   * The default maximal value (in <b>milliseconds</b>) for the random delay
   * before sending a packet, it is set at {@value} (milliseconds).
   * 
   * @see #send(Packet,SocketAddress) send
   */
  public static final int DEFAULT_SEND_DELAY = 200; // milliseconds

  /**
   * This {@code Charset} is used to convert between the Java native String
   * encoding for our packets and a chosen encoding for the effective packets
   * that fly over the network.
   */
  private static final Charset CONVERTER = Charset.availableCharsets().get(
      "UTF-8");

  /**
   * Converts data from the Java native {@code String} encoding to a
   * conventional encoding for effective networking.
   * 
   * @param data
   *          a {@code String} to be encoded
   * @return a {@code ByteBuffer} containing the encoded data
   */
  private static ByteBuffer encode(String data) {
    return CONVERTER.encode(data);
  }

  /**
   * Builds a ready-to-send datagram packet from a {@code String} containing the
   * data to send, and the destination (host+port) socket address.
   * 
   * @param data
   *          a {@code String} to be send
   * @param destination
   *          the specified destination address
   * @return a {@code DatagramPacket}
   */
  private static DatagramPacket wrap(String data, SocketAddress destination) {
    ByteBuffer buf = encode(data);
    byte[] buf2 = new byte[buf.limit()];
    buf.get(buf2);
    DatagramPacket realPacket = null;
    try {
      realPacket = new DatagramPacket(buf2, buf2.length, destination);
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
    return realPacket;
  }

  /**
   * Encodes a {@link packets#Packet Packet} to a conventional encoding for
   * effective networking.
   * 
   * @param packet
   *          a {@code Packet} to be encoded
   * @return a {@code ByteBuffer} containing the encoded packet
   */
  @SuppressWarnings("unused")
  private static ByteBuffer encode(Packet packet) {
    return encode(packet.getEncoding());
  }

  /**
   * Builds a ready-to-send datagram packet from a {@link packets#Packet Packet}
   * and the destination address.
   * 
   * @param packet
   *          a {@code Packet} to be send
   * @param destination
   *          the specified destination address
   * @return a {@code DatagramPacket}
   */
  protected static DatagramPacket wrap(Packet packet, SocketAddress destination) {
    return wrap(packet.getEncoding(), destination);
  }

  /**
   * This internal timer is used for sending delayed packets. It may also be
   * used for various ancillary tasks.
   * 
   */
  protected final Timer TIMER = new Timer("LinkLayer_Timer", true);

  /**
   * An incremental number used to identify the different links.
   */
  private static int nbLinks = 0;

  /**
   * The symbolic name of this {@code Link}, returned by {@link #toString}.
   */
  private String name;

  private int successRate = MAX_SUCCESS_RATE; // the success current rate
  private int maxDelay = DEFAULT_SEND_DELAY; // the current maximal delay
  private volatile DatagramSocket localSocket = null; // the socket used for
                                                      // receiving and sending
  private Thread receiver; // for the receiving loop
  private volatile Layer aboveLayer; // the layer to which incoming packets are
                                     // forwarded

  /**
   * The core constructor for a {@code Link}. The new object is not ready for
   * working since it is not yet bound. One must first invoke both the
   * {@link #deliverTo deliverTo} method, to specify the {@code Layer} to which
   * the incoming packets will be forwarded, and the {@link #bind bind} method,
   * to specify a local {@code DatagramSocket} which will be operated.
   * <p/>
   * When the {@code prefix} argument is not {@code null}, a link number is
   * automatically append to it to build a symbolic name of the form
   * <tt>"prefix-number"</tt> that identifies this {@code Link}.<br/>
   * When the {@code prefix} argument is {@code null}, building a symbolic name
   * is left to the {@code bind} method.
   * 
   * @see #bind bind
   * @see #deliverTo deliverTo
   * @param prefix
   *          used to build a symbolic name which identifies this {@code Link},
   *          or {@code null} to leave the job to the {@code bind} method
   */
  protected Link(String prefix) {
    if (prefix != null)
      name = prefix + '-' + (++nbLinks);
  }

  public void deliverTo(Layer above) {
    aboveLayer = above;
  }

  /**
   * Binds this {@code Link} to the specified local socket and starts receiving.
   * One must first invoke the {@link #deliverTo deliverTo} method, to specify
   * the above layer to which the incoming packets will be forwarded. The
   * {@code bind} method launches a new thread that receives any incoming
   * {@code DatagramPacket} from the socket and pushes its content as a
   * {@link packets.Packet} to the above layer.
   * <p/>
   * When a symbolic name was not given by the {@link #Link(String) Link
   * constructor}, it is now build from the socket parameters.
   * 
   * @see #Link(String)
   * @see #deliverTo deliverTo
   * @see #receive receive
   * @param socket
   *          the datagram socket used to send and receive through UDP
   */
  protected void bind(DatagramSocket socket) {
    if (name == null) {
      try {
        name = InetAddress.getLocalHost().getCanonicalHostName();
      } catch (UnknownHostException e) {
        name = "unknownHost";
      }
      name += "-" + socket.getLocalPort();
    }
    localSocket = socket;
    // We create and launch the thread that will wait for incoming messages.
    receiver = new Thread(this.name + "_receiver") {
      @SuppressWarnings("synthetic-access")
      @Override
      public void run() {
        // a single instance of buffer is enough to receive
        byte[] buffer = new byte[16384]; // such a size should be enough !
        // this buffer is shared as storage by a DatagramPacket
        DatagramPacket realPacket = new DatagramPacket(buffer, buffer.length);
        // and a ByteBuffer
        ByteBuffer bytes = ByteBuffer.wrap(buffer);
        while (localSocket != null && !Thread.interrupted()) {
          try {
            localSocket.receive(realPacket);
            bytes.position(0); // must reset offset
            bytes.limit(realPacket.getLength()); // and length
            CharBuffer chars = CONVERTER.decode(bytes);
            String rawPacket = chars.toString();
            InetSocketAddress sender = (InetSocketAddress) (realPacket
                .getSocketAddress());
            if (rawPacket.length() > 0 && aboveLayer != null)
              aboveLayer.receive(Packet.buildPacketFrom(rawPacket, sender),
                  Link.this);
          } catch (SocketException e) {
            System.err.println("-- link socket closed --");
            localSocket = null;
          } catch (IOException e) {
            throw new IllegalStateException(e);
          }
        }
      }
    };
    receiver.start();
  }

  /**
   * Sends immediately a datagram packet through the local socket.
   * 
   * @param packet
   *          the packet to be send
   */
  // This method sends immediately through the local socket
  protected void send(DatagramPacket packet) {
    if (localSocket != null)
      try {
        localSocket.send(packet);
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
  }

  /**
   * Sends a {@code String} to the specified destination address, but this data
   * may be artificially lost or delayed. For an effective delivery, the
   * specified data is wrapped inside a standard datagram packet.
   * <p/>
   * This method first picks a random number between 0 (inclusive) and
   * {@value #MAX_SUCCESS_RATE} (exclusive). If this random number is strictly
   * smaller than the current success rate, then the packet will be sent to the
   * network. Otherwise, the packet will never be sent. The success rate for
   * this {@code Link} is initially set to {@value #MAX_SUCCESS_RATE} (no lost)
   * but it may be changed by a call to {@link #setSuccessRate setSuccessRate}.
   * <p/>
   * In order to simulate wide and slow networks, this methods returns
   * immediately but the actual sending of the packet will be randomly delayed.
   * The delay in milliseconds is a positive random number picked between 0 and
   * a given maximal delay. The maximal delay for this {@code Link} is initially
   * set to {@value #DEFAULT_SEND_DELAY} milliseconds but it may be changed by a
   * call to {@link #setMaxDelay setMaxDelay}.<br/>
   * Note that, depending on the maximal delay value, the packet ordering may be
   * very affected. Only a maximal delay of 0 will ensure that all packets are
   * sent in order.
   * 
   * @param data
   *          a {@code String} to be sent
   * @param destination
   *          the specified destination address
   * 
   * @see #setSuccessRate(int) setSuccessRate
   * @see #setMaxDelay(int) setMaxDelay
   */
  protected void send(String data, SocketAddress destination) {
    if (data == null || data.length() == 0)
      return;
    if (RAND.nextInt(MAX_SUCCESS_RATE) >= successRate)
      return;
    DatagramPacket realPacket = wrap(data, destination);
    if (maxDelay <= 0)
      send(realPacket);
    else {
      final DatagramPacket packet = realPacket;
      TIMER.schedule(new TimerTask() {
        @Override
        public void run() {
          send(packet);
        }
      }, RAND.nextInt(maxDelay));
    }
  }

  /**
   * Sends a {@code Packet} to the specified destination address, but this
   * packet may be artificially lost or delayed.
   * 
   * @param packet
   *          a {@code Packet} to be sent
   * @param destination
   *          the specified destination address
   * 
   * @see #send(String,SocketAddress)
   */
  public void send(Packet packet, SocketAddress destination) {
    if (packet == null)
      return;
    send(packet.getEncoding(), destination);
  }

  /**
   * Throws an {@code UnsupportedOperationException}, as a poor implementation
   * of the {@link Layer#send send} method of the {@link common.Layer}
   * interface. Since a {@code Link} has no default destination address, this
   * method makes no sense and it must be overridden in subclasses that really
   * implement the {@code Layer} interface.
   * 
   * @throws UnsupportedOperationException
   * @see #send(Packet,SocketAddress)
   */
  public void send(Packet packet) {
    throw new UnsupportedOperationException(
        "a Link has no default destination address");
  }

  /**
   * Throws an {@code UnsupportedOperationException}, since a {@code Link} is at
   * the bottom of the layers stack and can't receive by this way.
   * 
   * @throws UnsupportedOperationException
   * @see #bind bind
   */
  public void receive(Packet packet, Layer from) {
    throw new UnsupportedOperationException(
        "a Link is at the bottom of the stack and can't receive by this way");
  }

  /**
   * Flushes the sending buffer of this {@code Link}, then the local socket is
   * closed. Closing the socket is deferred until all the packets, submitted
   * prior to this call to close, but not yet sent (artificially delayed), are
   * send at their scheduled time.
   * 
   * @see #send(String,SocketAddress)
   */
  public void close() {
    if (localSocket == null)
      return;
    System.err.println("-- flushing the link layer " + name + " ....");
    TIMER.schedule(new TimerTask() {
      @SuppressWarnings("synthetic-access")
      @Override
      public void run() {
        if (localSocket != null)
          localSocket.close();
        // localSocket = null;
        receiver.interrupt();
        TIMER.cancel();
      }
    }, 2 * MAX_SEND_DELAY); // time enough to flush the queue
  }

  /**
   * Sets the sending success rate for this {@code Link}.
   * 
   * @param rate
   *          an {@code int} value between 0 (always miss) and
   *          {@value #MAX_SUCCESS_RATE} (always succeed). When the parameter is
   *          out of range, this method does nothing.
   * @see #send(String,SocketAddress)
   */
  public void setSuccessRate(int rate) {
    if ((rate >= 0) && (rate <= MAX_SUCCESS_RATE)) {
      successRate = rate;
      System.err.println("the sending success rate for link " + this
          + " is set to " + (successRate * 100 / MAX_SUCCESS_RATE) + "%.");
    }
  }

  /**
   * Sets the maximal sending delay (in <b>milliseconds</b>) for this
   * {@code Link}.
   * 
   * @param delay
   *          an {@code int} value between 0 and {@value #MAX_SEND_DELAY}. When
   *          the parameter is out of range, this method does nothing.
   * @see #send(String,SocketAddress)
   */
  public void setMaxDelay(int delay) {
    if ((delay >= 0) && (delay <= MAX_SEND_DELAY)) {
      maxDelay = delay;
      System.err.println("the sending max delay for link " + this
          + " is set to " + maxDelay + "ms.");
    }
  }

  /**
   * Returns the symbolic name which identifies this {@code Link}.
   * 
   * @return a name for this {@code Link}
   */

  @Override
  public final String toString() {
    return name;
  }

}
