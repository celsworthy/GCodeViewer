#!/bin/bash
echo "Proguard: " ${USE_PROGUARD}
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
	/home/ubuntu/proguard/bin/proguard.sh @gcodeviewer_proguard_linux.config
	cp proguard.mapping target/proguard.mapping.gcodeviewer.${RELEASE_VERSION}
	cp -f proguard/GCodeViewerObfuscated.jar release/GCodeViewer.jar
	cp -f proguard/Configuration-1.2Obfuscated.jar release/lib/Configuration-1.2.jar
	cp -f proguard/Stenographer-1.9Obfuscated.jar release/lib/Stenographer-1.9.jar
	cp -f proguard/Licence-1.0Obfuscated.jar release/lib/Licence-1.0.jar
fi
zip -r GCodeViewer.zip release/*
