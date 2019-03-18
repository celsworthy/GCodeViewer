#!/bin/bash
echo "Proguard: " ${USE_PROGUARD}
if [ $1 == "true" ]; then
	echo Run Proguard
	mkdir -p proguard
	rm -f proguard/*.jar
	/home/ubuntu/proguard/bin/proguard.sh @gcodeviewer_proguard_linux.config
	cp -f proguard/GCodeViewerObfuscated.jar target/GCodeViewer.jar
	cp -f proguard/Configuration-1.2Obfuscated.jar target/lib/Configuration-1.2.jar
	cp -f proguard/Stenographer-1.9Obfuscated.jar target/lib/Stenographer-1.9.jar
	cp -f proguard/Licence-1.0Obfuscated.jar target/lib/Licence-1.0.jar
	cp proguard.mapping target/proguard.mapping.gcodeviewer.${RELEASE_VERSION}
fi
