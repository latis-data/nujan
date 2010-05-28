
package hdfnet;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;


// Msg 05: fill value

class MsgFillValue extends MsgBase {


int dtype;                      // one of HdfGroup.DTYPE*
boolean isFillExtant;           // if extant but fillValue==null: default
Object fillValue;
int elementLen;


final int fillVersion = 2;

// Meanings for allocTime:
//   0  not used
//   1  early: alloc space when dataset is created
//   2  late: alloc space when dataset is written
//   3  incremental: alloc space chunk by chunk
final int fillAllocTime = 2;

// Meanings for writeTime:
//   0  on alloc: write fill value when space is allocated
//   1  never
//   2  if user has a non-default fill value, write on alloc.
final int fillWriteTime = 2;

// defined == 1 iff a fill value is defined for this dataset
//   For version 0, 1: we still have fillSize, fillValue fields
//   For version 2: fillSize, fillValue exist only if defined == 1.
int fillDefined;

byte[] fillBytes;


// If isFillExtant but fillValue==null,
// we output fillDefined=1, fillLen=0,
// which h5dump interprets as "fill value defined: default".


MsgFillValue(
  int dtype,                      // one of HdfGroup.DTYPE*
  boolean isFillExtant,           // if extant but fillValue==null: default
  Object fillValue,               // null, Byte, Short, Int, Long,
                                  // Float, Double, String, etc.
  int elementLen,                 // element length of the fillValue
  HdfGroup hdfGroup,              // the owning group
  HdfFile hdfFile)
throws HdfException
{
  super( TP_FILL_VALUE, hdfGroup, hdfFile);
  this.dtype = dtype;
  this.isFillExtant = isFillExtant;
  this.fillValue = fillValue;
  this.elementLen = elementLen;

  if (! isFillExtant) {
    if (fillValue != null) throwerr("not extant but fillValue != null");
    if (elementLen != 0) throwerr("fillValue == null but elementLen != 0");
    fillDefined = 0;
    fillBytes = null;
  }

  else if (fillValue == null) {
    if (elementLen != 0) throwerr("fillValue == null but elementLen != 0");
    fillDefined = 1;
    fillBytes = null;
  }

  else {
    fillDefined = 1;
    fillBytes = new byte[elementLen];
    ByteBuffer tbuf = ByteBuffer.wrap( fillBytes);
    tbuf.order( ByteOrder.LITTLE_ENDIAN);

    if (dtype == HdfGroup.DTYPE_FIXED08) {
      if (! (fillValue instanceof Byte))
        throwerr("fill type mismatch.  Expected: Byte.  Found: "
          + fillValue.getClass());
      tbuf.put( ((Byte) fillValue).byteValue());
    }
    else if (dtype == HdfGroup.DTYPE_FIXED16) {
      if (! (fillValue instanceof Short))
        throwerr("fill type mismatch.  Expected: Short.  Found: "
          + fillValue.getClass());
      tbuf.putShort( ((Short) fillValue).shortValue());
    }
    else if (dtype == HdfGroup.DTYPE_FIXED32) {
      if (! (fillValue instanceof Integer))
        throwerr("fill type mismatch.  Expected: Integer.  Found: "
          + fillValue.getClass());
      tbuf.putInt( ((Integer) fillValue).intValue());
    }
    else if (dtype == HdfGroup.DTYPE_FIXED64) {
      if (! (fillValue instanceof Long))
        throwerr("fill type mismatch.  Expected: Long.  Found: "
          + fillValue.getClass());
      tbuf.putLong( ((Long) fillValue).longValue());
    }
    else if (dtype == HdfGroup.DTYPE_FLOAT32) {
      if (! (fillValue instanceof Float))
        throwerr("fill type mismatch.  Expected: Float.  Found: "
          + fillValue.getClass());
      tbuf.putFloat( ((Float) fillValue).floatValue());
    }
    else if (dtype == HdfGroup.DTYPE_FLOAT64) {
      if (! (fillValue instanceof Double))
        throwerr("fill type mismatch.  Expected: Double.  Found: "
          + fillValue.getClass());
      tbuf.putDouble( ((Double) fillValue).doubleValue());
    }
    else if (dtype == HdfGroup.DTYPE_STRING_FIX) {
      if (! (fillValue instanceof String))
        throwerr("fill type mismatch.  Expected: String.  Found: "
          + fillValue.getClass());
      byte[] bytes = Util.encodeString( (String) fillValue, true, hdfGroup);
      tbuf.put( Util.padNull( bytes, elementLen));
    }
    else if (dtype == HdfGroup.DTYPE_STRING_VAR) {
      // We will fill the fillBytes later, in formatMsgCore below,
      // since it depends on the gcol addr.
      // Element is: len(4), gcolAddr(8), gcolIndex(4)

      if (! (fillValue instanceof String))
        throwerr("fill type mismatch.  Expected: String.  Found: "
          + fillValue.getClass());
      if (elementLen != 4 + HdfFile.OFFSET_SIZE + 4)
        throwerr("invalid elementLen for fillValue");
    }
    else throwerr("unknown class for fillValue: "
      + fillValue.getClass() + "  dtype: " + dtype);
  }
}




public String toString() {
  String res = super.toString();
  if (fillValue == null) res += "  fillValue: (null)";
  else res += "  fillValue: " + fillValue + " (" + fillValue.getClass() + ")";
  return res;
}






// Format everything after the message header
void formatMsgCore( int formatPass, HBuffer fmtBuf)
throws HdfException
{
  fmtBuf.putBufByte("MsgFillValue: fillVersion", fillVersion);
  fmtBuf.putBufByte("MsgFillValue: fillAllocTime", fillAllocTime);
  fmtBuf.putBufByte("MsgFillValue: fillWriteTime", fillWriteTime);
  fmtBuf.putBufByte("MsgFillValue: fillDefined", fillDefined);
  if (fillVersion == 1
    || fillVersion == 2 && fillDefined == 1)
  {
    fmtBuf.putBufInt("MsgFillValue: elementLen", elementLen);

    if (dtype == HdfGroup.DTYPE_STRING_VAR) {
      byte[] bytes = Util.encodeString( (String) fillValue, false, hdfGroup);
      int heapIx = hdfFile.mainGlobalHeap.putHeapItem("fillValue", bytes);
      fmtBuf.putBufInt("MsgFillValue: vstring len", bytes.length);
      fmtBuf.putBufLong(
        "MsgFillValue: vstring gcol", hdfFile.mainGlobalHeap.blkPosition);
      fmtBuf.putBufInt("MsgFillValue: vstring heapIx", heapIx);
    }
    else {
      for (int ii = 0; ii < elementLen; ii++) {
        fmtBuf.putBufByte("MsgFillValue: fillBytes", 0xff & fillBytes[ii]);
      }
    }
  }
}



} // end class
