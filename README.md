# raire-kotlin

This is a kotlin port of [raire-java](https://github.com/DemocracyDevelopers/raire-java) 
mostly as a way of learning raire. 

Also a partial port of [raire-servoce](https://github.com/DemocracyDevelopers/raire-service) 
mostly to have a consumer of raire-java.

Notable Changes
* Replace com.fasterxml.jackson with libs.ktor.serialization for JSON serialization.
* Serialization is identical except for Map<String, Any>
* Replace RaireResultOrError with Result<RaireResult, RaireError> from com.michael-bull.kotlin-result.
* Minor refactoring for idiomatic kotlin.
* The two libraries should perform identically.