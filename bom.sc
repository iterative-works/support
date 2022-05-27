import mill._, scalalib._

object IWMaterials {

  object Versions {
    val akka = "2.6.16"
    val akkaHttp = "10.2.4"
    val cats = "2.7.0"
    val elastic4s = "7.12.2"
    val http4s = "0.23.10"
    val http4sPac4J = "4.0.0"
    val laminar = "0.14.2"
    val laminext = laminar
    val logbackClassic = "1.2.10"
    val pac4j = "5.2.0"
    val play = "2.8.8"
    val playJson = "2.9.2"
    val refined = "0.9.29"
    val scalaTest = "3.2.9"
    val slf4j = "1.7.36"
    val slick = "3.3.3"
    val sttpClient = "3.5.0"
    val tapir = "0.20.1"
    val urlDsl = "0.4.0"
    val waypoint = "0.5.0"
    val zio = "2.0.0-RC2"
    val zioConfig = "3.0.0-RC2"
    val zioInteropCats = "3.3.0-RC2"
    val zioJson = "0.3.0-RC3"
    val zioLogging = "2.0.0-RC5"
    val zioPrelude = "1.0.0-RC10"
    val zioZMX = "0.0.11"
  }

  object Deps extends AkkaLibs with SlickLibs {
    import IWMaterials.{Versions => V}

    val zioOrg = "dev.zio"

    def zioLib(name: String, version: String): Dep =
      ivy"$zioOrg::zio-$name::$version"

    lazy val zio: Dep = ivy"$zioOrg::zio:${V.zio}"

    lazy val zioTest: Dep = zioLib("test", V.zio)
    lazy val zioTestSbt: Dep = zioLib("test-sbt", V.zio)

    lazy val zioConfig: Dep = zioLib("config", V.zioConfig)
    lazy val zioConfigTypesafe: Dep =
      zioLib("config-typesafe", V.zioConfig)
    lazy val zioConfigMagnolia: Dep =
      zioLib("config-magnolia", V.zioConfig)

    lazy val zioJson: Dep = zioLib("json", V.zioJson)
    lazy val zioLogging: Dep = zioLib("logging", V.zioLogging)
    lazy val zioLoggingSlf4j: Dep =
      zioLib("logging-slf4j", V.zioLogging)
    lazy val zioPrelude: Dep = zioLib("prelude", V.zioPrelude)
    lazy val zioStreams: Dep = zioLib("streams", V.zio)
    lazy val zioZMX: Dep = zioLib("zmx", V.zioZMX)
    lazy val zioInteropCats: Dep =
      zioLib("interop-cats", V.zioInteropCats)

    lazy val catsCore: Dep = ivy"org.typelevel::cats-core::${V.cats}"

    lazy val refined: Dep = ivy"eu.timepit::refined::${V.refined}"

    /* What is the equivalent? ZIOModule with prepared test config?
  def useZIO(testConf: Configuration*): Agg[Dep] = Agg(
    zio,
    zioTest,
    zioTestSbt,
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
   )
     */

    /*
  def useZIOAll(testConf: Configuration*): Seq[Def.Setting[_]] =
    useZIO(testConf: _*) ++ Seq(
      zioStreams,
      zioConfig,
      zioConfigTypesafe,
      zioConfigMagnolia,
      zioJson,
      zioZMX,
      zioLogging,
      zioPrelude
   )
     */

    private val tapirOrg = "com.softwaremill.sttp.tapir"
    def tapirLib(name: String): Dep =
      ivy"${tapirOrg}::tapir-$name::${V.tapir}"

    lazy val tapirCore: Dep = tapirLib("core")
    lazy val tapirZIO: Dep = tapirLib("zio")
    lazy val tapirZIOJson: Dep = tapirLib("json-zio")
    lazy val tapirSttpClient: Dep = tapirLib("sttp-client")
    lazy val tapirCats: Dep = tapirLib("cats")
    lazy val tapirZIOHttp4sServer: Dep = tapirLib("zio-http4s-server")

    val sttpClientOrg = "com.softwaremill.sttp.client3"
    def sttpClientLib(name: String): Dep =
      ivy"${sttpClientOrg}::${name}:${V.sttpClient}"

    lazy val sttpClientCore: Dep = sttpClientLib("core")
    lazy val sttpClientZIO: Dep = sttpClientLib("httpclient-backend-zio")

    lazy val http4sBlazeServer: Dep =
      ivy"org.http4s::http4s-blaze-server:${V.http4s}"

    lazy val http4sPac4J: Dep =
      ivy"org.pac4j::http4s-pac4j:${V.http4sPac4J}"
    lazy val pac4jOIDC: Dep =
      ivy"org.pac4j:pac4j-oidc:${V.pac4j}"

    lazy val scalaTest: Dep =
      ivy"org.scalatest::scalatest:${V.scalaTest}"
    lazy val scalaTestPlusScalacheck: Dep =
      ivy"org.scalatestplus::scalacheck-1-15:3.2.9.0"
    lazy val playScalaTest: Dep =
      ivy"org.scalatestplus.play::scalatestplus-play:5.1.0"

    private val playOrg = "com.typesafe.play"
    lazy val playMailer: Dep = ivy"${playOrg}::play-mailer:8.0.1"
    lazy val playServer: Dep = ivy"${playOrg}::play-server:${V.play}"
    lazy val playAkkaServer: Dep =
      ivy"${playOrg}::play-akka-http-server:${V.play}"
    lazy val play: Dep = ivy"${playOrg}::play:${V.play}"
    lazy val playAhcWs: Dep = ivy"${playOrg}::play-ahc-ws:${V.play}"
    lazy val playJson: Dep = ivy"${playOrg}::play-json:${V.playJson}"

    private val elastic4sOrg = "com.sksamuel.elastic4s"
    lazy val useElastic4S: Agg[Dep] = Agg(
      ivy"${elastic4sOrg}::elastic4s-core:${V.elastic4s}",
      ivy"${elastic4sOrg}::elastic4s-client-akka:${V.elastic4s}",
      ivy"${elastic4sOrg}::elastic4s-http-streams:${V.elastic4s}",
      ivy"${elastic4sOrg}::elastic4s-json-play:${V.elastic4s}"
    )

    lazy val laminar: Dep = ivy"com.raquo::laminar::${V.laminar}"

    private def laminext(name: String): Dep =
      ivy"io.laminext::$name::${V.laminar}"

    lazy val laminextCore: Dep = laminext("core")
    lazy val laminextTailwind: Dep = laminext("tailwind")
    lazy val laminextFetch: Dep = laminext("fetch")
    lazy val laminextValidationCore: Dep = laminext("validation-core")
    lazy val laminextUI: Dep = laminext("ui")

    lazy val waypoint: Dep =
      ivy"com.raquo::waypoint::${V.waypoint}"

    lazy val urlDsl: Dep =
      ivy"be.doeraene::url-dsl::${V.urlDsl}"

    lazy val scalaJavaTime: Dep =
      ivy"io.github.cquiroz::scala-java-time::2.3.0"

    lazy val scalaJavaLocales: Dep =
      ivy"io.github.cquiroz::scala-java-locales::1.2.1"

    lazy val logbackClassic: Dep =
      ivy"ch.qos.logback:logback-classic:${V.logbackClassic}"
  }

  trait AkkaLibs {

    self: SlickLibs =>

    object akka {
      val V = Versions.akka
      val tOrg = "com.typesafe.akka"
      val lOrg = "com.lightbend.akka"

      def akkaMod(name: String): Dep =
        ivy"$tOrg::akka-$name:$V"

      lazy val actor: Dep = akkaMod("actor")
      lazy val actorTyped: Dep = akkaMod("actor-typed")
      lazy val stream: Dep = akkaMod("stream")
      lazy val persistence: Dep = akkaMod("persistence-typed")
      lazy val persistenceQuery: Dep = akkaMod("persistence-query")
      lazy val persistenceJdbc: Dep =
        ivy"$lOrg::akka-persistence-jdbc:5.0.4"
      val persistenceTestKit: Dep = ivy"$tOrg::akka-persistence-testkit:$V"

      object http {
        val V = Versions.akkaHttp

        lazy val http: Dep = ivy"$tOrg::akka-http:$V"
        lazy val sprayJson: Dep = ivy"$tOrg::akka-http-spray-json:$V"

      }

      object projection {
        val V = "1.2.2"

        lazy val core: Dep =
          ivy"$lOrg::akka-projection-core:$V"
        lazy val eventsourced: Dep =
          ivy"$lOrg::akka-projection-eventsourced:$V"
        lazy val slick: Dep =
          ivy"$lOrg::akka-projection-slick:$V"
        lazy val jdbc: Dep =
          ivy"$lOrg::akka-projection-jdbc:$V"
      }

      object profiles {
        // TODO: deal with cross-version for Scala3 (for3Use2_13)
        lazy val eventsourcedJdbcProjection: Agg[Dep] = Agg(
          persistenceQuery,
          projection.core,
          projection.eventsourced,
          projection.slick,
          persistenceJdbc
        ) ++ slick.default
      }
    }
  }

  trait SlickLibs {
    object slick {
      val V = IWMaterials.Versions.slick
      val org = "com.typesafe.slick"

      lazy val slick: Dep = ivy"$org::slick:$V"
      lazy val hikaricp: Dep = ivy"$org::slick-hikaricp:$V"
      lazy val default: Agg[Dep] = Agg(slick, hikaricp)
    }
  }

}
