package com.strumenta.entity.parser.semantics

import com.strumenta.entity.parser.EntityParser
import com.strumenta.entity.parser.ast.ConstructorExpression
import com.strumenta.entity.parser.ast.Feature
import com.strumenta.entity.parser.ast.Import
import com.strumenta.entity.parser.ast.InvocationExpression
import com.strumenta.entity.parser.ast.Module
import com.strumenta.entity.parser.ast.Operation
import com.strumenta.entity.parser.ast.ReferenceExpression
import com.strumenta.kolasu.model.Expression
import com.strumenta.kolasu.model.KReferenceByName
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.model.kReferenceByNameProperties
import com.strumenta.kolasu.testing.assertReferencesResolved
import com.strumenta.kolasu.traversing.walkChildren
import com.strumenta.kolasu.traversing.walkDescendants
import org.junit.Test
import kotlin.test.assertTrue
import kotlin.test.fail

class EntitySemanticsTest {
    private val parser: EntityParser = EntityParser()

    @Test
    fun resolveInvocations() {
        val simpleModuleFinder = SimpleModuleFinder()
        val animalsModule =
            parser.parse(
                """
                module animals
                
                entity Dog {
                    move(): Integer {
                        return 2
                    }
                }
                
                entity FastDog {
                    move(): Integer {
                        return new Dog().move() * 2
                    }
                }               
                """.trimIndent(),
            ).root!! as Module
        simpleModuleFinder.registerModule(animalsModule)
        animalsModule.semanticEnrichment(simpleModuleFinder)
        animalsModule.assertReferencesResolved(forProperty = InvocationExpression::operation)
    }

    @Test
    fun testSymbolResolution() {
        val simpleModuleFinder = SimpleModuleFinder()
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
            ).root!! as Module
        simpleModuleFinder.registerModule(personModule)
        try {
            personModule.semanticEnrichment(simpleModuleFinder)
            fail()
        } catch (e: IllegalStateException) {
            // Expected
        }

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
            ).root!! as Module
        simpleModuleFinder.registerModule(addressModule)
        personModule.semanticEnrichment(simpleModuleFinder)
        addressModule.semanticEnrichment(simpleModuleFinder)
        addressModule.assertReferencesResolved(forProperty = Feature::type)
        addressModule.assertReferencesResolved(forProperty = Operation::type)
        addressModule.assertReferencesResolved(forProperty = ReferenceExpression::target)
        addressModule.assertReferencesResolved(forProperty = ConstructorExpression::entity)
        personModule.assertReferencesResolved(forProperty = Import::module)
        personModule.assertReferencesResolved(forProperty = Feature::type)
        personModule.assertReferencesResolved(forProperty = InvocationExpression::operation)
    }

    @Test
    fun testTypeCalculation() {
        val simpleModuleFinder = SimpleModuleFinder()
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
                        let address = address.clone()
                        return new Person(firstname, lastname, address)
                    }
                    
                }
                """.trimIndent(),
            ).root!! as Module
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
            ).root!! as Module
        simpleModuleFinder.registerModule(addressModule)
        simpleModuleFinder.registerModule(personModule)
        addressModule.semanticEnrichment(simpleModuleFinder)
        personModule.semanticEnrichment(simpleModuleFinder)

        personModule.assertAllExpressionsHaveType()
        addressModule.assertAllExpressionsHaveType()
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

private fun Node.assertAllExpressionsHaveType() {
    val expressionsWithoutType = this.walkDescendants(com.strumenta.entity.parser.ast.Expression::class).filter { it.type == null }.toList()
    assert(expressionsWithoutType.isEmpty()) {
        "Some expressions have no type: ${expressionsWithoutType.joinToString(", ")}"
    }
}
