#!/bin/bash

VCF=$1
TSV=$2
PHARMCAT="$HOME/gareth/pharmcat/pharmcat-2.9.0-all.jar"
REF="$HOME/ddna/data/genome/hg38.fa"

#the outside call file follows the format here: https://pharmcat.org/using/Outside-Call-Format/

java -jar $PHARMCAT -vcf $VCF -o pharmcat_output -reporterJson -po $TSV
