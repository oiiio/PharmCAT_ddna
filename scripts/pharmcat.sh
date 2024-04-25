#!/bin/bash

VCF="/Users/gareth/PharmCAT_ddna/scratch/rerun_NA12878.preprocessed.vcf.bgz"
TSV="/Users/gareth/PharmCAT_ddna/scratch/cyp2d6.hybrid.tsv"
PHARMCAT="/Users/gareth/PharmCAT_ddna/build/libs/pharmcat-2.11.0-16-g16fabf20-all.jar"
REF="$HOME/ddna/data/genome/hg38.fa"

#the outside call file follows the format here: https://pharmcat.org/using/Outside-Call-Format/

java -jar $PHARMCAT -vcf $VCF -o scratch/pharmcat_output -reporterJson -po $TSV