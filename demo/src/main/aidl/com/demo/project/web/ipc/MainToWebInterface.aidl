package com.demo.project.web.ipc;

/** 主进程执行完 H5 命令后，将结果回传给 WebView 进程。 */
interface MainToWebInterface {
    void onResult(String callbackName, String response);
}
