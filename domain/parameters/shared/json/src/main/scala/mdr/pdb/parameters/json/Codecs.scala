package mdr.pdb
package parameters
package json

import zio.json.*

trait Codecs:

  given JsonCodec[ParameterCriterion] = DeriveJsonCodec.gen
  given JsonCodec[Parameter] = DeriveJsonCodec.gen
