@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  Gradle startup script for Windows
@rem
@rem ##########################################################################

set APP_BASE_NAME=%~n0
set APP_HOME=%~dp0

@rem Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

if defined JAVA_HOME goto findJavaFromJavaHome

set JAVACMD=java
"%JAVACMD%" -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto init
echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
goto fail

:findJavaFromJavaHome
set JAVACMD=%JAVA_HOME%/bin/java.exe
if exist "%JAVACMD%" goto init
echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
goto fail

:init
set CLASSPATH=%APP_HOME%gradle\wrapper\gradle-wrapper.jar
"%JAVACMD%" %DEFAULT_JVM_OPTS% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
goto end

:fail
exit /b 1

:end
exit /b 0
