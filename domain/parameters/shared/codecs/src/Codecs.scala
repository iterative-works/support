package mdr.pdb
package parameters
package codecs

import zio.json.*

trait Codecs:

  given JsonCodec[ParameterCriterion] = DeriveJsonCodec.gen
  given JsonCodec[Parameter] = DeriveJsonCodec.gen
