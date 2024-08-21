package portaly.forms

trait UIFormReadOnlyComponentsResolver:
    def resolveReadOnlyComponents(ident: FormIdent): ReadOnlyComponents

trait UIFormReadWriteComponentsResolver:
    def resolveReadWriteComponents(ident: FormIdent): Components

trait UIFormComponentsResolver extends UIFormReadOnlyComponentsResolver
    with UIFormReadWriteComponentsResolver
