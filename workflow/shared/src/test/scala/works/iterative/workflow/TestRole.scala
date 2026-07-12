// PURPOSE: Test role type standing in for an application-supplied role enum in the workflow specs
// PURPOSE: Exercises the R type parameter of Guard/GuardContext/RequireRole with concrete cases
package works.iterative.workflow

/** A minimal role type used by the guard and interpreter specs to instantiate the `R` parameter. */
enum TestRole:
    case AdministratorZakazek
    case Pracovnik
end TestRole
