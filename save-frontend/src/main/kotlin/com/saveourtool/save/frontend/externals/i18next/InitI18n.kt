/**
 * File containing initialization of i18next
 */

package com.saveourtool.save.frontend.externals.i18next

import com.saveourtool.save.frontend.externals.i18next.locales.*

/**
 * Function that encapsulates i18n initialization.
 *
 * @param isDebug flag to set debug mode
 * @param interpolationEscapeValue interpolation.escapeValue value
 */
@Suppress("UNUSED_PARAMETER")
fun initI18n(isDebug: Boolean = false, interpolationEscapeValue: Boolean = false) {
    js("""
        var i18n = require("i18next");
        var reactI18n = require("react-i18next");
        var resources = {
            en: {
                "vulnerability-collection": $VULN_COLLECTION_EN,
                "thanks-for-registration": $THANKS_FOR_REGISTRATION_EN,
                "vulnerability-upload": $VULN_UPLOAD_EN,
                "table-headers": $TABLE_HEADERS_EN,
                "topbar": $TOPBAR_EN,
                "cookies": $COOKIES_EN,
                "proposing": $PROPOSING_VULN_EN,
                "welcome": $WELCOME_EN,
                "organization": $ORGANIZATION_EN
            },
            cn: {
                "vulnerability-collection": $VULN_COLLECTION_CN,
                "thanks-for-registration": $THANKS_FOR_REGISTRATION_CN,
                "vulnerability-upload": $VULN_UPLOAD_CN,
                "table-headers": $TABLE_HEADERS_CN,
                "topbar": $TOPBAR_CN,
                "cookies": $COOKIES_CN,
                "proposing": $PROPOSING_VULN_CN,
                "welcome": $WELCOME_CN,
                "organization": $ORGANIZATION_CN
            },
            ru: {
                "vulnerability-collection": $VULN_COLLECTION_RU,
                "thanks-for-registration": $THANKS_FOR_REGISTRATION_RU,
                "vulnerability-upload": $VULN_UPLOAD_RU,
                "table-headers": $TABLE_HEADERS_RU,
                "topbar": $TOPBAR_RU,
                "cookies": $COOKIES_RU,
            }
        };
        
        i18n.use(reactI18n.initReactI18next).init({
            resources: resources,
            lng: "en",
            fallbackLng: "en",
            debug: isDebug,
            interpolation: {
                escapeValue: interpolationEscapeValue
            }
        }); 
    """)
}
