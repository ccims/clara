package de.unistuttgart.iste.sqa.clara.utils.list

import arrow.core.Either
import arrow.core.right

fun <A, B> List<Either<A, Iterable<B>>>.flattenRight(): List<Either<A, B>> {
    return buildList {
        for (either in this@flattenRight) {
            when (either) {
                is Either.Left -> add(either)
                is Either.Right -> addAll(either.value.map { it.right() })
            }
        }
    }
}

fun <A, B> List<Either<A, B>>.getLeft(): List<A> {
    return buildList {
        for (either in this@getLeft) {
            when (either) {
                is Either.Left -> add(either.value)
                is Either.Right -> {}
            }
        }
    }
}

fun <A, B> List<Either<A, B>>.getRight(): List<B> {
    return buildList {
        for (either in this@getRight) {
            when (either) {
                is Either.Left -> {}
                is Either.Right -> add(either.value)
            }
        }
    }
}
