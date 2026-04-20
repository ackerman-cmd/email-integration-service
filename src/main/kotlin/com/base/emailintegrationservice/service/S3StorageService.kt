package com.base.emailintegrationservice.service

import com.base.emailintegrationservice.config.S3Properties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest

@Service
class S3StorageService(
    private val s3Client: S3Client,
    private val s3Properties: S3Properties,
) {
    private val log = LoggerFactory.getLogger(S3StorageService::class.java)

    fun upload(
        key: String,
        bytes: ByteArray,
        contentType: String,
    ): String {
        s3Client.putObject(
            PutObjectRequest
                .builder()
                .bucket(s3Properties.bucket)
                .key(key)
                .contentType(contentType)
                .contentLength(bytes.size.toLong())
                .build(),
            RequestBody.fromBytes(bytes),
        )
        log.debug("Uploaded to S3: bucket={} key={} size={}", s3Properties.bucket, key, bytes.size)
        return buildUrl(key)
    }

    fun download(key: String): ByteArray {
        val response = s3Client.getObject(
            GetObjectRequest
                .builder()
                .bucket(s3Properties.bucket)
                .key(key)
                .build(),
        )
        return response.readAllBytes()
    }

    fun buildUrl(key: String): String {
        val base = s3Properties.publicUrl.ifBlank { s3Properties.endpoint }
        return "$base/${s3Properties.bucket}/$key"
    }
}
