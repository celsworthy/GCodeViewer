set RELEASE_VERSION=3.03.00_A8
if not exist "%PROGUARD_HOME%" set PROGUARD_HOME=D:\CEL\proguard6.0.3
if exist release rmdir /S/Q release
mkdir release

copy /Y target\GCodeViewer.jar release\GCodeViewer.jar
copy /Y GCodeViewer.json release\GCodeViewer.json
copy /Y GCodeViewer.configFile.xml release\GCodeViewer.configFile.xml
robocopy /S Language release\Language
robocopy /S target/lib release/lib

if not exist proguard mkdir proguard
if exist proguard\*.jar del /Y proguard\*.jar
java -jar "%PROGUARD_HOME%\lib\proguard.jar" @gcodeviewer_proguard.config
copy /Y proguard\GCodeViewerObfuscated.jar release\GCodeViewer.jar
copy /Y proguard\Configuration-1.2Obfuscated.jar release\lib\Configuration-1.2.jar
copy /Y proguard\Stenographer-1.9Obfuscated.jar release\lib\Stenographer-1.9.jar
copy /Y proguard\Licence-1.0Obfuscated.jar release\lib\Licence-1.0.jar
copy /Y proguard.mapping proguard.mapping.gcodeviewer.%RELEASE_VERSION%

