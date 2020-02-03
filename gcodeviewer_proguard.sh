#!/bin/bash
if [[ -e ${PROGUARD_HOME} ]]
then
	export PROGUARD_HOME=/home/ubuntu/proguard
fi
mkdir -p release
rm -rf release/*
cp target/GCodeViewer.jar release/GCodeViewer.jar
cp GCodeViewer.json release/GCodeViewer.json
cp GCodeViewer.configFile.xml release/GCodeViewer.configFile.xml
cp -r Language release/Language
cp -r target/lib release/lib
if [ ${USE_PROGUARD} == "true" ]; then
	echo Run Proguard
	mkdir -p proguard
	rm -f proguard/*.jar
	${PROGUARD_HOME}/bin/proguard.sh @gcodeviewer_proguard_linux.config
	cp proguard.mapping target/proguard.mapping.gcodeviewer.${RELEASE_VERSION}
	cp -f proguard/GCodeViewerObfuscated.jar release/GCodeViewer.jar
	cp -f proguard/Configuration-1.3Obfuscated.jar release/lib/Configuration-1.3.jar
	cp -f proguard/Stenographer-1.10Obfuscated.jar release/lib/Stenographer-1.10.jar
	cp -f proguard/Licence-1.1Obfuscated.jar release/lib/Licence-1.1.jar
fi
