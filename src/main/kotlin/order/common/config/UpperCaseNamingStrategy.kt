package order.common.config

import org.hibernate.boot.model.naming.Identifier
import org.hibernate.boot.model.naming.PhysicalNamingStrategy
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment

class UpperCaseNamingStrategy : PhysicalNamingStrategy {
    override fun toPhysicalCatalogName(name: Identifier?, jdbcEnvironment: JdbcEnvironment?): Identifier? {
        return name
    }

    override fun toPhysicalSchemaName(name: Identifier?, jdbcEnvironment: JdbcEnvironment?): Identifier? {
        return name
    }

    override fun toPhysicalTableName(name: Identifier, jdbcEnvironment: JdbcEnvironment): Identifier {
        return Identifier.toIdentifier(name.text.uppercase(), name.isQuoted)
    }

    override fun toPhysicalSequenceName(name: Identifier?, jdbcEnvironment: JdbcEnvironment?): Identifier? {
        return name
    }

    override fun toPhysicalColumnName(name: Identifier, jdbcEnvironment: JdbcEnvironment): Identifier {
        return name
    }
}
