#!/bin/sh


# Validation using synthetic data.
#
# Run Testa.java on various configurations and compare the
# output with saved known good files.
#
# For usage, see badparms below.
#
# To compare a new netcdf type (int and double, for example):
#   cd ../netcdf
#   for ii in 0 1 2 3 4 5; do diff <(sed -e '1,/ncdump/ d'  testSynOut/test.int.rank.$ii.out) <(sed -e '1,/ncdump/ d'  testSynOut/test.double.rank.$ii.out) | less; done


BUILDDIR=/home/steves/tech/hdf5/build
PKGBASE=edu.ucar.ral.nujan


badparms() {
  echo Error: $1
  echo Parms:
  echo "  version:   all / v1 / v2"
  echo "  chunkSpec: all / contig / chunked"
  echo "  compress:  all / compressLevel (0==none, 1 - 9)"
  echo "  dtype:     all / sfixed08 / ufixed08 /fixed16,32,64"
  echo "               float32,64 string14 vstring reference compound"
  echo "  rank:      all/0/1/2/3/4/5"
  echo "  bugs       optional: none / echo / continue / update"
  echo "               none: no debug msgs"
  echo "               echo: some debug msgs"
  echo "               continue: continue even if diff errors"
  echo "               update: update verification results - Caution"
  echo ""
  echo "Examples:"
  echo "./testSyn.sh v1  contig  0 fixed16  1"
  echo "./testSyn.sh v1  chunked 0 string14 2"
  echo "./testSyn.sh v2  contig  0 vstring  all"
  echo "./testSyn.sh all all     0 all      all"
  exit 1
}

if [ $# -eq 1 ]; then
  if [ "$1" != "all" ]; then badparms "wrong num parms"; fi
  ./testSyn.sh all all 0 all all
  if [ "$?" -ne 0 ]; then echo "exiting"; exit 1; fi
  ./testSyn.sh all chunked 5 all all
  if [ "$?" -ne 0 ]; then echo "exiting"; exit 1; fi
  exit 0
fi


if [ $# -ne 5 -a $# -ne 6 ]; then badparms "wrong num parms"; fi

versionSpec=$1
chunkSpec=$2
compressSpec=$3
dtypeSpec=$4
rankSpec=$5
bugs=none
if [ $# -eq 6 ]; then bugs=$6; fi

if [ "$versionSpec" == "all" ]; then versions="1 2"
elif [ "$versionSpec" == "v1" ]; then versions="1"
elif [ "$versionSpec" == "v2" ]; then versions="2"
else badparms "invalid ver"
fi

if [ "$chunkSpec" == "all" ]; then chunks="contig chunked"
else chunks="$chunkSpec"
fi

if [ "$compressSpec" == "all" ]; then compressVals="0 5"
else compressVals="$compressSpec"
fi

if [ "$dtypeSpec" == "all" ]; then
  dtypes="sfixed08 ufixed08 fixed16 fixed32 fixed64 float32 float64 string14 vstring reference"
else dtypes=$dtypeSpec
fi

if [ "$rankSpec" == "all" ]; then ranks="0 1 2 3 4 5 6 7"
else ranks=$rankSpec
fi



echo "versions: $versions"
echo "chunks: $chunks"
echo "compressVals: $compressVals"
echo "dtypes: $dtypes"
echo "ranks: $ranks"

make all
if [ $? -ne 0 ]; then badparms "make failed"; fi








testOne() {
  if [ $# -ne 5 ]; then
    badparms "wrong num parms"
  fi
  fileVersion=$1
  chunk=$2
  compress=$3
  dtype=$4
  rank=$5
  if [ "$bugs" != "none" ]; then
    echo "testOne: vers: $fileVersion  chunk: $chunk  compress: $compress"
    echo "  dtype: $dtype  rank: $rank"
  fi

  if [[ "$rank" == "0" ]]; then dims="0"
  elif [[ "$rank" == "1" ]]; then dims="3"
  elif [[ "$rank" == "2" ]]; then dims="3,4"
  elif [[ "$rank" == "3" ]]; then dims="3,4,5"
  elif [[ "$rank" == "4" ]]; then dims="3,4,5,2"
  elif [[ "$rank" == "5" ]]; then dims="3,4,5,2,3"
  elif [[ "$rank" == "6" ]]; then dims="3,4,5,2,3,2"
  elif [[ "$rank" == "7" ]]; then dims="3,4,5,2,3,2,3"
  else badparms "invalid rank: $rank"
  fi

  if [ "$compress" != "0" -a "$dtype" == "vstring" ]; then
    # Cannot compress vstring because vstrings are kept on
    # the global heap, and hdf5 compresses only the references
    # to the strings, not the strings themselves.
    # So we don't allow compression of vstrings.
    echo "Cannot compress vstring (rank $rank) ... ignoring"
  elif [ "$chunk" == "chunked" -a "$rank" == "0" ]; then
    echo "Cannot use chunked with scalar data ... ignoring"
  elif [ "$compress" != "0" -a "$rank" == "0" ]; then
    echo "Cannot compress scalar data ... ignoring"
  elif [ "$compress" != "0" -a "$chunk" == "contig" ]; then
    echo "Cannot compress contiguous data ... ignoring"
  else

    /bin/rm -f tempa.h5

    cmd="java -cp ${BUILDDIR} \
      ${PKGBASE}.hdfTest.Testa \
      -bugs 10 \
      -dtype $dtype \
      -dims $dims \
      -fileVersion $fileVersion \
      -chunked $chunk \
      -compress $compress \
      -outFile tempa.h5"

    if [ "$bugs" != "none" ]; then echo "cmd: $cmd"; fi
    configMsg="./testSyn.sh v$fileVersion $chunk $compress $dtype $rank"

    $cmd > tempa.log
    if [ "$?" -ne "0" ]; then
      echo "Cmd failed for config: $configMsg"
      echo "  cmd: $cmd"
      exit 1
    fi

    echo "  test: $configMsg  size: $(wc -c tempa.h5 | cut -f 1 -d ' ')"

    oldTxt=testSynOut.v$fileVersion/test.$dtype.rank.$rank.out.gz

    dumpCmd="h5dump -p -w 10000 tempa.h5"
    $dumpCmd > tempout.newa
    if [ "$?" -ne "0" ]; then
      echo "h5dump failed for config: $configMsg"
      echo "  cmd: $cmd"
      echo "  dumpCmd: $dumpCmd"
      exit 1
    fi

    /bin/egrep -v '^ *OFFSET' tempout.newa > tempout.newb

    # Filter out stuff that changes with contig vs chunked.
    if [ "$chunk" == "contig" ]; then
      /bin/sed -e 's/^ *CONTIGUOUS.*/          contigOrChunked/' \
        tempout.newb > tempout.newc
    else
      /bin/sed -e 's/^ *CHUNKED.*/          contigOrChunked/' \
        tempout.newb > tempout.newc
    fi

    # Filter out stuff that changes with compression level
    if [ "$compress" == "0" ]; then
      /bin/sed -e 's/^ *NONE$/          compressType/' \
        -e '1,/^   DATASET "dim00"/s/^ *SIZE.*/          someSize/' \
        tempout.newc > tempout.newd
    else
      /bin/sed -e 's/^ *COMPRESSION DEFLATE.*/          compressType/' \
        -e '1,/^   DATASET "dim00"/s/^ *SIZE.*/          someSize/' \
        tempout.newc > tempout.newd
    fi

    # Filter out group offsets for reference types
    if [ "$dtype" == "reference" ]; then
      /bin/sed -e 's/GROUP [0-9][0-9]*/GROUP someOffset/g' \
        -e 's/DATASET [0-9][0-9]*/DATASET someDataset/g' \
        tempout.newd > tempout.newe
    else
      cp tempout.newd tempout.newe
    fi

    zcat $oldTxt > tempout.olda
    diffCmd="diff -w tempout.olda tempout.newe"
    if [ "$bugs" != "none" ]; then echo diffCmd: $diffCmd; fi
    $diffCmd
    diffOk=$?

    # Copy to testSynOut
    if [ "$bugs" == "update" ]; then
      gzip -c tempout.newe > $oldTxt
      echo '*** updated ***'
    fi

    if [ "$diffOk" -ne "0" \
      -a "$bugs" != "continue" \
      -a "$bugs" != "update" ]; then
      echo "Diff failed for config: $configMsg"
      echo "  cmd: $cmd"
      echo "  diffCmd: $diffCmd"
      echo "wc:"
      wc tempout.olda tempout.newe
      exit 1
    fi

  fi
} # end testOne




for version in $versions; do
  for chunk in $chunks; do
    for compress in $compressVals; do
      for dtype in $dtypes; do
        for rank in $ranks; do

          testOne $version $chunk $compress $dtype $rank

        done
      done
    done
  done
done
