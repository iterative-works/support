package works.iterative.core
package json

import zio.json.*
import zio.prelude.Validation
import zio.json.ast.Json
import scala.annotation.tailrec

trait JsonUtils:
    /** Deserialize part of the original json data */
    def getFromJsonObjAs[T](jsonObj: Json.Obj)(keyPath: String)(using
        d: JsonDecoder[T]
    ): Option[T] =
        @tailrec
        def getAux(path: List[String], obj: Json.Obj): Option[T] =
            path match
                case Nil => d.fromJsonAST(obj).toOption
                case head :: Nil =>
                    obj.get(head).flatMap(d.fromJsonAST(_).toOption)
                case head :: tail =>
                    obj.get(head).collect {
                        case j: Json.Obj => j
                    } match
                        case Some(o) => getAux(tail, o)
                        case None    => None

        getAux(keyPath.split('.').toList, jsonObj)
    end getFromJsonObjAs

    def getValidatedFromJsonObjAs[T](jsonObj: Json.Obj)(keyPath: String)(using
        d: JsonDecoder[T]
    ): Validated[T] =
        Validation.fromOptionWith(UserMessage("error.json.key.missing", keyPath))(
            getFromJsonObjAs(jsonObj)(keyPath)
        )
    end getValidatedFromJsonObjAs
end JsonUtils

object JsonUtils extends JsonUtils:
    extension (jsonObj: Json.Obj)
        def getAs[T: JsonDecoder](keyPath: String): Option[T] =
            getFromJsonObjAs(jsonObj)(keyPath)
        end getAs

        def getValidatedAs[T: JsonDecoder](keyPath: String): Validated[T] =
            getValidatedFromJsonObjAs(jsonObj)(keyPath)
    end extension
end JsonUtils
