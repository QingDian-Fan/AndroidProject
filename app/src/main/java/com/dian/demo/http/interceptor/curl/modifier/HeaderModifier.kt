package com.dian.demo.http.interceptor.curl.modifier

import com.dian.demo.http.interceptor.curl.Header

/**
 * HeaderModifier allow for changing header name/value before creating curl log
 */
interface HeaderModifier {
    /**
     * @param header the header to check
     * @return true if header should be modified and false otherwise.
     */
    fun matches(header: Header): Boolean

    /**
     * @param header the header to modify
     * @return modified header or null to omit header in curl log
     */
    fun modify(header: Header): Header?
}