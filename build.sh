#javac -d . -classpath "/usr/share/tomcat6/webapps/openam_10.0.0/WEB-INF/lib/*:lib/*" $(find . -name "*.java")
javac -d . -classpath "lib/*" $(find . -name "*.java")
# ./src/com/toopher/api/ApiResponseObject.java ./src/com/toopher/api/AuthenticationStatus.java ./src/com/toopher/api/PairingStatus.java ./src/com/toopher/api/RequestError.java ./src/com/toopher/api/ToopherAPI.java ./src/com/toopher/openam/ToopherSecondFactorPrincipal.java ./src/com/toopher/openam/ToopherSecondFactor.java
jar cf toopher-openam.jar com
