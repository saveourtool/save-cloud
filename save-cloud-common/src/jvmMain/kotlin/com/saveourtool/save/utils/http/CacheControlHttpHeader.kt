package com.saveourtool.save.utils.http

import org.springframework.http.HttpHeaders

/**
 * HttpHeader for [HttpHeaders.CACHE_CONTROL]
 */
object CacheControlHttpHeader : HttpHeader {
    override val name: String = HttpHeaders.CACHE_CONTROL

    /**
     * [`no-transform`](https://www.rfc-editor.org/rfc/rfc7234#section-5.2.2.4)
     * is absolutely necessary, so that SSE stream passes through the
     * [proxy](https://github.com/chimurai/http-proxy-middleware) without
     * [compression](https://github.com/expressjs/compression).
     *
     * Otherwise, the front-end receives all the events at once, and only
     * after the response body is fully written.
     *
     * See
     * [this comment](https://github.com/facebook/create-react-app/issues/7847#issuecomment-544715338)
     * for details:
     *
     * The rest of the `Cache-Control` header is merely what _Spring_ sets by default.
     */
    override val value: String = arrayOf(
        "no-cache",
        "no-store",
        "no-transform",
        "max-age=0",
        "must-revalidate",
    ).joinToString()
}
