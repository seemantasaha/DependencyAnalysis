#!/bin/bash

REPO=/home/dmcd2356/Projects/ISSTAC/repo
#JAVA=/home/zzk/projects/isstac/jre1.7.0_80/lib
#BASE=/home/zzk/projects/isstac/zzk/tweeter
JAVA=${REPO}/jre1.7.0_80/lib
BASE=${REPO}/engagement/engagement_4/challenge_programs/tweeter
LIB=${BASE}/janalyzer/averroes/lib
TOOLS=${REPO}/janalyzer/tool

#REG=com.tweeter.**:ec.util.**:org.springframework.**:util.**:vash.**
#MAIN=org.springframework.boot.loader.JarLauncher
REG=com.tweeter.**:ec.util.**:util.**:vash.**
MAIN=com.tweeter.TwitterApplication
APP=${BASE}/challenge_program/Tweeter-1.0.0a.jar
OUT=output_tweeter

# create the library list from the jar files in the ${LIB} directory
LIBSTRING=""
while read -r NAME; do
    LIBSTRING+=:${NAME}
done < <(ls -1 -X ${LIB}/*.jar)
LIBSTRING=${LIBSTRING:1}
#LIBSTRING=$LIB/antlr-2.7.7.jar:$LIB/aspectjweaver-1.8.9.jar:$LIB/classmate-1.3.1.jar:$LIB/dom4j-1.6.1.jar:$LIB/groovy-2.4.7.jar:$LIB/h2-1.3.176.jar:$LIB/hibernate-commons-annotations-5.0.1.Final.jar:$LIB/hibernate-core-5.0.9.Final.jar:$LIB/hibernate-entitymanager-5.0.9.Final.jar:$LIB/hibernate-jpa-2.1-api-1.0.0.Final.jar:$LIB/hibernate-validator-5.2.4.Final.jar:$LIB/jackson-annotations-2.8.1.jar:$LIB/jackson-core-2.8.1.jar:$LIB/jackson-databind-2.8.1.jar:$LIB/jandex-2.0.0.Final.jar:$LIB/javassist-3.20.0-GA.jar:$LIB/javax.transaction-api-1.2.jar:$LIB/jboss-logging-3.3.0.Final.jar:$LIB/jcl-over-slf4j-1.7.21.jar:$LIB/joda-time-2.9.3.jar:$LIB/jul-to-slf4j-1.7.21.jar:$LIB/log4j-over-slf4j-1.7.21.jar:$LIB/logback-classic-1.1.7.jar:$LIB/logback-core-1.1.7.jar:$LIB/nekohtml-1.9.15.jar:$LIB/ognl-3.0.8.jar:$LIB/slf4j-api-1.7.21.jar:$LIB/snakeyaml-1.17.jar:$LIB/spring-aop-4.3.2.RELEASE.jar:$LIB/spring-aspects-4.3.2.RELEASE.jar:$LIB/spring-beans-4.3.2.RELEASE.jar:$LIB/spring-boot-1.4.0.RELEASE.jar:$LIB/spring-boot-autoconfigure-1.4.0.RELEASE.jar:$LIB/spring-boot-starter-1.4.0.RELEASE.jar:$LIB/spring-boot-starter-aop-1.4.0.RELEASE.jar:$LIB/spring-boot-starter-data-jpa-1.4.0.RELEASE.jar:$LIB/spring-boot-starter-jdbc-1.4.0.RELEASE.jar:$LIB/spring-boot-starter-logging-1.4.0.RELEASE.jar:$LIB/spring-boot-starter-security-1.4.0.RELEASE.jar:$LIB/spring-boot-starter-thymeleaf-1.4.0.RELEASE.jar:$LIB/spring-boot-starter-tomcat-1.4.0.RELEASE.jar:$LIB/spring-boot-starter-web-1.4.0.RELEASE.jar:$LIB/spring-context-4.3.2.RELEASE.jar:$LIB/spring-core-4.3.2.RELEASE.jar:$LIB/spring-data-commons-1.12.2.RELEASE.jar:$LIB/spring-data-jpa-1.10.2.RELEASE.jar:$LIB/spring-expression-4.3.2.RELEASE.jar:$LIB/spring-jdbc-4.3.2.RELEASE.jar:$LIB/spring-orm-4.3.2.RELEASE.jar:$LIB/spring-security-config-4.1.1.RELEASE.jar:$LIB/spring-security-core-4.1.1.RELEASE.jar:$LIB/spring-security-web-4.1.1.RELEASE.jar:$LIB/spring-session-1.2.1.RELEASE.jar:$LIB/spring-tx-4.3.2.RELEASE.jar:$LIB/spring-web-4.3.2.RELEASE.jar:$LIB/spring-webmvc-4.3.2.RELEASE.jar:$LIB/thymeleaf-2.1.5.RELEASE.jar:$LIB/thymeleaf-layout-dialect-1.4.0.jar:$LIB/thymeleaf-spring4-2.1.5.RELEASE.jar:$LIB/tomcat-embed-core-8.5.4.jar:$LIB/tomcat-embed-el-8.5.4.jar:$LIB/tomcat-embed-websocket-8.5.4.jar:$LIB/tomcat-jdbc-8.5.4.jar:$LIB/tomcat-juli-8.5.4.jar:$LIB/unbescape-1.1.0.RELEASE.jar:$LIB/validation-api-1.1.0.Final.jar:$LIB/xercesImpl-2.9.1.jar:$LIB/xml-apis-1.4.01.jar

echo java -jar ${TOOLS}/averroes.jar -r ${REG} -m ${MAIN} -a ${APP} -l ${LIBSTRING} -o ${OUT} -j ${JAVA}
java -jar ${TOOLS}/averroes.jar -r ${REG} -m ${MAIN} -a ${APP} -l ${LIBSTRING} -o ${OUT} -j ${JAVA}
