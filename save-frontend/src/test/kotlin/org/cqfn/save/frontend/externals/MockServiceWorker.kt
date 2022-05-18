/**
 * https://github.com/mswjs/msw
 */

@file:JsModule("msw")
@file:JsNonModule

package org.cqfn.save.frontend.externals

external val rest: dynamic

/**
 * The setupWorker() function is used for client-side mocking. You usually create a worker instance and activate it by calling the start() method on the returned API.
 * Source: https://mswjs.io/docs/api/setup-worker
 */
external fun setupWorker(
    vararg requestHandlers: dynamic
): dynamic
