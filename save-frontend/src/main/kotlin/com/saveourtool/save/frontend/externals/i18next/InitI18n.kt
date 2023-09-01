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
                    "Notifications": "Notifications",
                    "Propose a new vulnerability": "Propose a new vulnerability",
                    "Short description": "Short description",
                    "Description": "Description",
                    "Original vulnerability identifier": "Original vulnerability identifier",
                    "Related link": "Related link",
                    "Language": "Language",
                    "Criticality": "Criticality",
                    "Organization": "Organization",
                    "Collaborators": "Collaborators",
                    "Affected projects:": "Affected projects:",
                    "Propose a vulnerability": "Propose a vulnerability",
                    "Project type: ": "Project type: ",
                    "Versions": "Versions",
                    "Sign in with": "Sign in with",
                    "Don't have an account?": "Don't have an account?",
                    "with limited functionality": "with limited functionality"
                },
                "vulnerability-collection": $VULN_COLLECTION_EN,
                "thanks-for-registration": $THANKS_FOR_REGISTRATION_EN,
                "vulnerability-upload": $VULN_UPLOAD_EN,
                "table-headers": $TABLE_HEADERS_EN,
                "topbar": $TOPBAR_EN,
                "cookies": $COOKIES_EN,
            },
            cn: {
                translation: {
                    "Notifications": "通知",
                    "Propose a new vulnerability": "创建一个新漏洞报告",
                    "Short description": "漏洞简介",
                    "Description": "漏洞描述",
                    "Original vulnerability identifier": "原始漏洞ID",
                    "Related link": "相关链接",
                    "Language": "语言",
                    "Criticality": "危险程度",
                    "Organization": "组织",
                    "Collaborators": "合作者",
                    "Affected projects:": "受影响项目",
                    "Propose a vulnerability": "提交",
                    "Project type: ": "项目类型",
                    "Versions": "版本范围",
                    "Sign in with": "登录方式",
                    "Don't have an account?": "没有账户?",
                    "with limited functionality": "部分功能使用受限"
                }
            },
            ru: {
                translation: {
                    "Notifications": "Уведомления"
                },
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
