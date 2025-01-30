package works.iterative.ui.components

import works.iterative.core.Moment

trait StatusDisplayComponents[T] extends Components[T]:
    import StatusDisplayComponents.*

    /** Displays a status with appropriate styling */
    def status(
        state: String,
        variant: StatusVariant = StatusVariant.Default
    ): T

    /** Displays a timestamp with proper formatting */
    def timestamp(datetime: Moment): T

    /** Displays a metadata field with label */
    def metadataField(
        label: String,
        value: T,
        className: String = ""
    ): T

    /** Groups multiple metadata fields */
    def metadataGroup(fields: T*): T
end StatusDisplayComponents

object StatusDisplayComponents:
    enum StatusVariant:
        case Default, Success, Warning, Error, Info
