package com.strumenta.entity.parser.semantics

import com.strumenta.entity.parser.EntityParser
import com.strumenta.entity.parser.ast.ConstructorExpression
import com.strumenta.entity.parser.ast.Feature
import com.strumenta.entity.parser.ast.Import
import com.strumenta.entity.parser.ast.Operation
import com.strumenta.entity.parser.ast.OperationReference
import com.strumenta.entity.parser.ast.ReferenceExpression
import com.strumenta.entity.parser.ast.Variable
import com.strumenta.kolasu.model.KReferenceByName
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.model.kReferenceByNameProperties
import com.strumenta.kolasu.testing.assertReferencesNotResolved
import com.strumenta.kolasu.testing.assertReferencesResolved
import com.strumenta.kolasu.traversing.walkChildren
import org.junit.Test
import kotlin.test.assertTrue

class EntitySemanticsTest {
    private val parser: EntityParser = EntityParser()

    @Test
    fun testSymbolResolution() {
        val personModule =
            parser.parse(
                """
                module person
                
                import address
                
                entity Person {

                    firstname: String
                    lastname: String
                    address: Address
                    
                    describe(): String {
                        return firstname + " " + lastname + ", living in " + address.describe()
                    }
                    
                    clone(): Person {
                        let address: Address = address.clone()
                        return new Person(firstname, lastname, address)
                    }
                    
                }
                """.trimIndent(),
            ).root!!
        // address module is currently not included in the workspace
        // hence symbol resolution fails to resolve references to its symbols
        personModule.assertReferencesNotResolved(forProperty = Import::module)
        personModule.assertReferencesResolved(forProperty = Operation::type)
        personModule.assertReferencesResolved(forProperty = ConstructorExpression::entity)
        personModule.assertReferencesNotResolved(forProperty = Variable::type)
        personModule.assertSomeReferencesNotResolved(forProperty = Feature::type)
        personModule.assertSomeReferencesNotResolved(forProperty = OperationReference::operation)
        val addressModule =
            parser.parse(
                """
                module address
                
                entity Address {
                    
                    street: String
                    number: Integer
                    city: String                
                    
                    describe(): String {
                        return street + ", " + number + " (" + city + ")"
                    }
                    
                    clone(): Address {
                        return new Address(street, number, city)
                    }
                    
                }
                """.trimIndent(),
            ).root!!
        addressModule.assertReferencesResolved(forProperty = Feature::type)
        addressModule.assertReferencesResolved(forProperty = Operation::type)
        addressModule.assertReferencesResolved(forProperty = ReferenceExpression::target)
        addressModule.assertReferencesResolved(forProperty = ConstructorExpression::entity)
        personModule.assertReferencesResolved(forProperty = Import::module)
        personModule.assertReferencesResolved(forProperty = Feature::type)
        personModule.assertReferencesResolved(forProperty = OperationReference::operation)
    }
}

private fun Node.assertSomeReferencesNotResolved(forProperty: KReferenceByName<out Node>) {
    assertTrue { this.containsReferencesNotResolved(forProperty = forProperty) }
}

private fun Node.containsReferencesNotResolved(forProperty: KReferenceByName<out Node>): Boolean {
    return this.kReferenceByNameProperties()
        .filter { it == forProperty }
        .mapNotNull { it.get(this) }
        .takeIf { it.isNotEmpty() }
        ?.any { !(it as ReferenceByName<*>).resolved } ?: false ||
        this.walkChildren().any { it.containsReferencesNotResolved(forProperty = forProperty) }
}
