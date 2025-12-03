package works.iterative.sqldb.postgresql

import zio.*
import works.iterative.sqldb.{FlywayConfig, FlywayMigrationService}

/** Base trait for PostgreSQL database modules that provides common infrastructure
  *
  * This trait orchestrates the creation of database infrastructure components like datasource,
  * transactor, and migration management. It's the foundation layer for database modules.
  *
  * Classification: Infrastructure Configuration
  */
object PostgreSQLDatabaseSupport:
    /** Base database infrastructure type including data source and transactor
      */
    type BaseDatabaseInfrastructure = PostgreSQLDataSource & PostgreSQLTransactor

    /** Creates a ZLayer with the base database infrastructure (DataSource & Transactor)
      */
    val layer: ZLayer[Scope, Throwable, BaseDatabaseInfrastructure] =
        // Create the shared data source
        val dataSourceLayer = PostgreSQLDataSource.managedLayer
        // Create the transactor from the data source
        val transactorLayer = dataSourceLayer >+> PostgreSQLTransactor.managedLayer

        // Return the combined layer
        transactorLayer
    end layer

    /** Helper method to create FlywayConfig with optional additional locations
      */
    protected def createFlywayConfig(additionalLocations: List[String] = List.empty): FlywayConfig =
        if additionalLocations.nonEmpty then
            FlywayConfig(locations = FlywayConfig.DefaultLocation :: additionalLocations)
        else
            FlywayConfig.default

    /** Creates a ZLayer with the base database infrastructure and runs migrations first This layer
      * forwards both the database infrastructure and migration service
      *
      * @param additionalLocations
      *   Additional classpath locations to scan for migrations
      */
    def layerWithMigrations(
        additionalLocations: List[String] = List.empty
    ): ZLayer[Scope, Throwable, BaseDatabaseInfrastructure & FlywayMigrationService] =
        // Create flyway config using the helper method
        val flywayConfig = createFlywayConfig(additionalLocations)

        // Create the underlying layers
        val dataSourceLayer = PostgreSQLDataSource.managedLayer
        val transactorLayer = dataSourceLayer >+> PostgreSQLTransactor.managedLayer
        val flywayLayer = dataSourceLayer >>> PostgreSQLFlywayMigrationService.layerWithConfig(flywayConfig)

        // Combine layers to create the final ZLayer with both infrastructure and migration service
        val combinedLayer = transactorLayer ++ flywayLayer

        // Run migrations before providing the layer
        ZLayer.scoped {
            for
                migrator <- ZIO.service[FlywayMigrationService].provideSome[Scope](flywayLayer)
                _ <- migrator.migrate().tapError(err =>
                    ZIO.logError(s"Migration failed: ${err.getMessage}")
                )
            yield ()
        } >>> combinedLayer
    end layerWithMigrations

    /** Runs flyway migrations with custom locations
      *
      * @param additionalLocations
      *   Additional classpath locations to scan for migrations
      */
    def migrate(
        additionalLocations: List[String] = List.empty
    ): ZIO[Scope, Throwable, Unit] =
        val dataSourceLayer = PostgreSQLDataSource.managedLayer

        // Create flyway config using the helper method
        val flywayConfig = createFlywayConfig(additionalLocations)

        val flywayLayer = dataSourceLayer >>> PostgreSQLFlywayMigrationService.layerWithConfig(flywayConfig)

        ZIO.scoped {
            for
                migrator <- ZIO.service[FlywayMigrationService].provideSome[Scope](flywayLayer)
                result <- migrator.migrate()
                _ <- ZIO.logInfo(
                    s"Migration completed: ${result.migrationsExecuted} migrations executed"
                )
            yield ()
        }
    end migrate
end PostgreSQLDatabaseSupport
