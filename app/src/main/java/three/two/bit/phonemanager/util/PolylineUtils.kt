package three.two.bit.phonemanager.util

import com.google.android.gms.maps.model.LatLng

/**
 * Story E4.2: Polyline downsampling utilities for performance optimization
 *
 * AC E4.2.2: Downsample large polylines to 200-500 points for rendering performance
 */
object PolylineUtils {

    /**
     * Downsample a list of points to a target count using simple interval-based sampling
     *
     * @param points Original list of LatLng points
     * @param targetCount Target number of points (default 300)
     * @return Downsampled list of points
     */
    fun downsample(points: List<LatLng>, targetCount: Int = 300): List<LatLng> {
        if (points.size <= targetCount) return points

        // Simple interval-based downsampling
        // Keep first and last points, distribute others evenly
        val result = mutableListOf<LatLng>()
        result.add(points.first())

        val step = points.size.toDouble() / (targetCount - 1)
        for (i in 1 until targetCount - 1) {
            val index = (i * step).toInt()
            result.add(points[index])
        }

        result.add(points.last())
        return result
    }

    /**
     * Downsample LocationEntity latitude/longitude pairs
     *
     * @param latitudes List of latitude values
     * @param longitudes List of longitude values
     * @param targetCount Target number of points
     * @return Pair of downsampled (latitudes, longitudes)
     */
    fun downsampleCoordinates(
        latitudes: List<Double>,
        longitudes: List<Double>,
        targetCount: Int = 300,
    ): Pair<List<Double>, List<Double>> {
        require(latitudes.size == longitudes.size) { "Latitude and longitude lists must have same size" }

        if (latitudes.size <= targetCount) {
            return latitudes to longitudes
        }

        val points = latitudes.zip(longitudes).map { (lat, lng) -> LatLng(lat, lng) }
        val downsampled = downsample(points, targetCount)

        return downsampled.map { it.latitude } to downsampled.map { it.longitude }
    }
}
