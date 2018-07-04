@echo off

echo CLEANING
del .\bin\*.class
echo BUILDING
javac -d .\bin *.java
echo MAKING JAR
jar cf .\bin\XMLParse.jar .\bin\*.class