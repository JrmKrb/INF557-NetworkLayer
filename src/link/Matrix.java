package link;

import java.io.IOException;
import java.io.PrintStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import packets.Packet;
import packets.PacketType;

import common.Layer;
import common.TopLayer;

/**
 * This program implements a <i>central switching node</i> that forwards any
 * incoming packet to a subset of satellite nodes, following a configurable
 * <i>directed graph</i> policy. The considered subset corresponds to the
 * neighborhood that is currently defined for the sender of the incoming packet.
 * This should not be considered as routing but rather as wired switching, since
 * the central switching node does not analyze the contents of the packets and
 * the forwarding rules are only based on the sender identity defined at the
 * link layer.
 * <p/>
 * 
 * Usage : <tt>java link/Matrix trafficPort controlPort</tt>
 * 
 * <p/>
 * To use this feature, a satellite node must bind with a {@link SwitchedLink}
 * configured to address the <i>traffic port</i> of the central switching node.
 * The topology of the emulated network is defined by commands arriving on the
 * <i>control port</i> of the central switching node. The {@link Control}
 * program is the tool designed to send well formatted commands to the central
 * switching node.
 * 
 * <p/>
 * The policy for the {@code Matrix} program is to consider the network as a
 * <b>directed graph</b> whose edges can be controlled one at a time.
 * 
 * The two main commands are:
 * <ul>
 * <li><tt>add A B</tt> &nbsp; to add the directed link from node A to node B</li>
 * <li><tt>del A B</tt> &nbsp; to remove the directed link from node A to node B
 * </li>
 * </ul>
 * Note that a newly registered node is not connected in the graph, not even to
 * itself, and that two <tt>add</tt> commands are required to establish a
 * bidirectional link between two distinct nodes. <br/>
 * The {@link HubsPool} program is a variant where the connection policy looks
 * like plugging each node into a connection slot of a pool of hubs.
 * 
 * @see SwitchedLink
 * @see Control
 * @see HubsPool
 * 
 * @author Philippe Chassignet
 * @author INF557, DIX, © 2010-2011 École Polytechnique
 */

public class Matrix {

  /**
   * The lock used to synchronize between command handling and packets
   * processing.
   */
  protected final Object lock = new Object();

  /**
   * The map used to retrieve the {@code SocketAddress} of a node, given its
   * name.
   */
  protected final Map<String, SocketAddress> mapNameToAddress;

  /**
   * The map used to retrieve the name of a node, given its
   * {@code SocketAddress}.
   */
  protected final Map<SocketAddress, String> mapAddressToName;

  /**
   * The map used to retrieve the connection timeout task for a node, given its
   * {@code SocketAddress}.
   */
  private final Map<SocketAddress, TimerTask> mapAddressToTimeout;

  /**
   * The map used to retrieve the set of destination nodes, given the
   * {@code SocketAddress} of a sender node.
   */
  protected final Map<SocketAddress, Set<SocketAddress>> mapAddressToDestinations;

  private static final Timer TIMER = new Timer();
  private static final int LINK_TIMEOUT = 30000;

  /**
   * Constructs a {@code Matrix} and its maps. A {@code Matrix} that has just
   * been constructed is not ready for working since it is not yet bound with
   * the two ports it must listen. One has first to invoke the {@link #start
   * start} method, to specify the two network interfaces which will be
   * operated.
   */
  protected Matrix() {
    mapNameToAddress = new ConcurrentHashMap<String, SocketAddress>();
    mapAddressToName = new ConcurrentHashMap<SocketAddress, String>();
    mapAddressToTimeout = new ConcurrentHashMap<SocketAddress, TimerTask>();
    mapAddressToDestinations = new ConcurrentHashMap<SocketAddress, Set<SocketAddress>>();
  }

  /**
   * Starts the operation of this {@code Matrix} by linking it with the two
   * specified network interfaces.
   * 
   * @param traffic
   *          the interface for receiving the packets to be switched
   * @param control
   *          the interface for receiving the control commands
   */
  protected void start(Link traffic, Link control) {
    traffic.deliverTo(new TopLayer() {
      @SuppressWarnings("synthetic-access")
      public void receive(Packet packet, Layer from) {
        handlePacket(packet, (Link) from);
      }
    });
    control.deliverTo(new TopLayer() {
      @SuppressWarnings("synthetic-access")
      public void receive(Packet packet, Layer from) {
        handleControlPacket(packet);
      }
    });
  }

  private void remove(String name, SocketAddress remoteSocket) {
    synchronized (lock) {
      mapNameToAddress.remove(name);
      mapAddressToName.remove(remoteSocket);
      TimerTask task = mapAddressToTimeout.get(remoteSocket);
      if (task != null)
        task.cancel();
      mapAddressToTimeout.remove(remoteSocket);
      for (Set<SocketAddress> links : mapAddressToDestinations.values())
        links.remove(remoteSocket);
      mapAddressToDestinations.remove(remoteSocket);
    }
  }

  /**
   * The hook called when a new node has been registered.
   * 
   * @param name
   *          the name identifying the new node
   */
  protected void handleNewNode(String name) {
    System.out.println("*** new interface : " + name);

  }

  private boolean isValidName(String name) {
    for (int i = 0; i < name.length(); ++i) {
      char c = name.charAt(i);
      if (Character.UnicodeBlock.of(c) != Character.UnicodeBlock.BASIC_LATIN)
        return false;
      switch (Character.getType(c)) {
      case Character.UPPERCASE_LETTER:
      case Character.LOWERCASE_LETTER:
      case Character.DECIMAL_DIGIT_NUMBER:
      case Character.DASH_PUNCTUATION: // -
      case Character.CONNECTOR_PUNCTUATION: // _
        break;
      case Character.START_PUNCTUATION: // ( [ {
      case Character.END_PUNCTUATION: // ) ] }
      case Character.OTHER_PUNCTUATION: // ! " # % & ' * , . / : ; ? @ \
      case Character.MATH_SYMBOL:// + < = > | ~
      case Character.CURRENCY_SYMBOL: // $
      case Character.MODIFIER_SYMBOL: // ^ `
      default:
        return false;
      }
    }
    return true;
  }

  private void handleLINK(final String name, final SocketAddress remoteSocket) {
    SocketAddress oldSocket = mapNameToAddress.get(name);
    boolean newLink = false;
    if (oldSocket == null)
      newLink = true;
    else if (!oldSocket.equals(remoteSocket)) {
      System.out.println("*** resume interface : " + name);
      for (Set<SocketAddress> links : mapAddressToDestinations.values())
        if (links.contains(oldSocket))
          links.add(remoteSocket);
      Set<SocketAddress> links = mapAddressToDestinations.get(oldSocket);
      if (links != null)
        mapAddressToDestinations.put(remoteSocket, links);
      remove(name, oldSocket);
    }
    mapNameToAddress.put(name, remoteSocket);
    mapAddressToName.put(remoteSocket, name);
    TimerTask task = mapAddressToTimeout.get(remoteSocket);
    if (task != null)
      task.cancel();
    task = new TimerTask() {
      @SuppressWarnings("synthetic-access")
      @Override
      public void run() {
        System.out.println("*** timeout for " + name + " " + remoteSocket);
        remove(name, remoteSocket);
      }
    };
    mapAddressToTimeout.put(remoteSocket, task);
    TIMER.schedule(task, LINK_TIMEOUT);
    if (newLink)
      handleNewNode(name);
  }

  private void handlePacket(Packet packet, Link link) {
    if (packet == null)
      return;
    SocketAddress senderSocket = packet.getSenderAddress();
    synchronized (lock) {
      switch (packet.getType()) {
      case LINK:
        if (SwitchedLink.LINK_DESTINATION.equals(packet.getDestination()))
          if (isValidName(packet.getSource()))
            handleLINK(packet.getSource(), senderSocket);
        break;
      default:
        Set<SocketAddress> neighbors = mapAddressToDestinations
            .get(senderSocket);
        if (neighbors != null)
          for (SocketAddress destination : neighbors)
            link.send(packet, destination);
        break;
      }
    }
  }

  private void add(SocketAddress sourceSocket, SocketAddress destinationSocket) {
    Set<SocketAddress> neighbors = mapAddressToDestinations.get(sourceSocket);
    if (neighbors == null) {
      neighbors = new HashSet<SocketAddress>();
      mapAddressToDestinations.put(sourceSocket, neighbors);
    }
    neighbors.add(destinationSocket);
  }

  private void del(SocketAddress sourceSocket, SocketAddress destinationSocket) {
    Set<SocketAddress> neighbors = mapAddressToDestinations.get(sourceSocket);
    if (neighbors != null)
      neighbors.remove(destinationSocket);
  }

  /**
   * Displays a message as the result of a control command. It sends the message
   * back to the control program, through the given socket's stream and also
   * displays the message on the local console.
   * 
   * @param message
   *          the {@code String} to be displayed
   * @param results
   *          the socket's stream to send back to the control program
   */
  protected static void displayResult(String message, PrintStream results) {
    if (results != null)
      results.println(message);
    System.out.println(message);
  }

  /**
   * Fetches the {@code SocketAddress} of a node, given its name. When the
   * specified name is not yet registered, a message is sent back to the control
   * program.
   * 
   * @param name
   *          the name identifying the requested node
   * @param results
   *          the socket's stream to send back to the control program
   * @return the {@code SocketAddress} of the specified node, or {@code null} if
   *         not registered
   */
  protected SocketAddress nameToAddress(String name, PrintStream results) {
    SocketAddress nodeSocket = mapNameToAddress.get(name);
    if (nodeSocket == null)
      displayResult("*** unknown interface " + name, results);
    return nodeSocket;
  }

  /**
   * The hook called to process an <tt>add</tt> command. The two parameters for
   * this command are given as the <i>source</i> and the <i>destination</i> of a
   * packet-like structure. A message is sent back to the control program.
   * 
   * @param packet
   *          the received parameters for <tt>add</tt>, formatted as a
   *          {@code Packet}
   * @param results
   *          the socket's stream to send back to the control program
   */
  protected void handleAdd(Packet packet, PrintStream results) {
    synchronized (lock) {
      String source = packet.getSource();
      SocketAddress sourceSocket = nameToAddress(source, results);
      String destination = packet.getDestination();
      SocketAddress destinationSocket = nameToAddress(destination, results);
      if (sourceSocket == null || destinationSocket == null)
        return;
      add(sourceSocket, destinationSocket);
      displayResult("OK node " + destination + " is now reachable from node "
          + source, results);
    }
  }

  /**
   * The hook called to process a <tt>del</tt> command. The two parameters for
   * this command are given as the <i>source</i> and the <i>destination</i> of a
   * packet-like structure. A message is sent back to the control program.
   * 
   * @param packet
   *          the received parameters for <tt>del</tt>, formatted as a
   *          {@code Packet}
   * @param results
   *          the socket's stream to send back to the control program
   */
  protected void handleDel(Packet packet, PrintStream results) {
    synchronized (lock) {
      String source = packet.getSource();
      SocketAddress sourceSocket = nameToAddress(source, results);
      String destination = packet.getDestination();
      SocketAddress destinationSocket = nameToAddress(destination, results);
      if (sourceSocket == null || destinationSocket == null)
        return;
      del(sourceSocket, destinationSocket);
      displayResult("done", results);
      displayResult("OK node " + destination
          + " is no longer reachable from node " + source, results);
    }
  }

  private void listNodes(PrintStream stream) {
    stream.println("========== NODES ==========");
    for (String name : mapAddressToName.values())
      stream.print(' ' + name);
    stream.println();
    stream.println("===========================");
  }

  /**
   * The hook called to send the help line to the control program, in response
   * to the <tt>help_add</tt> command.
   * 
   * @param results
   *          the socket's stream to send back to the control program
   */
  protected void helpAdd(PrintStream results) {
    results.println("\tadd A B : add the directed link from node A to node B");
  }

  /**
   * The hook called to send the help line to the control program, in response
   * to the <tt>help_del</tt> command.
   * 
   * @param results
   *          the socket's stream to send back to the control program
   */
  protected void helpDel(PrintStream results) {
    results
        .println("\tdel A B : remove the directed link from node A to node B");
  }

  /**
   * The hook called to send to the control program, a list of the established
   * links, in response to the <tt>links</tt> command.
   * 
   * @param results
   *          the socket's stream to send back to the control program
   */
  protected void listLinks(PrintStream results) {
    results.println("========== LINKS ==========");
    for (Map.Entry<SocketAddress, Set<SocketAddress>> entry : mapAddressToDestinations
        .entrySet()) {
      results.print(mapAddressToName.get(entry.getKey()) + " ->");
      for (SocketAddress addr : entry.getValue())
        results.print(' ' + mapAddressToName.get(addr));
      results.println();
    }
    results.println("===========================");
  }

  private final void handleControlPacket(Packet packet) {
    if (packet == null)
      return;
    InetAddress senderAddress = packet.getSenderAddress().getAddress();
    System.out.println("CONTROL: " + packet + " from " + senderAddress);
    PacketType type = packet.getType();
    if (type != PacketType.RAW)
      return;
    int resultPort = packet.getNum();
    if (resultPort < 1024)
      return;
    PrintStream results = null;
    Socket resultSocket = null;
    try {
      resultSocket = new Socket();
      resultSocket.connect(new InetSocketAddress(senderAddress, resultPort),
          2000);
      results = new PrintStream(resultSocket.getOutputStream());
    } catch (IOException e) {
      System.out.println(e);
    }
    if (results == null) {
      System.out.println("*** got no feedback stream for " + senderAddress
          + ':' + resultPort);
      return;
    }

    String typ = packet.getRawType();
    synchronized (lock) {
      if ("add".equals(typ)) {
        handleAdd(packet.getNestedPacket(), results);
      } else if ("del".equals(typ)) {
        handleDel(packet.getNestedPacket(), results);
      } else if ("nodes".equals(typ)) {
        listNodes(results);
        listNodes(System.out);
      } else if ("links".equals(typ)) {
        listLinks(results);
        listLinks(System.out);
      } else if ("help_add".equals(typ)) {
        helpAdd(results);
      } else if ("help_del".equals(typ)) {
        helpDel(results);
      } else
        System.out.println("*** unknown command : " + packet + " from "
            + senderAddress);
    }
    try {
      if (resultSocket != null)
        resultSocket.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Constructs both a {@code Link} and a {@code DatagramSocket}, bound
   * together, and starts them operating.
   * 
   * @param name
   *          the {@code String} used to build a symbolic name that identifies
   *          the {@code Link}
   * @param port
   *          a value to be used as the local port number for binding the
   *          {@code DatagramSocket}
   * @return the constructed {@code Link} which hides the {@code DatagramSocket}
   */
  protected static Link startInterface(String name, int port) {
    DatagramSocket socket = null;
    try {
      socket = new DatagramSocket(port);
    } catch (SocketException e) {
      System.err.println(name + " " + port + " : " + e);
      System.exit(0);
    }
    Link link = new Link(name + port);
    link.setSuccessRate(100);
    link.setMaxDelay(0);
    link.bind(socket);
    return link;
  }

  /** See the class documentation above. */
  public static void main(String[] args) {
    if (args.length < 2) {
      System.err.println("syntax : java tools/Matrix linkPort controlPort");
      return;
    }
    int linkPort = Integer.parseInt(args[0]);
    Link link = startInterface("LINK@", linkPort);
    int controlPort = Integer.parseInt(args[1]);
    Link control = startInterface("CONTROL@", controlPort);
    new Matrix().start(link, control);
  }

}
