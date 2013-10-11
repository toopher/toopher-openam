/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2011 ForgeRock AS. All Rights Reserved
*
* The contents of this file are subject to the terms
* of the Common Development and Distribution License
* (the License). You may not use this file except in
* compliance with the License.
*
* You can obtain a copy of the License at
* http://forgerock.org/license/CDDLv1.0.html
* See the License for the specific language governing
* permission and limitations under the License.
*
* When distributing Covered Code, include this CDDL
* Header Notice in each file and include the License file
* at http://forgerock.org/license/CDDLv1.0.html
* If applicable, add the following below the CDDL Header,
* with the fields enclosed by brackets [] replaced by
* your own identifying information:
* "Portions Copyrighted [year] [name of copyright owner]"
*
*/

package com.forgerock.openam.examples;

import java.io.Serializable;
import java.security.Principal;

/**
*
* @author Steve Ferris steve.ferris@forgerock.com
*/
public class PairingRecoveryPrincipal implements Principal, Serializable {
    private String name;
    private final static String CLASSNAME = "PairingRecoveryPrincipal";
    private final static String COLON = " : ";
    
    public PairingRecoveryPrincipal(String name) {
        if (name == null) {
            throw new NullPointerException("illegal null input");
        }
        
        this.name = name;
    }
    
    /**
    * Return the LDAP username for this &lt;code&gt; PairingRecoveryPrincipal &lt;/code&gt;.
    *
    * <p>
    *
    * @return the LDAP username for this &lt;code&gt; PairingRecoveryPrincipal &lt;/code&gt;
    */
    public String getName() {
        return name;
    }
    
    /**
    * Return a string representation of this &lt;code&gt; PairingRecoveryPrincipal &lt;/code&gt;.
    *
    * <p>
    *
    * @return a string representation of this &lt;code&gt;TestAuthModulePrincipal&lt;/code&gt;.
    */
    @Override
    public String toString() {
        return new StringBuilder().append(CLASSNAME).append(COLON).append(name).toString();
    }
    
    /**
    * Compares the specified Object with this &lt;code&gt; PairingRecoveryPrincipal &lt;/code&gt;
    * for equality.  Returns true if the given object is also a
    * &lt;code&gt; PairingRecoveryPrincipal &lt;/code&gt; and the two PairingRecoveryPrincipal
    * have the same username.
    *
    * <p>
    *
    * @param o Object to be compared for equality with this
    *      &lt;code&gt; PairingRecoveryPrincipal &lt;/code&gt;.
    *
    * @return true if the specified Object is equal equal to this
    *      &lt;code&gt; PairingRecoveryPrincipal &lt;/code&gt;.
    */
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        
        if (this == o) {
            return true;
        }
        
        if (!(o instanceof PairingRecoveryPrincipal)) {
            return false;
        }
        PairingRecoveryPrincipal that = (PairingRecoveryPrincipal) o;
        
        if (this.getName().equals(that.getName())) {
            return true;
        }
        return false;
    }
    
    /**
    * Return a hash code for this &lt;code&gt; SampleAuthPrincipal &lt;/code&gt;.
    *
    * <p>
    *
    * @return a hash code for this &lt;code&gt; SampleAuthPrincipal &lt;/code&gt;.
    */
    @Override
    public int hashCode() {
        return name.hashCode();
    }
}