package com.saveourtool.save.frontend.common.components.modal

import com.saveourtool.save.frontend.common.utils.WindowOpenness

/**
 * The dialog window.
 *
 * @property strings the string labels of a dialog window.
 * @property window the window itself.
 */
internal data class ModalDialog(
    val strings: ModalDialogStrings,
    val window: WindowOpenness,
)
