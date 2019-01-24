if NOT DEFINED CEL_HOME set CEL_HOME=C:\Program Files\CEL
if NOT DEFINED GCODE_VIEWER_HOME set GCODE_VIEWER_HOME=D:\CEL\Dev\GCodeViewer\target
if NOT DEFINED GCODE_VIEWER_CONFIG set GCODE_VIEWER_CONFIG=D:\CEL\Dev\GCodeViewer
if NOT DEFINED JAVA_HOME set JAVA_HOME=%CEL_HOME%\AutoMaker\java\bin

"%JAVA_HOME%\java.exe" -DlibertySystems.configFile="%GCODE_VIEWER_CONFIG%\GCodeViewer.configFile.xml" -jar "%GCODE_VIEWER_HOME%\GCodeViewer.jar" %*
