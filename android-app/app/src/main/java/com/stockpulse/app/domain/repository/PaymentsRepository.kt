package com.stockpulse.app.domain.repository

data class CreditPackage(
    val code: String,
    val name: String,
    val priceMxn: Double,
    val credits: Int
)

data class CheckoutLaunch(
    val sessionId: String,
    val checkoutUrl: String
)

data class CreditPurchaseResult(
    val paid: Boolean,
    val message: String,
    val creditsAdded: Int?,
    val newBalance: Double?
)

sealed interface PackagesResult {
    data class Success(val packages: List<CreditPackage>) : PackagesResult
    data class Failure(val message: String) : PackagesResult
}

sealed interface CheckoutResult {
    data class Success(val launch: CheckoutLaunch) : CheckoutResult
    data class Failure(val message: String) : CheckoutResult
}

sealed interface PurchaseStatusResult {
    data class Success(val result: CreditPurchaseResult) : PurchaseStatusResult
    data class Failure(val message: String) : PurchaseStatusResult
}

interface PaymentsRepository {
    suspend fun packages(): PackagesResult
    suspend fun checkout(packageCode: String): CheckoutResult
    suspend fun verify(sessionId: String): PurchaseStatusResult
}
