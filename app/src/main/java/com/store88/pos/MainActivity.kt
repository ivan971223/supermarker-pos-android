package com.store88.pos

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.store88.pos.domain.Screen
import com.store88.pos.ui.admin.CategoriesAdmin
import com.store88.pos.ui.admin.ExpiryAdmin
import com.store88.pos.ui.admin.InventoryAdmin
import com.store88.pos.ui.admin.PrinterSettingsAdmin
import com.store88.pos.ui.admin.ProductsAdmin
import com.store88.pos.ui.admin.PromosAdmin
import com.store88.pos.ui.admin.ReceiptDesignAdmin
import com.store88.pos.ui.admin.ReceiveAdmin
import com.store88.pos.ui.admin.ReportsAdmin
import com.store88.pos.ui.admin.TransactionsAdmin
import com.store88.pos.ui.checkout.CheckoutScreen
import com.store88.pos.ui.common.FlashBanner
import com.store88.pos.ui.customer.CustomerDisplayController
import com.store88.pos.ui.dayclose.DayCloseScreen
import com.store88.pos.ui.login.LoginScreen
import com.store88.pos.ui.payment.PaymentScreen
import com.store88.pos.ui.theme.Store88Theme
import com.store88.pos.ui.theme.Teal
import com.store88.pos.ui.theme.TealBg

class MainActivity : ComponentActivity() {
    private val vm: PosViewModel by viewModels()
    private var customerDisplay: CustomerDisplayController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        customerDisplay = CustomerDisplayController(
            context = this,
            lifecycleOwner = this,
            savedStateOwner = this,
            uiFlow = vm.ui,
            scope = lifecycleScope,
        ).also { it.start() }

        setContent {
            Store88Theme {
                val ui by vm.ui.collectAsState()
                Box(Modifier.fillMaxSize().safeDrawingPadding().background(TealBg)) {
                    if (!ui.ready) {
                        CircularProgressIndicator(Modifier.align(Alignment.Center), color = Teal)
                    } else {
                        when (ui.screen) {
                            Screen.Login -> LoginScreen(ui, vm)
                            Screen.Checkout -> CheckoutScreen(ui, vm)
                            Screen.Payment -> PaymentScreen(ui, vm)
                            Screen.DayClose -> DayCloseScreen(ui, vm)
                            Screen.Products -> ProductsAdmin(ui, vm)
                            Screen.Categories -> CategoriesAdmin(ui, vm)
                            Screen.Promotions -> PromosAdmin(ui, vm)
                            Screen.Inventory -> InventoryAdmin(ui, vm)
                            Screen.Expiry -> ExpiryAdmin(ui, vm)
                            Screen.Receive -> ReceiveAdmin(ui, vm)
                            Screen.Reports -> ReportsAdmin(ui, vm)
                            Screen.Transactions -> TransactionsAdmin(ui, vm)
                            Screen.ReceiptDesign -> ReceiptDesignAdmin(ui, vm)
                            Screen.PrinterSettings -> PrinterSettingsAdmin(ui, vm)
                        }
                    }
                    ui.flash?.let { msg ->
                        FlashBanner(
                            message = msg,
                            onDismiss = vm::clearFlash,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 20.dp),
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        customerDisplay?.refresh()
    }

    override fun onDestroy() {
        customerDisplay?.stop()
        customerDisplay = null
        super.onDestroy()
    }
}
