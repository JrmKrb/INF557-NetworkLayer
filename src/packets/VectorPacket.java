package packets;

/**
 * A {@code VectorPacket} can be used in various ways. It transmits a
 * two-dimensional array of {@code String} elements. A VECTOR packet is used
 * especially at the network layer to transmit a distance vector. <br/>
 * The numeric field of the packet gives the size of the sub-arrays. All the
 * elements of the sub-arrays are appended in order, one sub-array after the
 * other, as a variable number of additional fields.
 * 
 * @author Philippe Chassignet
 * @author INF557, DIX, © 2010-2011 École Polytechnique
 * @version 1.2, 2011/11/03
 */

public class VectorPacket extends Packet {
  /**
   * Constructs a {@code VectorPacket}. The size of the sub-arrays is handled as
   * the numeric field and will be retrieved through the inherited
   * {@link #getNum() getNum} method. The transmitted two-dimensional array will
   * be retrieved through the inherited {@link #getArray getArray} method.
   * 
   * @param source
   *          the source of the packet
   * @param destination
   *          the destination of the packet
   * @param vector
   *          the two-dimensional array to be embarked as the payload
   */
  public VectorPacket(String source, String destination, String[][] vector) {
    super(source, destination, PacketType.VECTOR, chunkSize(vector),
        encode(vector));
  }
}
