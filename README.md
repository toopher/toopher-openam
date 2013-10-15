Installing and Configuring the Toopher for OpenAM Integration
=============================================================

*  [Overview](#overview)

<h2 id="overview">Overview</h2>
<h3 id="scope">Document Scope</h3>
This guide details everything needed to integrate Toopher Two-Factor Authentication with OpenAM, a popular open-source
Authentication, Authorization, Entitlement, and Federation solution.  Readers should already be familiar with 
installation and administration for OpenAM and OpenLDAP, as well as be comfortable with general Linux
administration tasks.



<h3 id="compatibility">Compatibility Notes</h3>
These materials were primarily developed on the following environment, and some of the commands references may
be specific to these vendors.  If you would like us to provide assistance with installation on a different 
environment, please contact us at <support@toopher.com>:

    OS                : Centos 6.3 (2.6.32-279.el6.i686)
    OpenLDAP          : 2.4.23
    OpenAM            : OpenAM 10.0.0 (2012-April-13 10:24)
    Servlet Container : Apache Tomcat 6.0.24 (June 20 2013 1452)
    JVM Version       : 1.6.0_17-b04
    JVM Vendor        : Sun Microsystems Inc.

Other configurations are likely to work without issue, but have not been specifically tested at Toopher.

<h2 id="preparing">Preparing the System</h2>
<h3 id="importschema">Import the LDAP Schema</h3>
To facilitate keeping all user information in a single place, Toopher uses several custom LDAP
attributeTypes and objectClasses to track individual user's authentication settings.  These schema changes
must be imported into the LDAP directory prior to installing the OpenAM module.

A `slapd.conf`-compatible schema file is provided in the `schema` directory

    sudo cp schema/toopher_schema.schema /etc/openldap/schema

After copying the schema file, edit `/etc/openldap/slapd.conf` to `#include` it after `inetorgperson.schema`.

If your server uses [On-Line Configuration](http://www.zytrax.com/books/ldap/ch6/slapd-config.html) instead of `slapd.conf`,
install the ldif file instead:

    sudo cp cn\=\{99\}toopher_schema.ldif /etc/openldap/slapd.d/cn=config/cn=schema

It is a good idea to ensure that the configuration files are readable by the `ldap` user

    sudo chown -R ldap:ldap /etc/openldap

The schema changes will become active after the `slapd` daemon is restarted:

    sudo service slapd restart

<h3 id="copyfiles">Copy the Installation Files to OpenAM</h3>
All executable and configuration files must be copied to the proper spot in the OpenAM installation:

    sudo cp openam/lib/*.jar ${CATALINA_HOME}/webapps/openam/WEB_INF/lib/
    sudo cp openam/config/amAuthToopherSecondFactor.xml ${CATALINA_HOME}/webapps/openam/WEB_INF/classes/
    sudo cp openam/config/amAuthToopherSecondFactor.properties ${CATALINA_HOME}/webapps/openam/WEB_INF/classes/
    sudo cp openam/config/ToopherSecondFactor.xml ${CATALINA_HOME}/webapps/openam/config/auth/default/
    sudo cp openam/config/toopher-openam.js ${CATALINA_HOME}/webapps/openam/js

Again, it is a good idea to make sure tomcat can access all of the new files:

    sudo chown -R tomcat:tomcat ${CATALINA_HOME}/webapps/openam

<h2 id="install">Installation</h2>
<h3 id="enablessoadm">Enable ssoadm.jsp</h3>
Adding the new service requires `ssoadm.jsp`, which is disabled by default in newer versions of OpenAM for security
reasons.  Follow the instructions on the [Forgerock wiki](https://wikis.forgerock.org/confluence/display/openam/Activate+ssoadm.jsp)
to enable it.  Alternately, users who are familiar with the command-line `ssoadm` tool may wish to use
it instead of `ssoadm.jsp`, but this guide will focus on using `ssoadm.jsp`

<h3 id="installsvc">Create and Register the Toopher OpenAM Service</h3>
Log in to the OpenAM Top-Level Realm as an administrator (usually `amAdmin`), and navigate to 
<http://SERVERNAME:PORT/openam/ssoadm.jsp?cmd=create-svc> in a web browser.  Paste the *contents* of 
`amAuthToopherSecondFactor.xml` into the text area, and click `Submit`

Next, navigate to <http://SERVERNAME:PORT/openam/ssoadm.jsp?cmd=register-auth-module> in a web browser.
Enter `com.toopher.openam.ToopherSecondFactor` into the text box and click `Submit`

