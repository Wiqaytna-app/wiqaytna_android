package covid.trace.morocco.protocol.v2

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import covid.trace.morocco.streetpass.CentralDevice

class V2WriteRequestPayload(
    val v: Int,
    val id: String,
    val o: String,
    central: CentralDevice,
    val rs: Int
) {

    val mc: String = central.modelC

    fun getPayload(): ByteArray {
        return gson.toJson(this).toByteArray(Charsets.UTF_8)
    }

    companion object {
        val gson: Gson = GsonBuilder()
            .disableHtmlEscaping().create()

        fun fromPayload(dataBytes: ByteArray): V2WriteRequestPayload {
            val dataString = String(dataBytes, Charsets.UTF_8)
            return gson.fromJson(dataString, V2WriteRequestPayload::class.java)
        }
    }
}
