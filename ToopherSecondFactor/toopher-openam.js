// vim: ts=2 sw=2 expandtab cindent
//addEventListener polyfill 1.0 / Eirik Backer / MIT Licence
(function (win, doc) {
  if (win.addEventListener) return; //No need to polyfill

  function docHijack(p) {
    var old = doc[p];
    doc[p] = function (v) {
      return addListen(old(v))
    }
  }

  function addEvent(on, fn, self) {
    return (self = this).attachEvent('on' + on, function (e) {
      var e = e || win.event;
      e.preventDefault = e.preventDefault ||
      function () {
        e.returnValue = false
      }
      e.stopPropagation = e.stopPropagation ||
      function () {
        e.cancelBubble = true
      }
      fn.call(self, e);
    });
  }

  function addListen(obj, i) {
    if (i = obj.length) while (i--) obj[i].addEventListener = addEvent;
    else obj.addEventListener = addEvent;
    return obj;
  }

  addListen([doc, win]);
  if ('Element' in win) win.Element.prototype.addEventListener = addEvent; //IE8
  else { //IE < 8
    doc.attachEvent('onreadystatechange', function () {
      addListen(doc.all)
    }); //Make sure we also init at domReady
    docHijack('getElementsByTagName');
    docHijack('getElementById');
    docHijack('createElement');
    addListen(doc.all);
  }
})(window, document);

(function (window, document) {
  var docCookies = {
    getItem: function (sKey) {
      return decodeURIComponent(document.cookie.replace(new RegExp("(?:(?:^|.*;)\\s*" + encodeURIComponent(sKey).replace(/[\-\.\+\*]/g, "\\$&") + "\\s*\\=\\s*([^;]*).*$)|^.*$"), "$1")) || null;
    },
    setItem: function (sKey, sValue, vEnd, sPath, sDomain, bSecure) {
      if (!sKey || /^(?:expires|max\-age|path|domain|secure)$/i.test(sKey)) {
        return false;
      }
      var sExpires = "";
      if (vEnd) {
        switch (vEnd.constructor) {
        case Number:
          sExpires = vEnd === Infinity ? "; expires=Fri, 31 Dec 9999 23:59:59 GMT" : "; max-age=" + vEnd;
          break;
        case String:
          sExpires = "; expires=" + vEnd;
          break;
        case Date:
          sExpires = "; expires=" + vEnd.toUTCString();
          break;
        }
      }
      document.cookie = encodeURIComponent(sKey) + "=" + encodeURIComponent(sValue) + sExpires + (sDomain ? "; domain=" + sDomain : "") + (sPath ? "; path=" + sPath : "") + (bSecure ? "; secure" : "");
      return true;
    },
    removeItem: function (sKey, sPath, sDomain) {
      if (!sKey || !this.hasItem(sKey)) {
        return false;
      }
      document.cookie = encodeURIComponent(sKey) + "=; expires=Thu, 01 Jan 1970 00:00:00 GMT" + (sDomain ? "; domain=" + sDomain : "") + (sPath ? "; path=" + sPath : "");
      return true;
    },
    hasItem: function (sKey) {
      return (new RegExp("(?:^|;\\s*)" + encodeURIComponent(sKey).replace(/[\-\.\+\*]/g, "\\$&") + "\\s*\\=")).test(document.cookie);
    }
  };

  function form2dict(form) {
    if (!form || form.nodeName !== "FORM") {
      return;
    }
    var result = {};
    for (var i = form.elements.length - 1; i >= 0; i = i - 1) {
      if (form.elements[i].name === "") {
        continue;
      }
      result[form.elements[i].name] = form.elements[i].value;
    }
    return result;
  }

  function serializeDict(dict) {
    var q = [];
    for (var key in dict) {
      if (dict.hasOwnProperty(key)) {
        q.push(key + '=' + encodeURIComponent(dict[key]));
      }
    }
    return q.join('&');
  }

  function ajaxPost(url, data, callback) {
    var xmlhttp = window.XMLHttpRequest ? new XMLHttpRequest() : new ActiveXObject("Microsoft.XMLHTTP");

    xmlhttp.onreadystatechange = function () {
      if (xmlhttp.readyState == 4 && xmlhttp.status == 200) {
        callback(xmlhttp.responseText);
      }
    }

    xmlhttp.open("POST", url, true);
    xmlhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
    xmlhttp.setRequestHeader("X-Requested-With", "XMLHttpRequest");
    xmlhttp.send(data);
  }

  function createDummyHiddenInputIfNecessary(form, inputName) {
    for (var i = 0; i < form.elements.length; i++) {
      if (form.elements[i].name === inputName) {
        return;  // if the element is already present, we don't want to add another copy
      }
    }

    var input = document.createElement('input');
    input.setAttribute('type', 'hidden');
    input.setAttribute('name', inputName);
    input.setAttribute('id', inputName);
    input.setAttribute('value', 'dummy');
    form.appendChild(input);
  }

  function poll(url, data) {
    ajaxPost(url, data, function (responseText) {
      var obj = JSON.parse(responseText);
      if (obj.poll) {
        setTimeout(function () {
          poll(url, data);
        }, 2000);
      } else {
        LoginSubmit('poll');
      }
    });
  }

  function startPolling(form) {
    poll(form.action, serializeDict(form2dict(form)));
  }

  function toopher_auth_manager() {
    var loginForm = null;
    for (var i = 0; i < document.forms.length; i++) {
      if (document.forms[i].name === 'Login') {
        loginForm = document.forms[i];
        break;
      }
    }
    if (loginForm) {
      createDummyHiddenInputIfNecessary(loginForm, 'IDToken0');
      createDummyHiddenInputIfNecessary(loginForm, 'IDToken1');
    }

    var toopherTerminalId = docCookies.getItem("toopher_terminal_id");
    if (toopherTerminalId === null) {
      docCookies.setItem('toopher_terminal_id', (Math.random() + 1).toString(36).substring(2), Infinity, '/', '', true);
    }

    var status = docCookies.getItem("toopher_auth_status");
    docCookies.removeItem("toopher_auth_status");
    if (status === 'poll') {
      startPolling(loginForm);
    }

  }

  window.addEventListener("load", toopher_auth_manager(), false);
})(window, document);
