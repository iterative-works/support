package works.iterative
package core
package service

trait SequenceIdGeneratorFactory:
    def generatorFor(row: String): IdGenerator[Int]
