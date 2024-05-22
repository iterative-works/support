package works.iterative.tapir
package api

import works.iterative.core.service.FileStore

import zio.*
import zio.json.*
import works.iterative.core.service.FileStoreWriter
import works.iterative.core.service.FileStoreLoader
import CustomTapir.*
import endpoints.FileStoreEndpointsModule
import sttp.capabilities.zio.ZioStreams

trait FileApi[T <: FileStoreEndpointsModule](endpoints: T):
    object file:
        val store: ZServerEndpoint[FileStoreWriter, Any] =
            endpoints.store.zServerLogic { parts =>
                for
                    metadata <- ZIO
                        .fromOption(parts.find(_.name == "metadata").map(_.body))
                        .map(new String(_, "UTF-8"))
                        .map(_.fromJson[FileStore.Metadata])
                        .right
                        .orDieWith {
                            case Left(error) =>
                                new IllegalArgumentException(
                                    s"Unable to decode metadata: $error"
                                )
                            case _ => new IllegalArgumentException("Metadata not found")
                        }
                    refs <- ZIO.foreach(parts.filterNot(_.name == "metadata"))(part =>
                        FileStore.store(
                            part.fileName
                                .map(f => new String(BigInt(f, 16).toByteArray, "UTF-8"))
                                .getOrElse(part.name),
                            part.body,
                            part.contentType,
                            metadata
                        )
                    )
                yield refs.toList
            }

        val storeFile: ZServerEndpoint[FileStoreWriter, ZioStreams] =
            endpoints.storeFile.zServerLogic((params, contentType, file) =>
                FileStore.store(
                    file.getName(),
                    file,
                    params.toMap + (FileStore.Metadata.FileType -> contentType)
                )
            )

        val load: ZServerEndpoint[FileStoreLoader, Any] =
            endpoints.load.zServerLogic { url =>
                FileStore.load(url.mkString("/")).someOrFail("File not found")
            }

        val loadStream: ZServerEndpoint[FileStoreLoader, ZioStreams] =
            endpoints.loadStream.zServerLogic { url =>
                FileStore.loadStream(url.mkString("/"))
            }

        val update: ZServerEndpoint[FileStoreWriter, Any] =
            endpoints.update.zServerLogic { a =>
                FileStore.update(a.urls, a.metadata)
            }
    end file
end FileApi
