// WebToMainInterface.aidl
package com.dian.demo;

// Declare any non-default types here with import statements
import com.dian.demo.MainToWebInterface;

interface WebToMainInterface {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void handleWebCommand(String commandName, String jsonParams, in MainToWebInterface callback);
}
