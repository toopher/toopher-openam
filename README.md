Installing and Configuring the Toopher for OpenAM Integration
=============================================================

## Overview
### Document Scope
This guide details everything needed to integrate Toopher Two-Factor Authentication with OpenAM, a popular open-source
Authentication, Authorization, Entitlement, and Federation solution.  Readers should already be familiar with 
installation and administration for OpenAM and OpenLDAP, as well as be comfortable with general Linux
administration tasks.



### Compatibility Notes
These materials were primarily developed on the following environment, and some of the commands referenced may
be specific to these vendors.  If you would like us to provide assistance with installation on a different 
environment, please contact us at <support@toopher.com>:

    OS                : Centos 6.3 (2.6.32-279.el6.i686)
    OpenLDAP          : 2.4.23
    OpenAM            : OpenAM 10.0.0 (2012-April-13 10:24)
    Servlet Container : Apache Tomcat 6.0.24 (June 20 2013 1452)
    JVM Version       : 1.6.0_17-b04
    JVM Vendor        : Sun Microsystems Inc.

Other configurations are likely to work without issue, but have not been specifically tested at Toopher.

## Preparing the System
### Extract files from archive
All the required files are included in the tarball we provided.  Extract the files into a directory of your choice as usual:

    tar xzvf toopher-openam.tgz
    
The commands given below assume your working directory is the same as that used when extracting the tarball (unless otherwise described)
    
### Import the LDAP Schema
To facilitate keeping all user information in a single place, Toopher uses several custom LDAP
attributeTypes and objectClasses to track individual user's authentication settings.  These schema changes
must be imported into the LDAP directory prior to installing the OpenAM module.

A `slapd.conf`-compatible schema file is provided in the `schema` directory

    sudo cp schema/toopher_schema.schema /etc/openldap/schema

After copying the schema file, edit `/etc/openldap/slapd.conf` to `#include` it after `inetorgperson.schema`.

If your server uses [On-Line Configuration](http://www.zytrax.com/books/ldap/ch6/slapd-config.html) instead of `slapd.conf`,
install the ldif file instead:

    sudo cp schema/cn\=\{90\}toopher_schema.ldif /etc/openldap/slapd.d/cn=config/cn=schema

Ensure that the configuration files are readable by the `ldap` user

    sudo chown -R ldap:ldap /etc/openldap

The schema changes will become active after the `slapd` daemon is restarted:

    sudo service slapd restart

### Copy the Installation Files to OpenAM
All executable and configuration files must be copied to the proper spot in the OpenAM installation:

    sudo cp openam/lib/*.jar ${CATALINA_HOME}/webapps/openam/WEB_INF/lib/
    sudo cp openam/config/amAuthToopherSecondFactor.xml ${CATALINA_HOME}/webapps/openam/WEB_INF/classes/
    sudo cp openam/config/amAuthToopherSecondFactor.properties ${CATALINA_HOME}/webapps/openam/WEB_INF/classes/
    sudo cp openam/config/ToopherSecondFactor.xml ${CATALINA_HOME}/webapps/openam/config/auth/default/
    sudo cp openam/config/toopher-openam.js ${CATALINA_HOME}/webapps/openam/js

Edit Login.jsp to include the Toopher javascript file at the end of the page (just above the closing `</body>` tag):

                    </div>
                </div>
                <!--Beginning of required change-->
                <script language="JavaScript" src="<%= ServiceURI%>/js/toopher-openam.js" type="text/javascript"></script>
                <!--End of required change-->
            </body>
        </jato:useViewBean>
    </html>

Make sure tomcat can access all of the new files:

    sudo chown -R tomcat:tomcat ${CATALINA_HOME}/webapps/openam

## Installation
### Enable ssoadm.jsp
Adding the new service requires `ssoadm.jsp`, which is disabled by default in newer versions of OpenAM.  Follow the instructions on the [Forgerock wiki](https://wikis.forgerock.org/confluence/display/openam/Activate+ssoadm.jsp)
to enable it.  Alternately, users who are familiar with the command-line `ssoadm` tool may wish to use
it instead of `ssoadm.jsp`, but this guide will focus on using `ssoadm.jsp`

### Create and Register the Toopher OpenAM Service
Log in to the OpenAM Top-Level Realm as an administrator (usually `amAdmin`), and navigate to 
<http://SERVERNAME:PORT/openam/ssoadm.jsp?cmd=create-svc> in a web browser.  Paste the *contents* of 
`amAuthToopherSecondFactor.xml` into the text area, and click `Submit`

Next, navigate to <http://SERVERNAME:PORT/openam/ssoadm.jsp?cmd=register-auth-module> in a web browser.
Enter `com.toopher.openam.ToopherSecondFactor` into the text box and click `Submit`

Finally, restart tomcat so the changes take effect:

    sudo service tomcat6 restart

### Configure the Toopher module
If you have not done so already, provision a new Toopher Consumer Key/Secret pair for the OpenAM server by visiting <https://dev.toopher.com> and creating a new requester.

From the [OpenAM Administration page](http://SERVERNAME:PORT/openam/task/Home), navigate to the Toopher Service
 Configuration page (Click on `Configuration`, then click on `Toopher Two-Factor Security`.  
Enter your Toopher Consumer Key and Secret, along with the information required to connect to your LDAP server.
The `Bind User DN` user must have sufficient permissions to modify user objects and create new entries in the directory. 
When finished, click `Save`.

## Realm Configuration
### Enable Toopher for selected realms
From the [OpenAM Administration Page](http://SERVERNAME:PORT/openam/task/Home), navigate to the realm's Authentication setup page (Click the `Access Control` tab, click on realm you want to modify, then select `Authentication`).  In the `Module Instances` section, click `New` to add the Toopher module.  When prompted, name the module "ToopherSecondFactor", set the type to `Toopher Two-Factor Security`, and click `OK`.

### Create an Authentication Chain with Toopher
Once `ToopherSecondFactor` appears in the `Module Instances` list, create a new Authentication Chain by clicking `New` in the `Authentication Chaining` section.  Name the chain `ToopherAuth`, and click `OK`.

Add the `LDAP` module to the chain, with Criteria set to `REQUISITE`.

Add `ToopherSecondFactor` to the chain, with Criteria set to `REQUIRED`.  

In the `Options` field of `ToopherSecondFactor`, paste the following option:

    iplanet-am-auth-shared-state-behavior-pattern=useFirstPass

Configure the rest of the realm settings as fits your needs.

Congratulations - You're done!

## Administering a Toopher-Enabled Userbase
### Understanding The Toopher Authentication Chain
Each LDAP user has two attributes that control how the `ToopherAuth` Authentication Chain handles their Authentication session

* `toopherAuthenticateLogon` (Boolean) - Holds a `TRUE` or `FALSE` value that determines whether the user wants to add Toopher authentication.  This attribute can also be missing, which indiciates that the user has not made an election on whether to use Toopher.
* `toopherPairingId` (String) - Holds a pseudo-random string that identifies the user's mobile device to the Toopher API.

There are four states a user can be in when they authenticate:

* **`toopherAuthenticateLogon` and `toopherPairingId` both missing** : This will be the common case immediately after the Toopher for OpenAM module is enabled.  Users will be prompted to choose whether or not they want to enroll in Toopher authentication.  If they select not to enroll, the value `FALSE` will be stored in `toopherAuthenticateLogon`, and the user will be logged in.
* **`toopherAuthenticateLogon` == `FALSE`, `toopherPairingId`=ANY** : The user has already elected not to participate in Toopher authentication.  They will be logged in.
* **`toopherAuthenticateLogon` == `TRUE`, `toopherPairingId` missing or empty** : The user will be prompted to pair their mobile device with their account.  The resulting Pairing ID is stored in `toopherPairingId`, and the user is authenticated through Toopher before being logged in.
* **`toopherAuthenticateLogon` == `TRUE`, `toopherPairingId` present** : The user has already paired their mobile device with their account, and they will be authenticated through Toopher before being logged in.

## Future Enhancements
This pilot demonstrates the core functionality of a Toopher-enhanced OpenAM authentication flow.  Several further improvements are in active development and will be available soon:

* Self-service Toopher pairing management (e.g., account recovery for when a user loses access to their second factor, etc.)
* Support for `ldaps://` protocol, with configurable support for self-signed certificates
* User-accessible configuration options
* Admin pairing management (e.g., allow admins to deactivate pairing, etc.)
* More dynamic Toopher authentication waiting room (currently the webpage periodically refreshes while Toopher authentication is performed)

### FAQ
#### What happens if users lose their mobile device?
Currently, the best solution is for an administrator to manually delete the `toopherAuthenticateLogon` and `toopherPairingId` for that user.  This will allow them to opt-out of Toopher authentication then next time they log in, or pair with a new device.  We are currently developing a self-service Pairing Recovery capability in Toopher for OpenAM which will reduce the administrative burden of this task, expected to be available in late 2013.

#### Can users authenticate if their mobile device is not connected to the network?
Yes, users can still authenticate with a One-Time Password by clicking on the "Authenticate with One-Time Password" button when logging in.  The Toopher mobile app can generate valid One-Time Passwords regardless of network connectivity.
