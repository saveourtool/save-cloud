package com.saveourtool.save.frontend.components.basic.uploader

import com.saveourtool.save.domain.FileInfo
import com.saveourtool.save.frontend.components.basic.fileIconWithMode
import com.saveourtool.save.frontend.externals.*

import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLSpanElement
import react.FC
import react.Props
import react.create
import react.useState

import kotlin.js.Promise
import kotlin.test.Test
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class FileIconTest {
    private val fileInfoForTest = FileInfo(
        name = "Test file",
        uploadedMillis = LocalDateTime(2022, 7, 14, 12, 28).toInstant(TimeZone.UTC).toEpochMilliseconds(),
        sizeBytes = 42000,
        isExecutable = false,
    )

    @Test
    fun shouldRenderFileIconAndChangeTextOnClick(): Promise<HTMLElement> {
        val wrapper: FC<Props> = FC {
            // required for correct test execution; `require` is called higher in the real code.
            js("require(\"popper.js\")")
            js("require(\"bootstrap\")")

            val (fileInfo, setFileInfo) = useState(fileInfoForTest)
            fileIconWithMode {
                this.fileInfo = fileInfo
                onExecutableChange = { file, isChecked ->
                    setFileInfo(
                        file.copy(isExecutable = isChecked)
                    )
                }
            }
        }

        render(
            wrapper.create()
        )

        val span: HTMLSpanElement = screen.getByTextAndCast("file")
        val outerSpan = span.parentElement!!.closest("span")

        userEvent.click(outerSpan)

        // text should have changed after click: `file` -> `exe`
        return screen.findByTextAndCast<HTMLSpanElement>("exe")
    }
}
