package works.iterative.ui.model.forms

trait FormState:
    def getString(id: AbsolutePath): Option[String]
    def getDouble(id: AbsolutePath): Option[Double]
    def getInt(id: AbsolutePath): Option[Int]
    def getStringList(id: AbsolutePath): Option[List[String]]
    def getFileList(id: AbsolutePath): Option[List[UIFile]]
    def itemsFor(id: AbsolutePath): List[(String, String)]
end FormState
