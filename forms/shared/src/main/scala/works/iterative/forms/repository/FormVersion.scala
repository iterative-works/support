package portaly.forms.repository

enum FormVersion:
    case Latest
    case Version(version: String)

    def toOption: Option[String] =
        this match
        case FormVersion.Latest     => None
        case FormVersion.Version(v) => Some(v)
end FormVersion

object FormVersion:
    def apply(version: Option[String]): FormVersion =
        version match
        case Some(v) => FormVersion.Version(v)
        case None    => FormVersion.Latest
end FormVersion
