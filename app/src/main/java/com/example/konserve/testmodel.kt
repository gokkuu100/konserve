package com.example.konserve

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class TFLiteModel(context: Context) {
    private val interpreter: Interpreter

    init {
        // Load TFLite model from assets as a ByteBuffer
        val assetManager = context.assets
        val fileDescriptor = assetManager.openFd("model_optimized.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

        // Initialize interpreter with ByteBuffer
        interpreter = Interpreter(modelBuffer)
    }

    fun preprocessImage(bitmap: Bitmap): ByteBuffer {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
        val byteBuffer = ByteBuffer.allocateDirect(224 * 224 * 3 * 4)
        byteBuffer.order(ByteOrder.nativeOrder())

        for (y in 0 until 224) {
            for (x in 0 until 224) {
                val pixel = resizedBitmap.getPixel(x, y)
                byteBuffer.putFloat(((pixel shr 16 and 0xFF) - 127.5f) / 127.5f)
                byteBuffer.putFloat(((pixel shr 8 and 0xFF) - 127.5f) / 127.5f)
                byteBuffer.putFloat(((pixel and 0xFF) - 127.5f) / 127.5f)
            }
        }
        return byteBuffer
    }

    fun runInference(inputBuffer: ByteBuffer): FloatArray {
        val outputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 1001), DataType.FLOAT32)
        interpreter.run(inputBuffer, outputBuffer.buffer.rewind())
        return outputBuffer.floatArray
    }
}

