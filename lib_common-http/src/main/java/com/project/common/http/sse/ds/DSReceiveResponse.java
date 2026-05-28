package com.project.common.http.sse.ds;

import java.util.ArrayList;

/**
 * {
 *     "id": "ec8772cd-08bd-4c77-9a3c-366bde2d08db",
 *     "object": "chat.completion.chunk",
 *     "created": 1746523126,
 *     "model": "deepseek-chat",
 *     "system_fingerprint": "fp_8802369eaa_prod0425fp8",
 *     "choices": [
 *         {
 *             "index": 0,
 *             "delta": {
 *                 "content": "帮你"
 *             },
 *             "logprobs": null,
 *             "finish_reason": null
 *         }
 *     ]
 * }
 */
public class DSReceiveResponse {
    public String id;
    public String object;
    public String created;
    public String model;
    public String system_fingerprint;
    public ArrayList<DSReceiveChoices> choices;

}
