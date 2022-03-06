package mdr.pdb
package parameters.command
package json

import zio.json.*

trait Codecs extends mdr.pdb.json.Codecs:

  given JsonCodec[AutorizujDukaz] = DeriveJsonCodec.gen
