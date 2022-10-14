package com.saveourtool.save.frontend.components.basic.codeeditor

import com.saveourtool.save.frontend.externals.reactace.AceModes

/**
 * @property prettyName displayed name
 * @property editorMode highlight mode that should be enabled, if null, mode can be chosen using selector
 */
enum class FileType(val prettyName: String, val editorMode: AceModes?) {
    CODE("code", null),
    SAVE_TOML("save.toml", AceModes.TOML),
    SETUP_SH("setup.sh", AceModes.SHELL),
    ;
    companion object {
        /**
         * @param fileType [FileType]
         * @param optionForCode will be returned if [fileType] is CODE
         * @param optionForSaveToml will be returned if [fileType] is SAVE_TOML
         * @param optionForSetupSh will be returned if [fileType] is SETUP_SH
         * @return option corresponding to [fileType]
         */
        fun <T> getTypedOption(
            fileType: FileType,
            optionForCode: T,
            optionForSaveToml: T,
            optionForSetupSh: T,
        ) = when (fileType) {
            CODE -> optionForCode
            SAVE_TOML -> optionForSaveToml
            SETUP_SH -> optionForSetupSh
        }

        /**
         * @param fileType [FileType]
         * @param optionForCode will be returned if [fileType] is CODE
         * @param optionForSaveToml will be returned if [fileType] is SAVE_TOML
         * @param optionForSetupSh will be returned if [fileType] is SETUP_SH
         * @return option corresponding to [fileType] or null if [fileType] is null
         */
        fun <T> getTypedOption(
            fileType: FileType?,
            optionForCode: T,
            optionForSaveToml: T,
            optionForSetupSh: T,
        ) = fileType?.let { getTypedOption(it, optionForCode, optionForSaveToml, optionForSetupSh) }
    }
}
