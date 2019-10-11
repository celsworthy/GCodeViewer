if NOT DEFINED CEL_HOME set CEL_HOME=C:\Program Files\CEL
if NOT DEFINED GCODE_VIEWER_HOME set GCODE_VIEWER_HOME=%CEL_HOME%\Common\GCodeViewer
if NOT DEFINED GCODE_VIEWER_JAR set GCODE_VIEWER_JAR=%GCODE_VIEWER_HOME%\GCodeViewer.jar
if NOT DEFINED GCODE_VIEWER_JSON set GCODE_VIEWER_JSON=%GCODE_VIEWER_HOME%
if NOT DEFINED GCODE_VIEWER_CONFIG set GCODE_VIEWER_CONFIG=%GCODE_VIEWER_HOME%\GCodeViewer.configFile.xml
if NOT DEFINED JAVA_HOME set JAVA_HOME=%CEL_HOME%\AutoMaker\java\bin

set GCODE_VIEWER_JAR=D:\CEL\Java11\GCodeViewer\target\GCodeViewer.jar
set GCODE_VIEWER_JSON=D:\CEL\Java11\GCodeViewer
set GCODE_VIEWER_CONFIG=D:\CEL\Java11\GCodeViewer\GCodeViewerFake.configFile.xml

"%JAVA_HOME%\bin\java.exe" -DlibertySystems.configFile="%GCODE_VIEWER_CONFIG%" -jar "%GCODE_VIEWER_JAR%" -cd "%GCODE_VIEWER_JSON%" %*
