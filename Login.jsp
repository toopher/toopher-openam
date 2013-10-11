<%--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

   Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved

   The contents of this file are subject to the terms
   of the Common Development and Distribution License
   (the License). You may not use this file except in
   compliance with the License.

   You can obtain a copy of the License at
   https://opensso.dev.java.net/public/CDDLv1.0.html or
   opensso/legal/CDDLv1.0.txt
   See the License for the specific language governing
   permission and limitations under the License.

   When distributing Covered Code, include this CDDL
   Header Notice in each file and include the License file
   at opensso/legal/CDDLv1.0.txt.
   If applicable, add the following below the CDDL Header,
   with the fields enclosed by brackets [] replaced by
   your own identifying information:
   "Portions Copyrighted [year] [name of copyright owner]"

   $Id: Login.jsp,v 1.11 2009/01/09 07:13:21 bhavnab Exp $

--%>
<%--
   Portions Copyrighted 2012 ForgeRock Inc
--%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html xmlns="http://www.w3.org/1999/xhtml">
<%@page info="Login" language="java"%>
<%@taglib uri="/WEB-INF/jato.tld" prefix="jato"%>
<%@taglib uri="/WEB-INF/auth.tld" prefix="auth"%>
<jato:useViewBean
	className="com.sun.identity.authentication.UI.LoginViewBean">
	<%@page contentType="text/html"%>
	<head>
	<title><jato:text name="htmlTitle_Login" /></title>
	<%
                String ServiceURI = (String) viewBean.getDisplayFieldValue(viewBean.SERVICE_URI);
                String encoded = "false";
                String gotoURL = (String) viewBean.getValidatedInputURL(
                        request.getParameter("goto"), request.getParameter("encoded"), request);
                String gotoOnFailURL = (String) viewBean.getValidatedInputURL(
                        request.getParameter("gotoOnFail"), request.getParameter("encoded"), request);
                String encodedQueryParams = (String) viewBean.getEncodedQueryParams(request);
                if ((gotoURL != null) && (gotoURL.length() != 0)) {
                    encoded = "true";
                }
            %>
	<link href="<%= ServiceURI%>/css/new_style.css" rel="stylesheet"
		type="text/css" />
	<!--[if IE 9]> <link href="<%= ServiceURI%>/css/ie9.css" rel="stylesheet" type="text/css"> <![endif]-->
	<!--[if lte IE 7]> <link href="<%= ServiceURI%>/css/ie7.css" rel="stylesheet" type="text/css"> <![endif]-->
	<script language="JavaScript" src="<%= ServiceURI%>/js/auth.js"
		type="text/javascript"></script>
	<script type="text/javascript">

	function autoremove(){
		var arr_elms = [];
		arr_elms = document.body.getElementsByTagName("input");
		var elms_len = arr_elms.length;
		for (var i = 0; i < elms_len; i++) {
		  arr_elms[i].autocomplete="off"
			  }
	}
	setTimeout(autoremove(),30000)
            function createCookie(name,value,days) {
 			if (days) {
 				var date = new Date();
 				date.setTime(date.getTime()+(days*24*60*60*1000));
 				var expires = "; expires="+date.toGMTString();
 			}
 			else var expires = "";
 			document.cookie = name+"="+escape(value)+expires+"; path=/";
   		 }

    	function readCookie(name) {
    			var nameEQ = name + "=";
    			var ca = document.cookie.split(';');
    			for(var i=0;i < ca.length;i++) {
    				var c = ca[i];
    				while (c.charAt(0)==' ') c = c.substring(1,c.length);
    				if (c.indexOf(nameEQ) == 0) return c.substring(nameEQ.length,c.length);
    			}
    			return null;
    	}
        function checkCookie(_username) {
 		   var terminal_id = readCookie(_username+"toopher_app");
 		  if (terminal_id==null || terminal_id==undefined || terminal_id == ""){
		      terminal_id="tooper_app"+new Date().getTime();
              createCookie(_username+"toopher_app", terminal_id, 30);
              document.getElementById('terminalId').value=terminal_id;
		    }else{
		    	document.getElementById('terminalId').value=terminal_id;
		    	LoginSubmit('Log In'); 
			}
        }
		
            </script>

	<jato:content name="validContent">
		<script language="JavaScript" type="text/javascript">
                    <!--
                    var defaultBtn = 'Submit';
                    var elmCount = 0;

                    /**
                     * submit form with given button value
                     *
                     * @param value of button
                     */
                    function LoginSubmit(value) {
					document.getElementById('spinner').style.display = 'block';
                        aggSubmit();
                        var hiddenFrm = document.forms['Login'];
        
                        if (hiddenFrm != null) {
                            hiddenFrm.elements['IDButton'].value = value;
                            if (this.submitted) {
                                alert("The request is currently being processed");
                            }
                            else {
                                this.submitted = true;
                                hiddenFrm.submit();
                            }
                        }
                    }
                    -->
                </script>
	</jato:content>
	</head>
	<body onload="placeCursorOnFirstElm();autoremove();">
	<div id="spinner" class="spinner" style="display:none;width:100%;height: 100%;position: fixed;z-index: 10000;background-color: #fff;opacity:0.5;">
		<img src="<%=request.getContextPath()%>/images/download.jpg" alt="Loading....." style="position: absolute;margin: auto;left:0;right: 0;top: 0;bottom: 0;z-index:10001;"/>
	</div>
	<div class="container_12">
	<div class="grid_4 suffix_8"><a class="logo"
		href="<%= ServiceURI%>"></a></div>
	<div class="box clear-float">
	<div class="grid_3">
	<div class="product-logo"></div>
	</div>
	<div class="grid_9 left-seperator">
	<div class="box-content clear-float"><jato:content
		name="ContentStaticTextHeader">
		<h1><jato:getDisplayFieldValue name='StaticTextHeader'
			defaultValue='Authentication' fireDisplayEvents='true' escape='false' /></h1>
	</jato:content> <jato:content name="validContent">
		<auth:form name="Login" method="post"
			defaultCommandChild="DefaultLoginURL">
			<jato:tiledView name="tiledCallbacks"
				type="com.sun.identity.authentication.UI.CallBackTiledView">
				<script language="javascript" type="text/javascript">
                                            <!--
                                            elmCount++;
                                            -->
                                        </script>
				<jato:content name="textBox">
					<div class="row"><label
						for="IDToken<jato:text name="txtIndex" />"> <jato:text
						name="txtPrompt" defaultValue="User name:" escape="false" /> <jato:content
						name="isRequired">
						<img src="<%= ServiceURI %>/images/required.gif"
							alt="Required Field" title="Required Field" width="7" height="14" />
					</jato:content> </label> <input class="textbox" type="text"
						name="IDToken<jato:text name="txtIndex" />"
						id="IDToken<jato:text name="txtIndex" />"
						value="<jato:text name="txtValue" />" /></div>
				</jato:content>
				<jato:content name="password">
					<div class="row"><label
						for="IDToken<jato:text name="txtIndex" />"> <jato:text
						name="txtPrompt" defaultValue="Password:" escape="false" /> <jato:content
						name="isRequired">
						<img src="<%= ServiceURI %>/images/required.gif"
							alt="Required Field" title="Required Field" width="7" height="14" />
					</jato:content> </label> <input class="textbox" type="password"
						name="IDToken<jato:text name="txtIndex" />"
						id="IDToken<jato:text name="txtIndex" />" value="" /></div>
				</jato:content>
				<jato:content name="choice">
					<div class="row"><label
						for="IDToken<jato:text name="txtIndex" />"> <jato:text
						name="txtPrompt" defaultValue="RadioButton:" escape="false" /> <jato:content
						name="isRequired">
						<img src="<%= ServiceURI %>/images/required.gif"
							alt="Required Field" title="Required Field" width="7" height="14" />
					</jato:content> </label>
					<div class="radios"><jato:tiledView name="tiledChoices"
						type="com.sun.identity.authentication.UI.CallBackChoiceTiledView">
						<jato:content name="selectedChoice">
							<input type="radio"
								name="IDToken<jato:text name="txtParentIndex" />"
								id="IDToken<jato:text name="txtIndex" />"
								value="<jato:text name="txtIndex" />" checked="checked" />
							<label for="IDToken<jato:text name="txtIndex" />"> <jato:text
								name="txtChoice" /> </label>
						</jato:content>

						<jato:content name="unselectedChoice">
							<input type="radio"
								name="IDToken<jato:text name="txtParentIndex" />"
								id="IDToken<jato:text name="txtIndex" />"
								value="<jato:text name="txtIndex" />" />
							<label for="IDToken<jato:text name="txtIndex" />"> <jato:text
								name="txtChoice" /> </label>
						</jato:content>
					</jato:tiledView></div>
					</div>
				</jato:content>
			</jato:tiledView>

			<jato:content name="ContentStaticTextResult">
				<!-- after login output message -->
				<p><b><jato:getDisplayFieldValue name='StaticTextResult'
					defaultValue='' fireDisplayEvents='true' escape='false' /></b></p>
			</jato:content>
			<jato:content name="ContentHref">
				<!-- URL back to Login page -->
				<p><auth:href name="LoginURL" fireDisplayEvents='true'>
					<jato:text name="txtGotoLoginAfterFail" />
				</auth:href></p>
			</jato:content>
			<jato:content name="ContentImage">
				<!-- customized image defined in properties file -->
				<p><img name="IDImage"
					src="<jato:getDisplayFieldValue name='Image'/>" alt="" /></p>
			</jato:content>

			<jato:content name="ContentButtonLogin">
				<fieldset><jato:content name="hasButton">
					<div class="row"><jato:tiledView name="tiledButtons"
						type="com.sun.identity.authentication.UI.ButtonTiledView">
						<input name="Login.Submit" type="button"
							onclick="LoginSubmit('<jato:text name="txtButton" />'); return false;"
							class="button" value="<jato:text name="txtButton" />" />
					</jato:tiledView></div>
					<script language="javascript" type="text/javascript">
                                                    <!--
                                                    defaultBtn = '<jato:text name="defaultBtn" />';
                                                    var inputs = document.getElementsByTagName('input');
                                                    for (var i = 0; i < inputs.length; i ++) {
                                                        if (inputs[i].type == 'button' && inputs[i].value == defaultBtn) {
                                                            inputs[i].setAttribute("class", "button primary");;
                                                            break;
                                                        }
                                                    }
                                                    -->
                                                </script>
				</jato:content> <jato:content name="hasNoButton">
					<div class="row"><input name="Login.Submit" type="submit"
						onclick="LoginSubmit('<jato:text name="lblSubmit" />'); return false;"
						class="button primary" value="<jato:text name="lblSubmit" />" />
					</div>
				</jato:content></fieldset>
			</jato:content>
			<script language="javascript" type="text/javascript">
                                        <!--
                                        if (elmCount != null) {
                                            document.write("<input name=\"IDButton"  + "\" type=\"hidden\">");
                                        }
                                        -->
                                    </script>
			<input type="hidden" name="goto" value="<%= gotoURL%>" />
			<input type="hidden" name="gotoOnFail" value="<%= gotoOnFailURL%>" />
			<input type="hidden" name="SunQueryParamsString"
				value="<%= encodedQueryParams%>" />
			<input type="hidden" name="encoded" value="<%= encoded%>" />
			<input id="terminalId" type="hidden" size="36" name="terminalId" autocomplete='off' value="" />
			<%
			if(request.getAttribute("isSecond")!=null && (Boolean)request.getAttribute("isSecond")){%>
				<script language="javascript">
					window.onload = function(e)
						{
							document.getElementById('IDToken1').value="<%=request.getAttribute("username")%>";
							document.getElementById("IDToken1").disabled = true;
							document.getElementById('IDToken2').setAttribute('type','Text');
							document.getElementById('IDToken2').setAttribute('autocomplete','off');
							var arr_elms = [];
							arr_elms = document.body.getElementsByTagName("label");
							var elms_len = arr_elms.length;
							for (var i = 0; i < elms_len; i++) {
							  if(arr_elms[i].getAttribute("for") != null && arr_elms[i].getAttribute("for")=="IDToken2"){
										<%
									if(request.getAttribute("haspairingId")!=null && (Boolean)request.getAttribute("haspairingId")){
										if((Boolean)request.getAttribute("OTPS")){
											%>
												arr_elms[i].innerHTML="One Time Password <img src='/openam/images/required.gif' alt='Required Field' title='Required Field' width='7' height='14'>"
												 document.getElementById('terminalId').value="OTP-RE"
													 document.getElementById("pairing").style.display="";
											<%
										}else if(request.getAttribute("question")!=null){
											%>
											arr_elms[i].innerHTML="Answer <img src='/openam/images/required.gif' alt='Required Field' title='Required Field' width='7' height='14'>";
											document.getElementsByClassName('row')[0].style.display = 'none';
											var aTag = document.createElement('div');
											aTag.className ="box-content clear-float"
											aTag.innerHTML = "<h3 style='margin-bottom: 10px;'>Security questions :<%=request.getAttribute("question")%></h3>";
											document.forms['Login'].insertBefore(aTag,document.forms['Login'].firstChild);
											<%
										} else{
											%>
											   arr_elms[i].innerHTML="Terminal Name";
								               checkCookie("<%=request.getAttribute("username")%>");
								               document.getElementById("pairing").style.display="";
											<%
										}
									}else{
										%>
											arr_elms[i].innerHTML="Pairing Phrase <img src='/openam/images/required.gif' alt='Required Field' title='Required Field' width='7' height='14'>"
											var aTag = document.createElement('div');
											aTag.className ="row"
											aTag.innerHTML = "<label style='margin-bottom: 10px;'>Security questions<img src='/openam/images/required.gif' alt='Required Field' title='Required Field' width='7' height='14'></label><input class='textbox' type='text' name='question' id='question' autocomplete='off' value=''>";
											document.forms['Login'].insertBefore(aTag,document.body.getElementsByTagName('fieldset')[0]);
											var aTag1 = document.createElement('div');
											aTag1.className ="row"
											aTag1.innerHTML = "<label style='margin-bottom: 10px;'>Security Answer <img src='/openam/images/required.gif' alt='Required Field' title='Required Field' width='7' height='14'></label><input class='textbox' type='text' name='answer' autocomplete='off' id='answer' value=''>";
											document.forms['Login'].insertBefore(aTag1,document.body.getElementsByTagName('fieldset')[0]);
										<%
									}
										%>
								}
										
							}
						}
				</script>
			<%
			}
            %>
            <div class="row" id="pairing" style="display: none;"> <a href='http://demo.oneappcloud.com:8082/openam/UI/Login?service=recoverPairing' style=" float: right;
    color: red; font-size: 15px;"> Click here to recover pairing</a></div>
		</auth:form>
	</jato:content></div>
	</div>
	</div>
	<div class="footer alt-color">
	<div class="grid_6 suffix_3">
	<p><auth:resBundle bundleName="amAuthUI"
		resourceKey="copyright.notice" /></p>
	</div>
	</div>
	</div>
	</body>
</jato:useViewBean>
</html>
