javac -d ..\bin\ ..\Cliente\*.java
javac -d ..\bin\ ..\Autonoma\*.java
cd ..\Servidor\
call mvn clean install
pause


