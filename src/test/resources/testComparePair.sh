#!/bin/sh


# Invoke h5dump and ncdump on newDs, which is a NetCDF4 binary file.
# Filter out some noise in the output.
# Compare the resulting output with the old standard, oldTxt.
# If not identical, print error messages and exit 1.


badparms() {
  echo "testComparePair.sh: error:"
  echo $1
  echo "testComparePair.sh: Parms"
  echo "  bugs"
  echo "  chunk"
  echo "  compress"
  echo "  oldTxt"
  echo "  newDs"
  exit 1
}






checkOne() {
  if [ $# -ne 5 ]; then badparms "wrong num parms"; fi
  bugs=$1
  chunk=$2
  compress=$3
  oldTxt=$4
  newDs=$5

  dumpCmd="h5dump -p -w 10000 $newDs"
  $dumpCmd | sed '1s/HDF5 .*/HDF5 "someFile" {/' > tempout.newa
  if [ "$?" -ne "0" ]; then
    echo "h5dump failed for:"
    echo "  config: $configMsg"
    echo "  cmd: $cmd"
    echo "  copyCmd: $copyCmd"
    echo "  dumpCmd: $dumpCmd"
    exit 1
  fi

  echo '===== ncdump =====' >> tempout.newa
  dumpCmd="ncdump $newDs"
  $dumpCmd | sed '1s/netcdf .*{/netcdf someFile {/' >> tempout.newa
  if [ "$?" -ne "0" ]; then
    echo "ncdump failed for:"
    echo "  config: $configMsg"
    echo "  cmd: $cmd"
    echo "  copyCmd: $copyCmd"
    echo "  dumpCmd: $dumpCmd"
    exit 1
  fi

  #sed -e 's/OFFSET [0-9][0-9]*/OFFSET someOffset/g' \
  #    -e 's/GROUP [0-9][0-9]*/GROUP someOffset/g' \
  #    -e 's/DATASET [0-9][0-9]*/DATASET someOffset/g' \
  #  tempout.newa > tempout.newb
  /bin/egrep -v '^ *OFFSET' tempout.newa > tempout.newb

  # Filter out stuff that changes with contig vs chunked.
  # For netcdf, don't filter the dimension variables,
  # so the sed ends at the start of dimensions '^   DATASET "dim00"'.
  #

  if [ "$chunk" == "contiguous" ]; then
    /bin/sed -e '1,/^   DATASET "dim00"/s/^ *CONTIGUOUS.*/          contigOrChunked/' \
      tempout.newb > tempout.newc
  else
    /bin/sed -e '1,/^   DATASET "dim00"/s/^ *CHUNKED.*/          contigOrChunked/' \
      tempout.newb > tempout.newc
  fi

  # Filter out stuff that changes with compression level
  if [ "$compress" == "0" ]; then
    /bin/sed -e '1,/^   DATASET "dim00"/s/^ *NONE$/          compressType/' \
      -e '1,/^   DATASET "dim00"/s/^ *SIZE.*/          someSize/' \
      tempout.newc > tempout.newd
  else
    /bin/sed -e '1,/^   DATASET "dim00"/s/^ *COMPRESSION DEFLATE.*/          compressType/' \
      -e '1,/^   DATASET "dim00"/s/^ *SIZE.*/          someSize/' \
      tempout.newc > tempout.newd
  fi

  # Filter out group offsets for reference types
  /bin/sed -e 's/GROUP [0-9][0-9]*/GROUP someOffset/g' \
    -e 's/DATASET [0-9][0-9]*/DATASET someDataset/g' \
    tempout.newd > tempout.newe

  # Filter out "_FillValue" and other attributes
  ./filterAttrs.py tempout.newe > tempout.newf
  if [ $? -ne 0 ]; then
    echo filterAttrs.py failed
    exit 1
  fi

  zcat $oldTxt > tempout.olda
  diffCmd="diff -w tempout.olda tempout.newf"
  if [ "$bugs" != "none" ]; then echo diffCmd: $diffCmd; fi
  $diffCmd
  diffOk=$?

  if [ "$diffOk" -ne "0" ]; then
    echo "Diff failed for:"
    echo "  config: $configMsg"
    echo "  cmd: $cmd"
    echo "  copyCmd: $copyCmd"
    echo "  diffCmd: $diffCmd"
    echo "wc:"
    wc tempout.olda tempout.newf
    exit 1
  fi
} # end checkOne



checkOne $1 $2 $3 $4 $5 $6 $7 $8 $9

