@echo off

echo CLEANING
del .\bin\xmlparse\*.class
echo BUILDING
javac -d .\bin .\src\xmlparse\*.java
echo MAKING JAR
cd .\bin
jar cf XMLParse.jar .\xmlparse\*.class
cd ..\