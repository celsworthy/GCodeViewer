set RELEASE_VERSION=4.00.01
if not exist "%PROGUARD_HOME%" set PROGUARD_HOME=D:\CEL\proguard6.1.1
if exist release rmdir /S/Q release
mkdir release

copy /Y target\GCodeViewer.jar release\GCodeViewer.jar
copy /Y GCodeViewer.json release\GCodeViewer.json
copy /Y GCodeViewer.configFile.xml release\GCodeViewer.configFile.xml
robocopy /S Language release\Language
robocopy /S target\lib release\lib

if not exist proguard mkdir proguard
if exist proguard\*.jar del /Y proguard\*.jar
%JAVA_HOME%\bin\java -jar "%PROGUARD_HOME%\lib\proguard.jar" @gcodeviewer_proguard.config
copy /Y proguard\GCodeViewerObfuscated.jar release\GCodeViewer.jar
copy /Y proguard\Configuration-1.3Obfuscated.jar release\lib\Configuration-1.3.jar
copy /Y proguard\Stenographer-1.10Obfuscated.jar release\lib\Stenographer-1.10.jar
copy /Y proguard\Licence-1.1Obfuscated.jar release\lib\Licence-1.1.jar
copy /Y proguard.mapping proguard.mapping.gcodeviewer.%RELEASE_VERSION%

