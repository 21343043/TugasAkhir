package com.fadhil.financereceipt.ui.format

import java.math.BigInteger
import java.text.NumberFormat
import java.util.Locale

fun planRupiah(amount: BigInteger): String =
    "Rp" + NumberFormat.getIntegerInstance(Locale.forLanguageTag("id-ID")).format(amount)
