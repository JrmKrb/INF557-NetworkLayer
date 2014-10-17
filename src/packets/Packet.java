package packets;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

/**
 * This abstract class implements the core definition of any formatted packet as
 * well as utilities for formatting packets, checking and extracting the fields
 * of a formatted packet.
 * <p/>
 * Any packet has a source, a destination and a type. A packet may also have
 * additional fields, depending on its type, see {@link PacketType}.
 * <p/>
 * A particular additional field, useful in many types of packets, is the
 * <i>numeric field</i> with a limited range. Normal values are restricted
 * between 0 and {@link #MAX_NUMERIC_FIELD}{@code -1}. The special value -1 can
 * also be used as an indicator.
 * <p/>
 * A packet also keeps track of the socket address of its sender. This address
 * is required to implement low level features at the link layer but it should
 * not be used at above levels.
 * 
 * @author Philippe Chassignet
 * @author INF557, DIX, © 2010-2012 École Polytechnique
 * @version 1.2, 2012/12/07
 */

public abstract class Packet {

  protected static final String DELIMITER = "\u00A6";
  protected final String[] fields;
  private final String delimitedContent;
  private final String encoding;
  private InetSocketAddress sender;

  /**
   * The upper value for the numeric field, set at {@value} (exclusive). Thus,
   * normal values are in the range {@code 0 .. MAX_NUMERIC_FIELD-1}.
   * 
   * @see #truncatedNumeric truncatedNumeric
   */
  public static final int MAX_NUMERIC_FIELD = 64;

  /**
   * Returns a normalized value for the numeric field. Strictly negatives values
   * will be turned to {@code -1}. Positive values will be truncated by modular
   * arithmetic.
   * 
   * @see #MAX_NUMERIC_FIELD
   */
  public static int truncatedNumeric(int value) {
    if (value < 0)
      return -1;
    return value % MAX_NUMERIC_FIELD;
  }

  /**
   * This enum type specifies the position (and order) of the different fields
   * in a formatted packet. </br> The order is defined as
   * {@code DESTINATION, SOURCE, TYPE, NUMERIC, CONTENT}. The {@code CONTENT}
   * field is actually the first of many content fields, as seen in some complex
   * cases.
   */
  static enum Field {
    DESTINATION, SOURCE, TYPE, NUMERIC, CONTENT, SECOND_CONTENT
  }

  /**
   * Sets the value of the indexed component in the given array.
   * 
   * @param value
   *          the {@code String} value that will be assigned to the specified
   *          component
   * @param index
   *          the index in the given array
   * @param array
   *          the array whose an indexed component will be set
   * @throws ArrayIndexOutOfBoundsException
   *           if the given index is out of range
   * @throws IllegalArgumentException
   *           if the indexed component is already set to a non {@code null}
   *           value
   * @see #setField(String, Field, String[])
   */
  private static void setField(String value, int index, String[] array) {
    if (index < 0 || index >= array.length)
      return;
    if (array[index] != null)
      throw new IllegalArgumentException("The field at index " + index
          + " is already set to " + array[index]);
    array[index] = value;
  }

  /**
   * Sets the value of the specified component in the given array. The component
   * is specified by its symbolic name, that is a value of the {@link Field}
   * enum.
   * 
   * @param value
   *          the {@code String} value that will be assigned to the specified
   *          component
   * @param field
   *          the enum constant that specifies the component in the given array
   * @param array
   *          the array whose component will be set
   * @throws ArrayIndexOutOfBoundsException
   *           if the index specified by the symbolic name is out of range
   * @throws IllegalArgumentException
   *           if the indexed component was already set to a non {@code null}
   *           value
   * @see #setField(String, int, String[])
   * @see Field
   */
  private static void setField(String value, Field field, String[] array) {
    setField(value, field.ordinal(), array);
  }

  /**
   * Packs the given {@code String} values, that are considered to be fields of
   * a packet, in a well-sized array of {@code String}. The values for the
   * {@code source}, {@code destination}, {@code type} and {@code numeric}
   * arguments are assigned to their respective position as specified by the
   * {@link Field} enum. The additional fields are assigned in order, starting
   * in the array from the position specified by the {@link Field#CONTENT}
   * constant.
   * 
   * @param source
   *          the source of the packet
   * @param destination
   *          the destination of the packet
   * @param type
   *          the canonical encoding of the type of the packet
   * @param numeric
   *          the encoding of the numeric field of the packet
   * @param extraFields
   *          a varying number of additional fields
   * @return an new array, well-sized and filled with the specified values
   * @see Field
   */
  public static String[] pack(String source, String destination, String type,
      String numeric, String... extraFields) {
    String[] array = new String[4 + extraFields.length];
    setField(source, Field.SOURCE, array);
    setField(destination, Field.DESTINATION, array);
    setField(type, Field.TYPE, array);
    setField(numeric, Field.NUMERIC, array);
    int i = Field.CONTENT.ordinal();
    for (String s : extraFields)
      setField(s, i++, array);
    return array;
  }

  /**
   * Formats, as an external {@code Packet}-like representation, all the fields
   * given in an array of {@code String}, but starting at the specified index.
   * Used in conjunction with {@link #pack pack}, this method allows to forge
   * any packet-like {@code String}.
   * 
   * @param array
   *          the array giving the values of the fields
   * @param from
   *          the index of the first formatted value
   * @return the external representation of the specified part of the given
   *         values
   * @see #pack pack
   * @see #decode decode
   */
  public static String encode(String[] array, int from) {
    StringBuilder s = new StringBuilder();
    if (array.length > from) {
      if (array[from] != null)
        s.append(array[from]);
      for (int i = from + 1; i < array.length; ++i) {
        s.append(DELIMITER);
        if (array[i] != null)
          s.append(array[i]);
      }
    }
    return s.toString();
  }

  /**
   * Returns an array containing all the fields of a raw packet given as a
   * {@code String}. This method works regardless the expected number of fields
   * for a given type of packet.
   * 
   * @param rawPacket
   *          the {@code String} to be analyzed as a packet
   * @return {@code null} if the argument is {@code null}; otherwise, an array
   *         containing all the fields found by splitting the given
   *         {@code String}
   * @see #encode(String[],int) encode
   */
  protected static String[] decode(String rawPacket) {
    if (rawPacket == null)
      return null;
    return rawPacket.split(DELIMITER, -1);
  }

  /**
   * Returns a set containing all the fields of a raw packet given as a
   * {@code String}. This method works regardless the expected number of fields
   * for a given type of packet.
   * 
   * @param rawPacket
   *          the {@code String} to be analyzed as a packet
   * @return {@code null} if the argument is {@code null}; otherwise, a set
   *         containing all the fields found by splitting the given
   *         {@code String}
   * @see #decode(String) decode
   */
  protected static Set<String> decodeSet(String rawPacket) {
    if (rawPacket == null)
      return null;
    Set<String> set = new HashSet<String>();
    for (String s : decode(rawPacket))
      set.add(s);
    return set;
  }

  /**
   * Returns the maximal length of sub-arrays found in the given two-dimensional
   * array.
   * 
   * @param array
   *          the two-dimensional array to be analyzed
   * @return the maximal length of sub-arrays
   */
  public static int chunkSize(String[][] array) {
    int size = 0;
    for (String[] tuple : array)
      if (tuple != null && size < tuple.length)
        size = tuple.length;
    return size;
  }

  /**
   * Formats all the elements of a two-dimensional array of {@code String} as an
   * external {@code Packet}-like representation. All the elements of the given
   * sub-arrays are appended in order, one sub-array after the other.
   * 
   * @param array
   *          the two-dimensional array to be formatted
   * @return the external representation of the specified array
   * @see #decode(String,int) encode
   */
  public static String encode(String[][] array) {
    int size = chunkSize(array);
    StringBuilder s = new StringBuilder();
    for (String[] tuple : array) {
      int chunkSize = 0;
      if (tuple != null) {
        chunkSize = tuple.length;
        for (int i = 0; i < chunkSize; ++i) {
          if (tuple[i] != null)
            s.append(tuple[i]);
          s.append(DELIMITER);
        }
      }
      for (int i = chunkSize + 1; i < size; ++i)
        s.append(DELIMITER);
    }
    s.delete(s.length() - 1, s.length());
    return s.toString();
  }

  /**
   * Returns an two-dimensional array containing all the fields of a
   * {@code Packet}-like representation given as a {@code String}. The sizes of
   * the sub-arrays are equal and are specified as a parameter. The number of
   * sub-arrays is then defined by the number of fields.
   * 
   * @param formattedArray
   *          the {@code String} representation to be decoded as a
   *          two-dimensional array
   * @param chunkSize
   *          the specified size for each sub-array
   * @return a structured two-dimensional array containing all the fields found
   *         by splitting the given {@code String}
   * @throws IllegalArgumentException
   *           if the {@code chunkSize} parameter is of negative or zero length
   * @see #encode(String[][]) encode
   */
  protected static String[][] decode(String formattedArray, int chunkSize) {
    if (chunkSize <= 0)
      throw new IllegalArgumentException("invalid size " + chunkSize
          + " for a sub-array");
    String[] t = formattedArray.split(DELIMITER, -1);
    String[][] array = new String[(t.length + chunkSize - 1) / chunkSize][chunkSize];
    int k = 0;
    for (int i = 0; i < array.length; ++i)
      for (int j = 0; j < chunkSize && k < t.length; ++j, ++k)
        array[i][j] = t[k];
    return array;
  }

  /**
   * Returns a new {@code Packet} build from the external representation given
   * as a {@code String}. This method checks if the argument is well-formatted
   * or not. Its use should be reserved for the reconstruction of packets
   * transmitted over the network.
   * 
   * @param rawPacket
   *          the {@code String} to be analyzed as a packet
   * @return a well-formed {@code Packet} corresponding to the external
   *         representation, if applicable; a {@code RawPacket} otherwise
   * @see #encode encode
   * @see #decode decode
   * @see #buildPacketFrom(String,InetSocketAddress)
   */
  protected static Packet buildPacketFrom(String rawPacket) {
    String[] rawFields = Packet.decode(rawPacket);
    if (rawFields == null || rawFields.length < 3)
      return new RawPacket(rawPacket);
    PacketType type = typeOf(getField(Field.TYPE, rawFields));
    if (type == PacketType.RAW)
      return new RawPacket(rawPacket);
    if (rawFields.length < type.fieldsCount())
      return new RawPacket(rawPacket);
    return type.makePacket(getField(Field.SOURCE, rawFields),
        getField(Field.DESTINATION, rawFields), rawFields, rawPacket);
  }

  /**
   * Returns a new {@code Packet} build from the external representation given
   * as a {@code String}, and which also stores the {@code SocketAddress} of its
   * sender. This method checks if the argument is well-formatted or not. Its
   * use should be reserved for the reconstruction of packets transmitted over
   * the network.
   * 
   * @param rawPacket
   *          the {@code String} to be analyzed as a packet
   * @param sender
   *          the {@code SocketAddress} of the sender program
   * @return a well-formed {@code Packet} corresponding to the external
   *         representation, if applicable; {@code null} if the given
   *         {@code String} is {@code null}; a {@code RawPacket} otherwise
   * @see #encode encode
   * @see #decode decode
   * @see #buildPacketFrom(String)
   */
  public static Packet buildPacketFrom(String rawPacket,
      InetSocketAddress sender) {
    if (rawPacket == null)
      return null;
    Packet p = buildPacketFrom(rawPacket);
    p.sender = sender;
    return p;
  }

  /**
   * Returns the value stored at the specified index in the given array.
   * 
   * @param index
   *          the index in the given array
   * @param array
   *          the array whose an indexed component will be retrieved
   * @return the retrieved value, if well-specified; {@code null} otherwise.
   * @see #getField(Field,String[])
   */
  private static String getField(int index, String[] array) {
    if (array == null || index < 0 || index >= array.length)
      return null;
    return array[index];
  }

  /**
   * Returns the value of the specified component in the given array. The
   * component is specified by its symbolic name, that is a value of the
   * {@link Field} enum.
   * 
   * @param field
   *          the enum constant that specifies the component in the given array
   * @param array
   *          the array whose an indexed component will be retrieved
   * @return the retrieved value, if well-specified; {@code null} otherwise.
   */
  protected static String getField(Field field, String[] array) {
    return getField(field.ordinal(), array);
  }

  /**
   * Returns an effective type corresponding to the given name. It is a wrapper
   * around the {@link PacketType#valueOf PacketType.valueOf} method that
   * catches the {@code IllegalArgumentException} that is thrown when the given
   * name does not correspond to any defined type.
   * 
   * @return the type field of this packet, if it is defined;
   *         {@code PacketType.RAW} otherwise
   */
  private static PacketType typeOf(String name) {
    try {
      return PacketType.valueOf(name);
    } catch (IllegalArgumentException e) {
      return PacketType.RAW;
    }
  }

  /**
   * The core constructor.
   * 
   * @param source
   *          the source of the packet
   * @param destination
   *          the destination of the packet
   * @param type
   *          the type of the packet
   * @param numeric
   *          the encoding of the numeric field of the packet
   * @param extraFields
   *          a varying number of additional fields
   */
  private Packet(String source, String destination, PacketType type,
      String numeric, String... extraFields) {
    fields = pack(source, destination, type.name(), numeric, extraFields);
    encoding = encode(fields, 0);
    delimitedContent = encode(fields, Field.CONTENT.ordinal());
  }

  /**
   * Constructs a {@code Packet} having a numeric field. It can not be used
   * directly but it is called by the constructors of subclasses. The given
   * value for the numeric field will be mapped in the allowed range.
   * 
   * @param source
   *          the source of the packet
   * @param destination
   *          the destination of the packet
   * @param type
   *          the type of the packet
   * @param numeric
   *          the numeric field of the packet
   * @param extraFields
   *          a varying number of additional fields
   * @see #truncatedNumeric truncatedNumeric
   */
  protected Packet(String source, String destination, PacketType type,
      int numeric, String... extraFields) {
    this(source, destination, type, "" + truncatedNumeric(numeric), extraFields);
  }

  /**
   * Constructs a {@code Packet} having no numeric field. It can not be used
   * directly but it is called by the constructors of subclasses.
   * 
   * @param source
   *          the source of the packet
   * @param destination
   *          the destination of the packet
   * @param type
   *          the type of the packet
   * @param extraFields
   *          a varying number of additional fields
   */
  protected Packet(String source, String destination, PacketType type,
      String... extraFields) {
    this(source, destination, type, null, extraFields);
  }

  /**
   * Returns the value of the specified component of this packet. The component
   * is specified by its symbolic name, that is a value of the {@link Field}
   * enum.
   * 
   * @param field
   *          the enum constant that specifies the component
   * @return the retrieved value, if well-specified; {@code null} otherwise.
   * @see #getField(Field,String[])
   */
  protected String getField(Field field) {
    return getField(field.ordinal(), fields);
  }

  /**
   * Returns the source field of this packet.
   * 
   * @return the source field of this packet, if it is defined; {@code null}
   *         otherwise
   */
  public String getSource() {
    return getField(Field.SOURCE);
  }

  /**
   * Returns the destination field of this packet.
   * 
   * @return the destination field of this packet, if it is defined;
   *         {@code null} otherwise
   */
  public String getDestination() {
    return getField(Field.DESTINATION);
  }

  /**
   * Returns the type field of this packet.
   * 
   * @return the type field of this packet, if it is defined; {@code null}
   *         otherwise
   */
  public PacketType getType() {
    return typeOf(getField(Field.TYPE));
  }

  /**
   * Returns, as a {@code String}, the type field of this packet.
   * 
   * @return the type field of this packet, if it is defined; {@code null}
   *         otherwise
   */
  public String getRawType() {
    PacketType type = getType();
    if (type == null)
      return null;
    return type.name();
  }

  /**
   * Returns the numeric field of this packet.
   * 
   * @return the numeric field of this packet, if it is defined;
   * @throws IllegalArgumentException
   *           if this packet has no numeric field
   */
  public int getNum() {
    String s = getField(Field.NUMERIC);
    if (s == null)
      throw new IllegalArgumentException("message of type " + getType()
          + " has no numeric field");
    return Integer.parseInt(s);
  }

  /**
   * Returns the (first) content field of this packet.
   * 
   * @return the content field of this packet, if it is defined;
   * @throws IllegalArgumentException
   *           if this packet has no content field
   */
  public String getContent() {
    String s = getField(Field.CONTENT);
    if (s == null)
      throw new IllegalArgumentException("message of type " + getType()
          + " has no content field");
    return s;
  }

  /**
   * Returns the (first) content field of this packet, parsed as an
   * <code>int</code> value.
   * 
   * @return the content field of this packet, if it is defined;
   * @throws IllegalArgumentException
   *           if this packet has no content field
   * @throws NumberFormatException
   *           if the content field does not contain a parsable <code>int</code>
   */
  public int getContentAsInteger() {
    return Integer.parseInt(getContent());
  }

  /**
   * Returns the indexed content field of this packet.
   * 
   * @param index
   *          the index for the specified field, starting at 0 for the first
   *          content field
   * @return the specified content field of this packet, if it is defined;
   * @throws IllegalArgumentException
   *           if this packet has no such content field
   */
  public String getContent(int index) {
    String s = getField(Field.CONTENT.ordinal() + index, fields);
    if (s == null)
      throw new IllegalArgumentException("message of type " + getType()
          + " has no content field of index " + index);
    return s;
  }

  /**
   * Returns the indexed content field of this packet.
   * 
   * @param index
   *          the index for the specified field, starting at 0 for the first
   *          content field
   * @return the specified content field of this packet, if it is defined;
   * @throws IllegalArgumentException
   *           if this packet has no such content field
   * @throws NumberFormatException
   *           if the content field does not contain a parsable <code>int</code>
   */
  public int getContentAsInteger(int index) {
    return Integer.parseInt(getContent(index));
  }

  /**
   * Returns the content fields of this packet, interpreted as constituting a
   * new {@code Packet}. All the additional fields, starting from the first
   * content field are regarded in order as those of a nested packet.
   * 
   * @return a well-formed packet corresponding to the content fields of this
   *         packet, if applicable; a {@code RawPacket} otherwise
   */
  public Packet getNestedPacket() {
    return buildPacketFrom(delimitedContent);
  }

  /**
   * Returns a two-dimensional array of {@code String} elements obtained from
   * the content fields of this packet. The numeric field of this packet gives
   * the size of the sub-arrays. The number of sub-arrays is then defined by the
   * number of fields. All the elements are found in order, one sub-array after
   * the other.
   * 
   * @return a structured two-dimensional array containing all the content
   *         fields found in this packet
   */
  public String[][] getArray() {
    int chunkSize = getNum();
    String[][] vector = Packet.decode(delimitedContent, chunkSize);
    return vector;
  }

  /**
   * Returns a set of {@code String} elements obtained from the content fields
   * of this packet.
   * 
   * @return a set containing all the content fields found in this packet
   */
  public Set<String> getSet() {
    return decodeSet(delimitedContent);
  }

  /**
   * Returns the {@code SocketAddress} of the program that sends this
   * {@code Packet}, if applicable. This method is required to implement low
   * level features at the link layer but it should not be used at above levels.
   * For a {@code Packet} build by using directly one subclass constructor, this
   * method returns {@code null}.
   * 
   * @return a {@code SocketAddress}, if applicable; {@code null} otherwise
   * @see #buildPacketFrom(String,InetSocketAddress)
   */
  public InetSocketAddress getSenderAddress() {
    return sender;
  }

  /**
   * Returns the formatted external representation of this {@code Packet}.
   * 
   * @return the formatted external representation of this packet
   */
  public String getEncoding() {
    return encoding;
  }

  /**
   * Returns a external representation of this {@code Packet}.
   * 
   * @return an external representation of this packet
   */
  @Override
  public String toString() {
    return getEncoding();
  }

}
