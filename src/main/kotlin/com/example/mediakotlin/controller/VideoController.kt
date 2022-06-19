package com.example.mediakotlin.controller

import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.File
import java.io.RandomAccessFile
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@RestController
@RequestMapping("/video")
class VideoController {
    private val log = LoggerFactory.getLogger(this.javaClass)

    @RequestMapping("/get")
    fun getVideo(request: HttpServletRequest, response: HttpServletResponse) {
        response.reset()
        val fileName = request.getParameter("fileName")
        val rangeField = request.getHeader("Range")
        log.info("fileName: {}, range: {}", fileName, rangeField)
        try {
            val outputStream = response.outputStream
            val resource = ClassPathResource(fileName)
            val file = File(resource.uri)
            if (file.exists()) {
                val targetFile = RandomAccessFile(file, "r")
                val fileLength = targetFile.length()

                if (rangeField != null) {
                    val rangeString: List<String> = rangeField.substring(rangeField.indexOf("=") + 1).split("-")

                    val rangeStart = rangeString[0].toLong()
                    val rangeEnd = rangeString[1].toLong()
                    response.setHeader(HttpHeaders.CONTENT_TYPE, "video/mp4")
                    response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes")
                    //设置此次相应返回的数据范围
                    var requestSize = rangeEnd - rangeStart + 1
                    if (rangeEnd < fileLength) {
                        response.setHeader(HttpHeaders.CONTENT_LENGTH, (rangeEnd - rangeStart + 1).toString())
                        response.setHeader(HttpHeaders.CONTENT_RANGE, "bytes $rangeStart-$rangeEnd/$fileLength")
                    } else {
                        val len = fileLength - rangeStart
                        response.setHeader(HttpHeaders.CONTENT_LENGTH, len.toString())
                        response.setHeader(HttpHeaders.CONTENT_RANGE, "bytes $0-${fileLength-1}/$fileLength")
                    }

                    //返回码需要为206，而不是200
                    response.status = HttpServletResponse.SC_PARTIAL_CONTENT
                    targetFile.seek(rangeStart)
                    val cache = ByteArray(4096)

                    while (requestSize > 0) {
                        val len = targetFile.read(cache)
                        if (requestSize < cache.size) {
                            outputStream.write(cache, 0, requestSize.toInt())
                        } else {
                            outputStream.write(cache, 0, len)
                            if (len < cache.size) {
                                break
                            }
                        }
                        requestSize -= cache.size
                    }
                } else {
                    response.setHeader(HttpHeaders.CONTENT_TYPE,"video/mp4")
                    response.setHeader(HttpHeaders.CONTENT_LENGTH,"$fileLength")
                    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "fileName=$fileName")

                    val cache = ByteArray(4096)
                    var flag = targetFile.read(cache)
                    while (flag != -1) {
                        outputStream.write(cache, 0 , flag)
                        flag = targetFile.read(cache)
                    }
                }


            } else {
                val message = "file: $fileName not exists"

                response.setHeader(HttpHeaders.CONTENT_TYPE,"application/json")
                outputStream.write(message.encodeToByteArray())
            }
            outputStream.flush()
            outputStream.close()

        } catch (e: Exception) {
//            e.printStackTrace()
        } finally {
            log.info("done")
        }
    }

    @RequestMapping("/hello")
    fun hello(request: HttpServletRequest): ResponseEntity<String> {
        return ResponseEntity(request.getParameter("fileName"), HttpStatus.OK)
    }
}