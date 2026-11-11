package com.clinic.scheduling.client;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// ConcurrentMapCacheManager không có hạn dùng: xóa định kỳ để ca vừa đổi / tắt bên Provider có hiệu lực sau tối đa một chu kỳ
@Component
public class WorkingHoursCacheEvictor {

    @Scheduled(fixedRateString = "${clinic.cache.working-hours-ttl:30s}")
    @CacheEvict(value = ProviderClient.WORKING_HOURS_CACHE, allEntries = true)
    public void evictAll() {
    }
}
