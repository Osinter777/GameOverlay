package com.fox.gameoverlay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class Detection(
    val label: String,
    val score: Float,
    val rect: RectF
)

class TFLiteHelper(context: Context) : AutoCloseable {

    companion object {
        private const val TAG = "TFLiteHelper"
        const val INPUT_SIZE = 300
        const val NUM_CLASSES = 90
        const val NUM_BOXES = 10
        const val SCORE_THRESHOLD = 0.45f

        private val LABELS = arrayOf(
            "person","bicycle","car","motorcycle","airplane","bus","train","truck","boat",
            "traffic light","fire hydrant","stop sign","parking meter","bench","bird","cat",
            "dog","horse","sheep","cow","elephant","bear","zebra","giraffe","backpack",
            "umbrella","handbag","tie","suitcase","frisbee","skis","snowboard","sports ball",
            "kite","baseball bat","baseball glove","skateboard","surfboard","tennis racket",
            "bottle","wine glass","cup","fork","knife","spoon","bowl","banana","apple",
            "sandwich","orange","broccoli","carrot","hot dog","pizza","donut","cake","chair",
            "couch","potted plant","bed","dining table","toilet","tv","laptop","mouse",
            "remote","keyboard","cell phone","microwave","oven","toaster","sink","refrigerator",
            "book","clock","vase","scissors","teddy bear","hair drier","toothbrush"
        )
    }

    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null

    private val inputBuffer: ByteBuffer = ByteBuffer
        .allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4)
        .order(ByteOrder.nativeOrder())

    private val outputBoxes = Array(1) { Array(NUM_BOXES) { FloatArray(4) } }
    private val outputClasses = Array(1) { FloatArray(NUM_BOXES) }
    private val outputScores = Array(1) { FloatArray(NUM_BOXES) }
    private val outputCount = FloatArray(1)

    private val outputMap = HashMap<Int, Any>().apply {
        put(0, outputBoxes)
        put(1, outputClasses)
        put(2, outputScores)
        put(3, outputCount)
    }

    init {
        try {
            val model = FileUtil.loadMappedFile(context, "ssd_mobilenet.tflite")
            val options = Interpreter.Options().apply {
                setNumThreads(4)
                val compat = CompatibilityList()
                if (compat.isDelegateSupportedOnThisDevice) {
                    gpuDelegate = GpuDelegate(compat.bestOptionsForThisDevice)
                    addDelegate(gpuDelegate)
                }
            }
            interpreter = Interpreter(model, options)
        } catch (t: Throwable) {
            Log.e(TAG, "model not found", t)
            interpreter = null
        }
    }

    fun detect(bitmap: Bitmap): List<Detection> {
        val itp = interpreter ?: return emptyList()

        inputBuffer.rewind()
        val scaled = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        scaled.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        if (scaled !== bitmap) scaled.recycle()

        for (p in pixels) {
            inputBuffer.putFloat(((p shr 16) and 0xFF) / 255f)
            inputBuffer.putFloat(((p shr 8) and 0xFF) / 255f)
            inputBuffer.putFloat((p and 0xFF) / 255f)
        }
        inputBuffer.rewind()

        itp.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputMap)

        val count = outputCount[0].toInt().coerceIn(0, NUM_BOXES)
        val result = ArrayList<Detection>(count)
        val srcW = bitmap.width.toFloat()
        val srcH = bitmap.height.toFloat()

        for (i in 0 until count) {
            val score = outputScores[0][i]
            if (score < SCORE_THRESHOLD) continue
            val cls = outputClasses[0][i].toInt()
            val box = outputBoxes[0][i]
            val top = box[0].coerceIn(0f, 1f)
            val left = box[1].coerceIn(0f, 1f)
            val bottom = box[2].coerceIn(0f, 1f)
            val right = box[3].coerceIn(0f, 1f)
            val rect = RectF(left * srcW, top * srcH, right * srcW, bottom * srcH)
            val label = if (cls in LABELS.indices) LABELS[cls] else "cls_$cls"
            result.add(Detection(label, score, rect))
        }
        return result
    }

    override fun close() {
        try { interpreter?.close() } catch (_: Throwable) {}
        try { gpuDelegate?.close() } catch (_: Throwable) {}
        interpreter = null
        gpuDelegate = null
    }
}
