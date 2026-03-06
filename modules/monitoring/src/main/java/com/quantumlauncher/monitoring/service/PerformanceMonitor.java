package com.quantumlauncher.monitoring.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Сервис мониторинга производительности системы
 */
@Service
public class PerformanceMonitor {
    
    private static final Logger log = LoggerFactory.getLogger(PerformanceMonitor.class);
    
    private final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    
    // История метрик
    private final Map<String, List<MetricSnapshot>> metricsHistory = new ConcurrentHashMap<>();
    private final int MAX_HISTORY_SIZE = 60; // 60 записей (5 минут при 5сек интервале)
    
    // Счётчик FPS для игры (если запущена)
    private final AtomicInteger currentFps = new AtomicInteger(0);
    private final Queue<Integer> fpsBuffer = new LinkedList<>();
    private static final int FPS_BUFFER_SIZE = 60;
    
    /**
     * Снэпшот метрик
     */
    public static class MetricSnapshot {
        public long timestamp;
        public double cpuUsage;
        public long usedMemory;
        public long maxMemory;
        public double memoryUsagePercent;
        public int threadCount;
        public double systemLoad;
        
        public MetricSnapshot() {
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    /**
     * Сбор метрик системы
     */
    public MetricSnapshot collectMetrics() {
        MetricSnapshot snapshot = new MetricSnapshot();
        
        try {
            // CPU Usage (системная нагрузка)
            snapshot.systemLoad = osBean.getSystemLoadAverage();
            
            // Memory Usage
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
            
            snapshot.usedMemory = heapUsage.getUsed() + nonHeapUsage.getUsed();
            snapshot.maxMemory = heapUsage.getMax();
            snapshot.memoryUsagePercent = (snapshot.usedMemory * 100.0) / snapshot.maxMemory;
            
            // Thread Count
            snapshot.threadCount = ManagementFactory.getThreadMXBean().getThreadCount();
            
        } catch (Exception e) {
            log.error("Ошибка сбора метрик", e);
        }
        
        return snapshot;
    }
    
    /**
     * Сбор метрик с сохранением в историю
     */
    @Scheduled(fixedRate = 5000) // каждые 5 секунд
    public void collectAndStoreMetrics() {
        MetricSnapshot snapshot = collectMetrics();
        
        // Добавляем в историю
        metricsHistory.computeIfAbsent("system", k -> new ArrayList<>())
            .add(snapshot);
        
        // Ограничиваем размер истории
        List<MetricSnapshot> history = metricsHistory.get("system");
        while (history.size() > MAX_HISTORY_SIZE) {
            history.remove(0);
        }
        
        // Логируем при высокой нагрузке
        if (snapshot.memoryUsagePercent > 90) {
            log.warn("Высокое использование памяти: {}%", 
                String.format("%.1f", snapshot.memoryUsagePercent));
        }
    }
    
    /**
     * Получение текущих метрик
     */
    public MetricSnapshot getCurrentMetrics() {
        return collectMetrics();
    }
    
    /**
     * Получение истории метрик
     */
    public List<MetricSnapshot> getMetricsHistory(String type) {
        return metricsHistory.getOrDefault(type, new ArrayList<>());
    }
    
    /**
     * Обновление FPS (вызывается из процесса игры)
     */
    public void updateFps(int fps) {
        currentFps.set(fps);
        
        synchronized (fpsBuffer) {
            fpsBuffer.add(fps);
            if (fpsBuffer.size() > FPS_BUFFER_SIZE) {
                fpsBuffer.poll();
            }
        }
    }
    
    /**
     * Получение среднего FPS
     */
    public double getAverageFps() {
        synchronized (fpsBuffer) {
            if (fpsBuffer.isEmpty()) return 0;
            
            int sum = 0;
            for (int fps : fpsBuffer) {
                sum += fps;
            }
            return (double) sum / fpsBuffer.size();
        }
    }
    
    /**
     * Получение текущего FPS
     */
    public int getCurrentFps() {
        return currentFps.get();
    }
    
    /**
     * Предсказание FPS на основе текущих метрик
     */
    public String predictPerformance() {
        MetricSnapshot current = getCurrentMetrics();
        
        // Простая эвристика предсказания
        if (current.memoryUsagePercent > 90) {
            return "LOW - Высокое использование памяти. Рекомендуется увеличить RAM.";
        }
        
        if (current.systemLoad > osBean.getAvailableProcessors()) {
            return "MEDIUM - Высокая нагрузка на систему.";
        }
        
        if (current.memoryUsagePercent > 75) {
            return "MEDIUM - Умеренное использование памяти.";
        }
        
        return "HIGH - Система в хорошем состоянии для игры.";
    }
    
    /**
     * Получение рекомендаций по оптимизации
     */
    public List<String> getOptimizationRecommendations() {
        List<String> recommendations = new ArrayList<>();
        MetricSnapshot current = getCurrentMetrics();
        
        // Memory рекомендации
        if (current.memoryUsagePercent > 80) {
            recommendations.add("Увеличьте выделенную RAM в настройках инстанса");
            recommendations.add("Уменьшите количество установленных модов");
        }
        
        // Thread рекомендации
        if (current.threadCount > 200) {
            recommendations.add("Обнаружено много потоков. Возможна утечка памяти.");
        }
        
        // CPU рекомендации
        if (current.systemLoad > osBean.getAvailableProcessors() * 0.8) {
            recommendations.add("Высокая нагрузка CPU. Закройте другие приложения.");
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("Система работает оптимально ✓");
        }
        
        return recommendations;
    }
    
    /**
     * Очистка истории метрик
     */
    public void clearHistory() {
        metricsHistory.clear();
        log.info("История метрик очищена");
    }
    
    /**
     * Получение информации о системе
     */
    public Map<String, Object> getSystemInfo() {
        Map<String, Object> info = new HashMap<>();
        
        info.put("osName", osBean.getName());
        info.put("osVersion", osBean.getVersion());
        info.put("osArch", osBean.getArch());
        info.put("availableProcessors", osBean.getAvailableProcessors());
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("javaVendor", System.getProperty("java.vendor"));
        
        return info;
    }
}
