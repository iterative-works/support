package works.iterative.entity

import zio.*

trait ViewProcessor[Event]:
    def process(event: Event): UIO[Unit]
