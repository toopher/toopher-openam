javac -d . -classpath "/usr/share/tomcat6/webapps/openam_10.0.0/WEB-INF/lib/*" $(find src/ -name "*.java")
jar cf toopher-openam.jar com
