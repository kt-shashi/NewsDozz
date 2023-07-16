import com.google.firebase.Timestamp

data class BookmarkData(
    var title: String = "",
    var desc: String = "",
    var imageUrl: String = "",
    var newsUrl: String = "",
    var timestamp: Timestamp? = null
) {

}