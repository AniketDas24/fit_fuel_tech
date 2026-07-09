package com.fitfuel.admin;

import java.math.BigDecimal;

record AdminStatsResponse(long todayOrderCount, BigDecimal todayRevenue, long pendingOrderCount,
                          long totalCustomers) {
}
