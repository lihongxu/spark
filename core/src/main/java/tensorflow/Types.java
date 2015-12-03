// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: tensorflow/core/framework/types.proto

package tensorflow;

public final class Types {
  private Types() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
  }
  /**
   * Protobuf enum {@code tensorflow.DataType}
   */
  public enum DataType
      implements com.google.protobuf.ProtocolMessageEnum {
    /**
     * <code>DT_INVALID = 0;</code>
     *
     * <pre>
     * Not a legal value for DataType.  Used to indicate a DataType field
     * has not been set.
     * </pre>
     */
    DT_INVALID(0, 0),
    /**
     * <code>DT_FLOAT = 1;</code>
     *
     * <pre>
     * Data types that all computation devices are expected to be
     * capable to support.
     * </pre>
     */
    DT_FLOAT(1, 1),
    /**
     * <code>DT_DOUBLE = 2;</code>
     */
    DT_DOUBLE(2, 2),
    /**
     * <code>DT_INT32 = 3;</code>
     */
    DT_INT32(3, 3),
    /**
     * <code>DT_UINT8 = 4;</code>
     */
    DT_UINT8(4, 4),
    /**
     * <code>DT_INT16 = 5;</code>
     */
    DT_INT16(5, 5),
    /**
     * <code>DT_INT8 = 6;</code>
     */
    DT_INT8(6, 6),
    /**
     * <code>DT_STRING = 7;</code>
     */
    DT_STRING(7, 7),
    /**
     * <code>DT_COMPLEX64 = 8;</code>
     *
     * <pre>
     * Single-precision complex
     * </pre>
     */
    DT_COMPLEX64(8, 8),
    /**
     * <code>DT_INT64 = 9;</code>
     */
    DT_INT64(9, 9),
    /**
     * <code>DT_BOOL = 10;</code>
     */
    DT_BOOL(10, 10),
    /**
     * <code>DT_QINT8 = 11;</code>
     *
     * <pre>
     * Quantized int8
     * </pre>
     */
    DT_QINT8(11, 11),
    /**
     * <code>DT_QUINT8 = 12;</code>
     *
     * <pre>
     * Quantized uint8
     * </pre>
     */
    DT_QUINT8(12, 12),
    /**
     * <code>DT_QINT32 = 13;</code>
     *
     * <pre>
     * Quantized int32
     * </pre>
     */
    DT_QINT32(13, 13),
    /**
     * <code>DT_BFLOAT16 = 14;</code>
     *
     * <pre>
     * Float32 truncated to 16 bits.  Only for cast ops.
     * </pre>
     */
    DT_BFLOAT16(14, 14),
    /**
     * <code>DT_FLOAT_REF = 101;</code>
     *
     * <pre>
     * Do not use!  These are only for parameters.  Every enum above
     * should have a corresponding value below (verified by types_test).
     * </pre>
     */
    DT_FLOAT_REF(15, 101),
    /**
     * <code>DT_DOUBLE_REF = 102;</code>
     */
    DT_DOUBLE_REF(16, 102),
    /**
     * <code>DT_INT32_REF = 103;</code>
     */
    DT_INT32_REF(17, 103),
    /**
     * <code>DT_UINT8_REF = 104;</code>
     */
    DT_UINT8_REF(18, 104),
    /**
     * <code>DT_INT16_REF = 105;</code>
     */
    DT_INT16_REF(19, 105),
    /**
     * <code>DT_INT8_REF = 106;</code>
     */
    DT_INT8_REF(20, 106),
    /**
     * <code>DT_STRING_REF = 107;</code>
     */
    DT_STRING_REF(21, 107),
    /**
     * <code>DT_COMPLEX64_REF = 108;</code>
     */
    DT_COMPLEX64_REF(22, 108),
    /**
     * <code>DT_INT64_REF = 109;</code>
     */
    DT_INT64_REF(23, 109),
    /**
     * <code>DT_BOOL_REF = 110;</code>
     */
    DT_BOOL_REF(24, 110),
    /**
     * <code>DT_QINT8_REF = 111;</code>
     */
    DT_QINT8_REF(25, 111),
    /**
     * <code>DT_QUINT8_REF = 112;</code>
     */
    DT_QUINT8_REF(26, 112),
    /**
     * <code>DT_QINT32_REF = 113;</code>
     */
    DT_QINT32_REF(27, 113),
    /**
     * <code>DT_BFLOAT16_REF = 114;</code>
     */
    DT_BFLOAT16_REF(28, 114),
    UNRECOGNIZED(-1, -1),
    ;

    /**
     * <code>DT_INVALID = 0;</code>
     *
     * <pre>
     * Not a legal value for DataType.  Used to indicate a DataType field
     * has not been set.
     * </pre>
     */
    public static final int DT_INVALID_VALUE = 0;
    /**
     * <code>DT_FLOAT = 1;</code>
     *
     * <pre>
     * Data types that all computation devices are expected to be
     * capable to support.
     * </pre>
     */
    public static final int DT_FLOAT_VALUE = 1;
    /**
     * <code>DT_DOUBLE = 2;</code>
     */
    public static final int DT_DOUBLE_VALUE = 2;
    /**
     * <code>DT_INT32 = 3;</code>
     */
    public static final int DT_INT32_VALUE = 3;
    /**
     * <code>DT_UINT8 = 4;</code>
     */
    public static final int DT_UINT8_VALUE = 4;
    /**
     * <code>DT_INT16 = 5;</code>
     */
    public static final int DT_INT16_VALUE = 5;
    /**
     * <code>DT_INT8 = 6;</code>
     */
    public static final int DT_INT8_VALUE = 6;
    /**
     * <code>DT_STRING = 7;</code>
     */
    public static final int DT_STRING_VALUE = 7;
    /**
     * <code>DT_COMPLEX64 = 8;</code>
     *
     * <pre>
     * Single-precision complex
     * </pre>
     */
    public static final int DT_COMPLEX64_VALUE = 8;
    /**
     * <code>DT_INT64 = 9;</code>
     */
    public static final int DT_INT64_VALUE = 9;
    /**
     * <code>DT_BOOL = 10;</code>
     */
    public static final int DT_BOOL_VALUE = 10;
    /**
     * <code>DT_QINT8 = 11;</code>
     *
     * <pre>
     * Quantized int8
     * </pre>
     */
    public static final int DT_QINT8_VALUE = 11;
    /**
     * <code>DT_QUINT8 = 12;</code>
     *
     * <pre>
     * Quantized uint8
     * </pre>
     */
    public static final int DT_QUINT8_VALUE = 12;
    /**
     * <code>DT_QINT32 = 13;</code>
     *
     * <pre>
     * Quantized int32
     * </pre>
     */
    public static final int DT_QINT32_VALUE = 13;
    /**
     * <code>DT_BFLOAT16 = 14;</code>
     *
     * <pre>
     * Float32 truncated to 16 bits.  Only for cast ops.
     * </pre>
     */
    public static final int DT_BFLOAT16_VALUE = 14;
    /**
     * <code>DT_FLOAT_REF = 101;</code>
     *
     * <pre>
     * Do not use!  These are only for parameters.  Every enum above
     * should have a corresponding value below (verified by types_test).
     * </pre>
     */
    public static final int DT_FLOAT_REF_VALUE = 101;
    /**
     * <code>DT_DOUBLE_REF = 102;</code>
     */
    public static final int DT_DOUBLE_REF_VALUE = 102;
    /**
     * <code>DT_INT32_REF = 103;</code>
     */
    public static final int DT_INT32_REF_VALUE = 103;
    /**
     * <code>DT_UINT8_REF = 104;</code>
     */
    public static final int DT_UINT8_REF_VALUE = 104;
    /**
     * <code>DT_INT16_REF = 105;</code>
     */
    public static final int DT_INT16_REF_VALUE = 105;
    /**
     * <code>DT_INT8_REF = 106;</code>
     */
    public static final int DT_INT8_REF_VALUE = 106;
    /**
     * <code>DT_STRING_REF = 107;</code>
     */
    public static final int DT_STRING_REF_VALUE = 107;
    /**
     * <code>DT_COMPLEX64_REF = 108;</code>
     */
    public static final int DT_COMPLEX64_REF_VALUE = 108;
    /**
     * <code>DT_INT64_REF = 109;</code>
     */
    public static final int DT_INT64_REF_VALUE = 109;
    /**
     * <code>DT_BOOL_REF = 110;</code>
     */
    public static final int DT_BOOL_REF_VALUE = 110;
    /**
     * <code>DT_QINT8_REF = 111;</code>
     */
    public static final int DT_QINT8_REF_VALUE = 111;
    /**
     * <code>DT_QUINT8_REF = 112;</code>
     */
    public static final int DT_QUINT8_REF_VALUE = 112;
    /**
     * <code>DT_QINT32_REF = 113;</code>
     */
    public static final int DT_QINT32_REF_VALUE = 113;
    /**
     * <code>DT_BFLOAT16_REF = 114;</code>
     */
    public static final int DT_BFLOAT16_REF_VALUE = 114;


    public final int getNumber() {
      if (index == -1) {
        throw new java.lang.IllegalArgumentException(
            "Can't get the number of an unknown enum value.");
      }
      return value;
    }

    public static DataType valueOf(int value) {
      switch (value) {
        case 0: return DT_INVALID;
        case 1: return DT_FLOAT;
        case 2: return DT_DOUBLE;
        case 3: return DT_INT32;
        case 4: return DT_UINT8;
        case 5: return DT_INT16;
        case 6: return DT_INT8;
        case 7: return DT_STRING;
        case 8: return DT_COMPLEX64;
        case 9: return DT_INT64;
        case 10: return DT_BOOL;
        case 11: return DT_QINT8;
        case 12: return DT_QUINT8;
        case 13: return DT_QINT32;
        case 14: return DT_BFLOAT16;
        case 101: return DT_FLOAT_REF;
        case 102: return DT_DOUBLE_REF;
        case 103: return DT_INT32_REF;
        case 104: return DT_UINT8_REF;
        case 105: return DT_INT16_REF;
        case 106: return DT_INT8_REF;
        case 107: return DT_STRING_REF;
        case 108: return DT_COMPLEX64_REF;
        case 109: return DT_INT64_REF;
        case 110: return DT_BOOL_REF;
        case 111: return DT_QINT8_REF;
        case 112: return DT_QUINT8_REF;
        case 113: return DT_QINT32_REF;
        case 114: return DT_BFLOAT16_REF;
        default: return null;
      }
    }

    public static com.google.protobuf.Internal.EnumLiteMap<DataType>
        internalGetValueMap() {
      return internalValueMap;
    }
    private static final com.google.protobuf.Internal.EnumLiteMap<
        DataType> internalValueMap =
          new com.google.protobuf.Internal.EnumLiteMap<DataType>() {
            public DataType findValueByNumber(int number) {
              return DataType.valueOf(number);
            }
          };

    public final com.google.protobuf.Descriptors.EnumValueDescriptor
        getValueDescriptor() {
      return getDescriptor().getValues().get(index);
    }
    public final com.google.protobuf.Descriptors.EnumDescriptor
        getDescriptorForType() {
      return getDescriptor();
    }
    public static final com.google.protobuf.Descriptors.EnumDescriptor
        getDescriptor() {
      return tensorflow.Types.getDescriptor().getEnumTypes().get(0);
    }

    private static final DataType[] VALUES = values();

    public static DataType valueOf(
        com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
      if (desc.getType() != getDescriptor()) {
        throw new java.lang.IllegalArgumentException(
          "EnumValueDescriptor is not for this type.");
      }
      if (desc.getIndex() == -1) {
        return UNRECOGNIZED;
      }
      return VALUES[desc.getIndex()];
    }

    private final int index;
    private final int value;

    private DataType(int index, int value) {
      this.index = index;
      this.value = value;
    }

    // @@protoc_insertion_point(enum_scope:tensorflow.DataType)
  }


  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n%tensorflow/core/framework/types.proto\022" +
      "\ntensorflow*\354\003\n\010DataType\022\016\n\nDT_INVALID\020\000" +
      "\022\014\n\010DT_FLOAT\020\001\022\r\n\tDT_DOUBLE\020\002\022\014\n\010DT_INT3" +
      "2\020\003\022\014\n\010DT_UINT8\020\004\022\014\n\010DT_INT16\020\005\022\013\n\007DT_IN" +
      "T8\020\006\022\r\n\tDT_STRING\020\007\022\020\n\014DT_COMPLEX64\020\010\022\014\n" +
      "\010DT_INT64\020\t\022\013\n\007DT_BOOL\020\n\022\014\n\010DT_QINT8\020\013\022\r" +
      "\n\tDT_QUINT8\020\014\022\r\n\tDT_QINT32\020\r\022\017\n\013DT_BFLOA" +
      "T16\020\016\022\020\n\014DT_FLOAT_REF\020e\022\021\n\rDT_DOUBLE_REF" +
      "\020f\022\020\n\014DT_INT32_REF\020g\022\020\n\014DT_UINT8_REF\020h\022\020" +
      "\n\014DT_INT16_REF\020i\022\017\n\013DT_INT8_REF\020j\022\021\n\rDT_",
      "STRING_REF\020k\022\024\n\020DT_COMPLEX64_REF\020l\022\020\n\014DT" +
      "_INT64_REF\020m\022\017\n\013DT_BOOL_REF\020n\022\020\n\014DT_QINT" +
      "8_REF\020o\022\021\n\rDT_QUINT8_REF\020p\022\021\n\rDT_QINT32_" +
      "REF\020q\022\023\n\017DT_BFLOAT16_REF\020rb\006proto3"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
        new com.google.protobuf.Descriptors.FileDescriptor.    InternalDescriptorAssigner() {
          public com.google.protobuf.ExtensionRegistry assignDescriptors(
              com.google.protobuf.Descriptors.FileDescriptor root) {
            descriptor = root;
            return null;
          }
        };
    com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        }, assigner);
  }

  // @@protoc_insertion_point(outer_class_scope)
}
