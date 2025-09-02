package com.example.todo.controller;

import com.example.todo.model.Priority;
import com.example.todo.model.Todo;
import com.example.todo.repository.TodoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Controller
public class TodoController {

    private final TodoRepository repository;

    public TodoController(TodoRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/")
    public String index(@RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "sortBy", defaultValue = "custom") String sortBy,
                        Model model) {
        try {
            List<Todo> todos;
            
            if ("custom".equals(sortBy)) {
                // Drag & Drop: uncompleted tasks first, then completed
                todos = repository.findAllByOrderByCompletedAscDisplayOrderAscCreatedAtDesc();
            } else {
                // Other sorting options
                todos = repository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
                todos.sort(Comparator
                        .comparing(Todo::isCompleted)
                        .thenComparing(todo -> todo.getPriority().ordinal())
                        .thenComparing(Todo::getCreatedAt, Comparator.reverseOrder()));
            }
            
            model.addAttribute("todos", todos);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("error", error);
        } catch (Exception e) {
            model.addAttribute("todos", List.of());
            model.addAttribute("error", "Error loading tasks: " + e.getMessage());
        }
        return "index";
    }

    @PostMapping("/todos")
    public String create(@RequestParam("title") String title,
                         @RequestParam(value = "description", required = false) String description,
                         @RequestParam(value = "priority", defaultValue = "LOW") String priority,
                         RedirectAttributes ra) {
        try {
            if (title == null || title.trim().isEmpty()) {
                ra.addAttribute("error", "Title cannot be empty");
                return "redirect:/";
            }
            Todo t = new Todo();
            t.setTitle(title.trim());
            if (description != null && !description.trim().isEmpty()) {
                t.setDescription(description.trim());
            }
            try {
                t.setPriority(Priority.valueOf(priority.toUpperCase()));
            } catch (IllegalArgumentException e) {
                t.setPriority(Priority.LOW);
            }
            
            // Set order value for a new task
            Integer maxOrder = repository.findMaxDisplayOrderByCompleted(false);
            t.setDisplayOrder(maxOrder != null ? maxOrder + 1 : 0);
            
            repository.save(t);
        } catch (Exception e) {
            ra.addAttribute("error", "Error saving task: " + e.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/todos/{id}/toggle")
    @Transactional
    public String toggle(@PathVariable Long id, RedirectAttributes ra) {
        try {
            Optional<Todo> opt = repository.findById(id);
            if (opt.isPresent()) {
                Todo todo = opt.get();
                boolean wasCompleted = todo.isCompleted();
                todo.setCompleted(!todo.isCompleted());
                
                // Update ordering when completion status changes
                if (wasCompleted && !todo.isCompleted()) {
                    // Completed task made active again
                    Integer maxOrder = repository.findMaxDisplayOrderByCompleted(false);
                    todo.setDisplayOrder(maxOrder != null ? maxOrder + 1 : 0);
                } else if (!wasCompleted && todo.isCompleted()) {
                    // Active task completed
                    Integer maxOrder = repository.findMaxDisplayOrderByCompleted(true);
                    todo.setDisplayOrder(maxOrder != null ? maxOrder + 1 : 0);
                }
                
                repository.save(todo);
            } else {
                ra.addAttribute("error", "Task not found");
            }
        } catch (Exception e) {
            ra.addAttribute("error", "Error updating task: " + e.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/todos/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            if (repository.existsById(id)) {
                repository.deleteById(id);
            } else {
                ra.addAttribute("error", "Task to delete not found");
            }
        } catch (Exception e) {
            ra.addAttribute("error", "Error deleting task: " + e.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/todos/{id}/priority")
    public String updatePriority(@PathVariable Long id, 
                                  @RequestParam("priority") String priority,
                                  RedirectAttributes ra) {
        try {
            Optional<Todo> opt = repository.findById(id);
            if (opt.isPresent()) {
                Todo todo = opt.get();
                try {
                    todo.setPriority(Priority.valueOf(priority.toUpperCase()));
                    repository.save(todo);
                } catch (IllegalArgumentException e) {
                    ra.addAttribute("error", "Invalid priority value");
                }
            } else {
                ra.addAttribute("error", "Task not found");
            }
        } catch (Exception e) {
            ra.addAttribute("error", "Error updating priority: " + e.getMessage());
        }
        return "redirect:/";
    }

    // AJAX endpoint for Drag & Drop
    @PostMapping("/api/todos/reorder")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, String>> reorderTodos(@RequestBody ReorderRequest request) {
        try {
            List<Todo> todos = new ArrayList<>();
            for (Long id : request.getOrderedIds()) {
                Optional<Todo> todoOpt = repository.findById(id);
                if (todoOpt.isPresent()) {
                    todos.add(todoOpt.get());
                }
            }
            
            // Save new ordering
            for (int i = 0; i < todos.size(); i++) {
                Todo todo = todos.get(i);
                todo.setDisplayOrder(i);
                repository.save(todo);
            }
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Order updated successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Error updating order: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // DTO class
    public static class ReorderRequest {
        private List<Long> orderedIds;
        
        public List<Long> getOrderedIds() {
            return orderedIds;
        }
        
        public void setOrderedIds(List<Long> orderedIds) {
            this.orderedIds = orderedIds;
        }
    }
}