package three.two.bit.phonemanager.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import timber.log.Timber

/**
 * Story E11.9 Task 9: QR Code Generator Utility
 *
 * AC E11.9.2: Generate QR code representation of invite codes
 *
 * Uses ZXing library to generate QR codes containing deep links
 * in the format: phonemanager://join/{code}
 */
object QRCodeGenerator {

    private const val DEFAULT_SIZE = 300

    /**
     * Generate a QR code bitmap for the given content.
     *
     * @param content The content to encode in the QR code (e.g., deep link URL)
     * @param size The size of the QR code in pixels (default: 300)
     * @return Bitmap of the QR code, or null if generation fails
     */
    fun generateQRCode(content: String, size: Int = DEFAULT_SIZE): Bitmap? = try {
        val hints = mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.ERROR_CORRECTION to com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M,
        )

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)

        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }

        Timber.d("QR code generated successfully for content: ${content.take(50)}...")
        bitmap
    } catch (e: Exception) {
        Timber.e(e, "Failed to generate QR code")
        null
    }

    /**
     * Generate a QR code for a group invite code.
     *
     * Creates a deep link in the format: phonemanager://join/{code}
     *
     * @param inviteCode The 8-character invite code
     * @param size The size of the QR code in pixels (default: 300)
     * @return Bitmap of the QR code, or null if generation fails
     */
    fun generateInviteQRCode(inviteCode: String, size: Int = DEFAULT_SIZE): Bitmap? {
        val deepLink = "phonemanager://join/$inviteCode"
        return generateQRCode(deepLink, size)
    }

    /**
     * Generate a high-resolution QR code for sharing/printing.
     *
     * @param content The content to encode
     * @return Bitmap of the high-res QR code (600x600), or null if generation fails
     */
    fun generateHighResQRCode(content: String): Bitmap? = generateQRCode(content, 600)
}
