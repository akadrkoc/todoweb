package com.example.todo.controller;

import com.example.todo.model.Priority;
import com.example.todo.model.Todo;
import com.example.todo.repository.TodoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Controller
public class TodoController {

    private final TodoRepository repository;

    public TodoController(TodoRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/")
    public String index(@RequestParam(value = "error", required = false) String error,
                        Model model) {
        try {
            List<Todo> todos = repository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));

            // Priority order: HIGH -> MEDIUM -> LOW, then completion status.
            todos.sort(Comparator
                    .comparing(Todo::isCompleted)
                    .thenComparing(todo -> todo.getPriority().ordinal())
                    .thenComparing(Todo::getCreatedAt, Comparator.reverseOrder()));
            
            model.addAttribute("todos", todos);
            model.addAttribute("error", error);
        } catch (Exception e) {
            model.addAttribute("todos", List.of());
            model.addAttribute("error", "An error occurred while loading tasks: " + e.getMessage());
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
                ra.addAttribute("error", "Title can't be empty");
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
            repository.save(t);
        } catch (Exception e) {
            ra.addAttribute("error", "Error occurred while saving task: " + e.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/todos/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes ra) {
        try {
            Optional<Todo> opt = repository.findById(id);
            if (opt.isPresent()) {
                Todo todo = opt.get();
                todo.setCompleted(!todo.isCompleted());
                repository.save(todo);
            } else {
                ra.addAttribute("error", "Task not found!");
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
                ra.addAttribute("error", "No task found to delete!");
            }
        } catch (Exception e) {
            ra.addAttribute("error", "Error while deleting task: " + e.getMessage());
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
                    ra.addAttribute("error", "Invalid priority value!");
                }
            } else {
                ra.addAttribute("error", "Task not found!");
            }
        } catch (Exception e) {
            ra.addAttribute("error", "Error updating priority: " + e.getMessage());
        }
        return "redirect:/";
    }
}