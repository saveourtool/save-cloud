package com.saveourtool.save.sandbox.controller

import com.saveourtool.save.orchestrator.config.ConfigProperties
import com.saveourtool.save.sandbox.controller.SandboxInternalController.Companion.DEBUG_INFO_FILE_NAME
import com.saveourtool.save.sandbox.storage.SandboxStorageKey
import com.saveourtool.save.sandbox.storage.SandboxStorageKeyType
import com.saveourtool.save.storage.Storage
import com.saveourtool.save.utils.overwrite
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.ByteBuffer

@RestController
@RequestMapping("/sandbox/api")
class SandboxController(
    val configProperties: ConfigProperties,
    val storage: Storage<SandboxStorageKey>,
) {
    /**
     * @param userName
     * @param file
     * @return count of written bytes
     */
    @PostMapping("/upload-file", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadFile(
        @RequestPart userName: String,
        @RequestPart file: Mono<FilePart>,
    ): Mono<Long> = doUpload(
        userName,
        SandboxStorageKeyType.FILE,
        file
    )

    /**
     * @param userName
     * @param file
     * @return count of written bytes
     */
    @PostMapping("/upload-test", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadTest(
        @RequestPart userName: String,
        @RequestPart file: Mono<FilePart>,
    ): Mono<Long> = doUpload(
        userName,
        SandboxStorageKeyType.TEST,
        file
    )

    private fun doUpload(
        userName: String,
        type: SandboxStorageKeyType,
        file: Mono<FilePart>,
    ): Mono<Long> = file.flatMap { filePart ->
        storage.overwrite(
            key = SandboxStorageKey(
                userName,
                type,
                filePart.filename()
            ),
            content = filePart
        )
    }

    /**
     * @param userName
     * @return [Mono] with content of DebugInfo
     * @throws ResponseStatusException if request is invalid or result cannot be returned
     */
    @Suppress("ThrowsCount", "UnsafeCallOnNullableType")
    @PostMapping(path = ["/get-debug-info"])
    fun getDebugInfo(
        @RequestParam userName: String,
    ): Flux<ByteBuffer> {
        val storageKey = SandboxStorageKey(
            userName,
            SandboxStorageKeyType.DEBUG_INFO,
            DEBUG_INFO_FILE_NAME,
        )
        return storage.download(storageKey)
    }
}
