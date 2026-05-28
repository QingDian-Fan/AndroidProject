package com.project.common.http.sse.ds;

import java.util.ArrayList;

public class DSSendData {
    public String model;
    public boolean stream;
    public ArrayList<DSSendMessage> messages;

    public DSSendData(String model, boolean stream, ArrayList<DSSendMessage> messages) {
        this.model = model;
        this.stream = stream;
        this.messages = messages;
    }
}
