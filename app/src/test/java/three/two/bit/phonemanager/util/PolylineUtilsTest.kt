package three.two.bit.phonemanager.util

import com.google.android.gms.maps.model.LatLng
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PolylineUtilsTest {

    @Test
    fun `downsample returns original list when size is less than target`() {
        // Given
        val points = listOf(
            LatLng(48.1, 17.1),
            LatLng(48.2, 17.2),
            LatLng(48.3, 17.3),
        )

        // When
        val result = PolylineUtils.downsample(points, targetCount = 10)

        // Then
        assertEquals(3, result.size)
        assertEquals(points, result)
    }

    @Test
    fun `downsample reduces list to target count`() {
        // Given
        val points = (1..1000).map { LatLng(48.0 + it * 0.001, 17.0 + it * 0.001) }

        // When
        val result = PolylineUtils.downsample(points, targetCount = 300)

        // Then
        assertEquals(300, result.size)
        assertEquals(points.first(), result.first()) // First point preserved
        assertEquals(points.last(), result.last()) // Last point preserved
    }

    @Test
    fun `downsample preserves first and last points`() {
        // Given
        val points = (1..100).map { LatLng(48.0 + it, 17.0 + it) }

        // When
        val result = PolylineUtils.downsample(points, targetCount = 10)

        // Then
        assertEquals(LatLng(49.0, 18.0), result.first())
        assertEquals(LatLng(148.0, 117.0), result.last())
    }

    @Test
    fun `downsampleCoordinates works with latitude and longitude lists`() {
        // Given
        val latitudes = (1..1000).map { 48.0 + it * 0.001 }
        val longitudes = (1..1000).map { 17.0 + it * 0.001 }

        // When
        val (resultLat, resultLng) = PolylineUtils.downsampleCoordinates(latitudes, longitudes, 200)

        // Then
        assertEquals(200, resultLat.size)
        assertEquals(200, resultLng.size)
        assertEquals(latitudes.first(), resultLat.first(), 0.0001)
        assertEquals(longitudes.last(), resultLng.last(), 0.0001)
    }

    @Test
    fun `downsampleCoordinates throws when sizes don't match`() {
        // Given
        val latitudes = listOf(48.1, 48.2)
        val longitudes = listOf(17.1, 17.2, 17.3)

        // When/Then
        try {
            PolylineUtils.downsampleCoordinates(latitudes, longitudes)
            assertTrue(false, "Expected exception not thrown")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("same size") == true)
        }
    }
}
