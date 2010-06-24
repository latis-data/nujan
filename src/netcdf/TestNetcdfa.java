// The MIT License
// 
// Copyright (c) 2009 University Corporation for Atmospheric
// Research and Massachusetts Institute of Technology Lincoln
// Laboratory.
// 
// Permission is hereby granted, free of charge, to any person
// obtaining a copy of this software and associated documentation
// files (the "Software"), to deal in the Software without
// restriction, including without limitation the rights to use,
// copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the
// Software is furnished to do so, subject to the following
// conditions:
// 
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
// 
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
// OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT.  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
// HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
// WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
// OTHER DEALINGS IN THE SOFTWARE.


package nhPkgTest;

import java.util.Arrays;

import nhPkg.NhDimension;
import nhPkg.NhException;
import nhPkg.NhFileWriter;
import nhPkg.NhGroup;
import nhPkg.NhVariable;

import hdfnet.HdfException;      // used for sample data generation
import hdfnet.HdfGroup;          // used for sample data generation
import hdfnetTest.TestData;      // used for sample data generation


// Test byte / ubyte / short / int / long / float / double / char / vstring,
// with any number of dimensions.


public class TestNetcdfa {


static void badparms( String msg) {
  prtf("Error: %s", msg);
  prtf("parms:");
  prtf("  -bugs         <int>");
  prtf("  -nhType       byte / ubyte / short / int / long / float / double / char / vstring");
  prtf("  -dims         <int,int,...>   or \"0\" if a scalar");
  prtf("  -fileVersion  1 / 2");
  prtf("  -compress     compression level: 0==none, 1 - 9");
  prtf("  -outFile      <fname>");
  System.exit(1);
}



public static void main( String[] args) {
  try { runit( args); }
  catch( Exception exc) {
    exc.printStackTrace();
    prtf("main: caught: %s", exc);
    System.exit(1);
  }
}


static void runit( String[] args)
throws NhException
{
  int bugs = -1;
  String typeStg = null;
  int nhType = -1;
  int[] dims = null;
  String fileVersionStg = null;
  int compressLevel = -1;
  String outFile = null;

  if (args.length % 2 != 0) badparms("parms must be key/value pairs");
  for (int iarg = 0; iarg < args.length; iarg += 2) {
    String key = args[iarg];
    String val = args[iarg+1];
    if (key.equals("-bugs")) bugs = Integer.parseInt( val);
    else if (key.equals("-nhType")) {
      typeStg = val;
      if (val.equals("sbyte")) nhType = NhVariable.TP_SBYTE;
      else if (val.equals("ubyte")) nhType = NhVariable.TP_UBYTE;
      else if (val.equals("short")) nhType = NhVariable.TP_SHORT;
      else if (val.equals("int")) nhType = NhVariable.TP_INT;
      else if (val.equals("long")) nhType = NhVariable.TP_LONG;
      else if (val.equals("float")) nhType = NhVariable.TP_FLOAT;
      else if (val.equals("double")) nhType = NhVariable.TP_DOUBLE;
      else if (val.equals("char")) nhType = NhVariable.TP_CHAR;
      else if (val.equals("vstring")) nhType = NhVariable.TP_STRING_VAR;
      else badparms("unknown nhType: " + val);
    }
    else if (key.equals("-dims")) {
      if (val.equals("0")) dims = new int[0];
      else {
        String[] stgs = val.split(",");
        dims = new int[ stgs.length];
        for (int ii = 0; ii < stgs.length; ii++) {
          dims[ii] = Integer.parseInt( stgs[ii]);
          if (dims[ii] < 1) badparms("invalid dimension: " + dims[ii]);
        }
      }
    }
    else if (key.equals("-fileVersion")) fileVersionStg = val;
    else if (key.equals("-compress")) compressLevel = Integer.parseInt( val);
    else if (key.equals("-outFile")) outFile = val;
    else badparms("unkown parm: " + key);
  }

  if (bugs < 0) badparms("missing parm: -bugs");
  if (typeStg == null || nhType < 0) badparms("missing parm: -nhType");
  if (dims == null) badparms("missing parm: -dims");
  if (fileVersionStg == null) badparms("missing parm: -fileVersion");
  if (compressLevel < 0) badparms("missing parm: -compress");
  if (outFile == null) badparms("missing parm: -outFile");

  int fileVersion = 0;
  if (fileVersionStg.equals("1")) fileVersion = 1;
  else if (fileVersionStg.equals("2")) fileVersion = 2;
  else badparms("unknown fileVersion: " + fileVersionStg);

  prtf("TestNetcdfa: bugs: %d", bugs);
  prtf("TestNetcdfa: typeStg: \"%s\"", typeStg);
  prtf("TestNetcdfa: nhType: \"%s\"", NhVariable.nhTypeNames[nhType]);
  prtf("TestNetcdfa: rank: %d", dims.length);
  for (int idim : dims) {
    prtf("  TestNetcdfa: dim: %d", idim);
  }
  prtf("TestNetcdfa: fileVersion: %s", fileVersion);
  prtf("TestNetcdfa: compress: %d", compressLevel);
  prtf("TestNetcdfa: outFile: \"%s\"", outFile);

  NhFileWriter hfile = new NhFileWriter( outFile, NhFileWriter.OPT_OVERWRITE, fileVersion);
  hfile.setDebugLevel( bugs);
  hfile.setHdfDebugLevel( bugs);

  NhGroup rootGroup = hfile.getRootGroup();

  // Create test data and fill value.
  int dtype = 0;
  int stgFieldLen = 0;
  if (nhType == NhVariable.TP_SBYTE) dtype = HdfGroup.DTYPE_SFIXED08;
  else if (nhType == NhVariable.TP_UBYTE) dtype = HdfGroup.DTYPE_UFIXED08;
  else if (nhType == NhVariable.TP_SHORT) dtype = HdfGroup.DTYPE_FIXED16;
  else if (nhType == NhVariable.TP_INT) dtype = HdfGroup.DTYPE_FIXED32;
  else if (nhType == NhVariable.TP_LONG) dtype = HdfGroup.DTYPE_FIXED64;
  else if (nhType == NhVariable.TP_FLOAT) dtype = HdfGroup.DTYPE_FLOAT32;
  else if (nhType == NhVariable.TP_DOUBLE) dtype = HdfGroup.DTYPE_FLOAT64;
  else if (nhType == NhVariable.TP_CHAR) dtype = HdfGroup.DTYPE_TEST_CHAR;
  else if (nhType == NhVariable.TP_STRING_VAR)
    dtype = HdfGroup.DTYPE_STRING_VAR;
  else throwerr("unknown nhType: " + NhVariable.nhTypeNames[nhType]);

  Object vdata = null;
  Object fillValue = null;
  try {
    vdata = TestData.genHdfData(
      dtype,
      stgFieldLen,
      null,           // refGroup
      dims,
      0);             // ival, origin 0.

    fillValue = TestData.genFillValue(
      dtype,
      0);             // stgFieldLen.  If 0 and FIXED, MsgAttribute calcs it.
  }
  catch( HdfException exc) {
    exc.printStackTrace();
    throwerr("caught: " + exc);
  }

  // Netcdf doesn't support fill values for Strings or scalars.
  if (nhType == NhVariable.TP_STRING_VAR) fillValue = null;
  if (dims.length == 0) fillValue = null;

  NhGroup alpha1 = rootGroup.addGroup("alpha1");
  NhGroup alpha2 = rootGroup.addGroup("alpha2");
  NhGroup alpha3 = rootGroup.addGroup("alpha3");

  int rank = dims.length;
  NhDimension[] nhDims = new NhDimension[rank];
  for (int ii = 0; ii < rank; ii++) {
    nhDims[ii] = rootGroup.addDimension(
      String.format("dim%02d", ii),
      dims[ii]);
  }

  int numAttr = 3;
  // NetCDF attributes all have rank == 1, or be a simple String.
  // For nhType == TP_CHAR, must have isVlen = false.
  if (rank <= 1) {
    for (int ii = 0; ii < numAttr; ii++) {
      rootGroup.addAttribute(
        String.format("globAttr%04d", ii),   // attrName
        nhType,
        vdata);
    }
    if (numAttr > 0) {
      rootGroup.addAttribute(
        "globTextAttr",
        NhVariable.TP_STRING_VAR,
        "globTextValue");
    }
  }


// xxx Definitely weird.
//
// String valued attributes must be encoded with isVstring=false.
// If a String valued attribute is encoded as isVstring=true,
// ncdump fails:
//   ncdump: tempa.nc: Can't open HDF5 attribute
// Even though h5dump reads it just fine.
//
// But if the attr value is an array of String, it should be
// encoded as isVstring=true.  If it's encoded as isVstring=false,
// then ncdump may look for null termination.  Example:
//       string testVar0000:varAttr0000 = "0", "1a", "2ab?\177" ;
//                                                   * should be "2ab".
//
// One alternative would be to store add null term to all the
// fixed len strings, but that increases the storage len and
// the Unidata reader then returns the incremented length.
 


  int numVar = 2;
  NhVariable[] testVars = new NhVariable[ numVar];
  for (int ivar = 0; ivar < numVar; ivar++) {
    testVars[ivar] = testDefineVariable(
      numAttr,
      alpha2,                            // parentGroup
      ivar,
      String.format("testVar%04d", ivar),  // varName
      nhType,
      nhDims,
      compressLevel,
      vdata,
      fillValue);
  }

  hfile.endDefine();

  for (int ii = 0; ii < numVar; ii++) {
    testVars[ii].writeData( vdata);
  }

  hfile.close();
}



// Weird.  For chars ...
// For a variable, netcdf stores array of string len 1.
// For an attribute, netcdf stores array of signed byte,
//   for both nc_put_attr_schar / uchar / ubyte.
//
//    call:                 parm:      ncdump
//
//    nc_def_var NC_CHAR 3  "abcdef..."      vara = "abc" ;
//
//    nc_put_attr_schar     "abc"      :sattrNamea = 97b, 98b, 99b ;
//    nc_put_attr_uchar     "abc"      :sattrNamea = 97b, 98b, 99b ;
//    nc_put_attr_ubyte     "abc"      :sattrNamea = 97b, 98b, 99b ;
//
//    
//       DATASET "vara" {
//          DATATYPE  H5T_STRING {
//                STRSIZE 1;
//                STRPAD H5T_STR_NULLTERM;
//                CSET H5T_CSET_ASCII;
//                CTYPE H5T_C_S1;
//             }
//          DATASPACE  SIMPLE { ( 3 ) / ( 3 ) }
//          DATA {
//          (0): "a", "b", "c"
//          }
// 
//
//       ATTRIBUTE "attrSchar" {
//          DATATYPE  H5T_STD_I8LE
//          DATASPACE  SIMPLE { ( 3 ) / ( 3 ) }
//          DATA {
//          (0): 97, 98, 99
//          }
//       }
//       ATTRIBUTE "attrUbyte" {
//          DATATYPE  H5T_STD_I8LE
//          DATASPACE  SIMPLE { ( 3 ) / ( 3 ) }
//          DATA {
//          (0): 97, 98, 99
//          }
//       }
//       ATTRIBUTE "attrUchar" {
//          DATATYPE  H5T_STD_I8LE
//          DATASPACE  SIMPLE { ( 3 ) / ( 3 ) }
//          DATA {
//          (0): 97, 98, 99
//          }
//       }







static NhVariable testDefineVariable(
  int numAttr,
  NhGroup parentGroup,
  int ivar,
  String varName,
  int nhType,
  NhDimension[] nhDims,   // varDims
  int compressLevel,      // compression level: 0==none, 1 - 9
  Object vdata,
  Object fillValue)
throws NhException
{
  int rank = nhDims.length;

  NhVariable vara = parentGroup.addVariable(
    varName,             // varName
    nhType,              // nhType
    nhDims,              // varDims
    fillValue,
    compressLevel);

  // NetCDF attributes must have rank == 1, or be a simple String.
  if (ivar == 0 && rank <= 1) {
    for (int ii = 0; ii < numAttr; ii++) {
      vara.addAttribute(
        String.format("varAttr%04d", ii),   // attrName
        nhType,
        vdata);
    }
    if (numAttr > 0) {
      vara.addAttribute(
        "varaTextAttr",
        NhVariable.TP_STRING_VAR,
        "varaTextValue");
    }
  }

  prtf("TestNetcdfa: parentGroup: %s", parentGroup);
  prtf("TestNetcdfa: vara: %s", vara);
  return vara;

} // end testDefineVariable




static void throwerr( String msg, Object... args)
throws NhException
{
  throw new NhException( String.format( msg, args));
}





static void prtf( String msg, Object... args) {
  System.out.printf( msg, args);
  System.out.printf("\n");
}

} // end class
