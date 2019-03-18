SET RELEASE_VERSION 3.03.00
if not exist "%PROGUARD_HOME%" set PROGUARD_HOME=D:\CEL\proguard6.0.3
if not exist proguard mkdir proguard
if exist proguard\*.jar del proguard\*.jar

java -jar "%PROGUARD_HOME%\lib\proguard.jar" @gcodeviewer_proguard.config
copy /Y proguard\GCodeViewerObfuscated.jar target\GCodeViewer.jar
copy /Y proguard\Configuration-1.2Obfuscated.jar target\lib\Configuration-1.2.jar
copy /Y proguard\Stenographer-1.9Obfuscated.jar target\lib\Stenographer-1.9.jar
copy /Y proguard\Licence-1.0Obfuscated.jar target\lib\Licence-1.0.jar
copy /Y proguard\Licence-1.0Obfuscated.jar target\lib\Licence-1.0.jar
copy /Y proguard.mapping target\proguard.mapping.gcodeviewer.%RELEASE_VERSION%
