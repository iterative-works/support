package mdr.pdb.app.services

import com.raquo.airstream.core.Observer

trait DataFetcher[K, A]:
  def fetch(id: K, sink: Observer[A]): Unit
