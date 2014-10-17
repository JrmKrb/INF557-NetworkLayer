package packets;

import java.util.Set;

import packets.Packet.Field;

/**
 * This enum specifies the declared types for formatted packets and implements
 * some utilities for formatting packets and checking if packets are well
 * formatted.
 * <p>
 * Any packet has a source, a destination and a type. Depending on its type, a
 * packet may also have a numeric field (a positive integer used for control)
 * and other additional fields.
 * 
 * @see Packet
 * 
 * @author Philippe Chassignet
 * @author INF557, DIX, © 2010-2012 École Polytechnique
 * @version 1.2, 2012/12/07
 */

public enum PacketType {

  /**
   * Constant to type a {@link RawPacket}. A {@code RawPacket} is used
   * internally as a wrapper to keep track of a badly formatted packet. For such
   * a packet, even the required fields, as source, destination and type may be
   * undefined.
   */
  RAW(-3) {
    @Override
    protected Packet makePacket(String source, String destination,
        String[] args, String rawPacket) {
      throw new IllegalStateException();
    }
  },

  /**
   * Constant to type a {@link LinkPacket}. A {@code LinkPacket} is used
   * internally at the link layer by a {@code SwitchedLink} to register with a
   * <i>central switching node</i>. Such a packet has no additional field.
   * 
   * @see link.SwitchedLink
   * @see link.Matrix
   */
  LINK(0) {
    @Override
    protected Packet makePacket(String source, String destination,
        String[] args, String rawPacket) {
      return new LinkPacket(source, destination);
    }
  },

  /**
   * Constant to type a {@link ConnectPacket}. A {@code ConnectPacket} is used
   * at the transport layer and it is sent by a client to a server to request
   * the opening of a connection. Such a packet has two additional fields, a
   * sequence number and a content field being the proposed window size.
   * 
   * @see #ACCEPT
   */
  CONNECT(2) {
    @Override
    protected Packet makePacket(String source, String destination,
        String[] args, String rawPacket) {
      try {
        return new ConnectPacket(source, destination, Integer.parseInt(Packet
            .getField(Field.NUMERIC, args)), Integer.parseInt(Packet.getField(
            Field.CONTENT, args)));
      } catch (NumberFormatException e) {
        return new RawPacket(rawPacket);
      }
    }
  },

  /**
   * Constant to type an {@link AcceptPacket}. An {@code AcceptPacket} is used
   * at the transport layer and it is sent by a server to a client as the
   * positive answer to a connection request. Such a packet has three additional
   * fields, a sequence number, a content field being the accepted window size
   * and additional content field giving the acknowledgment number for the
   * received {@link ConnectPacket}.
   * 
   * @see #CONNECT
   */
  ACCEPT(3) {
    @Override
    protected Packet makePacket(String source, String destination,
        String[] args, String rawPacket) {
      try {
        return new AcceptPacket(source, destination, Integer.parseInt(Packet
            .getField(Field.NUMERIC, args)), Integer.parseInt(Packet.getField(
            Field.CONTENT, args)), Integer.parseInt(Packet.getField(
            Field.SECOND_CONTENT, args)));
      } catch (NumberFormatException e) {
        return new RawPacket(rawPacket);
      }
    }
  },

  /**
   * Constant to type a {@link DataPacket}. A {@code DataPacket} is used at the
   * transport layer and it is sent to transmit a wrapped piece of data. Such a
   * packet has two additional fields, a sequence number and the piece of data
   * as the content field.
   * 
   * @see #ACK
   * @see #NACK
   */
  DATA(2) {
    @Override
    protected Packet makePacket(String source, String destination,
        String[] args, String rawPacket) {
      try {
        return new DataPacket(source, destination, Integer.parseInt(Packet
            .getField(Field.NUMERIC, args)), Packet.getField(Field.CONTENT,
            args));
      } catch (NumberFormatException e) {
        return new RawPacket(rawPacket);
      }
    }
  },

  /**
   * Constant to type an {@link AckPacket}. An {@code AckPacket} is used at the
   * transport link layer and it is sent to acknowledge one or more received
   * packets. Such a packet has a sequence number which must be consistent with
   * that of the acknowledged packets.
   * 
   * @see #DATA
   * @see #NACK
   */
  ACK(1) {
    @Override
    protected Packet makePacket(String source, String destination,
        String[] args, String rawPacket) {
      try {
        return new AckPacket(source, destination, Integer.parseInt(Packet
            .getField(Field.NUMERIC, args)));
      } catch (NumberFormatException e) {
        return new RawPacket(rawPacket);
      }
    }
  },

  /**
   * Constant to type a {@link NackPacket}. A {@code NackPacket} is used at the
   * transport link layer and it is sent to tell the other side that an expected
   * packet is missing. Such a packet has a sequence number which must be
   * consistent with that of the missing packet.
   * 
   * @see #DATA
   * @see #ACK
   */
  NACK(1) {
    @Override
    protected Packet makePacket(String source, String destination,
        String[] args, String rawPacket) {
      try {
        return new NackPacket(source, destination, Integer.parseInt(Packet
            .getField(Field.NUMERIC, args)));
      } catch (NumberFormatException e) {
        return new RawPacket(rawPacket);
      }
    }
  },

  /**
   * Constant to type a {@link ClosePacket}. A {@code ClosePacket} is used at
   * the transport link layer and it is sent to notify the other side of the end
   * of the transmission in this direction. Such a packet has a sequence number
   * which must be consistent with that of the preceding packet.
   */
  CLOSE(1) {
    @Override
    protected Packet makePacket(String source, String destination,
        String[] args, String rawPacket) {
      try {
        return new ClosePacket(source, destination, Integer.parseInt(Packet
            .getField(Field.NUMERIC, args)));
      } catch (NumberFormatException e) {
        return new RawPacket(rawPacket);
      }
    }
  },

  /**
   * Constant to type an {@link HelloPacket}. An {@code HelloPacket} is used at
   * the network layer by a node to tell its own existence and so register with
   * its neighbors. Such a packet has no additional field.
   */
  HELLO(0) {
    @Override
    protected Packet makePacket(String source, String destination,
        String[] args, String rawPacket) {
      return new HelloPacket(source, destination);
    }
  },

  /**
   * Constant to type a {@link NextHopPacket}. A {@code NextHopPacket} is used
   * at the network layer to transmit a nested packet for a single step in a
   * multi-hop network. Such a packet has a numeric field, used as the TTL, and
   * all the fields of the nested packet are pushed as a variable number of
   * additional fields.
   */
  NEXTHOP(1) {
    @Override
    protected Packet makePacket(String source, String destination,
        String[] args, String rawPacket) {
     try {
        return new NextHopPacket(source, destination, Integer.parseInt(Packet
            .getField(Field.NUMERIC, args)), Packet.buildPacketFrom(Packet
            .encode(args, Field.CONTENT.ordinal())));
      } catch (NumberFormatException e) {
        return new RawPacket(rawPacket);
      }
    }
  },

  /**
   * Constant to type a {@link VectorPacket}. A {@code VectorPacket} is used at
   * the network layer to transmit a distance vector.
   */
  VECTOR(1) {
    @Override
    protected Packet makePacket(String source, String destination,
        String[] args, String rawPacket) {
      try {
        int chunkSize = Integer.parseInt(Packet.getField(Field.NUMERIC, args));
        String[][] vector = Packet.decode(
            Packet.encode(args, Field.CONTENT.ordinal()), chunkSize);
        return new VectorPacket(source, destination, vector);
      } catch (NumberFormatException e) {
        return new RawPacket(rawPacket);
      }
    }
  },

  /**
   * Constant to type a {@link DontRelayPacket}. A {@code DontRelayPacket} is
   * used at the network layer to transmit the set of the names of nodes that
   * don't relay messages in a multicast protocol.
   */
    DONT_RELAY(0) {
    @Override
    protected Packet makePacket(String source, String destination,
        String[] args, String rawPacket) {
      Set<String> set = Packet.decodeSet(Packet.encode(args,
          Field.CONTENT.ordinal()));
      return new DontRelayPacket(source, destination, set);
    }
  },

  /**
   * Constant to type a {@link JoinPacket}. A {@code JoinPacket} is used at the
   * network layer to transmit the name of a multicast channel to which one want
   * to subscribe, toward the source of this channel. The source of the channel
   * must be given as the destination of such a packet and this packet must be
   * nested inside NEXTHOP packets to be forwarded up to the source. The name of
   * the subscribed channel is given as the content field.
   */
//  JOIN(5) {
    JOIN(2) {
    @Override
    protected Packet makePacket(String source, String destination,
        String[] args, String rawPacket) {
      return new JoinPacket(source, destination, Packet.getField(Field.CONTENT,
          args));
    }
  },
  ;

  private final int nFields;

  private PacketType(int n) {
    nFields = n;
  }

  /**
   * Returns the number of fields specified for this type of packet.
   * 
   * @return the number of fields specified for this type of packet
   */
  public int fieldsCount() {
    return nFields;
  }

  /**
   * Returns a new instance of the subclass of {@code Packet} that corresponds
   * to this type of packet. Any type constant declared in the present enum must
   * implement this method in order to check the parameter values and pass them
   * to the appropriate constructor. For convenience, the parameters have some
   * redundancy.<br/>
   * 
   * @param source
   *          value for the source field
   * @param destination
   *          value for the destination field
   * @param args
   *          the whole set of fields that must be given in the correct order.
   * @param rawPacket
   *          a raw representation of the packet, used to build a
   *          {@code RawPacket} in case of bad field values are given
   * 
   * @return a corresponding instance of {@code Packet}
   */
  abstract protected Packet makePacket(String source, String destination,
      String[] args, String rawPacket);

}
