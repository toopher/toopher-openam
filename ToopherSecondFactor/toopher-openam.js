var toopherOpenAM = (function (window, document, $) {

  var formInputElement = null;
  var loginForm = null;

  var serializeDict = function(dict) {
    var key;
    var q = [];
    for (key in dict) {
      if (dict.hasOwnProperty(key)) {
        q.push(key + '=' + encodeURIComponent(dict[key]));
      }
    }
    return q.join('&');
  };

  var getToopherFormInputElement = function() {
    if (!formInputElement) {
      var formInputElementLabel = $('label:contains("#TOOPHER_HIDE#")');
      var inputElementId = formInputElementLabel.attr("for");
      formInputElement = $('#' + inputElementId);
    }
    return formInputElement;
  };

  var getLoginForm = function() {
    if (!loginForm) {
      var formEls = getToopherFormInputElement().parents('form');
      if (formEls.length) {
        loginForm = $(formEls[0]);
      }
    }
    return loginForm;
  };

  var getOrCreateIframeTargetElement = function() {
    // see if there is an already-created element we should use
    var explicitTarget = document.getElementById('toopher-iframe');
    if (explicitTarget) {
      return explicitTarget;
    }

    // nope - need to create our own
    var iframeEl = $("<iframe id='toopher-iframe'></iframe>");
    iframeEl.css("width", "100%");
    iframeEl.css("height", "300px");
    iframeEl.css("visibility", "none");
    getLoginForm().before(iframeEl);
    return iframeEl;
  };

  var init = function(iframeSrcUrl) {
    if (!iframeSrcUrl) {
      // no iframeSrcUrl == not our turn
      return;
    }
    var form = getLoginForm();
    form.css("visibility", "hidden");
    var iframeEl = getOrCreateIframeTargetElement();
    iframeEl.attr("src", iframeSrcUrl);
    iframeEl.css("visibility", "inline-block");
  };

  var handleMessage = function(e){
    var msgData = JSON.parse(e.data);
    if (msgData.status === 'toopher-api-complete') {
      getToopherFormInputElement().attr("value", serializeDict(msgData.payload));
      getLoginForm().submit();
    }
  };

  if (window.addEventListener) {
    window.addEventListener('message', handleMessage, false);
  } else {
    window.attachEvent('onmessage', handleMessage);
  }

  var exports = {
    init : init
  };
  return exports;
})(window, document, jQuery);

