import com.saveourtool.save.utils.toByteBufferFlux
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import org.springframework.core.io.Resource
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import reactor.core.publisher.Flux
import java.nio.ByteBuffer

private const val DEFAULT_BUFFER_SIZE = 4096

fun ByteReadChannel.toByteBufferFlux(): Flux<ByteBuffer> =
        DataBufferUtils.readInputStream(
            ::toInputStream,
            DefaultDataBufferFactory.sharedInstance,
            DEFAULT_BUFFER_SIZE,
        )
            .map {
                it.asByteBuffer()
            }
