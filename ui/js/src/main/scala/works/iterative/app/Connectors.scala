package works.iterative.app

trait Connectors[Env, P]:
    def make: List[Connector[Env, P]]

object Connectors:
    def apply[Env, P](factories: Connector[Env, P]*): Connectors[Env, P] =
        new Connectors[Env, P]:
            override def make: List[Connector[Env, P]] = factories.toList
end Connectors
