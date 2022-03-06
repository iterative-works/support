package mdr.pdb
package parameters
package json

import zio.json.*

trait Codecs:

  given JsonCodec[ParameterCriteria] = DeriveJsonCodec.gen
  given JsonCodec[Parameter] = DeriveJsonCodec.gen
