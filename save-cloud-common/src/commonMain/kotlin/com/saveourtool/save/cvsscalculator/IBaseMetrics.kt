@file:Suppress("FILE_NAME_INCORRECT")

package com.saveourtool.save.cvsscalculator

/**
 * base interface for all BaseMetrics classes
 */
@Suppress("CLASS_NAME_INCORRECT")
interface IBaseMetrics {
    /**
     * Version of CVSS
     **/
    var version: CvssVersion

    /**
     * @return true if BaseMetrics is valid, false otherwise
     */
    fun isValid(): Boolean

    /**
     * @return severity score vector
     */
    fun scoreVectorString(): String
}
