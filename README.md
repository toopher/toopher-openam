Installing and Configuring the Toopher for OpenAM Integration
=============================================================

## Overview
### Document Scope
This guide details everything needed to integrate Toopher Two-Factor Authentication with OpenAM, a popular open-source
Authentication, Authorization, Entitlement, and Federation solution.  Readers should already be familiar with 
installation and administration for OpenAM, as well as be comfortable with general Linux
administration tasks.



### Compatibility Notes
These materials were primarily developed on the following environment, and some of the commands referenced may
be specific to these vendors.  If you would like us to provide assistance with installation on a different 
environment, please contact us at <support@toopher.com>:

    OS                : Centos 6.5 (2.6.32-431.5.1.el6)
    OpenLDAP          : 2.4.23
    OpenAM            : OpenAM 11.x
    Servlet Container : Apache Tomcat 6.0.24 (June 20 2013 1452)
    JVM Version       : 1.6.0_17-b04
    JVM Vendor        : Sun Microsystems Inc.

Other configurations are likely to work without issue, but have not been specifically tested at Toopher.

## Preparing the System
### Extract files from archive
All the required files are included in the tarball we provided.  Extract the files into a directory of your choice as usual:

    tar xzvf toopher-openam.tgz
    
The commands given below assume your working directory is the same as that used when extracting the tarball (unless otherwise described)
    
### Copy the Installation Files to OpenAM
All executable and configuration files must be copied to the proper spot in the OpenAM installation.  The paths under the `/openam` directory correspond to the default layout of the OpenAM .WAR file:

    sudo cp -r openam/* ${CATALINA_HOME}/webapps/openam/

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

## Realm Configuration
### Add a Toopher module instance for specific realms
From the [OpenAM Administration Page](http://SERVERNAME:PORT/openam/task/Home), navigate to the realm's Authentication setup page (Click the `Access Control` tab, click on realm you want to modify, then select `Authentication`).  In the `Module Instances` section, click `New` to add the Toopher module.  When prompted, name the module "ToopherSecondFactor", set the type to `Toopher Two-Factor Security`, and click `OK`.


### Configure the Toopher Module Instance
If you have not done so already, provision a new Toopher Consumer Key/Secret pair for the OpenAM server by visiting <https://dev.toopher.com> and creating a new requester.

Click the `ToopherSecondFactor` entry in the `Module Instances` list to go to the module options page.  Fill in appropriate values for all fields:

* **Allow Users to Opt-Out of Toopher Authentication** : If enabled, individual users will be able to disable Toopher Authentication when they log in.  This has the effect that the `ToopherSecondFactor` module instance will immedately grant access to the user
* **User email attribute** : Name of the User Principal attribute that holds a valid email address for the user.  The Toopher Two-Factor ID authentication module uses this address to send self-service Pairing Reset emails to the user *(not yet implemented)*
* **Authentication Level** : Authentication Level to be reported to OpenAM
* **Toopher API URL** : URL of the Toopher Web API.  This should be `https://api.toopher.com/v1/` for most users.
* **Toopher Consumer Key**,  **Toopher Consumer Secret** : The Toopher Consumer Key/Secret pair for your requester, generated by <https://dev.toopher.com>

### Create an Authentication Chain with Toopher
Once `ToopherSecondFactor` appears in the `Module Instances` list, create a new Authentication Chain by clicking `New` in the `Authentication Chaining` section.  Name the chain `ToopherAuth`, and click `OK`.

Add the `LDAP` module to the chain, with Criteria set to `REQUISITE`.

Add `ToopherSecondFactor` to the chain, with Criteria set to `REQUIRED`.  

Configure the rest of the realm settings as fits your needs.

Congratulations - You're done!

## Administering a Toopher-Enabled Userbase
### The Toopher Two-Factor Login Flow

1. The first time OpenAM users authenticate through the Toopher service, they will be given the chance to opt-out of Toopher Authentication if the **Allow Users to Opt-Out of Toopher** setting is enabled.
1. If the user has opted-out of Toopher Authentication, they will be immediately authenticated successfully by the Toopher Authentication Service
1. If the user has not paired a mobile device with their OpenAM profile, they will be prompted to enter a "pairing phrase" generated by the Toopher Mobile App (Available for Android and iOS through the respective app store)
1. If the user has never logged in from the current terminal, they will be prompted to assign a "Friendly Name" to the terminal.  Terminals are identified by setting a secure cookie in the browser.
1. The user is prompted via push message on their mobile device to authenticate the login.  If sufficient location information is available, the user is given the option to automatically allow future logins from that specific terminal when the mobile device is in that location

## Future Enhancements
This pilot demonstrates the core functionality of a Toopher-enhanced OpenAM authentication flow.  Several further improvements are in active development and will be available soon:

* Self-service Toopher pairing management (e.g., account recovery for when a user loses access to their second factor, etc.)
* User-accessible configuration options
* Admin pairing management (e.g., allow admins to deactivate pairing, etc.)
* More dynamic Toopher authentication waiting room (currently the webpage periodically refreshes while Toopher authentication is performed)

### FAQ
#### How can users un-pair their mobile device from Toopher?
Users can delete the pairing from their mobile device by tapping on the pairing in the Toopher mobile app, then selecting "Remove Pairing".  The next time they authenticate through OpenAM, the user will be prompted to re-pair their account with a mobile device.  If the **Allow Users to Opt-Out of Toopher** option is `enabled`, the user will first be given the opportunity to disable Toopher authentication on their account.

#### Can users authenticate if their mobile device is not connected to the network?
Yes, users can still authenticate with a One-Time Password by clicking on the "Authenticate with
One-Time Password" button when logging in.  The Toopher mobile app can generate valid One-Time
Passwords regardless of network connectivity.

#### What happens if users lose their mobile device, or delete the Toopher app?
Currently, this situation requires an administrator to manually reset the user's Toopher Pairing
status by running `reset_user.py` script, available in the `tools` directory of the installation archive.
`reset_user.py` requires access to the same Toopher Consumer Key and Secret used to configure the Toopher
OpenAM module.  There are two ways to supply these credentials to the script:

1. Set the `TOOPHER_CONSUMER_KEY` and `TOOPHER_CONSUMER_SECRET` environment variables *(preferred)*
1. Manually enter them when running the script - `reset_user.py` will prompt for the key and secret
if they are not available in environment variables

Whichever method is chosen for setting the Toopher API Credentials, `reset_user.py` takes a single
command-line argument: the UID for the OpenAM user needing to be reset.

    # reset the Toopher status of a user with uid `johndoe`.
    # Assumes that TOOPHER_CONSUMER_KEY and TOOPHER_CONSUMER_SECRET environment variables are set

    python tools/reset_user.py johndoe

We are currently developing a self-service Pairing
Recovery capability in Toopher for OpenAM which will reduce the administrative burden of this task,
expected to be available in late 2013.
