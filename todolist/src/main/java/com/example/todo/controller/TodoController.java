package com.example.todo.controller;

import com.example.todo.model.Priority;
import com.example.todo.model.Todo;
import com.example.todo.model.user;
import com.example.todo.repository.TodoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
            user currentUser = getCurrentUser();
            List<Todo> todos;

            if ("custom".equals(sortBy)) {
                todos = repository.findAllByUserOrderByCompletedAscDisplayOrderAscCreatedAtDesc(currentUser);
            } else {
                todos = repository.findAllByUserOrderByCompletedAscDisplayOrderAscCreatedAtDesc(currentUser);
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
            user currentUser = getCurrentUser();

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

            t.setUser(currentUser);

            Integer maxOrder = repository.findMaxDisplayOrderByUserAndCompleted(currentUser, false);
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
            user currentUser = getCurrentUser();
            Optional<Todo> opt = repository.findById(id);

            if (opt.isPresent() && opt.get().getUser().getId().equals(currentUser.getId())) {
                Todo todo = opt.get();
                boolean wasCompleted = todo.isCompleted();
                todo.setCompleted(!todo.isCompleted());

                if (wasCompleted && !todo.isCompleted()) {
                    Integer maxOrder = repository.findMaxDisplayOrderByUserAndCompleted(currentUser, false);
                    todo.setDisplayOrder(maxOrder != null ? maxOrder + 1 : 0);
                } else if (!wasCompleted && todo.isCompleted()) {
                    Integer maxOrder = repository.findMaxDisplayOrderByUserAndCompleted(currentUser, true);
                    todo.setDisplayOrder(maxOrder != null ? maxOrder + 1 : 0);
                }

                repository.save(todo);
            } else {
                ra.addAttribute("error", "Task not found or access denied");
            }
        } catch (Exception e) {
            ra.addAttribute("error", "Error updating task: " + e.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/todos/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            user currentUser = getCurrentUser();
            Optional<Todo> opt = repository.findById(id);

            if (opt.isPresent() && opt.get().getUser().getId().equals(currentUser.getId())) {
                repository.delete(opt.get());
            } else {
                ra.addAttribute("error", "Task not found or access denied");
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
            user currentUser = getCurrentUser();
            Optional<Todo> opt = repository.findById(id);

            if (opt.isPresent() && opt.get().getUser().getId().equals(currentUser.getId())) {
                Todo todo = opt.get();
                try {
                    todo.setPriority(Priority.valueOf(priority.toUpperCase()));
                    repository.save(todo);
                } catch (IllegalArgumentException e) {
                    ra.addAttribute("error", "Invalid priority value");
                }
            } else {
                ra.addAttribute("error", "Task not found or access denied");
            }
        } catch (Exception e) {
            ra.addAttribute("error", "Error updating priority: " + e.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/api/todos/reorder")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, String>> reorderTodos(@RequestBody ReorderRequest request) {
        try {
            user currentUser = getCurrentUser();
            List<Todo> todos = new ArrayList<>();

            for (Long id : request.getOrderedIds()) {
                Optional<Todo> todoOpt = repository.findById(id);
                if (todoOpt.isPresent() && todoOpt.get().getUser().getId().equals(currentUser.getId())) {
                    todos.add(todoOpt.get());
                }
            }

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

        public List<Long> getOrderedIds() { return orderedIds; }
        public void setOrderedIds(List<Long> orderedIds) { this.orderedIds = orderedIds; }
    }

    private user getCurrentUser() {
        user user = new user();
        user.setId(1L);
        user.setUsername("testuser");
        return user;
    }
}
