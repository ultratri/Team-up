@echo off
cd /d %~dp0
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dmaven.test.skip=true"
