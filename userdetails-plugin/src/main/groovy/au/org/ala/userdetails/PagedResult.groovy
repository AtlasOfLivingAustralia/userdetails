package au.org.ala.userdetails

/**
 * Container for results from paged operations.  By convention if count is null then the backend
 * does not support paging by offset and the next page token must be used instead.
 * @param <T> The type of result being returned
 */
class PagedResult<T> {

    List<T> list
    Integer count
    String nextPageToken
}
