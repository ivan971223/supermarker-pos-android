package com.store88.pos.data


import com.store88.pos.domain.AppState
import com.store88.pos.domain.CartLine
import com.store88.pos.domain.Lot
import com.store88.pos.domain.PosConstants
import com.store88.pos.domain.Product
import com.store88.pos.domain.Promo
import com.store88.pos.domain.ReceiptConfig
import java.time.LocalDate

object SeedData {
    private fun daysFromNow(n: Int): String = LocalDate.now().plusDays(n.toLong()).toString()

    private val catalog: List<Product> = listOf(
        Product("p1", "Coca-Cola 330ml", "可口可樂 330毫升", "4890008000510", "drinks", 4.0, 8.0, 120, 24, false, "🥤"),
        Product("p2", "Sprite 500ml", "雪碧 500毫升", "4890001000028", "drinks", 4.0, 8.0, 95, 24, false, "🥤"),
        Product("p3", "Vita Lemon Tea 500ml", "維他檸檬茶 500毫升", "4892388882007", "drinks", 4.0, 7.5, 80, 20, false, "🧃"),
        Product("p4", "Bonaqua Water 1.5L", "飛雪礦泉水 1.5公升", "4890001000066", "drinks", 2.5, 6.0, 150, 36, false, "💧"),
        Product("p5", "Ito En Green Tea 500ml", "伊藤園綠茶 500毫升", "4890001000073", "drinks", 4.2, 7.5, 60, 18, false, "🍵"),
        Product("p6", "Lay's Classic 184g", "樂事薯片 原味 184克", "4890002000018", "snacks", 8.0, 15.0, 55, 15, false, "🥔"),
        Product("p7", "Doritos Nacho Cheese 198g", "多力多滋 芝士味 198克", "4891671175234", "snacks", 8.0, 15.0, 40, 12, false, "🌮"),
        Product("p8", "Pocky Chocolate 47g", "百奇朱古力味 47克", "4890002000032", "snacks", 5.0, 9.5, 48, 12, false, "🍫"),
        Product("p9", "Haagen-Dazs Strawberry 100ml", "哈根達斯 士多啤梨 100毫升", "4890004000029", "frozen", 20.0, 35.0, 24, 6, true, "🍦"),
        Product("p10", "Andrex Toilet Tissue 3 Ply 10 Rolls", "安力士三層廁紙 10卷裝", "4891028001376", "household", 25.0, 42.9, 30, 8, false, "🧻"),
        Product("p11", "Vitasoy 250ml", "維他奶 250毫升", "4890001000042", "drinks", 3.2, 6.5, 70, 20, true, "🥛"),
        Product("p12", "Pocari Sweat 500ml", "寶礦力 500毫升", "4890001000059", "drinks", 5.5, 11.0, 45, 12, false, "💦"),
        Product("p13", "Fanta Orange 330ml", "芬達橙汁汽水 330毫升", "4890001000103", "drinks", 3.8, 7.5, 88, 20, false, "🍊"),
        Product("p14", "Red Bull Energy 250ml", "紅牛能量飲品 250毫升", "90162602", "drinks", 8.5, 14.5, 36, 12, false, "🔋"),
        Product("p15", "Yakult Probiotics 5s", "益力多 5支裝", "4891028700018", "dairy", 9.0, 15.5, 42, 15, true, "🥛"),
        Product("p16", "Meiji Fresh Milk 1L", "明治鮮牛奶 1公升", "4891028701107", "dairy", 16.0, 26.9, 28, 10, true, "🥛"),
        Product("p17", "Calbee Hot & Spicy 80g", "卡樂B熱浪薯片 80克", "4890002000100", "snacks", 6.5, 12.5, 50, 15, false, "🌶️"),
        Product("p18", "Oreo Original 137g", "奧利奧原味夾心餅 137克", "7622210123456", "snacks", 8.0, 14.9, 33, 12, false, "🍪"),
        Product("p19", "KitKat 4 Finger", "奇巧朱古力 4條裝", "3800020417504", "snacks", 5.5, 9.9, 60, 18, false, "🍫"),
        Product("p20", "Pringles Original 149g", "品客原味薯片 149克", "038000845407", "snacks", 12.0, 22.9, 22, 10, false, "🥔"),
        Product("p21", "Nissin Demae Ramen Spicy", "日清出前一丁麻油味", "4891028710017", "instant", 3.2, 6.5, 95, 30, false, "🍜"),
        Product("p22", "Indomie Mi Goreng 5s", "營多撈麵 5包裝", "8998866100467", "instant", 12.0, 19.9, 9, 15, false, "🍜"),
        Product("p23", "Magnum Classic 110ml", "夢龍雪糕經典味 110毫升", "8711327370100", "frozen", 14.0, 25.0, 18, 8, true, "🍦"),
        Product("p24", "Birds Eye Garden Peas 500g", "鳥眼青豆 500克", "5000116123456", "frozen", 11.0, 19.5, 26, 8, true, "🫛"),
        Product("p25", "CP Chicken Nuggets 400g", "正大雞塊 400克", "8850123456789", "frozen", 18.0, 32.9, 14, 6, true, "🍗"),
        Product("p26", "Fresh Eggs Grade A 10pcs", "新鮮雞蛋 A級 10隻", "4890003000011", "fresh", 18.0, 28.9, 40, 12, true, "🥚"),
        Product("p27", "Banana per kg", "香蕉 每公斤", "2000000000271", "fresh", 8.0, 14.9, 35, 10, true, "🍌"),
        Product("p28", "Tomato Tray 500g", "番茄 盒裝 500克", "4890003000028", "fresh", 9.0, 16.5, 7, 10, true, "🍅"),
        Product("p29", "Pak Choi Bundle", "白菜苗 一紮", "4890003000035", "fresh", 5.0, 9.9, 20, 8, true, "🥬"),
        Product("p30", "Fairy Dishwashing Liquid 450ml", "飛碟洗潔精 450毫升", "8001090123456", "household", 14.0, 24.9, 31, 10, false, "🫧"),
        Product("p31", "Softlan Softener Blue 800ml", "柔麗衣物柔順劑藍色 800毫升", "8850006310010", "household", 16.0, 29.9, 19, 8, false, "🧴"),
        Product("p32", "Colgate Total Toothpaste 150g", "高露潔全效牙膏 150克", "8850001234567", "personal", 15.0, 27.5, 5, 10, false, "🪥"),
        Product("p33", "Golden Elephant Thai Rice 5kg", "金象泰國香米 5公斤", "8850123456790", "rice_oil", 48.0, 78.0, 22, 6, false, "🍚"),
        Product("p34", "Knife Cooking Oil 900ml", "刀嘜食用油 900毫升", "4891028000888", "rice_oil", 18.0, 32.9, 35, 10, false, "🫒"),
        Product("p35", "Lee Kum Kee Premium Soy Sauce 500ml", "李錦記特級豉油 500毫升", "4891028000999", "rice_oil", 12.0, 22.5, 40, 12, false, "🧂"),
        Product("p36", "Nissin Cup Noodles Seafood", "合味道杯麵海鮮味", "4891028710888", "instant", 4.5, 8.9, 60, 20, false, "🍜"),
        Product("p37", "Safeguard Soap White 115g", "舒膚佳香皂白色 115克", "6903148000123", "personal", 5.0, 9.9, 48, 15, false, "🧼"),
    )

    private val customPriceProducts: List<Product> = (1..50).map { n ->
        Product(
            id = "custom-$n",
            nameEn = "$$n",
            nameZh = "${n}元",
            barcode = "OPEN%03d".format(n),
            category = "custom",
            cost = 0.0,
            price = n.toDouble(),
            stock = 99999,
            reorderMin = 0,
            trackExpiry = false,
            emoji = "💲",
        )
    }

    fun createSeedState(): AppState = AppState(
        version = 7,
        shopName = "88 Store",
        shopNameZh = "88超市",
        uiLanguage = "both",
        categories = PosConstants.DEFAULT_CATEGORIES,
        favouriteIds = listOf("p1", "p3", "p11", "p21"),
        products = catalog + customPriceProducts,
        lots = listOf(
            Lot("l1", "p9", "ICE-H01", 24, daysFromNow(60)),
            Lot("l2", "p11", "VSOY-E01", 70, daysFromNow(40)),
        ),
        promos = listOf(
            Promo(
                id = "pr1",
                name = "Buy 2 Drinks 10% Off / 買兩支飲品九折",
                type = "buy_x_percent",
                buyQty = 2,
                discountPercent = 10.0,
                categoryId = "drinks",
                startDate = daysFromNow(-30),
                endDate = daysFromNow(60),
                active = false,
            ),
            Promo(
                id = "pr2",
                name = "3 for $20.00 Snacks / 3件$20.00 零食",
                type = "buy_x_for_price",
                buyQty = 3,
                bundlePrice = 20.0,
                categoryId = "snacks",
                startDate = daysFromNow(-30),
                endDate = daysFromNow(60),
                active = true,
            ),
        ),
        sales = emptyList(),
        held = emptyList(),
        session = null,
        cart = listOf(
            CartLine("p1", 1),
            CartLine("p3", 2),
            CartLine("p7", 1),
            CartLine("p10", 1),
        ),
        cartDiscountPercent = 0.0,
        offlineQueue = emptyList(),
        lastSyncAt = null,
        receiptConfig = ReceiptConfig(),
    )
}
