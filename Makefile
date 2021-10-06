MAKEFLAGS = -j1

# Determine this makefile's path.
# Be sure to place this BEFORE `include` directives, if any.
THIS_FILE := $(lastword $(MAKEFILE_LIST))

ifdef PHARMCAT_DATA_DIR
  dataDir := $(PHARMCAT_DATA_DIR)
else
  dataDir := build
endif

ifeq ($(OS),Windows_NT)
	GRADLE_CMD := cmd /c gradlew.bat --console=plain
else
	GRADLE_CMD := ./gradlew --console=plain
endif


.PHONY: docker
docker:
	docker build -t pcat .


.PHONY: vcfTests
vcfTests:
	${GRADLE_CMD} clean
	rm -rf ${dataDir}/testVcf ${dataDir}/autogeneratedTestResults
	src/scripts/generate_vcf_test_data.sh
	${GRADLE_CMD} testAutogeneratedVcfs
	cd ${dataDir}; zip -r vcfTest-`date +'%Y-%m-%d'`.zip autogeneratedTestResults

.PHONY: vcfMissingTests
vcfMissingTests:
	${GRADLE_CMD} clean
	rm -rf ${dataDir}/testVcf ${dataDir}/autogeneratedTestResults
	src/scripts/generate_vcf_test_data.sh -m
	${GRADLE_CMD} testAutogeneratedVcfs
	cd ${dataDir}; zip -r vcfTest-`date +'%Y-%m-%d'-missing`.zip autogeneratedTestResults


.PHONY: exactVcfTests
exactVcfTests:
	${GRADLE_CMD} clean
	rm -rf ${dataDir}/testVcf ${dataDir}/autogeneratedTestResults
	src/scripts/generate_vcf_test_data.sh
	${GRADLE_CMD} testAutogeneratedVcfsExactMatchOnly
	cd ${dataDir}; zip -r vcfTest-`date +'%Y-%m-%d'`-exact.zip autogeneratedTestResults

.PHONY: exactVcfMissingTests
exactVcfMissingTests:
	${GRADLE_CMD} clean
	rm -rf ${dataDir}/testVcf ${dataDir}/autogeneratedTestResults
	src/scripts/generate_vcf_test_data.sh -m
	${GRADLE_CMD} testAutogeneratedVcfsExactMatchOnly
	cd ${dataDir}; zip -r vcfTest-`date +'%Y-%m-%d'`-exact-missing.zip autogeneratedTestResults


.PHONY: fuzzyVcfTests
fuzzyVcfTests:
	${GRADLE_CMD} clean
	rm -rf ${dataDir}/testVcf ${dataDir}/autogeneratedTestResults
	src/scripts/generate_vcf_test_data.sh
	${GRADLE_CMD} testAutogeneratedVcfsFuzzyMatch
	cd ${dataDir}; zip -r vcfTest-`date +'%Y-%m-%d'`-fuzzy.zip autogeneratedTestResults

.PHONY: fuzzyVcfMissingTests
fuzzyVcfMissingTests:
	${GRADLE_CMD} clean
	rm -rf ${dataDir}/testVcf ${dataDir}/autogeneratedTestResults
	src/scripts/generate_vcf_test_data.sh -m
	${GRADLE_CMD} testAutogeneratedVcfsFuzzyMatch
	cd ${dataDir}; zip -r vcfTest-`date +'%Y-%m-%d'`-fuzzy-missing.zip autogeneratedTestResults


.PHONY: updateData
updateData:
	${GRADLE_CMD} clean
	${GRADLE_CMD} updateData
	mv src/main/resources/org/pharmgkb/pharmcat/definition/alleles/pharmcat_positions.* .
	${GRADLE_CMD} updateExample
	mv docs/examples/pharmcat_positions.matcher.html    docs/examples/pharmcat.example.matcher.html
	mv docs/examples/pharmcat_positions.matcher.json    docs/examples/pharmcat.example.matcher.json
	mv docs/examples/pharmcat_positions.phenotyper.json docs/examples/pharmcat.example.phenotyper.json
	mv docs/examples/pharmcat_positions.report.html     docs/examples/pharmcat.example.report.html
	mv docs/examples/pharmcat_positions.report.json     docs/examples/pharmcat.example.report.json
