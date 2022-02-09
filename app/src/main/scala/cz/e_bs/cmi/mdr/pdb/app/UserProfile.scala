package cz.e_bs.cmi.mdr.pdb.app

case class UserInfo(
    name: String,
    email: String,
    phone: String,
    img: Option[String],
    oi: String,
    mainFunction: String
)

case class UserProfile(username: String, userInfo: UserInfo)
