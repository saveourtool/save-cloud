package com.saveourtool.save.s3

import reactor.core.scheduler.BoundedElasticScheduler
import reactor.core.scheduler.Schedulers
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class S3Operations(

) {

    private val executor = ThreadPoolExecutor(
        Schedulers.DEFAULT_POOL_SIZE,
        Schedulers.DEFAULT_BOUNDED_ELASTIC_SIZE,
        BoundedElasticScheduler.DEFAULT_TTL_SECONDS,
        TimeUnit.SECONDS,
        LinkedBlockingQueue(Schedulers.DEFAULT_BOUNDED_ELASTIC_QUEUESIZE)
    )

    private val test = Executors.newFixedThreadPool(4)
}