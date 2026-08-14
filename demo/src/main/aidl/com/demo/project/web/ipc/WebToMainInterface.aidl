package com.demo.project.web.ipc;

import com.demo.project.web.ipc.MainToWebInterface;

/** WebView 进程将 H5 命令转发到主进程。 */
interface WebToMainInterface {
    void handleWebCommand(
        String commandName,
        String jsonParams,
        MainToWebInterface callback
    );
}
