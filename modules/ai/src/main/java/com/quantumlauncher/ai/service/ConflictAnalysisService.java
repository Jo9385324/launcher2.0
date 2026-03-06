package com.quantumlauncher.ai.service;

import com.quantumlauncher.core.model.Mod;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * AI Сервис для анализа конфликтов между модами
 * 
 * Использует графовый анализ для выявления конфликтов
 */
@Service
public class ConflictAnalysisService {
    
    private static final Logger log = LoggerFactory.getLogger(ConflictAnalysisService.class);
    
    /**
     * Результат анализа конфликтов
     */
    public static class ConflictResult {
        public boolean hasConflicts;
        public List<Conflict> conflicts;
        public Map<String, List<String>> modDependencies;
        public List<String> recommendations;
        
        public ConflictResult() {
            this.conflicts = new ArrayList<>();
            this.modDependencies = new HashMap<>();
            this.recommendations = new ArrayList<>();
        }
    }
    
    /**
     * Информация о конфликте
     */
    public static class Conflict {
        public String mod1;
        public String mod2;
        public String reason;
        public ConflictType type;
        
        public Conflict(String mod1, String mod2, String reason, ConflictType type) {
            this.mod1 = mod1;
            this.mod2 = mod2;
            this.reason = reason;
            this.type = type;
        }
    }
    
    public enum ConflictType {
        CLASS_CONFLICT,      // Конфликт классов
        DEPENDENCY_CONFLICT, // Конфликт зависимостей
        VERSION_CONFLICT,    // Конфликт версий
        INCOMPATIBLE         // Несовместимость
    }
    
    /**
     * Анализ конфликтов между модами
     */
    public ConflictResult analyzeConflicts(List<Mod> mods) {
        log.info("Начинаем анализ конфликтов для {} модов", mods.size());
        
        ConflictResult result = new ConflictResult();
        
        if (mods == null || mods.size() < 2) {
            result.hasConflicts = false;
            return result;
        }
        
        // Построение графа зависимостей
        Graph<String, DefaultEdge> dependencyGraph = buildDependencyGraph(mods, result);
        
        // Поиск циклических зависимостей
        findCyclicDependencies(dependencyGraph, result);
        
        // Анализ известных конфликтов
        analyzeKnownConflicts(mods, result);
        
        // Генерация рекомендаций
        generateRecommendations(result);
        
        result.hasConflicts = !result.conflicts.isEmpty();
        
        log.info("Анализ завершён. Найдено {} конфликтов", result.conflicts.size());
        return result;
    }
    
    /**
     * Построение графа зависимостей
     */
    private Graph<String, DefaultEdge> buildDependencyGraph(List<Mod> mods, ConflictResult result) {
        Graph<String, DefaultEdge> graph = new SimpleGraph<>(DefaultEdge.class);
        
        // Добавление всех модов как вершин графа
        for (Mod mod : mods) {
            graph.addVertex(mod.getName());
            result.modDependencies.put(mod.getName(), new ArrayList<>());
        }
        
        // Анализ зависимостей (упрощённая версия)
        // В реальной реализации здесь был бы парсинг MANIFEST.MF или метаданных
        for (Mod mod : mods) {
            List<String> deps = analyzeModDependencies(mod);
            result.modDependencies.put(mod.getName(), deps);
            
            for (String dep : deps) {
                if (graph.containsVertex(dep)) {
                    graph.addEdge(mod.getName(), dep);
                }
            }
        }
        
        return graph;
    }
    
    /**
     * Анализ зависимостей мода (упрощённый)
     */
    private List<String> analyzeModDependencies(Mod mod) {
        List<String> dependencies = new ArrayList<>();
        
        // Здесь должен быть парсинг метаданных мода
        // Для демонстрации возвращаем пустой список
        
        // Пример анализа по названию (очень упрощённо)
        String name = mod.getName().toLowerCase();
        
        if (name.contains("jei")) {
            dependencies.add("modmenu");
        }
        if (name.contains("rei")) {
            dependencies.add("modmenu");
        }
        if (name.contains("roughly")) {
            dependencies.add("java");
        }
        
        return dependencies;
    }
    
    /**
     * Поиск циклических зависимостей
     */
    private void findCyclicDependencies(Graph<String, DefaultEdge> graph, ConflictResult result) {
        // DFS для поиска циклов
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        
        for (String vertex : graph.vertexSet()) {
            if (detectCycle(vertex, graph, visited, recursionStack, new ArrayList<>(), result)) {
                // Цикл найден, уже добавлен в result
            }
        }
    }
    
    private boolean detectCycle(String vertex, Graph<String, DefaultEdge> graph,
                                 Set<String> visited, Set<String> recursionStack,
                                 List<String> path, ConflictResult result) {
        
        if (recursionStack.contains(vertex)) {
            // Найден цикл
            int cycleStart = path.indexOf(vertex);
            if (cycleStart >= 0) {
                List<String> cycle = path.subList(cycleStart, path.size());
                cycle.add(vertex);
                
                result.conflicts.add(new Conflict(
                    cycle.get(0),
                    cycle.get(cycle.size() - 2),
                    "Циклическая зависимость: " + String.join(" -> ", cycle),
                    ConflictType.DEPENDENCY_CONFLICT
                ));
            }
            return true;
        }
        
        if (visited.contains(vertex)) {
            return false;
        }
        
        visited.add(vertex);
        recursionStack.add(vertex);
        path.add(vertex);
        
        for (DefaultEdge edge : graph.edgesOf(vertex)) {
            String neighbor = graph.getEdgeTarget(edge);
            if (neighbor.equals(vertex)) {
                neighbor = graph.getEdgeSource(edge);
            }
            
            if (detectCycle(neighbor, graph, visited, recursionStack, new ArrayList<>(path), result)) {
                return true;
            }
        }
        
        recursionStack.remove(vertex);
        return false;
    }
    
    /**
     * Анализ известных конфликтов
     */
    private void analyzeKnownConflicts(List<Mod> mods, ConflictResult result) {
        Set<String> modNames = new HashSet<>();
        for (Mod mod : mods) {
            modNames.add(mod.getName().toLowerCase());
        }
        
        // Известные конфликты JEI/REI/EMI
        int recipeViewers = 0;
        if (modNames.contains("jei") || modNames.contains("just enough items")) recipeViewers++;
        if (modNames.contains("rei") || modNames.contains("roughly enough items")) recipeViewers++;
        if (modNames.contains("emi") || modNames.contains("emi")) recipeViewers++;
        
        if (recipeViewers > 1) {
            result.conflicts.add(new Conflict(
                "JEI/REI/EMI",
                "Multiple Recipe Viewers",
                "Обнаружено несколько модов просмотра рецептов. Рекомендуется использовать только один.",
                ConflictType.INCOMPATIBLE
            ));
        }
        
        // Конфликт OptiFine и некоторых модов
        if (modNames.contains("optifine")) {
            if (modNames.contains("sodium") || modNames.contains("rubidium")) {
                result.conflicts.add(new Conflict(
                    "OptiFine",
                    "Sodium/Rubidium",
                    "OptiFine несовместим с Sodium. Используйте только один мод оптимизации.",
                    ConflictType.INCOMPATIBLE
                ));
            }
        }
        
        // Конфликт версий модов
        for (Mod mod : mods) {
            if (mod.getVersion() == null || mod.getVersion().isEmpty()) {
                result.recommendations.add("У мода '" + mod.getName() + "' не указана версия");
            }
        }
    }
    
    /**
     * Генерация рекомендаций
     */
    private void generateRecommendations(ConflictResult result) {
        if (result.conflicts.isEmpty()) {
            result.recommendations.add("Конфликтов не обнаружено ✓");
            return;
        }
        
        // Проверка наличия важных модов
        Set<String> allMods = result.modDependencies.keySet();
        
        if (!allMods.stream().anyMatch(m -> m.toLowerCase().contains("jei") || 
                                            m.toLowerCase().contains("rei") ||
                                            m.toLowerCase().contains("emi"))) {
            result.recommendations.add("Рекомендуется установить мод просмотра рецептов (JEI/REI/EMI)");
        }
        
        if (!allMods.contains("modmenu")) {
            result.recommendations.add("Рекомендуется установить ModMenu для управления модами");
        }
    }
}
