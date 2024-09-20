package com.strumenta.entity.parser.metamodel

import com.strumenta.entity.parser.ast.Entity
import com.strumenta.entity.parser.ast.Expression
import com.strumenta.entity.parser.ast.Feature
import com.strumenta.entity.parser.ast.Module
import com.strumenta.entity.parser.ast.Operation
import com.strumenta.entity.parser.ast.Statement
import com.strumenta.entity.parser.ast.Workspace
import com.strumenta.kolasu.emf.MetamodelBuilder
import org.eclipse.emf.ecore.resource.Resource
import kotlin.reflect.KClass

object EntityMetamodelBuilder : (Resource?) -> MetamodelBuilder {
    private const val PACKAGE_NAME: String = "com.strumenta.entity.parser.ast"
    private const val NS_URI: String = "https://strumenta.com/entity"
    private const val NS_PREFIX: String = "entity"

    private val metaclasses: List<KClass<*>> =
        listOf(
            Workspace::class,
            Module::class,
            Entity::class,
            Feature::class,
            Operation::class,
            Statement::class,
            Expression::class,
        )

    override fun invoke(resource: Resource?): MetamodelBuilder =
        MetamodelBuilder(metamodelName = PACKAGE_NAME, nsURI = NS_URI, nsPrefix = NS_PREFIX, resource = resource)
            .apply { metaclasses.forEach(this::provideClass) }
}
