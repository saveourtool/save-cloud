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
                translation: {
                    "Notifications": "Notifications"
                },
                "vulnerability-collection": $VULN_COLLECTION_EN,
                "table-headers": $TABLE_HEADERS_EN,
            },
            cn: {
                translation: {
                    "Notifications": "通知"
                }
            },
            ru: {
                translation: {
                    "Notifications": "Уведомления"
                },
                "vulnerability-collection": $VULN_COLLECTION_RU,
                "table-headers": $TABLE_HEADERS_RU,
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
