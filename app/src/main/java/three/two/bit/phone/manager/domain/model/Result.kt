package three.two.bit.phone.manager.domain.model

/**
 * A generic Result wrapper for handling success and error states.
 * Used throughout the app for error handling in repositories and use cases.
 *
 * @param T the type of data returned on success
 */
sealed interface Result<out T> {
    /**
     * Success state containing the data.
     */
    data class Success<T>(val data: T) : Result<T>

    /**
     * Error state containing error message and optional exception.
     */
    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : Result<Nothing>

    /**
     * Loading state to indicate an ongoing operation.
     */
    data object Loading : Result<Nothing>
}

/**
 * Extension function to handle Result in a functional way.
 *
 * @param onSuccess callback invoked when Result is Success
 * @param onError callback invoked when Result is Error
 * @param onLoading callback invoked when Result is Loading
 */
inline fun <T> Result<T>.handle(
    onSuccess: (T) -> Unit = {},
    onError: (String, Throwable?) -> Unit = { _, _ -> },
    onLoading: () -> Unit = {}
) {
    when (this) {
        is Result.Success -> onSuccess(data)
        is Result.Error -> onError(message, exception)
        is Result.Loading -> onLoading()
    }
}

/**
 * Map the data inside a Success Result.
 *
 * @param transform function to transform the data
 * @return Result with transformed data
 */
inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> {
    return when (this) {
        is Result.Success -> Result.Success(transform(data))
        is Result.Error -> this
        is Result.Loading -> this
    }
}

/**
 * Check if Result is Success.
 */
fun <T> Result<T>.isSuccess(): Boolean = this is Result.Success

/**
 * Check if Result is Error.
 */
fun <T> Result<T>.isError(): Boolean = this is Result.Error

/**
 * Check if Result is Loading.
 */
fun <T> Result<T>.isLoading(): Boolean = this is Result.Loading

/**
 * Get data if Success, null otherwise.
 */
fun <T> Result<T>.getOrNull(): T? {
    return when (this) {
        is Result.Success -> data
        else -> null
    }
}
