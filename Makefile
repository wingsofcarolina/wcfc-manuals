APP_VERSION := $(shell mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
APP_JAR := target/wcfc-manuals-$(APP_VERSION).jar
JAVA_FILES := $(shell find src/main/java/org/wingsofcarolina -name '*.java')

$(APP_JAR): pom.xml client/node_modules $(JAVA_FILES)
	@mvn

client/node_modules: client/package.json client/package-lock.json
	@cd client && npm install --legacy-peer-deps

.PHONY: format
format:
	@echo Formatting pom.xml files...
	@find . -name pom.xml -exec xmllint --format --output {} {} \;
	@echo Formatting Java files...
	@mvn prettier:write -q

.PHONY: clean
clean:
	@rm -rfv target/ docker/ client/build/ client/dist/ client/.sveltekit/

.PHONY: distclean
distclean: clean
	@rm -rfv .mvn/ root/ log/ data/ dynamic/ images/ tmp/ client/node_modules/ pom.xml.tag pom.xml.releaseBackup pom.xml.versionsBackup pom.xml.next release.properties dependency-reduced-pom.xml
