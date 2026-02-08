package com.aamsco.awardswithfriends.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "BillingRepository"
        const val COMPETITIONS_PRODUCT_ID = "com.awardswithfriends.competitions"
    }

    private val _hasCompetitionsAccess = MutableStateFlow(false)
    val hasCompetitionsAccess: StateFlow<Boolean> = _hasCompetitionsAccess.asStateFlow()

    private val _products = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products: StateFlow<List<ProductDetails>> = _products.asStateFlow()

    private val _isConnected = MutableStateFlow(false)

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _productPrice = MutableStateFlow<String?>(null)
    val productPrice: StateFlow<String?> = _productPrice.asStateFlow()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        Log.d(TAG, "Purchase updated: responseCode=${billingResult.responseCode}, message=${billingResult.debugMessage}")
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                Log.d(TAG, "Processing purchase: ${purchase.products}, state=${purchase.purchaseState}")
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "User cancelled the purchase")
        } else {
            Log.e(TAG, "Purchase failed: ${billingResult.debugMessage}")
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    init {
        Log.d(TAG, "Initializing BillingRepository")
        startConnection()
    }

    fun startConnection() {
        Log.d(TAG, "Starting billing connection...")
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                Log.d(TAG, "Billing setup finished: responseCode=${billingResult.responseCode}")
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _isConnected.value = true
                    queryProducts()
                    queryPurchases()
                } else {
                    Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                    _isLoading.value = false
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected")
                _isConnected.value = false
            }
        })
    }

    private fun queryProducts() {
        Log.d(TAG, "Querying products for: $COMPETITIONS_PRODUCT_ID")
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(COMPETITIONS_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, queryResult ->
            val productDetailsList = queryResult.productDetailsList
            Log.d(TAG, "Products query result: responseCode=${billingResult.responseCode}, products=${productDetailsList.size}")
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _products.value = productDetailsList
                if (productDetailsList.isEmpty()) {
                    Log.w(TAG, "No products found! Make sure product is set up in Google Play Console")
                } else {
                    productDetailsList.forEach { product ->
                        Log.d(TAG, "Found product: ${product.productId}, price=${product.oneTimePurchaseOfferDetails?.formattedPrice}")
                    }
                }
                // Update price
                productDetailsList
                    .find { it.productId == COMPETITIONS_PRODUCT_ID }
                    ?.oneTimePurchaseOfferDetails
                    ?.formattedPrice
                    ?.let { _productPrice.value = it }
            } else {
                Log.e(TAG, "Products query failed: ${billingResult.debugMessage}")
            }
            _isLoading.value = false
        }
    }

    private fun queryPurchases() {
        Log.d(TAG, "Querying existing purchases...")
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchasesList ->
            Log.d(TAG, "Purchases query result: responseCode=${billingResult.responseCode}, purchases=${purchasesList.size}")
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasAccess = purchasesList.any { purchase ->
                    val hasProduct = purchase.products.contains(COMPETITIONS_PRODUCT_ID)
                    val isPurchased = purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                    Log.d(TAG, "Purchase check: products=${purchase.products}, hasProduct=$hasProduct, isPurchased=$isPurchased")
                    hasProduct && isPurchased
                }
                Log.d(TAG, "Has competitions access: $hasAccess")
                _hasCompetitionsAccess.value = hasAccess

                // Acknowledge any unacknowledged purchases
                purchasesList.forEach { purchase ->
                    if (!purchase.isAcknowledged && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        Log.d(TAG, "Acknowledging purchase: ${purchase.products}")
                        handlePurchase(purchase)
                    }
                }
            } else {
                Log.e(TAG, "Purchases query failed: ${billingResult.debugMessage}")
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        Log.d(TAG, "Handling purchase: ${purchase.products}, state=${purchase.purchaseState}, acknowledged=${purchase.isAcknowledged}")
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (purchase.products.contains(COMPETITIONS_PRODUCT_ID)) {
                Log.d(TAG, "Granting competitions access")
                _hasCompetitionsAccess.value = true
            }

            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { result ->
                    Log.d(TAG, "Acknowledge result: ${result.responseCode}, ${result.debugMessage}")
                }
            }
        }
    }

    fun purchaseCompetitions(activity: Activity): Boolean {
        Log.d(TAG, "Starting purchase flow...")
        if (!_isConnected.value) {
            Log.w(TAG, "Not connected, reconnecting...")
            startConnection()
            return false
        }

        val productDetails = _products.value.find { it.productId == COMPETITIONS_PRODUCT_ID }
        if (productDetails == null) {
            Log.e(TAG, "Product not found! Products available: ${_products.value.map { it.productId }}")
            return false
        }

        Log.d(TAG, "Launching billing flow for: ${productDetails.productId}")
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)
        Log.d(TAG, "Billing flow result: ${billingResult.responseCode}")

        return billingResult.responseCode == BillingClient.BillingResponseCode.OK
    }

    fun restorePurchases(): Boolean {
        Log.d(TAG, "Restoring purchases...")
        queryPurchases()
        return _hasCompetitionsAccess.value
    }

    fun getCompetitionsProductPrice(): String? {
        return _products.value
            .find { it.productId == COMPETITIONS_PRODUCT_ID }
            ?.oneTimePurchaseOfferDetails
            ?.formattedPrice
    }
}
