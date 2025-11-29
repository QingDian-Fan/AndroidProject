var demojs = {};
demojs.os = {};
demojs.os.isIOS = /iOS|iPhone|iPad|iPod/i.test(navigator.userAgent);
demojs.os.isAndroid = !demojs.os.isIOS;
demojs.callbacks = {}

demojs.callback = function (callbackname, response) {
   var callbackobject = demojs.callbacks[callbackname];
   console.log("xxxx"+callbackname);
   if (callbackobject !== undefined){
       if(callbackobject.callback != undefined){
          console.log("xxxxxx"+response);
            var ret = callbackobject.callback(response);
           if(ret === false){
               return
           }
           delete demojs.callbacks[callbackname];
       }
   }
}

demojs.takeNativeAction = function(commandname, parameters){
    console.log("demojs takenativeaction")
    var request = {};
    request.name = commandname;
    request.param = parameters;
    if(window.demojs.os.isAndroid){
        console.log("android take native action" + JSON.stringify(request));
        window.webview.takeNativeAction(JSON.stringify(request));
    } else {
        window.webkit.messageHandlers.webview.postMessage(JSON.stringify(request))
    }
}

demojs.takeNativeActionWithCallback = function(commandname, parameters, callback) {
    var callbackname = "nativetojs_callback_" +  (new Date()).getTime() + "_" + Math.floor(Math.random() * 10000);
    demojs.callbacks[callbackname] = {callback:callback};

    var request = {};
    request.name = commandname;
    request.param = parameters;
    request.param.callbackname = callbackname;
    if(window.demojs.os.isAndroid){
        window.webview.takeNativeAction(JSON.stringify(request));
    } else {
        window.webkit.messageHandlers.webview.postMessage(JSON.stringify(request))
    }
}

window.demojs = demojs;