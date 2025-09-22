SHELL := /bin/bash

APP_NAME := wcfc-manuals
APP_VERSION := $(shell mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
APP_JAR := target/$(APP_NAME)-$(APP_VERSION).jar
JAVA_FILES := $(shell find src/main/java/org/wingsofcarolina -name '*.java')
GOOGLE_CLOUD_REGION := us-central1
CONTAINER_TAG := $(GOOGLE_CLOUD_REGION)-docker.pkg.dev/wcfc-apps/wcfc-apps/$(APP_NAME):$(APP_VERSION)

ifneq ($(shell which podman),)
	CONTAINER_CMD := podman
else
ifneq ($(shell which docker),)
	CONTAINER_CMD := docker
else
	CONTAINER_CMD := /bin/false  # force error when used
endif
endif

$(APP_JAR): pom.xml client/node_modules $(JAVA_FILES) $(shell find src/main/resources -type f)
	@mvn --batch-mode

client/node_modules: client/package.json client/package-lock.json
	@cd client && npm ci --legacy-peer-deps
	@touch client/node_modules

docker/.build: $(APP_JAR)
	@cd docker && $(CONTAINER_CMD) build . -t $(CONTAINER_TAG)
	@touch docker/.build

.PHONY: build
build: docker/.build

.PHONY: check-version-not-dirty
check-version-not-dirty:
	@if [[ "$(CONTAINER_TAG)" == *"dirty"* ]]; then echo Refusing to build/push dirty version; git status; exit 1; fi

.PHONY: push
push: check-version-not-dirty docker/.build
	@echo Pushing $(CONTAINER_TAG)...
	@$(CONTAINER_CMD) push $(CONTAINER_TAG)

.PHONY: deploy
deploy: check-version-not-dirty push
	@gcloud run deploy $(APP_NAME) --image $(CONTAINER_TAG) --region $(GOOGLE_CLOUD_REGION)

.PHONY: launch
launch: docker/.build
	@echo Launching app...
	@./scripts/launch $(CONTAINER_TAG)
	@echo App should be running at http://localhost:9300

.PHONY: shutdown
shutdown:
	@echo Shutting down app...
	@$(CONTAINER_CMD) rm -f $(APP_NAME)

.PHONY: integration-tests
integration-tests: $(APP_JAR)
	@integration-tests/run-integration-tests.sh

.PHONY: format
format: client/node_modules
	@echo Formatting pom.xml files...
	@find . -name pom.xml -print0 | xargs -0 -I{} bash -c 'xmllint --format --output {} {}'
	@echo Formatting Java files...
	@mvn prettier:write -q
	@echo Formatting UI files...
	@cd client && npx prettier -w . --log-level silent

.PHONY: check-format
check-format: client/node_modules
	@find . -name pom.xml -print0 | xargs -0 -I{} bash -c 'xmllint --format {} | diff -q - {} > /dev/null' || (echo Problem in XML formatting; exit 1)
	@mvn prettier:check -q || (echo Problem in Java formatting; exit 1)
	@cd client && npx prettier --check . --log-level silent || (echo Problem in UI formatting; exit 1)

.PHONY: version
version:
	@echo $(APP_VERSION)

.PHONY: app-jar
app-jar:
	@echo $(APP_JAR)

.PHONY: clean
clean:
	@rm -rfv target/ docker/ client/build/ client/dist/ client/.sveltekit/

.PHONY: distclean
distclean: clean
	@rm -rfv .mvn/ root/ log/ data/ dynamic/ images/ tmp/ client/node_modules/ pom.xml.tag pom.xml.releaseBackup pom.xml.versionsBackup pom.xml.next release.properties dependency-reduced-pom.xml
