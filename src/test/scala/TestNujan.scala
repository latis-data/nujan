import edu.ucar.ral.nujan.netcdf._
import edu.ucar.ral.nujan.netcdf.NhVariable._


object TestNujan extends App {
  
  val ncWriter = new NhFileWriter("/data/tmp/nujan_test.nc")
  val rootGroup = ncWriter.getRootGroup
  
  // Define dimensions
  val tdim = rootGroup.addDimension("time", 2) //Note: unlimited not supported
  
  // Define variables
  val tvar = rootGroup.addVariable("time", TP_STRING_VAR, Array(tdim), null, null, 0)
  val fvar = rootGroup.addVariable("flux", TP_DOUBLE, Array(tdim), null, null, 0)
  
  ncWriter.endDefine()
  
  // Write data
  val tdata = Array("2018-01-01", "2018-01-02")
  val fdata = Array(1.23, 2.34)
  
  tvar.writeData(null, tdata)
  fvar.writeData(null, fdata)
  
  ncWriter.close()
  
}