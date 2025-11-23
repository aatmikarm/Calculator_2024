package com.aatmik.calculator.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object SubscriptionManager {
    private const val TAG = "SubscriptionManager"
    private const val SUBSCRIPTION_PRODUCT_ID = "monthly_subscription" // Monthly subscription plan
    private const val PREFS_NAME = "subscription_prefs"
    private const val KEY_IS_PREMIUM = "is_premium"

    private var billingClient: BillingClient? = null
    private var isPremiumUser = false

    /**
     * Initialize billing client - Call this in Application onCreate
     */
    fun init(context: Context) {
        // Load saved premium status immediately
        loadPremiumStatus(context)

        billingClient = BillingClient.newBuilder(context)
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    for (purchase in purchases) {
                        handlePurchase(context, purchase)
                    }
                }
            }
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()

        // Connect and check subscription status
        connectAndCheckSubscription(context)
    }

    /**
     * Load premium status from SharedPreferences
     */
    private fun loadPremiumStatus(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isPremiumUser = prefs.getBoolean(KEY_IS_PREMIUM, false)

        if (isPremiumUser) {
            AdConfig.disableAds()
            Log.d(TAG, "✅ Premium status loaded from prefs - Ads disabled")
        }
    }

    /**
     * Save premium status to SharedPreferences
     */
    private fun savePremiumStatus(context: Context, isPremium: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS_PREMIUM, isPremium).apply()
        isPremiumUser = isPremium

        if (isPremium) {
            AdConfig.disableAds()
            Log.d(TAG, "✅ Premium status saved - Ads disabled")
        } else {
            AdConfig.enableAds()
            Log.d(TAG, "❌ Premium status removed - Ads enabled")
        }
    }

    /**
     * Connect to Google Play and check if user has active subscription
     */
    private fun connectAndCheckSubscription(context: Context) {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing client connected successfully")
                    checkSubscriptionStatus(context)
                } else {
                    Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.d(TAG, "Billing service disconnected")
                // Try to reconnect after 3 seconds
                CoroutineScope(Dispatchers.Main).launch {
                    kotlinx.coroutines.delay(3000)
                    connectAndCheckSubscription(context)
                }
            }
        })
    }

    /**
     * Check if user has active subscription - Works after reinstall!
     */
    private fun checkSubscriptionStatus(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val params = QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()

                val purchasesResult = billingClient?.queryPurchasesAsync(params)

                var hasActiveSubscription = false

                purchasesResult?.purchasesList?.forEach { purchase ->
                    if (purchase.products.contains(SUBSCRIPTION_PRODUCT_ID) &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {

                        hasActiveSubscription = true
                        Log.d(TAG, "✅ User has active subscription!")

                        // Acknowledge purchase if not already
                        if (!purchase.isAcknowledged) {
                            acknowledgePurchase(purchase)
                        }
                    }
                }

                // Update premium status
                withContext(Dispatchers.Main) {
                    savePremiumStatus(context, hasActiveSubscription)
                }

                if (!hasActiveSubscription) {
                    Log.d(TAG, "❌ No active subscription found")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error checking subscription: ${e.message}")
            }
        }
    }

    /**
     * Start subscription purchase flow
     */
    fun startSubscriptionPurchase(activity: Activity, onError: ((String) -> Unit)? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val productList = listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(SUBSCRIPTION_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )

                val params = QueryProductDetailsParams.newBuilder()
                    .setProductList(productList)
                    .build()

                val productDetailsResult = withContext(Dispatchers.IO) {
                    billingClient?.queryProductDetails(params)
                }

                val productDetails = productDetailsResult?.productDetailsList?.firstOrNull()

                if (productDetails != null) {
                    withContext(Dispatchers.Main) {
                        launchPurchaseFlow(activity, productDetails)
                    }
                } else {
                    Log.e(TAG, "Product details not found")
                    withContext(Dispatchers.Main) {
                        onError?.invoke("Product not available. Please try again later.")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error starting purchase: ${e.message}")
                withContext(Dispatchers.Main) {
                    onError?.invoke("Failed to start purchase: ${e.message}")
                }
            }
        }
    }

    /**
     * Launch the purchase dialog
     */
    private fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails) {
        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken

        if (offerToken != null) {
            val productDetailsParamsList = listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)
                    .build()
            )

            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()

            billingClient?.launchBillingFlow(activity, billingFlowParams)

            // Log purchase attempt
            AnalyticsManager.log("subscription_purchase_started")
        }
    }

    /**
     * Handle purchase - called automatically by Google Play
     */
    private fun handlePurchase(context: Context, purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                acknowledgePurchase(purchase)
            }

            savePremiumStatus(context, true)
            Log.d(TAG, "✅ Purchase successful! User is now premium")

            // Log successful purchase
            AnalyticsManager.log("subscription_purchased")
        }
    }

    /**
     * Acknowledge the purchase
     */
    private fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                billingClient?.acknowledgePurchase(acknowledgePurchaseParams)
                Log.d(TAG, "Purchase acknowledged")
            } catch (e: Exception) {
                Log.e(TAG, "Error acknowledging purchase: ${e.message}")
            }
        }
    }

    /**
     * Check if user is premium (has active subscription)
     */
    fun isPremium(): Boolean {
        return isPremiumUser
    }

    /**
     * Manually refresh subscription status
     */
    fun refreshSubscriptionStatus(context: Context) {
        if (billingClient?.isReady == true) {
            checkSubscriptionStatus(context)
        } else {
            connectAndCheckSubscription(context)
        }
    }
}