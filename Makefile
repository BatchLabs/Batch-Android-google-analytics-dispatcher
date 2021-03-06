all: aar

aar: clean
	./gradlew assembleRelease --no-build-cache && \
	mkdir -p release/ && \
	cp google-analytics-dispatcher/build/outputs/aar/google-analytics-dispatcher-release.aar release/ && \
	cp LICENSE release/

clean:
	./gradlew clean

test:
	./gradlew testDebugUnitTest

test-coverage:
	./gradlew testDebugCoverageUnitTest && \
    awk -F"," '{ instructions += $$4 + $$5; covered += $$5 } END { print covered, "/", instructions, "instructions covered"; print "Total", 100*covered/instructions "% covered" }' google-analytics-dispatcher/build/test-results/jacoco.csv

check-token:
ifndef SONAR_TOKEN
	$(error SONAR_TOKEN is undefined)
endif

sonar: check-token
	./gradlew sonarqube

lint:
	./gradlew lintDebug

ci: clean lint test-coverage aar

.PHONY: ci sonar check-token
