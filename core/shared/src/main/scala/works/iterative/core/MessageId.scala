package works.iterative
package core

// TODO: use refined's NonEmptyString after macros are ported to Scala 3
// https://github.com/fthomas/refined/issues/932
// We could than use the inlined macro to validate non empty strings at runtime

/* MessageId is an opaque type to mark all keys to translations.
 * The intent is to use it during build to generate a list of all keys
 * and lately to check if we have all the translations we need
 */
opaque type MessageId = String

object MessageId:
    def apply(id: String): MessageId = id

    extension (m: MessageId)
        def value: String = m
        def append(ids: String*): MessageId = MessageId((m +: ids).mkString("."))
        def /(id: String): MessageId = append(id)
    end extension

    inline given Conversion[String, MessageId] with
        inline def apply(id: String): MessageId = MessageId(id)
end MessageId
